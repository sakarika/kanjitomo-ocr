package net.kanjitomo.area;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Find columns that contain furigana
 */
public class FindFurigana extends AreaStep {

	public FindFurigana(AreaTask task) {
		super(task, "furigana");
	}

	private RTree<Column> index;
	
	@Override
	protected void runImpl() throws Exception {

		// create column index
		index = new RTree<Column>(task.binaryImage, task.columns);
		
		// find furigana columns
		for (Column col : task.columns) {
			findFurigana(col);
		}
	}
	
	/**
	 *	Finds argument column's furigana columns.
	 */
	private void findFurigana(Column col) {
		
		// Furigana column must be close (right side if vertical, above if horizontal) 
		// but much thinner than main column.
		
		Rectangle probe;
		if (col.vertical) {
			probe = new Rectangle(col.getMaxX() + 1, col.getY(),
						col.getWidth()/2, col.getHeight());
		} else {
			probe = new Rectangle(col.getX(), col.getY() - col.getHeight()/2 - 1,
						col.getWidth(), col.getHeight()/2);
		}
		
		if (checkBackground(probe)) {
			return;
		}
		
		List<Column> furiganaCols = new ArrayList<Column>();
		for (Column col2 : index.get(probe, col)) {
			
			if (col2.getMinorDim() < col.getMinorDim()*0.55f &&
				col2.getMinorDim() > col.getMinorDim()*0.20f &&
				col2.getMajorDim() < col.getMajorDim()*1.05f && 
				col2.getMedianAreaSize() < col.getMedianAreaSize()*0.5f
				) {
				
				furiganaCols.add(col2);
			}
		}
		
		for (Column furigana : furiganaCols) {
			furigana.furigana = true;
			furigana.changed = true;
			col.furiganaColumns.add(furigana);			
		}
	}
	
	/**
	 * @return true if probe intersects with the background
	 */
	private boolean checkBackground(Rectangle probe) {
		
		int pixels = task.countPixels(probe, true, false);
		if (pixels >= 2) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	protected void addDebugImages() throws Exception {

		task.addDefaultDebugImage("furigana", par.vertical);
	}
}
