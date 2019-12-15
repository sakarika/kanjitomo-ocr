package net.kanjitomo.ocr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import net.kanjitomo.util.KryoFactory;
import net.kanjitomo.util.Parameters;

/**
 * Loads the cache of reference matrices
 */
public class ReferenceMatrixCacheLoader {

	private Parameters par = Parameters.getInstance();

	private static ReferenceMatrixCache cache;
		
	/**
	 * Loads the cache from serialized data file (unless already done)
	 */
	public void load() throws Exception {
		
		if (cache != null) {
			return;
		}
		long started = System.currentTimeMillis();
		
		deserialize();
		applyScoreModifiers();
		
		long done = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			System.out.println("ReferenceMatrixCache "+(done - started)+" ms");
		}
	}
	
	public ReferenceMatrixCache getCache() {
		return cache;
	}
	
	/**
	 * Gets reference matrices for given font
	 */
	public List<ReferenceMatrix> getReferences(String font) throws Exception {
		return cache.get(font);
	}
	
	/**
	 * Reads serialized matrices from disk using Kryo library
	 * 
	 * https://github.com/EsotericSoftware/kryo
	 * https://www.baeldung.com/kryo
	 */
	private void deserialize() throws Exception {
		
		cache = new ReferenceMatrixCache();
		for (String font : par.referenceFonts) {
			String fileName = ReferenceMatrixHashCalculator.getReferenceFileName(font, Parameters.targetSize,
					Parameters.ocrHaloSize, Characters.all);
			if (deserializeJar(font, fileName)) {
				continue;
			}
			File file = ReferenceMatrixHashCalculator.getReferenceFile(font, Parameters.targetSize,
					Parameters.ocrHaloSize, Characters.all);
			if (deserializeFile(font, file)) {
				continue;
			}
			throw new Exception(file.getName()+" not found, rebuild cache");
		}
	}
	
	private boolean deserializeJar(String font, String resourceName) throws Exception {
		
		InputStream in = getClass().getResourceAsStream("/"+resourceName);
		if (in == null) {
			return false;
		}
		if (par.isPrintOutput()) {
			System.out.println("Deserializing references:"+resourceName+" from jar");
		}
		deserializeStream(font, in);	
		return true;
	}
	
	private boolean deserializeFile(String font, File file) throws Exception {
		
		if (!file.exists()) {
			return false;
		}
		if (par.isPrintOutput()) {
			System.out.println("Deserializing references from file:"+file);
		}
		deserializeStream(font, new FileInputStream(file));
		return true;
	}	
	
	private void deserializeStream(String font, InputStream in) {
		
		Kryo kryo = KryoFactory.getKryo();
		Input input = new Input(in);
		@SuppressWarnings("unchecked")
		ArrayList<ReferenceMatrix> list = (ArrayList<ReferenceMatrix>) kryo.readClassAndObject(input);
		cache.put(font, list);
		input.close();
	}
	
	private void applyScoreModifiers() {
		
		for (ReferenceMatrix matrix : cache.getAll()) {
			matrix.scoreModifier = Characters.getScoreModifier(matrix.character);
		}
	}
}
