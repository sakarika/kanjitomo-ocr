package net.kanjitomo.test;

import static net.kanjitomo.DictionaryType.JAPANESE_DEFAULT;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import net.kanjitomo.Column;
import net.kanjitomo.DictionaryType;
import net.kanjitomo.KanjiTomo;
import net.kanjitomo.OCRResults;
import net.kanjitomo.ocr.OCR;
import net.kanjitomo.util.Parameters;
import net.kanjitomo.util.PrintLevel;
import net.kanjitomo.util.SaveAreaImages;
import net.kanjitomo.util.SaveOCRImages;

/**
 * Runs tests
 */
public class Tester {
	
	private Parameters par = Parameters.getInstance();
	private boolean dataLoaded = false;
	private KanjiTomo tomo;
	private TestSetLoader loader;
	private boolean runAreaTests = false;
	private boolean runOCRTests = false;
	
	/** Test set name -> TestSet object */
	private Map<String, TestSet> testSets;

	/**
	 * Loads data structures used by OCR. Must be called before any tests are run.
	 */
	public void loadData() throws Exception {

		if (dataLoaded) {
			return;
		}
		
		tomo = new KanjiTomo();
		tomo.loadData();
		
		loader = new TestSetLoader();
		testSets = loader.readTestSets();
	}
	
	/**
	 * Runs all tests
	 */
	public void runTests(boolean areaTests, boolean ocrTests) throws Exception {
		
		for (TestSet testSet : testSets.values()) {
			runTests(testSet.name, areaTests, ocrTests, null, new String[] {});
		}
	}
	
	private List<Test> failedTests;
	
	/**
	 * Runs tests
	 * 
	 * @param testSetName Test set name, for example: "default" or "local". Tests are loaded
	 * from file: "test/testSetName.txt"
	 * @param characters Character filter. Run only tests with target character in this list
	 * @param images Filename filter. If empty, runs all tests. if set, runs tests that have matching 
	 * image filename. Regular expression syntax
	 */
	public void runTests(String testSetName, boolean areaTests, boolean ocrTests, String characters, String ... images) throws Exception {
		
		testDebugImageDirExists();
		
		runAreaTests = areaTests;
		runOCRTests = ocrTests;
		
		List<Pattern> filterPatterns = new ArrayList<>();
		for (String filter : images) {
			filterPatterns.add(Pattern.compile(filter));
		}
		
		// run tests that match any filter
		failedTests = new ArrayList<>();
		if (!testSets.containsKey(testSetName)) {
			throw new Exception("Test set:"+testSetName+" not found");
		}
		TestSet testSet = testSets.get(testSetName);
		tests: for (TestImage testImage : testSet.images) {
			if (images.length == 0) {
				runTestSet(testImage, characters);
			} else {
				for (Pattern filter : filterPatterns) {
					Matcher m = filter.matcher(testImage.file.getName());
					if (m.matches()) {
						runTestSet(testImage, characters);
						continue tests;
					}
				}
			}
		}
		
		// print failed tests
		if (failedTests.size() > 0) {
			System.err.println("Failed tests:");
			for (Test test : failedTests) {
				System.err.println(test);
			}
		}
	}
	
	/**
	 * Analyzes all image files in directory. Finds areas and columns but doesn't run any OCR.
	 * Debug images are saved. Check Parameters.debugImages.
	 * 
	 * @param suffix For example: ".png", only these files are processed.
	 */
	public void runAreasDirectory(File directory, String suffix) throws Exception { 
		
		par.saveAreaImages = SaveAreaImages.ALL; 
		
		for (File file : directory.listFiles()) {
			
			if (!file.getName().endsWith(suffix)) {
				continue;
			}
			
			BufferedImage image = ImageIO.read(file);
			tomo.setTargetImage(image);
		}
	}
	
	/**
	 * Searches default dictionary.
	 * 
	 * @param startsWith If true, only words starting with the searchString are
	 * considered. If false, searchString can appear anywhere in the word (kanji or
	 * kana fields, search from description field is not supported)
	 */
	public void runTestSearchDefault(String searchString, boolean startsWith) throws Exception {
		
		tomo.setDictionary(JAPANESE_DEFAULT, null);
		tomo.loadData();
		tomo.searchDictionary(searchString, startsWith);
	}
	
	/**
	 * Searches names dictionary.
	 * 
	 * @param startsWith If true, only words starting with the searchString are
	 * considered. If false, searchString can appear anywhere in the word (kanji or
	 * kana fields, search from description field is not supported)
	 */
	public void runTestSearchNames(String searchString, boolean startsWith) throws Exception {
		
		tomo.setDictionary(DictionaryType.JAPANESE_NAMES, null);
		tomo.loadData();
		tomo.searchDictionary(searchString, startsWith);
	}
	
	/**
	 * Searches names dictionary.
	 * 
	 * @param startsWith If true, only words starting with the searchString are
	 * considered. If false, searchString can appear anywhere in the word (kanji or
	 * kana fields, search from description field is not supported)
	 */
	public void runTestSearchCombined(String searchString, boolean startsWith) throws Exception {
		
		tomo.setDictionary(DictionaryType.JAPANESE_DEFAULT, DictionaryType.JAPANESE_NAMES);
		tomo.loadData();
		tomo.searchDictionary(searchString, startsWith);
	}
	
	private void runTestSet(TestImage testImage, String characters) throws Exception {
		
		System.out.println("\nImage:"+testImage.file.getName());
		
		// load test image
		String testImageName = testImage.file.getName().replace(".png", "");
		par.tempDebugFilePrefix = testImageName;
		
		// set expected areas
		if (runAreaTests) {
			par.expectedRectangles.clear();
			for (Test test : testImage.tests) {
				if (test instanceof AreaTest) {
					par.expectedRectangles.add(((AreaTest)test).rect);
				}
			}
		}
		
		// find areas 
		tomo.setTargetImage(ImageIO.read(testImage.file));
		
		// run test
		for (Test test : testImage.tests) {
			if (!runAreaTests && test instanceof AreaTest) {
				continue;
			}
			if (!runOCRTests && test instanceof OCRTest) {
				continue;
			}
			if (runOCRTests && characters != null && test instanceof OCRTest) {
				boolean found = false;
				for (Character testCharacter : ((OCRTest)test).characters.toCharArray()) {
					if (characters.contains(testCharacter+"")) {
						found = true;
						break;
					}	
				}
				if (!found) {
					continue;
				}
			}
			System.out.println("Test:"+test);
			boolean passed = runTest(test);
			if (passed) {
				System.out.println("Passed");
			} else {
				System.out.println("Failed");
				failedTests.add(test);
			}
		}
	}
	
	private boolean runTest(Test test) throws Exception {
		
		if (test instanceof AreaTest) {
			return runAreaTest((AreaTest) test);
		} else {
			return runOCRTest((OCRTest) test);
		}
	}
	
	/**
	 * Runs area test
	 * 
	 * @return true if test passed, else false
	 */
	private boolean runAreaTest(AreaTest test) throws Exception {
		
		for (Column column : tomo.getColumns()) {
			if (column.rect.equals(test.rect)) {
				return true;
			}
			for (Rectangle area : column.areas) {
				if (area.equals(test.rect)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Runs OCR test
	 * 
	 * @return true if test passed, else false
	 */
	private boolean runOCRTest(OCRTest test) throws Exception {
		
		par.ocrMaxCharacters = test.characters.length();
		par.expectedCharacters = test.characters;
		OCRResults results = tomo.runOCR(test.point);
		if (results == null || results.bestMatchingCharacters.length() == 0) {
			return false;
		}
		if (results.bestMatchingCharacters.equals(test.characters)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void close() {
		
		if (tomo != null) {
			tomo.close();
		}
	}
	
	/**
	 * Checks if debug image dir exits. If not, creates it.
	 */
	private void testDebugImageDirExists() throws Exception {
		
		File debugDir = par.getDebugDir(); 
		if (!debugDir.exists()) {
			debugDir.mkdir();
		}
	}
	
	/**
	 * Deletes old debug images
	 */
	private void clearDebugImages() throws Exception {
			
		testDebugImageDirExists();
		for (File file : par.getDebugDir().listFiles()) {
			if (file.getName().endsWith(".png")) {
				file.delete();
			}
		}
	}
	
	public static void main(String[] args) {
		
		Parameters par = Parameters.getInstance();
		par.printLevel = PrintLevel.DEBUG;
		par.saveAreaImages = SaveAreaImages.OFF;
		par.saveOCRImages = SaveOCRImages.OFF;
		par.ocrThreads = 1;
		par.primaryDictionary = null;
		
		Tester tester = new Tester();
		try {
			tester.loadData();
			tester.clearDebugImages();
			tester.runTests(false, true);
//			tester.runTests("default", false, true, "今", "1.png");
//			tester.runTests("local", false, true, "想", "487.png");
//			tester.runTestSearchDefault("日", true);
//			tester.runTestSearchNames("あきら", true);
//			tester.runTestSearchCombined("希一", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (OCR.getTotalOCRCount() > 0) {
			System.out.println("Average OCR time:"+Math.round(OCR.getAverageOCRTime())+" ms");
		}
		
		tester.close();
		System.exit(0);
	}
}
