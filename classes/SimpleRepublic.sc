
SimpleRepublic {

	var <broadcastAddr, <republicName;
	var <addrs, <nickname, <nameList;
	var <>fixedLangPort = true;
	var <>verbose = false, <>private = false; // use this later to delegate traffic
	var <oldAddrs, task, resp, broadcastWasOn;
	
	classvar <>default;
	
	*new { |broadcastAddr, republicName = '/republic'|
		^super.newCopyArgs(broadcastAddr, republicName).init
	}
	
	makeDefault { default = this }
	
	init {
		addrs = ();
		oldAddrs = ();
		nameList = List.new;
	}
	
	join { |name|
	
		this.checkLangPort;
		
		this.leave;
		this.switchBroadcast(true);
		
		nickname = name ? nickname;
		
		this.makeResponder;
		this.makeSender;
		
	}
	
	
	leave {
		task.stop;
		resp.remove;
		addrs.do(_.disconnect);
		addrs = ();
		oldAddrs = ();
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
		var deadAddr = this.getDifference(oldAddrs, addrs);
		var newAddr = this.getDifference(addrs, oldAddrs);
		
		deadAddr.keysDo { |key|
			this.removeParticipant(key);
			("% has just left the building.".format(key)).postln;
		};
		newAddr.keysValuesDo { |key, addr|
			this.addParticipant(key, addr);
			("% has joined the Republic.".format(key)).postln;
		};
		
		oldAddrs = addrs.copy;
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
				var otherNick = msg[1], addr;
				if(addrs.at(otherNick).isNil) {
					addr = NetAddr(replyAddr.addr.asIPString, replyAddr.port);
					addrs.put(otherNick, addr); // already put it here
				}
		}).add;
	}
	
	makeSender {
		var routine = Routine {
			inf.do { |i|
				broadcastAddr.do(_.sendMsg(republicName, nickname));
				if(i % 3 == 0) { this.assemble };
				nil.yield;
			}
		};
		routine.next; // send once immediately
		task.stop;
		task = SkipJack(routine, 2.0, name: republicName);
	}
	
	getDifference { |thisDict, thatDict|
		var res = ();
		thisDict.pairsDo {|key, val|
			if(thatDict[key].isNil) { res.put(key, val) }
		};
		^res
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

