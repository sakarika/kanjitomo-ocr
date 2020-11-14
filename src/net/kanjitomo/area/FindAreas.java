package net.kanjitomo.area;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Util;

/**
 * Finds areas by scanning the image for groups of touching pixels 
 */
public class FindAreas extends AreaStep {
		
	public FindAreas(AreaTask task) {
		super(task, "touching", "background", "areas");
	}

	private int width;
	private int height;
	private boolean[][] image;
	private boolean[][] visited;
	private boolean[][] background;
	private int areaMinX = 0;
	private int areaMinY = 0;
	private int areaMaxX = 0;
	private int areaMaxY = 0;
	
	private BufferedImage debugImage;
	private Graphics2D debugGraphics = null;
	
	private boolean debug = false;
	
	/** 
	 * Areas that contains less than this number of pixels are rejected.
	 */
	private static final int MIN_PIXELS_AREA = 3; // TODO relative to resolution
	
	@Override
	protected void runImpl() throws Exception {

		findAreas();
		removeDitherAreas1();
		removeDitherAreas2();
		removeTinyAreas();
	}
	
	/**
	 * Finds all connected areas.
	 */
	private void findAreas() {
		
		width = task.width;
		height = task.height;
		image = task.binaryImage;
		visited = new boolean[width][height];
		background = new boolean[width][height];
		task.areas = new ArrayList<Area>();
		
		if (addDebugImages) {
			debugImage = ImageUtil.createWhiteImage(width, height);
			debugGraphics = debugImage.createGraphics();
		}
		
		for (int y=0 ; y<height ; y++) {
			for (int x=0 ; x<width ; x++) {
				if (image[x][y]) {
					// mark all black pixels as belonging to background by default,
					// pixels that belong to areas are removed from background in findArea
					background[x][y] = true;
				} else {
					// mark all white pixels as done
					visited[x][y] = true;
				}
			}
		}
		
		// find touching areas containing black pixels
		for (int y=0 ; y<height ; y++) {
			for (int x=0 ; x<width ; x++) {
				if (!visited[x][y]) {
					findArea(x, y);
				}
			}
		}
		
		task.backgroundImage = background;
	}
	
	/**
	 * Finds a new area starting from initX, initY
	 */
	private void findArea(int initX, int initY) {
		
		if (addDebugImages) {
			debugGraphics.setPaint(getRandomColor());
		}
		
		areaMinX = initX;
		areaMinY = initY;
		areaMaxX = initX;
		areaMaxY = initY;
		
		boolean touchesBorders = false;
		
		/** Pixels to be checked */
		Queue<Pixel> todo = new LinkedBlockingQueue<Pixel>();
		
		/** Confirmed pixels in single continous area */
		List<Pixel> pixels = new ArrayList<Pixel>();
		
		todo.add(new Pixel(initX, initY));
		
		while (!todo.isEmpty()) {
			
			Pixel p = todo.remove();
			if (visited[p.x][p.y]) continue;
			visited[p.x][p.y] = true;
			
			if (task.getPixel(p.x, p.y)) {

				if (addDebugImages) {
					debugGraphics.fillRect(p.x, p.y, 1, 1);
				}
				
				if (p.x <= 0 || p.x >= width-1 || p.y <= 0 || p.y >= height-1 ||
					task.getBorderPixel(p.x, p.y)) {
					touchesBorders = true;
				}
				
				pixels.add(p);
				
				if (p.x < areaMinX) areaMinX = p.x;
				if (p.y < areaMinY) areaMinY = p.y;
				if (p.x > areaMaxX) areaMaxX = p.x;
				if (p.y > areaMaxY) areaMaxY = p.y;
				
				for (int newY = p.y-1 ; newY <= p.y+1 ; newY++) {
					for (int newX = p.x-1 ; newX <= p.x+1 ; newX++) {
						
						if (newX < 0 || newX >= width || newY < 0 || newY >= height) {
							continue;
						}
								
						if (visited[newX][newY]) {
							continue;
						}
						
						todo.add(new Pixel(newX, newY));
					}
				}
			}
		}
		
		if (touchesBorders) {
			return;
		}
		
		int width = areaMaxX-areaMinX+1;
		int height = areaMaxY-areaMinY+1;
		
		Rectangle rect = new Rectangle(areaMinX, areaMinY, width, height);
		Area area = new Area(rect, pixels.size());
		area.sourceAreas.add(area);
		
		// TODO relative to resolution
		if (height > 120 || width > 120) {
			return;
		}
		
		// TODO find worst case ratio for valid characters			
		float ratio = 1.0f*pixels.size() / (height * width);
		if (area.getSize() > 300 && ratio < 0.09f) {
			return;
		}
	
		// reject area if it represents a speech bubble
		if (isSpeechBubble(area)) {
			return;
		}
		
		// calculate area's minimum rgb value
		int minRGB = 255;
		for (Pixel p : pixels) {
			int rgb = task.getPixelRGB(p.x, p.y);
			if (rgb < minRGB) {
				minRGB = rgb;
			}
		}
//		if (minRGB > par.pixelRGBThreshold) {
//			return;
//		}
		area.minRGB = minRGB;
				
		// remove area's pixels from background
		for (Pixel p : pixels) {
			background[p.x][p.y] = false;
		}
		
		task.areas.add(area);
		
		if (debug) {
			System.err.println("area:"+area+" rgb:"+area.minRGB);
		}
	}
	
	private class Pixel {

		int x;
		int y;
		
		public Pixel(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * Checks if area represents a speech bubble
	 */
	private boolean isSpeechBubble(Area area) {
		
		// speech bubbles should be filtered because they might confuse column detector
		// this check is only needed for small bubbles that enclose a few characters,
		// large bubbles are rejected because their pixel/area ratio is too low 
		
		if (area.getWidth() < 80 || area.getHeight() < 80) {
			// areas smaller than these are likely to represent single characters
			return false;
		}
		
		// ellipse parameters
		float size = 0.88f;
		float a2 = (float) Math.pow(1.0f*area.getWidth()/2*size, 2);
		float b2 = (float) Math.pow(1.0f*area.getHeight()/2*size, 2);
		int centerX = area.getMidpoint().x;
		int centerY = area.getMidpoint().y;
		
		// count pixels that are outside the ellipse
		int pixels = 0;
		for (int y=area.getY(); y<=area.getMaxY() ; y++) {
			for (int x=area.getX(); x<=area.getMaxX() ; x++) {
				
				float value = (float) (
						1.0f*Math.pow(x - centerX, 2)/a2 + 
						1.0f*Math.pow(y - centerY, 2)/b2);
				if (value <= 1) {
					// inside ellipse
					continue;
				}
				
				if (image[x][y]) {
					++pixels;
				}
			}
		}

		// reject area if most of the pixels are located outside the ellipse
		float ratio = 1.0f*pixels/area.getPixels();
		if (ratio > 0.92f) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Removes areas that have less than MIN_PIXELS_AREA pixels
	 */
	private void removeTinyAreas() {

		// these are not included in the background so that they
		// don't confuse column detection
		
		Iterator<Area> i = task.areas.iterator();
		while (i.hasNext()) {
			Area area = i.next();
			if (area.pixels < MIN_PIXELS_AREA) {
				i.remove();
				// actual pixels should not be removed since they often contains 
				// radical fragments in small resolution images
			}
		}
	}
	
	/**
	 * Removes areas that contain many unconnected individual pixels, 
	 * most likely representing dither patterns in the background
	 */
	private void removeDitherAreas1() {
		
		for (Area area : task.areas) {

			if (area.getPixels() <= 15) {
				continue;
			}
			
			int[] neighbourCounts = new int[5];
			for (int x=area.getX() ; x<=area.getMaxX() ; x++) {
				for (int y=area.getY() ; y<=area.getMaxY() ; y++) {
					if (!task.getPixel(x, y)) continue;			
					int neighbours = 0;
					if (task.getPixel(x-1, y)) ++neighbours;
					if (task.getPixel(x+1, y)) ++neighbours;
					if (task.getPixel(x, y-1)) ++neighbours;
					if (task.getPixel(x, y+1)) ++neighbours;			
					neighbourCounts[neighbours]++;
				}
			}
			
			if (neighbourCounts[0] > area.getPixels()/2) {
				area.remove = true;
				continue;
			}
			
			float score = (neighbourCounts[0]*2f + neighbourCounts[1]) / area.getPixels();
			float rgbQuality = 1.0f * area.getMinRGB() / par.pixelRGBThreshold;
			float rgbFactor = Util.scale(rgbQuality, 0.5f, 0.7f, 1.0f, 0.6f);
//			float pixelsFactor = Util.scale(area.pixels, 6, 15, 1.25f, 0.8f);
			float threshold = 1.3f * rgbFactor;
			
//			if (area.getX() == 28 && area.getY() == 14) {
//				System.err.println("area      :"+area);
//				System.err.println("0         :"+neighbourCounts[0]);
//				System.err.println("1         :"+neighbourCounts[1]);
//				System.err.println("score     :"+score);
//				System.err.println("minRGB    :"+area.getMinRGB());
//				System.err.println("rgbQuality:"+rgbQuality+"\t("+rgbFactor+")");
//				System.err.println("pixels    :"+area.pixels+"\t("+pixelsFactor+")");
//				System.err.println("threshold :"+threshold);
//			}
			
			if (score >= threshold) {
				//markedAreas.add(area);
				area.remove = true;
				continue;
			}
		}
		
		Iterator<Area> i = task.areas.iterator();
		while (i.hasNext()) {
			Area area = i.next();
			if (area.remove) {
				i.remove();
			}
		}
	}
	
	/**
	 * Removes regions that contain many unconnected small areas in close proximity, 
	 * most likely representing dither patterns in the background
	 */
	private void removeDitherAreas2() {
		
		// this version finds small dense areas close together
		
		RTree<Area> index = new RTree<>(image, task.areas);
		
		// TODO scale with resolution
		int probeSize = 80; 
		int probeOverlap = 8;
		
		for (int x = 0 ; x < width ; x += probeSize - probeOverlap) {
			for (int y = 0 ; y < height ; y += probeSize - probeOverlap) {
				
				Rectangle probe = new Rectangle(x, y, probeSize, probeSize);
				List<Area> smallAreas = new ArrayList<Area>();
				for (Area area : index.get(probe)) {
					// TODO scale with resolution
					if (area.pixels <= 6 && area.getRatio() > 0.6f) {
						smallAreas.add(area);
					}
				}

				if (smallAreas.size() >= 80) {
					for (Area area : smallAreas) {
						area.remove = true;
					}
				}
			}
		}
		
		Iterator<Area> i = task.areas.iterator();
		while (i.hasNext()) {
			Area area = i.next();
			if (area.remove) {
				i.remove();
			}
		}
	}
	
	private Random rand = new Random();
	
	private Color getRandomColor() {
		
		int red = rand.nextInt(240);
		int green = rand.nextInt(240);
		int blue = rand.nextInt(240);
		return new Color(red,green,blue);
	}

	@Override
	protected void addDebugImages() throws Exception {

		task.addDebugImage(debugImage, "touching");
		task.addDebugImage(task.backgroundImage, "background");
		task.addDefaultDebugImage("areas");
	}
}
