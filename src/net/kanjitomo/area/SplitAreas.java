package net.kanjitomo.area;

/**
 * Splits too long areas
 */
public class SplitAreas extends AreaStep {

	public SplitAreas(AreaTask task) {
		
		super(task, "splitareas");
	}

	/** Area must be longer than this to be considered for splitting */
	private final float splitMinLength = 1.25f;
	
	/** Split is considered starting from this line */
	private final float scanFrom = 0.25f;
	
	/** Split is considered until this line (% of major dimension) */
	private final float scanTo = 0.75f;
	
	/** Split is possible in line with this many or less ratio of pixels */
	private final float maxPixelsPrct = 0.14f;
	
	@Override
	protected void runImpl() throws Exception {
		
		for (Column col : task.columns) {
			int minLength = (int) Math.ceil(col.getMinDim()*splitMinLength);
			for (int i=0 ; i<col.areas.size() ; i++) {
				Area area = col.areas.get(i);
				if (area.getHeight() < 10 && area.getWidth() < 10) {
					continue;
				}
				int refLength = par.vertical ? area.getHeight() : area.getWidth();
				if (refLength > minLength) {
					if (splitArea(area, col)) {
						i--; // splitted areas are added to the end of list
					}
				}
			}
		}
	}
	
	private boolean splitArea(Area area, Column col) {
		
		if (par.vertical) {
			return splitVertical(area, col);
		} else {
			return splitHorizontal(area, col);
		}
	}
	
	private boolean splitVertical(Area area, Column col) {
		
		// find line with least amount of pixels 
		
		int minY = area.getY() + (int) Math.floor(area.getHeight() * scanFrom);
		int maxY = area.getY() + (int) Math.ceil(area.getHeight() * scanTo);
		int minPixels = (int) Math.ceil(area.getWidth() * maxPixelsPrct) + 1;
		int splitAt = -1;

		if (minY <= 0 || maxY >= task.height-1) {
			return false;
		}
		
		// iterate from the middle: 5,6,4,7,3,8,...

		int delta = 0;
		for (int y = minY + (maxY - minY)/2 ; y >= minY && y <= maxY ;
				y = (delta%2 == 0 ? y + delta : y - delta)) { 
			
			int pixels = task.countPixelsHorizontal(area.getX(), area.getMaxX(), y);
			if (pixels < minPixels) {
				minPixels = pixels;
				splitAt = y;
			}
			++delta;
			if (delta == (maxY - minY)/4) {
				// give priority to center
				minPixels = (int)Math.floor(minPixels * 0.9f);
			}
		}
		
		if (splitAt > -1) {
			
			// check which area should splitAt line be assigned
			int up = 0;
			int down = 0;
			for (int x = col.getX() ; x <= col.getMaxX() ; x++) {
				if (task.getPixel(x, splitAt)) {
					if (task.getPixel(x, splitAt-1)) {
						++up;
					}
					if (task.getPixel(x, splitAt+1)) {
						++down;
					}
				}
			}
			if (up > down) {
				splitAt++;
			}
			
			int index = col.areas.indexOf(area);
			col.areas.remove(area);
			col.areas.addAll(index, area.splitY(splitAt));
			return true;
			
		} else {
			return false;
		}
	}
	
	private boolean splitHorizontal(Area area, Column col) {
		
		// find line with least amount of pixels 
		
		int minX = area.getX() + (int) Math.floor(area.getWidth() * scanFrom);
		int maxX = area.getX() + (int) Math.ceil(area.getWidth() * scanTo);
		int minPixels = (int) Math.ceil(area.getHeight() * maxPixelsPrct);
		int splitAt = -1;
		
		if (minX <= 0 || maxX >= task.width-1) {
			return false;
		}
		
		// iterate from the middle: 5,6,4,7,3,8,...
		
		int delta = 0;
		for (int x = minX + (maxX - minX)/2 ; x >= minX && x <= maxX ;
				x = (delta%2 == 0 ? x + delta : x - delta)) {
			
			int pixels = task.countPixelsVertical(x, area.getY(), area.getMaxY());
			if (pixels < minPixels) {
				minPixels = pixels;
				splitAt = x;
			}
			++delta;
			if (delta == (maxX - minX)/4) {
				// give priority to center
				minPixels = (int)Math.floor(minPixels * 0.9f);
			}
		}

//		// iterate pairs of lines, find pair that don't have any touching pixels 
//		
//		if (splitAt == -1) {
//			pairs: for (int x = minX ; x < maxX-1 ; x++) {
//				for (int y = area.getY() ; y <= area.getMaxY() ; y++) {
//					if (task.hasPixel(x, y) && task.hasPixel(x+1, y)) {
//						continue pairs;
//					}
//				}
//				splitAt = x;
//				break pairs;
//			}
//		}
		
		if (splitAt > -1) {
			
			// check which area should splitAt line be assigned
			int left = 0;
			int right = 0;
			for (int y = col.getY() ; y <= col.getMaxY() ; y++) {
				if (task.getPixel(splitAt, y)) {
					if (task.getPixel(splitAt-1, y)) {
						++left;
					}
					if (task.getPixel(splitAt+1, y)) {
						++right;
					}
				}
			}
			if (left > right) {
				splitAt++;
			}
			
			int index = col.areas.indexOf(area);
			col.areas.remove(area);
			col.areas.addAll(index, area.splitX(splitAt));
			return true;
			
		} else {
			return false;
		}
	}
	
	@Override
	protected void addDebugImages() throws Exception {

		task.addDefaultDebugImage("splitareas", par.vertical);
	}
}
