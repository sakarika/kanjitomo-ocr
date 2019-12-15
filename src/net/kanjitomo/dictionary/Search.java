package net.kanjitomo.dictionary;

import static net.kanjitomo.dictionary.SearchMode.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.kanjitomo.Word;
 
/**
 * Fast search from dictionary. 
 */
public class Search {
	
	/**
	 * Target dictionary
	 */
	private Dictionary dictionary;
	
	/**
	 * String to be searched
	 */
	private String searchStr;
	
	/**
	 * Search type
	 */
	private SearchMode searchMode;
	
	/**
	 * If true, search kanji fields
	 */
	private boolean targetKanji;
	
	/**
	 * If true, search kana fields
	 */
	private boolean targetKana;
	
	/**
	 * If true, search description fields
	 */
	private boolean targetDescription;
	
	/**
	 * If true, only the first result may be returned.
	 * If false, all results are returned.
	 */
	private boolean firstResult;
	
	/**
	 * Sets the target dictionary
	 */
	public void setDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
	}
	
	/**
	 * Returns target dictionary
	 */
	public Dictionary getDictionary() {
		return dictionary;
	}
	
	/**
	 * Returns all words that contain searchStr in kanji or kana field
	 * 
	 * @param targetKanji If true, search kanji fields
	 */
	public List<Word> search(boolean targetKanji, boolean targetKana, boolean targetDescription,
			String searchStr, SearchMode searchMode, boolean firstResult) throws Exception {	
		
		this.targetKanji = targetKanji;
		this.targetKana = targetKana;
		this.targetDescription = targetDescription;
		this.searchStr = searchStr;
		this.searchMode = searchMode;
		this.firstResult = firstResult;
		
		if (!targetKanji && !targetKana && !targetDescription) {
			throw new Exception("No target field selected");
		}
		if ((targetKanji || targetKana) && targetDescription) {
			// ngram search should be expanded to cover this case
			throw new Exception("Description search can't be combined with kanji/kana search");
		}
		
		List<Word> words = search();
		if (words != null) {
			return words;
		} else {
			return new LinkedList<>();
		}
	}
	
	/**
	 * Returns all words that contain searchStr in relevant fields
	 */
	private List<Word> search() throws Exception {
		
		if (dictionary == null) {
			throw new Exception("Dictionary not set");
		}
		if (searchStr == null || searchStr.isEmpty()) {
			return new LinkedList<>();
		}

		// replace synonyms and convert to uppercase
		searchStr = CharacterUtil.toCanonical(searchStr);
		
		// find words that contain searchStr n-grams
		List<List<Word>> words = nGramSearch();
		if (words == null) {
			return null;
		}
		
		// sort word lists from short to long
		// this increases hash join (intersect) performance
		Collections.sort(words, new Comparator<List<Word>>() {
			@Override
			public int compare(List<Word> o1, List<Word> o2) {
				Integer i1 = o1.size();
				Integer i2 = o2.size();			
				return i1.compareTo(i2);
			}
		});
		
		// intersect word lists
		// merged will contain only words that are present in all lists
		List<Word> merged = null;
		for (List<Word> wordTmp : words) {
			merged = intersect(merged, wordTmp);
			if (merged.size() == 0) {
				return null;
			}
		}

		// check that searchStr is actually present in words,
		// n-grams might have been in wrong order
		if (searchStr.length() <= 2) {
			return merged;
		}
		if (targetDescription && searchStr.length() <= 3) {
			return merged;
		}
		return filterInvalidWords(merged);
	}

	/**
	 * Split searchStr into n-grams and find corresponding words from dictionary 
	 * 
	 * @param descriptions If true, trigrams are used to search words from descriptions.
	 * If false, kanji/kana digrams or monograms are used
	 * 
	 * @return List of Words that contain each n-gram. null if any n-gram doesn't contain
	 * any words.
	 */
	private List<List<Word>> nGramSearch() throws Exception{
		
		if (targetDescription) {
			return trigramSearchDescriptions();
		} else if (searchStr.length() > 1) {
			return digramSearchKanjiKana();
		} else {
			return monogramSearchKanji();
		}
	}
	
	/**
	 * Splits searchStr into trigrams and searches descriptions
	 */
	private List<List<Word>> trigramSearchDescriptions() throws Exception {
		
		List<List<Word>> words = new LinkedList<>();
		char[] chars = searchStr.toCharArray();
		for (int i=0 ; i<chars.length-2 ; i++) {
			char c1 = chars[i];
			char c2 = chars[i+1];
			char c3 = chars[i+2];
			Trigram trigram = new Trigram(c1,c2,c3);
			List<Word> newWords = dictionary.getWordsDescription(trigram);
			if (newWords == null) {
				return null;
			}
			words.add(newWords);
		}
		return words;
	}
	
	/**
	 * Splits searchStr into digrams and searches kanji and kana fields
	 */
	private List<List<Word>> digramSearchKanjiKana() throws Exception {
		
		List<List<Word>> words = new LinkedList<>();
		char[] chars = searchStr.toCharArray();
		boolean startsWith = true;
		for (int i=0 ; i<chars.length-1 ; i++) {
			char c1 = chars[i];
			char c2 = chars[i+1];
			Digram digram = new Digram(c1,c2);
			List<Word> newWords = dictionary.getWordsDigram(digram, startsWith);
			if (newWords == null) {
				return null;
			}
			words.add(newWords);
			startsWith = false;
		}
		return words;		
	}
	
	/**
	 * Searches words that contain first charater in searchStr 
	 */
	private List<List<Word>> monogramSearchKanji() throws Exception {
		
		if (searchStr.length() != 1) {
			throw new Exception("Monogram search is only for single character searchStr");
		}
		List<List<Word>> words = new LinkedList<>();
		char c = searchStr.charAt(0);
		if (!CharacterUtil.isKanji(c)) {
			return null;
		}
		List<Word> newWords = dictionary.getWordsKanji(c, searchMode == STARTS_WITH);
		if (newWords == null) {
			return null;
		}
		words.add(newWords);
		return words;		
	}	
	
    /**
     * Builds a new list that contains words in both argument lists.
     * Hash join algorithm. w1 should be shorter for better performance.
     */
    private static List<Word> intersect(List<Word> w1, List<Word> w2) {
    	
    	List<Word> results = new LinkedList<Word>();
    	
    	if (w1 == null && w2 == null) {
    		return results;
    	} else if (w1 == null) {
    		return w2;
    	} else if (w2 == null) {
    		return w1;
    	}
    	
    	// build hash set
    	Set<Word> set = new HashSet<Word>();
    	for (Word word : w1) {
    		set.add(word);
    	}

    	// probe the set
    	for (Word word : w2) {
    		if (set.contains(word) ) {
    			results.add(word);
    		}
    	}
    	
    	return results;
    }
    
    /**
     * Removes words that don't have searchStr in target fields
     */
    private List<Word> filterInvalidWords(List<Word> words) {
		
    	List<Word> filtered = new LinkedList<>();
    	for (Word word : words) {
    		if (checkWord(word)) {
    			filtered.add(word);
    			if (firstResult) {
    				return filtered;
    			}
    		} 
    	}
    	return filtered;
    }
    
    /**
     * Checks if word has searchStr in target fields
     */
    private boolean checkWord(Word word) {
    	
    	if (targetKanji && checkValue(word.kanji)) {
    		return true;
    	}
    	if (targetKana && checkValue(word.kana)) {
    		return true;
    	}
    	if (targetDescription && checkValue(word.description)) {
    		return true;
    	}
    	return false;
    }
    
    // TODO use lambda
    
    private boolean checkValue(String value) {
    	
    	value = CharacterUtil.toCanonical(value);
    	if (searchMode == EQUALS) {
    		return value.equals(searchStr);
    	}
    	if (searchMode == STARTS_WITH) {
    		return value.startsWith(searchStr);
    	} 
    	return value.contains(searchStr);
    }
}
