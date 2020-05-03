package net.kanjitomo.area;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import net.kanjitomo.CharacterColor;
import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Parameters;

/**
 * Inverts target image colors in regions with black background 
 */
public class InvertImage extends AreaStep {
	
	public InvertImage(AreaTask task) {
		super(task, "invert");
	}
	
	@Override
	protected void runImpl() throws Exception {
		
		if (Parameters.fixedBlackLevelEnabled || par.colorTarget == CharacterColor.BLACK_ON_WHITE) {
			// don't invert
		} else if (par.colorTarget == CharacterColor.AUTOMATIC) {
			detectBlackOnWhite();
		} else if (par.colorTarget == CharacterColor.WHITE_ON_BLACK) {
			invertWholeImage();
		}
	}
	
	/**
	 * Image is divided into blocks. BLOCK_SIZE specifies block width and height in pixels. 
	 */
	final static int BLOCK_SIZE = 15; // TODO scale with resolution
	
	/**
	 * Target image width divided by BLOCK_SIZE 
	 */
	private int width;
	
	/**
	 * Target image height divided by BLOCK_SIZE 
	 */
	private int height;
	
	/** 
	 * Blocks that have already been processed. Indexed by image.xy/BLOCK_SIZE.  
	 */ 
	private boolean visited[][];
	
	/** 
	 * Blocks that have been marked for inversion. Indexed by image.xy/BLOCK_SIZE.
	 */
	private boolean invert[][];
	
	/** 
	 * Number of touching neighbour blocks (top,bottom,left/right) that have been inverted.
	 * Indexed by image.xy/BLOCK_SIZE.
	 */
	private int neighboursInverted[][];
	
	/**
	 * Image coordinates divided by BLOCK_SIZE
	 */
	private class Block {

		int x;
		int y;
		
		public Block(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * Detects and inverts regions of the target image with black background
	 */
	private void detectBlackOnWhite() {
		
		// divide image into blocks and count the number of black pixels in each block
		// if pixel count is high enough, invert pixels 
		width = (int) Math.ceil(1.0f*task.width/BLOCK_SIZE);
		height = (int) Math.ceil(1.0f*task.height/BLOCK_SIZE);
		visited = new boolean[width][height];
		invert = new boolean[width][height];
		neighboursInverted = new int[width][height];
		task.borderPixels = new boolean[task.width][task.height];
			
		// mark image blocks that needs to be inverted
		for (int x=0 ; x<width ; x++) {
			for (int y=0 ; y<height ; y++) {
				checkBlock(x, y);
			}
		}
		
		// invert marked blocks
		for (int x=0 ; x<width ; x++) {
			for (int y=0 ; y<height ; y++) {
				if (invert[x][y]) {
					invertBlock(x, y);
				}
			}
		}
		
		task.inverted = invert;
	}
	
	/**
	 * Calculates black pixel ratio in the block
	 */
	private float calcPixelAreaRatio(Block block) {
		
		int blackPixels = 0;
		int allPixels = 0;
		
		for (int px=block.x*BLOCK_SIZE ; px<(block.x+1)*BLOCK_SIZE && px<task.width ; px++) {
			for (int py=block.y*BLOCK_SIZE ; py<(block.y+1)*BLOCK_SIZE && py<task.height ; py++) {
				if (task.getPixel(px, py)) {
					++blackPixels;
				}
				++allPixels;
			}
		}
		
		return 1.0f*blackPixels/allPixels;
	}

	private boolean debugEllipse = false;
	
	private class Pixel extends Block {
		
		public Pixel(int x, int y) {
			super(x, y);
		}}
	
	private List<Pixel> debugEllipsePixels = new ArrayList<Pixel>();
	
	/**
	 * Calcuates black pixel ratio within ellipse
	 */
	private float calcPixelAreaRatioEllipse(int minX, int maxX, int minY, int maxY, float size) {
		
		int width = maxX - minX + 1;
		int height = maxY - minY + 1;
		int pWidth = width*BLOCK_SIZE;
		int pHeight = height*BLOCK_SIZE;
		float a2 = (float) Math.pow(1.0f*pWidth/2*size, 2);
		float b2 = (float) Math.pow(1.0f*pHeight/2*size, 2);
		int pxMin = minX*BLOCK_SIZE;
		int pxMax = (maxX+1)*BLOCK_SIZE - 1;
		int pyMin = minY*BLOCK_SIZE;
		int pyMax = (maxY+1)*BLOCK_SIZE - 1;
		int pxCenter = pxMin + pWidth/2;
		int pyCenter = pyMin + pHeight/2;
		int blackPixels = 0;
		int allPixels = 0;
		
		for (int px=pxMin; px<=pxMax && px<task.width ; px++) {
			for (int py=pyMin ; py<=pyMax && py<task.height ; py++) {
				float value = (float) (
						1.0f*Math.pow(px - pxCenter, 2)/a2 + 
						1.0f*Math.pow(py - pyCenter, 2)/b2);
				if (value > 1) {
					// outside the ellipse
					continue;
				}
				if (debugEllipse && value > 0.95f) {
					debugEllipsePixels.add(new Pixel(px, py));
				}
				if (task.getPixel(px, py)) {
					++blackPixels;
				}
				++allPixels;
			}
		}

		float ratio = 1.0f * blackPixels / allPixels;
		//System.err.println("blackPixels:"+blackPixels+" allPixels:"+allPixels+" ratio:"+ratio);
		return ratio;
	}
	
	/**
	 * Checks if block x,y needs to be inverted. If yes, marks the block and flood-fills
	 * it's neighbours that also need to be inverted.
	 */
	private void checkBlock(int x, int y) {
		
		// blocks marked to be inverted
		List<Block> marked = new ArrayList<Block>();
		
		// blocks where large majority of pixels are black
		int blackBlocks = 0;
		
		// outer bounds of marked area
		int minX = x;
		int maxX = x;
		int minY = y;
		int maxY = y;
		
		// find connected region of blocks with large number of black pixels,
		// mark blocks to be inverted
		Queue<Block> todo = new LinkedBlockingQueue<Block>();
		todo.add(new Block(x, y));		
		while (!todo.isEmpty()) {
			Block block = todo.remove();
			if (visited[block.x][block.y]) {
				continue;
			}
			visited[block.x][block.y] = true;
			float pixelRatio = calcPixelAreaRatio(block);
			float threshold = 0.95f - neighboursInverted[block.x][block.y]*0.25f;
			if (pixelRatio >= threshold) {
				markBlock(block, todo);
				marked.add(block);
			}
			if (pixelRatio >= 0.95f) {
				++blackBlocks;
			}
			if (block.x < minX) minX = block.x;
			if (block.x > maxX) maxX = block.x;
			if (block.y < minY) minY = block.y;
			if (block.y > maxY) maxY = block.y;
		}
		
		// calculate number of marked quad (2x2) blocks
		int quadBlocks = 0;
		for (Block block : marked) {
			try {
				if (invert[block.x+1][block.y] && 
					invert[block.x][block.y+1] &&
					invert[block.x+1][block.y+1]) {
					++quadBlocks;
					// quads might overlap but that's fine, this is not a packing problem
				}
			} catch (Exception e) {}
		}
		
		// rollback inverted blocks if they don't form large enough continous region
		// this is done to prevent inversion of strokes inside large thick characters
		if (quadBlocks < 4 || blackBlocks < 4) {
			for (Block block : marked) {
				invert[block.x][block.y] = false;
			}
			return;
		}
		
		// fill gaps within marked region
		Rectangle region = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1); 
		fillGaps(region);
	}
	
	/**
	 * Marks bloc to be inverted and adds neighbour blocks to todo list
	 */
	private void markBlock(Block block, Queue<Block> todo) {
		
		int x = block.x;
		int y = block.y;
		
		// invert block
		invert[x][y] = true;
		
		// check neighbors
		if (x > 0) {
			neighboursInverted[x-1][y]++;
			todo.add(new Block(x-1, y));
		}
		if (x < width-1) {
			neighboursInverted[x+1][y]++;
			todo.add(new Block(x+1, y));
		}
		if (y > 0) {
			neighboursInverted[x][y-1]++;
			todo.add(new Block(x, y-1));
		}
		if (y < height-1) {
			neighboursInverted[x][y+1]++;
			todo.add(new Block(x, y+1));
		}
	}
	
	/**
	 * Fills gaps within region
	 * 
	 * @param marked List of inverted blocks that belong to region
	 * @param region Region bounds
	 */
	private void fillGaps(Rectangle region) {

		// rectangle that is one block smaller in every direction
		Rectangle internal = new Rectangle(region.x+1, region.y+1,
				region.width-2, region.height-2);

		boolean[][] visited = new boolean[width][height];
		
		// find gaps that are enclosed in inverted blocks
		for (int x=internal.x ; x<internal.x + internal.width ; x++) {
			for (int y=internal.y ; y<internal.y + internal.height ; y++) {
				
				if (invert[x][y] || visited[x][y]) {
					continue;
				}
				
				// outer bounds of the gap
				int minX = x;
				int maxX = x;
				int minY = y;
				int maxY = y;
				
				// flood-fill non-inverted blocks
				List<Block> marked = new ArrayList<Block>();
				boolean touchesBorder = false;
				Queue<Block> todo = new LinkedBlockingQueue<Block>();
				todo.add(new Block(x,y));
				while (!todo.isEmpty()) {
					Block block = todo.remove();
					if (invert[block.x][block.y] || visited[block.x][block.y]) {
						continue;
					}
					if (!internal.contains(block.x, block.y)) {
						touchesBorder = true;
						continue;
					}
					visited[block.x][block.y] = true;
					marked.add(block);
					todo.add(new Block(block.x, block.y-1));
					todo.add(new Block(block.x, block.y+1));
					todo.add(new Block(block.x-1, block.y));
					todo.add(new Block(block.x+1, block.y));
					if (block.x < minX) minX = block.x;
					if (block.x > maxX) maxX = block.x;
					if (block.y < minY) minY = block.y;
					if (block.y > maxY) maxY = block.y;
				}
				
				if (touchesBorder) {
					continue;
				}
				
				// check that the gap is not too large
				int width = maxX - minX + 1;
				int height = maxY - minY + 1;
				int maxGapSize = 8;
				if (width > maxGapSize && height > maxGapSize) {
					continue;
				}
				
				// check if the gap contains an ellipse with low pixel density,
				// then it's most likely a small speech bubble on a black background
				int minCheckEllipse = 3;
				if (width >= minCheckEllipse && height >= minCheckEllipse) {
					float ratio = calcPixelAreaRatioEllipse(minX, maxX, minY, maxY, 0.9f);
					if (ratio < 0.1f) {
						continue;
					}
				}
				
				// TODO check also 90% square?
												
				// invert blocks inside the gap
				for (Block block : marked) {
					invert[block.x][block.y] = true;
				}
			}
		}
	}
	
	/**
	 * Inverts block x,y
	 */
	private void invertBlock(int x, int y) {
		
		// determine if this block is at the edge of inverted region,
		// line is drawn around region's border. this way the border is included
		// in the background and doesn't confuse column detector
		
		boolean top = false;
		if (y > 0) {
			if (!invert[x][y-1]) top = true;
		}
		boolean bottom = false;
		if (y < height-1) {
			if (!invert[x][y+1]) bottom = true;
		}
		boolean left = false;
		if (x > 0) {
			if (!invert[x-1][y]) left = true;
		}
		boolean right = false;
		if (x < width-1) {
			if (!invert[x+1][y]) right = true;
		}
		
		Rectangle rect = new Rectangle(x*BLOCK_SIZE, y*BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
		invertRegion(rect, top, bottom, left, right);
	}
	
	/**
	 * Inverts pixels inside rectangle. Paints argument borders black
	 */
	private void invertRegion(Rectangle rect, boolean top, boolean bottom,
			boolean left, boolean right) {
		
		int minX = rect.x;
		int maxX = rect.x + (int)rect.getWidth() - 1;
		int minY = rect.y;
		int maxY = rect.y + (int)rect.getHeight() - 1;
		
		// invert pixels
		for (int x=minX ; x<=maxX && x<task.width ; x++) {
			for (int y=minY ; y<=maxY && y<task.height ; y++) {
				task.setPixel(x, y, !task.getPixel(x, y));
			}
		}
		
		// draw lines around border
		if (top) drawHorizontalLine(minX, maxX, minY);
		if (bottom) drawHorizontalLine(minX, maxX, maxY);
		if (left) drawVerticalLine(minX, minY, maxY);
		if (right) drawVerticalLine(maxX, minY, maxY);
	}
	
	/**
	 * Draws straight vertical line
	 */
	private void drawVerticalLine(int x, int startY, int endY) {
		
		for (int y=startY ; y<=endY ; y++) {
			task.setPixel(x, y, true);
			task.setBorderPixel(x, y, true);
		}
	}
	
	/**
	 * Draws straight horizontal line
	 */
	private void drawHorizontalLine(int startX, int endX, int y) {
		
		for (int x=startX ; x<=endX ; x++) {
			task.setPixel(x, y, true);
			task.setBorderPixel(x, y, true);
		}
	}
	
	/**
	 * Inverts pixels in the whole image
	 */
	private void invertWholeImage() {
		
		Rectangle rect = new Rectangle(0, 0, task.width, task.height);
		invertRegion(rect, false, false, false, false);
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		
		boolean markInvertedRegions = true;
		
		BufferedImage image = ImageUtil.createCopy(task.binaryImage);
		
		if (markInvertedRegions) {
						
			Graphics2D g = image.createGraphics();
			g.setPaint(Color.BLUE);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
			for (int y=0 ; y<height ; y++) {
				for (int x=0 ; x<width ; x++) {
					if (invert[x][y]) {
						g.fillRect(x*BLOCK_SIZE, y*BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
					}
				}
			}
			
			if (debugEllipse) {
				for (Pixel pixel : debugEllipsePixels) {
					image.setRGB(pixel.x, pixel.y, Color.RED.getRGB());
				}
			}
		}
		
		task.addDebugImage(image, "invert");
	}
}
