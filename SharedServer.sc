
SharedServer : Server { 
	var <myGroup, <>numClients = 8, <bufs; 

	asTarget { ^myGroup }
	asGroup { ^myGroup }
	asNodeID { ^myGroup.nodeID }

	initTree {
		nodeAllocator = NodeIDAllocator(clientID, options.initialNodeID);
		this.bind { 
			"initTree % : myGroup should come back. 
			Others have to call initTree as well, e.g. by hitting Cmd-Period.\n".postf(name);
			this.sendMsg("/g_new", 1, 0, 0);
			this.sendMsg("/g_new", myGroup.nodeID, 1, 1);
		};
		tree.value(this);
		ServerTree.run(this);
	}
	
	init { arg argName, argAddr, argOptions, argClientID;
		super.init(argName, argAddr, argOptions, argClientID); 
		myGroup = Group.basicNew(this, 100 + clientID);
	}
		// only do this once right after init!
	reserveOtherBusses { 
		var offsetAudio = options.numOutputBusChannels + options.numInputBusChannels;
		var numAudio = options.numAudioBusChannels - offsetAudio;
		var numCtl = options.numControlBusChannels;
		
		var numAudioEach = numAudio div: numClients; 
		var numCtlEach = numCtl div: numClients; 
		
		numClients.do { |id|
			if (id != clientID) { 
				audioBusAllocator.reserve(offsetAudio + (numAudioEach * id), numAudioEach);
				controlBusAllocator.reserve((numCtlEach * id), numCtlEach);
			};
		};
	} 

	reserveOtherBufnums { 
		var numBufsEach = options.numBuffers div: numClients; 
		
		numClients.do { |id|
			if (id != clientID) { 
				bufferAllocator.reserve((numBufsEach * id), numBufsEach);
			};
		};
	} 
	
	myOuts { 
		var numEach = options.numOutputBusChannels div: numClients;
		^(0 .. (numEach - 1)) + (numEach * clientID);
	}
	
	freeAll { |hardFree = false| 
		"SharedServer:freeAll...".postln; 
		if (hardFree) { 
			this.sendMsg("/g_freeAll", 0);
			this.sendMsg("/clearSched");
			this.initTree;
		} { 
			myGroup.freeAll;
		}
	}
	
	getBufs { |action| 
		var dur = (options.numBuffers * options.blockSize / sampleRate * 5).postln;
		var newBufs = Array(32);
		var resp = OSCresponder(nil, 'bufscan', { |time, resp, msg| 
			var bufnum, frames, chans, srate; 
			#bufnum, frames, chans, srate = msg.keep(-4); 
			if (chans > 0) { newBufs = newBufs.add(Buffer(this, frames, chans, srate, bufnum: bufnum).postln) };
		}).add;

		{	var bufnum = Line.kr(0, options.numBuffers, dur, doneAction: 2).round(1);
			var trig = HPZ1.kr(bufnum);
			SendReply.kr(trig, 'bufscan', 
				[bufnum, BufFrames.kr(bufnum), BufChannels.kr(bufnum), BufSampleRate.kr(bufnum)]);
		}.play(this);
		
		fork { 
			(dur + 0.5).wait; 
			resp.remove; 
			bufs = newBufs;
			(action ? { |bufs| "\t SharedServer - found these buffers: ".postln; bufs.printAll }).value(bufs);
		}
	}
}

+ Nil { 
	asTarget { ^Server.default.asTarget }
}
