package net.kanjitomo.ocr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import net.kanjitomo.util.ImageUtil;
import net.kanjitomo.util.KryoFactory;
import net.kanjitomo.util.MatrixUtil;
import net.kanjitomo.util.Parameters;

/**
 * Builds the reference matrix cache.
 * 
 * This needs to be run only if the character set changes.
 */
public class ReferenceMatrixCacheBuilder {

	/**
	 * Skips cache files that already exists
	 */
	private static boolean SKIP_EXISTS = false; 
	
	private Parameters par = Parameters.getInstance();
	private Set<Character> characters;
	private ComponentBuilder components;
	
	public ReferenceMatrixCacheBuilder() throws Exception {
		
		components = new ComponentBuilder();
	}
	
	public void buildCache() throws Exception {
	
		System.out.println("Building reference cache");
		for (String font : par.referenceFonts) {
			generateCharacters(font);
		}	
	}
	
	/**
	 * Generates characters for given font
	 */
	private void generateCharacters(String font) throws Exception {

		// skip if already generated
		File file = ReferenceMatrixHashCalculator.getReferenceFile(font, Parameters.targetSize,
				Parameters.ocrHaloSize, Characters.all);
		if (file.exists() && SKIP_EXISTS) {
			System.out.println(font+" already generated\nfile:"+file);
			return;
		}
		
		// generate characters
		checkFont(font);
		characters = new HashSet<Character>();
		System.out.println("Generating characters for "+font+"\nfile:"+file);
		int index = 0;
		// using Arraylist type because List doesn't implement serializable
		ArrayList<ReferenceMatrix> matrixList = new ArrayList<ReferenceMatrix>();
		for (Character c : Characters.getCharacters()) {
			if (characters.contains(c)) {
				throw new Exception("Duplicate character:"+c);
			}
			ReferenceMatrix matrix = buildReferenceMatrix(c, Parameters.targetSize, font);
			matrixList.add(matrix);
			characters.add(c);
			if (++index % 100 == 0) {
				System.out.println("Progress:"+index);
			}
		}
		System.out.println(index+" characters done");
		
		// save to disk
		serialize(font, matrixList);
	}
	
	/**
	 * Builds a bitmap representation of the character
	 */
	private ReferenceMatrix buildReferenceMatrix(char character, int targetSize, String fontName) throws Exception {
		
		BufferedImage image = paintCharacterBestFit(character, targetSize, fontName);
		
		ReferenceMatrix ref = new ReferenceMatrix();
		ref.character = character;
		ref.matrix = ImageUtil.buildMatrix32(image);
		ref.halo = MatrixUtil.buildMatrixHalo(ref.matrix, Parameters.ocrHaloSize-1);
		ref.pixels = MatrixUtil.countBits(ref.matrix);
		ref.fontName = fontName;
		ref.components = components.buildComponents(ref);
		
		// is this the only character that is different in vertical orientation?
		if (character == '｜') {
			ref.character = 'ー';
		}
		
		return ref;
	}
	
	/**
	 * Paints character to targetSize image. Tries multiple font sizes and selects one that
	 * fits the target size best. Resized to fit targetSize exactly.
	 */
	private BufferedImage paintCharacterBestFit(char character, int targetSize, String fontName) throws Exception {
		
		// font size does not correspond to pixels,
		// draw with multiple sizes, select the closest one	
		BufferedImage bestImage = null;
		int bestFit = 100;
		int style = isFontBold(fontName) ? Font.BOLD : Font.PLAIN;
		for (int size=25 ; size<45 ; size++) {
			Font font = new Font(fontName, style, size);
			BufferedImage image = paintCharacter(character, font);
			int hfit = Math.abs(image.getWidth() - targetSize);
			int vfit = Math.abs(image.getHeight() - targetSize);
			int fit = hfit < vfit ? hfit : vfit;
			if (fit < bestFit) {
				bestFit = fit;
				bestImage = image;
			}
		}
		
		// resize image to targetSize, center and surround with white border to 32x32 pixels
		bestImage = ImageUtil.stretchCheckRatio(bestImage, targetSize, 32);
		bestImage = ImageUtil.makeBlackAndWhite(bestImage, par.pixelRGBThreshold);
		
		checkRow(bestImage, character, 31-(32 - targetSize)/2);
		
		return bestImage;
	}
	
	/**
	 * True if font should be bold
	 */
	private boolean isFontBold(String fontName) throws Exception {
	
		for (int i=0 ; i<par.referenceFonts.length ; i++) {
			if (par.referenceFonts[i].equals(fontName)) {
				return par.referenceFontsBold[i];
			}
		}
		
		throw new Exception("Font:"+fontName+" not found");
	}
	
	/**
	 * Some character (山 for example) contain sharp edges at bottom corners in some fonts.
	 * Reference images should be at neutral as possible so these edges are cut by one pixe.
	 * row.
	 */
	private void checkRow(BufferedImage image, char character, int bottomRow) {
		
		int bottomLeftPixels = 0;
		int bottomMiddlePixels = 0;
		int bottomRightPixels = 0;
		
		for (int x = 0 ; x<32 ; x++) {
			if (image.getRGB(x, bottomRow) == Color.BLACK.getRGB()) {
				if (x <= 12) {
					++bottomLeftPixels;
				} else if (x >= 20) {
					++bottomRightPixels;
				} else {
					++bottomMiddlePixels;
				}
			}
		}
		
		if (bottomMiddlePixels > 0) {
			return;
		}

		if (bottomLeftPixels > 5 || bottomRightPixels > 5) {
			return;
		}
		
		if (bottomLeftPixels == 0 || bottomRightPixels == 0) {
			return;
		}
		
//		System.err.println("row:"+bottomRow);
//		System.err.println("pixels:"+bottomLeftPixels+","+bottomMiddlePixels+","+bottomRightPixels);
//		System.err.println("cut:"+character);

		for (int x = 0 ; x<32 ; x++) {
			image.setRGB(x, bottomRow, Color.WHITE.getRGB());
		}
	}
	
	/**
	 * Paints character to empty canvas
	 */
	private BufferedImage paintCharacter(char character, Font font) throws Exception {
		
		BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight()); // fill with white
		graphics.setBackground(Color.WHITE);
		graphics.setColor(Color.BLACK);
		graphics.setFont(font);
		graphics.drawString(character+"", 35, 35); // location unpredictable, draw to large canvas and center later
		
		return cutBorders(image);
	}
	
	/**
	 * Cut white pixels around character
	 */
	private static BufferedImage cutBorders(BufferedImage image) {

		int xMin = image.getWidth();
		int xMax = 0;
		int yMin = image.getHeight();
		int yMax = 0;
		
		for (int y=0 ; y<image.getHeight() ; y++) {
			for (int x=0; x<image.getWidth() ; x++) {
				if (image.getRGB(x, y) == Color.BLACK.getRGB()) {
					if (x > xMax) xMax = x;
					if (x < xMin) xMin = x;
					if (y > yMax) yMax = y;
					if (y < yMin) yMin = y;
				}
			}
		}
		
		int width = xMax-xMin+1;
		int height = yMax-yMin+1;
		
		return image.getSubimage(xMin, yMin, width, height);
	}
	
	/**
	 * Serializes cache to disk using Kryo library
	 * 
	 * https://github.com/EsotericSoftware/kryo
	 * https://www.baeldung.com/kryo
	 */
	private void serialize(String font, ArrayList<ReferenceMatrix> matrixList) throws Exception {
	
		File file = ReferenceMatrixHashCalculator.getReferenceFile(font, Parameters.targetSize,
				Parameters.ocrHaloSize, Characters.all);
		Kryo kryo = KryoFactory.getKryo();
		Output output = new Output(new FileOutputStream(file));
		kryo.writeClassAndObject(output, matrixList);
		output.close();
	}
	
	/**
	 * Checks that font supports Japanese characters. Throws exception if not.
	 */
	private void checkFont(String fontName) throws Exception {
		
		for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
			if (font.getName().equals(fontName)) {
				if (font.canDisplay('新') && font.canDisplay('を') && font.canDisplay('ア')) {
					return;
				} else {
					throw new Exception("Font:"+fontName+" doesn't support Japanese characters");
				}
			}
		}
		
		throw new Exception("Font:"+fontName+" not found");
	}
	
	public static void main(String[] args) {
		try {
			ReferenceMatrixCacheBuilder cache = new ReferenceMatrixCacheBuilder();
			cache.buildCache();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
