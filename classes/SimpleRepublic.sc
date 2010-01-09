
SimpleRepublic {

	var <broadcastAddr, <republicName;
	var <addrs, <nickname, <nameList;
	var <>fixedLangPort = true, <>graceCount = 16;
	var <>verbose = false, <>private = false; // use this later to delegate traffic
	var task, resp, broadcastWasOn, <presence;
	
	classvar <>default;
	
	*new { |broadcastAddr, republicName = '/republic'|
		^super.newCopyArgs(broadcastAddr, republicName).init
	}
	
	makeDefault { default = this }
	
	init {
		addrs = ();
		nameList = List.new;
		presence = ();
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
				var otherNick = msg[1], addr;
				if(addrs.at(otherNick).isNil) {
					addr = NetAddr(replyAddr.addr.asIPString, replyAddr.port);
					this.addParticipant(otherNick, addr);
					(" ---> % has joined the Republic. ---".format(otherNick)).postln;
				};
				presence.put(otherNick, graceCount);
		}).add;
	}
	
	makeSender {
		var routine = Routine {
			inf.do { |i|
				broadcastAddr.do(_.sendMsg(republicName, nickname));
				this.assemble;
				nil.yield;
			}
		};
		routine.next; // send once immediately
		task.stop;
		task = SkipJack(routine, 1.0, name: republicName);
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

