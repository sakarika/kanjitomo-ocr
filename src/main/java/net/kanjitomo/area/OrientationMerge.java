package net.kanjitomo.area;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import net.kanjitomo.Orientation;
import net.kanjitomo.util.Util;

/**
 * Finds columns that overlap with other columns in the same area but with different 
 * orientation. Calculates score for each orientation and keeps columns with best score. 
 * This is done locally so different regions of the image can have different orientation.
 */
public class OrientationMerge extends AreaStep {
	
	public OrientationMerge(AreaTask task) {
		super(task, "combined");
	}
	
	/**
	 * Generates debug images for columns containing this rectangle
	 */
	private Rectangle debugRect = null; // new Rectangle(604,544,62,123);

	private boolean debugAll = false;
	
	/**
	 * Index of all columns in both orientations
	 */
	private RTree<Column> index;
	
	/**
	 * Index of vertical furigana areas
	 */
	private RTree<Area> verticalFuriganaIndex;
	
	/**
	 * Index of horizontal furigana areas
	 */
	private RTree<Area> horizontalFuriganaIndex;
	
	/**
	 * Columns that have already been visited (and can be skipped)
	 */
	private Set<Column> visited = new HashSet<Column>();
	
	/**
	 * Finds areas where columns in different orientations overlap,
	 * calculates average score for each group of columns and keeps the best orientation.
	 * 
	 * After this method each character in target image belongs to only one column in one orientation. 
	 * It's still possible (and correct) that different regions have different orientation.
	 * 
	 * Results are stored in task.columns list. 
	 */
	protected void runImpl() throws Exception {

		if (par.orientationTarget == Orientation.VERTICAL) {
			task.columns = task.verticalColumns;
			return;
		}
		
		if (par.orientationTarget == Orientation.HORIZONTAL) {
			task.columns = task.horizontalColumns;
			return;
		}
		
		task.columns = new ArrayList<Column>();
		
		// populate index with columns from both orientations
		createIndexes();
		
		// create groups from columns
		processColumns();
						
		// remove small extra columns
		removeSmallColumns();
	}
	
	/**
	 * Creates R-tree index for columns in each orientation
	 */
	private void createIndexes() {
		
		index = new RTree<Column>(task.binaryImage);
		index.addAll(task.verticalColumns);
		index.addAll(task.horizontalColumns);
		
		verticalFuriganaIndex = new RTree<Area>(task.binaryImage);
		addToFuriganaIndex(task.verticalColumns, verticalFuriganaIndex);
		
		horizontalFuriganaIndex = new RTree<Area>(task.binaryImage);
		addToFuriganaIndex(task.horizontalColumns, horizontalFuriganaIndex);
	}
	
	private void addToFuriganaIndex(List<Column> cols, RTree<Area> index) {
		
		for (Column col : cols) {
			if (!col.furigana) {
				continue;
			}
			for (Area area : col.getAreas()) {
				index.add(area);
			}
		}
	}
	
	/**
	 * Creates groups from columns, calculates scores and keeps the best groups
	 */
	private void processColumns() {
		
		ArrayList<Column> cols = new ArrayList<Column>();
		cols.addAll(task.verticalColumns);
		cols.addAll(task.horizontalColumns);
		
		Collections.sort(cols, new Comparator<Column>() {

			// iterate from large to small so that small individual areas get included
			// in larger groups
			
			@Override
			public int compare(Column o1, Column o2) {

				Integer s1 = o1.getSize();
				Integer s2 = o2.getSize();
				
				return -1 * s1.compareTo(s2); 
			}
		});
		
		// reset scores created during FindColumns
		for (Column col : cols) {
			col.score = null;
		}
		
		for (Column col : cols) {
			processColumn(col);
		}
	}
	
	/**
	 * Find columns that are connected to each other in either orientation,
	 * starting from argument column. Keeps the group that has lower average score.
	 */
	private void processColumn(Column col) {
		
		if (visited.contains(col)) {
			return;
		}
		
		List<Column> verticalGroup = new ArrayList<Column>();
		List<Column> horizontalGroup = new ArrayList<Column>();
		
		Stack<Column> todo = new Stack<Column>();
		todo.add(col);
		
		Rectangle bounds = col.rect;
		
		while (!todo.empty()) {
			
			Column next = todo.pop();
			if (visited.contains(next)) {
				continue;
			}
			if (next.vertical) {
				verticalGroup.add(next);
			} else {
				horizontalGroup.add(next);
			}
			
			// find columns that intersect with next
			List<Column> candidates = new ArrayList<Column>();
			candidates.addAll(index.get(next.rect, next));
			for (Column furigana : next.furiganaColumns) {
				candidates.addAll(index.get(furigana.rect));
			}
			
			// check rgb values, skip background areas
			float colRGB = col.getMinRGBValue();
			Iterator<Column> i = candidates.iterator();
			while (i.hasNext()) {
				Column candidate = i.next();
				float candRGB = candidate.getMinRGBValue();
				int delta = (int) Math.abs(colRGB - candRGB);
				if (delta > 100) {
					i.remove();
				}
			}
			
			// TODO check average area sizes, skip if target areas are clearly too small 
			// to be part of the same column group
			
			// check intersect size
			for (Column cand : candidates) {
				Rectangle intersect = next.rect.intersection(cand.rect);
				int intSize = intersect.width * intersect.height;
				int refSize1 = (int) Math.ceil(Math.pow(next.getMinorDim(), 2)/4);
				int refSize2 = (int) Math.ceil(Math.pow(cand.getMinorDim(), 2)/4);
				if (intSize >= refSize1 || intSize >= refSize2) {
					todo.add(cand);
				}
			}
			visited.add(next);
			bounds = bounds.union(next.rect);
		}

		// score is calculated for the whole group of columns because this way
		// small errors in single column's score don't affect the result
		
		Float verticalScore = calcScore(verticalGroup, bounds);
		Float horizontalScore = calcScore(horizontalGroup, bounds);
		
		if (horizontalScore == null && verticalScore != null) {
			task.columns.addAll(verticalGroup);
		} else if (horizontalScore != null && verticalScore == null) {
			task.columns.addAll(horizontalGroup);
		} else if (horizontalScore == null && verticalScore == null) {
			task.columns.addAll(verticalGroup);
		} else if (verticalScore <= horizontalScore) {
			task.columns.addAll(verticalGroup);
		} else {
			task.columns.addAll(horizontalGroup);
		}
	}	
	
	/**
	 * Calculates score based on number and distance between areas.
	 * 
	 * @return Lower score is better. null if score can't be calculated.
	 */
	private Float calcScore(List<Column> cols, Rectangle bounds) {
		
		if (cols.size() == 0) {
			return null;
		}
		
		if (isDebug(cols)) {
			if (cols.get(0).vertical) {
				System.err.println("vertical");
			} else {
				System.err.println("horizontal");
			}
			System.err.println("bounds:"+bounds);
		}
		
		// calculate score based on average distance between areas
		Float areaDistanceScore = calcScoreAreaDistance(cols);
		
		// distance between columns could also be used but it only works with 
		// multiple columns
		
		// calculate score based on number of areas. correct orienation often contains
		// columns that are aligned in reading direction resulting in connected
		// column chains
		Float areaConnectionsScore = calcScoreConnected(cols);
		
		// calculate penalty for null columns (less than two valid areas
		// so distance can't be calculated)
		Float nullColsScore = calcScoreNullColumns(cols);
		
		Float score = null;
		if (areaDistanceScore != null && areaConnectionsScore != null && nullColsScore != null) {
			score = areaDistanceScore * areaConnectionsScore * nullColsScore;
		}

		if (isDebug(cols)) System.err.println("  score:"+score);
		
		return score;
	}
	
	/**
	 * Calculates score based on average distance between areas
	 * 
	 * @return null if score can't be calculated
	 */
	private Float calcScoreAreaDistance(List<Column> cols) {
		
		float distanceSum = 0;
		float weightSum = 0;

		for (Column col : cols) {
			Float areaDistance = calcAreaDistance(col);
			col.areaDistance = areaDistance;
			if (areaDistance == null) {
				continue;
			}
			float weight = (float) Math.sqrt(col.getAreaSizeSum());
			if (col.areas.size() == 2) {
				weight *= Math.pow(col.getAvgAreaRatio(), 2);
			}
			distanceSum += areaDistance * weight;
			weightSum += weight;
			if (isDebug(cols)) System.err.println("  col:"+col+" dist:"+areaDistance+" weight:"+weight);
		}
		
		Float areaDistanceScore = null;
		if (weightSum > 0) {
			areaDistanceScore = distanceSum / weightSum;
		}
		if (isDebug(cols)) System.err.println("  areaDistanceScore:"+areaDistanceScore);
		return areaDistanceScore;
	}
		
	/**
	 * Calculates average distance between areas inside column
	 * 
	 * @return null if score can't be calculated
	 */
	private Float calcAreaDistance(Column col) {
		
		float distanceSum = 0;
		int pairs = 0;
		
		List<Area> areas = filterFurigana(col.areas, col.vertical);
		
		for (int i=0 ; i<areas.size()-1 ; i++) {
			
			Area prev = areas.get(i);
			Area next = areas.get(i+1);
			
			// ignore all suspected special cases, concentrate only on good pairs.
			// most of the time there are enough good examples and this prevents
			// random problems with wrong orientation
			
			// ignore punctuation
			if (prev.isPunctuation() || next.isPunctuation()) {
				continue; 
			}
			
			// ignore splitted areas
			if (prev.splitted || next.splitted) {
				continue;
			}
			
			// ignore small square areas
			// for example か in wrong orientation
			float minAreaSize;
			float minAreaShape;
			if (prev.getSize() < next.getSize()) {
				minAreaSize = prev.getSize();
				minAreaShape = prev.getRatio();
			} else {
				minAreaSize = next.getSize();
				minAreaShape = next.getRatio();
			}
			float targetSize = (float) Math.pow(col.getMinorDim(), 2);
			float minAreaRatio = 1.0f * minAreaSize / targetSize;
			if (minAreaRatio <= 0.3f && minAreaShape >= 0.5f) {
				continue;
			}
			
			// ignore two thin areas
			// for example い in wrong orientation
			float avgRatio = (prev.getMajorMinorRatio() + next.getMajorMinorRatio())/2;
			if (avgRatio <= 0.7f) {
				continue;
			}
			
			// ignore too long areas
			float maxLength = Math.max(prev.getMajorDim(), next.getMajorDim());
			if (maxLength > col.getMinorDim()*1.5f) {
				continue;
			}
			
			// calculate distance between areas
			float distance = col.vertical ? 
					next.getMidpoint().y - prev.getMidpoint().y : 
					next.getMidpoint().x - prev.getMidpoint().x;

			// ignore too large gaps
			if (distance > col.getMinorDim()*2) {
				continue;
			}
			
			distanceSum += distance;
			++pairs;
		}
		
		if (pairs > 0) {
			return 1.0f * distanceSum / pairs;
		} else {
			return null;		
		}
	}

	/**
	 * Calculates score based on number of null areas (contain less than two valid
	 * areas).
	 * 
	 * @return null if score can't be calculated
	 */
	private Float calcScoreNullColumns(List<Column> cols) {
		
		float nullScoreWeight = 0f;
		float totalWeight = 0f;
		for (Column col : cols) {
			float weight = (float) Math.sqrt(col.getAreaSizeSum());
			if (col.areaDistance == null) {
				nullScoreWeight += weight;
			}
			totalWeight += weight;
		}
		float nullRatio = nullScoreWeight / totalWeight;
		
		Float nullColsScore;
		if (nullRatio < 0.5f) {
			nullColsScore = Util.scale(nullRatio, 0.0f, 0.5f, 1.0f, 1.1f);
		} else {
			nullColsScore = Util.scale(nullRatio, 0.5f, 1.0f, 1.1f, 10.0f);
		}
		
		if (isDebug(cols)) System.err.println("  nullColsScore:"+nullColsScore);
		
		return nullColsScore;
	}
	
	/**
	 * Calculates score based on connected areas. Lower is better.
	 * 
	 * @return null if score can't be calculated
	 */
	private Float calcScoreConnected(List<Column> cols) {
	
		HashSet<Column> colSet = new HashSet<Column>();
		colSet.addAll(cols);
		
		// find longest chain of connected areas
		Float bestScore = null;  
		for (Column col : cols) {
			
			// chain starts with a column that has no previous column
			// or the column is outside orientation calculation group (rare but possible)
			if (col.previousColumn != null && colSet.contains(col.previousColumn)) {
				continue;
			}
			List<Column> chain = new ArrayList<Column>();
			Column next = col;
			do {
				chain.add(next);
			} while ((next = next.nextColumn) != null);
			
			// calculate score for the chain
			Float score = calcScoreChain(chain);
			if (score != null) {
				if (bestScore == null || score < bestScore) {
					bestScore = score;
				}
			}
		}
		
		if (isDebug(cols)) System.err.println("  areaConnectionsScore:"+bestScore);
		
		return bestScore;
	}
	
	/**
	 * Calculates score based on single connected area chain. Lower is better.
	 * 
	 * @return null if score can't be calculated
	 */
	private Float calcScoreChain(List<Column> cols) {
		
		Float areaConnectionsScore = null;
		
		for (Column col : cols) {
			for (Area area : col.areas) {
				if (area.isPunctuation()) {
					continue;
				}
				if (areaConnectionsScore == null) {
					areaConnectionsScore = 0f;
				}
				// prefer square areas, sometimes wrong orientation results in
				// valid area to be splitted in two, like い
				areaConnectionsScore += area.getRatio();
			}
		}
		
		if (areaConnectionsScore == null) {
			return null;
		} else {
			return (float) (1.0f / Math.pow(areaConnectionsScore, 0.2f));
		}
	}
	
	/**
	 * @return true if any column in the list intersects with debugRect
	 */
	private boolean isDebug(List<Column> cols) {
		
		if (debugAll) {
			return true;
		} else {
			for (Column col : cols) {
				if (col.intersects(debugRect)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Removes areas that look like furigana columns in other orientation
	 */
	private List<Area> filterFurigana(List<Area> areas, final boolean vertical) {
		
		List<Area> validAreas = new ArrayList<Area>();

		// check if furigana areas in other orientation match exactly
		for (Area area : areas) {
			if (!isAreaFurigana(area, vertical)) {
				validAreas.add(area);
			}
		}
		
		Collections.sort(validAreas, new Comparator<Area>() {

			@Override
			public int compare(Area o1, Area o2) {
				
				Integer i1;
				Integer i2;
				if (vertical) {
					i1 = o1.getY();
					i2 = o2.getY();
				} else {
					i1 = o1.getX();
					i2 = o2.getX();
				}
				
				return i1.compareTo(i2);
			}
		});
		
		return validAreas;
	}
	
	/**
	 * Checks if area looks like furigana column in other orientation
	 */
	private boolean isAreaFurigana(Area area, boolean vertical) {
		
		int pixels = area.getPixels();
		
		List<Area> furiAreas;
		if (vertical) {
			furiAreas = horizontalFuriganaIndex.get(area.rect);
		} else {
			furiAreas = verticalFuriganaIndex.get(area.rect);
		}
		
		int furiPixels = 0;
		for (Area furiArea : furiAreas) {
			furiPixels += furiArea.getPixels();
		}
		
		if (pixels == furiPixels) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Removes too small columns
	 */
	private void removeSmallColumns() {
		
		Iterator<Column> i = task.columns.iterator();
		while (i.hasNext()) {
				Column column = i.next();
				if (column.getMinDim() <= 7) { // TODO relative to resolution
				//if (column.getSize() < 20) {
					i.remove();
			}
		}
	}
		
	@Override
	protected void addDebugImages() throws Exception {

		task.addDefaultDebugImage("combined");
	}
}
