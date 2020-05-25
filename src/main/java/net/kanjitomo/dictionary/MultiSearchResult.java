package net.kanjitomo.dictionary;

import java.util.List;

import net.kanjitomo.DictionaryType;
import net.kanjitomo.Word;

public class MultiSearchResult {

	/**
	 * List of words found after search with searchStr
	 */
	public List<Word> words;
	
	/**
	 * The actual search string used for the search.
	 */
	public String searchStr;
	
	/**
	 * Score that represents the length and quality of this match.
	 * Larger is better.
	 */
	public float score;
	
	/**
	 * Which dictionary was used for searching
	 */
	public DictionaryType dictionary;
	
	public MultiSearchResult(List<Word> words, String searchStr, float score, DictionaryType dictionary) {
		
		this.words = words;
		this.searchStr = searchStr;
		this.score = score;
		this.dictionary = dictionary;
	}
}
