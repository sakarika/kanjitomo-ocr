package net.kanjitomo.dictionary;

/**
 * How is search string matched against target string
 */
public enum SearchMode {

	/**
	 * Target string must match search string exactly
	 */
	EQUALS,
	
	/**
	 * Target string must start with search string
	 */
	STARTS_WITH,
	
	/**
	 * Target string must contain search string in any positions
	 */
	CONTAINS;
}
