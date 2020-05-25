package net.kanjitomo.ocr;

import net.kanjitomo.util.Parameters;

/**
 * Calculates OCR score by comparing target and reference matrices for a given alignment 
 * and calculating the number of common and nearby pixels.
 */
public class OCRScoreCalculator {

	private Parameters par = Parameters.getInstance();
	
	private TargetMatrix target;
	private ReferenceMatrix reference;
	
	private int blackPixels;
	private int whitePixels;
	private int[] targetHaloPixels;
	private int[] referenceHaloPixels;
	
	private int score;
	private boolean refined;
	
	/**
	 * Calculates OCR score for given alignment
	 * 
	 * @param halo If false, considers only common pixels between matrices.
	 * If true, considers also nearby pixels (halo matrices). This is slower but more accurate.
	 */
	public OCRResult calcScore(TargetMatrix target, ReferenceMatrix reference, boolean halo) {

		this.target = target;
		this.reference = reference;
		this.refined = halo;
		
		align();
		if (halo) {
			refine();
		}
		calcScore();
		
		return buildResult();
	}
	
	/**
	 * Aligns target and reference matrices. 
	 */
	private void align() {
		
		blackPixels = 0;
		for (int y = 0 ; y<32 ; y++) {
			int merged = target.matrix[y] & reference.matrix[y];
			int pixels = Integer.bitCount(merged);
			blackPixels += pixels;
		}
		
		targetHaloPixels = new int[] {target.pixels - blackPixels};
		referenceHaloPixels = new int[] {reference.pixels - blackPixels};
		whitePixels = 32*32 - blackPixels - targetHaloPixels[0] - referenceHaloPixels[0];
	}
	
	/**
	 * Aligns halo matrices
	 */
	private void refine() {
		
		int[] targetMatrix = target.matrix.clone();
		int[] referenceMatrix = reference.matrix.clone();
		
		int targetHaloRemaining = targetHaloPixels[0];
		int referenceHaloRemaining = referenceHaloPixels[0];

		targetHaloPixels = new int[Parameters.ocrHaloSize];
		referenceHaloPixels = new int[Parameters.ocrHaloSize];

		for (int i=0 ; i<Parameters.ocrHaloSize-1 ; i++) {

			int[] referenceHalo = reference.halo.get(i);
			for (int y = 0 ; y<32 ; y++) {
				int merged = targetMatrix[y] & referenceHalo[y];
				int pixels = Integer.bitCount(merged);
				targetHaloPixels[i] += pixels;
				targetHaloRemaining -= pixels;
				targetMatrix[y] |= referenceHalo[y];
			}
			
			int[] targetHalo = target.halo.get(i);
			for (int y = 0 ; y<32 ; y++) {
				int merged = referenceMatrix[y] & targetHalo[y];
				int pixels = Integer.bitCount(merged);
				referenceHaloPixels[i] += pixels;
				referenceHaloRemaining -= pixels;
				referenceMatrix[y] |= targetHalo[y];
			}
		}
		
		targetHaloPixels[Parameters.ocrHaloSize-1] = targetHaloRemaining;
		referenceHaloPixels[Parameters.ocrHaloSize-1] = referenceHaloRemaining;
	}	
	
	/**
	 * Calculates score from pixel values
	 */
	private void calcScore() {
		
		score = (int) Math.floor(par.ocrBaseScore + 
				blackPixels * par.ocrBlackPixelScore +
				whitePixels * par.ocrWhiteScore);
		
		for (int i=0 ; i<targetHaloPixels.length ; i++) {
			score += (int) Math.floor(targetHaloPixels[i] * par.ocrTargetHaloScores[i]);
		}
		
		for (int i=0 ; i<referenceHaloPixels.length ; i++) {
			score += (int) Math.floor(referenceHaloPixels[i] * par.ocrReferenceHaloScores[i]);
		}
		
		if (score > 1f) {
			score *= reference.scoreModifier;
		}
	}

	/**
	 * Builds result object from components
	 */
	private OCRResult buildResult() {
	
		OCRResult result = new OCRResult(target, reference);
		
		result.blackPixels = blackPixels;
		result.whitePixels = whitePixels;
		result.targetHaloPixels = targetHaloPixels;
		result.referenceHaloPixels = referenceHaloPixels;
		result.score = score;
		result.refinedAligment = refined;
		
		return result;
	}
}
