package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * List of areas inside a single column (or row in horizontal orientation).
 * Column is enclosed by a border that doesn't touch other areas.
 */
public class Column implements HasRectangle {
	
	/**
	 * Areas inside this column. Ordered in reading direction (top-down or left-right). 
	 */
	List<Area> areas = new ArrayList<Area>();
	
	/**
	 * Bounding box around areas
	 */
	Rectangle rect;
	
	/**
	 * If true, this column has vertical reading direction. If false, horizontal.
	 */
	boolean vertical = true;
	
	/**
	 * If true, this is a furigana column
	 */
	boolean furigana = false;
	
	/**
	 * Furigana columns next to this column
	 */
	List<Column> furiganaColumns = new ArrayList<Column>();
	
	/**
	 * Average distance between areas
	 */
	Float areaDistance = null;
	
	/**
	 * Used to compare this column to corresponding columns
	 */
	Float score = null;
	
	/**
	 * Next column in reading direction
	 */
	Column nextColumn;
	
	/**
	 * Previous column in reading direction
	 */
	Column previousColumn;
	
	/**
	 * If set, this color will be used in debug images
	 */
	Color debugColor;
	
	/**
	 * If true, column has been changed in this step. Used for debug printing.
	 */
	boolean changed = false;
	
	/**
	 * If true, column has been marked for removal
	 */
	boolean remove = false;
	
	/**
	 * @return true if this column has been changed in previous AreaStep.
	 * Changes columns are painted blue.
	 */
	public boolean isChanged() {
	
		return changed;
	}
	
	/**
	 * @return true if this column contains furigana
	 */
	public boolean isFurigana() {
		
		return furigana;
	}
	
	/**
	 * @return Furigana columns next to this column
	 */
	public List<Column> getFuriganaColumns() {
		
		return furiganaColumns;
	}
	
	/**
	 * @return true if column has vertical orientation
	 */
	public boolean isVertical() {
		
		return vertical;
	}
	
	public Column getNextColumn() {
		
		return nextColumn;
	}
	
	public Column getPreviousColumn() {
		
		return previousColumn;
	}
	
	/**
	 * Is this column connected to any other columns
	 */
	public boolean isConnected() {

		return previousColumn != null || nextColumn !=null;
	}
	
	/**
	 * @return Color used to paint this column in debug images
	 */
	public Color getDebugColor() {
		
		return debugColor; 
	}
	
	/**
	 * Surface area of the column
	 */
	public int getSize() {
		
		return rect.width * rect.height;
	}
	
	/**
	 * Surface area of column's areas
	 */
	public int getAreaSizeSum() {
		
		int sum = 0;
		for (Area area : areas) {
			sum += area.getSize();
		}
		return sum;
	}
	
	/**
	 * max(width, heighrt)
	 */
	public int getMaxDim() {
		
		if (rect.width > rect.height) {
			return rect.width;
		} else {
			return rect.height;
		}
	}
	
	/**
	 * min(width, heighrt)
	 */
	public int getMinDim() {
		
		if (rect.width < rect.height) {
			return rect.width;
		} else {
			return rect.height;
		}
	}
	
	/**
	 * @return true if column's rectangle contains point 
	 */
	public boolean contains(Point point) {
		
		if (point == null) {
			return false;
		} else {
			return rect.contains(point);
		}
	}
	
	/**
	 * @return true if column's rectangle intersect with argument rectangle 
	 */
	public boolean intersects(Rectangle rect) {
		
		if (rect == null) {
			return false;
		} else {
			return this.rect.intersects(rect);
		}
	}
	
	/**
	 * @return true if column's rectangle contains point 
	 */
	public boolean contains(int x, int y) {
		
		return contains(new Point(x,y));
	}
	
	/**
	 * @return true if column's rectangle contains col2's rectangle
	 */
	public boolean contains(Column col2) {
		
		return rect.contains(col2.rect);
	}
	
	/**
	 * Calculates the ratio of common pixels to total area with argument column
	 */
	public float getIntersectRatio(Column col2) {
		
		if (!col2.rect.intersects(rect)) {
			return 0f;
		}
		
		Rectangle intersect = col2.rect.intersection(rect);
		int intersectSize = intersect.height * intersect.width;
		int refSize = Math.min(getSize(), col2.getSize());
		
		return 1.0f * intersectSize / refSize;
	}
	
	/**
	 * Calculates the ratio of common x coordinates between columns
	 */
	public float getHorizontalIntersectRatio(Column col2) {
		
		if (col2.getX() > getMaxX() || col2.getMaxX() < getX()) {
			return 0f;
		}
		
		int intersectMinX = Math.max(getX(), col2.getX());
		int intersectMaxX = Math.min(getMaxX(), col2.getMaxX());
		int commonWidth = intersectMaxX - intersectMinX + 1;
		int refWidth = Math.min(rect.width, col2.rect.width);
		
		return 1.0f * commonWidth / refWidth; 
	}
	
	/**
	 * Merges this column with argument column
	 * 
	 * @param mergeAreas If true, overlapping areas inside this column are merged 
	 * along minor dimension (left/right in vertical columns)
	 * @return Merged column (this column is not affected)
	 */
	Column merge(Column col2) {
		
		Column mergedCol = new Column();
		mergedCol.rect = rect.union(col2.rect);
		mergedCol.addAreas(this.areas);
		mergedCol.addAreas(col2.areas);
		mergedCol.vertical = this.vertical;
		
		Collections.sort(mergedCol.areas, new Comparator<Area>() {
			@Override
			public int compare(Area o1, Area o2) {

				if (vertical) {
					Integer y1 = o1.getMidpoint().y;
					Integer y2 = o2.getMidpoint().y;
					return y1.compareTo(y2);
				} else {
					Integer x1 = o1.getMidpoint().x;
					Integer x2 = o2.getMidpoint().x;
					return x1.compareTo(x2);
				}
			}
		});
		
		mergeAreasMinorDim(mergedCol.areas);
		
		return mergedCol;
	}

	public void addAreas(List<Area> areas) {
		
		for (Area area : areas) {
			addArea(area);
		}
	}
	
	public void addArea(Area area) {
		
		area = area.clone();
		area.column = this;
		areas.add(area);
	}
	
	/**
	 * Merges areas that are overlapping in left/right direction (vertical orientation)
	 * or up/down direction (horizontal orientation).
	 */
	private void mergeAreasMinorDim(List<Area> areas) {
	
		for (int i=0 ; i<areas.size()-1 ; i++) {
		
			Area a1 = areas.get(i);
			Area a2 = areas.get(i+1);
			
			boolean intersect = false;
			if (vertical) {
				if (a1.getMaxY() >= a2.getY() && a1.getY() <= a2.getMaxY()) {
					intersect = true;
				}
			} else {
				if (a1.getMaxX() >= a2.getX() && a1.getX() <= a2.getMaxX()) {
					intersect = true;
				}
			}
			
			if (intersect) {
				areas.remove(i);
				areas.remove(i);
				areas.add(i, a1.merge(a2));
				--i;
			}
		}
	}
	
	public Rectangle getRectangle() {
		return rect;
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
	 * Column thickness
	 * 
	 * @return vertical -> width, horizontal -> height
	 */
	public int getMinorDim() {
		
		if (vertical) {
			return rect.width;
		}  else {
			return rect.height;
		}
	}
	
	/**
	 * Column length in reading direction
	 * 
	 * @return vertical -> height, horizontal -> width
	 */
	public int getMajorDim() {
		
		if (vertical) {
			return rect.height;
		}  else {
			return rect.width;
		}
	}
	
	/**
	 * Midpoint of this column's rectangle.
	 */
	public Point getMidpoint() {
		
		return new Point(rect.x + rect.width/2, rect.y + rect.height/2);
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

	public List<Area> getAreas() {
		return areas;
	}
	
	/**
	 * @return true if column contains only splitted areas
	 */
	public boolean isAllAreasSplitted() {
		
		for (Area area : areas) {
			if (!area.splitted) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Number of pixels inside column
	 */
	public int getPixels() {
		
		int pixels = 0;
		for (Area area : areas) {
			pixels += area.getPixels();
		}
		return pixels;
	}
	
	/**
	 * Ratio of pixels / column area.
	 * @return
	 */
	public float getPixelAreaRatio() {
		
		return 1.0f*getPixels() / getSize();
	}
	
	/**
	 * Ratio between smaller and larger dimensions.
	 */
	public float getRatio() {
		
		int min = Math.min(rect.width, rect.height);
		int max = Math.max(rect.width, rect.height);
		return 1.0f*min/max;
	}
		
	/**
	 * Average area shape
	 */
	public float getAvgAreaRatio() {
		
		if (areas.size() == 0) {
			return 1;
		}
		
		float sum = 0f;
		for (Area area : areas) {
			sum += area.getRatio();
		}
		return sum / areas.size();
	}
	
	/**
	 * Average of area sizes
	 */
	public float getAvgAreaSize() {
		
		if (areas.size() == 0) {
			return 1;
		}
		
		float sum = 0f;
		for (Area area : areas) {
			sum += area.getSize();
		}
		return sum / areas.size();
	}

	/**
	 * Number of original areas (before merges) combined into this column
	 */
	public int getSourceAreaCount() {

		int count = 0;
		
		for (Area area : areas) {
			count += area.getSourceAreas().size();
		}
		
		return count;
	}
	
	/**
	 * Median of area sizes
	 */
	public float getMedianAreaSize() {
		
		if (areas.size() == 0) {
			return 0;
		}
		
		List<Area> areasBySize = new ArrayList<Area>();
		areasBySize.addAll(areas);
		
		Collections.sort(areasBySize, new Comparator<Area>() {

			@Override
			public int compare(Area o1, Area o2) {
				
				Integer s1 = o1.getSize();
				Integer s2 = o2.getSize();
				
				return s1.compareTo(s2);
			}
		});
		
		if (areas.size()%2 == 1) {
			return areasBySize.get(areas.size()/2).getSize();
		} else {
			int size1 = areasBySize.get(areas.size()/2-1).getSize();
			int size2 = areasBySize.get(areas.size()/2).getSize();
			return 1.0f * (size1 + size2) / 2;
		}
	}
	
	/**
	 * Standard deviation of area sizes
	 */
	public float getAreaSizeStd() {
		
		float mean = getAvgAreaSize();
		
		float sum = 0f;
		for (Area area : areas) {
			sum += Math.pow(area.getSize() - mean, 2);
		}
		return (float) Math.sqrt(sum);
	}
	
	/**
	 * Coefficient of variation of area sizes
	 */
	public float getAreaSizeCV() {
		
		float mean = getAvgAreaSize();
		
		float sum = 0f;
		for (Area area : areas) {
			sum += Math.pow(area.getSize() - mean, 2);
		}
		float std = (float) Math.sqrt(sum);
		
		return std / mean;
	}
	
	/**
	 * Average area minorDim/majorDim ratio.
	 */
	public float getAvgMinorMajorRatio() {
		
		if (areas.size() == 0) {
			return 1;
		}
		
		float sum = 0f;
		for (Area area : areas) {
			sum += 1.0f*area.getMinorDim() / area.getMajorDim();
		}
		return sum / areas.size();
	}
	
	/**
	 * Minimum area minorDim/majorDim ratio.
	 */
	public float getMinMinorMajorRatio() {
		
		if (areas.size() == 0) {
			return 1;
		}
		
		float min = 10000f;
		for (Area area : areas) {
			float ratio = 1.0f*area.getMinorDim() / area.getMajorDim();
			if (ratio < min) {
				min = ratio;
			}
		}
		return min;
	}
	
	/**
	 * Maximum area minorDim/majorDim ratio.
	 */
	public float getMaxMinorMajorRatio() {
		
		if (areas.size() == 0) {
			return 1;
		}
		
		float max = 0f;
		for (Area area : areas) {
			float ratio = 1.0f*area.getMinorDim() / area.getMajorDim();
			if (ratio > max) {
				max = ratio;
			}
		}
		return max;
	}
	
	/**
	 * Maximum area width/height ratio
	 */
	public float getMaxAreaRatio() {
		
		float max = 0f;
		for (Area area : areas) {
			if (area.getRatio() > max) {
				max = area.getRatio();
			}
		}
		return max;
	}
	
	/**
	 * Minimum area width/height ratio
	 */
	public float getMinAreaRatio() {
		
		float min = 1f;
		for (Area area : areas) {
			if (area.getRatio() < min) {
				min = area.getRatio();
			}
		}
		return min;
	}
	
	/**
	 * Gets the mininum RGB value among areas pixels
	 */
	public int getMinRGBValue() {

		int minRGB = 255;
		for (Area area : areas) {
			if (area.minRGB < minRGB) {
				minRGB = area.minRGB;
			}
		}
		
		return minRGB;
	}
	
	/**
	 * Gets the average minimum RGB value among areas
	 */
	public float getAvgRGBValue() {

		int rgbSum = 0;
		int rgbWeight = 0;
		
		for (Area area : areas) {
			int weight = area.getPixels();
			rgbSum += area.minRGB * weight; 
			rgbWeight += weight;
		}
		
		return 1.0f * rgbSum / rgbWeight;
	}
	
	@Override
	public String toString() {
		return (vertical ? "v" : "h")+":"+rect.x+","+rect.y+","+rect.width+":"+rect.height;
	}
	
	/**
	 * Simplified Column representation intended for API users.
	 */
	private net.kanjitomo.Column simpleColumn = null;
	
	/**
	 * Simplified Column representation intended for API users.
	 */
	public net.kanjitomo.Column getSimpleColumn() {
		
		if (simpleColumn != null) {
			return simpleColumn;
		}
		
		simpleColumn = new net.kanjitomo.Column();
		simpleColumn.areas = new ArrayList<>();
		for (Area area : areas) {
			if (!area.punctuation) {
				simpleColumn.areas.add(area.getRectangle());
			}
		}
		simpleColumn.rect = rect;
		simpleColumn.vertical = vertical;
		simpleColumn.furigana = furigana;
		
		return simpleColumn;
	}
}
