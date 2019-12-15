package net.kanjitomo.util;

/**
 * How much information is printed to STDOUT
 */
public enum PrintLevel {
	
	/** 
	 * No information is printed to stdout. This should be used in production code.
	 */
	OFF,
	
	/**
	 * Basic information is printed to stdout. 
	 */
	BASIC,
	
	/** 
	 * Extra debug information is printed to stdout. 
	 */
	DEBUG;
	
	PrintLevel() {
		this.level = ordinal();
	}
	
	private final int level;
	
	/**
	 * @return true if this debug level is greater or equal than argument level
	 */
	public boolean isGE(PrintLevel arg) {
		if (this.level >= arg.level) {
			return true;
		} else {
			return false;
		}
	}
}