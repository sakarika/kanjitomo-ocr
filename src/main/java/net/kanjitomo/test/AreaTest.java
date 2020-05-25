package net.kanjitomo.test;

import java.awt.Rectangle;

/**
 * Tests that represents correct area or column in the target image.
 */
public class AreaTest extends Test {

	public final Rectangle rect;
	
	/**
	 * @param rect Expected area on target image
	 */
	public AreaTest(Rectangle rect) {
		
		this.rect = rect;
	}
	
	public AreaTest(int x, int y, int width, int height) {
		
		rect = new Rectangle(x, y, width, height);
	}
	
	@Override
	public String toString() {
		
		return image.getName()+" area:"+rect;
	}
}
