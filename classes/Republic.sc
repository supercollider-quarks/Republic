

Republic : SimpleRepublic {
	
	classvar <maxID = 31;

	var <serverPort = 57109;
	var <servers, <>latency;
	var <republicServer, <clientID, <>options;
	var <synthDefResp, synthDefSendCmd;
	var <allClientIDs;
	var <>onJoinAction;
	
	var <>usesRandIDs = true, <>usesSeparateState = true, <>postSource = true;
	
	init {
		super.init;
		servers = ();
		allClientIDs = ();
		synthDefSendCmd =  republicName.asString ++ "/synthDef";
		options = this.defaultServerOptions;
	}
	
	join { | name, argClientID, argServerPort |
		
		name = name.asSymbol;
		if (this.nameIsFree(name)) {
			clientID = argClientID ?? { this.nextFreeID };
			serverPort = argServerPort ? serverPort;
			
			super.join(name);
			this.initEventSystem;
		}
	}
	
	leave { |free = false| 
		
		synthDefResp.remove;
		servers.do { |sharedServer| sharedServer.freeAll };
		try { servers.at(nickname).quit };
		servers.do(this.removeServer(_));
		servers = ();
		
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
		this.sendSynthDef(\all, synthDef);
	}
	
	removeSynthDef { | name |
		// synthDefs.removeAt(name);
		// maybe remove all synthDescs for everyone?
		^this.notYetImplemented
	}
			
	// private implementation
	
	nextFreeID { 
		var res;
		if (usesRandIDs) { 
			res = (0..maxID).removeAll(allClientIDs.keys).choose;
		} { 
			res = (0 .. maxID).detect { |i| allClientIDs.includes(i).not };
		}; 
		if (res.isNil) { 
			warn("Republic: no more free clientIDs!"); 
			^nil	
		} { ^res }
	}

	addParticipant { | key, addr, otherClientID, config |
		
		addrs.put(key, addr); 
		allClientIDs.put(key, otherClientID);
		
		if (clientID.notNil) { // I play with my own id on remote server
			this.addServer(key, addr, serverPort, config);
		}
	}
			
	removeParticipant { | key |
		addrs.removeAt(key);
		allClientIDs.removeAt(key);
		this.removeServer(key); 
	}
		
	addServer { | name, addr, port, config |
		
		var server = Server.named.at(name);
		var isLocalAndInRepublic = addrs.at(nickname).notNil and: {
				addrs.at(nickname).hostname == addr.hostname
		};
		// if nonexistent or in the republic already but local, we just add the server anew
		// this only happens when we keep several republics with the same name locally
		if(server.isNil or: isLocalAndInRepublic) {
			server = this.makeNewServer(name, addr, port, config);
		} {
			this.displayServer(server);
			"\nRepublic (%): server % already there - fine.\n".postf(nickname, name);
			"You may still need to boot the server\n".postln;
		};
			
		servers.put(name, server);
		server.sendBundle(nil, ['/error', 0], ['/notify', 1]);
		
		// send all synthdefs to the new server
		this.shareSynthDefs(name);		
	}
	
	removeServer { | who |
		servers.removeAt(who).remove;
	}
	
	displayServer { |server|
		defer { try { server.makeGui } };	
	}
	
	makeNewServer { | name, addr, port, config |
			
			var newServer, serverOptions;
					
			addr = addr.addr.asIPString;
			if(name == nickname) { addr = "127.0.0.1" }; // replace by loopback
			port = port ?? { serverPort };
			
			// make a new server representation
			newServer = SharedServer.new(name, NetAddr(addr, port), clientID: clientID);
			
			"\nRepublic (%): new server added: %\n".postf(nickname, name);
			
			if(name == nickname) {
				newServer.options = options;
				this.displayServer(newServer);
				newServer.waitForBoot { onJoinAction.value(this, newServer) };		
			} {
				newServer.options = this.defaultServerOptions(config);
				"\nRepublic (%): server % not my own, assume running.\n".postf(nickname, name);
				newServer.serverRunning_(true);
			};
			
			newServer.latency = latency; // not sure if compatible
			^newServer
	}
	
	defaultServerOptions { |config|
		var op = SharedServerOptions.fromConfig(config);
		var maxNumClients = (maxID + 1);
		if(usesSeparateState) {
			op.numAudioBusChannels = 128 * maxNumClients;
			op.numControlBusChannels = 4096 * maxNumClients;
			op.memSize = 8192 * maxNumClients;
			op.numClients = maxID;
		};
		^op
	}
		
	statusData {
		var res = super.statusData;
		options !? { res = res ++ options.asConfig }; // send hardware info etc.
		^res
	}
	
	// sharing synth defs
	
	
	shareSynthDefs { | who |
		fork {
			rrand(1.0, 2.0).wait; // wait for the other server to boot
			SynthDescLib.global.synthDescs.do { |synthDesc|
					var sentBy, bytes, doSend, sourceCode;
					synthDesc.metadata !? { 
						sentBy = synthDesc.metadata.at(\sentBy);
						bytes = synthDesc.metadata.at(\bytes);
						sourceCode = synthDesc.metadata.at(\sourceCode);
					};
					
					doSend = sentBy.notNil and: { bytes.notNil }
							and: {
								nickname == sentBy	// was me
								or: { addrs.at(sentBy).isNil } // has left
							};
					if(doSend) {
						this.sendSynthDefBytes(who, synthDesc.name, bytes, sourceCode);
						0.1.rand.wait; // distribute load
					}
			}
		}
	}
	
	sendSynthDef { | who, synthDef |
		this.sendSynthDefBytes(who, synthDef.name, 
				synthDef.asBytes, synthDef.asCompileString);
		if(verbose) { "Republic (%): sent synthdef % to %\n".postf(nickname, synthDef.name, who) };
	}
	
	sendSynthDefBytes { | who, defName, bytes, sourceCode |
		this.send(who, synthDefSendCmd, nickname, defName, sourceCode, bytes);
		this.sendServer(who, "/d_recv", bytes);
	}

	synthDescs {
		^SynthDescLib.global.synthDescs.select { |desc| 
			desc.metadata.notNil and: { desc.metadata[\sentBy].notNil } 
		}
	}
	
	storeRemoteSynthDef { | sentBy, name, sourceCode, bytes |

		var lib = SynthDescLib.global;
		var stream = CollStream(bytes);
		var dict = SynthDesc.readFile(stream, false, lib.synthDescs);
		var args = [\instrument, name];
		var synthDesc = lib.at(name);
		
		try { this.manipulateSynthDesc(name) };
		
		// add the origin and SynthDef data to the metadata field
		synthDesc.metadata = (sentBy: sentBy, bytes: bytes, sourceCode: sourceCode);
		
		// post a prototype event:	
		dict.at(name).controls.do { |ctl| 
			args = args.add(ctl.name.asSymbol).add((ctl.defaultValue ? 0.0).round(0.00001))
		};
		"// SynthDef \"%\" added:\n".postf(name);
		if(postSource) {
			sourceCode = format("%.share", sourceCode);
			if(sourceCode.find("\n").notNil) { sourceCode = format("(\n%\n);", sourceCode) };
			"\n%\n".postf(sourceCode);
		};
		().putPairs(args).postcs;
	}
	
	manipulateSynthDesc { | name |
		var synthDesc = SynthDescLib.at(name);
		var ctl = synthDesc.controlNames;
		// this is done to guarantee that the "where" parameter is collected from the event
		synthDesc !? {
			if(ctl.isNil or: { synthDesc.controlNames.includes("where").not }) {
				synthDesc.controlNames = synthDesc.controlNames.add("where");
				synthDesc.controls = synthDesc.controls.add(
					ControlName().name_("where").defaultValue_(nickname);				);
				synthDesc.makeMsgFunc; // make msgFunc again
			};
		}
	}
	
	s { ^republicServer ? Server.default }
	
	myServer {
		^servers.at(nickname)
	}
	
	asTarget {
		^this.myServer.asTarget
	}
	
	homeServer {
		this.deprecated(thisMethod);
		^this.myServer
	}

	initEventSystem {
		synthDefResp = OSCresponderNode(nil, synthDefSendCmd, { | t, r, msg |
				
				var sentBy = msg[1];
				var name = msg[2];
				var sourceCode = msg[3];
				var bytes = msg[4];

				this.storeRemoteSynthDef(sentBy, name, sourceCode, bytes)
			}).add;
			
			// this is only an interface to the event system
			republicServer = RepublicServer(this, clientID); 
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
		this.newAllocators;
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
		 ^nodeAllocator.value.alloc
		// ^-1
	}
	
	asTarget {
		^republic.asTarget	
	}
	
	latency {
		^republic.latency
	}
	
	name {
		^republic.nickname
	}
	
	// this needs some work, other allocators..
	newAllocators {
		nodeAllocator = { republic.myServer.nodeAllocator };
	}

	
}



+ SynthDef {

	share { | republic |
		republic = republic ? Republic.default;
		if(republic.isNil) {
			if(Main.versionAtMost(3, 3)) { this.memStore } { this.add }
		} {
			republic.addSynthDef(this)
		}
	}
	
	*unshare { |name|
		^this.notYetImplemented;
	}
	
	/*	
	*unshare { |name|
		var republic = republic ? Republic.default;
		if(republic.notNil) { 
			republic.removeSynthDef(this)
		}
		
	}*/

}


