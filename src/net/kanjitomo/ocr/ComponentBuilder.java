package net.kanjitomo.ocr;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.kanjitomo.util.MatrixUtil;
import net.kanjitomo.util.Parameters;

/**
 * Builds reference components from ReferenceMatrix.
 */
public class ComponentBuilder {

	private static Parameters par = Parameters.getInstance();
	private ComponentFindUnconnected unconnected = new ComponentFindUnconnected();
	private ComponentSplit split = new ComponentSplit();
	
	/**
	 * Extracts components from reference matrix
	 */
	public ArrayList<Component> buildComponents(ReferenceMatrix reference) {
		
		Component base = new Component();
		base.bounds = new Rectangle(0, 0, 32, 32);
		base.matrix = reference.matrix;
		
		ArrayList<Component> components = findSubComponents(base);
		compressComponents(components, reference.matrix);
		
		return components;
	}
	
	private ArrayList<Component> findSubComponents(Component parent) {
		
		// find unconnected components
		List<Component> components = unconnected.run(parent); 
		
		// split large components into smaller sub-components
		ArrayList<Component> componentsSplitted = new ArrayList<>();
		for (Component component : components) {
			componentsSplitted.addAll(split.run(component));
		}
		
		return componentsSplitted; 
	}
	
	/**
	 * Compresses component matrices into fewer matrices. This is done by assigning
	 * multiple components into singe matrix if their bounding boxes don't overlap.
	 * 
	 * @param baseLayer Original reference matrix that contains the whole character.
	 */
	private void compressComponents(List<Component> components, int[] baseLayer) {
		
		List<int[]> layers = new ArrayList<>();
		
		// add components to layers
		components: for (Component component : components) {
			
			// check if base layer can be used
			if (addToBaseLayer(component, baseLayer)) {
				continue;
			}
			
			// check if any existing layer can be used
			for (int i=0 ; i<layers.size() ; i++) {
				if (addToLayer(component, layers.get(i))) {
					continue components;
				}
			}
			
			// create a new layer
			int[] newLayer = new int[32];
			layers.add(newLayer);
			addToLayer(component, newLayer);
		}
	}
	
	/**
	 * Tries to add component into base layer. Base layer contains pixels from all
	 * components (original reference matrix). Only use it for components that don't
	 * contain any other pixels within their bounding boxes.
	 */
	private boolean addToBaseLayer(Component component, int[] baseLayer) {
		
		// check if there are any pixels that belong to other components
		if (MatrixUtil.countBits(baseLayer, component.bounds) > component.pixels) {
			return false;
		}
		
		// use the base layer
		component.matrix = baseLayer;
		return true;
	} 
	
	/**
	 * Tries to add component into argument layer. Checks that the target 
	 * layer has free space (bounding box doesn't contain pixels from other components)
	 * 
	 * @return true if component was added, false if not
	 */
	private boolean addToLayer(Component component, int[] layer) {
		
		// check if the layer contains pixels from other components within bounds
		if (MatrixUtil.countBits(layer, component.bounds) > 0) {
			return false;
		}
		
		// add to this layer
		MatrixUtil.addBits(component.matrix, layer, component.bounds);
		component.matrix = layer;
		return true;
	}
	
	public static void main(String[] args) {
		try {
			
			char testCharacter = 'り'; // り人化新港無
			
			// select reference matrix
			ReferenceMatrixCacheLoader loader = new ReferenceMatrixCacheLoader();
			loader.load();
			ReferenceMatrixCache cache = loader.getCache();
			ReferenceMatrix reference = null;
			for (ReferenceMatrix matrix : cache.get("MS Gothic")) {
				if (matrix.character == testCharacter) {
					reference = matrix;
				}
			}
			System.out.println("Reference");
			MatrixUtil.debugPrintMatrix(reference.matrix);
			
			// build components
			ComponentBuilder builder = new ComponentBuilder();
			List<Component> components = builder.buildComponents(reference);
			
			// print components
			System.out.println(components.size()+" components");
			for (Component component : components) {
				System.out.println(component.bounds+" pixels:"+component.pixels);
				int[] pixels = new int[32];
				MatrixUtil.addBits(component.matrix, pixels, component.bounds);
				MatrixUtil.debugPrintMatrix(pixels, component.matrix);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
