package net.kanjitomo.util;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.kanjitomo.CharacterColor;
import net.kanjitomo.DictionaryType;
import net.kanjitomo.Orientation;

public class Parameters {
	
	private Parameters() {}
	
	private static Parameters par;
	
	public static Parameters getInstance() {
		
		if (par == null) {
			par = new Parameters();
		}
		 
		return par;
	}
	
	/**
	 * Directory relative to package root that contains the data files
	 */
	public String dataDirName = "data";
	
	/**
	 * Directory inside data dir that contains dictionaries
	 */
	public String dictionaryDirName = "dictionary";
	
	/**
	 * Directory inside data dir that contains cache files
	 */
	public String cacheDirName = "cache";
	
	/**
	 * Current orientation.
	 * 
	 * If true, use vertical reading direction. 
	 * If false, use horizontal reading direction.
	 */
	public boolean vertical = true;
	
	/**
	 * Target orientation
	 */
	public Orientation orientationTarget = Orientation.AUTOMATIC;
	
	/**
	 * Target character color.
	 */
	public CharacterColor colorTarget = CharacterColor.AUTOMATIC;
	
	/**
	 * Fonts used to generate reference characters.
	 * First font is the primary font used for initial alignment. Other fonts
	 * are used for refined alignments.
	 */
	public String[] referenceFonts = new String[] {"MS Gothic","SimSun"	}; // MS Gothic, SimSun, Meiryo UI, SimHei, SimSun bold
	
	/**
	 * If true, font should be bold
	 */
	public boolean[] referenceFontsBold = new boolean[] {false, true};
	
	/**
	 * Target size for reference characters. Target characters are scaled to this size.
	 * Should be below 32 to make room for transformations. If modified reference cache 
	 * must to be regenerated.
	 */
	public static final int targetSize = 30;
	
	// image sharpening parameters
	public float unsharpAmount = 4.0f; // 4.0f
	public int unsharpRadius = 2; // 2
	public int unsharpThreshold = 2; // 2
	
	/**
	 * Minimum pixel RGB value
	 */
	public int pixelRGBThreshold = 140;
	
	/**
	 * Fixed black level can be used to manually specify (pick) text color
	 */
	public static boolean fixedBlackLevelEnabled = false;
	public static int fixedBlackLevelRed = 0;
	public static int fixedBlackLevelGreen = 0;
	public static int fixedBlackLevelBlue = 0;
	public static int fixedBlackLevelRange = 50;
	
	/**
	 * How many halo layers are generated around reference and target characters.
	 * If this is increased ReferenceMatrixCacheBuilder must be run again. Most layers are
	 * one pixel wide, last layer contains all remaining pixels.
	 */
	public static final int ocrHaloSize = 3;
	
	/**
	 * Color used for OCR debug images. First halo color is the layer closes to matching pixels.
	 * In-between colors are iterpolated 
	 */
	public Color ocrTargetHaloFirstColor = new Color(255,0,0);
	public Color ocrTargetHaloLastColor = new Color(255,175,175);
	public Color ocrReferenceHaloFirstColor = new Color(100,100,100);
	public Color ocrReferenceHaloLastColor = new Color(195,195,195);
	
	/**
	 * Score for common pixel that is found in both target and reference images.
	 */
	public float ocrBlackPixelScore = 4f; 

	/**
	 * Score for white pixels that are not part of target or reference images.
	 */
	public float ocrWhiteScore = 4f; 
	
	/**
	 * Score for pixels in target but not in reference image. Indexed by halo layer.
	 */
	public float[] ocrTargetHaloScores = new float[] {-1f, -5f, -12f};
	
	/**
	 * Score for pixels in reference but not in target image. Indexed by halo layer.
	 */
	public float[] ocrReferenceHaloScores = new float[] {-1f, -4f, -10f};
	
	/**
	 * Score for halo pixels that span two black regions. These are often brush strokes in 
	 * wrong place at invalid reference character.
	 * 
	 * TODO remove if not used
	 */
	public float ocrConnectedHaloPixelsScore = -5f; // -10f
	
	/**
	 * This is added to each score
	 */
	public float ocrBaseScore = 1000f;
	
	/**
	 * Number of best results that are returned from OCR stage 1
	 */
	public int ocrKeepResultsStage1 = 30;
	
	/**
	 * Number of best results that are returned from OCR stage 2
	 */
	public int ocrkeepResultsStage2 = 10;
	
	/**
	 * Maximum number of target characters that can be returned by
	 * single OCR run. 4 is a good number since it restricts the
	 * dictionary search nicely and matches most users CPU count. 
	 */
	public int ocrMaxCharacters = 4;
	
	/**
	 * Number of threads used to run OCR in parallel. This should
	 * not be larger than user's CPU count and not larger than 
	 * ocrMaxCharacters
	 */
	public int ocrThreads = 4;
	
	/**
	 * How large NGram indexes are created. Larger value might make
	 * search faster but takes more memory 
	 */
	public int indexMaxCharacters = 4;
	
	/**
	 * In combined search mode search is done agains both default and names dictionaries.
	 * Results are returned from names dictionary only if the score is better than this
	 * compared to default dictionary.
	 * 
	 * Larger value makes more likely that results are from default dictionary.
	 */
	public float defaultDictionaryBias = 1.05f; 
	
	/**
	 * First dictionary used for searching 
	 */
	public DictionaryType primaryDictionary = DictionaryType.JAPANESE_DEFAULT;
	
	/**
	 * If a match is not found from primary dictionary, secondary is used for searching  
	 */
	public DictionaryType secondaryDictionary = DictionaryType.JAPANESE_NAMES;
	
	/**
	 * Source file for default dictionary.
	 * Jim Breen's EDICT
	 * 
	 * http://www.edrdg.org/jmdict/edict.html
	 * http://nihongo.monash.edu/wwwjdicinf.html
	 */
	public String defaultDictionaryFileName= "edict2";
	
	/**
	 * Source file for names dictionary.
	 * Jim Breen's ENAMDICT
	 *  
	 * http://www.edrdg.org/enamdict/enamdict_doc.html
	 */
	public String namesDictionaryFileName= "enamdict";
	
	public File getDataDir() throws Exception {
		return Util.findFile(dataDirName);
	}
	
	public File getDictionaryDir() throws Exception {
		return new File(Util.findFile(dataDirName)+"/"+dictionaryDirName);
	}
	
	public File getCacheDir() throws Exception {
		return new File(Util.findFile(dataDirName)+"/"+cacheDirName);
	}
	
	/**
	 * Returns file name that contains serialized dictionary
	 * 
	 * @param description default, names or chinese
	 */
	public String getDictionaryCacheFileName(DictionaryType dictionary) throws Exception {
		return dictionary+".cache";
	}
	
	/**
	 * Returns file that contains serialized dictionary
	 * 
	 * @param description default, names or chinese
	 */
	public File getDictionaryCacheFile(DictionaryType dictionary) throws Exception {
		return new File(Util.findFile(dataDirName)+"/"+cacheDirName+"/"+
				getDictionaryCacheFileName(dictionary));
	}

	// debug-related parameters
	
	public PrintLevel printLevel = PrintLevel.OFF;
	public SaveAreaImages saveAreaImages = SaveAreaImages.OFF;
	public SaveOCRImages saveOCRImages = SaveOCRImages.OFF;
	
	/**
	 * printLevel >= PRINT_OUTPUT
	 */
	public boolean isPrintOutput() {
		return printLevel.isGE(PrintLevel.BASIC);
	}
	
	/**
	 * printLevel >= PRINT_DEBUG
	 */
	public boolean isPrintDebug() {
		return printLevel.isGE(PrintLevel.DEBUG);
	}
	
	/**
	 * saveAreaImages >= SAVE_FAILED
	 */
	public boolean isSaveAreaFailed() {
		return saveAreaImages.isGE(SaveAreaImages.FAILED);
	}
	
	/**
	 * saveAreaImages >= SAVE_ALL
	 */
	public boolean isSaveAreaAll() {
		return saveAreaImages.isGE(SaveAreaImages.ALL);
	}
	
	/**
	 * saveOCRImages >= SAVE_FAILED
	 */
	public boolean isSaveOCRFailed() {
		return saveOCRImages.isGE(SaveOCRImages.FAILED);
	}
	
	/**
	 * saveOCRImages >= SAVE_ALL
	 */
	public boolean isSaveOCRAll() {
		return saveOCRImages.isGE(SaveOCRImages.ALL);
	}
	
	/**
	 * Selects which area debug images are saved.
	 * Comment out unneeded images.
	 */
	public String[] debugImages = new String[] {
			"original",
			"sharpened",
			"binary",
			"invert",
			"touching",
			"background",
			"areas",
			"columns",
			"punctuation",
			"splitareas",
			"mergeareas",
			"furigana",
			"connections",
			"combined"
			};

	/**
	 * Small debug images are enlarged by this amount
	 */	
	public int smallDebugAreaImageScale = 1;
	
	/**
	 * If image size (larger dimension) is smaller than this, smallDebugAreaImageScale is used
	 */
	public int smallDebugAreaImageThreshold = 500;
	
	/**
	 * If true, writes area debug image to clipboard instead of file.
	 * Should be used only if there's single debug image.
	 */
	public boolean debugAreaImageToClipboard = false;
	
	/**
	 * OCR debug images are enlarged by this amount
	 */
	public int debugOCRImageScale = 5;
	
	/**
	 * Maximum number of debug images generated
	 */
	public int maxDebugImages = 1000;
	
	/**
	 * If set, these characters are the expected (correct) OCR results in test image.
	 * These are kept in the result queue even if bad score.
	 */
	public String expectedCharacters = null;
	
	/**
	 * Areas or columns that should be present in test image
	 */
	public List<Rectangle> expectedRectangles = new ArrayList<>();
	
	/**
	 * Directory relative to package root where debug images are stored
	 */
	public String debugDirName = "test results";
	
	/**
	 * Directory relative to package root where debug images are stored
	 */
	public File getDebugDir() throws Exception {
		return new File(getTestDir().getAbsolutePath()+"//"+debugDirName);
	}
	
	/**
	 * Directory relative to package root where test set specifications are stored
	 */
	public String testDirName = "test";
	
	/**
	 * Directory relative to package root where test set specifications are stored
	 */
	public File getTestDir() throws Exception {
		return Util.findFile(testDirName);
	}
	
	// rest of the parameters are for internal use and should not be edited
	
	public String tempDebugFilePrefix = null;

	public int tempDebugFileIndex = 1;
	
	/**
	 * String added in front of debug file names. Contains test reference and image sequence number
	 */
	public String getDebugFilePrefix() {
		if (tempDebugFilePrefix == null) {
			return "0."+(tempDebugFileIndex++); 
		} else {
			return tempDebugFilePrefix+"."+(tempDebugFileIndex++);
		}
	}
	
	public String getDebugFilePrefix(Integer charIndex) {
		if (charIndex == null) {
			return getDebugFilePrefix();
		} else  if (tempDebugFilePrefix == null) {
			return charIndex+"."+(tempDebugFileIndex++);
		} else { 
			return tempDebugFilePrefix+"."+(tempDebugFileIndex++);
		}
	}
	
	/**
	 * Gets the next debug file index but does not increment it.
	 */
	public int peekDebugFileIndex() {
		return tempDebugFileIndex;
	}
}
