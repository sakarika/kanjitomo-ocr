package net.kanjitomo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * List of areas inside a single column (or row in horizontal orientation)
 */
public class Column {

	// this is a simplified version of net.kanjitomo.area.Column intended to be used
	// as a result object from KanjiTomo class
	
	/**
	 * Rectangles around characters in this column.
	 * Ordered in reading direction (top-down or left-right). 
	 */
	public List<Rectangle> areas;
	
	/**
	 * Bounding box around areas
	 */
	public Rectangle rect;
	
	/**
	 * Next column in reading direction
	 */
	public Column nextColumn;
	
	/**
	 * Previous column in reading direction
	 */
	public Column previousColumn;	
	
	/**
	 * If true, this column has vertical reading direction. If false, horizontal.
	 */
	public boolean vertical;
	
	/**
	 * If true, this column contains furigana characters
	 */
	public boolean furigana;
	
	/**
	 * Furigana columns next to this column
	 */
	public List<Column> furiganaColumns = new ArrayList<Column>();
	
	@Override
	public String toString() {
		return "rect:"+rect+" areas:"+areas.size()+" vertical:"+vertical+" furigana:"+furigana;
	}
}
