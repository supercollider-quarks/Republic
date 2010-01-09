
 
     /////////////////////////////////////////////////////////
    /////////////////// Joining the Republic  ///////////////
   /////////////////////////////////////////////////////////

    
    // The class Republic allows a very simple organisation of
   // cooperation in changing groups over the local network.
  // It automatically administers each member's address and server
 // More details: see SimpleRepublic and Republic helpfiles



 //________________________Step 1_______________________________
// is it installed?

\Republic.asClass.notNil; // true? should be true.

// if you have Quarks working, do this:
"Republic".include; // ... and recompile if needed.

// otherwise get the Republic folder and drop it into your system extension folder



//________________________Step 2_______________________________
// one person creates a local network, all the others join it.
// Make sure: are you in the same network?


//________________________Step 3_______________________________
// someone finds out the broadcast IP (it's the same for everyone)
// usually this works:
      unixCmd("ifconfig | grep broadcast | awk '{print $NF}'") 
// Or: "ifconfig" in Terminal and search the printout for "broadcast"


//________________________Step 4_______________________________
// for everyone the same: make a republic with the broadcast IP.
	
	r = Republic(NetAddr("192.168.178.255", 57120)).makeDefault;


//________________________Step 5_______________________________
// let's give every person a unique NICKNAME (symbol),
//         and an ID - a unique integer between (0..31).
// then join the Republic:

	r.join(<\yourName>, <1>); //<- YOUR unique id here, and YOUR name over there.


// save your file, e.g. under the name My_Republic.scd.

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



