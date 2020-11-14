# kanjitomo-ocr

<h2>Introduction</h2>

KanjiTomo OCR is a Java library for identifying Japanese characters from images. The algorithm used in this library is custom made, originally used with KanjiTomo program: <a href="https://www.kanjitomo.net/">https://www.kanjitomo.net/</a> Description of the algorithm <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/how it works/">is here</a>.
<br><br>
This library is intented to be used with interactive programs where user can point to individual words with a mouse. Batch-processing whole pages is not supported. 

<h2>Installation</h2>

<ul>
	<li>Include <a href="deploy/KanjiTomoOCR.jar">KanjiTomoOCR.jar</a> to your project</li>
	<li>Add "--illegal-access=deny" JVM parameter, this is not strictly required but prevents unnecessary warnings on startup</li>
	<li>"-Xmx1200m" and "-server" JVM parameters are also recommended for performance reasons</li>
</ul>

<h2>Usage</h2>

<ul>
	<li>Create <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/KanjiTomo.html">KanjiTomo</a> class instance</li>
	<li>Load data structures with <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/KanjiTomo.html#loadData()">loadData</a> method. This needs to be done only at startup.</li>
	<li>Set the target image with <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/KanjiTomo.html#setTargetImage(java.awt.image.BufferedImage)">setTargetImage</a> method. This can be whole page or screenshot around target word. Screenshots around mouse cursor can be taken with Java's <a href="https://docs.oracle.com/javase/7/docs/api/java/awt/Robot.html">Robot</a> class.</li>
	<li>Start OCR with <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/KanjiTomo.html#runOCR(java.awt.Point)">runOCR</a> method. Point argument determines the first character to be scanned in target image's coordinates and should correspond to mouse cursor location.</li>
	<li>Results are returned as <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/OCRResults.html">OCRResults</a> object. <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/OCRResults.html#bestMatchingCharacters">bestMatchingCharacters</a> field contains a list of identified characters and <a href="https://raw.githack.com/sakarika/kanjitomo-ocr/master/javadoc/net/kanjitomo/OCRResults.html#words">words</a> list contains results of a dictionary search from these characters.</li>
</ul>

<h2>Example</h2>

```java
KanjiTomo tomo = new KanjiTomo();
tomo.loadData();
BufferedImage image = ImageIO.read(new File("file.png"));
tomo.setTargetImage(image);
OCRResults results = tomo.runOCR(new Point(80,40));
System.out.println(results);
```

<h2>License</h2>

KanjiTomo is free to use for non-commercial purposes. License file is <a href="LICENSE.txt">here</a>

<h2>Credits</h2>

KanjiTomo has been created by Sakari K&#228;&#228;ri&#228;inen. You can contact me at kanjitomo(at)gmail.com

<h2>Acknowledgements</h2>

EDICT, ENAMDICT and KANJIDIC dictionaries are the property of the Electronic Dictionary Research and Development Group, and are used in conformance with the Group's licence.<br> 
<a href="https://www.edrdg.org/jmdict/edict.html">https://www.edrdg.org/jmdict/edict.html</a>
<br><br>
imgscalr library by Riyad Kalla<br>
<a href="https://github.com/rkalla/imgscalr">https://github.com/rkalla/imgscalr</a>
<br><br>
Unsharp Mask code by Romain Guy<br>
<a href="http://www.java2s.com/Code/Java/Advanced-Graphics/UnsharpMaskDemo.htm">http://www.java2s.com/Code/Java/Advanced-Graphics/UnsharpMaskDemo.htm</a>
<br><br>
Kryo library by EsotericSoftware<br>
<a href="https://github.com/EsotericSoftware/kryo">https://github.com/EsotericSoftware/kryo</a>

