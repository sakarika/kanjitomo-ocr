package net.kanjitomo.ocr;

import java.io.File;

import net.kanjitomo.util.Parameters;

/**
 * Calculates reference matrix hash values and cache file names.
 */
public class ReferenceMatrixHashCalculator {

	/**
	 * Gets filename that represents set of reference matrices
	 */
	public static String getReferenceFileName(String font, int targetSize, int ocrHaloSize,
			String characters) throws Exception {
		
		int hashCode = calcHashCode(font, Parameters.targetSize,
				Parameters.ocrHaloSize, Characters.all);
		return "CHARACTERS_"+Integer.toHexString(hashCode).toUpperCase()+".cache";
	}
	
	/**
	 * Gets file that represents set of reference matrices
	 */
	public static File getReferenceFile(String font, int targetSize, int ocrHaloSize,
			String characters) throws Exception {
		
		return new File(Parameters.getInstance().getCacheDir()+"/"+
				getReferenceFileName(font, targetSize, ocrHaloSize, characters));
	}
	
	private static int calcHashCode(String font, int targetSize, int ocrHaloSize,
			String characters) {
		
		int hashCode = smear(font.hashCode());
		hashCode += smear(targetSize*1000);
		hashCode += smear(ocrHaloSize*1000000);
		
		for (Character c : characters.toCharArray()) {
			hashCode += smear(c);
		}
		
		return hashCode;
	}
	
	/*
	 * This method was written by Doug Lea with assistance from members of JCP
	 * JSR-166 Expert Group and released to the public domain, as explained at
	 * http://creativecommons.org/licenses/publicdomain
	 * 
	 * As of 2010/06/11, this method is identical to the (package private) hash
	 * method in OpenJDK 7's java.util.HashMap class.
	 */
	private static int smear(int hashCode) {
		
		// https://stackoverflow.com/questions/9624963/java-simplest-integer-hash
	    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
	    return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
	}
}
