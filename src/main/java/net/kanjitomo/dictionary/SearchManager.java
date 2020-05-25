package net.kanjitomo.dictionary;

import static net.kanjitomo.dictionary.SearchMode.STARTS_WITH;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.kanjitomo.DictionaryType;
import net.kanjitomo.Word;
import net.kanjitomo.util.Parameters;

/**
 * Container for dictionaries and search objects
 */
public class SearchManager {
	
	private Parameters par = Parameters.getInstance();
	private Search search;
	private MultiSearch multiSearch;
	
	/**
	 * Searches words from selected dictionary.
	 * 
	 * @param searchString Search term supplied by the user or read from OCR results
	 * @return List of maching words sorted by increasing length of kanji/kana field
	 */
	public List<Word> search(String searchString, SearchMode mode) throws Exception {
				
		// search first from primary (default) dictionary
		List<Word> primary = search(searchString, mode, true);
		if (primary.size() > 0 || par.secondaryDictionary == null) {
			return primary;
		}
		
		// search from secondary (names) dictionary if no results from primary
		prepareDictionary(false);
		List<Word> secondary = search(searchString, mode, false);
		return secondary;
	}
	
	/**
	 * Searches words from selected dictionary.
	 * 
	 * @param searchStr Search term supplied by the user or read from OCR results
	 * 
	 * @param primary If true, search from primary (default) dictionary. If false,
	 * search from secondary (names) dictionary
	 * 
	 * @return List of maching words sorted by increasing length of kanji/kana field
	 */
	private List<Word> search(String searchStr, SearchMode mode, boolean primary) throws Exception {
		
		prepareDictionary(primary);
		
		if (par.isPrintOutput()) {
			System.out.println("\nSearching:"+searchStr+" mode:"+mode+" primary:"+primary+
					" dictionary:"+search.getDictionary());
		}
		
		// check if there are non-japanese characters
		boolean nonJapaneseCharacters = false;
		for (Character c : searchStr.toCharArray()) {
			if (!CharacterUtil.isKanji(c) && !CharacterUtil.isKana(c)) {
				nonJapaneseCharacters = true;
				break;
			}
		}
		
		long started = System.currentTimeMillis();
		
		// search from kanji or kana if only japanese characters,
		// else search from descriptions
		List<Word> words;
		if (!nonJapaneseCharacters) {
			words = search.search(true, true, false, searchStr, mode, false);
		} else {
			words = search.search(false, false, true, searchStr, mode, false);
		}
		SortWords.sort(words, searchStr);
		
		long stopped = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			System.out.println("Search time:"+(stopped - started)+" ms");
		}
		
		if (par.isPrintOutput()) {
			System.out.println("Words found:"+words.size());
			if (words.size() > 0) {
				System.out.println("First word:"+words.get(0));
			}
		}
		
		return words;
	}
	
	/**
	 * Search words from dictionary based on OCR results. Multiple character candidates are
	 * considered for each position. Characters might be re-ordered based on search results.
	 * 
	 * @param characters Matched characters in each position
	 * 
	 * @param ocrScore OCR scores for each position
	 * 
	 * @param maxWidth Number of character positions considered. This can be lower than
	 * characters.length() if multiple columns and search is restricted to first column
	 */
	public MultiSearchResult multiSearch(List<String> characters,
			List<List<Integer>> ocrScores, int maxWidth) throws Exception {
		
		if (maxWidth > characters.size()) {
			throw new Exception("maxWidth "+maxWidth+" larger than characters:"+characters.size());
		}
		
		// search from primary (default) dictionary
		prepareDictionary(true);
		MultiSearchResult result = multiSearch.multiSearch(characters, ocrScores, maxWidth);

		// search also from secondary (names) dictionary and compare results
		if (par.secondaryDictionary != null) {
			prepareDictionary(false);
			MultiSearchResult secondary = multiSearch.multiSearch(characters, ocrScores, maxWidth);
			result = selectBestResults(result, secondary);
		}
		
		if (result.words.size() == 0) {
			return result;
		}
		
		// cut trailing kana since search doesn't account for grammar
		prepareDictionary(result.dictionary);
		String shortSearchStr = CharacterUtil.removeTrailingKana(result.searchStr);
		if (shortSearchStr != result.searchStr) {
			result.words = search.search(true, true, false, shortSearchStr, STARTS_WITH, false);
		}
		
		// sort results
		SortWords.sort(result.words, result.searchStr);
		Word first = result.words.get(0);
		if (first.kanji.equals(shortSearchStr) || first.kana.equals(shortSearchStr)) {
			result.searchStr = shortSearchStr;
		}
		
		// move character positions and ocr scores based on multisearch results 
		// so that first characters match searchStr
		for (int i=0 ; i<result.searchStr.length() ; i++) {
			char c = result.searchStr.charAt(i);
			int ci = characters.get(i).indexOf(c);
			if (ci > 0) {
				// move character
				StringBuilder sb = new StringBuilder(characters.get(i));
				sb.deleteCharAt(ci);
				sb.insert(0, c);
				characters.set(i, sb.toString());
				// move ocr score
				Integer score = ocrScores.get(i).get(ci);
				ocrScores.get(i).remove(ci);
				ocrScores.get(i).add(0, score);
			}
		}
		
		return result;
	}
	
	/**
	 * Adds words2 to words1 list, skips duplicates
	 */
	private static void addSkipDuplicates(List<Word> words1, List<Word> words2) {
		
		Set<Word> words1Set = new HashSet<>();
		words1Set.addAll(words1);
		
		for (Word word : words2) {
			if (!words1Set.contains(word)) {
				words1.add(word);
			}
		}
	}
	
	/**
	 * Select the best result set. Primary has priority unless it's empty or 
	 * secondary has better score. 
	 */
	private MultiSearchResult selectBestResults(MultiSearchResult primary,
			MultiSearchResult secondary) {
		
		// check scores
		if (par.isPrintDebug()) {
			System.out.println("\nPrimary score:"+primary.score);
			System.out.println("Secondary score:"+secondary.score);
		}
		if (primary.score * par.defaultDictionaryBias >= secondary.score) {
			if (par.isPrintDebug()) {
				System.out.println("Primary selected");
			}
			return primary;
		} else {
			if (par.isPrintDebug()) {
				System.out.println("Secondary selected");
			}
			return secondary;
		}
	}
	
	/**
	 * Prepares dictionary for searching. Loads data from files if necessary.
	 */
	private void prepareDictionary(DictionaryType type) throws Exception {
		
		loadData();
		search.setDictionary(DictionaryManager.getDictionary(type));
	}
	
	/**
	 * Prepares dictionary for searching. Loads data from files if necessary.
	 * 
	 * @priority Used in combined dictionary more. If true -> default dictionary,
	 * if false -> names dictioary. No effect in other dictionary types.
	 */
	private void prepareDictionary(boolean primary) throws Exception {
		
		loadData();
		Dictionary dictionary = DictionaryManager.getDictionary(primary ?
				par.primaryDictionary : par.secondaryDictionary);
		search.setDictionary(dictionary);
		multiSearch.setDictionary(dictionary);
	}
		
	/**
	 * Loads dictionary files into memory
	 */
	public void loadData() throws Exception {
		
		if (search == null) {
			search = new Search();
			multiSearch = new MultiSearch();
		}
		if (par.primaryDictionary == null) {
			throw new Exception("Dictionary not selected");
		}
		DictionaryManager.prepareDictionary(par.primaryDictionary);
		if (par.secondaryDictionary != null) {
			DictionaryManager.prepareDictionary(par.secondaryDictionary);
		}
	}
	
	/**
	 * Waits until all indexers are done.
	 */
	public void waitForIndexing() throws Exception {
		
		if (par.primaryDictionary != null) {
			waitForIndexing(DictionaryManager.getDictionary(par.primaryDictionary));
		}
		if (par.secondaryDictionary != null) {
			waitForIndexing(DictionaryManager.getDictionary(par.secondaryDictionary));
		}
	}
	
	private void waitForIndexing(Dictionary dictionary) throws Exception {
		
		dictionary.getWordsKanji(null, false);
		dictionary.getWordsDigram(null, false);
		dictionary.getWordsDescription(null);
	}
}
