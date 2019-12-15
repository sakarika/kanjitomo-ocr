package net.kanjitomo.util;

/**
 * Are area debug images saved to disk?
 */
public enum SaveAreaImages {
	
	/** 
	 * No debug images are saved. This should be used in production code.
	 */
	OFF,
	
	/**
	 * Area debug images are generated and saved for failed tests.
	 * This will slow down execution and should not be used in production.
	 */
	FAILED,
	
	/**
	 * Area debug images are generated and saved for all tests (failed and successful). 
	 * This will slow down execution and should not be used in production.
	 */
	ALL;
	
	SaveAreaImages() {
		this.level = ordinal();
	}
	
	private final int level;
	
	/**
	 * @return true if this debug level is greater or equal than argument level
	 */
	public boolean isGE(SaveAreaImages arg) {
		if (this.level >= arg.level) {
			return true;
		} else {
			return false;
		}
	}
}