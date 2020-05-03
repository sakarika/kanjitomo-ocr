package net.kanjitomo.ocr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kanjitomo.util.Parameters;

/**
 * Compares reference characters against target bitmap. Iterates through different alignments 
 * and selects best matches.  
 */
public class OCRAlignCharacters {
	
	private Parameters par = Parameters.getInstance();
	private Transform transform;
	private OCRScoreCalculator scoreCalculator;
	private int topN;
	private Character expectedCharacter = null;
	
	/** If true, expected character is always included in results */ 
	private boolean forceExpectedCharacter = false;
	
	public OCRAlignCharacters(OCRTask task, Transform transform) {

		this.transform = transform;
		scoreCalculator = new OCRScoreCalculator();
		if (par.expectedCharacters != null) {
			expectedCharacter = par.expectedCharacters.charAt(task.charIndex);
		}
	}
	
	/**
	 * Finds best matching characters 
	 * 
	 * @param characters Limits search to these characters. If null, considers all characters.
	 * @param refined If true, uses more accurate but slower algorithm
	 * @param maxTranslate Maximum number of pixels translated (up/down/left/right)
	 * @param maxStretch Maximum number of pixels image is scaled
	 * @param maxSteps Maximum number of translate and stretch steps allowed in total 
	 * @param topN Returns only the best N results 
	 */
	public List<OCRResult> run(Set<Character> characters, boolean refined, 
			int maxTranslate, int maxStretch, int maxSteps, int topN) throws Exception {
	
		List<OCRResult> bestResults = null;
		this.topN = topN;
		
		long started = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			if (!refined) {
				System.out.print("\nBasic alignment ");
			} else {
				System.out.print("Refined alignment ");
			}
		}
		
		// generate OCR targets by applying offset and stretch transformations to target bitmap
		List<TargetMatrix> targets = transform.run(maxTranslate, maxStretch, maxSteps);
		
		// align reference bitmaps with target bitmaps
		// calculate score for each aligment and keep topN best
		for (String font : getFonts(refined)) {
			List<ReferenceMatrix> references = loadReferences(font, characters);
			List<OCRResult> results = findBestAlignment(targets, references, refined);
			bestResults = combineResults(bestResults, results, topN);
		}
		
		long done = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			System.out.println((done - started)+" ms");
		}
		
		return bestResults;
	}
	
	/**
	 * Aligns all targets and references, calculates score for each alignment
	 * and returns best results.
	 * 
	 * @param refined Include pixels that are close to target image but not 
	 * an exact match. This is slower but produces more accurate results.
	 */
	private List<OCRResult> findBestAlignment(List<TargetMatrix> targets,
			List<ReferenceMatrix> references, boolean refined) throws Exception {
		
		OCRResultPriorityQueue queue = new OCRResultPriorityQueue(topN);
		if (forceExpectedCharacter) {
			queue.setExpectedCharacter(expectedCharacter);
		}
		
		// align all target and reference combinations
		for (ReferenceMatrix reference : references) {
			OCRResult bestResult = null;
			for (TargetMatrix target : targets) {			
				OCRResult result = scoreCalculator.calcScore(target, reference, refined);
				if (bestResult == null || result.getScore() > bestResult.getScore()) {
					bestResult = result;
				}
			}
			queue.add(bestResult);
		}
	
		return queue.getResults();
	}	

	/**
	 * Gets fonts that should be used as OCR references.
	 * 
	 * @param refined If false, only returns the primary font. If true, also returns
	 * secondary fonts.
	 */
	private List<String> getFonts(boolean refined) {
		
		List<String> fonts = new ArrayList<String>();
		
		// add primary font
		fonts.add(par.referenceFonts[0]);
		
		// add secondary fonts
		if (refined) {
			for (int i=1 ; i<par.referenceFonts.length ; i++) {
				fonts.add(par.referenceFonts[i]);
			}
		}
		
		return fonts;
	}
	
	/**
	 * Loads reference bitmaps
	 * 
	 * @param character Returns bitmaps only for these characters. If null, all characters.
	 */
	private List<ReferenceMatrix> loadReferences(String font, Set<Character> characters) throws Exception {
		
		if (characters == null) {
			return loadReferences(font);
		}
		
		List<ReferenceMatrix> references = new ArrayList<ReferenceMatrix>();
		for (ReferenceMatrix reference : loadReferences(font)) {
			if (characters.contains(reference.character)) {
				references.add(reference);
			}
		}
		
		return references;
	}
	
	private static ReferenceMatrixCache cache;
	
	/**
	 * Loads reference bitmaps from cache file or from memory if called before.
	 */
	private List<ReferenceMatrix> loadReferences(String font) throws Exception {
		
		if (cache == null) {
			ReferenceMatrixCacheLoader loader = new ReferenceMatrixCacheLoader();
			loader.load();
			cache = loader.getCache();
		}
		
		return cache.get(font);
	}
	
	/**
	 * Combines OCR results into single list. Keeps only the best score for each character.
	 */
	private List<OCRResult> combineResults(List<OCRResult> results1, List<OCRResult> results2,
			int maxSize) {
		
		if (results1 == null && results2 == null) {
			throw new Error("Both results are null");
		} else if (results1 == null) {
			return results2;
		} else if (results2 == null) {
			return results1;
		}
		
		Map<Character, OCRResult> bestScores = new HashMap<>();
		for (OCRResult result1 : results1) {
			bestScores.put(result1.getCharacter(), result1);
		}
		for (OCRResult result2 : results2) {
			Character c = result2.getCharacter();
			OCRResult result1 = bestScores.get(c); 
			if (result1 == null || result1.score < result2.score) {
				bestScores.put(c, result2);
			}
		}
		
		List<OCRResult> results = new ArrayList<OCRResult>();
		results.addAll(bestScores.values());
		Collections.sort(results, new Comparator<OCRResult>() {
			public int compare(OCRResult o1, OCRResult o2) {
				return -1*o1.getScore().compareTo(o2.getScore());
			}
		});
		
		if (forceExpectedCharacter) {
			OCRResult removedExpectedResult = null;
			while (results.size() > maxSize) {
				OCRResult removed = results.remove(results.size()-1);
				if (removed.getCharacter().equals(expectedCharacter)) {
					removedExpectedResult = removed;
				}
			}
			if (removedExpectedResult != null) {
				results.add(removedExpectedResult);
			}
		}
		
		return results;
	}
}
