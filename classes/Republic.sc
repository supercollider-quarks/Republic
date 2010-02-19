
Republic : SimpleRepublic {
	classvar <maxID = 31;

	var <clientID, <serverPort = 57109;
	var <servers, <synthDefs, <synthDescs, <>latency;
	var <republicServer;
	var <synthDefResp, synthDefSendCmd;
	var setAllocator;
	var <allIDs;
	
	var <>usesRandIDs = true;
	
	init {
		super.init;
		servers = ();
		synthDefs = ();
		synthDescs = ();
		allIDs = ();
		synthDefSendCmd =  republicName.asString ++ "/synthDef";
		
		setAllocator = { |serv| // better take this one, we use one for all
			serv.nodeAllocator = republicServer.nodeAllocator;
		};
		
	}
	
	join { | name, argClientID, argServerPort |
		
		if (this.nameIsFree(name)) { 
				// set clientID first so statusFunc call works
			clientID = argClientID ?? { this.freeID };
			super.join(name);

			serverPort = argServerPort ? serverPort;
			
			republicServer = RepublicServer(this, clientID); // interface to the event system
			
			synthDefResp = OSCresponderNode(nil, synthDefSendCmd, { | t,r,msg |
				var name = msg[1];
				var bytes = msg[2];
				this.storeRemoteSynthDef(name, bytes)
			}).add;
		};
	}

	assemble {
		super.assemble; 
				// make servers if clientID was added, 
				// and server is missing
		if (clientID.notNil) { 
			presence.keys.do { |key|
				var serv = servers[key];
				if (serv.isNil) { this.addServer(key, addrs[key]) }
			};
		}
	}
	
	leave { |free = false| 
		synthDefResp.remove;
		servers.do(this.removeServer(_));
		try { servers.at(nickname).quit };
			// quit all my nodes on the server if possible? 
		servers.do { }; 
		servers = ();
		clientID = nil; 
		super.leave(free);	// keep lurking by default
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
	
	freeID { 
		var res;
		if (usesRandIDs) { 
			res = (0..maxID).removeAll(allIDs.keys).choose;
		} { 
			res = (0 .. maxID).detect { |i| allIDs.includes(i).not };
		}; 
		if (res.isNil) { 
			warn("Republic: no more free clientIDs!"); 
			^nil	
		} { ^res }
	}

	addParticipant { | key, addr, clID |
		
		addrs.put(key, addr); 
		allIDs.put(key, clID);
		
		if (clientID.notNil) { 
			this.addServer(key, addr);
		}
	}
			
	removeParticipant { | key |
		addrs.removeAt(key);
		allIDs.removeAt(key);
		this.removeServer(key); 
	}

	statusFunc { 
		if (nickname.notNil) { 
			broadcastAddr.do(_.sendMsg(republicName, nickname, clientID)) 
		};
		
		this.assemble; 
	}
		
	addServer { | name, addr, port |
		var server, options;
		server = Server.named.at(name);

		if(server.isNil) {
			"\n Republic: new server added: %\n".postf(name);
			addr = addr.addr.asIPString;
			if(name == nickname) { addr = "127.0.0.1" }; // replace by loopback
			port = port ?? { serverPort };
			// make a new server representation
			server = Server.new(name, NetAddr(addr, port), clientID: clientID);
			
			server.tree = setAllocator;
			
			if(name == nickname) {
				"my own.".postln;
				options = Server.default.options.copy;
				options.numAudioBusChannels = 128 * 32;
				options.numControlBusChannels = 4096 * 32;
				options.memSize = 8192 * 32;
				server.options = options;
				server.boot;
				defer { server.makeWindow };
			} {
				"	server % not my own, assume running.\n".postf(name);
				server.serverRunning_(true);
			};
				
			// not sure if compatible
			server.latency = latency;
		} {
			"	server % already there - fine.\n".postf(name);
			server.tree = setAllocator;
			server.boot;
		};
			
		servers.put(name, server);
		server.sendBundle(nil, ['/error', -1],['/notify', 1],['/error', -2]);
		
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
		
		try { this.manipulateSynthDesc(name) };
		synthDescs.put(name, lib.at(name));
		
		lib.servers.do({ |server|
			server.value.sendBundle(nil, ["/d_recv", bytes])
		});
		
		// post a prototype event:	
		dict.at(name).controls.do { |ctl| 
			args = args.add(ctl.name.asSymbol).add(ctl.defaultValue.round(0.00001))
		};
		"// SynthDef \"%\" added:\n".postf(name);
		().putPairs(args).postcs;
		
	}
	
	manipulateSynthDesc { | name |
		var synthDesc = SynthDescLib.at(name);
		var ctl = synthDesc.controlNames;
		synthDesc !? {
			if(ctl.isNil or: { synthDesc.controlNames.includes("where").not }) {
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
	
	*unshare { |name|
		var republic = republic ? Republic.default;
		if(republic.notNil) { 
			republic.removeSynthDef(this)
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

