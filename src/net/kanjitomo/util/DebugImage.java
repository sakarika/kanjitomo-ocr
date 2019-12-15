package net.kanjitomo.util;

import java.awt.image.BufferedImage;

/**
 * Debug images are used during development to visualize processing steps
 */
public class DebugImage {

	/**
	 * Short one-word description of the processing step. For example: "binary" 
	 */
	final String step;
	
	final BufferedImage image;
	
	/**
	 * True if this image has vertical columns, false if horizontal, 
	 * null if both or no columns.
	 */
	final Boolean vertical;
	
	final String filename;
	
	/**
	 * @param step Short one-word description of the image. For example: "binary"
	 * This appears in file name and can be referenced in par.filterDebugImages 
	 * 
	 * @param vertical If set, orientation is diplayed in the file name
	 * 
	 * @param prefix String added in front of debug file names. Contains test reference
	 * and image sequence number
	 */
	public DebugImage(BufferedImage image, String step, Boolean vertical, String prefix) {
		
		this.image = image;
		this.step = step;
		this.vertical = vertical;
		
		String verticalStr = "";
		if (vertical != null) {
			verticalStr = vertical ? ".vertical" : ".horizontal";
		}
		
		filename = prefix+"."+step+verticalStr+".png";
	}
	
	public BufferedImage getImage() {
		return image;
	}
	
	public String getStep() {
		return step;
	}
	
	public Boolean getVertical() {
		return vertical;
	}
	
	public String getFilename() {
		return filename;
	}
}
