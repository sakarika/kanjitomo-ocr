package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Rectangle inside target image. Contains a single character after area detection 
 * is completed. Might contain character fragments (radicals) during area detection.
 */
public class Area implements HasRectangle {
	
	/**
	 * Location of the area in target image 
	 */
	Rectangle rect;
	
	/**
	 * Number of black pixels inside the area 
	 */
	int pixels;

	/**
	 * If true, this area contains punctuation character (bracket, dot or comma)
	 */
	boolean punctuation = false;
	
	/**
	 * If true, area has been changed in this step. Used for debug printing.
	 */
	boolean changed = false;
	
	/**
	 * If set, this color is used in debug images 
	 */
	public Color debugColor = null;
	
	/**
	 * If true, this area was splitted (was originally connected by touching pixels)
	 */
	public boolean splitted = false;
	
	/**
	 * Mininum RGB value within area's pixels
	 */
	public int minRGB;
	
	/**
	 * Column that contains this area
	 */
	public Column column;
	
	/**
	 * If true, this area has been removed from the image
	 */
	boolean remove = false;
	
	/**
	 * Initial areas created during FindAreas but later merged into this area
	 */
	List<Area> sourceAreas = new ArrayList<Area>();
	
	public Area(Rectangle rect, int pixels) {
		this.rect = rect;
		this.pixels = pixels;
	}
	
	public Rectangle getRectangle() {
		return rect;
	}
	
	/**
	 * Surface area.
	 * @return
	 */
	public int getSize() {
		return rect.width * rect.height;
	}
	
	public int getX() {
		return rect.x;
	}
	
	public int getY() {
		return rect.y;
	}
	
	public int getWidth() {
		return rect.width;
	}
	
	public int getHeight() {
		return rect.height;
	}
	
	/**
	 * Right border.
	 */
	public int getMaxX() {
		return rect.x + rect.width - 1;
	}
	
	/**
	 * Bottom border.
	 */
	public int getMaxY() {
		return rect.y + rect.height - 1;
	}

	/**
	 * Minumum pixel RGB value in this area
	 */
	public int getMinRGB() {
		return minRGB;
	}
	
	/**
	 * Min(width / height , height / width). Perfect square returns 1.0f, straigth line 
	 * returns close to 0.0f.
	 */
	float getRatio() {
		
		float r1 = 1.0f*rect.width / rect.height;
		float r2 = 1.0f*rect.height / rect.width;
		
		return r1 < r2 ? r1 : r2;
	}
	
	float getMajorMinorRatio() {
		
		if (column == null) {
			throw new Error("Orientation not determined");
			// called before columns have been detected
		}
		
		if (column.vertical) {
			return getHeightWidthRatio();
		}  else {
			return getWidthHeightRatio();
		}
	}
	
	float getMinorMajorRatio() {
		
		return 1/getMajorMinorRatio();
	}
	
	float getWidthHeightRatio() {
		
		return 1.0f*rect.width / rect.height;
	}
	
	float getHeightWidthRatio() {
		
		return 1.0f*rect.height / rect.width;
	}
	
	/**
	 * Gets ratio of pixels to total area
	 */
	float getPixelDensity() {

		return 1.0f*pixels/getSize();
	}
	
	public int getPixels() {
		
		return pixels; 
	}
	
	public boolean isPunctuation() {
		return punctuation;
	}
	
	public boolean isChanged() {
		return changed;
	}
	
	/**
	 * Initial areas created at FindAreas and merged into this area
	 */
	public List<Area> getSourceAreas() {
		
		return sourceAreas;
	}
	
	/**
	 * Midpoint of this area's rectangle.
	 */
	public Point getMidpoint() {
		
		return new Point(rect.x + rect.width/2, rect.y + rect.height/2);
	}

	Integer getMinDim() {
		
		if (rect.width <= rect.height)
			return rect.width;
		else
			return rect.height;
	}
	
	Integer getMaxDim() {
		
		if (rect.width >= rect.height)
			return rect.width;
		else
			return rect.height;
	}
	
	/**
	 * @return vertical -> width, horizontal -> height
	 */
	public int getMinorDim() {

		if (column == null) {
			throw new Error("Not implemented");
			// called before columns have been detected
			// TODO add orientation information to areas
		}
		
		if (column.vertical) {
			return rect.width;
		}  else {
			return rect.height;
		}
	}
	
	/**
	 * @return vertical -> height, horizontal -> width
	 */
	public int getMajorDim() {
		
		if (column == null) {
			throw new Error("Not implemented");
		}
		
		if (column.vertical) {
			return rect.height;
		}  else {
			return rect.width;
		}
	}

	int getAvgDim() {
		return (rect.width + rect.height) / 2;
	}
	
	/**
	 * @return true fn area contains Point(x,y)
	 */
	boolean contains(int x, int y) {
		
		return contains(new Point(x,y));
	}
	
	/**
	 * @return true in area contains point
	 */
	boolean contains(Point point) {
		
		if (point == null) {
			return false;
		}
		
		if (rect.contains(point)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Merges the area's rectangle with argument area.
	 */
	public Area merge(Area area2) {
		
		Area newArea = new Area(rect.union(area2.rect), pixels + area2.pixels);
		newArea.pixels = pixels + area2.pixels;
		newArea.column = column;
		newArea.sourceAreas.addAll(sourceAreas);
		newArea.sourceAreas.addAll(area2.sourceAreas);
		newArea.minRGB = Math.min(minRGB, area2.minRGB);
		
		return newArea;
	}
	
	/**
	 * Splits the area into two areas
	 */
	public List<Area> splitX(int x) {
		
		Rectangle leftRect = new Rectangle(rect.x, rect.y, x - rect.x, rect.height);
		Area left = new Area(leftRect, pixels/2);
		left.changed = true;
		left.column = column;
		left.splitted = true;
		left.minRGB = minRGB;
		left.sourceAreas.addAll(sourceAreas);
		
		Rectangle rightRect = new Rectangle(x, rect.y, rect.x + rect.width - x, rect.height);
		Area right = new Area(rightRect, pixels/2);
		right.changed = true;
		right.column = column;
		right.splitted = true;
		right.minRGB = minRGB;
		right.sourceAreas.addAll(sourceAreas);
		
		List<Area> areas = new ArrayList<Area>();
		areas.add(left);
		areas.add(right);
		return areas;
	}
	
	public List<Area> splitY(int y) {
		
		Rectangle upRect = new Rectangle(rect.x, rect.y, rect.width, y - rect.y);
		Area up = new Area(upRect, pixels/2);
		up.changed = true;
		up.column = column;
		up.splitted = true;
		up.sourceAreas.addAll(sourceAreas);
		
		Rectangle downRect = new Rectangle(rect.x, y, rect.width, rect.y + rect.height - y);
		Area down = new Area(downRect, pixels/2);
		down.changed = true;
		down.column = column;
		down.splitted = true;
		down.sourceAreas.addAll(sourceAreas);
		
		List<Area> areas = new ArrayList<Area>();
		areas.add(up);
		areas.add(down);
		return areas;
	}
	
	public Area clone() {
		
		Area clone = new Area(rect, pixels);
		clone.column = column;
		clone.minRGB = minRGB;
		clone.sourceAreas.addAll(sourceAreas);
		
		return clone;
	}
	
	/**
	 * @return true if area's rectangle intersect with argument rectangle 
	 */
	public boolean intersects(Rectangle rect) {
		
		if (rect == null) {
			return false;
		} else {
			return this.rect.intersects(rect);
		}
	}
	
	@Override
	public String toString() {
		return rect.x+","+rect.y+","+rect.width+":"+rect.height;
	}
}
