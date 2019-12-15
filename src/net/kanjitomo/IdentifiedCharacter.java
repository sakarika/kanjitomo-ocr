package net.kanjitomo;

import java.awt.Rectangle;
import java.util.List;

/**
 * OCR results for a single target character
 */
public class IdentifiedCharacter {

	/**
	 * List of reference characters that match the target character best, 
	 * ordered by OCR score (first character is the closest match). 
	 */
	public final String referenceCharacters;
	
	/**
	 * OCR scores for each reference character. Same order as in referenceCharacters.
	 * Higher score is better but reference characters might have been re-ordered if 
	 * first match didn't result in a valid dictionary word.
	 */
	public final List<Integer> scores;
	
	// TODO normalized scores
	
	/**
	 * Location of the character in target image's coordinates
	 */
	public final Rectangle location;
	
	public IdentifiedCharacter(String matchedCharacters, Rectangle location,
			List<Integer> scores) {
		
		this.referenceCharacters = matchedCharacters;
		this.location = location;
		this.scores = scores;
	}
}
