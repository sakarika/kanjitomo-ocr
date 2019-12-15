package net.kanjitomo.area;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.kanjitomo.Orientation;
import net.kanjitomo.util.Parameters;

/**
 * Area detection coordinator.
 */
public class AreaDetector {
	
	private Parameters par = Parameters.getInstance();
	
	private AreaTask task;
	
	public void run(AreaTask task) throws Exception {
		
		this.task = task;
		
		long started = System.currentTimeMillis();
		
		// apply unsharp mask filter to image
		new SharpenImage(task).run();
		
		// create binary black and white image
		new CreateBinaryImage(task).run();
		
		// invert regions with white text on black background
		new InvertImage(task).run();
				
		// group touching pixels into areas
		new FindAreas(task).run();
		
		// areas list is modified during vertical columns detection, make a copy so that 
		// original list can be used as input for horizontal columns detection
		List<Area> areas = task.areas;
		
		// find vertical columns
		if (par.orientationTarget == Orientation.AUTOMATIC || par.orientationTarget == Orientation.VERTICAL) {
			findColumns(true);
			task.verticalColumns = task.columns;
		} else {
			task.columns = new ArrayList<Column>();
		}
				
		// find horizontal columns
		if (par.orientationTarget == Orientation.AUTOMATIC || par.orientationTarget == Orientation.HORIZONTAL) {
			task.areas = areas;
			findColumns(false);
			task.horizontalColumns = task.columns;
		} else {
			task.columns = new ArrayList<Column>();
		}
		
		// filter columns by score so that each area in target image corresponds to only 
		// one column in one orientation.
		new OrientationMerge(task).run();
		
		if (par.isPrintDebug()) {
			long done = System.currentTimeMillis();
			System.out.println("AreaDetector total "+(done - started)+" ms");
		}
		
		checkDebugImages();
	}
	
	/**
	 * Groups areas into columns
	 * 
	 * @vertical If true, uses vertical orientation. If false, uses horizontal
	 * orientation.
	 */
	private void findColumns(boolean vertical) throws Exception {
		
		task.columns = null;
		par.vertical = vertical;
		
		// find columns
		new FindColumns(task).run();
		
		// find dot or comma areas
		new FindPunctuation(task).run();
		
		// split too large areas
		new SplitAreas(task).run();
		
		// merge too small areas 
		//new MergeAreas2(task).run();
		new MergeAreas(task).run();
		
		// find furigana columns
		new FindFurigana(task).run();
		
		// find connections (continuing text) between columns
		new FindConnections(task).run();
	}
	
	/**
	 * Saves debug images if needed
	 */
	private void checkDebugImages() throws Exception {
	
		if (par.isSaveAreaAll()) {
			task.writeDebugImages();
		} else if (par.isSaveAreaFailed()) {
			// check that expected rectangles (areas or columns) are present
			rect: for (Rectangle rect : par.expectedRectangles) {
				for (Column col : task.getColumns()) {
					if (col.rect.equals(rect)) {
						continue rect;
					}
					for (Area area : col.getAreas()) {
						if (area.rect.equals(rect)) {
							continue rect;
						}
					}
				}
				task.writeDebugImages();
				break;
			}
		}
	}
}
