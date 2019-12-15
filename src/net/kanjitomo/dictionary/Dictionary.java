package net.kanjitomo.dictionary;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.kanjitomo.DictionaryType;
import net.kanjitomo.Word;

/**
 * Set of words accessed through indexes.
 */
public class Dictionary {
	
	public final DictionaryType type;
	
	public Dictionary(DictionaryType type) {
		
		this.type= type;
		kanjiContains = new HashMap<Character, List<Word>>();
		kanjiStartsWith = new HashMap<Character, List<Word>>();
		digramContains = new HashMap<Digram, List<Word>>();
		digramStartsWith = new HashMap<Digram, List<Word>>();
		descriptionIndex = new HashMap<Trigram, List<Word>>();		
	}
	
	/**
	 * Kanji -> List of words that start with kanji monogram. 
	 * Kana monograms are not indexed and seach for single kana is not supported. 
	 */
	HashMap<Character, List<Word>> kanjiStartsWith;
	
	/**
	 * Kanji -> List of words that contain kanji monogram (not in first position) 
	 * Kana monograms are not indexed and seach for single kana is not supported. 
	 */
	HashMap<Character, List<Word>> kanjiContains;
	
	/**
	 * Digram -> List of words that start with digram
	 * This is the most commonly used index since pair of kanji characters is
	 * very selective without consuming too much memory.
	 */
	HashMap<Digram, List<Word>> digramStartsWith;
	
	/**
	 * Digram -> List of words that start with digram (not in first position)
	 * This is the most commonly used index since pair of kanji characters is
	 * very selective without consuming too much memory.
	 */
	HashMap<Digram, List<Word>> digramContains;
	
	/**
	 * Trigram in description -> List of words containing the alphabet trigram.
	 */
	HashMap<Trigram, List<Word>> descriptionIndex;
	
	// status flags that determine if indexing is still running
	volatile boolean kanjiIndexingDone = false;
	volatile boolean digramIndexingDone = false;
	volatile boolean descriptionIndexingDone = false;
	
	/**
	 * How long to wait for indexing until throwing timeout error (in milliseconds).
	 * This should be long enough that even the slowest computer should have enough time to
	 * finish indexing.
	 */
	private static int MAX_WAIT_TIME = 10000;
	
	/**
	 * How often is indexing status polled (in milliseconds)
	 */
	private static int WAIT_RESOLUTION = 50;
	
	/**
	 * Gets all words that contain the kanji
	 * 
	 * @param startsWith Only words that start with the kanji are returned, else all words
	 * that contain kanji are returned
	 */
	public List<Word> getWordsKanji(Character kanji, boolean startsWith) throws Exception {
		
		// wait if indexing is not done
		int waitTime = 0;
		while (!kanjiIndexingDone) {
			Thread.sleep(WAIT_RESOLUTION);
			waitTime += WAIT_RESOLUTION;
			if (waitTime > MAX_WAIT_TIME) {
				throw new Exception("Timeout waiting for indexer");
			}
		}

		// return results in a new list so that it can be safely modified
		// LinkedList is returned instead of ArrayList since get(int index) is not needed
		// and list manipulation is faster
		// https://stackoverflow.com/questions/322715/when-to-use-linkedlist-over-arraylist-in-java
		List<Word> result = new LinkedList<>();
		result.addAll(wrapNull(kanjiStartsWith.get(kanji)));
		if (!startsWith) {
			result.addAll(wrapNull(kanjiContains.get(kanji)));
		}
		
		return result;
	}
	
	/**
	 * Gets all words that contain the digram in kanji or kana field
	 * 
	 * @param startsWith Only words that start with the dg are returned, else all words
	 * that contain dg are returned
	 */
	public List<Word> getWordsDigram(Digram dg, boolean startsWith) throws Exception {
		
		// wait if indexing is not done
		int waitTime = 0;
		while (!digramIndexingDone) {
			Thread.sleep(WAIT_RESOLUTION);
			waitTime += WAIT_RESOLUTION;
			if (waitTime > MAX_WAIT_TIME) {
				throw new Exception("Timeout waiting for indexer");
			}
		}
		
		// return results in a new list
		List<Word> result = new LinkedList<>();
		result.addAll(wrapNull(digramStartsWith.get(dg)));
		if (!startsWith) {
			result.addAll(wrapNull(digramContains.get(dg)));
		}
		
		return result;
	}
	
	/**
	 * Gets all words that contain the trigram in description field
	 */
	public List<Word> getWordsDescription(Trigram tg) throws Exception {
		
		// wait if indexing is not done
		int waitTime = 0;
		while (!descriptionIndexingDone) {
			Thread.sleep(WAIT_RESOLUTION);
			waitTime += WAIT_RESOLUTION;
			if (waitTime > MAX_WAIT_TIME) {
				throw new Exception("Timeout waiting for indexer");
			}
		}
		
		// startsWith not supported for descriptions
		
		List<Word> result = new LinkedList<>();
		result.addAll(wrapNull(descriptionIndex.get(tg)));

		return result;
	}
	
	/**
	 * Returns empty list if null argument
	 */
	private static List<Word> wrapNull(List<Word> words) {
		
		if (words == null) {
			return new LinkedList<>();
		} else {
			return words;
		}
	}
	
	@Override
	public String toString() {
		return type.toString();
	}
}
