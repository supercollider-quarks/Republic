Blackboard {

	var <>broadcastAddr;
	var <doc, <wasChanged = false, <mode = \waiting; // one of the three: \passive, \active, \waiting
	var skippy, resp, id;
	var <>useDocument = true;
	
	classvar <>backup;
	
	*new { |broadcastAddr|
		^super.newCopyArgs(broadcastAddr ?? { Republic.default.broadcastAddr })
	}
	
	start {
		var test = false;
		skippy = SkipJack({ this.idle }, 2);
		resp = 
			[
				OSCresponderNode(nil, '/blackboard', { |t, r, msg|
					if(msg[1] != id) {
						this.trySetString(msg[2].asString);
					};
					
				}).add,
				OSCresponderNode(nil, '/blackboardFree', { |t, r, msg|
					if(msg[1] != id) {
						defer { this.tryMakeWaiting };
					}
					
				}).add,
				OSCresponderNode(nil, '/blackboardTest', { |t, r, msg|
					if(msg[1] == id) {
						test = true;
					}
					
				}).add
			];
		
		id = 100000.rand;
		this.waiting;
		
		this.makeGUI;
		
		fork {
			broadcastAddr.sendMsg('/blackboardTest', id);
			2.wait; 
			if(test) { "Blackboard Broadcast is working." } { "Blackboard Broadcast FAILED. Check Address." }.postln 
		};
	}
	
	stop {
		skippy.stop;
		resp.do(_.remove);
	}
	
	makeGUI {
		var docClass = if(useDocument) { Document } { BlackboardWindow };
		doc = docClass.new("blackboard");
		doc.string = backup ? "";
		doc.onClose = { backup = doc.string; this.stop };
	
	}
	
	// private implementation
	
	idle {
		if(wasChanged.not) { 
			this.waiting;
		};
		wasChanged = false; // will be reset by keyDown
	}
	
	
	trySetString { |str|
		defer { 
			if(mode == \waiting) { this.passive };
			if(mode != \active) {
				doc !? { 
					doc.editable = true; 
					doc.string = str;
					doc.editable = false; 
				};
				wasChanged = true;
			};
		}
	}
	
	tryMakeWaiting { |str|
		if(mode != \active) {
			this.waiting;
		};
	}
	
	waiting {
		if(mode == \active) { 
			broadcastAddr.sendMsg('/blackboardFree', id); 
		};
		mode = \waiting;
		
		doc !? {
			doc.editable = true;
			doc.background = Color.white;
			doc.title = "if you like, write on blackboard";
			doc.keyDownAction = {
						wasChanged = true;
						this.active;
			};
		};
	}
	
	active {
		var func = {
				broadcastAddr.sendMsg('/blackboard', id, doc.string);
				wasChanged = true;
		};
		mode = \active;
		doc !? {
			doc.editable = true;
			doc.background = Color(0.7, 0.9, 0.6);
			doc.title = "writing on blackboard";
			doc.keyDownAction = {
				AppClock.sched(0.03, { func.value; nil });
			};
		};
		func.value;
	}
	
	passive {
		mode = \passive;
		
		doc !? {
			doc.editable = false;
			doc.background = Color.grey(0.8);
			doc.title = "blackboard";
			doc.keyDownAction = nil;
		};
	}
	
	
}

BlackboardWindow {
	var <>textView, <>window;
	*new { | title="Untitled", string="" |
		^super.new.init(title, string)
	}
	
	init { |title, string|
		window = Window.new(title, Rect(100, 100, 500, 400));
		textView = TextView(window, Rect(0, 0, 500, 400));
		window.front;
	}
	
	editable_ { |bool|
		textView.editable_(bool)
	}
	
	background_ { |color|
		textView.background_(color)
	}
	
	title_ { |string|
		window.name_(string)
	}
	
	keyDownAction_ { |func|
		textView.keyDownAction_(func)
	}
	
	string {
		^textView.string
	}
	
	string_ { |string|
		^textView.string_(string)
	}
	
	onClose_ { |func|
		window.onClose_(func)
	}
	
	onClose {
		^window.onClose
	}
	
}

