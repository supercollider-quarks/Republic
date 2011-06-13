
/*
	// MasterFX for republic
	// needs latest MasterFX checkout!

	
input filtering: 
	LeakDC, 
	bitcrushing, 
	finalise/Limiter,
	post everything: clip2 (0.98), 
	onepole spectrum tilt;

PAST EVERYTHING: 
	CheckBadValues on the outputs
	to avoid evil numbers killing the audio drivers.
	
	
r.s.queryAllNodes(false);
s.freeAll(true);

Ndef(\a, { PinkNoise.ar * 0.1 ! 2}).play;

*/
(
{ PinkNoise.ar ! 2 }.play;

	// add a few global filter functions to use - these are then available by name.
ProxyChain.add(
	\leakDC,  \filter -> { |in, lowDamp=0.01| LeakDC.ar(in, 1 - lowDamp) },
	\krush, \filter -> { |in, bits=16, downsamp=2| 
		var down;
		in = in.round(0.5 ** (bits - 1));
		down = Latch.ar(in, Impulse.ar(SampleRate.ir / downsamp.max(2))); 
			// below 1/2 downsamp, do xfade:
		blend(in, down, (downsamp - 1).clip(0, 1));
	},
	\limit, \filter -> { |in, drive=1, ampLimit=0.98| 
		Limiter.ar(in * drive, ampLimit);
	},
	\rollClip, \filter -> { |in, drive = 1, clip=0.99, pole=0.2|
		OnePole.ar((in * drive.lag(0.2)).clip2(clip.min(0.98).lag(0.2)), (pole.lag(0.2) ** 0.7));
	}
);
	// and specs for them fof gui control.
Spec.add(\lowDamp, [0, 1, \amp]);
Spec.add(\drive, [0.1, 10, \exp]);
Spec.add(\clip, [0.0, 0.98, \amp]);
Spec.add(\pole, [-0.95, 0.95, \lin]);

Spec.add(\bits, [16, 1]);
Spec.add(\downsamp, [1, 100, \exp]);

Spec.add(\limDrive, \ampx4);
Spec.add(\ampLimit, [0.1, 0.95, \amp]);

MasterFX.clear('adc') ;
m = MasterFX.new(server: r.myServer, 
	numChannels: 2, 
	slotNames: [\leakDC, \krush, \ampFin, \rollClip], 
	busIndex: 0);

m.pxChain.add(\leakDC).add(\rollClip);
g = m.gui;
)
