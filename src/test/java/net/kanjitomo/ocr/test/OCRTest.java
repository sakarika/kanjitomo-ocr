package net.kanjitomo.ocr.test;

import java.awt.Point;

public class OCRTest extends Test {

	public Point point;
	public String characters;
	
	/**
	 * @param point Target image coordinates
	 * @param characters Expected character
	 */
	public OCRTest(Point point, String characters) {
		
		this.point = point;
		this.characters = characters;
	}
	
	/**
	 * @param x target image coordinates
	 * @param y
	 * @param c expected character
	 */
	public OCRTest(int x, int y, String characters) {
		
		point = new Point(x, y);
		this.characters = characters;
	}
	
	@Override
	public String toString() {
		
		return image.getName()+" ocr:"+point+":"+characters;
	}
}
