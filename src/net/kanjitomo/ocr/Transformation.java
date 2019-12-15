package net.kanjitomo.ocr;

import java.io.Serializable;

/**
 * Parameters used to modify bitmap before aligment.
 */
public class Transformation implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// positive translate moves image right or down by one pixel
	final int horizontalTranslate; // TODO rename to deltaX?
	final int verticalTranslate;
	
	// positive stretch makes image larger by one pixel
	final int horizontalStretch;
	final int verticalStretch;
	
	/**
	 * Creates default no-op transformation (all parameters zero).
	 */
	public Transformation() {
		
		horizontalTranslate = 0;
		verticalTranslate = 0;
		horizontalStretch = 0;
		verticalStretch = 0;
	}
	
	public Transformation(int horizontalTranslate, int verticalTranslate,
			int horizontalStretch, int verticalStretch) {
		
		this.horizontalTranslate = horizontalTranslate;
		this.verticalTranslate = verticalTranslate;
		this.horizontalStretch = horizontalStretch;
		this.verticalStretch = verticalStretch;		
	}
	
	/**
	 * @return true if this transformation contains argument parameters
	 */
	public boolean contains(int horizontalTranslate, int verticalTranslate,
			int horizontalStretch, int verticalStretch) {
		
		if (this.horizontalTranslate == horizontalTranslate &&
			this.verticalTranslate == verticalTranslate &&
			this.horizontalStretch == horizontalStretch &&
			this.verticalStretch == verticalStretch) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		
		Transformation p = (Transformation)obj;
		
		return p.horizontalTranslate == horizontalTranslate &&
			p.verticalTranslate == verticalTranslate &&
			p.horizontalStretch == horizontalStretch &&
			p.verticalStretch == verticalStretch;
	}
	
	@Override
	public int hashCode() {
		
		return horizontalTranslate + verticalTranslate + 
			horizontalStretch + verticalStretch;
	}
	
	@Override
	public String toString() {
		
		return horizontalTranslate+"."+verticalTranslate+"."+
				horizontalStretch+"."+verticalStretch;
	}
}
