	// add a group of names to Republic r:
r.addGroup(\A, [\b, \c, \d]);
r.groups;
r.replaceGroupNames([\adc, \a]);

	// on sending, the group is replaced with the real names.
	// could remove Duplicates - maybe later.
r.send([\adc, \a], \chat, \adc, \test);
r.removeGroup(\a, [\b, \c, \d]);

