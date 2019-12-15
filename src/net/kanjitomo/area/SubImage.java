package net.kanjitomo.area;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Image that contains single character cropped from original image
 */
public class SubImage {
	
	/**
	 * Cropped around single character
	 */
	public BufferedImage image;
	
	/**
	 * Location of the character in original image.
	 */
	public Rectangle location;
	
	/**
	 * Column that contains the rectangle. This can be null if manual rectangles
	 * are used. 
	 */
	public Column column;
	
	public SubImage(BufferedImage binaryImage, Rectangle location, Column column) {
		
		this.image = binaryImage;
		this.location = location;
		this.column = column;
	}
	
	public boolean isVertical() {
		
		if (column != null) {
			return column.vertical;
		} else {
			return true;
		}
	}
	
	public int getMinX() {
		return location.x;
	}
	
	public int getMaxX() {
		return location.x + location.width-1;
	}
	
	public int getMinY() {
		return location.y;
	}
	
	public int getMaxY() {
		return location.y + location.height-1;
	}
	
	public int getMidX() {
		return location.x + location.width/2;
	}
	
	public int getMidY() {
		return location.y + location.height/2;
	}
	
	@Override
	public String toString() {
		return location.toString();
	}	
}
