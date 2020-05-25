package net.kanjitomo.ocr;

import java.awt.Rectangle;
import java.io.Serializable;

/**
 * Reference character component. Roughly equilevant to radical but automatically generated
 * and might be different from official radicals. Pixel groups that don't touch each other
 * form different components but might be divided further if large.
 * 
 * Components are used in third OCR stage to fine-tune results.
 */
public class Component implements Serializable {

	private static final long serialVersionUID = 2L;
	
	/**
	 * Location of the component inside reference character
	 */
	Rectangle bounds;
	
	/**
	 * Component's pixels. Matrix might also contain pixels that belong to other components 
	 * outside bounds.
	 */
	int[] matrix;
	
	/**
	 * Number of pixels in this component
	 */
	int pixels;
}
