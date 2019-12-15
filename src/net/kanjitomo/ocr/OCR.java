package net.kanjitomo.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Parameters;

/**
 * Runs all OCR algorithm steps. Each steps is more accurate but slower than last, only a
 * subset of best results become input of next step. 
 */
public class OCR {
	
	private Parameters par = Parameters.getInstance();
	private static int totalOCRCount = 0;
	private static long totalOCRTime = 0;
	
	public void run(OCRTask task) throws Exception {
		
		long started = System.currentTimeMillis();
		
		// holds target image transformations (target is stretched and compressed to different
		// sizes). these are shared between first two steps and should not be re-generated 
		Transform transform = new Transform(task);
		
		// step 1, find best alignment, consider only common pixels between target and references
		OCRAlignCharacters step1 = new OCRAlignCharacters(task, transform);
		task.results = step1.run(null, false, par.ocrKeepResultsLevel1);
		
		// step 2, consider also pixels that are not exact match (halo matrices)
		OCRAlignCharacters step2 = new OCRAlignCharacters(task, transform);
		task.results = step2.run(getCharacters(task.results), true, par.ocrkeepResultsLevel2);
		
		// step 3, align individual components
		OCRAlignComponents alignComponents = new OCRAlignComponents();
		task.results = alignComponents.run(task.results);
				
		long done = System.currentTimeMillis();
		long time = done - started;
		
		++totalOCRCount;
		totalOCRTime += time;
		
		debug(task, time);
	}
	
	private Set<Character> getCharacters(List<OCRResult> results) {
		
		Set<Character> set = new HashSet<>();
		for (OCRResult result : results) {
			set.add(result.getCharacter());
		}
		return set;
	}
	
	/**
	 * Gets how many times OCR has been run accross all threads
	 */
	public static int getTotalOCRCount() {
		return totalOCRCount;
	}
	
	/**
	 * Gets the average total OCR time in ms
	 */
	public static float getAverageOCRTime() {
		return 1.0f * totalOCRTime / totalOCRCount;
	}
	
	
	/**
	 * Prints debug information and saves debug images if needed
	 */
	private void debug(OCRTask task, long time) throws Exception {
		
		if (par.isPrintDebug()) {
			System.out.println("OCR total "+time+" ms");
			if (task.results.size() == 0) {
				System.err.println("No results");
				return;
			}
			System.out.println("\nOCR results:");
			for (OCRResult result : task.results) {
				System.out.println(result);
			}
		}
		
		if (par.isSaveOCRFailed()) {
			Character bestMatch = task.results.get(0).getCharacter();
			Character expectedCharacter = null; 
			if (par.expectedCharacters != null) {
				expectedCharacter = par.expectedCharacters.charAt(task.charIndex);
			}
			if (par.isSaveOCRAll() || !bestMatch.equals(expectedCharacter)) {
				writeDebugImages(task);
			}
		}
	}
	
	/**
	 * If true, target image is written to file
	 */
	private boolean writeTarget = true;
	
	/**
	 * If true, reference image is written to file
	 */
	private boolean writeReference = true; 
	
	private void writeDebugImages(OCRTask task) throws Exception {
		
		for (OCRResult result : task.results) {
			if (writeTarget) {
				writeTargetImage(result);
			}
			if (writeReference) {
				writeReferenceImage(result);
			}
			writeDebugImage(result);
		}
	}
	
	private void writeTargetImage(OCRResult result) throws Exception {
				
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".ocr.original.png");
		BufferedImage targetImage = ImageUtil.buildImage(result.target.matrix);
		BufferedImage colorImage = ImageUtil.colorizeImage(targetImage, par.ocrTargetHaloLastColor);
		BufferedImage scaledImage = ImageUtil.buildScaledImage(colorImage, par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}
	
	private void writeReferenceImage(OCRResult result) throws Exception {
		
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".ocr.reference."+result.getCharacter()+".png");
		BufferedImage referenceImage = ImageUtil.buildImage(result.reference.matrix);
		BufferedImage colorImage = ImageUtil.colorizeImage(referenceImage, par.ocrReferenceHaloLastColor);
		BufferedImage scaledImage = ImageUtil.buildScaledImage(colorImage, par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}
	
	private void writeDebugImage(OCRResult result) throws Exception {
		
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".ocr.result."+result.getCharacter()+"."+
				result.getScore()+"."+result.target.transform+".png");
		BufferedImage scaledImage = ImageUtil.buildScaledImage(result.buildDebugImage(), par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}	
}
