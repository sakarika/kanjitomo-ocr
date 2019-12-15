package net.kanjitomo.dictionary;

/**
 * Single character
 */
public class Monogram extends NGram {

	public final Character first;

	public Monogram(char first) {
		this.first = first;
	}
	
	@Override
    public int hashCode() {
		return first;
    }
	
	@Override
	public boolean equals(Object obj) {
		Monogram dg2 = (Monogram)obj;
		return first.equals(dg2.first);
	};
	
	public String toString() {
		return ""+first;
	}
}
