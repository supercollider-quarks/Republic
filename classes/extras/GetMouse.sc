GetMouseX {
	var <>minval=0, <>maxval=1, <warp=0, <lag=0.2;
	var <dt, <value;
	var synth, resp;
	
	*new { |minval=0, maxval=1, warp=0, lag=0.2, server|
		^super.newCopyArgs(minval, maxval, warp, lag).run(server)
	}
	
	run { |server|
		var cmd = '/' ++ this.mouseClass.name;
		server = server? Server.default;
		server.bind {
			synth = { SendReply.kr(
						Impulse.kr(8), 
						cmd, 
						this.mouseClass.kr(minval, maxval, warp, lag)
					)
			}.play;
			resp = OSCresponderNode(server.addr, cmd, { |t,r,msg| value = msg[3] });
			resp.add;
			CmdPeriod.add(this);
		
		}
	
	}
	
	cmdPeriod { resp.remove; CmdPeriod.remove(this) }
	
	stop { resp.remove; synth.stop; }

		
	mouseClass {
		^MouseX
	}
}

GetMouseY : GetMouseX {
	mouseClass {
		^MouseY
	}
}