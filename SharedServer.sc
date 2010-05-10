
SharedServer : Server { 
	var <myGroup, <>numClients = 8; 

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
		// only do this after init!
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
			} { 
			
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
}

+ Nil { 
	asTarget { ^Server.default.asTarget }
}
