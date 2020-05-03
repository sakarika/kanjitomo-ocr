package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.kanjitomo.CharacterColor;
import net.kanjitomo.util.DebugImage;
import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Parameters;

/**
 * Area detection algorithm input and output values.
 */
public class AreaTask {
	
	private Parameters par = Parameters.getInstance();
	
	public AreaTask(BufferedImage targetImage) {
		this.width = targetImage.getWidth();
		this.height = targetImage.getHeight();
		this.originalImage = targetImage;
	}
	
	/**
	 * Target image width
	 */
	int width;
	
	/**
	 * Target image height
	 */
	int height;
	
	/**
	 * Original image (read from file or captured from screen).
	 */
	BufferedImage originalImage;
		
	/**
	 * Target image after unsharp mask filter. Used to increase contranst.
	 */
	BufferedImage sharpenedImage;
	
	/**
	 * Image is divided into InvertImage.BLOCK_SIZE^2 blocks. Colors in each block can be inverted
	 * if it contains white text on dark background. This matrix marks the inverted blocks as true.
	 */
	boolean[][] inverted;
	
	/**
	 * Image that contains only black or white pixels (true = black).
	 * Parameters.blackThreshold is used as a threshold for determining which pixels are considered black.
	 */
	boolean[][] binaryImage;
	
	/**
	 * Binary image of the background outside areas
	 */
	boolean[][] backgroundImage;
	
	/**
	 * Pixels that define black/white background border
	 */
	boolean[][] borderPixels;
	
	/**
	 * Connected groups on pixels in target image that represent (candidate) characters
	 */
	List<Area> areas;
	
	/**
	 * Areas that are close to each other in reading direction are grouped into columns.
	 */
	List<Column> columns;
	
	/**
	 * Column in vertical orientation.
	 */
	List<Column> verticalColumns;
	
	/**
	 * Column in horizontal orientation.
	 */
	List<Column> horizontalColumns;
	
	/**
	 * List of images used during development to visualize processing steps.
	 * Only generated if debugMode = true;
	 */
	List<DebugImage> debugImages = new ArrayList<>();
	
	/**
	 * Collects areas from columns into areas list 
	 */
	void collectAreas() {
		
		areas = new ArrayList<Area>();
		for (Column col : columns) {
			areas.addAll(col.areas);
		}
	}
	
	/**
	 * Clears all changed flags from areas and columns.
	 * Used to highlight changes in debug images.
	 */
	void clearChangedFlags() {
		
		if (areas != null) {
			for (Area area : areas) {
				area.changed = false;
			}
		}
		
		if (columns != null) {
			for (Column column : columns) {
				column.changed = false;
			}
		}
	}
	
	/**
	 * Gets the closest area near point
	 * 
	 * @param point Mouse cursor location relative to target image
	 */
	public Area getArea(Point point) {
		
		int minDistance = 1000000;
		Area closestArea = null;
	
		for (Area area : areas) {
			if (area.isPunctuation()) {
				continue;
			}
			int distance = (int)area.getMidpoint().distance(point);
			if (distance < minDistance) {
				minDistance = distance;
				closestArea = area;
			}
		}
		
		if (closestArea == null) {
			// no valid areas found
			return null;
		} else if (minDistance > closestArea.getMaxDim()) {
			// closest area is too far away
			return null;
		} else {
			return closestArea;
		}
	}
	
	/**
	 * Checks if the binary image has black pixel at x,y
	 */
	public boolean getPixel(int x, int y) {
		
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return false;
		}
		
		return binaryImage[x][y];
	}
	
	public void setPixel(int x, int y, boolean value) {
		
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}
		
		binaryImage[x][y] = value;
	}
	
	/**
	 * Checks if the image has border pixel (between black and white areas) at x,y
	 */
	public boolean getBorderPixel(int x, int y) {
		
		if (x < 0 || y < 0 || x >= width || y >= height || borderPixels == null) {
			return false;
		}
		
		return borderPixels[x][y];
	}
	
	/**
	 * Sets pixel on black/white region border
	 */
	public void setBorderPixel(int x, int y, boolean value) {
		
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}
		
		borderPixels[x][y] = value;
	}

	/**
	 * Checks if the binary image has black background pixel at x,y
	 */
	public boolean getBackgroundPixel(int x, int y) {
		
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return false;
		}
		
		return backgroundImage[x][y];
	}
	
	/**
	 * Returns true if image contains any pixels in vertical line
	 * @param black If true, scans for black pixels, else white pixels
	 */
	public boolean containsPixelsVertical(boolean black, int x, int startY, int endY) {
		
		for (int y=startY ; y<=endY ; y++) {
			if (binaryImage[x][y] == black) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if image contains any pixels in horizontal line
	 * @param black If true, scans for black pixels, else white pixels
	 */
	public boolean containsPixelsHorizontal(boolean black, int startX, int endX, int y) {

		for (int x=startX ; x<=endX ; x++) {
			if (binaryImage[x][y] == black) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return true if image contains any black pixels within rect
	 */
	public boolean containsPixels(Rectangle rect) {
		
		for (int y=rect.y ; y<=rect.y + rect.height - 1 ; y++) {
			for (int x=rect.x ; x<=rect.x + rect.width - 1 ; x++) {
				if (getPixel(x, y)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Counts the number of pixels along horizontal line.
	 */
	public int countPixelsHorizontal(int minX, int maxX, int y) {
		
		int pixels = 0;
		for (int x = minX ; x <= maxX ; x++) {
			if (getPixel(x, y)) {
				++pixels;
			}
		}
		return pixels;
	}

	/**
	 * Counts the number of pixels along vertical line.
	 */
	public int countPixelsVertical(int x, int minY, int maxY) {
		
		int pixels = 0;
		for (int y = minY ; y <= maxY ; y++) {
			if (getPixel(x, y)) {
				++pixels;
			}
		}
		return pixels;
	}
	
	/**
	 * Counts the number of pixels in rectangle
	 * 
	 * @param background If true, checks only the background image
	 * @param inside If true, checks also pixels inside the rectangle. If false, only
	 * checks the border pixels
	 */
	public int countPixels(Rectangle rect, boolean background, boolean inside) {
		
		int pixels = 0;
		for (int y=rect.y ; y<=rect.y+rect.height-1 ; y++) {
			for (int x=rect.x ; x<=rect.x+rect.width-1 ; x++) {
				if (inside || y==rect.y || y==rect.y+rect.height-1 || x==rect.x || x==rect.x+rect.width-1) {
					if (x<0 || x>=width || y<0 || y>=height) {
						continue;
					}
					if (background) {
						if (getBackgroundPixel(x, y)) {
							++pixels;
						}
					} else {
						if (getPixel(x, y)) {
							++pixels;
						}
					}
				}
			}
		}
		return pixels;
	}
	
	/**
	 * Gets RGB value of pixel x,y (minimum from single channel)
	 */
	public int getPixelRGB(int x, int y) {
		
		int rgb = originalImage.getRGB(x, y);
		Color color = new Color(rgb);
		int red = color.getRed();
		
		int green = color.getGreen();
		int blue = color.getBlue();
		int min = Math.min(Math.min(red, green), blue);
		
		if (isPixelInverted(x, y)) {
			min = 255 - min;
		}
		
		return min;
	}
	
	/**
	 * @return true if x,y pixels have been inverted.
	 */
	protected boolean isPixelInverted(int x, int y) {
		
		if (Parameters.fixedBlackLevelEnabled || par.colorTarget == CharacterColor.BLACK_ON_WHITE) {
			return false;
		} else if (par.colorTarget == CharacterColor.AUTOMATIC) {
			return inverted[x/InvertImage.BLOCK_SIZE][y/InvertImage.BLOCK_SIZE];
		} else {
			return true;
		}
	}
	
	/**
	 * Gets at most par.ocrMaxCharacters images containing single characters closest 
	 * to the point in reading direction. Returns empty list if none is found.
	 * 
	 * @param point Mouse cursor location relative to target image
	 */
	public List<SubImage> getSubImages(Point point) {
		
		List<SubImage> subImages = new ArrayList<SubImage>();
		
		// find closest area to the point
		Area firstArea = getArea(point);
		if (firstArea == null) {
			return subImages;
		}
		
		// find next areas by following columns
		List<Area> areas = new ArrayList<Area>();
		boolean found = false;
		Column column = firstArea.column;
		loop: while (true) {
			for (Area area : column.areas) {
				if (area == firstArea) {
					found = true;
				}
				if (found && !area.isPunctuation()) {
					areas.add(area);
				}
				if (areas.size() == par.ocrMaxCharacters) {
					break loop;
				}
			}
			if (column.nextColumn == null) {
				// last column reached
				break;
			}
			column = column.nextColumn;
			if (column == firstArea.column) {
				// column loop detected, this might happen with intersecting columns
				// (most likely in wrong orientation)
				break;
			}
		}
		
		// create subimages from areas
		for (Area area : areas) {
			
			// crop from binary image
			BufferedImage croppedImage = ImageUtil.crop(binaryImage, area.getRectangle());
			SubImage subImage = new SubImage(croppedImage, area.getRectangle(), area.column);
			subImages.add(subImage);
		}
		
		return subImages;
	}
	
	/**
	 * Gets subimages from list of rectangles.
	 */
	public List<SubImage> getSubImages(List<Rectangle> areas) {
		
		List<SubImage> subImages = new ArrayList<SubImage>();
		for (Rectangle area : areas) {
			BufferedImage croppedImage = ImageUtil.crop(binaryImage, area);
			SubImage subImage = new SubImage(croppedImage, area, null);
			cropBorder(subImage);
			subImages.add(subImage);
		}
		
		return subImages;
	}
	
	/**
	 * Crops empty border around subimage
	 */
	private void cropBorder(SubImage subImage) {
		
		try {
			
			int minX;
			for (minX=subImage.getMinX() ; minX<subImage.getMidX() ; minX++) {
				if (containsPixelsVertical(true, minX, subImage.getMinY(), subImage.getMaxY())) {
					break;
				}
			}
			
			int maxX;
			for (maxX=subImage.getMaxX() ; maxX>subImage.getMidX() ; maxX--) {
				if (containsPixelsVertical(true, maxX, subImage.getMinY(), subImage.getMaxY())) {
					break;
				}
			}
			
			int minY;
			for (minY=subImage.getMinY() ; minY<subImage.getMidY() ; minY++) {
				if (containsPixelsHorizontal(true, subImage.getMinX(), subImage.getMaxX(), minY)) {
					break;
				}
			}
			
			int maxY;
			for (maxY=subImage.getMaxY() ; maxY>subImage.getMidY() ; maxY--) {
				if (containsPixelsHorizontal(true, subImage.getMinX(), subImage.getMaxX(), maxY)) {
					break;
				}
			}
			
			subImage.location = new Rectangle(minX, minY, maxX-minX+1, maxY-minY+1);
			subImage.image = ImageUtil.crop(binaryImage, subImage.location);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds image to list of debug images
	 * 
	 * @param image Image to be added to list of debug images
	 * 
	 * @param step Short one-word description of the image. For example: "binary"
	 * This appears in file name and can be referenced in par.filterDebugImages 
	 */
	void addDebugImage(boolean[][] image, String step) {
		
		BufferedImage bufferedImage = ImageUtil.createImageFromMatrix(image);
		addDebugImage(bufferedImage, step);
	}
	
	/**
	 * Adds image to list of debug images
	 * 
	 * @param image Image to be added to list of debug images
	 * 
	 * @param step Short one-word description of the image. For example: "binary"
	 * This appears in file name and can be referenced in par.filterDebugImages 
	 */
	void addDebugImage(BufferedImage image, String step) {
		
		addDebugImage(new DebugImage(image, step, null, par.getDebugFilePrefix()));
	}
	
	/**
	 * Adds image to list of debug images
	 * 
	 * @param image Image to be added to list of debug images
	 * 
	 * @param step Short one-word description of the image. For example: "binary"
	 * This appears in file name and can be referenced in par.filterDebugImages 
	 * 
	 * @param vertical If set, orientation is diplayed in the file name
	 */
	void addDebugImage(BufferedImage image, String step, Boolean vertical) {
		
		addDebugImage(new DebugImage(image, step, vertical, par.getDebugFilePrefix()));
	}
	
	/**
	 * Adds image to list of debug images
	 */
	private void addDebugImage(DebugImage image) {
		
		if (debugImages.size() < par.maxDebugImages) {
			debugImages.add(image);
		} else {
			System.err.println("maxDebugImages reached");
		}
	}
	
	public List<Column> getColumns() {
		
		return columns;
	}
	
	public List<Area> getAreas() {
		
		return areas;
	}
	
	/**
	 * Paints and adds a default debug image displaying columns and areas
	 * and gray background.
	 * 
	 * @param vertical if set, orientation is displayed in debug file name
	 */
	void addDefaultDebugImage(String name) throws Exception {
		
		addDefaultDebugImage(name, areas, columns, null);
	}
	
	/**
	 * Paints and adds a default debug image displaying columns and areas
	 * and gray background.
	 * 
	 * @param vertical if set, orientation is displayed in debug file name
	 */
	void addDefaultDebugImage(String name, Boolean vertical) throws Exception {
		
		addDefaultDebugImage(name, areas, columns, vertical);
	}
		
	/**
	 * Paints and adds a default debug image displaying columns and areas
	 * and gray background.
	 * 
	 * @param vertical if set, orientation is displayed in debug file name
	 */
	void addDefaultDebugImage(String name, List<Area> areas, List<Column> columns,
			Boolean vertical) throws Exception {
		
		BufferedImage image = createDefaultDebugImage(areas, columns);
		addDebugImage(image, name, vertical);
	}
	
	/**
	 * Paints a default debug image displaying columns and areas and gray background.
	 */
	BufferedImage createDefaultDebugImage() throws Exception {
		
		return createDefaultDebugImage(areas, columns);
	}
	
	/**
	 * Paints a default debug image displaying columns and areas and gray background.
	 */
	BufferedImage createDefaultDebugImage(List<Area> areas, List<Column> columns) throws Exception {
		
		BufferedImage image = ImageUtil.createGrayImage(binaryImage, backgroundImage);
		image = ImageUtil.paintAreas(image, areas);
		if (columns != null) {
			for (Column col : columns) {
				ImageUtil.paintColumn(image, col);
			}
		}
		return image;
	}
	
	/**
	 * Writes debug images to target directory. filenameBase is included in each file name.
	 */
	public void writeDebugImages() throws Exception {
		
		if (!par.isSaveAreaFailed()) {
			return;
		}
		
		System.out.println("Writing area debug images");
		for (DebugImage image : debugImages) {
			writeDebugImage(image);
		}
	}
	
	/**
	 * Writes debug image to disk in par.debugDir directory. Files are 
	 * named as "test number.index number.algorithm step.png"
	 */
	private void writeDebugImage(DebugImage image) throws Exception {
		
		if (image == null) {
			return;
		}
		
		File targetDir = par.getDebugDir();
				
		String filename = image.getFilename();
		
		check: if (par.debugImages != null && par.debugImages.length > 0) {
			for (String debugImage : par.debugImages) {
				if (filename.contains(debugImage)) {
					break check;
				}
			}
			return;
		}
		
		File file = new File(targetDir+"/"+filename);
		
		int scale = 1;
		int minDim = Math.max(image.getImage().getWidth(), image.getImage().getHeight());
		if (minDim < par.smallDebugAreaImageThreshold) {
			scale = par.smallDebugAreaImageScale;
		}

		BufferedImage scaledImage = ImageUtil.buildScaledImage(image.getImage(), scale);
		if (!par.debugAreaImageToClipboard) {
			ImageIO.write(scaledImage, "png", file);
		} else {
			ImageUtil.setClipboard(scaledImage);
		}
	}
}
