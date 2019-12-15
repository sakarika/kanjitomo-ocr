package net.kanjitomo.ocr;

public class Pixel {
	
	final int x;
	final int y;
	
	public Pixel(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public int hashCode() {
		return x + 100000*y;
	}
	
	@Override
	public boolean equals(Object obj) {
		return ((Pixel)obj).x == x && ((Pixel)obj).y == y;
	}
	
	public boolean isNeighbour(Pixel px2) {
		int deltaX = Math.abs(x - px2.x);
		int deltaY = Math.abs(y - px2.y);
		if (deltaX <= 1 && deltaY <= 1) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return x+","+y;
	}
}
