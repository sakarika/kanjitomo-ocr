package net.kanjitomo.ocr.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kanjitomo.util.Parameters;

/**
 * Loads set of tests from a file
 * 
 */
public class TestSetLoader {

	private Parameters par = Parameters.getInstance();
	private File testDir;
	
	/**
	 * Reads test sets from "test" directory
	 * @return Test set name -> TestSet object
	 * @throws Exception
	 */
	public Map<String, TestSet> readTestSets() throws Exception {
		
		Map<String, TestSet> testSets = new HashMap<String, TestSet>();
		testDir = par.getTestDir();
		for (File file : testDir.listFiles()) {
			if (file.getName().endsWith(".txt") && !file.getName().toLowerCase().equals("readme.txt")) {
				TestSet testSet = readTestSet(file);
				testSets.put(testSet.name, testSet);
			}
		}
		
		return testSets;
	}
	
	/**
	 * Reads tests from file
	 * 
	 * Example:
	 * 
	 * # this is a comment
	 * filename1 51,325,22,114 # area test x,y,width,height
	 * filename2 35,75,思  # OCR test x,y,character
	 * 
	 * @param file Filename in base/test dir
	 */
	private TestSet readTestSet(File testSetFile) throws Exception {
		
		String testImageDirName = testSetFile.getName().replace(".txt", "")+" images";
		File testImagesDir = new File(testSetFile.getParentFile().getAbsolutePath()+"//"+testImageDirName);
		if (!testImagesDir.exists()) {
			throw new Exception("Test image directory:"+testImagesDir+" doesn't exist");
		}
		
		// parse file
		System.out.println("parsing file:"+testSetFile);
		List<TestImage> testImages = new ArrayList<>();
		String line;
        BufferedReader in = new BufferedReader(
           new InputStreamReader(new FileInputStream(testSetFile), "UTF-8"));
        while ((line = in.readLine()) != null) {
        	TestImage test = parseLine(testImagesDir, line);
        	if (test != null) {
        		testImages.add(test);
        	}
        }
        in.close();
        System.out.println(testImages.size()+" tests loaded");

        TestSet testSet = new TestSet();
        testSet.name = testSetFile.getName().replace(".txt", "");
        testSet.images = testImages;
        
        return testSet;
	}
	
	/**
	 * Parses one line from test definition file
	 * 
	 * @return null if no test image found
	 */
	private TestImage parseLine(File testImagesDir, String line) throws Exception {
		
		line = line.trim();
		
		if (line.startsWith("#") || line.startsWith("//") || line.isBlank()) {
			return null;
		}
		
		List<Test> tests = new ArrayList<Test>();
		File file = null;
		
		for (String snip : line.split("\\s+")) {
			if (snip.startsWith("#") || snip.startsWith("//")) {
				break;
			} else if (file == null) {
				file = new File(testImagesDir+"//"+snip);
				if (!file.exists()) {
					throw new Exception("Test image file:"+file+" doesn't exist");
				}
			} else {
				try {
					Test test = parseTest(snip);
					test.image = file;
					tests.add(test);
				} catch (Exception e) {
					System.err.println("line:"+line);
					throw e;
				}
			}
		}
		
		if (tests.size() == 0) {
			return null;
		}
		
		TestImage testSet = new TestImage();
		testSet.file = file;
		testSet.tests = tests;
		
		return testSet;
	}
	
	/**
	 * Parses single test. Parameters separated by commas.
	 * 
	 * For example:
	 * 51,325,22,114 -> area test (x, y, width, height)
	 * 35,75,思  -> OCR test (x, y, character)
	 */
	private static Test parseTest(String testStr) throws Exception {
		
		String[] parameters = testStr.split(",");
		if (parameters.length > 4) {
			throw new Exception("Syntax error at:"+testStr+" too many parameters");
		}
		
		List<Integer> ints = new ArrayList<Integer>();
		String characters = "";
		for (String parameter : parameters) {
			try {
				ints.add(new Integer(parameter));
			} catch (NumberFormatException e) {
				characters = parameter;
			}
		}
		
		if (characters.length() == 0) {
			// Area test
			if (ints.size() < 4) {
				throw new Exception("Syntax error at:"+testStr+" too few parameters");
			}
			int x = ints.get(0);
			int y = ints.get(1);
			int width = ints.get(2);
			int height = ints.get(3);
			return new AreaTest(x, y, width, height);
		} else {
			// OCR test
			if (ints.size() < 2) {
				throw new Exception("Syntax error at:"+testStr+" too few parameters");
			}
			int x = ints.get(0);
			int y = ints.get(1);
			return new OCRTest(x, y, characters);
		}
	}
}
