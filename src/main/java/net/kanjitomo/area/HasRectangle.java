package net.kanjitomo.area;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Rectangle on target image
 */
public interface HasRectangle {

	public Rectangle getRectangle();
	
	/**
	 * Center of the rectangle
	 */
	public Point getMidpoint();
}
