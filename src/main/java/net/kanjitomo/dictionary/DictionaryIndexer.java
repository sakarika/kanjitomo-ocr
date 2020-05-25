package net.kanjitomo.dictionary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.kanjitomo.Word;
import net.kanjitomo.util.Parameters;

/**
 * Builds dictionary indexes.
 */
public class DictionaryIndexer {

	private Parameters par = Parameters.getInstance();
	private Dictionary dictionary;
	private List<Word> words;
	
	public void buildIndexes(Dictionary dictionary, List<Word> words) throws InterruptedException {
		
		this.dictionary = dictionary;
		this.words = words;
		
		// create threads
		List<Indexer> threads = new ArrayList<>();
		for (int i=1 ; i<=3 ; i++) {
			Indexer indexer = new Indexer(i);
			indexer.start();
			threads.add(indexer);
		}
		
		// indexing is done in the background,
		// get methods in Dictionary object wait if indexing is not ready
	}
	
	/**
	 * Thread for indexing kanji, digram or description maps in the background
	 */
	class Indexer extends Thread {
		
		final int role;
		
		/**
		 * @param role 1 = Index kanji, 2 = Index kana, 3 = Index descriptions
		 */
		public Indexer(int role) {
			
			this.role = role;
		}
		
		@Override
		public void run() {
			
			long started = System.currentTimeMillis();
			for (Word word : words) {
				if (role == 1) {
					addToKanjiIndex(word);
				} else if (role == 2) {
					addToDigramIndex(word);
				} else {
					addToDescriptionIndex(word);
				}
			}
			if (role == 1) {
				dictionary.kanjiIndexingDone = true;
			} else if (role == 2) {
				dictionary.digramIndexingDone = true;
			} else {
				dictionary.descriptionIndexingDone = true;
			}
			long done = System.currentTimeMillis();
			if (par.isPrintDebug()) {
				System.out.println(dictionary.type+" indexer thread:"+role+" done "+(done - started)+" ms");
			}
		}
	}
	
	private void addToKanjiIndex(Word word) {
		
		// startsWith
		Character c = CharacterUtil.toCanonical(word.kanji.charAt(0));
		if (!CharacterUtil.isKanji(c)) {
			return;
		}
		List<Word> words = dictionary.kanjiStartsWith.get(c);
		if (words == null) {
			words = new ArrayList<Word>();
			dictionary.kanjiStartsWith.put(c, words);
		}
		words.add(word);
		
		// contains
		Set<Character> added = new HashSet<>();
		char[] str = CharacterUtil.toCanonical(word.kanji).toCharArray();
		for (int i=1 ; i<str.length ; i++) {
			c = str[i];
			if (!CharacterUtil.isKanji(c)) {
				continue;
			}
			if (added.contains(c)) {
				continue;
			}
			words = dictionary.kanjiContains.get(c);
			if (words == null) {
				words = new ArrayList<Word>();
				dictionary.kanjiContains.put(c, words);
			}
			words.add(word);
			added.add(c);
		}
	}
	
	private Set<Digram> added;
	
	private void addToDigramIndex(Word word) {
		
		added = new HashSet<>();
		addToDigramIndex(word, CharacterUtil.toCanonical(word.kanji));
		addToDigramIndex(word, CharacterUtil.toCanonical(word.kana));
	}
	
	private void addToDigramIndex(Word word, String str) {
	
		if (str.length() < 2) {
			return;
		}
		
		// startsWith
		String str2 = str.substring(0, 2);
		Digram dg = new Digram(str2.charAt(0), str2.charAt(1));
		List<Word> words = dictionary.digramStartsWith.get(dg);
		if (words == null) {
			words = new ArrayList<Word>();
			dictionary.digramStartsWith.put(dg, words);
		}
		words.add(word);
		
		// contains
		char[] chars = str.toCharArray();
		for (int i=1 ; i<chars.length-1 ; i++) {
			char c1 = chars[i];
			char c2 = chars[i+1];
			dg = new Digram(c1,c2);
			if (added.contains(dg)) {
				continue;
			}
			words = dictionary.digramContains.get(dg);
			if (words == null) {
				words = new ArrayList<Word>();
				dictionary.digramContains.put(dg, words);
			}
			words.add(word);
			added.add(dg);
		}	
	}
	
	private void addToDescriptionIndex(Word word) {
		
		char[] chars = word.description.toCharArray();
		for (int i=0 ; i<chars.length-2 ; i++) {
			char c1 = Character.toUpperCase(chars[i]);
			char c2 = Character.toUpperCase(chars[i+1]);
			char c3 = Character.toUpperCase(chars[i+2]);
			Trigram trigram = new Trigram(c1,c2,c3);
			List<Word> words = dictionary.descriptionIndex.get(trigram);
			if (words == null) {
				words = new ArrayList<Word>();
				dictionary.descriptionIndex.put(trigram, words);
			}
			words.add(word);
		}
	}
}
