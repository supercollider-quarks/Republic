
Republic : SimpleRepublic {

	var <clientID, <serverPort = 57109;
	var <servers, <synthDefs, <synthDescs, <>latency;
	var <republicServer;
	var <synthDefResp, synthDefSendCmd;
	var setAllocator;
	
	
	init {
		super.init;
		servers = ();
		synthDefs = ();
		synthDescs = ();
		synthDefSendCmd =  republicName.asString ++ "/synthDef";
		
		setAllocator = { |serv| // better take this one, we use one for all
			serv.nodeAllocator = republicServer.nodeAllocator;
		};
		
	}
	
	join { | nickname, argClientID = 0, argServerPort = 57110 |
		
		super.join(nickname);
		
		clientID = argClientID;
		serverPort = argServerPort;
		
		republicServer = RepublicServer(this, clientID); // interface to the event system
		
		synthDefResp = OSCresponderNode(nil, synthDefSendCmd, { | t,r,msg |
			var name = msg[1];
			var bytes = msg[2];
			this.storeRemoteSynthDef(name, bytes)
		}).add;
		
		
	}
	
	leave {
		synthDefResp.remove;
		servers.do(this.removeServer(_));
		try { servers.at(nickname).quit };
		servers = ();
		super.leave;
	}
		
		
	sendServer { |name ... args|
		// "send server %\nmessages: %\n".postf(name, [args]);
		this.prSendWithDict(servers, name, [args], latency)
	}
	
	sendServerBundle { |latency, name ... args|
		// "send server %\nmessages: %\n".postf(name, args);
		this.prSendWithDict(servers, name, args, latency)
	}
	
	addSynthDef { | synthDef |
		var name = synthDef.name.asSymbol;
		synthDefs.put(name, synthDef);
		this.sendSynthDef(\all, synthDef);
	}
	
	removeSynthDef { | name |
		synthDefs.removeAt(name);
		// maybe remove all synthDescs for everyone?
	}
	
		
	// private implementation
	
	addParticipant { | key, addr |
		addrs.put(key, addr); 
		this.addServer(key, addr);
	}
	
	removeParticipant { | key |
		addrs.removeAt(key);
		this.removeServer(key); 
	}
		
	addServer { | name, addr, port |
		var server;
		server = Server.named.at(name);

		if(server.isNil) {
			"new server: %\n".postf(name);
			addr = addr.addr.asIPString;
			if(name == nickname) { addr = "127.0.0.1" }; // replace by loopback
			port = port ?? { serverPort };
			// make a new server representation
			server = Server.new(name, NetAddr(addr, port), clientID: clientID);
			
			server.tree = setAllocator;
			
			if(name == nickname) {
				"my own.".postln;	
				server.boot;
				defer { server.makeWindow };
			} {
				"server % not my own, but assume running.\n".postf(name);
				server.serverRunning_(true);
			};
				
			// not sure if compatible
			server.latency = latency;
		} {
			"server % already there - fine.\n".postf(name);
			server.tree = setAllocator;
			server.boot;
		};
			
		servers.put(name, server);
		if(verbose) { "Republic(%): added server %\n".postf(nickname, name); };
		// send all synthdefs to the new server
		synthDefs.do { |synthDef| 
			this.sendSynthDef(name, synthDef) 
		};
		setAllocator.value(server);
		
	}
	
	removeServer { | name |
		servers.removeAt(name).remove;
	}
	
	
	sendSynthDef { | who, synthDef |
		var bytes = synthDef.asBytes;
		this.send(who, synthDefSendCmd, synthDef.name, bytes);
		this.sendServer(who, "/d_recv", bytes);
		if(verbose) { "Republic (%): sent synthdef % to %\n".postf(nickname, synthDef.name, who) };
	}

	storeRemoteSynthDef { | name, bytes |
		var lib = SynthDescLib.global;
		var stream = CollStream(bytes);
		var dict = SynthDesc.readFile(stream, false, lib.synthDescs);
		var args = [\instrument, name];
		
		this.manipulateSynthDesc(name);
		
		synthDescs.put(name, lib.at(name));
		
		lib.servers.do({ |server|
			server.value.sendBundle(nil, ["/d_recv", bytes])
		});
		
		// post a prototype event:	
		dict.at(name).controls.do { |ctl| 
			args = args.add(ctl.name.asSymbol).add(ctl.defaultValue.round(0.00001))
		};
		"// % SynthDef \"%\" added:\n".postf(nickname, name);
		().putPairs(args).postcs;
		
	}
	
	manipulateSynthDesc { | name |
		var synthDesc = SynthDescLib.at(name);
		synthDesc !? {
			if(synthDesc.controlNames.includes("where").not) {
				//"adding where..".postln;
				synthDesc.controlNames = synthDesc.controlNames.add("where");
				synthDesc.controls = synthDesc.controls.add(
					ControlName().name_("where").defaultValue_(nickname);				);
				// synthDesc.controlNames.postcs;
				synthDesc.makeMsgFunc; // again
			};
		}
	}
	
	// sure?
	s { ^republicServer ? Server.default }
	
	homeServer {
		^servers.at(nickname)
	}
	
	*dumpOSC { |flag = true|
		thisProcess.recvOSCfunc = if(flag) { { |time, replyAddr, msg| 
			if(#['status.reply', '/status.reply'].includes(msg[0]).not) {
				"At time %s received message % from %\n".postf( time, msg, replyAddr )
			}  // post all incoming traffic except the server status messages
			} } { nil }
	}


}

// this can be sneaked into events.
// again, no gated synths currently

RepublicServer {
	var <republic, <clientID;
	var <nodeAllocator, <audioBusAllocator, <controlBusAllocator, <bufferAllocator;
	
	*new { |republic, clientID|
		^super.newCopyArgs(republic, clientID).init
	}
	
	init {
		var options = Server.default.options;
		/*if(options.numAudioBusChannels <= 1024) {
			("in networks, it makes sense to set the allocators maximum much higher."
			"\ne.g. current value of numAudioBusChannels: % suggested: 2048"
			).format(options.numAudioBusChannels);
			options.numAudioBusChannels = 4096;
			options.numControlBusChannels = 8192;
			options.numBuffers = 2048 + 2;
		};*/
		this.newAllocators(republic.clientID, options);
	}

	
	sendBundle { |time ... msgs|
		republic.sendServerBundle(time, this.findWhere(msgs[0]), *msgs);
	}
	
	sendMsg { |... msg|
		republic.sendServerBundle(nil, this.findWhere(msg), msg);
	}
	
	findWhere { |msg|
		var indexWhere, where;
		indexWhere = msg.indexOf(\where); // indexOf is very fast in arrays
		^indexWhere !? { msg[indexWhere + 1] };
	}
	
	nextNodeID { |where|
		 ^nodeAllocator.alloc
		// ^-1
	}
	
	latency {
		^republic.latency
	}
	
	name {
		^republic.nickname
	}

	newAllocators { |clientID, options|
		nodeAllocator = NodeIDAllocator(clientID, options.initialNodeID);
		/*controlBusAllocator = PowerOfTwoAllocator.newRange(
			clientID, options.numControlBusChannels);
		audioBusAllocator = PowerOfTwoAllocator.newRange(
				clientID, options.numAudioBusChannels, options.firstPrivateBus);
		bufferAllocator = PowerOfTwoAllocator.newRange(clientID, options.numBuffers);*/
	}

	
}




+ Server {

	remove {
		Server.all.remove(this);
		Server.named.removeAt(name);
		SynthDescLib.global.removeServer(this);
		try { this.window.close };
	}
	
	nodeAllocator_ { |allocator|
		nodeAllocator = allocator
	}
	
	controlBusAllocator_ { |allocator|
		controlBusAllocator = allocator
	}
	
	audioBusAllocator_ { |allocator|
		audioBusAllocator = allocator
	}
	
	bufferAllocator_ { |allocator|
		bufferAllocator = allocator
	}
}

+ SynthDef {

	share { | republic |
		republic = republic ? Republic.default;
		if(republic.isNil) {
			this.memStore 
		} {
			republic.addSynthDef(this);
		}
	}

}

/*+ PowerOfTwoAllocator {

	// to be tested.
	
	*newRange { |user = 0, size, pos = 0|
		
		var blockStart, blockSize, block;
		
	//	[\user, user, \size, size, \pos, pos].postln;
		
		if(user > 31) { "maximal 32 users, maximum clientID is 31".error };
	
		blockSize = (size - pos) div: 32;
		blockStart = pos + (user * blockSize);
	//	[\blockSize, blockSize, \blockStart, blockStart].postln;
		^this.new(blockSize, blockStart + blockSize)
	}
	
}
*/

