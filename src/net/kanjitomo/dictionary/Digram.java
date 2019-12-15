package net.kanjitomo.dictionary;

/**
 * Pair of characters
 */
public class Digram extends NGram {

	public final Character first;
	public final Character second;

	public Digram(char first, char second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
    public int hashCode() {
		return first + second*1000;
    }
	
	@Override
	public boolean equals(Object obj) {
		Digram dg2 = (Digram)obj;
		return first.equals(dg2.first) && second.equals(dg2.second);
	};
	
	public String toString() {
		return ""+first+second;
	}
}
