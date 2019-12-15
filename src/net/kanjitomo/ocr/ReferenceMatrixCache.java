package net.kanjitomo.ocr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kanjitomo.util.Parameters;

/**
 * Cache of reference matrices for each character listed in Character class
 */
public class ReferenceMatrixCache {
	
	/** 
	 * Font name -> list of reference matrices for each character 
	 */ 
	private Map<String, List<ReferenceMatrix>> cache;	
	
	public ReferenceMatrixCache() {
		
		cache = new HashMap<>();
		for (String font : Parameters.getInstance().referenceFonts) {
			cache.put(font, new ArrayList<ReferenceMatrix>());
		}
	}
	
	/**
	 * Adds matrix list to the cache
	 */
	public void put(String font, List<ReferenceMatrix> list) {
		
		cache.put(font, list);
	}
	
	/**
	 * Gets reference matrices for given font
	 */
	public List<ReferenceMatrix> get(String font) throws Exception {
		
		if (!cache.containsKey(font)) {
			throw new Exception("Cache doesn't contain font:"+font+", regenerate cache");
		}
		return cache.get(font);
	}
	
	/**
	 * Gets reference matrices for all fonts
	 */
	public List<ReferenceMatrix> getAll() {
		
		List<ReferenceMatrix> matrices = new ArrayList<>();
		for (String font : cache.keySet()) {
			matrices.addAll(cache.get(font));
		}
		return matrices;
	}
	
	/**
	 * Gets fonts in the cache
	 */
	public Set<String> getFonts() {
		
		return cache.keySet();
	}
}
