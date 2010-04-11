
 
     /////////////////////////////////////////////////////////
    /////////////////// Joining the Republic  ///////////////
   /////////////////////////////////////////////////////////

    
    // The class Republic allows a very simple organisation of
   // cooperation in changing groups over the local network.
  // It automatically administers each member's address and server
 // More details: see SimpleRepublic and Republic helpfiles



 //________________________Step 1 - INSTALLING ___________________________
// is it installed already?

\Republic.asClass.notNil; // true if installed already.

// if you have Quarks working, do this:
"Republic".include; // ... and recompile if needed.

// if not, get the Republic folder and drop it into your system extensions folder

unixCmd("open" + quote(Platform.userExtensionDir))


\Republic.asClass.notNil; // should be true now! 


//________________________Step 2 - AGREE ON A COMMON NETWORK________________
// two options: 
// * Take an existing network
// * one person creates a local network, all the others join it.

// * Make sure: are you in the same network?


//________________________Step 3 - CONFIGURE/ENABLE BROADCASTING ____________
// someone finds out the broadcast IP (it's the same for everyone)

		// usually the first one is 192.168.178.255, or 192.168.0.255 - use this
	Republic.getBroadcastIPs.postcs;

		// do it at a lower level if necessary 
      unixCmd("ifconfig | grep broadcast | awk '{print $NF}'") 
// on linux
      unixCmdGetStdOut("ifconfig | grep Bcast | awk 'BEGIN {FS = \"[ :]+\"}{print $6}'");

// Or do "ifconfig" in Terminal and search the printout for "broadcast" or "Bcast"

		// enable broadcasting in SC3
	NetAddr.broadcastFlag = true;


//________________________Step 4 - CREATE A REPUBLIC _______________________
// for everyone the same: make a republic with the broadcast IP found in step 3

// everyone on OSX, this will probably work fine:
r = Republic(NetAddr("192.168.178.255", 57120)).makeDefault;

// on Linux with SwingOSC, the language port sometimes jumps, so we use:
Republic.fixedLangPort = false;
r = Republic(NetAddrMP("192.168.1.255").ports_( (57120..57124) ) ).makeDefault;
	
		// optional: you can see other members' names and IDs 
		// on a GUI, so you can choose a unique name. 
	EZRepublicGui(republic: r);

//________________________Step 5 - JOIN THE REPUBLIC _______________________
// let's give every person 
//		a unique NICKNAME (a symbol),
//     	and an ID - a unique integer between (0..31).
// then join the Republic:

	// remove <> signs to make this line legal code,
	// and put YOUR unique name and id here!
	// you can also leave out id and you will be assigned a random one

	r.join( <\yourName>, <1> ); 


///// NOW save your personal copy of this file, e.g. under the name My_Republic.scd.

	// now the different participants should appear by themselves
	// you see the members appear on the post window.

	r.addrs; // see whose addresses are collected already
	r.servers; // see whose servers are collected already



    /////////////////////////////////////////////////////////
   //////////////// Welcome to the Republic  ///////////////
  /////////////////////////////////////////////////////////



// Blackboard:
// using text document:
Blackboard.new.start;

// not using document
// esp. linux and windows

Blackboard.new.useDocument_(false).start;

// to share your code, evaluate the following:
// (use tab to chat from chat window)

(
	g = OSCresponder(nil, '/hist', {|t,r,msg| 
		History.enter(msg[2].asString, msg[1]) 
	}).add; 	
	
	History.start;
	History.makeWin;
	History.forwardFunc = { |code|
		r.send(\all, '/hist', r.nickname, code) 
	};
	History.localOff;
	EZRepublicGui(republic: r);
);



// what to do now?

// e.g. share a SynthDef with all participants
(
SynthDef(\xxy, {|out, sustain = 1, freq = 440|
	var env = Line.kr(0.1, 0, sustain, doneAction: 2);
	var son = SinOsc.ar(freq * [1, 1.2, 1.5, 1.7]).sum;
	Out.ar(out, son * env);
}).share;
)


// and send a synth to everyone:
(server: r.s, instrument: \xxy, freq: exprand(300, 1000)).play;

// or to a random member:
(server: r.servers.choose, instrument: \xxy, freq: exprand(300, 1000)).play;

// or to a specific member (replace \XXX with someone's nickname)
(server: r.s, where: \XXX, instrument: \xxy, freq: exprand(300, 1000)).play;

(server: r.s, instrument: \xxy, where: \XXX, freq: exprand(300, 1000)).play;

(
Pdef(\x,
	Pbind(
		\instrument, \xxy,
		\server, r.s,
		\where, Prand([\XXX], 8), // 
		\dur, 1,
		\legato, 0.3, 
		\freq, Pwhite().linexp(0, 1, 500, 2000)
	).trace
).play;
)

// For documentation of the session in the end:
History.document;


// For quitting republic:
(
History.stop;
g.remove;
r.leave;
);

// when you just end SC or recompile, you will leave automatically.



