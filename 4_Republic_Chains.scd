//!! (3) Markov Chains


(instrument: \nix, addAction: \t).play
(
q = q ?? ();
q[\chaindt] =  { 0.2 };
q[\whonext] = { r.nameList.choose };
q[\chainfunc] = { |countDown, args|
	var freq = args[0] ? 5000;
	freq = freq * rrand(0.98, 1.01);
	(instrument: \xxy, freq: freq * [1, 1.2, 1.5], sustain: 0.03).play;
};


OSCresponder(nil, '/chain', { |t, resp, msg|
			var countDown = min(msg[1], 50);
			var howMany = msg[2];
			// "countdown: % times.\n".postf(countDown);
			
			if(countDown > 0) { 
				q[\chainfunc].value(countDown, msg[3..]); 
			
			fork { 
				q[\chaindt].value.wait;
				
					min(howMany, 2).do {
						r.addrs[q[\whonext].value].sendMsg('/chain', countDown - 1, 1, *msg[3..])
					}
				};
			};		
}).add;
);

	// test playing
q[\chainfunc].value(12, [500]);

r.addrs.choose.sendMsg('/chain', 5, 1, 500);


	// emergency stop:
OSCresponder(nil, '/chain', { }).add;


	// change what the chainfunc does here:
(
q[\chainfunc] = { |countDown, args|
	var freq = args[0] ? 5000;
	var sndef = args[1] ? \xxy;
	freq = freq * rrand(0.98, 1.02);
	(instrument: sndef, freq: freq * [1, 1.2, 1.5], sustain: 0.01).play;
};
);

	// change where the chainfunc should go 
q[\whonext] = { r.nameList.choose };

	// how fast to send to the next address:
q[\chaindt] = { exprand(0.02, 0.2) };

// test:
q[\chainfunc].value(12, [9000]);

// start a new one chain:
r.addrs.choose.sendMsg('/chain', 12, 1, 2000, [\xxy, \zzz].choose);


