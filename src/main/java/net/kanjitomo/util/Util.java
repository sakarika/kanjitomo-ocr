package net.kanjitomo.util;

import java.io.File;

public class Util {

    /**
     * Finds file reference
    */
    public static File findFile(String fileName) throws Exception {
    	
    	// this is needed because location can be relative to class or jar file,
    	// depending on if the program is launched directly from Eclipse or packaged first
    	File file;
    	File projectDir = new File(Util.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
    	file = new File(projectDir.getAbsolutePath()+"/"+fileName);
    	if (file.exists()) {
    		return file;
    	}
    	file = new File(projectDir.getAbsolutePath()+"/../"+fileName);
    	if (file.exists()) {
    		return file;
    	} else {
    		throw new Exception("File not found:"+fileName);
    	}
    }
    
    /**
     * Scales sourceValue to target value range
     */
    public static float scale(float sourceValue,
    		float minSourceValue, float maxSourceValue,
    		float targetValue1, float targetValue2) {
    	
    	if (minSourceValue > maxSourceValue) {
    		throw new Error("minSourceValue:"+minSourceValue+" larger than maxSourceValue:"+maxSourceValue);
    	}
    	
    	if (sourceValue < minSourceValue) {
    		sourceValue = minSourceValue;
    	} else if (sourceValue > maxSourceValue) {
    		sourceValue = maxSourceValue;
    	}
    	
    	float scale = (sourceValue - minSourceValue) / (maxSourceValue - minSourceValue);
    	
    	return targetValue1 * (1 - scale) + targetValue2 * scale;
    }
       
    /**
     * Prints array as: [a,b,c,..]
     */
    public static String printArray(int[] array) {
    	
    	String ret = "[";
    	for (int i=0 ; i<array.length ; i++) {
    		ret += array[i];
    		if (i < array.length-1) {
    			ret += ",";
    		}
    	}
    	ret += "]";
    	
    	return ret;
    }
    
    /**
     * Creates boolean matrix from bitmap matrix
     */
    public static boolean[][] createBinaryMatrix32(int[] matrix) {
    	
    	if (matrix.length != 32) {
    		new Error("Invalid length");
    	}
    	
    	boolean[][] boolMatrix = new boolean[32][32];
    	
    	for (int y=0 ; y<32 ; y++) {
    		for (int x=0 ; x<32 ; x++) {
    			if (MatrixUtil.isBitSet(x, y, matrix)) {
    				boolMatrix[x][y] = true;
    			}
    		}
    	}
    	
    	return boolMatrix;
    }

	/**
	 * Prints java memory statistics
	 */
	public static void printMemoryUsage() {
		
		System.gc();
		long heapSize = Runtime.getRuntime().totalMemory(); 
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		long heapUsedSize = heapSize - heapFreeSize;
		System.out.println("\nMemory usage:");
		System.out.println("heapSize    :"+heapSize);
		System.out.println("heapMaxSize :"+heapMaxSize);
		System.out.println("heapFreeSize:"+heapFreeSize);
		System.out.println("heapUsedSize:"+heapUsedSize);		
	}
}
