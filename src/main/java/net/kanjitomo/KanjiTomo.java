package net.kanjitomo;

import static net.kanjitomo.DictionaryType.*;
import static net.kanjitomo.dictionary.SearchMode.CONTAINS;
import static net.kanjitomo.dictionary.SearchMode.STARTS_WITH;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.kanjitomo.area.AreaDetector;
import net.kanjitomo.area.AreaTask;
import net.kanjitomo.area.Column;
import net.kanjitomo.area.SubImage;
import net.kanjitomo.dictionary.MultiSearchResult;
import net.kanjitomo.dictionary.SearchManager;
import net.kanjitomo.ocr.OCRManager;
import net.kanjitomo.ocr.OCRResult;
import net.kanjitomo.ocr.OCRTask;
import net.kanjitomo.ocr.ReferenceMatrixCacheLoader;
import net.kanjitomo.util.Parameters;
import net.kanjitomo.util.PrintLevel;

/**
 * Main class of the KanjiTomo OCR library
 */
public class KanjiTomo {

	private Parameters par = Parameters.getInstance();
	private OCRManager ocr;
	private SearchManager searchManager;
	private AreaTask areaTask;
	private List<SubImage> subImages;
	private List<Rectangle> locations;
	private List<OCRTask> ocrTasks;
	private List<String> characters;
	private List<List<Integer>> ocrScores;
	private MultiSearchResult multiSearchResult;
	private OCRResults results;
	
	/**
	 * Loads data structures into memory. This should be called first on it's own
	 * thread as the program starts because loading data can take couple of seconds.
	 * It's allowed to call this multiple times, results are cached and further calls
	 * don't take any more time unless dictionary is changed.
	 */
	public void loadData() throws Exception {
		
		if (ocr == null) {
			ocr = new OCRManager();
			ocr.loadReferenceData();
			new ReferenceMatrixCacheLoader().load();
			searchManager = new SearchManager();
		}
		
		if (par.primaryDictionary != null) {
			searchManager.loadData();
			searchManager.waitForIndexing();
		}
	}
	
	/**
	 * Sets the target image. This can be a screenshot around target characters or a whole page.
	 */
	public void setTargetImage(BufferedImage image) throws Exception {
		
		if (ocr == null) {
			loadData();
		}
		
		long started = System.currentTimeMillis();
		detectAreas(image);
		long time = System.currentTimeMillis() - started;
		
		if (par.isPrintDebug()) {
			System.out.println("Target image processed, "+time+" ms\n");
		}
		if (par.isPrintOutput() && !par.isPrintDebug()) {
			System.out.println("Target image processed\n");
		}
	}
	
	/**
	 * Gets columns detected from target image
	 */
	public List<net.kanjitomo.Column> getColumns() {
		
		if (areaTask == null) {
			return new ArrayList<net.kanjitomo.Column>();
		}
		
		List<net.kanjitomo.Column> simpleColumns = new ArrayList<>();
		for (Column column : areaTask.getColumns()) {
			net.kanjitomo.Column simpleColumn = column.getSimpleColumn();
			if (column.getPreviousColumn() != null) {
				simpleColumn.previousColumn = column.getPreviousColumn().getSimpleColumn();
			}
			if (column.getNextColumn() != null) {
				simpleColumn.nextColumn = column.getNextColumn().getSimpleColumn();
			}
			simpleColumns.add(simpleColumn);
		}
		
		return simpleColumns;
	}
	
	/**
	 * Runs OCR starting from a point.
	 * 
	 * @param point Coordinates inside target image. Closest character near this point
	 * will be selected as the first target character. Point should correspond to mouse
	 * cursor position relative to target image.
	 * 
	 * @return null if no characters found near point
	 */
	public OCRResults runOCR(Point point) throws Exception {
		
		if (areaTask == null) {
			throw new Exception("Target image not set");
		}
		
		if (par.isPrintOutput()) {
			System.out.println("Run OCR at point:"+(int)point.getX()+","+(int)point.getY());
		}
		
		// select areas near point
		subImages = areaTask.getSubImages(point);
		
		return runOCR();
	}
	
	/**
	 * Runs OCR inside pre-defined areas where each rectangle contains single characters.
	 * This can be used if area detection is done externally and KanjiTomo is only used for final OCR.
	 */
	public OCRResults runOCR(List<Rectangle> areas) throws Exception {
		
		if (areaTask == null) {
			throw new Exception("Target image not set");
		}

		if (par.isPrintOutput()) {
			System.out.println("Run OCR rectangle list");
		}
		
		// build subimages from argument areas
		subImages = areaTask.getSubImages(areas);
		
		return runOCR();
	}
	
	/**
	 * Runs OCR for target areas (SubImages)
	 */
	private OCRResults runOCR() throws Exception {
	
		long started = System.currentTimeMillis();
		
		// get target locations
		locations = new ArrayList<Rectangle>();
		for (SubImage subImage : subImages) {
			locations.add(subImage.location);
			verticalOrientation = subImage.isVertical();
		}
		if (subImages.size() == 0) {
			if (par.isPrintOutput()) {
				System.out.println("No characters identified");
			}
			return null;
		}
		
		// run ocr for each character
		ocrTasks = new ArrayList<OCRTask>();
		int charIndex = 0;
		Column lastColumn = null;
		for (SubImage subImage : subImages) {
			OCRTask ocrTask = new OCRTask(subImage.image);
			ocrTask.charIndex = charIndex++; 
			ocrTasks.add(ocrTask);
			if (lastColumn == null) {
				lastColumn = subImage.column;
			} else {
				if (lastColumn != subImage.column) {
					ocrTask.columnChanged = true;
				}
			}
			ocr.addTask(ocrTask);
		}
		ocr.waitUntilDone();
		
		// collect identified characters
		characters = new ArrayList<>();
		ocrScores = new ArrayList<>();
		for (OCRTask ocrTask : ocrTasks) {
			characters.add(ocrTask.getResultString());
			List<Integer> scores = new ArrayList<Integer>();
			for (OCRResult result : ocrTask.results) {
				scores.add(result.getScore());
			}
			ocrScores.add(scores);
		}
		
		if (par.primaryDictionary != null) {
			// cut off characters from other columns
			// This is used to restrict multisearch to characters within single column, multi-column
			// multisearch is too unreliable since it can often connect unrelated characters into single word.
			int maxWidth = 0;
			for (OCRTask task : ocrTasks) {
				if (task.columnChanged) {
					break;
				}
				++maxWidth;
			}
			multiSearchResult = searchManager.multiSearch(characters, ocrScores, maxWidth);
		}
		
		// wrap results into final object
		if (par.primaryDictionary != null) {
			results = new OCRResults(characters, locations, ocrScores,
					multiSearchResult.words, multiSearchResult.searchStr, verticalOrientation);
		} else {
			results = new OCRResults(characters, locations, ocrScores, null, null, verticalOrientation);
		}
		
		long time = System.currentTimeMillis() - started;
		
		if (par.isPrintOutput()) {
			System.out.println(results+"\n");
		}
		
		if (par.isPrintDebug()) {
			System.out.println("OCR runtime "+time+" ms\n");
		}
		
		return results; 
	}
	
	/**
	 * Analyzes the image and detects areas that might contain characters.
	 */
	private void detectAreas(BufferedImage image) throws Exception {
		
		areaTask = new AreaTask(image);
		new AreaDetector().run(areaTask);
	}
	
	/**
	 * Vertical orientation was used in the area closest to selected point
	 */
	private boolean verticalOrientation = true;
	
	/**
	 * Searches words from selected dictionary.
	 * 
	 * @param searchString Search term supplied by the user or read from OCR results
	 * 
	 * @param startsWith If true, only words starting with the searchString are
	 * considered. If false, searchString can appear anywhere in the word (kanji or
	 * kana fields, search from description field is not supported)
	 * 
	 * @return List of maching words sorted by increasing length of kanji/kana field
	 */
	public List<Word> searchDictionary(String searchString, boolean startsWith) throws Exception {
		return searchManager.search(searchString, startsWith ? STARTS_WITH : CONTAINS);
	}
	
	/**
	 * Sets which dictionary is used. It's recommended to call loadData() after changing the dictionary
	 * in background thread before user interaction. It's possible to turn off dictionary search
	 * by setting primaryDictionary to null but this is not recommended since search is used to
	 * refine OCR results.
	 * 
	 * @param primaryDictionary First dictionary used for searching
	 * @param secondaryDictionary If a match is not found from primary dictionary, secondary is used for searching
	 */
	public void setDictionary(DictionaryType primaryDictionary, DictionaryType secondaryDictionary) {
		
		par.primaryDictionary = primaryDictionary;
		par.secondaryDictionary = secondaryDictionary;
		
		if (primaryDictionary == CHINESE || secondaryDictionary == CHINESE) {
			throw new Error("Chinese dictionary is not implemented");
		}
	}
	
	/**
	 * Sets the reading direction. Default is automatic.
	 * 
	 * Target image needs to be re-analyzed after changing the orientation by calling setTargetImage again.
	 */
	public void setOrientation(Orientation orientation) {
		par.orientationTarget = orientation;
	}
	
	/**
	 * Sets character and background color. Black and white characters work best,
	 * but coloured characters might also work if there's enough contrast.
	 * 
	 * Target image needs to be re-analyzed after changing the color by calling setTargetImage again.
	 * 
	 * Default: CharacterColor.AUTOMATIC
	 */
	public void setCharacterColor(CharacterColor color) {
		par.colorTarget = color;
	}

	/**
	 * If true, OCR results are printed to stdout.
	 * If false, nothing is printed.
	 * Default: false
	 */
	public void setPrintOutput(boolean printOutput) {
		
		if (printOutput == false) {
			par.printLevel = PrintLevel.OFF;
		} else {
			par.printLevel = PrintLevel.BASIC;
		}
	}
	
	/**
	 * Stops all threads. This should be called before closing the program.
	 */
	public void close() {
		ocr.stopThreads();
	}
}
