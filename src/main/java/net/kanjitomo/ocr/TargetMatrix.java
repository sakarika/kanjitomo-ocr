package net.kanjitomo.ocr;

import java.util.List;

/**
 * Matrix that represents pixels in target image around single character, possibly after 
 * transformed by streching.
 */
public class TargetMatrix {
	
	/** Pixels of the target image. 32x32 bitmap */
	int[] matrix;
	
	/** Number of pixels (set bits) in the matrix */
	int pixels;
	
	/**
	 * Pixels around the target image (32x32 bitmaps). First matrix (index 0) represents pixels
	 * that are off by one compared to reference image (neighbour pixels), further
	 * levels increase the distance. Last level includes all the remaining pixels.
	 * Number of halo levels is determined by Parameters.ocrHaloSize.
	 */
	List<int[]> halo;
	
	/** Character index in source image (0 = character closest to mouse cursor) */
	Integer charIndex = null;
	
	/** Transformation used to modify the bitmap from original image */
	Transformation transform;
}
