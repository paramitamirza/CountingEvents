

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SemEvalDataParser {
	
	public void writeMateToolsInput(String filepath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line;
		StringBuilder conllString = new StringBuilder();
		
		line = br.readLine();
		List<String> words = new ArrayList<String>();
		String curSent = "";
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				String[] cols = line.split("\t");
				if (curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					if (!cols[1].equals("NEWLINE"))	words.add(cols[1]);
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
				} else if (!curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					conllString.append(MateToolsParser.toConllString(words));
					words.clear();
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
					if (!cols[1].equals("NEWLINE"))	words.add(cols[1]);
				} else {
					if (!cols[1].equals("NEWLINE"))	words.add(cols[1]);
				} 
			} else {
				conllString.append(MateToolsParser.toConllString(words));
				words.clear();
				curSent = "";
			}
			line = br.readLine();
		}
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(filepath + ".mate"));
        bw.write(conllString.toString());
        bw.close();
	}
	
	public void writeSentences(String filepath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String curSent = "";
		String curPart = "";
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				String[] cols = line.split("\t");
				if (curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
					
				} else if (!curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					sentences.append(sentStr);
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
				} 
				
				if (cols[1].equals("NEWLINE"))	sentStr += "\n";
				else if (cols[2].equals("DCT")) sentStr += cols[1];
				else if (!cols[2].equals(curPart)) sentStr += "\n\n" + cols[1] + " ";
				else sentStr += cols[1] + " ";
				
				curPart = cols[2];
				
			} else {
				if (line.startsWith("#begin")) {
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
					sentences.setLength(0);
				
				} else if (line.startsWith("#end")) {
					sentences.append(sentStr);
					BufferedWriter bw = new BufferedWriter(new FileWriter("./data/input/s1/files/" + filename + ".txt"));
			        bw.write(sentStr);
			        bw.close();					
				}
				curSent = "";
				sentStr = "";
			}
			line = br.readLine();
		}
		
//		System.out.println(sentences);
		
//		BufferedWriter bw = new BufferedWriter(new FileWriter(filepath + ".mate"));
//        bw.write(conllString.toString());
//        bw.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		SemEvalDataParser parser = new SemEvalDataParser();
		
		//Parse SemEval CoNLL format into Mate tools input format, one article per file
//		parser.writeMateToolsInput("./data/trial_data_final/input/s1/docs.conll");
		
		//Parse SemEval CoNLL format into sentence (raw text) format, one article per file
		parser.writeSentences("./data/trial_data_final/input/s1/docs.conll");
		
	}

}
