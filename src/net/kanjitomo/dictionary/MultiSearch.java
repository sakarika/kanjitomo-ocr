package net.kanjitomo.dictionary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.kanjitomo.Word;
import net.kanjitomo.util.Parameters; 

/**
 * Searches the dictionary with multiple candidate characters for each target 
 * character. This is needed because OCR results are not 100% reliable,
 * sometimes correct character is not in the first position.
 */
public class MultiSearch {

	private Parameters par = Parameters.getInstance();
	private Dictionary dictionary;
	
	private boolean debug = false;
	private boolean debugZeroResults = false;
	
	private List<String> ocrCharacters;
	private List<List<Float>> ocrScores;
	
	private int iteration = 0;
	private float bestScore = 0;
	private String bestSearchStr = null;
	private List<Word> bestWords = null;
	
	private int position=0;
	private List<Integer> indexes = null;
	
	private static final int MAX_DEPTH = 8;
	private static final int MAX_ITERATIONS = 1000;
	
	public void setDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
	}
	
	/**
	 * Searches the dictionary with multiple candidate characters for each target 
	 * character. This is used after OCR because results are not 100% reliable,
	 * sometimes the correct character is not in the first position.
	 * 
	 * @param ocrCharacters Each String represents a list of candidate characters
	 * for a given target character position. Characters in each string are
	 * ordered starting from best match. Ordered starting from the character
	 * closest to point (mouse cursor position).
	 * 
	 * @param ocrScores OCR scores for each candidate character indexed by position
	 * 
	 * @param maxWidth Number of character positions considered. This can be lower than
	 * characters.length() if multiple columns and search is restricted to first column
	 * 
	 */
	public MultiSearchResult multiSearch(List<String> ocrCharacters,
			List<List<Integer>> ocrScores, int maxWidth) throws Exception {
		
		if (par.isPrintDebug()) {
			System.out.println("\nMultiSearch "+dictionary);
		}
		
		long started = System.currentTimeMillis();
		debug = par.isPrintDebug();
		
		iteration = 0;
		bestScore = 0;
		bestSearchStr = null;
		bestWords = null;
		indexes = new ArrayList<>();
		indexes.add(0);
		position = 0;
		
		this.ocrCharacters = ocrCharacters;
		this.ocrScores = normalizeScores(ocrScores);
		
		// iterate through all character combinations
		// calculate score and keep the best
		if (debug) {System.out.println("\nMultiSearch scores");}
		loop: while (true) {
			//System.out.println("iteration:"+iteration+" position:"+position+" indexes:"+buildIndexStr()+" searchStr:"+buildSearchString());
			if (++iteration >= MAX_ITERATIONS) {
				break;
			}
			boolean resultFound = checkCombination();
			if (resultFound && position+1 < ocrCharacters.size() && position+1 < maxWidth) {
				// advance to next position (add new character to searchStr)
				position++;
				indexes.add(0);
				continue loop;
			}
			while (position >= 0) {
				int next = indexes.get(position)+1; 
				if (next < ocrCharacters.get(position).length() && next < MAX_DEPTH) {
					// advance to next candidate character in rightmost position
					indexes.set(position, next);
					continue loop;
				} else {
					// backtrack to previous position and advance to next character
					indexes.remove(position);
					position--;
				}
			}
			// all combinations checked
			break;
		}
		long done = System.currentTimeMillis();
		if (debug) {System.out.println("Iterations:"+iteration+" "+(done - started)+" ms");}
		
		if (bestScore == 0) {
			// this is unlikely to happen, at least there should be search 
			// results for single characters
			return new MultiSearchResult(new ArrayList<Word>(), "", 0, null);
		}
		
		return new MultiSearchResult(bestWords, bestSearchStr, bestScore, dictionary.type);
	}
	
	/**
	 * List of words found at given position
	 */
	private List<List<Word>> wordsAtPosition = new ArrayList<>();
	{
		for (int i=0 ; i<par.ocrMaxCharacters ; i++) {
			wordsAtPosition.add(null);
		}
	}
	
	/**
	 * Check if the current combination produces search results and if the
	 * score is better than previous combinations
	 * 
	 * @return true if a result was found or should continue to next position
	 */
	private boolean checkCombination() throws Exception {
		
		List<Word> words;
		if (position == 0) {
			if (CharacterUtil.isKanji(ocrCharacters.get(0).charAt(indexes.get(0)))) {
				words = startsWithKanjiMonogram();
			} else {
				// kana monograms are not indexed, continue to next position
				return true;
			}
		} else if (position == 1){
			words = startsWithDigram();
		} else {
			words = filter(wordsAtPosition.get(position-1));
		}
		
		// stop if no words found
		if (words.size() == 0) {
			if (debug && debugZeroResults) {System.out.println(buildIndexStr()+","+buildSearchString()+"\t");}
			return false;
		}
		
		// save word list for next round
		wordsAtPosition.set(position, words);
		
		// check score
		// keep the best score
		float score = calcScore();
		if (debug) {System.out.print(buildIndexStr()+","+buildSearchString()+"\t\t"+scoreStr);}
		if (score > bestScore) {
			// full startsWith check is still needed because per character kanji/kana checks
			// might have been inconsistent
			String searchStr = buildSearchString();
			filterFull(words, searchStr);
			// TODO update score with kanji/kana equals
			if (words.size() > 0) {
				if (debug) {System.out.print("\tbest");}
				bestSearchStr = searchStr;
				bestScore = score;
				bestWords = words;
			}
		}
		if (debug) {System.out.println();}
		
		return true;
	}
	
	private String buildIndexStr() {
		
		String indexStr = "";
		for (Integer index : indexes) {
			indexStr += index+",";
		}
		indexStr = indexStr.substring(0, indexStr.length()-1);
		
		return indexStr;
	}
	
	/**
	 * Find words that start with kanji monogram at current index position
	 */
	private List<Word> startsWithKanjiMonogram() throws Exception {
		
		char c = ocrCharacters.get(0).charAt(indexes.get(0));
		c = CharacterUtil.toCanonical(c);
		
		return dictionary.getWordsKanji(c, true);
	}
	
	/**
	 * Find words that start with digram at current index position
	 */
	private List<Word> startsWithDigram() throws Exception {
		
		char c1 = ocrCharacters.get(0).charAt(indexes.get(0));
		c1 = CharacterUtil.toCanonical(c1);
		char c2 = ocrCharacters.get(1).charAt(indexes.get(1));
		c2 = CharacterUtil.toCanonical(c2);
		
		Digram dg = new Digram(c1, c2);
		return dictionary.getWordsDigram(dg, true);
	}
	
	/**
	 * Removes words that don't contain correct character at current position
	 */
	private List<Word> filter(List<Word> words) {

		char c = ocrCharacters.get(position).charAt(indexes.get(position));
		c = CharacterUtil.toCanonical(c);
		
		List<Word> words2 = new LinkedList<>();
		for (Word word : words) {
			if (checkCharacter(word.kanji, c) || checkCharacter(word.kana, c)) {
				words2.add(word);
			}
		}
		return words2;
	}
	
	/**
	 * Removes words that don't start with searchStr
	 */
	private void filterFull(List<Word> words, String searchStr) {
		
		searchStr = CharacterUtil.toCanonical(searchStr);
		
		Iterator<Word> i = words.iterator();
		while (i.hasNext()) {
			Word word = i.next();
			String kanji = CharacterUtil.toCanonical(word.kanji);
			if (kanji.startsWith(searchStr)) {
				continue;
			}
			String kana = CharacterUtil.toCanonical(word.kana);
			if (kana.startsWith(searchStr)) {
				continue;
			}
			i.remove();
		}
	}
	
	private boolean checkCharacter(String str, char c) {
		
		if (str.length() <= position) {
			return false;
		}
		char c2 = str.charAt(position);
		c2 = CharacterUtil.toCanonical(c2);
		if (c2 == c) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Scales the score lists so that the best score is always 1.0
	 */
	private List<List<Float>> normalizeScores(List<List<Integer>> scores) {

		// scale scores to maxDepth+1	
		for (int i=0 ; i<scores.size() ; i++) {
			
			int referenceScore = scores.get(i).get(MAX_DEPTH+1);
			if (referenceScore >= 0) {
				continue;
			}
			for (int j=0 ; j<scores.get(i).size() ; j++) {
				int score = scores.get(i).get(j);
				int newScore = score - referenceScore;
				scores.get(i).set(j, newScore);
			}
		}
		
		// build new lists with normalized scores
		List<List<Float>> normalizedScores = new ArrayList<List<Float>>();
		for (int i=0 ; i<scores.size() ; i++) {
			
			int bestScore = scores.get(i).get(0);
			List<Float> newList = new ArrayList<Float>();
			for (int j=0 ; j<scores.get(i).size() && j<MAX_DEPTH ; j++) {
				int score = scores.get(i).get(j);
				float newScore = 1.0f*score / bestScore;
				newList.add(newScore);
			}
			normalizedScores.add(newList);
		}
		
		if (debug) {
			System.out.println("\nNormalized scores");
			for (int i=0 ; i<normalizedScores.size() ; i++) {
				for (int j=0 ; j<normalizedScores.get(i).size() ; j++) {
					char c = ocrCharacters.get(i).charAt(j);
					float score = normalizedScores.get(i).get(j);
					System.out.print(c+":"+score+"\t");
				}
				System.out.println();
			}
		}
		
		return normalizedScores;
	}
	
	/**
	 * Characters with OCR score below this are rejected
	 */
	private static float MIN_SCORE = 0.8f;
		
	private String scoreStr = "";
	
	/**
	 * Calculates score for given indexes without equals bonuses
	 */
	private float calcScore() {
		
		int kanjiCount = 0;
		int kanaCount = 0;
		float scoreSum = 0f;
		for (int i=0 ; i<indexes.size() ; i++) {
			int index = indexes.get(i);
			float score = ocrScores.get(i).get(index);
			if (score < MIN_SCORE) {
				return 0;
			}
			scoreSum += (float) Math.pow(score, 1.5f);
			if (CharacterUtil.isKanji(ocrCharacters.get(i).charAt(index))) {
				++kanjiCount;
			} else {
				++kanaCount;
			}
		}
		float avgScore = scoreSum / indexes.size();
		float countFactor = (float) Math.pow(kanjiCount + 0.5f*kanaCount, 0.11f);
		float score = avgScore * countFactor;
				
		if (par.isPrintDebug()) {
			scoreStr = String.format("[%.2f %.2f] -> %.2f",
					avgScore, countFactor, score);
		}
		
		return score; 
	}
	
	// TODO score including kanji/kana equals bonuses

	// TODO limit search depth

	/**
	 * Builds searchStr from current indexes
	 */
	private String buildSearchString() {
		
		StringBuilder sb = new StringBuilder();
		for (int i=0 ; i<indexes.size() ; i++) {
			Integer index = indexes.get(i);
			sb.append(ocrCharacters.get(i).charAt(index));
		}
		return sb.toString();
	}
}
