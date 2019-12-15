package net.kanjitomo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Results of the OCR run
 */
public class OCRResults {

	/**
	 * String of best matches starting from OCR target point.
	 */
	public final String bestMatchingCharacters;
	
	/**
	 * List of characters identified by OCR algorithm. First character in each element
	 * is added to bestMatchingCharacters. 
	 */
	public final List<IdentifiedCharacter> characters;
	
	/**
	 * String used in dictionary search
	 */
	public final String searchString;
	
	/**
	 * Results of dictionary search from best matching characters.
	 * Sorted roughly by ascending length.
	 */
	public final List<Word> words;
	
	/**
	 * If true, vertical orientation was used as reading direction.
	 * If false, horizontal orientation was used.
	 */
	public final boolean vertical;
	
	public OCRResults(List<String> characters, List<Rectangle> locations,
		List<List<Integer>> scores, List<Word> words, String searchString, boolean vertical) {
		
		this.characters = new ArrayList<IdentifiedCharacter>();
		String bestMatchingCharacters = "";
		for (int i=0 ; i<characters.size() ; i++) {
			IdentifiedCharacter character = new IdentifiedCharacter(characters.get(i), locations.get(i), scores.get(i)); 
			bestMatchingCharacters += character.referenceCharacters.charAt(0);
			this.characters.add(character);
		}
		this.bestMatchingCharacters = bestMatchingCharacters;
		this.words = words;
		this.searchString = searchString;
		this.vertical = vertical;
	}
	
	@Override
	public String toString() {
	
		StringBuilder sb = new StringBuilder();
		
		sb.append("\nCharacters:\n");
		for (IdentifiedCharacter character : characters) {
			sb.append(character.referenceCharacters+"\n");
		}
		
		sb.append("\nLocations:\n");
		for (IdentifiedCharacter character : characters) {
			sb.append(character.location+"\n");
		}
		
		if (searchString != null) {
			sb.append("\nSearch string:"+searchString+"\n");
			sb.append("Words found:"+words.size()+"\n");
			sb.append("First word:");
			if (words.size() > 0) {
				sb.append(words.get(0));
			}
		}
		
		return sb.toString();
	}
}
