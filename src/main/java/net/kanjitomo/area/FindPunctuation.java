package net.kanjitomo.area;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Finds and marks areas that represent punctuation. 
 * These areas are not merged with other areas.
 */
public class FindPunctuation extends AreaStep {
	
	public FindPunctuation(AreaTask task) {
		super(task, "punctuation");
	}
	
	@Override
	protected void runImpl() throws Exception {
		
		for (Column col : task.columns) {
			markBrackets(col);
			markDotCom(col);
		}
	}
	
	private void markBrackets(Column col) {
		
		for (Area area : col.areas) {
			if (isBracket(area, col)) {
				area.punctuation = true;
				area.changed = true;
			}
		}
	}
	
	/** Sub-area that must contain at least one pixel */
	private Rectangle square = new Rectangle();
	
	/** square width and height (relative to area size). */
	private static final float TEST_SQUARE_SIZE = 0.15f;
	private int squareSize;
	
	/** Sub-area that must be empty */
	private Triangle triangle = new Triangle();
	
	/** triangle width and height (relative to area size). */
	private static final float TEST_TRIANGLE_SIZE = 0.55f;
	private int triangleWidth;
	private int triangleHeight;
	
	// area coordinates
	private int minX;
	private int midX;
	private int maxX;
	private int minY;
	private int midY;
	private int maxY;
	
	// area corner squares (true -> square has at least one pixel)
	boolean ne = false;
	boolean nw = false;
	boolean se = false;
	boolean sw = false;
	
	/**
	 * Tests if area contains a bracket:｢［【〈｛(   or rotational equivalent
	 */
	private boolean isBracket(Area area, Column col) {
		
		if (area.getMajorMinorRatio() > 0.44f || area.getMajorDim() < 0.1f) {
			return false;
		}
		
		if (area.getMinDim() <= 2) {
			// TODO low-resolution version for [ and ｢ brackets
			return false;
		}
		
		// calculate test polygon sizes
		squareSize = (int) Math.ceil(area.getMinDim()*TEST_SQUARE_SIZE);
		square.width = squareSize;
		square.height = squareSize;
		triangleWidth = (int) Math.floor(area.getWidth()*TEST_TRIANGLE_SIZE);
		triangleHeight = (int) Math.floor(area.getHeight()*TEST_TRIANGLE_SIZE);
		
		// mark area extremes for easy reference
		minX = area.getX();
		midX = area.getMidpoint().x;
		maxX = area.getMaxX();
		minY = area.getY();
		midY = area.getMidpoint().y;
		maxY = area.getMaxY();
		
		// check corner squares
		square.x = maxX - squareSize + 1;
		square.y = minY;
		ne = testSquare(square);
		square.x = minX;
		square.y = minY;
		nw = testSquare(square);
		square.x = maxX - squareSize + 1;
		square.y = maxY - squareSize + 1;
		se = testSquare(square); 
		square.x = minX;
		square.y = maxY - squareSize + 1;
		sw = testSquare(square);
		
		if (col.vertical) {
			return isBracketHorizontal(area);
		} else {
			return isBracketVertical(area);
		}
	}
	
	/**
	 * Tests if area contains a horizontal bracket (for example: ﹁ or ﹂)
	 */
	private boolean isBracketHorizontal(Area area) {
		
		int triangleMinX = minX + (area.getWidth()-triangleWidth)/2;
		int triangleMaxX = maxX - (area.getWidth()-triangleWidth)/2;
		
		// top bracket ﹁  or horizontal (
		if (se && (ne || sw)) {
			triangle.v1 = new Point(midX, maxY - triangleHeight);
			triangle.v2 = new Point(triangleMaxX, maxY);
			triangle.v3 = new Point(triangleMinX, maxY);
			if (!testTriangle(triangle)) {
				return true;
			}
		}
		
		// bottom bracket ﹂ or horizontal )
		if (nw && (sw || ne)) {
			triangle.v1 = new Point(midX, minY + triangleHeight);
			triangle.v2 = new Point(triangleMinX, minY);
			triangle.v3 = new Point(triangleMaxX, minY);
			if (!testTriangle(triangle)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Tests if area contains a vertical bracket (for example: ｢ or ｣)
	 */
	private boolean isBracketVertical(Area area) {

		int triangleMinY = minY + (area.getHeight()-triangleHeight)/2;
		int triangleMaxY = maxY - (area.getHeight()-triangleHeight)/2;
		
		// left bracket ｢ or (
		if (ne && (nw || se)) {
			triangle.v1 = new Point(maxX - triangleWidth, midY);
			triangle.v2 = new Point(maxX, triangleMinY);
			triangle.v3 = new Point(maxX, triangleMaxY);
			if (!testTriangle(triangle)) {
				return true;
			}
		}
				
		// right bracket ｣ or )
		if (sw && (se || nw)) {
			triangle.v1 = new Point(minX + triangleWidth, midY);
			triangle.v2 = new Point(minX, triangleMaxY);
			triangle.v3 = new Point(minX, triangleMinY);
			if (!testTriangle(triangle)) {
				return true;
			}
		}
		
		return false;
	}
	
	private class Triangle {
		
		Point v1;
		Point v2;
		Point v3;
		
		@Override
		public String toString() {
			return "v1:"+v1+" v2:"+v2+" v3:"+v3;
		}
	}
	
	/**
	 * Tests if there is at least one pixel inside triangle 
	 */
	private boolean testTriangle(Triangle t) {

		for (int x=minX ; x<=maxX ; x++) {
			for (int y=minY ; y<=maxY ; y++) {
				Point pt = new Point(x, y);
				if (!pointInsideTriangle(pt, t)) {
					continue;
				}
				if (task.getPixel(x, y)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if pt is inside triangle t
	 */
	private boolean pointInsideTriangle(Point pt, Triangle t) {
		
		// https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
		
		float d1, d2, d3;
		boolean has_neg, has_pos;
		
	    d1 = sign(pt, t.v1, t.v2);
	    d2 = sign(pt, t.v2, t.v3);
	    d3 = sign(pt, t.v3, t.v1);
	    
	    has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
	    has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);
	    
	    return !(has_neg && has_pos);
	}
	
	private float sign(Point p1, Point p2, Point p3) {
		
		return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
	}
	
	/**
	 * Tests if square contains at least one pixel
	 */
	private boolean testSquare(Rectangle square) {
		
		for (int x=square.x ; x<square.x + square.width ; x++) {
			for (int y=square.y ; y<square.y + square.height ; y++) {
				if (task.getPixel(x, y)) {
					return true;
				}
			}
		}
		
		return false;
	}
		
	/**
	 * Mark areas that represent dot or comma in bottom right corner
	 */
	private void markDotCom(Column col) {
		
		for (int i=1 ; i<col.areas.size() ; i++) {
			
			Area prev = col.areas.get(i-1);
			Area area = col.areas.get(i);
			Area next = null;
			if (i<col.areas.size()-1) {
				next = col.areas.get(i+1);
			}
			
			boolean size;
			boolean location;
			boolean distance;
			
			if (par.vertical) {
				size = area.getMaxDim() <= Math.ceil(0.35f*col.getWidth());
				location = col.getMaxX() - area.getMaxX() < col.getWidth()*0.25f;
				if (next == null) {
					distance = true;
				} else {
					int prevDist = area.getY() - prev.getMaxY();
					int nextDist = next.getY() - area.getMaxY();
					distance = prevDist < nextDist;
				}
			} else {
				size = area.getMaxDim() <= Math.ceil(0.35f*col.getHeight());
				location = col.getMaxY() - area.getMaxY() < col.getHeight()*0.25f;
				if (next == null) {
					distance = true;
				} else {
					int prevDist = area.getX() - prev.getMaxX();
					int nextDist = next.getX() - area.getMaxX();
					distance = prevDist < nextDist;
				}
			}

			if (size && location && distance) {
				area.punctuation = true;
				area.changed = true;
			}
		}
	}
	
	@Override
	protected void addDebugImages() throws Exception {

		task.addDefaultDebugImage("punctuation", par.vertical);
	}
}
