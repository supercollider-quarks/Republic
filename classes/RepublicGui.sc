/*

use tab to send chat message

*/

EZRepublicGui {
	var <republic, <view, <nameView, <idView, <privateBtn, 
	<activeViews, <listView, <chatViewWrite, <chatView, task, resp;
	
	*new { |parent, bounds, republic, numCitizens = 12|
		^super.new.init(parent, bounds, republic, numCitizens)
	}
	
	init {|parent, bounds, aRepublic, numCitizens|
		var width, lifeComp;
		republic = aRepublic;
		bounds = bounds ?? { Rect(0, 500, 230, 600) };
		parent = parent ?? { this.makeWindow(bounds.width, bounds.height) };
		
		width = bounds.width - 8;
		view = CompositeView(parent, Rect(0, 0, width, bounds.height)).resize_(5);
		view.addFlowLayout;
		
		nameView = StaticText(view, Rect(0,0, 90, 20))			.string_("<name>");

		idView = StaticText(view, Rect(0,0, 30, 20))
			.string_("<id>");

		StaticText(view, Rect(0,0, 40, 20))
			.string_("private:");
		
		privateBtn = Button(view, Rect(0,0, 40, 20))
			.states_([["no"], ["yes"]])
			.action_({ |b| republic.private = b.value > 0 });
		
		listView = ListView(view, Rect(0, 0, width * 0.5 - 6, numCitizens * 18)).resize_(5);
		listView.background_(Color.clear);
		listView.hiliteColor_(Color.green(alpha:0.6));

		lifeComp = CompositeView(view, Rect(0, 0, width * 0.5 - 6, numCitizens * 18)).resize_(3);
		lifeComp.addFlowLayout(0@0, 1@1);
		
		activeViews = numCitizens.collect { 
			StaticText(lifeComp, Rect(0,0, width * 0.5 - 6 - 50.rand, 17))
				.background_(Color(0.1, 0.7, 0.1));
		};
		
		view.decorator.shift(0, 5);
		
		chatViewWrite = TextView(view, Rect(0, 0, width, 24)).resize_(8);
		chatView = TextView(view, Rect(0, 0, width, bounds.height - (numCitizens + 3.5 * 18))).resize_(8);
		
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
		var names = republic.addrs.keys.asArray.sort;
		var width; 
		
		nameView.string = republic.nickname.asString;
		idView.string = republic.clientID;
		privateBtn.value = republic.private.binaryValue; 
		 
		listView.items = names;
		
		activeViews.do { |v, i|
			width = (republic.presence[names[i]] ? 0 / republic.graceCount * 100);
			v.bounds_(v.bounds.width_(width));
		}
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