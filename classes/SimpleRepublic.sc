
SimpleRepublic {
	classvar <>fixedLangPort = true; // this should be a classvar otherwise it can't be set before calling new...

	var <broadcastAddr, <republicName;
	var <addrs, <nickname, <nameList, <joined = false;
	var<>graceCount = 16;
	var <>verbose = false, <>private = false; // use this later to delegate traffic
	var <skip, <resp, <broadcastWasOn, <presence;
	
	classvar <>default;
	
	*new { |broadcastAddr, republicName = '/republic'|
		^super.newCopyArgs(broadcastAddr, republicName).init
	}
	
	*getBroadcastIPs { 
		^Platform.case(
			\osx, {
				unixCmdGetStdOut("ifconfig | grep broadcast | awk '{print $NF}'")
				.split($\n).reject(_.isEmpty) },
			\windows, { // untested?
				unixCmdGetStdOut("ifconfig | grep broadcast | awk '{print $NF}'")
				.split($\n).reject(_.isEmpty) },
			\linux, { // works at least on ubuntu and xandros...
				unixCmdGetStdOut("/sbin/ifconfig | grep Bcast | awk 'BEGIN {FS = \"[ :]+\"}{print $6}'")
				.split($\n).reject(_.isEmpty) }
		);
	}
	
	makeDefault { default = this }
	
	init {
		addrs = ();
		nameList = List.new;
		presence = ();

		this.checkLangPort; 
		this.makeResponder; 
		this.makeSkip; 
	}
	
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
	
	join { |name|
		name = name.asSymbol;
		if (this.nameIsFree(name)) { 		
			nickname = name;
	
			this.statusFunc;
			skip.play;
					
			fork { 0.5.wait; this.checkJoined }
		};
	}
	
	checkJoined { 
		joined = addrs.keys.includes(nickname); 
		if (joined.not) { 
			warn("Nickname % has not joined Republic yet.\n"
			"BroadcastAddr may be wrong!"
				.format(nickname));
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
	}
	
	send { |name ... args|
		this.prSendWithDict(addrs, name, [args])
	}
	
	
	// need to decide what to send to server or client.
	// probably better a class that does this.
	// 
	
	// compatibility with Collective
	
	myIP {
		^this.myAddr.hostname.asIPString
	}
	
	myAddr {
		^addrs[nickname]
	}
	
	channel { ^("/" ++ republicName).asSymbol }
	
	/*sendToAll { arg ... args;
		broadcastAddr.listSendMsg(args)
	}*/
	// send my name with every message?
	// check rest of send interface: sendToIndex etc.
	
	
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
		}
	}
	
	makeResponder {
		resp = OSCresponderNode(nil, republicName, 
			{ |t,r,	msg, replyAddr|
				var otherNick, otherID, addr;
				
				otherNick = msg[1]; 
				otherID = msg[2]; 
								
				if(addrs.at(otherNick).isNil) {
					addr = NetAddr(replyAddr.addr.asIPString, replyAddr.port);
										// pass on clientID in Republic
					this.addParticipant(otherNick, addr, otherID);
					(" ---> % has joined the Republic. ---".format(otherNick)).postln;
				};
				presence.put(otherNick, graceCount);
		}).add;
	}
	
	statusFunc { 
		"SimpleRepublic:statusFunc runs.".postln;
		if (nickname.notNil) { broadcastAddr.do(_.sendMsg(republicName, nickname)) };
		this.assemble; 
	}
	
	makeSkip {

		this.statusFunc; // do once immediately
		skip.stop;
		skip = SkipJack({ this.statusFunc }, 1.0, name: republicName);
	}
	
	checkLangPort {
		if(fixedLangPort and: { NetAddr.langPort != 57120 }) { 
			Error(
				"Can't join Republic: please try and restart SuperCollider"
				" to get langPort 57120."
			).throw
		};
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
}

