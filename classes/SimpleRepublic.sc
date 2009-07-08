
SimpleRepublic {

	var <broadcastAddr, <republicName;
	var <addrs, <nickname;
	var oldAddrs, task, resp, broadcastWasOn;
	
	classvar <>default;
	
	*new { |broadcastAddr, republicName = '/republic'|
		^super.newCopyArgs(broadcastAddr, republicName).init
	}
	
	makeDefault { default = this }
	
	init {
		addrs = ();
		oldAddrs = ();
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
		this.prSendWithDict(addrs, name, args)
	}
	
	
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
	}
	
	removeParticipant { |key|
		addrs.removeAt(key);
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
					addr = NetAddr(replyAddr.addr.asIPString, 57120);
					addrs.put(otherNick, addr); // already put it here
				}
		}).add;
	}
	
	makeSender {
		var routine = Routine {
			inf.do { |i|
				broadcastAddr.sendMsg(republicName, nickname);
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
		if(NetAddr.langPort != 57120) { 
			Error(
				"Can't join Republic: please try and restart SuperCollider"
				" to get langPort 57120."
			).throw
		};
	}
	
	// if name == \all, send to each item in the dict.
	// otherwise send to each of the given names
	
	prSendWithDict { |dict, names, args|
	
		if(names == \all) {
			dict.do { |recv| recv.sendMsg(*args) }
		} {
			names.asArray.do { |name|
				var recv = dict.at(name);
				if(recv.isNil) { 
					"% is currently absent.\n".postf(name)
				} {
					recv.sendMsg(*args)
				};
			}
		}
	}
	
}