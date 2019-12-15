package net.kanjitomo.dictionary;

/**
 * Three characters
 */
public class Trigram extends NGram {

	public final char first;
	public final char second;
	public final char third;

	public Trigram(char first, char second, char third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	@Override
    public int hashCode() {
		return first + second*1000 + third*1000000;
    }
	
	@Override
	public boolean equals(Object obj) {
		Trigram o = (Trigram)obj;
		return first == o.first && second == o.second && third == o.third;
	};
	
	public String toString() {
		return ""+first+second+third;
	}
}
