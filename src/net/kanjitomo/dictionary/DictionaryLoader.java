package net.kanjitomo.dictionary;

import static net.kanjitomo.DictionaryType.CHINESE;
import static net.kanjitomo.DictionaryType.JAPANESE_DEFAULT;
import static net.kanjitomo.DictionaryType.JAPANESE_NAMES;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.kanjitomo.DictionaryType;
import net.kanjitomo.Word;
import net.kanjitomo.util.KryoFactory;
import net.kanjitomo.util.Parameters;

/**
 * Loads Jim Breen's Edict dictionary from source file.
 *  
 * http://www.edrdg.org/jmdict/edict.html 
 */
public class DictionaryLoader {
	
	private Parameters par = Parameters.getInstance();
	private Dictionary dictionary;
	private DictionaryType type;
	private List<Word> words;
	
	/**
	 * Loads new dictionaries from data folder
	 */
	public static void main(String[] args) {
		try {
			DictionaryLoader loader = new DictionaryLoader(JAPANESE_DEFAULT);
			loader.loadDictionary();
			loader = new DictionaryLoader(JAPANESE_NAMES);
			loader.loadDictionary();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
	
	public DictionaryLoader(DictionaryType type) throws Exception {
		
		words = new ArrayList<Word>();
		this.type = type;
		if (type == CHINESE) {
			throw new Exception("Not implemented");
		}
	}
	
	/**
	 * Loads dictionary from cache if present, else from source 
	 */
	public Dictionary loadDictionary() throws Exception {

		load: {
			String cacheFileName = par.getDictionaryCacheFileName(type);
			if (deserializeJar(cacheFileName)) {
				break load;
			}
			File cacheFile = par.getDictionaryCacheFile(type);
			if (cacheFile.exists() && !isCacheOutdated()) {
				deserializeFile(cacheFile);
				break load;
			}
			readFromSource();
			serializeToFile(cacheFile);
		}
		indexWords();
		
		return dictionary;
	}
	
	/**
	 * True if dictionary cache file is outdated and needs to be recreated
	 * (source file is newer)
	 */
	private boolean isCacheOutdated() {
		
		try {
			
			// source file modification time
			File sourceFile = getDictionaryFile();
			BasicFileAttributes sourceFileAttr = 
					Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
			FileTime sourceTime = sourceFileAttr.lastModifiedTime();
			
			// cache file modification time
			File cacheFile = par.getDictionaryCacheFile(type);
			BasicFileAttributes cacheFileAttr = 
					Files.readAttributes(cacheFile.toPath(), BasicFileAttributes.class);
			FileTime cacheTime = cacheFileAttr.lastModifiedTime();
			
			if (sourceTime.toMillis() > cacheTime.toMillis()) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception e) {
			System.err.println("Dictionary cache check failed");
			System.err.println(e);
			// use current cache file
			return false;
		}
	}
	
	/**
	 * Gets dictionary file reference for current dictionary
	 */
	private File getDictionaryFile() throws Exception {
		
		File file;
		if (type == JAPANESE_DEFAULT) {
			file = new File(par.getDictionaryDir()+"/"+par.defaultDictionaryFileName);
		} else if (type == JAPANESE_NAMES) {
			file = new File(par.getDictionaryDir()+"/"+par.namesDictionaryFileName);
		} else if (type == CHINESE) {
			throw new Exception("Not implemented");
		} else {
			throw new Exception("Dictionary not selected");
		}
		return file;
	}
	
	/**
	 * Reads dictionary from source file
	 */
	private void readFromSource() throws Exception {
		
		long started = System.currentTimeMillis();
		
		dictionary = new Dictionary(type);
		File file = getDictionaryFile();
		
		if (par.isPrintOutput()) {
			System.out.println("\nLoading dictionary:"+type);
			System.out.println("From source file:"+file);
		}
		
        String str;
        BufferedReader in = new BufferedReader(
           new InputStreamReader(new FileInputStream(file), "EUC-JP"));
        while ((str = in.readLine()) != null) {
        	
        	// example: 空港 [くうこう] /(n) airport/(P)/EntL1245570X/
        	
        	// read kanji and kana from start of the line
        	StringBuilder kanji = new StringBuilder();
        	StringBuilder kana = new StringBuilder();
        	int i=0;
        	boolean readKana = false;
        	while (true) {
        		char c = str.charAt(i++);
        		if (c == '/') {
        			break;
        		} else if (c == '[') {
        			readKana = true;
        		} else if (readKana && c == ']') {
        			readKana = false;
        		} else if (readKana) {
        			kana.append(c);
        		} else if (c == ' ') {
        			continue;
        		} else {
        			kanji.append(c);
        		}
        	}
        	
        	// rest of the line is description
        	StringBuilder description = new StringBuilder();
        	while (true) {
        		char c = str.charAt(i++);
        		if (c == '/') {
        			c = '\n';
        		}
        		description.append(c);
        		if (i == str.length()-1) {
        			break;
        		}
        	}
        	
        	Word word = new Word(kanji.toString(),
        			kana.length() > 0 ? kana.toString() : kanji.toString(), 
        			description.toString(), type == JAPANESE_NAMES);
        	List<Word> splittedWords;
        	if (type == JAPANESE_NAMES) {
        		splittedWords = splitKanji(word);
        	} else {
        		splittedWords = splitWithMap(word);
        	}
        	for (Word splittedWord : splittedWords) {
        		//words.add(splittedWord);
        		words.addAll(splitKana(splittedWord));
        	}
        	if (par.isPrintOutput() && words.size()%10000 == 0) {
        		System.out.println(words.size()+" words loaded");
        	}
        }
        in.close();
        
        // remove (...)
        for (Word word : words) {
    		int pos = word.kanji.indexOf("(");
    		if (pos != -1) {
    			word.kanji = word.kanji.substring(0, pos);
    		}
    	}
        
		long done = System.currentTimeMillis();
		if (par.isPrintOutput()) {
			System.out.println(words.size()+" words loaded "+(done - started)+" ms");
		}
	}
	
	/**
	 * Some words contain multiple versions split with ; in kanji field.
	 * Return them as individual words.
	 * @return
	 */
	private List<Word> splitKanji(Word word) {
	
		List<Word> words = new ArrayList<Word>();
		for (String kanji : word.kanji.split(";")) {
			words.add(new Word(cleanStr(kanji), word.kana, word.description, word.name));
		}
		return words;
	}
	
	private List<Word> splitKana(Word word) {
		
		List<Word> words = new ArrayList<Word>();
		for (String kana : word.kana.split(";")) {
			words.add(new Word(word.kanji, cleanStr(kana), word.description, word.name));
		}
		return words;
	}
	
	/**
	 * Some words contain multiple versions split with ; in kanji field.
	 * Return them as individual words.
	 * @return
	 */
	private List<Word> splitWithMap(Word word) {
	
		Map<String, String> kanaMap = buildKanjiKanaMap(word.kana);
		List<Word> words = new ArrayList<Word>();
		for (String kanji : word.kanji.split(";")) {
			String kana;
			if (kanaMap.containsKey(kanji)) {
				kana = kanaMap.get(kanji);
			} else if (kanaMap.containsKey("default")) {
				kana = kanaMap.get("default");
			} else {
				kana = word.kana;
			}
			if (kana == null) {
				kana = word.kana;
			}
			words.add(new Word(cleanStr(kanji), kana, word.description, word.name));
		}
		return words;
	}
	
	// example: でんきメーカー(電気メーカー,電機メーカー)
	private static final Pattern kanaMapPattern = Pattern.compile("^(.+)\\((.+)\\)");
	
	/**
	 * Some kanji and kana fields contain multiple options, like:
	 * 
	 * kanji:電気メーカー;電気メーカ;電機メーカー;電機メーカ
	 * kana:でんきメーカー(電気メーカー,電機メーカー);でんきメーカ(電気メーカ,電機メーカ)
	 * 
	 * Retuns a kanji -> kana map, for example: 電気メーカー -> でんきメーカー
	 * "default" -> defain kana (not mapped to any kanji)   
	 */
	private Map<String, String> buildKanjiKanaMap(String multikana) {
		
		// TODO do also for kana field

		String defaultStr = null;
		Map<String, String> map = new HashMap<>();
		for (String snip : cleanStr(multikana).split(";")) {
			Matcher m = kanaMapPattern.matcher(snip);
			if (m.matches()) {
				String kana = m.group(1);
				for (String kanji : m.group(2).split(",")) {
					map.put(kanji, kana);
				}
			} else {
				if (defaultStr == null) {
					defaultStr = snip;
				} else {
					defaultStr = defaultStr+";"+snip;
				}
			}
		}
		map.put("default", defaultStr);
		return map;
	}
	
	private String cleanStr(String str) {
		return str.replaceAll("\\(P\\)", "").replaceAll("\\(ok\\)", "").
				replaceAll("\\(oK\\)", "").replaceAll("\\(ik\\)", "").
				replaceAll("\\(iK\\)", "").replaceAll("\\(\\)", "").
				replaceAll("\\(gikun\\)", "");
	}

	/**
	 * Saves dictionary to serialized file
	 */
	public void serializeToFile(File file) throws Exception {
		
		if (par.isPrintOutput()) {
			System.out.println("\nSerializing dictionary:"+type);
			System.out.println("To file:"+file);	
		}
		
		Kryo kryo = KryoFactory.getKryo();
		Output output = new Output(new FileOutputStream(file));
		kryo.writeClassAndObject(output, words);
		output.close();
	}
	
	/**
	 * Loads words from serialized file
	 * 
	 * @param description default, names or chinese
	 * @param file serialized cache file
	 */
	@SuppressWarnings("unchecked")
	private void deserializeFile(File file) throws Exception {
		
		if (par.isPrintOutput()) {
			System.out.println("\nDeserializing dictionary from file:"+file);
		}
		deserializeStream(new FileInputStream(file));
	}
	
	/**
	 * Deserializes words from serialized file included in jar file
	 * 
	 * @return true if file exists and read is successful
	 */
	private boolean deserializeJar(String fileName) {
		
		InputStream in = getClass().getResourceAsStream("/"+fileName);
		if (in == null) {
			return false;
		}
		if (par.isPrintOutput()) {
			System.out.println("\nDeserializing dictionary:"+type+" from jar");
		}
		deserializeStream(in);		
		return true;
	}
	
	/**
	 * Deserializes words from input stream
	 */
	@SuppressWarnings("unchecked")
	private void deserializeStream(InputStream in) {
		
		long started = System.currentTimeMillis();
		
		dictionary = new Dictionary(type);
		Kryo kryo = KryoFactory.getKryo();
		Input input = new Input(in);
		words = (ArrayList<Word>) kryo.readClassAndObject(input);
		input.close();
		
		long done = System.currentTimeMillis();
		if (par.isPrintOutput()) {
			System.out.println(words.size()+" words loaded "+(done - started)+" ms");
		}
	}
	
	/**
	 * Indexes words in parallel threads
	 */
	private void indexWords() throws InterruptedException {
		
		new DictionaryIndexer().buildIndexes(dictionary, words);
	}
}
