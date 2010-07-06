
SimpleRepublic {
	
	var <broadcastAddr, <republicName;
	var <addrs, <nickname, <nameList, <joined = false;
	var <graceCount = 16;
	var <>verbose = false, <>private = false; // use this later to delegate traffic
	var <skip, <resp, <broadcastWasOn, <presence;
	
	classvar <>default;
	
	*new { |broadcastAddr, republicName = '/republic'|
		^super.newCopyArgs(broadcastAddr, republicName).init
	}
	
	makeDefault { default = this }
	
	init {
		broadcastAddr = broadcastAddr ?? {
			NetAddrMP("255.255.255.255", 57120 + (0..7))
		};
		// broadcastAddr.checkServesLangPort;
		this.switchBroadcast(true);
		
		addrs = ();
		nameList = List.new;
		presence = ();

		this.makeResponder; 
		this.makeSkip; 
	}
	
	
	join { |name|
		name = name.asSymbol;
		if (this.nameIsFree(name)) {
			nickname = name;
			this.statusFunc;
			skip.play;
			fork { 0.5.wait; this.checkJoined }
		}
	}
	
	
	leave { |free = false| 
		nickname = nil;
		joined = false;
		if (free) { this.free }
	}
	
	free { 
		skip.stop; 
		SkipJack.all.remove(skip);
		resp !? { resp.remove };
		addrs.do(_.disconnect);
		addrs = ();
		this.switchBroadcast(false);
		OSCresponder(nil, '/hist').remove;
	}
	
	send { |name ... args|
		this.prSendWithDict(addrs, name, [args])
	}
	
	
	// testing
	
	nameIsFree { |name| 
		var nameIsUsed = addrs.keys.includes(name); 
		if (nameIsUsed) { 
			if (name == nickname) { 
				inform("Republic  %: You have already joined as %.\n"
					.format(republicName, nickname));
			} { 
				inform("Republic %: nickname % is already in use!\n".format(name));
			}
		};
		^nameIsUsed.not
	}
	
	checkJoined { 
		joined = addrs.keys.includes(nickname); 
		if (joined.not) { 
			warn("Nickname % has not joined Republic yet.\n"
			"BroadcastAddr may be wrong!"
				.format(nickname));
		}
	}
	
	
	// compatibility with Collective
	
	myIP {
		^this.myAddr.hostname.asIPString
	}
	
	myAddr {
		^addrs[nickname]
	}
	
	channel { ^("/" ++ republicName).asSymbol }
	
	clientID { ^0 }
	
	// private implementation

	
	assemble {
		presence.keys.do { |key|
			var newVal = presence[key] - 1;
			
			if(newVal < 0) {
				this.removeParticipant(key);
				presence.removeAt(key);
				(" --- % has just left the building. --->".format(key)).postln;
			} { 
				presence.put(key, newVal)
			}
		};
		if(verbose) { presence.postln };
	}
	
	addParticipant { |key, addr|
		addrs.put(key, addr);
		if(nameList.includes(key).not) {
			nameList.add(key);
		};
	}
	
	removeParticipant { |key|
		addrs.removeAt(key);
		nameList.remove(key);
		presence.removeAt(key);
	}
	
	switchBroadcast { |flag|
		if(flag) {
			broadcastWasOn = NetAddr.broadcastFlag;
			NetAddr.broadcastFlag = true;
		} {
			NetAddr.broadcastFlag = broadcastWasOn;
		};
		"\n\nRepublic: switched global NetAddr broadcast flag to %.\n".format(flag).postln;
	}
	
	makeResponder {
		resp = OSCresponderNode(nil, republicName, 
			{ |t,r,	msg, replyAddr|
				var otherNick, otherID, addr, extraData;
				var tempo, beats, serverConfig;
				
				otherNick = msg[1];
				otherID = msg[2];
				extraData = msg[3..];
												
								
				if(addrs.at(otherNick).isNil) {
					addr = NetAddr(replyAddr.addr.asIPString, replyAddr.port);
					this.addParticipant(otherNick, addr, otherID, extraData);
					(" ---> % has joined the Republic. ---".format(otherNick)).postln;
				};
				presence.put(otherNick, graceCount);
		}).add;
	}
		
	statusFunc {
		var msg;
		if (nickname.notNil) {
			msg = [republicName] ++ this.statusData;
			broadcastAddr.do(_.sendBundle(nil, msg)) 
		};
		this.assemble; 
	}
	
	statusData {
		// subclass compatibility
		// the two 0's are for backwards compatibility (used to be beats and phase) 
		^[nickname, this.clientID, 0, 0] 
	}
	
	makeSkip {
		this.statusFunc; // do once immediately
		skip.stop;
		skip = SkipJack({ this.statusFunc }, 1.0, name: republicName);
	}

	
	// GUI
	
	gui { |parent, bounds|
		^EZRepublicGui(parent, bounds, this);
	}
	
	shareHistory { |useShout = true, winWhere| 
		if (useShout) { 
			OSCresponder(nil, '/hist', {|t,r,msg| 
				var who = msg[1];
				var codeStr = msg[2].asString;
				History.enter(codeStr, who);
				if (codeStr.beginsWith(Shout.tag)) { 
					defer { 
						Shout((codeStr.drop(Shout.tag.size).reject(_ == $\n) + ("/" ++ who)).postcs) 
					}
				}; 
			}).add; 	
		} {
			OSCresponder(nil, '/hist', {|t,r,msg| 
				History.enter(msg[2].asString, msg[1]) 
			}).add; 
		} { 
		
		};
		
		History.forwardFunc = { |code|
			if(joined) { this.send(\all, '/hist', nickname, code) }
		};	
	
		History.start;
		History.makeWin(winWhere);
		History.localOff;
			
	}
	
	*dumpOSC { |flag = true|
		thisProcess.recvOSCfunc = if(flag) { { |time, replyAddr, msg| 
			if(#['status.reply', '/status.reply'].includes(msg[0]).not) {
				"At time %s received message % from %\n".postf( time, msg, replyAddr )
			}  // post all incoming traffic except the server status messages
			} } { nil }
	}
	
	
	// if name == \all, send to each item in the dict.
	// otherwise send to each of the given names
	
	prSendWithDict { |dict, names, messages, latency|
		names = names ? nickname; // send to myself if none is given
		if(verbose) { "sent messages to: %.\nmessages: %\nlatency: %\n"
				.postf(names, messages, latency)};
		if(names == \all) {
			dict.do { |recv| recv.sendBundle(latency, *messages) }
		} {
			names.asArray.do { |name|
				var recv = dict.at(name);
				if(recv.isNil) { 
					"% is currently absent.\n".postf(name)
				} {
					recv.sendBundle(latency, *messages)
				};
			}
		}
	}
	
	// deprecated
	
	*fixedLangPort {
		^this.deprecated(thisMethod)
	}
	
	*fixedLangPort_ {
		^this.deprecated(thisMethod)
	}
	
	*getBroadcastIPs {
		"Please use instead: NetAddr.getBroadcastIPs".postln;
		^this.deprecated(thisMethod, NetAddr.findRespondingMethodFor(\getBroadcastIPs))
	}
	
}

