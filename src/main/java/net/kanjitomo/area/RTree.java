package net.kanjitomo.area;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * R-tree index used for finding intersecting rectangles (columns or areas)
 */
public class RTree<T extends HasRectangle> {

	/**
	 * Maximum number of rectangles in single node. If this limit is reached,
	 * node is split into child nodes.
	 */
	private static final int MAX_VALUES_PER_NODE = 16;
		
	/**
	 * If true, this object is a leaf node (contains rectangles).
	 * If false, this object contains references to child nodes.
	 */
	private boolean isLeaf = true;
	
	/**
	 * If true, this node won't be splitted to child nodes.
	 */
	private boolean noSplit = false;
	
	/**
	 * Contains all rectangles inserted to this node. Will grow as new values 
	 * are inserted but it will not shrink after values are removed
	 */
	private Rectangle coverage;
	
	/**
	 * Contains all possible rectangles that can be inserted to this node.
	 * 
	 * Difference between coverage and bounds is that coverage represents
	 * actual rectangles inserted so far, bounds represents all rectangles
	 * that might be inserted in the future. 
	 */
	private final Rectangle bounds;
	
	/**
	 * Rectangles in this node, only if leaf
	 */
	private List<T> values = new ArrayList<T>();
	
	/**
	 * Childred of this node, only if not leaf
	 */
	private List<RTree<T>> nodes;
	
	/**
	 * Overflow node contains rectangles that cross node boundaries.
	 */
	private RTree<T> overflow;
	
	/**
	 * Creates a new node
	 * 
	 * @param bound Outer limit of the rectangles that can be inserted to this node 
	 */
	public RTree(Rectangle bounds) {
		
		this.bounds = bounds;
	}
	
	/**
	 * Creates a new node that can hold all rectangles inside image.
	 */
	public RTree(BufferedImage image) {
		
		bounds = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(),
				image.getHeight());
	}
	
	/**
	 * Creates a new node that can hold all rectangles inside image
	 */
	public RTree(boolean[][] image) {
		
		int width = image.length;
		int height = image[0].length;
		bounds = new Rectangle(0, 0, width, height);
	}

	/**
	 * Creates a new index that can hold all rectangles inside image.
	 * Populates the index with argument rectangles.
	 */
	public RTree(BufferedImage image, List<T> values) {
		
		this(image);
		for (T value : values) {
			add(value);
		}
	}
	
	/**
	 * Creates a new index that can hold all rectangles inside image.
	 * Populates the index with argument rectangles.
	 */
	public RTree(boolean[][] image, List<T> values) {
		
		this(image);
		for (T value : values) {
			add(value);
		}
	}
	
	/**
	 * Adds rectangle to this node
	 */
	public void add(T value) {
		
		if (isLeaf) {
			values.add(value);
			if (values.size() > MAX_VALUES_PER_NODE && !noSplit) {
				split();
			}
		} else {
			RTree<T> target = null;
			for (RTree<T> node : nodes) {
				if (node.bounds.contains(value.getRectangle())) {
					target = node;
					break;
				}
			}
			if (target == null) {
				target = overflow;
			}
			target.add(value);			
		}
		if (coverage == null) {
			coverage = value.getRectangle();
		} else {
			coverage = coverage.union(value.getRectangle());
		}
	}
	
	/**
	 * Adds list of rectangles to this node
	 */
	public void addAll(List<T> values) {
		
		for (T value : values) {
			add(value);
		}
	}
	
	/**
	 * Removes rectangle from this node. Index is not rebalanced after remove.
	 * 
	 * @return true if rectangle was found and removed
	 */
	public boolean remove(T value) {
		
		if (isLeaf) {
			return values.remove(value);
		} else {
			RTree<T> target = null;
			for (RTree<T> node : nodes) {
				if (node.coverage != null && node.coverage.contains(value.getRectangle())) {
					target = node;
					break;
				}
			}
			if (target == null) {
				target = overflow;
			}
			return target.remove(value);
		}
	}
	
	/**
	 * Returns rectangles that intersect with the argument rectangle.
	 * 
	 * @param rect Rectangle used for intersect search
	 */
	public List<T> get(Rectangle rect) {
		
		List<T> results = new ArrayList<T>();
		if (isLeaf) {
			for (T rectangle : values) {
				if (rectangle.getRectangle().intersects(rect)) {
					results.add(rectangle);
				}
			}
		} else {
			for (RTree<T> node : nodes) {
				if (node.coverage != null && node.coverage.intersects(rect)) {
					results.addAll(node.get(rect));
				}
			}
			if (overflow.coverage != null && overflow.coverage.intersects(rect)) {
				results.addAll(overflow.get(rect));
			}
		}
		return results;
	}
	
	/**
	 * Returns rectangles that intersect with the argument rectangle.
	 * 
	 * @param rect Rectangle used for intersect search
	 * @param skip This rectangle is not included in the return list. 
	 */
	public List<T> get(Rectangle rect, T skip) {
		
		List<T> results = get(rect);
		
		Iterator<T> i = results.iterator();
		while (i.hasNext()) {
			if (i.next().equals(skip)) {
				i.remove();
			}
		}
		
		return results;
	}
	
	/**
	 * Return true if contains at least one rectangle that intersects with argument 
	 * rectangle.
	 * 
	 * @param rect Rectangle used for intersect search
	 */
	public boolean contains(Rectangle rect) {
		
		return get(rect).size() > 0;
	}
	
	/**
	 * Return true if contains at least one rectangle that intersects with argument 
	 * rectangle.
	 * 
	 * @param rect Rectangle used for intersect search
	 * @param skip This object is not included
	 */
	public boolean contains(Rectangle rect, T skip) {
		
		return get(rect, skip).size() > 0;
	}
		
	/**
	 * Splits this node into four child nodes plus one overflow node
	 */
	private void split() {
		
		// distribute values around average midpoint
		Point midPoint = calcAverageMidpoint();
		
		int leftWidth = midPoint.x - bounds.x;
		int rightWidth = bounds.x + bounds.width - midPoint.x;
		int upHeight = midPoint.y - bounds.y;
		int downHeight = bounds.y + bounds.height - midPoint.y;
		
		nodes = new ArrayList<RTree<T>>(); 
		
		Rectangle rect = new Rectangle(bounds.x, bounds.y, leftWidth, upHeight);
		nodes.add(new RTree<T>(rect));
		
		rect = new Rectangle(midPoint.x, bounds.y, rightWidth, upHeight);
		nodes.add(new RTree<T>(rect));
		
		rect = new Rectangle(bounds.x, midPoint.y, leftWidth, downHeight);
		nodes.add(new RTree<T>(rect));
		
		rect = new Rectangle(midPoint.x, midPoint.y, rightWidth, downHeight);
		nodes.add(new RTree<T>(rect));
		
		overflow = new RTree<T>(bounds);
		
		for (T value : values) {
			RTree<T> target = null;
			for (RTree<T> node : nodes) {
				if (node.bounds.contains(value.getRectangle())) {
					target = node;
					break;
				}
			}
			if (target == null) {
				target = overflow;
			}
			if (target.values.size() == MAX_VALUES_PER_NODE) {
				noSplit();
				return;
			} else {
				target.add(value);
			}
		}
		
		isLeaf = false;
		values = null;
	}
	
	private void noSplit() {
		noSplit = true;
		nodes = null;
		overflow = null;
	}
	
	/**
	 * Calculates average midpoint of all rectangles
	 */
	private Point calcAverageMidpoint() {
		
		int x = 0;
		int y = 0;
		for (HasRectangle value: values) {
			Point midPoint = value.getMidpoint();
			x += midPoint.x;
			y += midPoint.y;
		}
		x /= values.size();
		y /= values.size();
		return new Point(x,y); 
	}
	
	/**
	 * Returns all values from this node and child nodes. If called from
	 * the root node, returns all values in the index.
	 */
	public List<T> getAll() {
		
		List<T> collected = new ArrayList<T>();
		
		if (isLeaf) {
			collected.addAll(this.values);
		} else {
			for (RTree<T> node : nodes) {
				collected.addAll(node.getAll());
			}
			collected.addAll(overflow.getAll());
			return collected;
		}
		
		return collected;
	}
}
