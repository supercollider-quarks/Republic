/* 
	not fully working yet, just a sketch
	ToDo: 
	rewrite Server:makeGui as ServerGui : JITGui 
	(then this could become ServerAllGui)
		
*/

RepublicServerGui : JITGui { 
	var <homeZone, <lines; 
//	*new { |object, numItems, parent, bounds| 
//		
//	}
	
		// these methods should be overridden in subclasses:
	setDefaults { |options|
		if (parent.isNil) {
			defPos = 10@260
		} {
			defPos = skin.margin;
		};
		minSize = 300 @ (numItems + 1 * skin.buttonHeight + 4 + 120);
	}

	accepts { |obj| ^obj.isNil ?? obj.isKindOf(Republic) }
	
	object_ { |obj| 
		// should be used once only ATM
		if (obj.isNil) {
			homeZone.children.do(_.remove);
		} { 
			obj.myServer.makeGui(homeZone);
		}
	}
	
	getState { 
		if (object.isNil) { ^[] };
		
		^object.nameList.collect { |name, i|
			var sv = object.servers[name];
			(
				name: 		sv.name, 
				running: 		sv.serverRunning, 
				avgCPU: 		sv.avgCPU.round(0.1),
				peakCPU: 		sv.peakCPU.round(0.1),
				numUGens: 	sv.numUGens,
				numSynths: 	sv.numSynths,
				numGroups: 	sv.numGroups,
				numSynthDefs: sv.numSynthDefs
			)	
		}
	}
	
	makeViews { 
		homeZone = CompositeView(zone, Rect(0,0,300, 100))
			.background_(Color.grey(0.85));
		homeZone.addFlowLayout;
		
		StaticText(zone, Rect(0,0, 300, 20))
			.font_(Font("Monaco", 9))
			.string_(
			"  name " " status" 
			"  defs " " groups" " synths" "  peak " "  avg  " 
		//	"  10.3 " "  8.8  " "   5   " "  3   " "  93"
		);
		
		lines = numItems.collect { 
			var lineZone = CompositeView(zone, Rect(0, 0,300, 24));
			lineZone.addFlowLayout(2@2, 2@2);
			Button(lineZone, Rect(0,0, 80,20))
				.states_([[""],["", Color.black, skin.onColor]]);
			StaticText(lineZone, Rect(0,0, 200, 20))
				.font_(Font("Monaco", 9))
				.string_(
					"  93" "   3   " "   5   " "  10.3 " "  8.8  " 
			);
			lineZone;
		};
		
	//	object.myServer.makeGui(parent); 
	}
	checkUpdate { 
		var newState = this.getState;
		lines.do { |l, i| 
			var svstate = newState[i];
			var str; 
			l.visible_(svstate.notNil);
			if (svstate.notNil) { 
				l.children.first.value_(svstate[\running].binaryValue);
				str = " " 
				+ svstate.numSynthDefs 	++ "    "
				+ svstate.numGroups	++ "    "
				+ svstate.numSynths	++ "     "
				+ svstate.avgCPU		++ "   "
				+ svstate.peakCPU;
				
				l.children[1].string_(str);
			};
		}
	}
}
