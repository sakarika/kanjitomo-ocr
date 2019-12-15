package net.kanjitomo.ocr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.MatrixUtil;
import net.kanjitomo.util.Parameters;

/**
 * Compares character components (radicals) against target bitmap. Iterates through
 * different component transformations and selects best matches.  
 */
public class OCRAlignComponents {

	private Parameters par = Parameters.getInstance();
	private OCRScoreCalculator calculator = new OCRScoreCalculator();
	
	/** Extra debug images are saved for this character */
	private Character debugCharacter = null;
	
	/**
	 * Runs component alignment for each result.
	 *
	 * @return Improved results in descending score order.
	 */
	public List<OCRResult> run(List<OCRResult> results) throws Exception {
	
		long started = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			System.out.print("Component alignment ");
		}
		
		List<OCRResult> newResults = new ArrayList<>();
		for (OCRResult result : results) {
			newResults.add(alignComponents(result));
		}
		
		Collections.sort(newResults, new Comparator<OCRResult>() {
			// TODO define comparator in OCRResult object and re-use
			@Override
			public int compare(OCRResult o1, OCRResult o2) {
				Integer score1 = o1.score;
				Integer score2 = o2.score;
				return -1*score1.compareTo(score2);
			}
		});
		
		long done = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			System.out.println((done - started)+" ms");
		}
		
		return newResults;
	}
	
	/**
	 * Improve result by applying transformations to reference character components.
	 */
	public OCRResult alignComponents(OCRResult result) throws Exception {
		
		int maxDelta = 1;
		int maxStretch = 4;
		
		// current best score represents default (no-op) transformation
		float bestScore = result.score;
		ReferenceMatrix reference = buildNewReferenceMatrix(result.reference);
		
		// find best translation for each component. this doesn't guarantee globally optimal solution 
		// but is close and it's not possible to consider all combinations.
		// TODO try also reverse order?
		for (int i=0 ; i<result.reference.components.size() ; i++) {

			Transformation bestTransformation = reference.transformations.get(i);

			// generate transformations
			for (int deltaX=-maxDelta ; deltaX<=maxDelta ; deltaX++) {
				for (int deltaY=-maxDelta ; deltaY<=maxDelta ; deltaY++) {
					for (int stretchX=0 ; stretchX<=maxStretch ; stretchX++) {
						for (int stretchY=0 ; stretchY<=maxStretch ; stretchY++) {
							
							// skip combined strech
							if (stretchX != 0 && stretchY != 0) {
								continue; // TODO implement in MatrixUtil
							}
							
							// run alignment with generated transformation
							Transformation transformation = new Transformation(deltaX, deltaY, stretchX, stretchY);
							reference.transformations.set(i, transformation);
							OCRResult tempResult = align(result.target, reference);
							
							// check if new result is better than old
							if (tempResult.score > bestScore) {
								bestScore = tempResult.score;
								bestTransformation = transformation;
							}
							
							// debug if test character
							if (debugCharacter != null && reference.character == debugCharacter) {
								writeDebugImage(tempResult, transformation);
							}
						}
					}
				}
			}
			
			// restore best transformation for this component
			reference.transformations.set(i, bestTransformation);
		}
		
		// restore best result
		return align(result.target, reference);
	}
	
	/**
	 * Creates a new reference matrix that includes default (no-op) transformations
	 */
	private ReferenceMatrix buildNewReferenceMatrix(ReferenceMatrix reference) {
		
		ReferenceMatrix newReference = reference.clone();
		newReference.transformations = new ArrayList<>();
		for (int i=0 ; i<reference.components.size() ; i++) {
			newReference.transformations.add(new Transformation());
		}
		
		return newReference;
	}
	
	/**
	 * Aligns target and reference using component tranformations
	 */
	private OCRResult align(TargetMatrix target, ReferenceMatrix reference) {
		
		// build new matrix by applying transformations to each component
		reference.matrix = new int[32];
		for (int i=0 ; i<reference.components.size() ; i++) {
			Component component = reference.components.get(i);
			Transformation transform = reference.transformations.get(i);
			applyTransformation(reference, component, transform);
		}
		
		// halo and pixel count can change after translations
		reference.halo = MatrixUtil.buildMatrixHalo(reference.matrix, Parameters.ocrHaloSize-1);
		reference.pixels = MatrixUtil.countBits(reference.matrix);
		
		// calculate score for new matrix
		return calculator.calcScore(target, reference, true);
	}
	
	/**
	 * Updates reference matrix by applying transformation to component
	 */
	private void applyTransformation(ReferenceMatrix reference, Component component, Transformation transform) {
		
		if (transform.horizontalStretch == 0 && transform.verticalStretch == 0) {
			MatrixUtil.copyBits(component.matrix, reference.matrix, component.bounds,
					transform.horizontalTranslate, transform.verticalTranslate, false);
		} else {
			int[] stretchedMatrix = new int[32];
			Rectangle newBounds = MatrixUtil.stretchBits(component.matrix, stretchedMatrix,
					component.bounds, transform.horizontalStretch, transform.verticalStretch);
			MatrixUtil.copyBits(stretchedMatrix, reference.matrix, newBounds,
					transform.horizontalTranslate, transform.verticalTranslate, false);
		}
	}
	
	private void writeDebugImage(OCRResult result, Transformation transform) throws Exception {
		
		String transformStr = transform.horizontalTranslate+","+transform.verticalTranslate;
		
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".step3."+result.getCharacter()+"."+
				result.getScore()+".("+transformStr+").png");
		BufferedImage scaledImage = ImageUtil.buildScaledImage(result.buildDebugImage(), par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}
}
