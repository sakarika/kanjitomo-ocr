package net.kanjitomo.util;

/**
 * Are OCR debug images saved to disk?
 */
public enum SaveOCRImages {
	
	/** 
	 * No debug images are saved. This should be used in production code.
	 */
	OFF,
	
	/**
	 * OCR debug images are generated and saved for failed tests.
	 * This will slow down execution and should not be used in production.
	 */
	FAILED,
	
	/**
	 * OCR debug images are generated and saved for all tests (failed and successful). 
	 * This will slow down execution and should not be used in production.
	 */
	ALL;
	
	SaveOCRImages() {
		this.level = ordinal();
	}
	
	private final int level;
	
	/**
	 * @return true if this debug level is greater or equal than argument level
	 */
	public boolean isGE(SaveOCRImages arg) {
		if (this.level >= arg.level) {
			return true;
		} else {
			return false;
		}
	}
}