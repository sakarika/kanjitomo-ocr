package net.kanjitomo.dictionary;

/**
 * Utility methods for classifying characters
 */
public class CharacterUtil {

	// https://stackoverflow.com/questions/19899554/unicode-range-for-japanese
	
    public static boolean isHiragana(char c) {
    	
    	if (c == '｜') {
    		return true;
    	}
    	int codepoint = (int)c;
    	return codepoint >= 0x3040 && codepoint <= 0x309F; 
    }
    
    public static boolean isKatakana(char c) {
    	
    	if (c == '｜') {
    		return true;
    	}
    	int codepoint = (int)c;
    	return codepoint >= 0x30A0 && codepoint <= 0x30FF; 
    }

    public static boolean isKana(char c) {
    	
    	return isHiragana(c) || isKatakana(c); 
    } 
    
    public static boolean isKanji(char c) {
    	
    	int codepoint = (int)c;
    	if (c == 0x3005) { // 々
    		return true;
    	}
    	return codepoint >= 0x4E00 && codepoint <= 0x9FAF; 
    }
    
    /** 
     * Converts katakana to hiragana.
     * Returns the same character if not katakana.
     */
    private static char toHiragana(char c) {
    	
    	// from http://en.wikipedia.org/wiki/Kana#Kana_in_Unicode
    	if (!isKatakana(c))
    		return c;
    	int codepoint = (int)c;
    	if (codepoint > 0x30F6) {
    		return c;
    	}
    	codepoint -= 0x60;
    	return (char)codepoint;
    }
    
    /**
     * Converts katakana to hiragana, replaces synonyms and converts
     * to upper case (if alphabet).
     */
    public static String toCanonical(String str) {
    
    	StringBuilder sb = new StringBuilder();
    	for (char c : str.toCharArray()) {
    		char converted = toCanonical(c);
    		sb.append(converted);
    	}
    	
    	return sb.toString().toUpperCase();
    }
    
    /**
     * Converts katakana to hiragana, replaces synonyms and converts
     * to upper case (if alphabet).
     */
    public static char toCanonical(char c) {
    
    	char converted = toHiragana(c);
		if (converted == 'っ') {
			converted = 'つ';
		} else if (converted == 'ゃ') {
			converted = 'や';
		} else if (converted == 'ゅ') {
			converted = 'ゆ';
		} else if (converted == 'ょ') {
			converted = 'よ';
		} else if (converted == 'タ') {
			converted = '夕';
		} else if (converted == 'ロ') {
			converted = '口';
		} else if (converted == '|') {
			converted = '一';
		} else if (converted == '｜') {
			converted = '一';
		} else if (converted == 'ー') {
			converted = '一';
		} 
		return converted;
    }
    
    /**
     * Removes kana characters from the end of str.
     * Does nothing if all characters are kana.
     */
    public static String removeTrailingKana(String str) {
    	
    	int newLength = str.length();
 		for (int i=str.length()-1 ; i>=0 ; i--) {
 			if (CharacterUtil.isKana(str.charAt(i))) {
 				newLength--;
 			} else {
 				break;
 			}
 		}
 		if (newLength > 0) {
 			str = str.substring(0, newLength);
 		}
 		return str;
    }
    
    /**
     * Returns true if str has trailing kana.
     */
    public static boolean hasTrailingKana(String str) {
    	
    	int trailKanaCount = 0;
 		for (int i=str.length()-1 ; i>=0 ; i--) {
 			if (CharacterUtil.isKana(str.charAt(i))) {
 				trailKanaCount++;
 			} else {
 				break;
 			}
 		}
 		if (trailKanaCount == 0 || trailKanaCount == str.length()) {
 			return false;
 		} else {
 			return true;
 		}
    }
}
