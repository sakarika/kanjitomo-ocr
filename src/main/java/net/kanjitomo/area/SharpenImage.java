package net.kanjitomo.area;

import net.kanjitomo.util.ImageUtil;

/**
 * Runs unsharp mask to the original image
 */
public class SharpenImage extends AreaStep {
	
	public SharpenImage(AreaTask task) {
		super(task, "original", "sharpened");
	}

	@Override
	protected void runImpl() throws Exception {
		task.sharpenedImage = ImageUtil.sharpenImage(task.originalImage, par);
	}
	
	@Override
	protected void addDebugImages() throws Exception {
		task.addDebugImage(task.originalImage, "original");
		task.addDebugImage(task.sharpenedImage, "sharpened");
	}
}
