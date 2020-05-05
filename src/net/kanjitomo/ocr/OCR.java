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
 * Runs all OCR algorithm stages. Each stage is more accurate but slower than last, only a
 * subset of best results become input of next stage. 
 */
public class OCR {
	
	private Parameters par = Parameters.getInstance();
	private static int totalOCRCount = 0;
	private static long totalOCRTime = 0;
	private static boolean debugStages = false;
	
	public void run(OCRTask task) throws Exception {
		
		long started = System.currentTimeMillis();
		
		// holds target image transformations (target is stretched and compressed to different
		// sizes). these are shared between first two stages and should not be re-generated 
		Transform transform = new Transform(task);
		
		// stage 1, find common pixels, consider basic transformations
		OCRAlignCharacters stage1 = new OCRAlignCharacters(task, transform);
		task.results = stage1.run(null, true, 1, 1, 1, par.ocrKeepResultsStage1);
		if (debugStages) {debug(task, System.currentTimeMillis()-started, 1);}
		
		// stage 2, find common pixels, consider more transformations per character
		OCRAlignCharacters stage2 = new OCRAlignCharacters(task, transform);
		task.results = stage2.run(getCharacters(task.results), true, 2, 2, 4, par.ocrkeepResultsStage2);
		if (debugStages) {debug(task, System.currentTimeMillis()-started, 2);}		
		
		// stage 3, align individual components
		OCRAlignComponents alignComponents = new OCRAlignComponents();
		task.results = alignComponents.run(task.results);
				
		long time = System.currentTimeMillis() - started;
		totalOCRTime += time;
		++totalOCRCount;
		debug(task, time, 3);
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
	private void debug(OCRTask task, long time, int stage) throws Exception {
		
		if (par.isPrintDebug()) {
			System.out.println("OCR total "+time+" ms");
			if (task.results.size() == 0) {
				System.err.println("No results");
				return;
			}
			System.out.println("\nOCR results stage "+stage);
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
				writeDebugImages(task, stage);
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
	
	private void writeDebugImages(OCRTask task, int stage) throws Exception {
		
		for (OCRResult result : task.results) {
			if (writeTarget) {
				writeTargetImage(result, stage);
			}
			if (writeReference) {
				writeReferenceImage(result, stage);
			}
			writeDebugImage(result, stage);
		}
	}
	
	private void writeTargetImage(OCRResult result, int stage) throws Exception {
				
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".ocr."+stage+".ori.png");
		BufferedImage targetImage = ImageUtil.buildImage(result.target.matrix);
		BufferedImage colorImage = ImageUtil.colorizeImage(targetImage, par.ocrTargetHaloLastColor);
		BufferedImage scaledImage = ImageUtil.buildScaledImage(colorImage, par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}
	
	private void writeReferenceImage(OCRResult result, int stage) throws Exception {
		
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".ocr."+stage+".ref."+result.getCharacter()+".png");
		BufferedImage referenceImage = ImageUtil.buildImage(result.reference.matrix);
		BufferedImage colorImage = ImageUtil.colorizeImage(referenceImage, par.ocrReferenceHaloLastColor);
		BufferedImage scaledImage = ImageUtil.buildScaledImage(colorImage, par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}
	
	private void writeDebugImage(OCRResult result, int stage) throws Exception {
		
		File file = new File(par.getDebugDir().getAbsolutePath()+"/"+
				par.getDebugFilePrefix(result.target.charIndex)+".ocr."+stage+".res."+result.getCharacter()+"."+
				result.getScore()+"."+result.target.transform+".png");
		BufferedImage scaledImage = ImageUtil.buildScaledImage(result.buildDebugImage(), par.debugOCRImageScale);
		ImageIO.write(scaledImage, "png", file);
	}	
}
