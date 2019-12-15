package net.kanjitomo.dictionary;

import java.util.HashMap;
import java.util.Map;

import net.kanjitomo.DictionaryType;

/**
 * Class for loading and holding dictinary objects
 */
public class DictionaryManager {

	private static Map<DictionaryType, Dictionary> dictionaries = new HashMap<>();
	
	/**
	 * Checks that dictionary is loaded
	 */
	public static void prepareDictionary(DictionaryType type) throws Exception {
		
		if (dictionaries.containsKey(type)) {
			return;
		}
		
		Dictionary dictionary = new DictionaryLoader(type).loadDictionary();
		dictionaries.put(type, dictionary);
	}
	
	/**
	 * Gets dictionary of given type, loads if necessary
	 * @param type
	 * @return
	 */
	public static Dictionary getDictionary(DictionaryType type) throws Exception {

		prepareDictionary(type);
		return dictionaries.get(type);
	}
}
