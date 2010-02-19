
Shout {
	classvar <win, <txtView, <>tag="//!!";
	classvar <>width=1250, <shouts, <codeDumpFunc; 
	
	*initClass { 
		shouts = List.new; 
		codeDumpFunc = { |str| if (str.beginsWith(Shout.tag)) { Shout(str.drop(Shout.tag.size)) } };
	}

	*makeWin { |message="Shout this!"| 
	
		win = GUI.window.new("Shout'er", Rect(20, 800, width, 80)).front;
		win.alpha_(0.7);
		win.view.background_(Color.clear);
		win.alwaysOnTop_(true);
		
		txtView = GUI.textView.new(win, win.bounds.moveTo(0,0));
		txtView.background_(Color.clear);
		txtView.font_(GUI.font.new("Monaco", 32));
		
		this.setMessage(message);
	}

		// simple versions of methods, for sc-book chapter 
//	*setMessage { |message| 
//		txtView.string_(message.asString)
//	}
	
//	*new { |message="ÁShout'er!"| 
//		shouts.add(message);
//
//		if (win.isNil or: { win.isClosed }) { 
//			this.makeWin(message); 
//		} {
//			this.setMessage(message);
//		};
//	}

	*new { |message="ÁShout'er!"| 
		var currDoc;
		shouts.add(message);

		if (win.isNil or: { win.isClosed }) { 
			currDoc = Document.current;
			Task { 
				this.makeWin(message); 
				0.1.wait;
				currDoc.front;
			}.play(AppClock);
		} {
			this.setMessage(message);
		};
	}

	*setMessage { |message| 
		var messSize, fontSize;
		messSize = message.size;
		fontSize = (1.64 * width) / max(messSize, 32);
		
		defer { 
			txtView.font_(GUI.font.new("Monaco", fontSize))
				.string_(message.asString);
		};
		this.animate;
	}

	*animate { |dt=0.2, n=12|
		var colors = [Color.red, Color.green, Color.blue]; 
		Task { 
			n.do { |i| 
				try { 
					txtView.stringColor_(colors.wrapAt(i)); 
					dt.wait 
				}
			};
			try { txtView.stringColor_(Color.red) }; // make sure we end red
		}.play(AppClock);
	}

	*add { var interp = thisProcess.interpreter; 
		interp.codeDump = interp.codeDump.addFunc(codeDumpFunc); 
	}
	*remove { var interp = thisProcess.interpreter; 
		interp.codeDump = interp.codeDump.removeFunc(codeDumpFunc); 
	}

} 
/* Tests
Shout("We should consider stopping...me fkjdfgkfjdgkjfdhkgjf")
Shout("We should consider stopping...")
Shout("ÁHey Hey Hey Na na na!");

Shout.add;
//!! does this show up
Shout.remove;
//!! does this show up

// 	for pbup setup use, add to Oscresponder(nil, '/share' ...) :
	if (str.beginsWith(Shout.tag)) { Shout(str.drop(Shout.tag.size) + "-" ++ msg[1]) }; 
	
*/
