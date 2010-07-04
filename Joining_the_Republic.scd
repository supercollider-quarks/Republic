
 
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
	// 2.1 Take an existing network or one person creates a local network, all the others join it.
	// 2.2 Make sure: are you in the same network?
	
	
	
	//________________________Step 3 - CREATE A REPUBLIC _______________________
	
	r = Republic.new.makeDefault;
	r.gui; // add a display if you like.
	
	
	//________________________Step 4 - JOIN THE REPUBLIC _______________________
	// let's give every person 
	//		a unique NICKNAME (a symbol),
	//     	and an ID - a unique integer between (0..31).
	// then join the Republic:
	
	
	
		// put YOUR unique name and id here!
		// you can also leave out id and you will be assigned a random one
	
		r.join(\yourName, 0);  // <-- your unique name / id here
	
	
	
	//________________________Step 5 - TEST AND KEEP THE SETUP _______________________
	
		
		// now the different participants should appear by themselves
		// you see the members appear on the post window.
	
		r.addrs; // see whose addresses are collected already
		r.servers; // see whose servers are collected already
		
	
	// NOW save your personal copy of this file, 
	// e.g. under the name My_Republic.scd.
	






    /////////////////////////////////////////////////////////
   //////////////// Welcome to the Republic  ///////////////
  /////////////////////////////////////////////////////////


// to share your code, evaluate the following:
// (use tab to chat from chat window)

r.shareHistory;

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
g.remove;
r.leave;
);

// when you just end SC or recompile, you will leave automatically.



//////////////////////////////////////////////////////////////////////////////////////

// Blackboard:
// using text document:
Blackboard.new.start;

// not using document
// esp. linux and windows

Blackboard.new.useDocument_(false).start;

