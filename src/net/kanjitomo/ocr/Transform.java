package net.kanjitomo.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.MatrixUtil;
import net.kanjitomo.util.Parameters;

/**
 * Applies stretch and offset transformations to target image. Caches intermediate 
 * results for fast re-use.
 */
public class Transform {

	private Parameters par = Parameters.getInstance();
	private OCRTask task;
	
	/**
	 * Matrix representations of stretched target image with various stretch amounts.
	 */
	private Map<Transformation, int[]> stretchedMatrices = new HashMap<>();
	
	/**
	 * Image fitted to target size and sharpened
	 */
	private BufferedImage image;
	
	public Transform(OCRTask task) {
		
		this.task = task;
		
		// resize the image to target size 
		BufferedImage resizedImage = ImageUtil.stretchCheckRatio(task.image, par.targetSize, par.targetSize); 

		// image has already been sharpened once during area detection but resize might bring
		// back gray edges, so sharpen again
		image = ImageUtil.sharpenImage(resizedImage, par);
		
	}
	
	/**
	 * Iterates through translate and stretch combinations and creates transformed images.
	 * Increasing argument values might produce better aligment at the cost of
	 * execution time.
	 * 
	 * @param maxTranslate Maximum number of pixels translated (up/down/left/right)
	 * @param maxStretch Maximum number of pixels image is scaled
	 * @param maxSteps Maximum number of translate and stretch steps allowed in total 
	 */
	public List<TargetMatrix> run(int maxTranslate, int maxStretch, int maxSteps) throws Exception {
		
		List<TargetMatrix> targets = new ArrayList<TargetMatrix>(); 
		
		for (int ht = -maxTranslate ; ht <= maxTranslate ; ht++) {
			for (int vt = -maxTranslate ; vt <= maxTranslate ; vt++) {
				for (int hs = -maxStretch ; hs <= maxStretch ; hs++) {
					for (int vs = -maxStretch ; vs <= maxStretch ; vs++) {
						if (Math.abs(ht) + Math.abs(vt) + Math.abs(hs) + Math.abs(vs) > maxSteps) {
							continue;
						}
						if (Math.ceil(hs/2.0f) + Math.abs(ht) > (32 - par.targetSize)/2) {
							continue;
						}
						if (Math.ceil(vs/2.0f) + Math.abs(vt) > (32 - par.targetSize)/2) {
							continue;
						}
						Transformation parameters = new Transformation(ht, vt, hs, vs);
						targets.add(transform(image, parameters));
					}
				}
			}
		}
		
		return targets;
	}
	
	private boolean writeDebugImages = false;
	
	private TargetMatrix defaultTarget = null;
	
	/**
	 * Applies tranformations to the source image. Builds matrix representation.
	 */
	private TargetMatrix transform(BufferedImage image, Transformation parameters) 
			throws Exception {
		
		TargetMatrix target = new TargetMatrix();
		target.matrix = buildMatrix(image, parameters);
		target.halo = MatrixUtil.buildMatrixHalo(target.matrix, Parameters.ocrHaloSize-1);
		target.pixels = MatrixUtil.countBits(target.matrix);
		target.charIndex = task.charIndex;
		target.transform = parameters;
		
		if (writeDebugImages) {
			writeDebugImage(target, parameters);
		}
		
		if (defaultTarget == null && parameters.contains(0, 0, 0, 0)) {
			defaultTarget = target;
		}
		
		return target;
	}
	
	/**
	 * Gets target bitmap without any transformations
	 */
	public TargetMatrix getDefaultTarget() {
		
		return defaultTarget;
	}
	
	
	/**
	 * Builds binary matrix from image with given transformations
	 */
	private int[] buildMatrix(BufferedImage image, Transformation parameters) {

		int[] stretchedMatrix = stretchImage(image, parameters);
		int[] translatedMatrix = translateMatrix(stretchedMatrix, parameters);
		
		return translatedMatrix;
	}
	
	/**
	 * Stretches image with given transformations and fits to 32x32 square. Converts
	 * image to matrix form.
	 */
	private int[] stretchImage(BufferedImage image, Transformation parameters) {

		int horizontalStretch = parameters.horizontalStretch;
		int verticalStretch = parameters.verticalStretch;
		
		// this is slow operation so cache results		
		Transformation stretchAmount = new Transformation(0,0,horizontalStretch,verticalStretch);
		int[] stretchedMatrix = stretchedMatrices.get(stretchAmount);
		
		if (stretchedMatrix == null) {
			int newWidth = par.targetSize + horizontalStretch;
			int newHeight = par.targetSize + verticalStretch;
			BufferedImage grayscale = ImageUtil.stretch(image, newWidth, newHeight);
			BufferedImage squareGrayscale = ImageUtil.createSquareImage(grayscale, 32);
			BufferedImage squareBWImage = ImageUtil.makeBlackAndWhite(squareGrayscale, par.pixelRGBThreshold);
			stretchedMatrix = ImageUtil.buildMatrix32(squareBWImage);
			stretchedMatrices.put(stretchAmount, stretchedMatrix);
		}
		
		return stretchedMatrix;
	}
	
	/**
	 * Translates matrix with given transformations and crops to 32x32
	 */
	private int[] translateMatrix(int[] matrix, Transformation parameters) {

		return MatrixUtil.moveMatrix(matrix, parameters.horizontalTranslate,
				parameters.verticalTranslate);
	}
	
	private void writeDebugImage(TargetMatrix target, Transformation parameters) throws Exception {
		
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix()+".ocr.transform."+parameters+".png");
		BufferedImage image = ImageUtil.buildImage(target.matrix);
		BufferedImage scaledImage = ImageUtil.buildScaledImage(image, par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}
}
