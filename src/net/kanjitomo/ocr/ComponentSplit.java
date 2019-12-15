package net.kanjitomo.ocr;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.kanjitomo.util.MatrixUtil;

/**
 * Splits large components into subcomponents along x and y axis.
 */
public class ComponentSplit {

	/**
	 * Component axis must be larger or equal to be splitted
	 */
	private static int MIN_SPLIT_SIZE = 20;
	private static boolean SPLIT = false; // experimental

	/**
	 * Splits the component
	 */
	public List<Component> run(Component component) {
		
		if (!SPLIT) {
			List<Component> noSplit = new ArrayList<>();
			noSplit.add(component);
			return noSplit;
		}
		
		// find split points in each axis. if null, don't split
		Integer splitX = findSplitPointX(component);
		Integer splitY = findSplitPointY(component);
		
		// split in each direction
		List<Component> splittedX = splitX(component, splitX);
		List<Component> splittedY = new ArrayList<>();
		for (Component comp : splittedX) {
			splittedY.addAll(splitY(comp, splitY));
		}
		return splittedY;
	}
	
	/**
	 * Finds split point in x-axis. Null if component is too small.
	 */
	private Integer findSplitPointX(Component component) {
	
		int width = component.bounds.width; 
		if (width < MIN_SPLIT_SIZE) {
			return null;
		}
		return component.bounds.x + width/2;
	}

	/**
	 * Finds split point in y-axis. Null if component is too small.
	 */
	private Integer findSplitPointY(Component component) {
	
		int height = component.bounds.height; 
		if (height < MIN_SPLIT_SIZE) {
			return null;
		}
		return component.bounds.y + height/2;
	}
	
	/**
	 * Splits the component along x axis
	 * 
	 * @param splitX Split location. Returns the component without splitting if null.
	 */
	private List<Component> splitX(Component component, Integer splitX) {
		
		Rectangle bounds = component.bounds;
		List<Component> splitted = new ArrayList<>();
		
		if (splitX == null) {
			splitted.add(component);
			return splitted;
		}
		
		Component left = new Component();
		left.bounds = new Rectangle(bounds.x, bounds.y, splitX - bounds.x + 1, bounds.height);
		left.matrix = new int[32];
		MatrixUtil.addBits(component.matrix, left.matrix, left.bounds);
		left.pixels = MatrixUtil.countBits(left.matrix);
		
		Component right = new Component();
		right.bounds = new Rectangle(splitX + 1, bounds.y, bounds.width - left.bounds.width, bounds.height);
		right.matrix = new int[32];
		MatrixUtil.addBits(component.matrix, right.matrix, right.bounds);
		right.pixels = MatrixUtil.countBits(right.matrix);
		
		if (left.pixels > 0) {
			left.bounds = MatrixUtil.findBounds(left.matrix);
			splitted.add(left);
		}
		if (right.pixels > 0) {
			right.bounds = MatrixUtil.findBounds(right.matrix);
			splitted.add(right);
		}
		
		return splitted;
	}
	
	/**
	 * Splits the component along y axis
	 * 
	 * @param splitY Split location. Returns the component without splitting if null.
	 */
	private List<Component> splitY(Component component, Integer splitY) {
		
		Rectangle bounds = component.bounds;
		List<Component> splitted = new ArrayList<>();
		
		if (splitY == null) {
			splitted.add(component);
			return splitted;
		}		
		
		Component up = new Component();
		up.bounds = new Rectangle(bounds.x, bounds.y, bounds.width, splitY - bounds.y + 1);
		up.matrix = new int[32];
		MatrixUtil.addBits(component.matrix, up.matrix, up.bounds);
		up.pixels = MatrixUtil.countBits(up.matrix);
		
		Component down = new Component();
		down.bounds = new Rectangle(bounds.x, splitY + 1, bounds.width, bounds.height - up.bounds.height);
		down.matrix = new int[32];
		MatrixUtil.addBits(component.matrix, down.matrix, down.bounds);
		down.pixels = MatrixUtil.countBits(down.matrix);
		
		if (up.pixels > 0) {
			up.bounds = MatrixUtil.findBounds(up.matrix);
			splitted.add(up);
		}
		if (down.pixels > 0) {
			down.bounds = MatrixUtil.findBounds(down.matrix);
			splitted.add(down);
		}
		
		return splitted;
	}
}
