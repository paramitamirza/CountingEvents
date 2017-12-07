

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MateToolsParser {

	private String mateLemmatizerModel;
	private String mateTaggerModel;
	private String mateParserModel;
	private String mateSrlModel;
	
	public MateToolsParser() {
		
	}
	
	public MateToolsParser(String mateLemmatizerModelPath, String mateTaggerModelPath, String mateParserModelPath, String mateSrlModelPath) {
		this.setMateLemmatizerModel(mateLemmatizerModelPath);
		this.setMateTaggerModel(mateTaggerModelPath);
		this.setMateParserModel(mateParserModelPath);
		this.setMateSrlModel(mateSrlModelPath);
	}
	
	public void run(File inputFile, File outputFile) throws Exception {
		
		PrintStream originalOutStream = System.out;
		PrintStream originalErrStream = System.err;
		PrintStream dummyStream    = new PrintStream(new OutputStream(){
		    public void write(int b) {
		        //NO-OP
		    }
		});
		System.setOut(dummyStream);
		System.setErr(dummyStream);
		
		String[] lemmatizerArgs = {"-model", this.getMateLemmatizerModel(),
				"-test", inputFile.getPath(),
				"-out", "./data/temp"};
		is2.lemmatizer.Lemmatizer.main(lemmatizerArgs);
		
		String[] taggerArgs = {"-model", this.getMateTaggerModel(),
				"-test", "./data/temp",
				"-out", "./data/temp2"};
		is2.tag.Tagger.main(taggerArgs);
		
		String[] parserArgs = {"-model", this.getMateParserModel(),
				"-test", "./data/temp2",
				"-out", outputFile.getPath()};
		is2.parser.Parser.main(parserArgs);
		
		Files.delete(new File("./data/temp").toPath());
		Files.delete(new File("./data/temp2").toPath());
		
		System.setOut(originalOutStream);
		System.setErr(originalErrStream);
	}
	
	public void runFullPipeline(File inputFile, File outputFile) throws Exception {
		
//		PrintStream originalOutStream = System.out;
//		PrintStream originalErrStream = System.err;
//		PrintStream dummyStream    = new PrintStream(new OutputStream(){
//		    public void write(int b) {
//		        //NO-OP
//		    }
//		});
//		System.setOut(dummyStream);
//		System.setErr(dummyStream);
		
		String[] fullPipelineArgs = {"eng",
				"-test", inputFile.getPath(),
				"-out", outputFile.getPath(),
				"-lemma", this.getMateLemmatizerModel(),
				"-tagger", this.getMateTaggerModel(),
				"-parser", this.getMateParserModel(),
				"-srl", this.getMateSrlModel()
				};
		se.lth.cs.srl.CompletePipeline.main(fullPipelineArgs);
		
//		System.setOut(originalOutStream);
//		System.setErr(originalErrStream);
	}
	
	public List<String> runFullPipeline(File inputFile) throws Exception {
		List<String> result = new ArrayList<String>();
		
		runFullPipeline (inputFile, new File(inputFile.getPath() + ".dep"));
		
		Scanner fileScanner = new Scanner(new File(inputFile.getPath() + ".dep"));
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    result.add(next);
		}
		fileScanner.close();
		
		Files.delete(new File(inputFile + ".dep").toPath());
		
		return result;
	}
	
	public List<String> run(File inputFile) throws Exception {
		List<String> result = new ArrayList<String>();
		
		run (inputFile, new File(inputFile.getPath() + ".dep"));
		
		Scanner fileScanner = new Scanner(new File(inputFile.getPath() + ".dep"));
		while(fileScanner.hasNextLine()) {
		    String next = fileScanner.nextLine();
		    result.add(next);
		}
		fileScanner.close();
		
		Files.delete(new File(inputFile + ".dep").toPath());
		
		return result;
	}
	
	public static void main(String[] args) {
		
		String mateLemmatizerModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model";
		String mateTaggerModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model";
		String mateParserModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model";
		String mateSrlModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.srl-4.1.srl.model";
		
		try {
			
			MateToolsParser mateTools = new MateToolsParser(mateLemmatizerModel, mateTaggerModel, mateParserModel, mateSrlModel);
//			List<String> mateToolsColumns = mateTools.run(new File("./data/example_CoNLL/wsj_1014.conll"));
			List<String> mateToolsColumns = mateTools.runFullPipeline(new File("./data/1-81781.mate.conll"));
			
			BufferedWriter bw = new BufferedWriter(new FileWriter("./data/1-81781.mate.srl"));
	        for (String s : mateToolsColumns) bw.write(s + "\n");
	        bw.close();
			for (String s : mateToolsColumns) System.out.println(s);
			
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String toConllString(List<String> words) throws IOException {
		String conll = "";
		int idx = 1;
		for (String s : words) {
			conll += idx + "\t" + s;
			for (int i = 0; i < 13; i ++) {
				conll += "\t_";
			}
			conll += "\n";
			idx ++;
		}
		conll += "\n";
		
		return conll;
	}

	public String getMateLemmatizerModel() {
		return mateLemmatizerModel;
	}

	public void setMateLemmatizerModel(String mateLemmatizerModel) {
		this.mateLemmatizerModel = mateLemmatizerModel;
	}

	public String getMateTaggerModel() {
		return mateTaggerModel;
	}

	public void setMateTaggerModel(String mateTaggerModel) {
		this.mateTaggerModel = mateTaggerModel;
	}

	public String getMateParserModel() {
		return mateParserModel;
	}

	public void setMateParserModel(String mateParserModel) {
		this.mateParserModel = mateParserModel;
	}

	public String getMateSrlModel() {
		return mateSrlModel;
	}

	public void setMateSrlModel(String mateSrlModel) {
		this.mateSrlModel = mateSrlModel;
	}
}
