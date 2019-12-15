package net.kanjitomo.ocr;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Parameters;
import net.kanjitomo.util.Util;

/**
 * Result after alignment between target image and reference character
 */
public class OCRResult {
	
	private Parameters par = Parameters.getInstance();
	
	/**
	 * Matrix that represents pixels in target image
	 */
	TargetMatrix target;
	
	/**
	 * Matrix that represents pixels in reference character
	 */
	ReferenceMatrix reference;
	
	public OCRResult(TargetMatrix target, ReferenceMatrix reference) {
		
		this.target = target;
		this.reference = reference;
	}
	
	/**
	 * Common pixels that are found in both target and reference images
	 */
	int blackPixels;
	
	/** 
	 * Pixels not in either target or reference image
	 */
	int whitePixels;
	
	/**
	 * Pixels in target but not in reference image. Indexed by halo layer.
	 */
	int[] targetHaloPixels;
	
	/**
	 * Pixels in reference but not in targe image. Indexed by halo layer.
	 */
	int[] referenceHaloPixels;
	
	/** 
	 * Aligment score calculated from weighted sum of pixel types. Higher score is better. 
	 */
	int score;
	
	/**
	 * Average score across all transformations. Higher score is better.
	 */
	float avgScore;
	
	/**
	 * If true, refined aligment was used that includes halo pixels (pink and gray).
	 * If false, only normal alignment was used.
	 */
	boolean refinedAligment = false;
	
	/**
	 * Get score for this alignment. Higher is better.
	 */
	public Integer getScore() {
		return score;
	}
	
	/**
	 * Gets average score across all alignments. Higher is better.
	 */
	public Float getAvgScore() {
		return avgScore;
	}
	
	/**
	 * Get the reference character for this aligment.
	 */
	public Character getCharacter() {
		return reference.character;
	}
	
	public String toString() {	
		return getCharacter()+"\tscore:"+score+
				"\tblack:"+blackPixels+
				"\twhite:"+whitePixels+
				"\ttargetHalo:"+Util.printArray(targetHaloPixels)+
				"\treferenceHalo:"+Util.printArray(referenceHaloPixels)+"\n"+
				//"\tbadPixels:"+badPixels+
				"\tmodifier:"+reference.scoreModifier+
				"\ttransform:"+target.transform+
				"\tfont:"+reference.fontName;
	};
	
	BufferedImage buildDebugImage() {
		
		if (refinedAligment) {
			return buildDebugImageRefined();
		} else {
			return buildDebugImageBasic();
		}
	}

	BufferedImage buildDebugImageBasic() {
		
		BufferedImage image = ImageUtil.createWhiteImage(32, 32);
		
		addLayer(image, Color.BLACK, target.matrix, reference.matrix);
		addLayer(image, par.ocrTargetHaloFirstColor, target.matrix);
		addLayer(image, par.ocrReferenceHaloFirstColor, reference.matrix);
		
		return image;
	}
	
	BufferedImage buildDebugImageRefined() {
	
		BufferedImage image = ImageUtil.createWhiteImage(32, 32);
		
		addLayer(image, Color.BLACK, target.matrix, reference.matrix);
		for (int i=1 ; i< Parameters.ocrHaloSize ; i++) {
			Color col = interpolate(par.ocrTargetHaloFirstColor, par.ocrTargetHaloLastColor, i);
			addLayer(image, col, target.matrix, reference.halo.get(i-1));
		}
		for (int i=1 ; i< Parameters.ocrHaloSize ; i++) {
			Color col = interpolate(par.ocrReferenceHaloFirstColor, par.ocrReferenceHaloLastColor, i);
			addLayer(image, col, reference.matrix, target.halo.get(i-1));
		}
		addLayer(image, par.ocrTargetHaloLastColor, target.matrix);
		addLayer(image, par.ocrReferenceHaloLastColor, reference.matrix);
		
		return image;
	}
	
	/**
	 * Paints pixels that can be found in both matrixes with color.
	 * Only overwrites white pixels.
	 */
	private void addLayer(BufferedImage image, Color color, int[] matrix1, int[] matrix2) {

		for (int y=0 ; y<32 ; y++) {
			BigInteger paintPixels = BigInteger.valueOf(matrix1[y] & matrix2[y]);
			for (int x=0 ; x<32 ; x++) {
				if (paintPixels.testBit(31-x) && image.getRGB(x, y) == Color.WHITE.getRGB()) {
					image.setRGB(x, y, color.getRGB());
				}
			}
		}
	}
	
	/**
	 * Paints pixels that can be found in the matrix.
	 * Only overwrites white pixels.
	 */
	private void addLayer(BufferedImage image, Color color, int[] matrix) {

		for (int y=0 ; y<32 ; y++) {
			BigInteger paintPixels = BigInteger.valueOf(matrix[y]);
			for (int x=0 ; x<32 ; x++) {
				if (paintPixels.testBit(31-x) && image.getRGB(x, y) == Color.WHITE.getRGB()) {
					image.setRGB(x, y, color.getRGB());
				}
			}
		}
	}
	
	/**
	 * Interpolates halo colors
	 */
	private Color interpolate(Color col1, Color col2, int layer) {

		if (layer == 1) {
			return col1;
		}
		
		if (layer >= Parameters.ocrHaloSize) {
			return col2;
		}
		
		int red = interpolate(col1.getRed(), col2.getRed(), layer);
		int green = interpolate(col1.getGreen(), col2.getGreen(), layer);
		int blue = interpolate(col1.getBlue(), col2.getBlue(), layer);
		
		return new Color(red, green, blue);
	}
	
	/**
	 * Interpolates halo colors (single channel)
	 */
	private int interpolate(int rgb1, int rgb2, int layer) {
		
		int delta = rgb2 - rgb1;
		float ratio = 1.0f * (layer-1) / (Parameters.ocrHaloSize-1);
		return rgb1 + Math.round(ratio*delta);
	}
}
