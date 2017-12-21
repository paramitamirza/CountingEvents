

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import it.uniroma1.lcl.babelfy.commons.BabelfyConstraints;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters.MCS;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters.ScoredCandidates;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters.SemanticAnnotationResource;
import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation;
import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation.Source;
import it.uniroma1.lcl.babelfy.commons.annotation.TokenOffsetFragment;
import it.uniroma1.lcl.babelfy.core.Babelfy;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.babelnet.InvalidBabelSynsetIDException;
import it.uniroma1.lcl.babelnet.WordNetSynsetID;
import it.uniroma1.lcl.jlt.util.Language;

public class SemEvalDataParser {
	
	public void writeMateSentences(String filepath) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String curSent = "";
		String curPart = "";
		
        //Run Mate tool: dependency parsing and semantic role labelling
        String mateLemmatizerModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model";
		String mateTaggerModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model";
		String mateParserModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model";
		String mateSrlModel = "./models/CoNLL2009-ST-English-ALL.anna-3.3.srl-4.1.srl.model";
			
		MateToolsParser mateTools = new MateToolsParser(mateLemmatizerModel, mateTaggerModel, mateParserModel, mateSrlModel);
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				String[] cols = line.split("\t");
				if (curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
					
				} else if (!curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
				} 
				
				if (cols[1].equals("NEWLINE"))	sentStr += "\n";
				else if (cols[2].equals("DCT")) sentStr += "";
				else if (!cols[2].equals(curPart)) sentStr += "\n\n" + cols[1] + " ";
				else sentStr += cols[1] + " ";
				
				curPart = cols[2];
				
			} else {
				if (line.startsWith("#begin")) {
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
					sentences.setLength(0);
				
				} else if (line.startsWith("#end")) {
					for (String l : sentStr.split("\n")) {
						if (!l.trim().equals("")) {
							Document doc = new Document(l);
							for (Sentence sss : doc.sentences()) {
								List<String> words = sss.words();
								sentences.append(MateToolsParser.toConllString(words));
							}
						}
					}
					BufferedWriter bw = new BufferedWriter(new FileWriter("./data/mate/s1/files/" + filename + ".txt"));
			        bw.write(sentences.toString());
			        bw.close();	
			        
			        mateTools.runFullPipeline(new File("./data/mate/s1/files/" + filename + ".txt"), new File("./data/mate/s1/output/" + filename + ".srl"));
				}
				curSent = "";
				sentStr = "";
			}
			line = br.readLine();
		}
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
	}
	
	private boolean isIncludedInOffset(String offset1, String offset2) {
		int start1 = Integer.parseInt(offset1.split(",")[0]);
		int end1 = Integer.parseInt(offset1.split(",")[1]);
		int start2 = Integer.parseInt(offset2.split(",")[0]);
		int end2 = Integer.parseInt(offset2.split(",")[1]);
		if (start1 >= start2 && end1 <= end2) return true;
		else return false;
	}
	
	public void writeBabelOutput(String filepath) throws IOException, InvalidBabelSynsetIDException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String curSent = "";
		String curPart = "";
		
		int limit = 1;
		int start = 3;
		int num = 1;
		
		BabelNet bn = BabelNet.getInstance();
		BabelfyConstraints constraints = new BabelfyConstraints();
//		SemanticAnnotation a = new SemanticAnnotation(new TokenOffsetFragment(0, 0), "bn:03083790n",
//		    "http://dbpedia.org/resource/BabelNet", Source.OTHER);
//		constraints.addAnnotatedFragments(a);
		BabelfyParameters bp = new BabelfyParameters();
		bp.setAnnotationResource(SemanticAnnotationResource.BN);
		bp.setMCS(MCS.ON_WITH_STOPWORDS);
//		bp.setScoredCandidates(ScoredCandidates.ALL);
		
		Babelfy bfy = new Babelfy(bp);
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				String[] cols = line.split("\t");
				if (curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
					
				} else if (!curSent.equals("")
						&& !curSent.equals(cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1])) {
					curSent = cols[0].split("\\.")[0] + "." + cols[0].split("\\.")[1];
				} 
				
				if (cols[1].equals("NEWLINE"))	sentStr += "\n";
				else if (cols[2].equals("DCT")) sentStr += "";
				else if (!cols[2].equals(curPart)) sentStr += "\n\n" + cols[1] + " ";
				else sentStr += cols[1] + " ";
				
				curPart = cols[2];
				
			} else {
				if (line.startsWith("#begin")) {
					if (num > (start + limit - 1)) {
						break;
					}
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
					sentences.setLength(0);
				
				} else if (line.startsWith("#end")) {
					for (String l : sentStr.split("\n")) {
						
						if (!l.trim().equals("")) {
							Document doc = new Document(l);
							for (Sentence sss : doc.sentences()) {
//								List<String> words = sss.words();
								sentences.append(sss.text() + "\n");
							}
						}
					}
					
					if (num >= start) {
					
						BufferedWriter bw = new BufferedWriter(new FileWriter("./data/babel/s1/files/" + filename + ".txt"));
				        bw.write(sentences.toString());
				        bw.close();
						
						System.out.println("====" + num + "====");
	//					System.out.println(sentences.toString());
						
						List<SemanticAnnotation> bfyAnnotations = bfy.babelfy(sentences.toString(), Language.EN, constraints);
						//bfyAnnotations is the result of Babelfy.babelfy() call
						List<String> words = new ArrayList<String>();
						List<String> offsets = new ArrayList<String>();
						List<String> wordnetSynsets = new ArrayList<String>();
						List<String> babelSynsets = new ArrayList<String>();
						List<String> babelNetURLs = new ArrayList<String>();
						List<String> DBPediaURLs = new ArrayList<String>();
						
						for (SemanticAnnotation annotation : bfyAnnotations)
						{
						    //splitting the input text using the CharOffsetFragment start and end anchors
						    String frag = sentences.toString().substring(annotation.getCharOffsetFragment().getStart(),
						        annotation.getCharOffsetFragment().getEnd() + 1);
						    words.add(frag);
						    offsets.add(annotation.getCharOffsetFragment().getStart() + "," + (annotation.getCharOffsetFragment().getEnd() + 1));
						    babelSynsets.add(annotation.getBabelSynsetID());
						    babelNetURLs.add(annotation.getBabelNetURL());
						    DBPediaURLs.add(annotation.getDBpediaURL());
						    BabelSynset by = bn.getSynset(new BabelSynsetID(annotation.getBabelSynsetID()));
						    Set<String> wnSynSet = new HashSet<String>();
						    List<WordNetSynsetID> wnSynList = by.getWordNetOffsets();
						    for (WordNetSynsetID syn : wnSynList) {
						    	wnSynSet.add(syn.toString());
						    }					    
						    wordnetSynsets.add(wnSynSet.toString());
						}
						
						String lastOffset = "";
						
						bw = new BufferedWriter(new FileWriter("./data/babel/s1/output/" + filename + ".tsv"));
						for (int n = 0; n < words.size(); n ++) {
							if (n == 0 && n == (words.size()-1)) {	//the only concept
								bw.write(words.get(n)
					        			+ "\t" + offsets.get(n)
					        			+ "\t" + babelSynsets.get(n)
					        			+ "\t" + wordnetSynsets.get(n)
					        			+ "\t" + babelNetURLs.get(n)
					        			+ "\t" + DBPediaURLs.get(n)
					        			+ "\n"
					        			);
			        			lastOffset = offsets.get(n);
								
							} else if (n == 0 && (n+1) < words.size()) {		//first concept
								if (!isIncludedInOffset(offsets.get(n), offsets.get(n+1))) {
				        			bw.write(words.get(n)
						        			+ "\t" + offsets.get(n)
						        			+ "\t" + babelSynsets.get(n)
						        			+ "\t" + wordnetSynsets.get(n)
						        			+ "\t" + babelNetURLs.get(n)
						        			+ "\t" + DBPediaURLs.get(n)
						        			+ "\n"
						        			);
				        			lastOffset = offsets.get(n);
				        		}
								
							} else if (n == (words.size()-1) && (n-1) > 0) {	//last concept
								if (!isIncludedInOffset(offsets.get(n), lastOffset)) {
				        			bw.write(words.get(n)
						        			+ "\t" + offsets.get(n)
						        			+ "\t" + babelSynsets.get(n)
						        			+ "\t" + wordnetSynsets.get(n)
						        			+ "\t" + babelNetURLs.get(n)
						        			+ "\t" + DBPediaURLs.get(n)
						        			+ "\n"
						        			);
				        			lastOffset = offsets.get(n);
				        		}
								
							} else {	//excluding the first concept and the last concept
								if (!isIncludedInOffset(offsets.get(n), offsets.get(n+1))
										&& !isIncludedInOffset(offsets.get(n), lastOffset)) {
				        			bw.write(words.get(n)
						        			+ "\t" + offsets.get(n)
						        			+ "\t" + babelSynsets.get(n)
						        			+ "\t" + wordnetSynsets.get(n)
						        			+ "\t" + babelNetURLs.get(n)
						        			+ "\t" + DBPediaURLs.get(n)
						        			+ "\n"
						        			);
				        			lastOffset = offsets.get(n);
				        		}
				        	} 
						}
						bw.close();
					}
					
					num ++;
				}
				curSent = "";
				sentStr = "";
			}
			line = br.readLine();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		SemEvalDataParser parser = new SemEvalDataParser();
		
		//Parse SemEval CoNLL format into Mate tools input format, one article per file
//		parser.writeMateSentences("./data/trial_data_final/input/s1/docs.conll");
		
		//Parse SemEval CoNLL format into sentence (raw text) format, one article per file
//		parser.writeSentences("./data/trial_data_final/input/s1/docs.conll");
		
		//Parse SemEval CoNLL format into sentence (raw text) format, then run BabelFly on the sentences
		parser.writeBabelOutput("./data/trial_data_final/input/s1/docs.conll");
		
	}

}
