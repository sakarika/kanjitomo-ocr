package net.kanjitomo;

import java.io.Serializable;

import net.kanjitomo.dictionary.CharacterUtil;

/**
 * Single Japanese word loaded from Jim Breen's EDICT dictionary. 
 */
public class Word implements Serializable {

	private static final long serialVersionUID = 1L;

	public Word() {
		// Kryo needs no-arg constructor
	}
	
	/**
	 * Word in kanji form (might also contain kana characters)
	 */
	public String kanji;
	
	/**
	 * Word in kana form
	 */
	public String kana;
	
	/**
	 * English description
	 */
	public String description;
	
	/**
	 * If true, this is a common word. 
	 */
	public boolean common;
	
	/**
	 * If true, this word is from names dictionary.
	 * If false, this word is from default dictionary.
	 */
	public boolean name;
	
	/**
	 * Number of kanji characters in the kanji field
	 */
	public int kanjiCount;
	
	/**
	 * Creates a new word
	 * 
	 * @param name If true, this word is from names dictionary. If false, this word 
	 * is from default dictionary.
	 */
	public Word(String kanji, String kana, String description, boolean name) {
		
		this.kanji = kanji;
		this.kana = kana;
		this.description = description;
		this.name = name;
		
		if (description.contains("(P)")) {
			common = true;
		} else {
			common = false;
		}
		
		int kanjiCount = 0;
		for (char c : kanji.toCharArray()) {
			if (CharacterUtil.isKanji(c)) {
				++kanjiCount;
			}
		}
		this.kanjiCount = kanjiCount;
	}

	@Override
	public boolean equals(Object obj) {
		Word w = (Word)obj;
		return kanji.equals(w.kanji) && kana.equals(w.kana); 
	}
	
	@Override
	public int hashCode() {
		return kanji.hashCode() + kana.hashCode();
	}
	
	@Override
	public String toString() {
		return kanji+" "+kana;
	}
}
