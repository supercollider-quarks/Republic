
+ SynthDesc { 
	
	defaultNamesVals { |excludeNames = #[\out, \where]| 
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
		
		^namesVals
	}
	
	// build a postable string example event for a synthdesc 
	exampleEventString { |excludeNames = #[\out, \where], linePerPair = true| 
		var nl = if (linePerPair, "\n", "");
		var space = if (linePerPair, "\t", " ");
		var str = "( instrument: %" .format(name.asCompileString); /*)*/ // for bracket matching
		
		this.defaultNamesVals.pairsDo { |parname, val|
			str = str ++ (",%%%: %".format(nl, space, parname.asCompileString, val.unbubble));
		};
		
		/*(*/ // for bracket matching
		str = str ++ "%).play;\n".format(nl);
		^str;
	}
	
	examplePdefString { 
		var namesVals = this.defaultNamesVals;
		var str = 
			"(\n"
		++ 	"Pdef(\\" ++ { rrand(97, 122).asAscii }.dup(5).join ++ ",\n"
		++ 	"	Pbind("
		++	"\n		'instrument', '%',\n".format(name);

		namesVals.pairsDo { |parname, val, i|
			var comma = if (i < (namesVals.lastIndex - 1), ",", "");
			str = str ++ ("		%, %%\n".format(parname.asCompileString, val.unbubble, comma));
		};
		str = str 
		++ 	"	)\n).play;\n);\n";
		
		^str	
	}
}

+ Republic { 
	
	postSynthDefs { |newDoc = true| 
		var title = "// REPUBLIC - all shared synthdefs";
		var allStr = title ++ "\n\n";

		var synthdefStrings = this.synthDescs.asArray.sort({ |a, b| a.name < b.name })
			.collect { |desc| ("(\n" ++ desc.metadata.sourceCode ++ ".share;\n);\n") }; 		
		if (newDoc) { Document(title, allStr ++ synthdefStrings.join("\n")) };
		allStr.postln;
	}
	
	postEvents { |newDoc = true, linePerPair = false| 
		var title = "// REPUBLIC - example events for all shared synthdefs";
		var allStr = title ++ "\n\n";

		var eventStrings = this.synthDescs.asArray.sort({ |a, b| a.name < b.name })
			.collect (_.exampleEventString(linePerPair));
		if (newDoc) { Document(title, allStr ++ eventStrings.join("\n")) };
		allStr.postln;
	}
	
	postPdefs { |newDoc = true| 
		var title = "// REPUBLIC - example Pdefs for all shared synthdefs";
		var allStr = title ++ "\n\n";

		var eventStrings = this.synthDescs.asArray.sort({ |a, b| a.name < b.name })
			.collect (_.examplePdefString); 		
		if (newDoc) { Document(title, allStr ++ eventStrings.join("\n")) };
		allStr.postln;
	}

}
