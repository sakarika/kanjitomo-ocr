package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import net.kanjitomo.util.ImageUtil;

/**
 * Finds columns that are connected to each other (continue text) in reading direction.
 */
public class FindConnections extends AreaStep {
	
	private boolean debug = false;
	
	public FindConnections(AreaTask task) {
		super(task, "connections");
	}
	
	private RTree<Column> index;
	
	@Override
	protected void runImpl() throws Exception {
		
		// initialize index
		index = new RTree<Column>(task.binaryImage, task.columns);
		
		// find next column for each column
		for (Column col1 : task.columns) {
			findNextColumn(col1);
		}
	}
	
	/**
	 * Finds columns that are closest to argument column in reading direction.
	 * Normally this is only one column.
	 */
	private void findNextColumn(Column column) {
		
		if (debug) System.err.println("column:"+column);
				
		// find closest column in reading direction
		
		float probeSizeFactor = 1.75f;
		Rectangle probe;
		if (par.vertical) {
			int probeSize = (int)Math.ceil(column.getWidth()*probeSizeFactor);
			probe = new Rectangle(column.getX() - probeSize - 1, column.getY() - probeSize/2,
					probeSize, probeSize);
		} else {
			int probeSize = (int)Math.ceil(column.getHeight()*probeSizeFactor);
			probe = new Rectangle(column.getX() - probeSize/2, column.getMaxY() + 1,
					probeSize, probeSize);
		}
		
		if (debug) System.err.println("  probe:"+probe);
		
		List<Column> targetColumns = index.get(probe);
		
		// find closest column
		Column target = null;
		float distance = 100000f;
		for (Column tempCol : targetColumns) {

			if (column == tempCol) {
				continue;
			}
			if (tempCol.isFurigana()) {
				continue;
			}
			if (par.vertical) {
				if (!probe.contains(tempCol.getMaxX(), tempCol.getY())) {
					continue;
				}
			} else {
				if (!probe.contains(tempCol.getX(), tempCol.getY())) {
					continue;
				}
			}
			
			Point start = getConnectionStartPoint(column);
			Point end = getConnectionEndPoint(tempCol);
			float tempDist = (float) start.distance(end);
			
			if (tempDist < distance) {
				target = tempCol;
				distance = tempDist;
			}
		}
		
		if (target == null) {
			if (debug) System.err.println("  no target found");
			return;
		}
		
		if (debug) System.err.println("  target:"+target+" distance:"+distance);
		
		// connected columns should not form a tree
		if (target.previousColumn != null) {
			if (debug) System.err.println("  already connected");
			return;
		}
		
		// make sure that there's no divider between columns
		// test from endpoints
		Rectangle testBackground = ImageUtil.createRectangle(
				getConnectionStartPoint(column),
				getConnectionEndPoint(target));
		int backgroundPixels = task.countPixels(testBackground, true, true);
		if (backgroundPixels >= 2) {
			if (debug) System.err.println("  rejected from endpoint");
			return;
		}
		// test from midpoint
		testBackground = ImageUtil.createRectangle(
				column.getMidpoint(),
				getConnectionEndPoint(target));
		backgroundPixels = task.countPixels(testBackground, true, true);
		if (backgroundPixels >= 2) {
			if (debug) System.err.println("  rejected from midpoint");
			return;
		}
			
		// check that columns have roughly the same width
		int columnWidth = column.getMinorDim();
		int targetWidth = target.getMinorDim();
		float limit = 0.75f;
		if (columnWidth < targetWidth*limit || targetWidth < columnWidth*limit) {
			if (debug) System.err.println("  rejected from width");
			return;
		}
		
		// check that there's not another column between
		Rectangle testAnotherColumn = ImageUtil.createRectangle(
				getConnectionStartPoint(column),
				getConnectionEndPoint(target));
		for (Column col : index.get(testAnotherColumn)) {
			if (col == column || col == target) {
				continue;
			}
			if (debug) System.err.println("  rejected from crossing another column");
			return;
		}
		
		// mark the connection
		column.nextColumn = target;
		target.previousColumn = column;
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		
		BufferedImage image = task.createDefaultDebugImage();
		Graphics2D g = image.createGraphics();
		g.setPaint(Color.BLUE);
		for (Column column : task.columns) {
			paintNextColumn(column, g);
		}
		task.addDebugImage(image, "connections", par.vertical);
	}
	
	private Point getConnectionStartPoint(Column column) {
		
		if (column.vertical) {
			return new Point(column.getX(), column.getY());
		} else {
			return new Point(column.getX(), column.getMaxY());
		}
	}
	
	private Point getConnectionEndPoint(Column column) {
		
		if (column.vertical) {
			return new Point(column.getMaxX(), column.getY());
		} else {
			return new Point(column.getX(), column.getY());
		}
	}
	
	/**
	 * Paints a line from column to nextColumn
	 */
	private void paintNextColumn(Column column, Graphics2D g) {
		
		if (column == null) {
			return;
		}
		
		Column nextColumn = column.nextColumn;
		if (nextColumn == null) {
			return;
		}
		
		Point start = getConnectionStartPoint(column);
		Point end = getConnectionEndPoint(nextColumn);
		g.drawLine(start.x, start.y, end.x, end.y);
	}
}
