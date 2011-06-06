Republic - Kommunikation zwischen Personen und Rechnern

* 	Spatialisation - Verteilung von Klaengen 
* 	Code distribution - jede/r kann alles lesen, 
	was ausgefuehrt wurde andere aus Geschriebene 
* 	Chatting / Shouting - laute Messages schicken
* 	History - der Verlauf einer Session als Code 
	rekonstruierbar
* 	Echtzeit-Konversation, 
	Kollaboration, -Komposition, Improvisation


Ueberblick hier - Streams of sound events - 
Sound, Funktionen, Synth, SynthDef, Event, Pattern

r.postExamples
s.queryAllNodes
s.freeAll(true);

Sounds: 
	Ndef etc 				->	sound
	Function 				-> 	sound 
	SynthDef  -> Synth 	-> 	sound
	Event    (	-> Synth) 	-> 	sound
	Pattern  (-> Event) 	-> 	sound


{ SinOsc.ar }.play;

	// func.play erzeugt einen Synth
x = { |freq = 440, amp = 0.1| SinOsc.ar(freq) * amp }.play;

x.set(\freq, exprand(300, 3000));
x.free

	// envelopes - Verlaeufe von Zahlen in der Zeit, 
	// sehr oft fuer amplitude - lautstaerkeverlauf
	// --- Ende wenn Env vorbei ist 
Env.openHelpFile

Env(levels, times);

Env([0, 1, 0], [0.1, 0.9]).plot;
Env([0, 1, 0], [0.1, 0.9]).test;
Env([0, 1, 0], [0.1, 0.9], curve: \sine).plot;

etc etc etc

	// in einer Function:
{ SinOsc.ar * EnvGen.kr(Env.perc) }.play;

	// wenn der Sound bei Env-ende ganz aufhoeren soll: doneAction: 2!
{ SinOsc.ar * EnvGen.kr(Env.perc, doneAction: 2) }.play;


// SynthDef, Synth:
Function erzeugt eigentlich intern eine SynthDef und einen Synth 
- wir machens jetzt selber! 

// Der Sound von oben als SynthDef
(
SynthDef(\hopsa, { 
	 SinOsc.ar * EnvGen.kr(Env.perc, doneAction: 2) 
}).add;
)
Synth(\hopsa);
// -> spielt, aber unhoerbar! 
// -> SynthDef braucht einen Out UGen, damits auf die outputs gespielt wird.
(
SynthDef(\hopsa, { 
	var sound = SinOsc.ar * EnvGen.kr(Env.perc, doneAction: 2);
	Out.ar(0, sound);
}).add;
)

Synth(\hopsa);	// OK!

/// Speziell bei Republic: SynthDef(\x, { ... }).share; 
// schickt die neue SynthDef an alle in der Republic!




	// ARGUMENTE: freq, amp, out, pan, u.v.a.m. 
(
SynthDef(\hopsa, { arg out = 0, freq = 440, amp = 0.1;
	var sound = SinOsc.ar(freq); 
	var envgen = EnvGen.kr(Env.perc, doneAction: 2);
	Out.ar(out, sound * (envgen * amp));
}).share;
)
Synth(\hopsa, [\out, 0]);
Synth(\hopsa, [\out, 1]);

Synth(\hopsa, [\freq, 600]);
Synth(\hopsa, [\freq, rrand(400, 1200)]);

Synth(\hopsa, [\amp, 1]);
Synth(\hopsa, [\amp, 0.1]);

(	// ganz oft mit pan - stereo-position: 

SynthDef(\hopsa, { arg out = 0, freq = 440, amp = 0.1, pan = 0;
	var sound = SinOsc.ar(freq); 
	var envgen = EnvGen.kr(Env.perc, doneAction: 2);
	var pannedSound = Pan2.ar(
		sound * (envgen * amp), 
		pan
	);
	Out.ar(out, pannedSound);
}).share;
)

Synth(\hopsa, [\freq, rrand(400, 1200), \pan, 1.0.rand2].postcs);


(	// die gleiche envelope auch fuer tonhoehe verwenden: 
SynthDef(\hopsa, { arg out = 0, freq = 440, amp = 0.1, pan = 0;
	var envgen = EnvGen.kr(Env.perc, doneAction: 2);
	var freq2 = freq * (envgen + 0.5);
	var sound = SinOsc.ar(freq2); 
	
	Out.ar(out, 
		Pan2.ar(sound * (envgen * amp), amp)
	);
}).share;
)

Synth(\hopsa, [\freq, 600]);
Synth(\hopsa, [\freq, rrand(400, 1200)]);




(	// klingende Dauer steuern: 
SynthDef(\hopsa, { arg out = 0, freq = 440, amp = 0.1, pan = 0, sustain = 1;
	var envgen = EnvGen.kr(Env.perc(0.01, sustain), doneAction: 2);
	var freq2 = freq * (envgen + 0.5);
	var sound = SinOsc.ar(freq2); 
	
	Out.ar(out, 
		Pan2.ar(sound * (envgen * amp), amp)
	);
}).share;
)

Synth(\hopsa, [\freq, 600, \sustain, 0.1]);
Synth(\hopsa, [\freq, 600, \sustain, exprand(0.01, 1.0)].postln);
Synth(\hopsa, [\freq, 600, \sustain, 10].postln);



Events: Paare key, value, key, value ... 
	definieren Eigenschaften eines Events 

();	// leerer event
().play;	// lauter default-werte: synthdef = instrument, amp, freq, sustain

(instrument: \hopsa).play;	// instrument setzen

(freq: 1200).play;

(freq: 1200, instrument: \hopsa).play;
(freq: 1200, instrument: \hopsa, sustain: 0.1).play;
(freq: 1200, instrument: \hopsa, sustain: 0.1, amp: 0.5).play;

	// republic
(freq: 1200, instrument: \hopsa, server: r.servers.choose).play;


Events in einer Task zusammensetzen: 
(
Tdef(\hopsa, { 
	30.do { 
		(	
			freq: 1200, 
			instrument: \hopsa, 
			sustain: 0.1, 
			amp: 0.1
		).play;
		0.25.wait;
	};
}).play;
)


(
Tdef(\hopsa, { 
	30.do { 
		(freq: exprand(300, 1200), // zufaellige freq
		instrument: \hopsa, 
		sustain: 0.1, 
		amp: 0.1).play;
		0.25.wait;
	};
}).play;
)

(
Tdef(\hopsa, { 
	100.do { |count| 
		// count.postln; 
		var offset = (count % 7) + (count % 11);
		(freq: (60 * (10 + (offset * 3))).postln, 
		instrument: \hopsa, 
		server: r.servers.choose,
		sustain: exprand(0.1, 0.1), // zufaellige dauer
		amp: 0.03).play;
		0.125.wait;
	};
}).play;
)

	// zusammenhaenge: kurze sounds lauter
(
Tdef(\hopsa, { 
	30.do { 
		var sus = exprand(0.1, 10);
		(freq: exprand(300, 1200), 
		instrument: \hopsa, 
		sustain: sus, // zufaellige dauer
		amp: 0.1 * ( sus ** -0.5)).postln.play;
		0.125.wait;
	};
}).play;
)

(
SynthDef(\example, { arg out = 0, freq = 440, amp = 0.1, sustain = 0.8, pan = 0;
	var sound = SinOsc.ar(freq);
	var envgen = EnvGen.ar(Env.perc(0.01, sustain, amp));
	
	Out.ar(out, 
		Pan2.ar( sound * envgen, pan)
	)
}).share;
)

(instrument: \example).play;
(instrument: \example, freq: 2000).play;

(instrument: \example, freq: 2000, sustain: 0.1).play;

(instrument: \example, freq: 2000, sustain: 0.1, amp: 0.5).play;
(instrument: \example, freq: 2000, sustain: 0.01, amp: 0.5).play;
(instrument: \example, freq: 2000, sustain: 0.0, amp: 0.5).play;




Patterns: 
	value patterns
	bauplaene fuer Streams = Ketten von Werten; 
	
a = Pseq([0, 2, 3]);
b = a.asStream;	// das passiert intern 
b.next;
b.next;
b.next;
b.next;		// nil wenn aus - stream endet

c = Prand([0.5, 0.25, 0.125], 6);
d = c.asStream;
d.next;

Event patterns: 
bauen Events zusammen aus "key, value"-paaren, 
die dann gespielt werden koennen:
(
Pbind(
	\note, Pseq([0, 2, 3], 4), 
	\dur, Prand([0.25, 0.125], 12)
).trace.play;
)


Help - tutorial: 
A Practical Guide to Patterns


x = Pbind.new.play;	// lauter default-werte
x.stop;
r.servers.choose.freeAll(true);

x = Pbind.new.trace.play;

Pdef(\a, Pbind.new).play;
(
Pdef(\a, 
	Pbind(
		\freq, 300		// key, value
	)
).play;
)

(
Pdef(\a, 
	Pbind(
		\isntrument, \hopsa,		// fix 
		\freq, 300		// key, value
	)
).play;
)

(
Pdef(\a, 
	Pbind(
		\instrument, \hopsa,
		\dur, 0.1,
		\freq, 300	
	)
).play;
)



(
Pdef(\a, 
	Pbind(
		\instrument, \hopsa,
		\freq, exprand(300, 1200), // entscheidet nur einmal!
		\dur, 0.1
		)
).play;
)



(
Pdef(\a, 
	Pbind(
		\instrument, \hopsa, 
				// key, pattern
		\freq, Pfunc({ exprand(300, 1200) }),		\dur, 0.1
		)
).play;
)

(
Pdef(\a, 
	Pbind(
		\instrument, \hopsa,
				// key, pattern
		\freq, Pwhite(300, 1200),	// ein random range value pattern
		\dur, Pbrown(0.05, 0.25, 0.02), 
		\sustain, 0.01, 
		\server, r.s, 
		\where, \adc
		)
).stop.play;
)

	// change synthdef while playing!
(
SynthDef(\hopsa, { arg out = 0, freq = 440, amp = 0.1, pan = 0, sustain = 1;
	var envgen = EnvGen.kr(Env.perc(0.01, sustain), doneAction: 2);
	var freq2 = freq * (envgen + 0.5);
	var sound = Pulse.ar(freq2, 0.2); 
	
	Out.ar(out, 
		Pan2.ar(sound * (envgen * amp), amp)
	);
}).share;
)
