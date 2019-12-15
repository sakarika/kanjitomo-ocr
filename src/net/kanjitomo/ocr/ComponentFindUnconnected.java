package net.kanjitomo.ocr;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.kanjitomo.util.MatrixUtil;

/**
 * Finds unconnected components from characters (pixel groups that are not touching) 
 */
public class ComponentFindUnconnected {

	private int[] matrix;
	private Rectangle bounds;
	private boolean[][] visited;
	private Stack<Pixel> todo;
	private List<Pixel> pixels;
	
	/**
	 * Finds unconnected components (groups of pixels that are not touching)
	 */
	public List<Component> run(Component component) {
		
		matrix = component.matrix;
		bounds = component.bounds;
		visited = new boolean[32][32];
		todo = new Stack<Pixel>();
		pixels = new ArrayList<Pixel>();
		
		List<Component> components = new ArrayList<>(); 
		
		for (int x = bounds.x ; x < bounds.x + bounds.width ; x++) {
			for (int y = bounds.y ; y < bounds.y + bounds.height ; y++) {
				todo.add(new Pixel(x,y));
				while (!todo.isEmpty()) {
					checkPixel(todo.pop());
				}
				if (pixels.size() == 0) {
					continue;
				}
				components.add(buildNewComponent());
				pixels.clear();
			}
		}
		
		return components;
	}
	
	/**
	 * Checks if this pixel is set, then add to current list of pixels
	 */
	private void checkPixel(Pixel pixel) {
		
		if (pixel.x < bounds.x || pixel.x >= bounds.x + bounds.width ||
			pixel.y < bounds.y || pixel.y >= bounds.y + bounds.height) {
			return;
		}
				
		if (visited[pixel.x][pixel.y]) {
			return;
		}
		
		if (MatrixUtil.isBitSet(pixel.x, pixel.y, matrix)) {
			pixels.add(pixel);
			todo.add(new Pixel(pixel.x-1, pixel.y));
			todo.add(new Pixel(pixel.x+1, pixel.y));
			todo.add(new Pixel(pixel.x, pixel.y-1));
			todo.add(new Pixel(pixel.x, pixel.y+1));
		}
		
		visited[pixel.x][pixel.y] = true;
	}
	
	/**
	 * Create a new component object from current list of pixels
	 */
	private Component buildNewComponent() {
		
		Component component = new Component();
		
		int minX = 31;
		int minY = 31;
		int maxX = 0;
		int maxY = 0;
		component.matrix = new int[32];
		for (Pixel pixel : pixels) {
			MatrixUtil.setBit(pixel.x, pixel.y, component.matrix);
			if (pixel.x < minX) minX = pixel.x;
			if (pixel.y < minY) minY = pixel.y;
			if (pixel.x > maxX) maxX = pixel.x;
			if (pixel.y > maxY) maxY = pixel.y;
		}
		component.pixels = pixels.size();
		component.bounds = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
		
		return component;
	}
}
