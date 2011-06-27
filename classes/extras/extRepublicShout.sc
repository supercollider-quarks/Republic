+ Republic { 
	shout { |str|
		str = str ? "";
		this.send(\all, '/hist', nickname, Shout.tag + str) 
	}
}