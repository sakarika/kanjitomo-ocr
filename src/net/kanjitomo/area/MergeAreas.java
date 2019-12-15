package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kanjitomo.util.ImageUtil;

/**
 * Merges thin neighbour areas in up/down direction (vertical orientation) or 
 * left/right direction (horizontal orientation)
 */
public class MergeAreas extends AreaStep {
	
	public MergeAreas(AreaTask task) {
		super(task, "mergeareas");
	}
	
	/**
	 * Generates debug images only for columns containing this rectangle
	 */
	private final Rectangle debugRect = null; // new Rectangle(320,32,90,149);
	
	/**
	 * Generates debug images for all columns.
	 * 
	 * Warning! This will create large amount of data, use only for small target images.
	 */
	private final boolean debugAll = false;
	
	@Override
	protected void runImpl() throws Exception {
		
		for (Column col : task.columns) {
			mergeAreas(col);
		}
	}
	
	/**
	 * Maximum area size along major dimension compared to minor dimensions.
	 * 
	 * (vertical) area height must be below (column width * MAX_AREA_SIZE)
	 */
	private static final float MAX_AREA_SIZE = 1.5f;
	
	/**
	 * Maximum number of areas that are tested for combinations at once.
	 */
	private static final int MAX_CHUNK_SIZE = 10;
	
	/**
	 * Target area size (major axis). Most areas in the column are expected to be
	 * close to this size. 
	 */
	private int targetSize;
	
	/**
	 * Maximum area size (major axis). Areas are not allowed to be larger than this.
	 */
	private int maxSize;
	
	/**
	 * Loop and merge adjacent areas inside column until no merges can be done
	 */
	private void mergeAreas(Column col) throws Exception {
		
		// split areas into chunks delimited by punctuation and large areas that 
		// can't be merged. for areas inside each chunk, try all merge combinations
		// and selected the best
		
		float scale = calcScale(col);
		targetSize = (int) Math.ceil(col.getMinorDim() * scale);
		maxSize = (int) Math.ceil(col.getMinorDim() * MAX_AREA_SIZE * scale);
		
		List<Area> chunk = new ArrayList<Area>();
		int chunkStart = -1;

		for (int i=0 ; i<col.areas.size() ; i++) {
			
			Area area = col.areas.get(i);
			
			boolean punctuation = area.isPunctuation();
			boolean lastAreaInCol = i == col.areas.size()-1;
			boolean lastAreaInChunk = punctuation || lastAreaInCol;
			
			if (!punctuation) {
				// add new area to chunk
				chunk.add(area);
				if (chunk.size() == 1) {
					chunkStart = i;
				}
				// check if chunk should end
				if (chunk.size() == MAX_CHUNK_SIZE) {
					lastAreaInChunk = true;
				} else if (!lastAreaInCol) {
					Area next = col.areas.get(i+1);
					Area test = area.merge(next);
					if (test.getMajorDim() > maxSize) {
						lastAreaInChunk = true;
					}
				}
			}
			
			if (lastAreaInChunk && chunk.size() > 0) {
				// find best merge combination for chunk's areas
				List<Area> mergedAreas = findBestMerge(col, chunk);
				// replace original areas with merged areas
				for (int j = 0; j<chunk.size() ; j++) {
					col.areas.remove(chunkStart);
				}
				col.areas.addAll(chunkStart, mergedAreas);
				i = chunkStart + mergedAreas.size() - 1;
				// start next iteration from this iteration's last area,
				// possibly continuing merge
				if (!punctuation && !lastAreaInCol && chunk.size() > 1) {
					i--;
				}
				chunk.clear();
			}
		}
	}
		
	/**
	 * Finds best merge by checking all combinations for area list. Merges are scored
	 * by comparing area heights to column width (in vertical orientation).
	 * 
	 * @areas This list should be relatively short and not contain any punctuation 
	 * @nextCombination Which areas should be merged next? Example: 212 -> combine first and
	 * last two areas.
	 * 
	 * @return List of merged areas with best score
	 */
	private List<Area> findBestMerge(Column col, List<Area> areas) throws Exception {
		
		if (areas.size() == 1) {
			return areas;
		}
		
		if (isDebug(areas)) {
			System.err.println("findBestMerge");
			System.err.println("col:"+col);
			System.err.println("areas:"+toString(areas));
		}
		
		boolean[] combination = new boolean[areas.size()];

		float bestScore = 0f;
		List<Area> bestAreas = areas;
		String bestCombination = debug ? toString(combination) : null;
		
		do {
			
			List<Area> mergedAreas = mergeAreas(areas, combination);
			if (mergedAreas != null) {
				if (debug) {
					addIntermediateDebugImage(col, areas, mergedAreas);
					System.err.println(task.debugImages.get(task.debugImages.size()-1).getFilename());
					System.err.println("  combination:"+toString(combination));
				}
				Float score = calcScore(mergedAreas);
				if (score != null && score > bestScore) {
					bestScore = score;
					bestAreas = mergedAreas;
					if (debug) bestCombination = toString(combination);
				}
				if (debug) System.err.println("  score:"+score);
			}
			
			nextCombination(combination);
			
		} while (!combination[combination.length-1]); // last boolean must be false
		
		if (debug) {
			System.err.println("best:"+bestCombination);
			System.err.println("score:"+bestScore);
			System.err.println("areas:"+toString(bestAreas));
		}
		
		return bestAreas;
	}
	
	/**
	 * Advance combination by one. For example: ftff -> ttff -> fftf -> tftf
	 */
	private static void nextCombination(boolean[] combination) {
		
		for (int i=0 ; i<combination.length ; i++) {
			combination[i] = !combination[i]; 
			if (combination[i]) {
				break;
			}
		}
	}
	
	/**
	 * Distance between merged areas that resulted in indexed area. 0 if no merges were done
	 */
	private Map<Area, Integer> mergeDistances;
	
	/**
	 * Merges areas with given combination
	 * 
	 * @param combination If set, merges the indexed area with next area.
	 * 
	 * @return List of areas merged by combination. null if invalid combination.
	 * mergeDistances map is also updated as a side effect.
	 */
	private List<Area> mergeAreas(List<Area> areas, boolean[] combination) {
		
		List<Area> mergedAreas = new ArrayList<Area>();
		mergeDistances = new HashMap<Area, Integer>();

		Area prev = null;
		int maxDistance = 0;
		for (int i=0 ; i<combination.length ; i++) {
			
			Area area = areas.get(i);
			if (prev != null)  {
				
				int distance;
				if (area.column.vertical) {
					distance = area.getY() - prev.getMaxY();
				} else {
					distance = area.getX() - prev.getMaxX();
				}
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			
				area = prev.merge(area);
				if (area.getMajorDim() > maxSize) {
					return null;
				}
				area.changed = true;
			}
			
			if (combination[i]) {
				prev = area;
			} else {
				mergedAreas.add(area);
				mergeDistances.put(area, maxDistance);
				prev = null;
			}
		}
		// last boolean is always false
		
		return mergedAreas;
	}
	
	/**
	 * Determines the scale factor used for size parameters. This is done
	 * to account for compressed fonts that are not perfect squares.
	 */
	private float calcScale(Column col) {
		
		if (col.vertical) {
			// it seems that only horizontal title columns have compressed fonts
			return 1.0f;
		}
		
		if (col.areas.size() < 15) {
			// scale detection is not reliable for small columns, use the default
			return 1.0f;
		}
		
		// calculate upper quartile width
		List<Area> sortedAreas = new ArrayList<Area>();
		sortedAreas.addAll(col.areas);
		Collections.sort(sortedAreas, new Comparator<Area>() {

			@Override
			public int compare(Area o1, Area o2) {

				Integer i1 = o1.getWidth();
				Integer i2 = o2.getWidth();
				
				return i1.compareTo(i2);
			}
		});
		int floor = (int) Math.floor(col.areas.size()*0.75f);
		int ceil = (int) Math.ceil(col.areas.size()*0.75f);
		int width1 = sortedAreas.get(floor).getWidth();
		int width2 = sortedAreas.get(ceil).getWidth();
		int width = (width1 + width2) / 2;

		// calculate scale
		int reference = col.getHeight();
		float scale = (float) width/reference;
		if (scale > 0.8f) scale = 1.0f;
		if (scale < 0.6f) scale = 0.6f;
		if (debug && scale != 1.0f) System.err.println("col:"+col+" scale:"+scale);
		
		return scale;
	}
	
	private String toString(List<Area> areas) {
		
		String s = "";
		for (Area area : areas) {
			s += area+" ";
		}
		return s;
	}
	
	private float calcScore(List<Area> areas) {
		
		if (debug) System.err.println("  targetSize:"+targetSize+" maxSize:"+maxSize);

		float scoreSum = 0f;
		
		for (int i=0 ; i<areas.size() ; i++) {
			
			Area area = areas.get(i);
			int size = area.getMajorDim();
			
			float score;
			if (size <= targetSize) {
				float ratio = (float) size / targetSize;
				//if (ratio < 0.1f) ratio = 0.1f;
				score = (float) Math.pow(ratio, 1.0f);
			} else {
				float ratio = 1 - (float) (size - targetSize) / (maxSize - targetSize);
				score = (float) Math.pow(ratio, 1.5f);
			}
			
			int distance = mergeDistances.get(area);
			if (distance > maxSize) distance = maxSize;
			float distanceRatio = 1.0f * distance / maxSize;
			score *= Math.pow(1f - distanceRatio, 1.0f);
			
			//if (debug) System.err.println("  area:"+area+" size:"+size+" score:"+score);
			if (debug) System.err.println("  area:"+area+" size:"+size+" distance:"+distance+" score:"+score);
			scoreSum += score;
		}
		
		return scoreSum / areas.size();
	}
	
	private static String toString(boolean[] array) {
		
		String s = "";
		for (boolean b : array) {
			if (b) {
				s += "t";
			} else {
				s += "f";
			}
		}
		return s;
	}
	
	/**
	 * true if debugging is on at this moment 
	 */
	private boolean debug = false;
	
	/**
	 * @return true if area list should be debugged. debug state is saved to debug variable.
	 */
	private boolean isDebug(List<Area> areas) {
		
		if (debugAll) {
			debug = true;
			return debug;
		}
		
		if (debugRect != null) {
			for (Area area : areas) {
				if (area.intersects(debugRect)) {
					debug = true;
					return debug;
				}
			}
		}
		
		debug = false;
		return debug;
	}
	
	/**
	 * Generates debug image from the current algorithm state
	 * 
	 * @param chunkAreas Original areas in the current chunk
	 * @param mergedAreas Merged areas in the current chunk
	 */
	private void addIntermediateDebugImage(Column col, List<Area> chunkAreas, List<Area> mergedAreas)
			throws Exception {
		
		task.collectAreas();
		
		// paint merged areas inside chunk
		List<Area> areas = new ArrayList<Area>();
		for (Area area : task.areas) {
			if (!chunkAreas.contains(area)) {
				areas.add(area);
			}
		}
		areas.addAll(mergedAreas);
		BufferedImage image = task.createDefaultDebugImage(areas, task.columns);
		
		// paint rectangle around chunk
		Rectangle chunkRect;
		Area firstArea = chunkAreas.get(0);
		Area lastArea = chunkAreas.get(chunkAreas.size()-1);
		if (col.vertical) {
			chunkRect = new Rectangle(col.getX() - 1, firstArea.getY(),
					col.getWidth() + 2, lastArea.getMaxY() - firstArea.getY() + 1); 
		} else {
			chunkRect = new Rectangle(firstArea.getX(), col.getY() - 1,
					lastArea.getMaxX() - firstArea.getX() + 1, col.getHeight() + 2);
		}
		ImageUtil.paintRectangle(image, chunkRect, Color.GREEN);
		
		task.addDebugImage(image, "mergeareas", col.vertical);
	}
	
	@Override
	protected void addDebugImages() throws Exception {

		task.addDefaultDebugImage("mergeareas", par.vertical);
	}
}
