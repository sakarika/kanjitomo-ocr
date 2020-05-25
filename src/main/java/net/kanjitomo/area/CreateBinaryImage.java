package net.kanjitomo.area;

import java.awt.image.BufferedImage;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.Parameters;

/**
 * Creates a binary (black and white) image from grayscale image.
 */
public class CreateBinaryImage extends AreaStep {
		
	public CreateBinaryImage(AreaTask task) {
		super(task, "binary");
	}
	
	@Override
	protected void runImpl() throws Exception {

		// TODO instead of static blackThreshold calculate a histogram?
		BufferedImage bwImage = ImageUtil.makeBlackAndWhite(task.sharpenedImage,
				Parameters.fixedBlackLevelEnabled ? null : par.pixelRGBThreshold); 
		
		task.binaryImage = ImageUtil.createMatrixFromImage(bwImage); 
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		
		BufferedImage image = ImageUtil.createImageFromMatrix(task.binaryImage);
		task.addDebugImage(image, "binary");
	}
}
