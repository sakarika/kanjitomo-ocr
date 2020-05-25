package net.kanjitomo.ocr.test;

import java.io.File;
import java.util.List;

/**
 * Test image and it's associated tests
 */
public class TestImage {

	/**
	 * Target image for tests
	 */
	File file;
	
	/**
	 * Test definitions to be run agains target image
	 */
	List<Test> tests;
	
	public String toString() {
		
		String value = file.getName()+" ";
		
		for (Test test : tests) {
			value += test.toString()+" ";
		}
		
		return value;
	}
}
