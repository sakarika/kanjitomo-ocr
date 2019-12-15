package net.kanjitomo.dictionary;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.kanjitomo.Word;

/**
 * Sorts word list in ascending order
 */
public class SortWords {
	    
    /**
     * Sorts word list in ascending order
     * 
     * @param searchStr Words with this this as kanji (exact match) are moved to front
     */
    public static void sort(List<Word> words, final String searchStr) {
    	
    	if (words == null || words.size() <= 1) {
    		return;
    	}

    	// sort words
    	Collections.sort(words, new Comparator<Word>() {
    		
			public int compare(Word w1, Word w2) {
				
				Integer i1;
				Integer i2;
				Boolean b1;
				Boolean b2;
				
				// equals
				b1 = w1.kanji.equals(searchStr) || w1.kana.equals(searchStr);
				b2 = w2.kanji.equals(searchStr) || w2.kana.equals(searchStr);
				if (b1 != b2) {
					return -1*b1.compareTo(b2);
				}
				
				// kanji count
				i1 = w1.kanjiCount;
				i2 = w2.kanjiCount;
				if (i1 != i2) {
					return i1.compareTo(i2);
				}				
				
				// priority words
				i1 = getPriority(w1);
				i2 = getPriority(w2);
				if (i1 != i2) {
					return i1.compareTo(i2);
				}
				
				// trailing common kana
				i1 = trailingKanaCount(searchStr, w1);
				i2 = trailingKanaCount(searchStr, w2);
				// must be at least 2, else these's too big chance that matches next word
				if (i1 == 1) {i1 = 0;}
				if (i2 == 1) {i2 = 0;}
				if (i1 != i2) {
					return -1*i1.compareTo(i2);
				}
				
				// kanji length
				i1 = w1.kanji.length();
				i2 = w2.kanji.length();
				if (i1 != i2) {
					return i1.compareTo(i2);
				}
				
				// kana length
				i1 = w1.kana.length();
				i2 = w2.kana.length();
				if (i1 != i2) {
					return i1.compareTo(i2);
				}
				
				// common words
				if (w1.common && !w2.common) {
					return -1;
				} else if (!w1.common && w2.common) {
					return 1;
				}
				
				// ends with う
				b1 = getLastChar(w1) == 'う';
				b2 = getLastChar(w2) == 'う';
				if (b1 != b2) {
					return -1*b1.compareTo(b2);
				}
							
				return 0;
	    	}
		});
    }
    
    /**
     * Counts the number of kana characters at end of match after last kanji
     */
    private static int trailingKanaCount(String searchStr, Word word) {
    	
    	if (word.kanjiCount == 0) {
    		return 0;
    	}
    	
    	int kanaRun = 0;
    	for (int i = 0 ; i < searchStr.length() && i < word.kanji.length() ; i++) {
    		if (searchStr.charAt(i) != word.kanji.charAt(i)) {
    			break;
    		}
    		if (CharacterUtil.isKana(searchStr.charAt(i))) {
    			++kanaRun;
    		} else {
    			kanaRun = 0;
    		}
    	}
    	
    	return kanaRun;
    }
    
    /**
     * Gets last character from word.kanji
     */
    private static char getLastChar(Word word) {
    	return word.kanji.charAt(word.kanji.length()-1);
    }
    
    /**
     * Maximum size of priority list.
     */
    private static int PRIORITY_LIST_MAX_SIZE = 100;
    // TODO allow larger list and index words
    
    /**
     * Priority words. Lower index -> higher priority.
     */
    private static LinkedList<Word> priorityWords = new LinkedList<Word>();
    
    /**
     * Adds a word into the front of priority list. This word has higher priority than other word.
     */
    public static void addFirstPriority(Word word) {
    	
    	priorityWords.remove(word);
    	priorityWords.addFirst(word);
    	
    	while (priorityWords.size() > PRIORITY_LIST_MAX_SIZE) {
    		priorityWords.removeLast();
    	}
    }
    
    /**
     * Adds a word into end of priority list. This word has lower priority than other word.
     */
    public static void addLastPriority(Word word) {
    	
    	if (priorityWords.size() >= PRIORITY_LIST_MAX_SIZE) {
    		return;
    	}
    	
    	priorityWords.remove(word);
    	priorityWords.addLast(word);
    }
    
    /**
     * Gets word's priority, lower is better
     */
    private static int getPriority(Word word) {
    
    	int index = priorityWords.indexOf(word);
    	if (index != -1) {
    		return index;
    	} else {
    		return priorityWords.size();
    	}    	
    }
    
    /**
     * Gets priority word in order (highest priorty first)
     */
    public static List<Word> getPriorityWords() {
    	return priorityWords;
    }
}
