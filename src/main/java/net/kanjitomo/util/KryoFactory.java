package net.kanjitomo.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;

import net.kanjitomo.Word;
import net.kanjitomo.dictionary.Dictionary;
import net.kanjitomo.dictionary.Digram;
import net.kanjitomo.dictionary.Trigram;
import net.kanjitomo.ocr.Component;
import net.kanjitomo.ocr.ReferenceMatrix;
import net.kanjitomo.ocr.Transformation;

/**
 * Build Kryo objects and registers classes
 */
public abstract class KryoFactory {

	/**
	 * Gets Kryo object and registers classes
	 */
	public static Kryo getKryo() {
		
		Kryo kryo = new Kryo();
		
		// classes in serializable object trees must be defined here
		// all files serialized by Kryo must be rebuild if this list changes!
		kryo.register(Word.class);
		kryo.register(Dictionary.class);
		kryo.register(HashMap.class);
		kryo.register(Digram.class);
		kryo.register(Trigram.class);
		kryo.register(ArrayList.class);
		kryo.register(ReferenceMatrix.class);
		kryo.register(Component.class);
		kryo.register(Transformation.class);
		kryo.register(Rectangle.class);
		kryo.register(String.class);
		kryo.register(Character.class);
		kryo.register(Integer.class);
		kryo.register(Float.class);
		kryo.register(int[].class);
		
		return kryo;
	}
}
