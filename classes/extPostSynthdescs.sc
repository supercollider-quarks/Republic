
+ SynthDesc { 
	// build a postable string example event for a synthdesc 
	exampleEventString { |excludeNames = #[\out, \where]| 
		var str = "( instrument: '%'" .format(name); /*)*/ // for bracket matching
		var namesVals;
		var nextName, nextVal; 
		
		controls.do { |ctl| 
		//	if (excludeNames.includes(ctl.name.asSymbol).not) {
				if (ctl.name != '?') { 
						// add previous name and value
					if (nextName.notNil) { 
						namesVals = namesVals.add(nextName.asSymbol).add(nextVal.unbubble);
						nextName = nextVal = nil; 
						
					};
					nextName = ctl.name; 
				};
				nextVal = nextVal.add(ctl.defaultValue.round(0.0001));
		//	};
		};
		
		namesVals.pairsDo { |name, val|
			str = str ++ (", %: %".format(name, val.unbubble));
		};
		/*(*/ // for bracket matching
		str = str ++ ").play;\n";
		^str;
	}
}

+ Republic { 
	
	postExamples { |newDoc = true| 
		var title = "// REPUBLIC - all shared synthdefs";
		var allStr = title ++ "\n\n";

		var eventStrings = this.synthDescs.asArray.sort({ |a, b| a.name < b.name })
			.collect (_.exampleEventString); 		
		if (newDoc) { Document(title, allStr ++ eventStrings.join("\n")) };
		allStr.postln;
	}
}
