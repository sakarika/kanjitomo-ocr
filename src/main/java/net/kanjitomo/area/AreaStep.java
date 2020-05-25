package net.kanjitomo.area;

import net.kanjitomo.util.Parameters;

/**
 * Single algorithm step in area detector. Each step should do only one job such
 * as merging areas in left/right direction. Debug images can be generated after
 * each step.
 */
public abstract class AreaStep {

	protected AreaTask task;
	protected Parameters par = Parameters.getInstance();
	
	/**
	 * If true, debug images are generated after this step.
	 */
	final protected boolean addDebugImages;
	
	/**
	 * @param debugImages List of debug images that can be generated from this step.
	 * Any image generated in addDebugImages must first be registered here.
	 */
	public AreaStep(AreaTask task, String ... debugImages) {
		
		this.task = task;
		
		// check if this step should write debug images
		if (par.isSaveAreaFailed()) {
			for (String s1 : par.debugImages) {
				for (String s2 : debugImages) {
					if (s1.equals(s2)) {
						addDebugImages = true;
						return;
					}
				}
			}
		}
		addDebugImages = false;
	}
	
	/**
	 * Runs the algorithm step and creates debug information if requested.
	 */
	public void run() throws Exception {
		
		long started = System.currentTimeMillis();
		
		runImpl();
		
		if (task.columns != null && task.columns.size() > 0) {
			task.collectAreas();
		}
		
		long done = System.currentTimeMillis();
		if (par.isPrintDebug()) {
			String subclassName = this.getClass().getName();
			System.out.println(subclassName+" "+(done - started)+" ms");
		}
		if (addDebugImages) {
			addDebugImages();
		}
		
		task.clearChangedFlags();
	}
	
	/**
	 * The actual implementation of the algorithm step.
	 */
	protected abstract void runImpl() throws Exception;
	
	/**
	 * Paints and adds debug images to task
	 */
	protected abstract void addDebugImages() throws Exception;
}
