package net.kanjitomo.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imgscalr.Scalr;

import net.kanjitomo.area.Area;
import net.kanjitomo.area.Column;

public class ImageUtil {
	
	/**
	 * Returns true if pixel is determined to be black.
	 * @param rgb
	 * @param blackThreshold If null, uses fixedBlackLevel values instead of single threshold
	 * @return
	 */
	public static boolean containsPixel(int rgb, Integer blackThreshold) {
		
		if (blackThreshold == null) {
			return containsPixelFixedBlackLevel(rgb);
		}
		
		boolean red   = ((rgb & 0x00ff0000) >> 16) < blackThreshold;
		boolean green = ((rgb & 0x0000ff00) >> 8)  < blackThreshold;
		boolean blue  =  (rgb & 0x000000ff)        < blackThreshold;
		
		return (red && green) || (green && blue) || (red && blue); // 2 of 3		
	}

	/**
	 * Returns true if pixel is determined to be black.
	 * Uses fixed black level specified in Parameters.fixedBlackLevel*
	 */
	public static boolean containsPixelFixedBlackLevel(int rgb) {
		
		int red   = ((rgb & 0x00ff0000) >> 16);  
		int green = ((rgb & 0x0000ff00) >> 8);
		int blue  =  (rgb & 0x000000ff);
		
		//System.err.println("red  :"+red+" <-> "+Parameters.fixedBlackLevelRed);
		//System.err.println("green:"+green+" <-> "+Parameters.fixedBlackLevelGreen);
		//System.err.println("blue :"+blue+" <-> "+Parameters.fixedBlackLevelBlue);
		
		if (red < Parameters.fixedBlackLevelRed - Parameters.fixedBlackLevelRange) {
			return false;
		}
		if (red > Parameters.fixedBlackLevelRed + Parameters.fixedBlackLevelRange) {
			return false;
		}
		if (green < Parameters.fixedBlackLevelGreen - Parameters.fixedBlackLevelRange) {
			return false;
		}
		if (green > Parameters.fixedBlackLevelGreen + Parameters.fixedBlackLevelRange) {
			return false;
		}
		if (blue < Parameters.fixedBlackLevelBlue - Parameters.fixedBlackLevelRange) {
			return false;
		}
		if (blue > Parameters.fixedBlackLevelBlue + Parameters.fixedBlackLevelRange) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Builds a larger image, scaled by scale factor. Used for created
	 * larger debug images.
	 */
	public static BufferedImage buildScaledImage(BufferedImage image, int scale) {
		
		BufferedImage target = new BufferedImage(
				image.getWidth() * scale,
				image.getHeight() * scale,
				BufferedImage.TYPE_INT_RGB);
		
		for (int y=0 ; y<image.getHeight() ; y++) {
			for (int x=0 ; x<image.getWidth() ; x++) {
				int rgb = image.getRGB(x, y);
				for (int ty=y*scale ; ty<(y+1)*scale ; ty++) {
					for (int tx=x*scale ; tx<(x+1)*scale ; tx++) {
						target.setRGB(tx, ty, rgb);
					}
				}
			}
		}
		
		return target;
	}
	
	/**
	 * Changes image's color (replaces black pixels)
	 */
	public static BufferedImage colorizeImage(BufferedImage image, Color color) {
		
		BufferedImage target = new BufferedImage(
				image.getWidth(),
				image.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		
		for (int y=0 ; y<image.getHeight() ; y++) {
			for (int x=0 ; x<image.getWidth() ; x++) {
				if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
					target.setRGB(x, y, color.getRGB());
				} else if (image.getRGB(x, y) == Color.WHITE.getRGB()) {
					target.setRGB(x, y, Color.WHITE.getRGB());
				} else {
					throw new Error("Unknown color");
				}
			}
		}
		
		return target;
	}
	
	/**
	 * Creates empty copy of argument image with same dimensions and type
	 */
	public static BufferedImage createEmptyCopy(BufferedImage image) {
		
		int type = image.getType();
		if (type == 0) {
			type = BufferedImage.TYPE_INT_BGR;
		}
		
		return new BufferedImage(image.getWidth(), image.getHeight(), type);
	}
	
	/**
	 * Creates a copy of argument image
	 */
	public static BufferedImage createCopy(BufferedImage image) {
		
		BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		for (int x=0 ; x<image.getWidth() ; x++) {
			for (int y=0 ; y<image.getHeight() ; y++) {
				int rgb = image.getRGB(x, y);
				copy.setRGB(x, y, rgb);
			}
		}
		
		return copy;
	}
	
	/**
	 * Creates a copy of argument image
	 */
	public static BufferedImage createCopy(boolean[][] image) {
		
		int width = image.length;
		int height = image[0].length;
		
		BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x=0 ; x<width ; x++) {
			for (int y=0 ; y<height ; y++) {
				if (!image[x][y]) {
					copy.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		
		return copy;
	}
	
	/**
	 * Creates BufferedImage from boolean matrix
	 */
	public static BufferedImage createImageFromMatrix(boolean[][] image) {

		int width = image.length;
		int height = image[0].length;
		BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x=0 ; x<width ; x++) {
			for (int y=0 ; y<height ; y++) {
				if (!image[x][y]) {
					bImage.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		return bImage;
	}
	
	/**
	 * Creates boolean matrix from BufferedImage
	 */
	public static boolean[][] createMatrixFromImage(BufferedImage image) {

		int width = image.getWidth();
		int height = image.getHeight();
		boolean[][] matrix = new boolean[width][height];
		for (int x=0 ; x<width ; x++) {
			for (int y=0 ; y<height ; y++) {
				if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
					matrix[x][y] = true;
				}
			}
		}
		return matrix;
	}
		
	/**
	 * Creates BufferedImage where foreground pixels are black and background pixels are gray
	 */
	public static BufferedImage createGrayImage(boolean[][] image, boolean[][] backgroundImage) {
		
		int width = image.length;
		int height = image[0].length;
		BufferedImage bImage = createImageFromMatrix(image);
		for (int x=0 ; x<width ; x++) {
			for (int y=0 ; y<height ; y++) {
				boolean pixel = image[x][y];
				boolean backgroundPixel = backgroundImage[x][y];
				if (pixel && !backgroundPixel) {
					bImage.setRGB(x, y, Color.BLACK.getRGB());	
				} else if (backgroundPixel) {
					bImage.setRGB(x, y, Color.GRAY.getRGB());
				} else {
					bImage.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		
		return bImage;
	}
	
	/**
	 * Creates empty copy of argument image with same dimensions and type.
	 * Fill with white.
	 */
	public static BufferedImage createWhiteCopy(BufferedImage image) {
		
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		Graphics2D g = newImage.createGraphics();
		g.setPaint(Color.WHITE);
		g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
		
		return newImage;
	}
	
	/**
	 * Creates empty white image of same size as argument image
	 */
	public static BufferedImage createWhiteImage(boolean[][] image) {
		
		int width = image.length;
		int height = image[0].length;
		
		return createWhiteImage(width, height);
	}
	
	/**
	 * Creates empty white image of given size.
	 */
	public static BufferedImage createWhiteImage(int width, int height) {

		BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = newImage.createGraphics();
		g.setPaint(Color.WHITE);
		g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
		
		return newImage;
	}
	
	/**
	 * Paints areas into argument image using default colors
	 */
	public static BufferedImage paintAreas(BufferedImage image, List<Area> areas) throws Exception {
		
		BufferedImage newImage = ImageUtil.createCopy(image);
		
		for (Area area : areas) {
			Color col = Color.RED;
			if (area.isPunctuation()) {
				col = Color.LIGHT_GRAY;
			}
			if (area.column != null && area.column.isFurigana()) {
				col = Color.LIGHT_GRAY;
			}
			if (area.isChanged()) {
				col = Color.BLUE;
			}
			if (area.debugColor != null) {
				col = area.debugColor;
			}
			paintRectangle(newImage, area.getRectangle(), col);
		}
		
		return newImage;
	}
		
	public static void paintColumn(BufferedImage image, Column column) {
		
		Color color;
		if (column.getDebugColor() != null) {
			color = column.getDebugColor();
		} else if (column.isChanged()) {
			color = Color.BLUE;
		} else if (column.isFurigana()) {
			color = Color.LIGHT_GRAY;
		} else if (column.isVertical()){
			color = Color.ORANGE;
		} else {
			color = Color.CYAN;
		}
		
		paintRectangle(image, column.getRectangle(), color);
		
		for (Column furigana : column.getFuriganaColumns()) {
			paintColumn(image, furigana);
		}
	}
	
	public static void paintColumns(BufferedImage image, List<Column> columns) {
		
		for (Column column : columns) {
			paintColumn(image, column);
		}
	}
	
	/**
	 * Paints a rectangle to image.
	 */
	public static void paintRectangle(BufferedImage image, Rectangle rect, Color color) {
	
		// TODO AlphaComposite with background? see InvertImage for example
		
		// top
		for (int x = rect.x ; x<=rect.x+rect.width-1 ; x++) {
			int y = rect.y;
			paintPixel(image, color, x, y);
		}
		
		// bottom
		for (int x = rect.x ; x<=rect.x+rect.width-1 ; x++) {
			int y = rect.y+rect.height-1;
			paintPixel(image, color, x, y);
		}
		
		// left
		for (int y = rect.y + 1 ; y<=rect.y+rect.height-1 ; y++) {
			int x = rect.x;
			paintPixel(image, color, x, y);
		}
		
		// right
		for (int y = rect.y + 1 ; y<=rect.y+rect.height-1 ; y++) {
			int x = rect.x+rect.width-1;
			paintPixel(image, color, x, y);
		}
	}
	
	/**
	 * Counts the number of pixels in rectangle
	 * 
	 * @param inside If true, checks also pixels inside the rectangle. If false, only
	 * checks the border pixels
	 */
	public static int countPixels(Rectangle rect, BufferedImage image, boolean inside) {
		
		int pixels = 0;
		for (int y=rect.y ; y<=rect.y+rect.height-1 ; y++) {
			for (int x=rect.x ; x<=rect.x+rect.width-1 ; x++) {
				if (inside || y==rect.y || y==rect.y+rect.height-1 || x==rect.x || x==rect.x+rect.width-1) {
					if (x<0 || x>=image.getWidth() || y<0 || y>=image.getHeight()) {
						continue;
					}
					if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
						++pixels;
					}
				}
			}
		}
		return pixels;
	}
	
	/**
	 * Creates a rectangle that spans argument points. Order doesn't matter.
	 */
	public static Rectangle createRectangle(Point p1, Point p2) {
		
		int minX = Math.min(p1.x, p2.x);
		int maxX = Math.max(p1.x, p2.x);
		int minY = Math.min(p1.y, p2.y);
		int maxY = Math.max(p1.y, p2.y);
		int width = maxX - minX + 1;
		int height = maxY - minY + 1;
		
		return new Rectangle(minX, minY, width, height);
	}
	
	/**
	 * Debug image pixels ordered by painting priority. Pixels on 
	 * the end of the list replace earlier colors.
	 */
	private static Color[] debugColorPriority = new Color[] {
			Color.WHITE,
			Color.LIGHT_GRAY,
			Color.GRAY,
			Color.ORANGE,
			Color.CYAN,
			Color.GREEN,
			Color.RED,
			Color.BLUE,
			Color.BLACK
		};
	
	private static final Map<Color, Integer> debugColorPriorityMap = new HashMap<Color, Integer>();
	static {
		int priority = 0;
		for (Color col : debugColorPriority) {
			debugColorPriorityMap.put(col, priority++);
		}
	}
	
	/**
	 * Paints pixel at x,y coordinates. If pixel is already painted, it is replaced
	 * if new color has higher priority.
	 */
	private static void paintPixel(BufferedImage image, Color color, int x, int y) {

		// this version keeps single color according to priority
		
		Integer priority = debugColorPriorityMap.get(color);
		if (priority == null) {
			throw new Error("Unknown color:"+color);
		}
		
		if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
			return;
		}
		
		Color oldColor = new Color(image.getRGB(x, y));
		Integer oldPriority = debugColorPriorityMap.get(oldColor);
		if (oldPriority == null) {
			oldPriority = -1;
		}
		
		if (priority > oldPriority) {
			image.setRGB(x, y, color.getRGB());
		}
	}
	
	private static Scalr.Method quality = Scalr.Method.BALANCED;
	
	/**
	 * Stretches or compresses image to target width and height.
	 * Doesn't keep proportions.
	 */
	public static BufferedImage stretch(BufferedImage image, int width, int height) {

		BufferedImage scaledImage = Scalr.resize(image, quality, Scalr.Mode.FIT_EXACT, width, height);
		image.flush();
		return scaledImage;
	}
	
	/**
	 * Stretches or compresses image to target size. Checks image ratio, limits stretch amount.
	 * This is used to prevent 一｜ー characters from filling the whole block. Extra space in final
	 * image is filled with white and target image is positioned in the center.
	 */
	public static BufferedImage stretchCheckRatio(BufferedImage image, int targetSize, int finalSize) {
		
		// calculate image minor/major dimension ratio
		float ratio = 1.0f * image.getWidth() / image.getHeight();
		if (ratio > 1.0f) {
			ratio = 1/ratio;
		}
		
		// calculate target size, targetSize*targetSize unless ratio is below 0.4f
		int targetHeight = targetSize;
		int targetWidth = targetSize;
		int targetMinDim = Math.round(Util.scale(ratio, 0.1f, 0.4f, 8, targetSize));
		if (image.getWidth() > image.getHeight()) {
			targetHeight = targetMinDim;
		} else {
			targetWidth = targetMinDim;
		}
		
		// stretch image to target size
		BufferedImage stretchedImage = stretch(image, targetWidth, targetHeight);
				
		// create final image and move stretched image to center
		return createSquareImage(stretchedImage, finalSize);
	}
	
	/**
	 * Creates square image and positions sourceImage to center. Pixels may be cut
	 * if sourceImage is larget than final image
	 */
	public static BufferedImage createSquareImage(BufferedImage sourceImage, int size) {
		
		int sourceWidth = sourceImage.getWidth();
		int sourceHeight = sourceImage.getHeight();
		
		BufferedImage blockImage = createWhiteImage(size, size);
		
		int deltaX = (size - sourceWidth)/2;
		int deltaY = (size - sourceHeight)/2;
		for (int y=0 ; y<sourceHeight ; y++) {
			int targetY = y + deltaY;
			if (targetY < 0 || targetY >= size) {
				continue;
			}
			for (int x=0 ; x<sourceWidth ; x++) {
				int targetX = x + deltaX;
				if (targetX < 0 || targetX >= size) {
					continue;
				}
				int rgb = sourceImage.getRGB(x, y);
				blockImage.setRGB(targetX, targetY, rgb);
			}
		}
		
		return blockImage;
	}
	
	/**
	 * Creates a sub-image defined by rectangle.
	 */
	public static BufferedImage crop(BufferedImage image, Rectangle rect) {
		
		return image.getSubimage(rect.x, rect.y, rect.width, rect.height);
	}
	
	/**
	 * Creates a sub-image defined by rectangle.
	 */
	public static BufferedImage crop(boolean[][] image, Rectangle rect) {
		
		BufferedImage bImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
		
		for (int x=rect.x ; x<rect.x + rect.width ; x++) {
			for (int y=rect.y ; y<rect.y + rect.height ; y++) {
				if (!image[x][y]) {
					bImage.setRGB(x - rect.x, y - rect.y, Color.WHITE.getRGB());
				}
			}
		}
		
		return bImage;
	}
	
	/**
	 * Creates image that has only black and white pixels, determined by
	 * blackThreshold
	 * @param blackThreshold If null, uses fixedBlackLevel values instead of single threshold
	 */
	public static BufferedImage makeBlackAndWhite(BufferedImage image, Integer blackThreshold) {
		
		BufferedImage bwImage = ImageUtil.createEmptyCopy(image);
		
		for (int y=0 ; y<image.getHeight() ; y++) {
			for (int x=0 ; x<image.getWidth() ; x++) {
				int rgb = image.getRGB(x, y);
				boolean pixel = ImageUtil.containsPixel(rgb, blackThreshold);
				if (pixel) {
					bwImage.setRGB(x, y, Color.BLACK.getRGB());
				} else {
					bwImage.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		
		return bwImage;
	}
	
	/**
	 * Builds black and white image from 32x32 bit matrix
	 */
	public static BufferedImage buildImage(int[] matrix) {
		
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		
		for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				if (MatrixUtil.isBitSet(x, y, matrix)) {
					image.setRGB(x, y, Color.BLACK.getRGB());
				} else {
					image.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		
		return image;
	}
	
	/**
	 * Builds 32x32 bit matrix from image. Centers and crops image if necessary.
	 */
	public static int[] buildMatrix(BufferedImage image) {
		
		int height = image.getHeight();
		int width = image.getWidth();
		int[] matrix = new int[height];
		
		for (int y=0 ; y<height ; y++) {
			for (int x=0 ; x<width ; x++) {
				if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
					matrix[y] |= 1;
				}
				if (x < width-1)
					matrix[y] <<= 1;
			}
		}
		
		return matrix;
	}
		
	/**
	 * Builds 32x32 bit matrix from 32x32 image.
	 */
	public static int[] buildMatrix32(BufferedImage image) {
		
		int[] matrix = new int[32];
		
		for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
					matrix[y] |= 1;
				}
				if (x < 31)
					matrix[y] <<= 1;
			}
		}
		
		return matrix;
	}
	
	// TODO "create" or "build" not consistent
	
	/**
	 * Builds 32x32 bit matrix from 32x32 boolean matrix.
	 */
	public static int[] buildMatrix32(boolean[][] image) {
		
		int[] matrix = new int[32];
		
		for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				if (image[x][y]) {
					matrix[y] |= 1;
				}
				if (x < 31)
					matrix[y] <<= 1;
			}
		}
		
		return matrix;
	}
	
	/**
	 * Checks that the image contains only black and white pixels
	 */
	public static void checkImageBW(BufferedImage image) {
		
		for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				int rgb = image.getRGB(x, y);
				if (rgb != Color.BLACK.getRGB() && rgb != Color.WHITE.getRGB()) {
					throw new Error("Image must be black and white");
				}
			}
		}
	}
	
	/**
	 * Prints boolean matrix to stdout
	 */
	public static void debugPrintMatrix(boolean[][] matrix) {
		
		for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				if (matrix[x][y]) {
					System.out.print("x");
				} else {
					System.out.print(".");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * Sharpens the image
	 */
	public static BufferedImage sharpenImage(BufferedImage image, Parameters par) {
		
		if (Parameters.fixedBlackLevelEnabled) {
			return ImageUtil.createCopy(image);
		}
		
		BufferedImage sharpened = null;
		UnsharpMaskFilter filter = new UnsharpMaskFilter(
				par.unsharpAmount, par.unsharpRadius, par.unsharpThreshold);
		return filter.filter(image, sharpened);
	}
	
	/**
	 * Inverts black and white imagea
	 * @return
	 */
	public static BufferedImage invertImage(BufferedImage image) {
		
		BufferedImage inverted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		for (int y=0 ; y<image.getHeight() ; y++) {
			for (int x=0 ; x<image.getWidth() ; x++) {
				if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
					inverted.setRGB(x, y, Color.WHITE.getRGB());
				} else {
					inverted.setRGB(x, y, Color.BLACK.getRGB());
				}
			}
		}	
		return inverted;
	}
	
	/**
	 * Splits 32x32 rectangle into smaller rectangles.
	 *  
	 * @param divisor Per dimension (3 -> 3x3)
	 */
	public static List<Rectangle> split32Cube(int divisor) {
		
		List<Rectangle> parts = new ArrayList<>();
		int size = 32/divisor;
		int remainder = 32 - size*divisor;
		for (int x=0 ; x<divisor ; x++) {
			for (int y=0 ; y<divisor ; y++) {
				int width = size;
				int height = size;
				if (x == divisor-1) {
					width += remainder;
				}
				if (y == divisor-1) {
					height += remainder;
				}
				Rectangle rect = new Rectangle(x*size, y*size, width, height);
				parts.add(rect);
			}
		}
		return parts;
	}
	
	/**
	 * Writes image to clipboard
	 */
	public static void setClipboard(Image image) {
		
		// https://alvinalexander.com/java/java-copy-image-to-clipboard-example
		ImageSelection imgSel = new ImageSelection(image);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
	}
	
	public static void main(String[] args) {
		
		int[] matrix = new int[32];
		MatrixUtil.setBit(0,0,matrix);
		MatrixUtil.setBit(30,30,matrix);
		MatrixUtil.setBit(31,30,matrix);
		MatrixUtil.setBit(31,31,matrix);
		MatrixUtil.setBit(25,25,matrix);
		MatrixUtil.setBit(25,26,matrix);
		MatrixUtil.setBit(26,25,matrix);
		MatrixUtil.setBit(26,26,matrix);
		System.out.println("before:");
		MatrixUtil.debugPrintMatrix(matrix);
		int[] source = new int[32];
		MatrixUtil.setBit(29, 28, source);
		MatrixUtil.setBit(29, 29, source);
		System.out.println("source:");
		MatrixUtil.debugPrintMatrix(source);
		MatrixUtil.addBits(source, matrix);
		System.out.println("after:");
		MatrixUtil.debugPrintMatrix(matrix);
	}
}

//This class is used to hold an image while on the clipboard.
//https://alvinalexander.com/java/java-copy-image-to-clipboard-example
class ImageSelection implements Transferable
{
	private Image image;
	
	public ImageSelection(Image image)
	{
	 this.image = image;
	}
	
	// Returns supported flavors
	public DataFlavor[] getTransferDataFlavors()
	{
	 return new DataFlavor[] { DataFlavor.imageFlavor };
	}
	
	// Returns true if flavor is supported
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
	 return DataFlavor.imageFlavor.equals(flavor);
	}
	
	// Returns image
	public Object getTransferData(DataFlavor flavor)
	   throws UnsupportedFlavorException, IOException
	{
	 if (!DataFlavor.imageFlavor.equals(flavor))
	 {
	   throw new UnsupportedFlavorException(flavor);
	 }
	 return image;
	}
}