package net.kanjitomo.ocr;

import java.util.ArrayList;

/**
 * Matrix that represents a preloaded reference character. 
 * All common Japanese characters have their own matrix.
 */
public class ReferenceMatrix implements java.io.Serializable {

	private static final long serialVersionUID = 3L;

	/** Character represented by this matrix */
	char character;
	
	/** Pixels of the reference image. 32x32 bitmap */
	int[] matrix;
	
	/** Number of pixels (set bits) in the matrix */
	int pixels;
	
	/**
	 * Pixels around the target image (32x32 bitmaps). First matrix (index 0) represents pixels
	 * that are off by one compared to reference image (neighbour pixels), further
	 * levels increase the distance. Last level includes all the remaining pixels.
	 * Number of halo levels is determined by Parameters.ocrHaloSize.
	 */
	ArrayList<int[]> halo;
	
	/**
	 * Some characters are given a OCR score boost based on rarity 
	 */
	float scoreModifier = 1f;
	
	/**
	 * Font name used to generate the matrix
	 */
	String fontName;
	
	/**
	 * List of individual character components (radicals)
	 */
	ArrayList<Component> components;
	
	/**
	 * Transformations applied to individual components. Same order as components list. 
	 */
	ArrayList<Transformation> transformations;
	
	/**
	 * Creates a copy of this object
	 */
	public ReferenceMatrix clone() {
		
		ReferenceMatrix newReference = new ReferenceMatrix();
		
		newReference.character = character;
		newReference.components = components;
		newReference.fontName = fontName;
		newReference.halo = halo;
		newReference.matrix = matrix;
		newReference.pixels = pixels;
		newReference.scoreModifier = scoreModifier;
		
		// transformations will change so create a new list instead of referencing old
		if (transformations != null) {
			newReference.transformations = new ArrayList<>();
			newReference.transformations.addAll(transformations);
		}
		
		return newReference;
	}
}
