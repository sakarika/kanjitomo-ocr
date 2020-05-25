package net.kanjitomo.ocr;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Input and output for the OCR algorithm
 */
public class OCRTask {

	/**
	 * Target sub-image around single character. OCR is run against this image.
	 * This should be black and white sharpened image.
	 */
	public final BufferedImage image;
	
	/** 
	 * Index of the target character (0 = character closest to mouse cursor) 
	 */
	public Integer charIndex = null; 
	
	/**
	 * Best results (characters that are closest match to the target image)
	 * sorted by score (best match first).
	 */
	public List<OCRResult> results;
	
	/**
	 * If true, this sub-image is from column that does not contain the original target point.
	 * This is used to restrict dictionary search to characters within single column.
	 */
	public boolean columnChanged = false;
	
	/**
	 * @param image Target sub-image around single character. OCR is run 
	 * agains this image. This should be black and white sharpened image.
	 */
	public OCRTask(BufferedImage image) {
		
		this.image = image;
	}
	
	/**
	 * Gets results as a string of characters. Best match first.
	 */
	public String getResultString() {
		
		StringBuilder resultStr = new StringBuilder();
		for (OCRResult result : results) {
			resultStr.append(result.reference.character);
		}
		return resultStr.toString();
	}
	
	/**
	 * Gets the identified character (best match).
	 */
	public Character getCharacter() {
		
		if (results.size() > 0) {
			return results.get(0).getCharacter();
		} else {
			return null;
		}
	}
}
