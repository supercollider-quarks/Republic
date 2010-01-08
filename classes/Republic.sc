
Republic : SimpleRepublic {

	var <clientID, <serverPort = 57110;
	var <servers, <synthDefs, <synthDescs, <>latency;
	var <s;
	var synthDefResp, synthDefSendCmd;
	
	
	init {
		super.init;
		servers = ();
		synthDefs = ();
		synthDescs = ();
		synthDefSendCmd =  republicName.asString ++ "/synthDef";
		s = RepublicServer(this); // interface to the event system
	}
	
	join { | nickname, argClientID = 0, argServerPort = 57110 |
		super.join(nickname);
		clientID = argClientID;
		serverPort = argServerPort;
		synthDefResp = OSCresponderNode(nil, synthDefSendCmd, { | t,r,msg |
			var name = msg[1];
			var bytes = msg[2];
			this.storeRemoteSynthDef(name, bytes)
		}).add; 
	}
	
	leave {
		synthDefResp.remove;
		servers.do(this.removeServer(_));
		servers = ();
		super.leave;
	}
	
		
	sendServer { |name ... args|
		// "send server %\nmessages: %\n".postf(name, [args]);
		this.prSendWithDict(servers, name, [args], latency)
	}
	
	sendServerBundle { |name ... args|
		// "send server %\nmessages: %\n".postf(name, args);
		this.prSendWithDict(servers, name, args, latency)
	}
	
	addSynthDef { | synthDef |
		synthDefs.put(synthDef.name.asSymbol, synthDef);
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
		addr = addr.addr.asIPString;
		port = port ?? { serverPort };
		// make a new server representation
		server = Server.new(name, NetAddr(addr, port), clientID: clientID);
		server.serverRunning_(true);
		servers.put(name, server);
		if(verbose) { "Republic(%): added server %\n".postf(nickname, name); };
		// send all synthdefs to the new server
		synthDefs.do { |synthDef| this.sendSynthDef(name, synthDef) };
		
	}
	
	removeServer { | name |
		servers.removeAt(name).remove;
	}
	
	
	sendSynthDef { | who, synthDef |
		var bytes = synthDef.asBytes;
		this.send(who, synthDefSendCmd, synthDef.name, bytes);
		this.sendServer(who, "/d_recv", bytes);
		// "Republic (%): sent synthdef % to %\n".postf(nickname, synthDef.name, who);
	}

	storeRemoteSynthDef { | name, bytes |
		var lib = SynthDescLib.global;
		var stream = CollStream(bytes);
		var dict = SynthDesc.readFile(stream, false, lib.synthDescs);
		var args = [\instrument, name];
		dict.at(name).controls.do { |ctl| 
			args = args.add(ctl.name.asSymbol).add(ctl.defaultValue.round(0.00001))
		};			
		"// % SynthDef \"%\" added:\n".postf(nickname, name);
		().putPairs(args).postcs;
		synthDescs.put(name, lib.at(name));
	}
	

}

// this can be sneaked into events

RepublicServer {
	var <republic;
	
	*new { |republic|
		^super.newCopyArgs(republic)
	}
	
	sendBundle { |time ... msgs|
		var indexWho, who;
		indexWho = msgs[0].indexOf(\where); // indexOf is very fast in arrays
		who = indexWho !? { msgs[0][indexWho + 1] };
		republic.sendServerBundle(who, *msgs);
	}
	
	sendMsg { |... msg|
		var indexWho, who;
		indexWho = msg.indexOf(\where); // indexOf is very fast in arrays
		who = indexWho !? { msg[0][indexWho + 1] };
		republic.sendServerBundle(who, msg);
	}
	
	nextNodeID { |where|
		var server = republic.servers.at(where ? republic.nickname) ?  Server.default;
		^server.nextNodeID
	}
	
	latency {
		republic.latency
	}

}




+ Server {

	remove {
		Server.all.remove(this);
		Server.named.removeAt(name);
		SynthDescLib.global.removeServer(this);
		try { this.window.close };
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


