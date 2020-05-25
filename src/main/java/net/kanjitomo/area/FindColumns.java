package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Util;

/**
 * Merges areas into columns.
 */
public class FindColumns extends AreaStep {
	
	public FindColumns(AreaTask task) {
		super(task, "columns");
	}

	/**
	 * Generates debug images only for columns containing this rectangle
	 */
	private final Rectangle debugRect = null; // new Rectangle(273,96,4,9);

	/**
	 * Generates debug images for all columns.
	 * 
	 * Warning! This will create large amount of data, use only for small target images.
	 */
	private final boolean debugAll = false;

	/**
	 * If true, prints extra details about score calculations
	 */
	private boolean debugDetails = true;
	
	/**
	 * Areas are only combined into columns if their RGB values are closer than this
	 */
	private int rgbMaxDelta = 100;
	
	private RTree<Column> index;
			
	@Override
	protected void runImpl() throws Exception {
		
		if (task.areas.size() == 0) {
			task.columns = new ArrayList<Column>();
			return;
		}
		
		// merge areas into columns
		index = new RTree<Column>(task.binaryImage);
		createInitialColumns();
		for (int i = 1 ; i <= 3 ; i++) {
			mergeColumns(true, i);
		}
		mergeColumns(false, 1); 
		task.columns = index.getAll();
	}

	/**
	 * Creates new column for each area
	 */
	private void createInitialColumns() {

		for (Area area : task.areas) {
			index.add(createColumn(area));
		}
	}

	/**
	 * Creates new column that represents single area
	 */
	private Column createColumn(Area area) {
		
		Column column = new Column();
		column.addArea(area);
		column.rect = new Rectangle(
				area.rect.x,
				area.rect.y,
				area.rect.width,
				area.rect.height);
		column.vertical = par.vertical;
		column.score = column.getRatio();
				
		return column;
	}
	
	/**
	 * Merges close by areas into columns. Columns are scored and expansion is done only if 
	 * column score increases.
	 * 
	 * @param expandLength If true, columns length is expanded (in reading direction). 
	 * If false, width is expanded.
	 * 
	 * @param iteration Column length in expanded in stages. First by small amount
	 * (iteration 1), then in larger increments.
	 */
	private void mergeColumns(boolean expandLength, int iteration) throws Exception {
		
		if (debugAll) System.err.println("mergeColumn expandLength:"+expandLength+" iteration:"+iteration);
				
		// process areas in order that prioritizes column growth in reading direction
		List<Column> cols = index.getAll();
		PriorityQueue<Column> todo = new PriorityQueue<Column>(cols.size(),
				new Comparator<Column>() {

			@Override
			public int compare(Column o1, Column o2) {
				
				Integer size1 = o1.getMinorDim() * o1.getSize();
				Integer size2 = o2.getMinorDim() * o2.getSize();
				
				return size1.compareTo(size2);
			}
		});
		todo.addAll(cols);
		
		// merge columns until nothing can be done
		//while(!todo.empty()) {
		while (!todo.isEmpty()) {
			
			//Column col = todo.pop();
			Column col = todo.remove();
			
			if (col.remove) {
				continue;
			}
			
			// find close by target columns that are candidates for merging
			Rectangle probe = createProbe(col, expandLength, iteration);
			List<Column> targets = index.get(probe, col);
			
			if (isDebug(col)) {
				addIntermediateDebugImage(col, probe);
				System.err.println(task.debugImages.get(task.debugImages.size()-1).getFilename());
				System.err.println("  col:   "+col+" score:"+col.score+" rgb:"+col.getAvgRGBValue());
				System.err.println("  probe: "+probe);
			}

			// find the largest column (not always col, can be target)
			Column largest = col;
			for (Column target : targets) {
				if (target.getMinorDim() > largest.getMinorDim()) {
					largest = target;
				}
			}
			
			// filter columns that have too high rgb value compared to largest column
			// this is done to prevent expansion into unrelated background areas
			if (col.getAvgRGBValue() - largest.getAvgRGBValue() > rgbMaxDelta) {
				if (debug) System.err.println("  skip rgb");
				continue;
			}
			List<Column> rejected = filterTargetsByRGB(targets, largest);
			if (targets.isEmpty()) {
				if (debug) System.err.println("  skip empty");
				continue;
			}
			
			// merge col with targets
			Column merged = col;
			for (Column target : targets) {
				merged = merged.merge(target);
			}
					
			if (debug) {
				for (Column target : targets) {
					System.err.println("  target:"+target+" score:"+target.score+" rgb:"+target.getAvgRGBValue());
				}
				System.err.println("  merged:"+merged);
			}
			
			// sideways expansion is only allowed between adjacent columns, check
			// that merged column doesn't contain any columns not in original target list
			if (!expandLength) {
				if (checkNewTargets(merged, col, targets, rejected)) {
					continue;
				}
			}
			
			// check that merged column is valid and average column score increases
			if (checkMerge(merged, col, targets, largest)) {
				for (Column target : targets) {
					index.remove(target);
					target.remove = true;
				}
				index.remove(col);
				index.add(merged);
				todo.add(merged);
			}
		}
	}
	
	/**
	 * Filter targets that have too high RGB value compared to largest column
	 * 
	 * @return list of rejected columns
	 */
	private List<Column> filterTargetsByRGB(List<Column> targets, Column largest) {

		List<Column> rejected = new ArrayList<Column>();
		float refRGBValue = largest.getAvgRGBValue();
		Iterator<Column> i = targets.iterator();
		
		while (i.hasNext()) {
			Column target = i.next();
			// allow targets that are contained inside col
			if (largest.contains(target)) {
				continue;
			}
			// allow dakuten,
			// these are sometimes gray instead of black and can fail rgb check
			if (target.areas.size() == 1 &&
				target.getRatio() >= 0.6f &&
				target.getMaxY() >= largest.getY() - largest.getWidth()/4 &&
				target.getMaxY() < largest.getMaxY() &&
				target.getMidpoint().x > largest.getMidpoint().x &&
				target.getPixelAreaRatio() >= 0.5f &&
				largest.getHorizontalIntersectRatio(target) >= 0.7f) {
				continue;
			}
			
			// check rgb values
			if (target.getAvgRGBValue() - refRGBValue > rgbMaxDelta) {
				if (debug) System.err.println("  skip target:"+target+" rgb:"+target.getAvgRGBValue());
				i.remove();
				rejected.add(target);
			}
		}
		
		return rejected;
	}
	
	/**
	 * Checks that merged column's rectangle doesn't contain any new columns not
	 * in original targets list
	 * 
	 * @return true if merged rectangle contains extra columns
	 */
	private boolean checkNewTargets(Column merged, Column original, List<Column> targets,
			List<Column> rejected) {
		
		Set<Column> colSet = new HashSet<Column>();
		colSet.add(original);
		colSet.addAll(targets);
		colSet.addAll(rejected);
		
		for (Column col : index.get(merged.rect)) {
			if (!colSet.contains(col)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Creates a probe rectangle that is used to find expansion targets
	 * 
	 * @param expandLength If true, column's length is expanded (in reading direction). 
	 * If false, column's width is expanded.
	 */
	private Rectangle createProbe(Column col, boolean expandLength, int iteration) {
		
		if (expandLength) {
			return createLongProbe(col, iteration);
		} else {
			return createThickProbe(col);
		}
	}
	
	/**
	 * Creates a probe that is used to expand column in reading direction
	 */
	private Rectangle createLongProbe(Column col, int iteration) {
		
		int width = col.getWidth();
		int height = col.getHeight();
		int extra = (int) Math.ceil(Math.min(width, height) * 0.5f * iteration);
		
		if (col.vertical) { 
			return new Rectangle(col.getX(), col.getY() - extra,
					width, col.getHeight() + extra*2);
		} else {
			return new Rectangle(col.getX() - extra, col.getY(),
					col.getWidth() + extra*2, height);
		}
	}
	
	/**
	 * Creates a probe that is used to expand column sideways
	 */
	private Rectangle createThickProbe(Column col) {
		
		int width = col.getWidth();
		int height = col.getHeight();
		int extra = (int) Math.floor(Math.min(width, height));
		
		if (col.vertical) { 
			return new Rectangle(col.getX() - extra, col.getY(),
					width + extra*2, col.getHeight());
		} else {
			return new Rectangle(col.getX(), col.getY() - extra,
					width, col.getHeight() + extra*2);
		}
	}
	
	/**
	 * Checks that merged column is better that old columns
	 * 
	 * @return true if merged is better
	 */
	private boolean checkMerge(Column merged, Column col, List<Column> targets,
			Column largest) {
		
		// calculate score for merged column
		merged.score = calcScore(merged);
		
		// check if all columns are contained inside largest column
		boolean intersect = checkContains(largest, merge(col, targets));
		if (intersect) {
			if (debug) System.err.println("  all columns contained");
			return true;
		}
		
		// check if column is expanding along minor dimension, decrease score if 
		// large expansion and long column (often invalid expansions into furigana)  
		float minorDimExpansion = 1.0f * merged.getMinorDim() / largest.getMinorDim();
		float maxPenalty = Util.scale(col.areas.size(), 2, 4, 1.0f, 0.8f);
		float expansionScore = Util.scale(minorDimExpansion, 1.15f, 1.4f, 1.0f, maxPenalty); 
		merged.score *= expansionScore;
		if (debug && debugDetails) {
			System.err.println("  minorDimExp:"+minorDimExpansion);
			System.err.println("  expScore:   "+expansionScore);
			System.err.print("  score:      "+merged.score);
		}
		
		// calculate weighted average score from old columns
		float scoreSum = 0f;
		float weightSum = 0f;
		float lowestScore = 10000;
		for (Column target : merge(col, targets)) {
			if (target.score < lowestScore) {
				lowestScore = target.score;
			}
		}
		for (Column target : merge(col, targets)) {
			float weight = (float) Math.pow(target.getSize(), 0.58f);
			if (target.score == lowestScore) {
				weight *= 1.25f;
			}
			weight *= Util.scale(target.getMinRGBValue(), 0, par.pixelRGBThreshold,
					1.0f, 0.5f);
			scoreSum += target.score * weight;
			weightSum += weight;
		}
		float oldScore = scoreSum / weightSum;
				
		// check if merged column is better than average
		if (debug) System.err.println(" "+(merged.score >= oldScore ? ">=" : "<")+" old score:"+oldScore);
		if (merged.score >= oldScore) {
			// check that column doesn't cross background elements
			if (!checkBackground(merged)) {
				if (debug) System.err.println("  skip background");
				return false;
			}
			// check that column ends are no expanded into small area fragments
			if (!checkColumnEnds(merged)) {
				if (debug) System.err.println("  skip column ends");
				return false;
			}
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Merges col and cols into new list
	 */
	private List<Column> merge(Column col, List<Column> cols) {
		
		List<Column> joined = new ArrayList<Column>();
		joined.addAll(cols);
		joined.add(col);
		return joined;
	}

	/**
	 * Checks that column ends are not nearly empty
	 * 
	 * @return true if column is valid (pixel to area ratio at ends is high enough)
	 */
	private boolean checkColumnEnds(Column col) {

		// this prevents expansion into small unrelated fragments in the background
		
		Rectangle firstProbe;
		Rectangle secondProbe;
		
		int probeLength = col.getMinorDim();
		
		if (col.vertical) {
			firstProbe = new Rectangle(col.getX(), col.getY(),
					col.getWidth(), probeLength);
			secondProbe = new Rectangle(col.getX(), col.getMaxY() - probeLength,
					col.getWidth(), probeLength);
		} else {
			firstProbe = new Rectangle(col.getX(), col.getY(),
					probeLength, col.getHeight());
			secondProbe = new Rectangle(col.getMaxX() - probeLength, col.getY(),
					probeLength, col.getHeight());
		}
		
		if (!checkColumnEnd(col, firstProbe)) {
			return false;
		}
		if (!checkColumnEnd(col, secondProbe)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks that column end is not nearly empty
	 * 
	 * @return true if column is valid (pixel to area ratio at end is high enough)
	 */
	private boolean checkColumnEnd(Column col, Rectangle probe) {

		float pixelsSum = 0f;
		
		for (Area area : col.areas) {
			
			if (!probe.intersects(area.rect)) {
				continue;
			}
			
			Rectangle intersect = probe.intersection(area.rect);
			float ratio = 1.0f * intersect.height * intersect.width / area.getSize();
			float pixels = area.getPixels() * ratio;
			// thin lines along major dimension are fine, give them priority
			pixels *= Util.scale(area.getMajorMinorRatio(), 0.5f, 1.5f, 0.5f, 1.5f);
			
			pixelsSum += pixels;
		}
		
		float ratio = pixelsSum / (probe.width * probe.height);
		
		if (ratio < 0.05f) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks if largest column contains all targets
	 */
	private boolean checkContains(Column largest, List<Column> targets) {

		// check that majority of target's columns area is contained within largest column
		for (Column target : targets) {
			if (largest.getIntersectRatio(target) < 0.65) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Calculates score based on column's areas (higher score is better).
	 */
	private float calcScore(Column col) {
		
		float scoreSum = 0f;
		for (Area area : col.areas) {
			if (debug && debugDetails) System.err.println("  area:"+area);
			float sizeScore = calcScoreSize(area, col);
			float shapeScore = calcScoreShape(area, col);
			float locationScore = calcScoreLocation(area, col, sizeScore);
			float rgbScore = calcScoreRGB(area, col);
			float score = sizeScore * shapeScore * locationScore * rgbScore;
			if (debug && debugDetails) System.err.println("    score:        "+score);
			scoreSum += score;
		}
		if (debug && debugDetails) System.err.println("  scoreSum:   "+scoreSum);
		
		float scoreSqr = (float) Math.sqrt(scoreSum);
		if (debug && debugDetails) System.err.println("  scoreSqr:   "+scoreSqr);
		return scoreSqr;
	}
	
	/**
	 * Calculates size score (large is better)
	 */
	private float calcScoreSize(Area area, Column col) {
		
		float ratio = 1.0f * area.getMaxDim() / (col.getMinorDim() * 0.9f);
		if (ratio > 1.0f) ratio = 1.0f;
		float score = (float) Math.pow(ratio, 2);
		
		if (debug && debugDetails) {
			System.err.println("    sizeRatio:    "+ratio);
			System.err.println("    sizeScore:    "+score);
		}
		
		return score;
	}
	
	/**
	 * Calculates shape score (square is better)
	 */
	private float calcScoreShape(Area area, Column col) {
		
		float ratio = area.getRatio();
		// allow small deviation from ideal square without penalty
		float score = Util.scale(ratio, 0.0f, 0.9f, 0.0f, 1.0f);
		float exponent = 1.2f;
		score = (float) Math.pow(score, exponent);
		
		if (debug && debugDetails) {
			System.err.println("    shapeRatio:   "+ratio);
			System.err.println("    shapeScore:   "+score);
		}
		
		return score;
	}
	
	/**
	 * Calculates location score (center is better)
	 */
	private float calcScoreLocation(Area area, Column col, float sizeScore) {
		
		int firstEdge;
		int secondEdge;
		if (col.vertical) {
			firstEdge = area.getX() - col.getX();
			secondEdge = col.getMaxX() - area.getMaxX();
		} else {
			firstEdge = area.getY() - col.getY();
			secondEdge = col.getMaxY() - area.getMaxY();
		}
		int diff = Math.abs(firstEdge - secondEdge);
		float diffPrct = 1.0f * diff / col.getMinorDim();
		diffPrct = Util.scale(diffPrct, 0.1f, 1.0f, 0.0f, 1.0f);
		float exponent = Util.scale(sizeScore, 0.2f, 0.8f, 6f, 3f);
		float locationScore = (float) Math.pow(1.0f - diffPrct, exponent);
		
		if (debug && debugDetails) {
			System.err.println("    diffPrct:     "+diffPrct);
			System.err.println("    locationScore:"+locationScore);
		}
		
		return locationScore;
	}
	
	/**
	 * Calculates RGB value difference (smaller is better)
	 */
	private float calcScoreRGB(Area area, Column col) {
		
		int rgbDelta = (int) (area.minRGB - col.getMinRGBValue());
		float rgbScore = Util.scale(rgbDelta, 50, 100, 1.0f, 0.4f);
		
		if (debug && debugDetails) {
			System.err.println("    rgbDelta:     "+rgbDelta);
			System.err.println("    rgbScore:     "+rgbScore);
		}
		
		return rgbScore;
	}
	
	/**
	 * Prevents column expansion into background elements such as speech bubbles and
	 * divider lines
	 * 
	 * @return true if column is valid
	 */
	private boolean checkBackground(Column col) {
		
		// first check only border pixels since it is faster,
		// only check inside the column if necessary
		
		int borderPixels = task.countPixels(col.rect, true, false);

		if (borderPixels >= 2) {
			int insidePixels = task.countPixels(col.rect, true, true);  
			if (insidePixels >= col.getMinorDim()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * true if debugging is on at this moment 
	 */
	private boolean debug = false;
	
	/**
	 * @return true if column should be debugged. debug state is saved to debug variable.
	 */
	private boolean isDebug(Column col) {
		
		if (debugAll) {
			debug = true;
		} else if (debugRect != null) {
			debug = col.intersects(debugRect);
		} else {
			debug = false;
		}
		
		return debug;
	}
	
	/**
	 * Generates debug image from the current algorithm state
	 */
	private void addIntermediateDebugImage(Column col, Rectangle probe) throws Exception {
		
		col.changed = true;
		
		List<Column> columns = index.getAll();
		List<Area> areas = new ArrayList<>();
		for (Column column : columns) {
			areas.addAll(column.areas);
		}
		
		///task.addDefaultDebugImage("columns", areas, columns, par.vertical);
		
		BufferedImage image = task.createDefaultDebugImage(areas, columns);
		if (probe != null) {
			ImageUtil.paintRectangle(image, probe, Color.GREEN);
		}
		task.addDebugImage(image, "columns", col.vertical);
		
		col.changed = false;
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		
		task.addDefaultDebugImage("columns", par.vertical);
	}
}