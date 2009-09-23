/*

use tab to send chat message

*/

EZRepublicGui {
	var <republic, <view, <listView, <chatViewWrite, <chatView, task, resp;
	
	*new { |parent, bounds, republic|
		^super.new.init(parent, bounds, republic)
	}
	
	init {|parent, bounds, aRepublic|
		var width;
		republic = aRepublic;
		bounds = bounds ?? { Rect(0, 500, 230, 300) };
		parent = parent ?? { this.makeWindow(bounds.width, bounds.height) };
		
		width = bounds.width - 8;
		view = CompositeView(parent, Rect(0, 0, width, bounds.height)).resize_(5);
		view.addFlowLayout;
		listView = ListView(view, Rect(0, 0, width, bounds.height * (1/3))).resize_(5);
		listView.background_(Color.clear);
		listView.hiliteColor_(Color.green(alpha:0.6));
		
		chatViewWrite = TextView(view, Rect(0, 0, width, 24)).resize_(8);
		chatView = TextView(view, Rect(0, 0, width, bounds.height * (2/3) - 28)).resize_(8);
		
		chatViewWrite.font = Font("Helvetica", 14);
		chatView.font = Font("Helvetica", 12);
		chatView.hasVerticalScroller_(true);
		
		this.startTask;
		listView.onClose = { this.stopTask; this.removeChatResponder; };
		
		chatViewWrite
			.usesTabToFocusNextView_(false)
			.enterInterpretsSelection_(false)
			.keyDownAction_({Êarg v, char;
				var string;
				if(char === Char.tab) 
				{ 
					this.sendChat(v.string.copy); 
					AppClock.sched(0.1, { v.string = "" });
				} 
			});
		
		this.startChatResponder;
		
	}
	
	updateViews {
		listView.items = republic.addrs.keys.asArray.sort;
	}
	
	sendChat { |str|
		str.postln;
		republic.send(\all, '/chat', republic.nickname, str)
	}
	
	startTask {
		task = SkipJack({ this.updateViews }, 1);
	}
	
	stopTask {
		task.stop;
	}
	
	startChatResponder {
		resp = OSCresponder(nil, '/chat', { |t, r, msg|
			var name, string;
			name = msg[1];
			string = msg[2];
			defer { 
				chatView.string = chatView.string ++ (name ++ ":" + string) ++ "\n";
			};
		}).add;
	}
	
	removeChatResponder {
		resp.remove;
	}
	
	makeWindow { |width, height|
		^Window.new(republic.republicName.asString, Rect(30, 500, width, height)).front
	}
	
	

}