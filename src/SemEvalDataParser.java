

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
	
	public static void main(String[] args) throws Exception {
		
		SemEvalDataParser parser = new SemEvalDataParser();
		parser.writeMateToolsInput("./data/1-81781.conll");
		
	}

}
