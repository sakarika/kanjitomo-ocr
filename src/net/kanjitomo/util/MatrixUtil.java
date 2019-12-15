package net.kanjitomo.util;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Utility methods for 32x32 bit matrix manipulation
 */
public class MatrixUtil {

	/**
	 * Moves bits in int[] matrix
	 */
	public static int[] moveMatrix(int[] matrix, int horizontal, int vertical) {
		
		int[] matrix2 = new int[32];
		
		for (int y=0 ; y<matrix.length ; y++) {
			int newY = y + vertical;
			if (newY < 0 || newY > 31) {
				continue;
			}
			if (horizontal >= 0) {
				matrix2[newY] = matrix[y] >>> horizontal;
			} else {
				matrix2[newY] = matrix[y] << -1*horizontal;
			}
		}
		
		return matrix2;
	}

	/**
	 * Builds a matrix that represents bits around the argument matrix
	 * 
	 * @param layers how many halo layers are generated
	 */
	public static ArrayList<int[]> buildMatrixHalo(int[] matrix, int layers) {
		
		ArrayList<int[]> halo = new ArrayList<>();
		if (layers > 1) {
			matrix = matrix.clone();
		}
		
		for (int i=1 ; i<=layers ; i++) {
			int[] layer = new int[32];
			for (int y=0 ; y<32 ; y++) {
				for (int x=0 ; x<32 ; x++) {
					if (MatrixUtil.isHaloBit(x, y, matrix)) { 
						layer[y] |= 1;
					}
					if (x < 31) {
						layer[y] <<= 1;
					}
				}
			}
			halo.add(layer);
			if (i < layers) {
				for (int y=0 ; y<32 ; y++) {
					matrix[y] |= layer[y];
				}
			}
		}
			
		return halo;
	}

	/**
	 * Is the bit off by one from matrix
	 */
	public static boolean isHaloBit(int x, int y, int[] matrix) {
		
		if (MatrixUtil.isBitSet(x,y,matrix)) {
			return false;
		}
		
		for (int y2 = y-1 ; y2 <= y+1 ; y2++) {
			for (int x2 = x-1 ; x2 <= x+1 ; x2++) {
				if (MatrixUtil.isBitSet(x2,y2,matrix)) {
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * True if argument bit is set in matrix. false if outside matrix.
	 */
	public static boolean isBitSet(int x, int y, int[] matrix) {
		
		if (x < 0 || x >= 32 || y < 0 || y >= 32) {
			return false;		
		}
		
		int row = matrix[y];
		return (row & (1 << (31-x))) != 0;
	}

	/**
	 * Sets bit inside matrix. returns if outside matrix.
	 */
	public static void setBit(int x, int y, int[] matrix) {
		
		// https://stackoverflow.com/questions/12015598/how-to-set-unset-a-bit-at-specific-position-of-a-long
		if (x < 0 || x >= 32 || y < 0 || y >= 32) {
			return;		
		}
		
		matrix[y] |= 1 << (31-x);
	}

	/**
	 * Clears bit inside matrix. returns if outside matrix.
	 */
	public static void clearBit(int x, int y, int[] matrix) {
		
		// https://stackoverflow.com/questions/12015598/how-to-set-unset-a-bit-at-specific-position-of-a-long
		if (x < 0 || x >= 32 || y < 0 || y >= 32) {
			return;		
		}
		
		matrix[y] &= ~(1 << (31-x));
	}

	/**
	 * Counts set bits in matrix
	 */
	public static int countBits(int[] matrix) {
	
		int bits = 0;
		for (int row : matrix) {
			bits += Integer.bitCount(row);
		}
		
		return bits;
	}

	/**
	 * Counts set bits inside bounds in matrix
	 */
	public static int countBits(int[] matrix, Rectangle bounds) {
	
		int[] boundMatrix = new int[32];
		addBits(matrix, boundMatrix, bounds);
		
		int bits = 0;
		for (int y=bounds.y ; y<bounds.y + bounds.height ; y++) {
			bits += Integer.bitCount(boundMatrix[y]);
		}
		
		return bits;
	}
	
	/**
	 * Prints matrix to stdout
	 */
	public static void debugPrintMatrix(int[] matrix) {
		
		for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				if (isBitSet(x, y, matrix)) {
					System.out.print("x");
				} else {
					System.out.print(".");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * Prints two matrices side by side to stdout
	 */
	public static void debugPrintMatrix(int[] matrix1, int[] matrix2) {
		
		for (int y=0 ; y<32 ; y++) {
			String matrix1Line = "";
			String matrix2Line = "";
			for (int x=0 ; x<32 ; x++) {
				if (isBitSet(x, y, matrix1)) {
					matrix1Line += "x";
				} else {
					matrix1Line += ".";
				}
				if (isBitSet(x, y, matrix2)) {
					matrix2Line += "x";
				} else {
					matrix2Line += ".";
				}
			}
			System.out.println(matrix1Line + " " + matrix2Line);
		}
	}

	/**
	 * Moves rectangle inside matrix.
	 * 
	 * @param deltaX Bits inside rect are moved right by this amount
	 * @param deltaY Bits inside rect are moved down by this amount
	 */
	public static void moveRectangle(int[] matrix, Rectangle rect, int deltaX, int deltaY) {
		
		if (deltaX == 0 && deltaY == 0) {
			return;
		}
		
		// select iteration direction so that in-place replacement is possible
		
		int xMin   = rect.x;
		int xMax   = rect.x+rect.width-1;
		int xStart = deltaX >= 0 ? xMax : xMin;
		int xStop  = deltaX >= 0 ? xMin : xMax;
		int xStep  = deltaX >= 0 ? -1   : +1;
	
		int yMin   = rect.y;
		int yMax   = rect.y+rect.height-1;
		int yStart = deltaY >= 0 ? yMax : yMin;
		int yStop  = deltaY >= 0 ? yMin : yMax;
		int yStep  = deltaY >= 0 ? -1   : +1;		
		
		for (int sourceX=xStart ; xStep == +1 ? sourceX <= xStop : sourceX >= xStop ; sourceX += xStep) {
			for (int sourceY=yStart ; yStep == +1 ? sourceY <= yStop : sourceY >= yStop ; sourceY += yStep) {
				int targetX = sourceX + deltaX;
				int targetY = sourceY + deltaY;
				if (isBitSet(sourceX, sourceY, matrix)) {
					setBit(targetX, targetY, matrix);
					clearBit(sourceX, sourceY, matrix);
				} else {
					clearBit(targetX, targetY, matrix);
				}
			}
		}
	}

	/**
	 * Checks if matrices are equal
	 *
	 * @return true if equal
	 */
	public static boolean compareMatrix(int[] matrix1, int[] matrix2) {
		
		for (int x=0 ; x<32 ; x++) {
			for (int y=0 ; y<32 ; y++) {
				if (isBitSet(x, y, matrix1) != isBitSet(x, y, matrix2)) {
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Copies bits from source to target matrix. Restricted to rect area.
	 * Bits are translated by deltaX/Y amount.
	 * 
	 * @param clearTargetRect If true, target rectangle is first cleared (set to zero bits)). If false,
	 * bits are added but not removed.
	 */
	public static void copyBits(int[] source, int[] target, Rectangle rect, int deltaX, int deltaY,
			boolean clearTargetRect) {
	
		// clear target bits
		if (clearTargetRect) {
			int targetMinX = rect.x + deltaX;
			int targetMaxX = rect.x + rect.width - 1 + deltaX;
			int targetMinY = rect.y + deltaY;
			int targetMaxY = rect.y + rect.height - 1 + deltaY;
			if (targetMinX < 0) targetMinX = 0;
			if (targetMaxX > 31) targetMaxX = 31;
			if (targetMinY < 0) targetMinY = 0;
			if (targetMaxY > 31) targetMaxY = 31;
			int targetWidth = targetMaxX - targetMinX + 1;
			int mask = ~((~0 << (32 - targetWidth)) >>> targetMinX);
			for(int y=targetMinY ; y<=targetMaxY ; y++) {
				target[y] &= mask;
			}
		}
		
		// mask source bits (copy only from selected area)
		int sourceMinX = rect.x;
		int sourceMaxX = rect.x + rect.width - 1;
		int sourceMinY = rect.y;
		int sourceMaxY = rect.y + rect.height - 1;
		if (sourceMaxX > 31) sourceMaxX = 31;
		if (sourceMaxY > 31) sourceMaxY = 31;
		int sourceWidth = sourceMaxX - sourceMinX + 1;
		int mask = (~0 << (32 - sourceWidth)) >>> sourceMinX;
		
		// copy bits
		for (int sourceY = sourceMinY ; sourceY <= sourceMaxY ; sourceY++) {
			int targetY = sourceY + deltaY;
			if (targetY < 0 || targetY > 31) {
				continue;
			}
			if (deltaX >= 0) {
				target[targetY] |= (source[sourceY] & mask) >>> deltaX;
			} else {
				target[targetY] |= (source[sourceY] & mask) << -1*deltaX;
			}
		}
	}
	
	/**
	 * Stretches matrix by copying bits in the middle
	 */
	public static Rectangle stretchBits(int[] source, int[] target, Rectangle rect,
			int horizontalAmount, int verticalAmount) {
		
		if (horizontalAmount != 0 && verticalAmount != 0) {
			throw new Error("Not implemented");
			// TODO combined stretch
		} else if (horizontalAmount != 0) {
			return stretchBitsX(source, target, rect, horizontalAmount);
		} else {
			return stretchBitsY(source, target, rect, verticalAmount);
		}
	}
	
	/**
	 * Stretches matrix by copying bits in the middle to X direction
	 * 
	 * @param amount how many pixels source is stretched
	 * @return new bounds
	 */
	public static Rectangle stretchBitsX(int[] source, int[] target, Rectangle rect, int amount) {
		
		if (amount <= 0) {
			throw new Error("amount must be positive");
			// TODO shrink
		}
		
		int dividerX = rect.x + rect.width/2;
		Rectangle rightBlock = new Rectangle(dividerX, rect.y, 
				rect.x + rect.width - 1 - dividerX + 1, rect.height);
		Rectangle leftBlock = new Rectangle(rect.x, rect.y, dividerX - rect.x + 1, rect.height);
		Rectangle divider = new Rectangle(dividerX, rect.y, 1, rect.height);
		
		int moveRight = (amount + 1)/2;
		int moveLeft = amount - moveRight;
		
		// move block
		copyBits(source, target, rightBlock, moveRight, 0, false);
		copyBits(source, target, leftBlock, -moveLeft, 0, false);
		
		// fill gap
		for (int deltaX = -moveLeft+1 ; deltaX < moveRight ; deltaX++) {
			copyBits(source, target, divider, deltaX, 0, false);
		}
		
		// check bounds
		Rectangle unbounded =  new Rectangle(rect.x - moveLeft, rect.y, rect.width + amount, rect.height);
		Rectangle limits = new Rectangle(0,0,32,32);
		return unbounded.intersection(limits);
	}
	
	/**
	 * Stretches matrix by copying bits in the middle to Y direction
	 * 
	 * @param amount how many pixels source is stretched
	 * @return new bounds
	 */
	public static Rectangle stretchBitsY(int[] source, int[] target, Rectangle rect, int amount) {
		
		if (amount <= 0) {
			throw new Error("amount must be positive");
			// TODO shrink
		}
		
		int dividerY = rect.y + rect.height/2;
		Rectangle bottomBlock = new Rectangle(rect.x, dividerY, 
				rect.width, rect.y + rect.height - 1 - dividerY + 1);
		Rectangle upBlock = new Rectangle(rect.x, rect.y, rect.width, dividerY - rect.y + 1);
		Rectangle divider = new Rectangle(rect.x, dividerY, rect.width, 1);
		
		int moveDown = (amount + 1)/2;
		int moveUp = amount - moveDown;
		
		// move block
		copyBits(source, target, bottomBlock, 0, moveDown, false);
		copyBits(source, target, upBlock, 0, -moveUp, false);
		
		// fill gap
		for (int deltaY = -moveUp+1 ; deltaY < moveDown ; deltaY++) {
			copyBits(source, target, divider, 0, deltaY, false);
		}

		// check bounds
		Rectangle unbounded = new Rectangle(rect.x, rect.y - moveUp, rect.width, rect.height + amount);
		Rectangle limits = new Rectangle(0,0,32,32);
		return unbounded.intersection(limits);
	}	
	
	/**
	 * Adds all source bits into target. 0 in source doesn't overwrite target.
	 */
	public static void addBits(int[] source, int[] target) {
		
		for (int y=0 ; y<32 ; y++) {
			target[y] |= source[y]; 
		}
	}

	/**
	 * Adds all source bits into target within bounds. 0 in source doesn't overwrite target.
	 */
	public static void addBits(int[] source, int[] target, Rectangle bounds) {
		
		int mask = (~0 << (32 - bounds.width)) >>> bounds.x;
		for (int y=bounds.y ; y<bounds.y + bounds.height ; y++) {
			target[y] |= source[y] & mask;
		}
	}

	/**
	 * Removes all source bits from target. 0 in source doesn't overwrite target.
	 */
	public static void removeBits(int[] source, int[] target) {
		
		for (int y=0 ; y<32 ; y++) {
			target[y] &= ~source[y]; 
		}
	}

	/**
	 * Removes all source bits from target within bounds. 0 in source doesn't overwrite target.
	 */
	public static void removeBits(int[] source, int[] target, Rectangle bounds) {
		
		int mask = (~0 << (32 - bounds.width)) >>> bounds.x;
		for (int y=bounds.y ; y<bounds.y + bounds.height ; y++) {
			target[y] &= ~(source[y] & mask);
		}
	}

	/**
	 * Debug prints bits in value;
	 */
	public static void printBits(int value) {
		
		for (int x=0 ; x<32 ; x++) {
			if ((value & (1 << (31-x))) != 0) {
				System.out.print("1");
			} else {
				System.out.print("0");
			}
		}
		System.out.println();
	}
	
	/**
	 * Finds bounds around set bits
	 * 
	 * @return null if no bits found
	 */
	public static Rectangle findBounds(int[] matrix) {
		
		if (countBits(matrix) == 0) {
			return null;
		}
		
		int left = 0;
		left: for (int x=0 ; x<32 ; x++) {
			for (int y=0 ; y<32 ; y++) {
				if (isBitSet(x, y, matrix)) {
					break left;
				}
			}
			++left;
		}
		
		int right = 31;
		right: for (int x=31 ; x>=0 ; x--) {
			for (int y=0 ; y<32 ; y++) {
				if (isBitSet(x, y, matrix)) {
					break right;
				}
			}
			--right;
		}		
		
		int up = 0;
		up: for (int y=0 ; y<32 ; y++) {
			for (int x=0 ; x<32 ; x++) {
				if (isBitSet(x, y, matrix)) {
					break up;
				}
			}
			++up;
		}		
		
		int down = 31;
		down: for (int y=31 ; y>=0 ; y--) {
			for (int x=0 ; x<32 ; x++) {
				if (isBitSet(x, y, matrix)) {
					break down;
				}
			}
			--down;
		}
		
		int width = right - left + 1;
		int height = down - up + 1;
		
		return new Rectangle(left, up, width, height);
	}
}
