

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import it.uniroma1.lcl.babelfy.commons.BabelfyConstraints;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters.MCS;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters.ScoredCandidates;
import it.uniroma1.lcl.babelfy.commons.BabelfyParameters.SemanticAnnotationResource;
import it.uniroma1.lcl.babelfy.commons.BabelfyToken;
import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation;
import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation.Source;
import it.uniroma1.lcl.babelfy.commons.annotation.TokenOffsetFragment;
import it.uniroma1.lcl.babelfy.core.Babelfy;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSense;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.babelnet.BabelSynsetIDRelation;
import it.uniroma1.lcl.babelnet.InvalidBabelSynsetIDException;
import it.uniroma1.lcl.babelnet.WordNetSynsetID;
import it.uniroma1.lcl.jlt.util.Language;

public class SemEvalDataParser {
	
	private static int countKilling = 0;
	
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
	
	public void writeSentences(String filepath, String outputdir) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
//		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String prevSent = "", currSent = "";
		String word = "";
		
		File directory = new File(outputdir);
	    if (!directory.exists()){
	        directory.mkdirs();
	    }
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				
				String[] cols = line.split("\t");
				currSent = cols[0].split("\\.")[1];
				
				if (currSent.equals("DCT")) {
					sentStr += cols[1];
					prevSent = currSent;
				} else {
					word = cols[1];
					word = word.replaceAll("\\s", "");
					word = word.replaceAll(" ", "");
					
					if (word.isEmpty()) word = "EMPTY";
					else if (word.equals("  ") || word.equals(" ")) word = "EMPTY";
					
					if (currSent.equals(prevSent)) {
						if (word.equals("NEWLINE")) {
							sentStr += "\n";
						} else {
							sentStr += " " + word;
						}
					} else {
						sentStr += "\n" + word;
						prevSent = currSent;						
					}
				}
				
			} else {
				if (line.startsWith("#begin")) {
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
//					sentences.setLength(0);
				
				} else if (line.startsWith("#end")) {
//					sentences.append(sentStr);
					BufferedWriter bw = new BufferedWriter(new FileWriter(outputdir + "/" + filename + ".txt"));
			        bw.write(sentStr);
			        bw.close();					
				}
				currSent = "";
				prevSent = "";
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
	
	public void writeBabelOutput(String filepath, String outputdir) throws IOException, InvalidBabelSynsetIDException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String curSent = "";
		String curPart = "";
		String word = "";
		
		int limit = 1;
		int start = 1;
		int num = 1;
		
		File directory = new File(outputdir);
	    if (!directory.exists()){
	        directory.mkdirs();
	    }
		
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
				
				word = cols[1];
				word = word.replaceAll("\\s", "");
				word = word.replaceAll(" ", "");
				
				if (word.isEmpty()) word = "EMPTY";
				else if (word.equals("  ") || word.equals(" ")) word = "EMPTY";
				
				if (cols[2].equals("DCT")) sentStr += word;
				else if (!cols[2].equals(curPart)) sentStr += "\n" + word;
				else sentStr += " " + word;
				
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
					
					if (num >= start) {
						
						System.out.println("====" + num + "====");
						System.out.println(sentStr);
						
						sentStr = sentStr.trim();
						List<BabelfyToken> tokenizedInput = new ArrayList<>();
						for (String l : sentStr.split("\n")) {
							for (String tok : l.split(" ")) {
								tokenizedInput.add(new BabelfyToken(tok, Language.EN));
							}
						}
						
						List<SemanticAnnotation> bfyAnnotations = bfy.babelfy(tokenizedInput, Language.EN, constraints);
						//bfyAnnotations is the result of Babelfy.babelfy() call
						List<String> words = new ArrayList<String>();
						List<String> offsets = new ArrayList<String>();
						List<String> wordnetSynsets = new ArrayList<String>();
						List<String> babelTypes = new ArrayList<String>();
						List<String> babelSynsets = new ArrayList<String>();
						List<String> babelNetURLs = new ArrayList<String>();
						List<String> DBPediaURLs = new ArrayList<String>();
						List<String> babelMainSenses = new ArrayList<String>();
						
						for (SemanticAnnotation annotation : bfyAnnotations)
						{
						    //splitting the input text using the CharOffsetFragment start and end anchors
//						    String frag = sentStr.substring(annotation.getCharOffsetFragment().getStart(),
//						        annotation.getCharOffsetFragment().getEnd() + 1);
							
							//get the word from tokenized input text
							String frag = "";
							for (int t = annotation.getTokenOffsetFragment().getStart(); 
									t < (annotation.getTokenOffsetFragment().getEnd() + 1);
									t ++) {
								frag += " " + tokenizedInput.get(t).getWord();
							}
							frag = frag.substring(1);
							
							words.add(frag);
							
						    offsets.add(annotation.getTokenOffsetFragment().getStart() + "," + (annotation.getTokenOffsetFragment().getEnd() + 1));
						    babelSynsets.add(annotation.getBabelSynsetID());
						    babelNetURLs.add(annotation.getBabelNetURL());
						    DBPediaURLs.add(annotation.getDBpediaURL());
						    
						    BabelSynset by = bn.getSynset(new BabelSynsetID(annotation.getBabelSynsetID()));
						    babelTypes.add(by.getSynsetType().toString());
						    Set<String> wnSynSet = new HashSet<String>();
						    List<WordNetSynsetID> wnSynList = by.getWordNetOffsets();
						    for (WordNetSynsetID syn : wnSynList) {
						    	wnSynSet.add(syn.toString());
						    }					    
						    wordnetSynsets.add(wnSynSet.toString());
						    babelMainSenses.add(by.getMainSense(Language.EN).getLemma());
						}
						
						String lastOffset = "";
						
						BufferedWriter bw = new BufferedWriter(new FileWriter(outputdir + "/" + filename + ".tsv"));
						for (int n = 0; n < words.size(); n ++) {
							if (n == 0 && n == (words.size()-1)) {	//the only concept
								bw.write(words.get(n)
					        			+ "\t" + offsets.get(n)
					        			+ "\t" + babelSynsets.get(n)
					        			+ "\t" + babelTypes.get(n)
					        			+ "\t" + wordnetSynsets.get(n)
					        			+ "\t" + babelNetURLs.get(n)
					        			+ "\t" + DBPediaURLs.get(n)
					        			+ "\t" + babelMainSenses.get(n)
					        			+ "\n"
					        			);
			        			lastOffset = offsets.get(n);
								
							} else if (n == 0 && (n+1) < words.size()) {		//first concept
								if (!isIncludedInOffset(offsets.get(n), offsets.get(n+1))) {
				        			bw.write(words.get(n)
				        					+ "\t" + offsets.get(n)
						        			+ "\t" + babelSynsets.get(n)
						        			+ "\t" + babelTypes.get(n)
						        			+ "\t" + wordnetSynsets.get(n)
						        			+ "\t" + babelNetURLs.get(n)
						        			+ "\t" + DBPediaURLs.get(n)
						        			+ "\t" + babelMainSenses.get(n)
						        			+ "\n"
						        			);
				        			lastOffset = offsets.get(n);
				        		}
								
							} else if (n == (words.size()-1) && (n-1) > 0) {	//last concept
								if (((!lastOffset.equals("") && !isIncludedInOffset(offsets.get(n), lastOffset)))
										|| lastOffset.equals("")
											) {
				        			bw.write(words.get(n)
				        					+ "\t" + offsets.get(n)
						        			+ "\t" + babelSynsets.get(n)
						        			+ "\t" + babelTypes.get(n)
						        			+ "\t" + wordnetSynsets.get(n)
						        			+ "\t" + babelNetURLs.get(n)
						        			+ "\t" + DBPediaURLs.get(n)
						        			+ "\t" + babelMainSenses.get(n)
						        			+ "\n"
						        			);
				        			lastOffset = offsets.get(n);
				        		}
								
							} else {	//excluding the first concept and the last concept
								
								if (!isIncludedInOffset(offsets.get(n), offsets.get(n+1))
										&& (
											((!lastOffset.equals("") && !isIncludedInOffset(offsets.get(n), lastOffset)))
											|| lastOffset.equals("")
												)
										) {
				        			bw.write(words.get(n)
				        					+ "\t" + offsets.get(n)
						        			+ "\t" + babelSynsets.get(n)
						        			+ "\t" + babelTypes.get(n)
						        			+ "\t" + wordnetSynsets.get(n)
						        			+ "\t" + babelNetURLs.get(n)
						        			+ "\t" + DBPediaURLs.get(n)
						        			+ "\t" + babelMainSenses.get(n)
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
	
	public void getTopicsFromTitle(String filepath) throws IOException, InvalidBabelSynsetIDException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String curSent = "";
		String curPart = "";
		String dctStr = "";
		
		int limit = 1100;
		int start = 1;
		int num = 1;
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("./data/babel/s1/topic_per_title.tsv"));
		
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
				
//				if (cols[1].equals("NEWLINE"))	sentStr += "\n";
//				else if (cols[2].equals("DCT")) sentStr += cols[1];
//				else if (!cols[2].equals(curPart)) sentStr += "\n\n" + cols[1] + " ";
//				else sentStr += cols[1] + " ";
				
				if (cols[2].equals("TITLE")) sentStr += cols[1] + " ";
				else if (cols[2].equals("DCT")) dctStr = cols[1];
				
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
//					for (String l : sentStr.split("\n")) {
//						
//						if (!l.trim().equals("")) {
//							Document doc = new Document(l);
//							for (Sentence sss : doc.sentences()) {
////								List<String> words = sss.words();
//								sentences.append(sss.text() + "\n");
//							}
//						}
//					}
					
					if (num >= start) {
					
						sentStr = sentStr.trim();
						
//						System.out.println("====" + num + "====");
						
						BufferedReader brr = new BufferedReader(new FileReader("./data/babel/s1/title/" + filename + ".tsv"));
						String linee = brr.readLine();
						
						boolean injuring = false, killing = false;
						
						String vInjure = "00069879-v";
						String nInjury = "14285662-n";
						String aInjured = "01317954-a";
						
						String vKill = "01323958-v";
						String vDie = "00358431-v";
						String nKilling = "00219012-n";
						String aDead = "00095280-a";
						String aDeadly = "00993667-a";
						String rFatally = "00506577-r";
						
						double threshold = 4.0;
			
						while (linee != null) {
							
							double killingRelScore = 0.0;
							double injuringRelScore = 0.0;
							
							String[] cols = linee.split("\t");
							String wnSyn = cols[3];
							wnSyn = wnSyn.substring(1, wnSyn.length()-1);	// remove []
							if (!wnSyn.equals("")) {
								wnSyn = wnSyn.split(",")[0].trim();
								char wnPos = wnSyn.charAt(wnSyn.length()-1);
								wnSyn = wnSyn.substring(3, wnSyn.length()-1) + "-" + wnPos;	//remove prefix 'wn:' and add '-' before pos
								
								SemanticRelatedness rel, rel2;
								if (wnPos == 'a') {
									rel = new SemanticRelatedness(wnSyn, aInjured);
									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, aDead);
									rel2 = new SemanticRelatedness(wnSyn, aDeadly);
									killingRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
									
								} else if (wnPos == 'v') {
									rel = new SemanticRelatedness(wnSyn, vInjure);
									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, vKill);
									rel2 = new SemanticRelatedness(wnSyn, vDie);
									killingRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
									
								} else if (wnPos == 'n') {
									rel = new SemanticRelatedness(wnSyn, nInjury);
									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, nKilling);
									killingRelScore = rel.getHsoScore();
								
								} else if (wnPos == 'r') {
//									rel = new SemanticRelatedness(wnSyn, nInjury);
//									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, rFatally);
									killingRelScore = rel.getHsoScore();
								}
								
								if (injuringRelScore >= threshold) injuring = true;
								if (killingRelScore >= threshold) killing = true;
							}	
//							System.out.println(cols[0] + "\t" + wnSyn + "\t" + injuringRelScore + "\t" + killingRelScore);	//logging purpose
									
							linee = brr.readLine();
						}
						
						brr.close();
						
//						System.out.println(filename + "\t" + injuring + "\t" + killing + "\t" + sentStr.trim());
						bw.write(filename + "\t" + dctStr + "\t" + injuring + "\t" + killing + "\t" + sentStr.trim() + "\n");
						
					}
					
					num ++;
				}
				curSent = "";
				sentStr = "";
			}
			line = br.readLine();
			
		}
		
		bw.close();
	}
	
	public void getTopicsFromArticle(String filepath, String babeldir, String topicfile) throws IOException, InvalidBabelSynsetIDException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "";
		StringBuilder sentences = new StringBuilder();
		
		line = br.readLine();
		String sentStr = "";
		String curSent = "";
		String curPart = "";
		String dctStr = "";
		
		int limit = 10000;
		int start = 247;
		int num = 1;
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(topicfile, true));
		
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
				
//				if (cols[1].equals("NEWLINE"))	sentStr += "\n";
//				else if (cols[2].equals("DCT")) sentStr += cols[1];
//				else if (!cols[2].equals(curPart)) sentStr += "\n\n" + cols[1] + " ";
//				else sentStr += cols[1] + " ";
				
				if (cols[2].equals("TITLE")) sentStr += cols[1] + " ";
				else if (cols[2].equals("DCT")) dctStr = cols[1];
				
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
//					for (String l : sentStr.split("\n")) {
//						
//						if (!l.trim().equals("")) {
//							Document doc = new Document(l);
//							for (Sentence sss : doc.sentences()) {
////								List<String> words = sss.words();
//								sentences.append(sss.text() + "\n");
//							}
//						}
//					}
					
					if (num >= start) {
					
						sentStr = sentStr.trim();
						
						System.out.println("====" + num + "====");
						
						BufferedReader brr = new BufferedReader(new FileReader(babeldir + filename + ".tsv"));
						String linee = brr.readLine();
						
						boolean injuring = false, killing = false, fire = false, displace = false;
						
						String vInjure = "00069879-v";
						String nInjury = "14285662-n";
						String aInjured = "01317954-a";
						
						String vKill = "01323958-v";
						String vDie = "00358431-v";
						String nKilling = "00219012-n";
						String aDead = "00095280-a";
						String aDeadly = "00993667-a";
						String rFatally = "00506577-r";
						
						String nFire = "07302836-n";
						
						String vDisplace = "02402825-v";
						
						double threshold = 4.0;
			
						while (linee != null) {
							
							double killingRelScore = 0.0;
							double injuringRelScore = 0.0;
							double fireRelScore = 0.0;
							double displaceRelScore = 0.0;
							
							String[] cols = linee.split("\t");
							String wnSyn = cols[4];
							wnSyn = wnSyn.substring(1, wnSyn.length()-1);	// remove []
							
							if (!wnSyn.equals("")) {
								wnSyn = wnSyn.split(",")[0].trim();
								char wnPos = wnSyn.charAt(wnSyn.length()-1);
								wnSyn = wnSyn.substring(3, wnSyn.length()-1) + "-" + wnPos;	//remove prefix 'wn:' and add '-' before pos
								
								SemanticRelatedness rel, rel2;
								if (wnPos == 'a') {
									rel = new SemanticRelatedness(wnSyn, aInjured);
									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, aDead);
									rel2 = new SemanticRelatedness(wnSyn, aDeadly);
									killingRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
									
								} else if (wnPos == 'v') {
									rel = new SemanticRelatedness(wnSyn, vInjure);
									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, vKill);
									rel2 = new SemanticRelatedness(wnSyn, vDie);
									killingRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
									
									rel = new SemanticRelatedness(wnSyn, vDisplace);
									displaceRelScore = rel.getHsoScore();
									
								} else if (wnPos == 'n') {
									rel = new SemanticRelatedness(wnSyn, nInjury);
									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, nKilling);
									killingRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, nFire);
									fireRelScore = rel.getHsoScore();
								
								} else if (wnPos == 'r') {
//									rel = new SemanticRelatedness(wnSyn, nInjury);
//									injuringRelScore = rel.getHsoScore();
									
									rel = new SemanticRelatedness(wnSyn, rFatally);
									killingRelScore = rel.getHsoScore();
								}
								
								if (injuringRelScore >= threshold) injuring = true;
								if (killingRelScore >= threshold) killing = true;
								if (fireRelScore >= threshold) fire = true;
								if (displaceRelScore >= threshold) displace = true;
							}	
//							System.out.println(cols[0] + "\t" + wnSyn + "\t" + injuringRelScore + "\t" + killingRelScore);	//logging purpose
									
							linee = brr.readLine();
						}
						
						brr.close();
						
//						System.out.println(filename + "\t" + injuring + "\t" + killing + "\t" + sentStr.trim());
						bw.write(filename + "\t" + dctStr + "\t" + injuring + "\t" + killing + "\t" + fire + "\t" + displace + "\n");
						
					}
					
					num ++;
				}
				curSent = "";
				sentStr = "";
			}
			line = br.readLine();
			
		}
		
		bw.close();
	}
	
	private Map<String, Map<Integer, Set<Span>>> extractBabelEvents(String babelfile, Map<Integer, Integer> sentMapping, 
			String topicfile, Map<String, List<Double>> wnSenseSims, Map<String, List<Double>> babelSenseSims) throws IOException {
		Map<String, Map<Integer, Set<Span>>> events = new HashMap<String, Map<Integer, Set<Span>>>();
		
		events.put("injuring", new HashMap<Integer, Set<Span>>());
		events.put("killing", new HashMap<Integer, Set<Span>>());
		events.put("fire", new HashMap<Integer, Set<Span>>());
		events.put("displace", new HashMap<Integer, Set<Span>>());
		
		events.put("dbpedia_ent", new HashMap<Integer, Set<Span>>());
		
		BufferedReader brr = new BufferedReader(new FileReader(babelfile));
		String linee = brr.readLine();
		
		double threshold = 0.25;

		while (linee != null) {
			
			double killingRelScore = 0.0;
			double injuringRelScore = 0.0;
			double fireRelScore = 0.0;
			double displaceRelScore = 0.0;
			
			String[] cols = linee.split("\t");
			
//			System.out.println(linee);
			
			Span s = new Span(cols[0], Integer.parseInt(cols[1].split(",")[0]), Integer.parseInt(cols[1].split(",")[1]));
			Integer sentIdx = sentMapping.get(s.getStartOffset()); s.setSentIndex(sentIdx);
			
			String babelSyn = cols[2];
			
//			String wnSyn = cols[4];
//			wnSyn = wnSyn.substring(1, wnSyn.length()-1);	// remove []
			if (!cols[3].equals("Named Entity")) {
//				wnSyn = wnSyn.split(",")[0].trim();
//				char wnPos = wnSyn.charAt(wnSyn.length()-1);
//				wnSyn = wnSyn.substring(3, wnSyn.length()-1) + "-" + wnPos;	//remove prefix 'wn:' and add '-' before pos
				
				if (wnSenseSims.containsKey(babelSyn)
						|| cols[2].equals("bn:00191195n")	//gunshot
						|| cols[2].equals("bn:00082264v")	//hurt
						) {
					
					if (!cols[2].equals("bn:00191195n")
							&& !cols[2].equals("bn:00082264v")) {
						injuringRelScore = wnSenseSims.get(babelSyn).get(0);
						killingRelScore = wnSenseSims.get(babelSyn).get(1);
						fireRelScore = wnSenseSims.get(babelSyn).get(2);
						displaceRelScore = wnSenseSims.get(babelSyn).get(3);
					}
					
					if (injuringRelScore > 5.0
							|| cols[2].equals("bn:00089349v")
							|| cols[2].equals("bn:00191195n")
							|| cols[2].equals("bn:00082264v")
							) injuringRelScore = 1.0; else injuringRelScore = 0.0;
					if (killingRelScore > 5.0 
							&& !cols[0].startsWith("victim")
							) killingRelScore = 1.0; else killingRelScore = 0.0;
					if (fireRelScore > 5.0 
							&& !cols[2].equals("bn:00043011n")
							&& !cols[2].equals("bn:00032021n")
							) fireRelScore = 1.0; else fireRelScore = 0.0;
					if (displaceRelScore >= 4.0) displaceRelScore = 1.0; else displaceRelScore = 0.0;
				}
				
//				if (babelSenseSims.containsKey(babelSyn)) {
//					injuringRelScore += babelSenseSims.get(babelSyn).get(0);
//					killingRelScore += babelSenseSims.get(babelSyn).get(1);
//					fireRelScore += babelSenseSims.get(babelSyn).get(2);
//					displaceRelScore += babelSenseSims.get(babelSyn).get(3);
//					
//					injuringRelScore = injuringRelScore / 2;
//					killingRelScore = killingRelScore / 2;
//					fireRelScore = fireRelScore / 2;
//					displaceRelScore = displaceRelScore / 2;
//				}
				
				if (injuringRelScore > threshold) {
					if (!events.get("injuring").containsKey(sentIdx)) events.get("injuring").put(sentIdx, new HashSet<Span>());
					events.get("injuring").get(sentIdx).add(s);
				}
				if (killingRelScore > threshold) {
					if (!events.get("killing").containsKey(sentIdx)) events.get("killing").put(sentIdx, new HashSet<Span>());
					events.get("killing").get(sentIdx).add(s);
				}
				if (fireRelScore > threshold) {
					if (!events.get("fire").containsKey(sentIdx)) events.get("fire").put(sentIdx, new HashSet<Span>());
					events.get("fire").get(sentIdx).add(s);
				}
				if (displaceRelScore > threshold) {
					if (!events.get("displace").containsKey(sentIdx)) events.get("displace").put(sentIdx, new HashSet<Span>());
					events.get("displace").get(sentIdx).add(s);
				}
			
			} 
//			System.out.println(cols[0] + "\t" + babelSyn + "\t" + injuringRelScore + "\t" + killingRelScore + "\t" + fireRelScore + "\t" + displaceRelScore);	//logging purpose
			
			if (!cols[6].equals("null")
					&& cols[3].equals("Named Entity")) {
				s.setDbpediaUrl(cols[6]);
				if (!events.get("dbpedia_ent").containsKey(sentIdx)) events.get("dbpedia_ent").put(sentIdx, new HashSet<Span>());
				events.get("dbpedia_ent").get(sentIdx).add(s);
			}
					
			linee = brr.readLine();
		}
		
		brr.close();
		
		return events;
	}
	
	private List<String> extractWordList(String sentencefile) throws IOException {
		List<String> words = new ArrayList<String>();
		
		BufferedReader brr = new BufferedReader(new FileReader(sentencefile));
		String linee = brr.readLine();
		while (linee != null) {
			if (!linee.trim().equals("")) {
				String[] cols = linee.split(" ");
				for (String w : cols) {
					words.add(w);
				}
			} 
			
			linee = brr.readLine();
		}
		
		return words;
	}
	
	private Map<Integer, Integer> extractSentenceMapping(String sennafile, List<String> words) throws IOException {
		Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();
		
		int wIdx = 0, sentIdx = 0;
		
		BufferedReader brr = new BufferedReader(new FileReader(sennafile));
		String linee = brr.readLine();
		while (linee != null) {
			if (!linee.trim().equals("")) {
				if (wIdx < words.size() && words.get(wIdx).equals("NEWLINE")) {
					mapping.put(wIdx, sentIdx);
					wIdx ++;
				}
//				System.out.println(wIdx + "---" + words.get(wIdx) + "---" + linee.trim());
				mapping.put(wIdx, sentIdx);
				wIdx ++;
				
			} else {
				sentIdx ++;
				if (wIdx < words.size() && words.get(wIdx).equals("NEWLINE")) {
					mapping.put(wIdx, sentIdx);
					wIdx ++;
				}
			}
			
			linee = brr.readLine();
		}
		if (wIdx < words.size() && words.get(wIdx).equals("NEWLINE")) {
			mapping.put(wIdx, sentIdx);
			wIdx ++;
		}
		
		return mapping;
	}
	
	private Set<Predicate> extractSennaPredicates(String sennafile, List<String> words) throws IOException {
		Set<Predicate> allPredicates = new HashSet<Predicate>();
		
		BufferedReader brr = new BufferedReader(new FileReader(sennafile));
		int wIdx = 0, numpred = 0;
		String word, pos, chunk, ner, ispred, tree;
		
		List<Predicate> predicates = new ArrayList<Predicate>();
		List<Span> spans = new ArrayList<Span>();
		List<Span> persons = new ArrayList<Span>();
		Span currPerson = new Span();
		boolean firstLine = true;
		int sentIdx = 0;
		
		String linee = brr.readLine();
		while (linee != null) {
			if (!linee.trim().equals("")) {
//				System.out.println(wIdx + "---" + linee);
				
				String[] cols = linee.split("\t");
				word = cols[0].trim();
				pos = cols[1].trim();
				chunk = cols[2].trim();
				ner = cols[3].trim();
				ispred = cols[4].trim();
				tree = cols[cols.length-1].trim();
				numpred = cols.length - 6;
				
				if (wIdx < words.size() && words.get(wIdx).equals("NEWLINE")) wIdx ++;
				
				if (firstLine) {		//what to do at the first line of sentence
					
					if (numpred > 0) {
						for (int k=0; k<numpred; k++) {
							String srl = cols[k+5].trim();
							Span s = new Span();
							Predicate p = new Predicate();
							
							if (srl.startsWith("B")) {
								s.setSentIndex(sentIdx);
								s.setStartOffset(wIdx);
								s.setEndOffset(wIdx + 1);
								s.setText(word);
								s.setType(srl.substring(2));
							
							} else if (srl.startsWith("S")) {
								s.setSentIndex(sentIdx);
								s.setStartOffset(wIdx);
								s.setEndOffset(wIdx + 1);
								s.setText(word);
								s.setType(srl.substring(2));
								
								if (s.getType().equals("V")) {								
									Span sss = new Span(s.getText(),
											s.getSentIndex(),
											s.getStartOffset(),
											s.getEndOffset(),
											s.getType());
									
									p.setPredicate(sss);
								
								} else {
									Span sss = new Span(s.getText(),
											s.getSentIndex(),
											s.getStartOffset(),
											s.getEndOffset(),
											s.getType());
									
									p.getArguments().add(sss);
								}
							}
							
							spans.add(s);
							predicates.add(p);
						}
					}
					
					if (cols[3].trim().equals("B-PER")) {
						currPerson = new Span();
						currPerson.setSentIndex(sentIdx);
						currPerson.setStartOffset(wIdx);
						currPerson.setEndOffset(wIdx + 1);
						currPerson.setText(word);
					
					} else if (cols[3].trim().equals("S-PER")) {
						currPerson = new Span();
						currPerson.setSentIndex(sentIdx);
						currPerson.setStartOffset(wIdx);
						currPerson.setEndOffset(wIdx + 1);
						currPerson.setText(word);
						persons.add(currPerson);
					}
					
					firstLine = false;
					
				} else {
					for (int k=0; k<numpred; k++) {
						
						String srl = cols[k+5].trim();
						
						if (srl.startsWith("B")) {
							spans.get(k).setSentIndex(sentIdx);
							spans.get(k).setStartOffset(wIdx);
							spans.get(k).setEndOffset(wIdx + 1);
							spans.get(k).setText(word);
							spans.get(k).setType(srl.substring(2));
							
						} else if (srl.startsWith("I")) {
							spans.get(k).setEndOffset(wIdx + 1);
							spans.get(k).setText(spans.get(k).getText() + " " + word);
							spans.get(k).setType(srl.substring(2));
						
						} else if (srl.startsWith("E")) {
							spans.get(k).setEndOffset(wIdx + 1);
							spans.get(k).setText(spans.get(k).getText() + " " + word);
							spans.get(k).setType(srl.substring(2));
							
							if (spans.get(k).getType().equals("V")) {								
								Span sss = new Span(spans.get(k).getText(),
										spans.get(k).getSentIndex(),
										spans.get(k).getStartOffset(),
										spans.get(k).getEndOffset(),
										spans.get(k).getType());
								
								Predicate ppp = predicates.get(k);
								ppp.setPredicate(sss);
							
							} else {
								Span sss = new Span(spans.get(k).getText(),
										spans.get(k).getSentIndex(),
										spans.get(k).getStartOffset(),
										spans.get(k).getEndOffset(),
										spans.get(k).getType());
								
								Predicate ppp = predicates.get(k);
								ppp.getArguments().add(sss);
							}
							
							spans.set(k, new Span());
						
						} else if (srl.startsWith("S")) {
							spans.get(k).setSentIndex(sentIdx);
							spans.get(k).setStartOffset(wIdx);
							spans.get(k).setEndOffset(wIdx + 1);
							spans.get(k).setText(word);
							spans.get(k).setType(srl.substring(2));
							
							if (spans.get(k).getType().equals("V")) {								
								Span sss = new Span(spans.get(k).getText(),
										spans.get(k).getSentIndex(),
										spans.get(k).getStartOffset(),
										spans.get(k).getEndOffset(),
										spans.get(k).getType());
								
								Predicate ppp = predicates.get(k);
								ppp.setPredicate(sss);
							
							} else {
								Span sss = new Span(spans.get(k).getText(),
										spans.get(k).getSentIndex(),
										spans.get(k).getStartOffset(),
										spans.get(k).getEndOffset(),
										spans.get(k).getType());
								
								Predicate ppp = predicates.get(k);
								ppp.getArguments().add(sss);
							}
							
							spans.set(k, new Span());
						}
					}
					
					if (cols[3].trim().equals("B-PER")) {
						currPerson = new Span();
						currPerson.setSentIndex(sentIdx);
						currPerson.setStartOffset(wIdx);
						currPerson.setEndOffset(wIdx + 1);
						currPerson.setText(word);
					
					} else if (cols[3].trim().equals("S-PER")) {
						currPerson = new Span();
						currPerson.setSentIndex(sentIdx);
						currPerson.setStartOffset(wIdx);
						currPerson.setEndOffset(wIdx + 1);
						currPerson.setText(word);
						persons.add(currPerson);
					
					} else if (cols[3].trim().equals("I-PER")) {
						currPerson.setEndOffset(wIdx + 1);
						currPerson.setText(currPerson.getText() + " " + word);
					
					} else if (cols[3].trim().equals("E-PER")) {
						currPerson.setEndOffset(wIdx + 1);
						currPerson.setText(currPerson.getText() + " " + word);
						persons.add(currPerson);
					}
				}							
				
				wIdx ++;
				
			} else {
				firstLine = true;
				sentIdx ++;
				if (wIdx < words.size() && words.get(wIdx).equals("NEWLINE")) wIdx ++;
				
				for (Predicate p : predicates) {
					List<Span> args = p.getArguments();
					for (Span person : persons) {
						for (Span s : args) {
							if ((s.getType().equals("A0") || s.getType().equals("A1"))
									&& person.isIncludedIn(s)) {
								person.setType(s.getType());
								p.getPersons().add(person);
							}
						}
					}
				}
				
				allPredicates.addAll(predicates);
				
				spans.clear();
				predicates.clear();
				persons.clear();
				
			}
			linee = brr.readLine();
		}
		
		brr.close();
		
		return allPredicates;
	}
	
	private Long containsNumber(String str) {
		long num = -999;
		
		for (String s : Numbers.digits) {
			if (str.startsWith(s + " ")
//					|| str.endsWith(" " + s)	//doesn't make any sense for cardinality
					|| str.contains(" " + s + " ")
					|| str.equals(s)) {
				return Numbers.getInteger(s);
			}
		}
		
		return num;
	}
	
	public void combineSennaBabelOutput(String filepath, String sentencedir, String babeldir, String sennadir, String topicfile, 
			int startDoc, String questionpath, int subtask) throws IOException, JSONException, URISyntaxException {
		
		String line, filename = "";
//		StringBuilder sentences = new StringBuilder();
		
		int limit = 10000;
		int start = startDoc;
		int num = 1;
		int predcol = 0;
		
		String dctStr = "";
		
		//////////////// Read file containing WordNet similarity of concepts to event type, saved in wnSenseSims ////////////////
		Map<String, List<Double>> wnSenseSims = new HashMap<String, List<Double>>();
		BufferedReader brr = new BufferedReader(new FileReader(topicfile.replace("_v1.tsv", "_wordnet.tsv")));
		line = brr.readLine();
		while (line != null) {
			String[] cols = line.split("\t");
			String val = cols[2] + "," + cols[3] + "," + cols[4] + "," + cols[5];
			if (!val.equals("0.0,0.0,0.0,0.0")) {
				double[] arr = Stream.of(val.split(","))
	                     .mapToDouble (Double::parseDouble)
	                     .toArray();
				wnSenseSims.put(cols[1], Arrays.asList(ArrayUtils.toObject(arr)));
			}
			line = brr.readLine();
		}
		brr.close();
		////////////////DONE!
		
		////////////////Read file containing sense embedding similarity of concepts to event type, saved in babelSenseSims ////////////////
		Map<String, List<Double>> babelSenseSims = new HashMap<String, List<Double>>();
		brr = new BufferedReader(new FileReader(topicfile.replace("_v1.tsv", "_babelnet_sim.tsv")));
		line = brr.readLine();
		while (line != null) {
			String[] cols = line.split("\t");
			String val = cols[6] + "," + cols[7] + "," + cols[8] + "," + cols[9];
			if (!val.equals("0.0,0.0,0.0,0.0")) {
				double[] arr = Stream.of(val.split(","))
	                     .mapToDouble (Double::parseDouble)
	                     .toArray();
				babelSenseSims.put(cols[1], Arrays.asList(ArrayUtils.toObject(arr)));
			}
			line = brr.readLine();
		}
		brr.close();
		////////////////DONE!
		
		
		////////////////Read file containing location (city, state) per document, saved in mapLocation ////////////////
		Map<String, String> mapLocation = new HashMap<String, String>();
		brr = new BufferedReader(new FileReader(topicfile.replace("topics_v1.tsv", "location_output_all.tsv")));
		line = brr.readLine();
		while (line != null) {
			String[] cols = line.split("\t");
			mapLocation.put(cols[0], cols[1] + "#" + cols[2]);
			line = brr.readLine();
		}
		brr.close();
		////////////////DONE!
		
		Map<String, Set<Predicate>> events = new HashMap<String, Set<Predicate>>();
		Map<String, Map<String, TreeMap<Integer, Span>>> mentions = new HashMap<String, Map<String, TreeMap<Integer, Span>>>();
		Map<String, String> mapDct = new HashMap<String, String>();
		Map<String, Map<String, Set<Predicate>>> mapPredicate = new HashMap<String, Map<String, Set<Predicate>>>();
		
		
		////////////////Delete all topics related file, because below they will be written with append mode ////////////////
		Map<String, Integer> eventTypeIdx = new HashMap<String, Integer>();
		eventTypeIdx.put("injuring", 0);
		eventTypeIdx.put("killing", 1);
		eventTypeIdx.put("fire", 2);
		eventTypeIdx.put("displace", 3);
		
		try {
			boolean result = Files.deleteIfExists((new File(topicfile)).toPath());
			result = Files.deleteIfExists((new File(topicfile.replace("_v1.tsv", "_v1_injuring.tsv"))).toPath());
			result = Files.deleteIfExists((new File(topicfile.replace("_v1.tsv", "_v1_killing.tsv"))).toPath());
			result = Files.deleteIfExists((new File(topicfile.replace("_v1.tsv", "_v1_fire.tsv"))).toPath());
			result = Files.deleteIfExists((new File(topicfile.replace("_v1.tsv", "_v1_displace.tsv"))).toPath());
			result = Files.deleteIfExists((new File(topicfile.replace("_v1.tsv", "_v1_dbpedia_ent.tsv"))).toPath());
		} catch(Exception e) {
			
		}
		
		
		////////////////Start reading the docs.conll file... ////////////////
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		line = br.readLine();
		
		int wIdx = 0;
		int eventId = 0;
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				
				String[] cols = line.split("\t");
				if (cols[2].equals("DCT")) dctStr = cols[1];
								
				wIdx ++;
				
			} else {
				boolean firstLine = false;
				
				if (line.startsWith("#begin")) {		/////All steps are done at the beginning of each document...
					if (num > (start + limit - 1)) {
						break;
					}
					
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
					
//					System.out.println(num + ": " + filename);
					
					wIdx = 0;
					
					////////////////Parse Senna output... ////////////////
//					System.out.println("Analyze Senna output...");
					List<String> words = extractWordList(sentencedir + filename + ".txt");
					Map<Integer, Integer> sentMapping = extractSentenceMapping(sennadir + filename + ".txt", words);
					Set<Predicate> predicates = extractSennaPredicates(sennadir + filename + ".txt", words);
					
					
					////////////////Parse BabelFly output... ////////////////
//					System.out.println("Analyze BabelFly output...");
					Map<String, Map<Integer, Set<Span>>> babelSenses = extractBabelEvents(babeldir + filename + ".tsv", sentMapping, topicfile, 
							wnSenseSims, babelSenseSims);
					
					
					////////////////Create a map of mentions (spans) to event type (e.g., shot = injuring) per document... ////////////////
					mentions.put(filename, new HashMap<String, TreeMap<Integer, Span>>());
					for (String eventType : babelSenses.keySet()) {
//						System.out.println(babelSenses.get(eventType));
						events.put(eventType, new HashSet<Predicate>());
						mentions.get(filename).put(eventType, new TreeMap<Integer, Span>());
						
						if (!eventType.equals("dbpedia_ent")) {
							for (Integer sentIdx : babelSenses.get(eventType).keySet()) {
								for(Span s : babelSenses.get(eventType).get(sentIdx)) {
//									int begin = s.getStartOffset();
//									int length = s.getEndOffset() - s.getStartOffset();
//									mentions.put(begin, length + ":" + eventTypeIdx.get(eventType) + "" + eventId);
									mentions.get(filename).get(eventType).put(s.getStartOffset(), s);
								}
							}
						}
						
						eventId ++;
					}
					
					
					////////////////Let's clean the predicates! ////////////////
					////////////////For example, "She said that he killed her", since 'said' predicate contains 'killed', it's removed (unnecessary) ////////////////
					Map<Integer, Predicate> currentPredicates = new HashMap<Integer, Predicate>();
					List<Integer> toBeDeleted = new ArrayList<Integer>();
					int n = 0;
					for (Predicate p : predicates) {
						currentPredicates.put(n, new Predicate(p));
						n ++;
					}
					
					for (Integer key : currentPredicates.keySet()) {
						for (Predicate p : predicates) {
							if (p.getPredicate() != null) {
								if (!currentPredicates.get(key).equals(p)) {
									Span a1 = null;
									Span a2 = null;
									Span aTmp = null;
									Span aAdv = null;
									for (Span s : currentPredicates.get(key).getArguments()) {
										if (s.getType().equals("A1")) a1 = s;
										if (s.getType().equals("A2")) a2 = s;
										if (s.getType().equals("AM-TMP")) aTmp = s;
										if (s.getType().equals("AM-ADV")) aAdv = s;
									}
									if (a1 != null
											&& p.getPredicate().isIncludedIn(a1)
											) {
										toBeDeleted.add(key);
									} else if (a2 != null
											&& p.getPredicate().isIncludedIn(a2)
											) {
										toBeDeleted.add(key);
									} else if (aTmp != null
											&& p.getPredicate().isIncludedIn(aTmp)
											) {
										toBeDeleted.add(key);
									} else if (aAdv != null
											&& p.getPredicate().isIncludedIn(aAdv)
											) {
										toBeDeleted.add(key);
									}
								}
							}
						}
					}
					
					for (Integer del : toBeDeleted) {
						currentPredicates.remove(del);
					}
					predicates.clear();
					for (Integer add : currentPredicates.keySet()) {
						predicates.add(currentPredicates.get(add));
					}

					
					////////////////For every predicate, assign it to the corresponding event type, saved in events ////////////////
//					System.out.println("Combine Senna and BabelFly output...");
					for (Predicate p : predicates) {
						if (p.getPredicate() != null) {
							for (String eventType : babelSenses.keySet()) {
								if (babelSenses.get(eventType).containsKey(p.getPredicate().getSentIndex())) {
									Set<Span> concepts = babelSenses.get(eventType).get(p.getPredicate().getSentIndex());
									for (Span span : concepts) {
										if (p.getPredicate().isIncludedIn(span)) {	//only based on predicate
											events.get(eventType).add(p);
										} else {
//											if (eventType.equals("dbpedia_ent")) {
												for (Span arg : p.getArguments()) {		//if event-related words occur in the arguments
													if (span.isIncludedIn(arg)) {
														events.get(eventType).add(p);
													}
												}
//											}
										}
									}
								}
							}
						}
					}
					
					//Additional: remove injuring predicate that occurs in killing, e.g., "He shot her to death" --> only killing
					for (Iterator<Predicate> iterator = events.get("injuring").iterator(); iterator.hasNext();) {
					    Predicate p = iterator.next();
					    if (events.get("killing").contains(p)) {
					        // Remove the current element from the iterator and the list.
					        iterator.remove();
					    }
					}
					
					
					
					
				
				} else if (line.startsWith("#end")) {	//We're at the end of the document...
					
					if (num >= start) {
						
						////////////////Write all topic files for each event type ////////////////
						mapDct.put(filename, dctStr);
						mapPredicate.put(filename, new HashMap<String, Set<Predicate>>());						
						for (String eventType : events.keySet()) {
							
							mapPredicate.get(filename).put(eventType, events.get(eventType));
							
							BufferedWriter bw = new BufferedWriter(new FileWriter(topicfile.replace(".tsv", "_" + eventType + ".tsv"), true));
							for (Predicate p : events.get(eventType)) {
								bw.write(filename + "\t" + dctStr + "\t" + p.toString2() + "\n");
							}						
							bw.close();
						}
						
						events.clear();
//						mentions.clear();
					}
					
					num ++;
					
				}
			}
			line = br.readLine();
		}
		br.close();
		
		
		////////////////Now, let's get the attributes for each event... ////////////////
		////////////////Create a lot of HashMaps! ////////////////
		
		List<String> eventTypes = new ArrayList<String>();
		eventTypes.add("injuring");
		eventTypes.add("killing");
		eventTypes.add("fire");
		eventTypes.add("displace");
		
		Map<String, List<String>> docIncidents = new HashMap<String, List<String>>();
		Map<String, List<String>> incDocs = new HashMap<String, List<String>>();
		
		Map<String, Set<String>> incCities = new HashMap<String, Set<String>>();
		Map<String, Set<String>> incStates = new HashMap<String, Set<String>>();
		Map<String, List<String>> incLocations = new TreeMap<String, List<String>>();
		Map<String, Set<LocalDate>> docTimes = new HashMap<String, Set<LocalDate>>();
		Map<String, Set<LocalDate>> incTimes = new HashMap<String, Set<LocalDate>>();
		Map<String, Set<String>> incPersons = new HashMap<String, Set<String>>();
		Map<String, Long> incNumVictims = new HashMap<String, Long>();
		
		//Read event times from HeidelTime... then save to eventTimes map.
		Map<String, LocalDate> eventTimes = new HashMap<String, LocalDate>();
		BufferedReader bt = new BufferedReader(new FileReader(topicfile.replace("topics_v1.tsv", "heideltime.txt")));
		String tline = bt.readLine();
		while (tline != null) {
			String[] cols = tline.split(", ");
			eventTimes.put(cols[0].replace(".txt", ""), 
					LocalDate.parse(cols[1])
					);
			tline = bt.readLine();
		}
		bt.close();
		
		//Extract all event attributes... then save to maps.
		int eventTypeId = 1, incidentIdx = 1;
		String city, state;
		for (String eventType : eventTypes) {
			
			////////Extract document clusters based on similarity files
			List<Set<String>> clusters = DocumentClustering.getClusters(topicfile.replace("topics_v1.tsv", "similar_doc_0.1.tsv"), 
					topicfile.replace("topics_v1.tsv", "similar_ent_0.1.tsv"), 
					topicfile.replace(".tsv", "_" + eventType + ".tsv")
					);
			
			
			for (Set<String> docs : clusters) {
				String incidentId = eventTypeId + "" + incidentIdx;		//Update event id...
				
				if (!incDocs.containsKey(incidentId)) incDocs.put(incidentId, new ArrayList<String>());
				
				if (!docTimes.containsKey(incidentId)) docTimes.put(incidentId, new HashSet<LocalDate>());
				if (!incTimes.containsKey(incidentId)) incTimes.put(incidentId, new HashSet<LocalDate>());
				
				////////For each document in a cluster, assume them to have the same event id...
				for (String doc : docs) {
					
					//Mapping between documents and event id
					if (!docIncidents.containsKey(doc)) {
						docIncidents.put(doc, new ArrayList<String>());
						docIncidents.get(doc).add("null");
						docIncidents.get(doc).add("null");
						docIncidents.get(doc).add("null");
						docIncidents.get(doc).add("null");
					}
					docIncidents.get(doc).set(eventTypes.indexOf(eventType), incidentId);
					
					//Mapping between event id and list of documents
					incDocs.get(incidentId).add(doc);
					
					//Mapping between event id and DCT, and event id and event time
					LocalDate dctDate = LocalDate.parse(mapDct.get(doc));
					docTimes.get(incidentId).add(dctDate);
					if (eventTimes.containsKey(doc)) incTimes.get(incidentId).add(eventTimes.get(doc));
					
					//Mapping between event id and city, state, and location (including provenance, i.e. doc id, dct etc.)
					city = mapLocation.get(doc).split("#")[0];
					state = mapLocation.get(doc).split("#")[1];
					if (!incCities.containsKey(incidentId)) incCities.put(incidentId, new HashSet<String>());
					incCities.get(incidentId).add(city);
					if (!incStates.containsKey(incidentId)) incStates.put(incidentId, new HashSet<String>());
					incStates.get(incidentId).add(state);
					if (!incLocations.containsKey(incidentId)) incLocations.put(incidentId, new ArrayList<String>());
					if (eventTimes.containsKey(doc))
						incLocations.get(incidentId).add(city + "\t" + state + "\t" + doc + "\t" + dctDate.toString() + "\t" + eventTimes.get(doc));
					else
						incLocations.get(incidentId).add(city + "\t" + state + "\t" + doc + "\t" + dctDate.toString() + "\t" + "null");
					
					//Mapping between event id and persons involved (based on Senna output)
					String name, firstname, middlename, lastname;
					if (!incPersons.containsKey(incidentId)) incPersons.put(incidentId, new HashSet<String>());
					for (Predicate p : mapPredicate.get(doc).get(eventType)) {
						List<Span> persons = p.getPersons();
						for (Span s : persons) {
							
							name = s.getText();
							String[] namesplit = name.split(" ");
							if (namesplit.length == 1) {
								firstname = "null";
								middlename = "null";
								lastname = name;
							} else if (namesplit.length == 2) {
								firstname = namesplit[0];
								middlename = "null";
								lastname = namesplit[1];
							} else {
								firstname = namesplit[0];
								middlename = " ";
								for (int x=1; x<namesplit.length-1; x++) middlename += " " + namesplit[x];
								middlename = middlename.substring(1);
								lastname = namesplit[namesplit.length-1];
							}
							
							String str = firstname + "\t" + middlename + "\t" + lastname;
							if (s.getType().equals("A0")) {
								if (p.getPredicate().getText().startsWith("die")
										|| p.getPredicate().getText().startsWith("dyi")
										|| p.getPredicate().getText().startsWith("suffer")) {
									str += "\t" + "victim";
								} else {
									str += "\t" + "perpetrator";
								}
							}
							else if (s.getType().equals("A1")) {
								if (p.getPredicate().getText().startsWith("arrest")
										|| p.getPredicate().getText().startsWith("charg")
										|| p.getPredicate().getText().startsWith("book")
										|| p.getPredicate().getText().startsWith("investigat")
										|| p.getPredicate().getText().startsWith("question")
										|| p.getPredicate().getText().startsWith("suspect")
										|| p.getPredicate().getText().startsWith("indict")
										|| p.getPredicate().getText().startsWith("releas")
										|| p.getPredicate().getText().startsWith("fac")
										) {
									str += "\t" + "perpetrator";
								
								} else if ( p.getPredicate().getText().startsWith("said")
										|| p.getPredicate().getText().startsWith("say")) {
									str += "\t" + "unknown";
								
								} else if ( p.getPredicate().getText().startsWith("taken")) {
									boolean hospital = false;
									for (Span sptaken: p.getArguments()) {
										if (sptaken.getText().contains("hospital")
												|| sptaken.getText().contains("wound")
												|| sptaken.getText().contains("injur")) {
											hospital = true;
											break;
										}
									}
									if (hospital) {
										str += "\t" + "victim";
									} else {
										str += "\t" + "unknown";
									}
									
								} else {
									str += "\t" + "victim";
								}								
							}
							
							str += "\t" + doc;
							str += "\t" + s.getSentIndex();
							
							str += "\t" + p.getPredicate().getText();
							for (Span arg : p.getArguments()) {
								if (arg.getType().equals("A2")) str += " " + arg.getText();
								if (arg.getType().equals("AM-MNR")) str += " " + arg.getText();
							}
							
							incPersons.get(incidentId).add(str);
						}
						
						if (p.getPredicate().getText().startsWith("arrest")
								|| p.getPredicate().getText().startsWith("charg")
								|| p.getPredicate().getText().startsWith("book")
								|| p.getPredicate().getText().startsWith("investigat")
								|| p.getPredicate().getText().startsWith("question")
								|| p.getPredicate().getText().startsWith("suspect")
								|| p.getPredicate().getText().startsWith("indict")
								|| p.getPredicate().getText().startsWith("releas")
								|| p.getPredicate().getText().startsWith("fac")
								|| p.getPredicate().getText().startsWith("said")
								|| p.getPredicate().getText().startsWith("say")
								) {
							//Not about the victims!
						} else {
							for (Span arg : p.getArguments()) {
								if (arg.getType().equals("A1")) {
									Long numInArg = containsNumber(arg.getText().toLowerCase());
									if (numInArg > 0) {
										incNumVictims.put(incidentId, numInArg);
									}
								}
							}
						}
					}
				}
				
				incidentIdx ++;
			}
			eventTypeId ++;
		}
		
		
		////////////////Now, write these maps to files (to be used to populate the KG) ////////////////
		
		//Documents to events/incidents
		Set<String> allIncidents = new HashSet<String>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(topicfile));
		for (String docid : docIncidents.keySet()) {			
			bw.write(docid + "\t" + mapDct.get(docid) + "\t" + docIncidents.get(docid).get(0) + 
					"\t" + docIncidents.get(docid).get(1) + 
					"\t" + docIncidents.get(docid).get(2) + 
					"\t" + docIncidents.get(docid).get(3) + "\n");
			if (!docIncidents.get(docid).get(0).equals("null")) allIncidents.add(docIncidents.get(docid).get(0));
			if (!docIncidents.get(docid).get(1).equals("null")) allIncidents.add(docIncidents.get(docid).get(1));
			if (!docIncidents.get(docid).get(2).equals("null")) allIncidents.add(docIncidents.get(docid).get(2));
			if (!docIncidents.get(docid).get(3).equals("null")) allIncidents.add(docIncidents.get(docid).get(3));
		}
		bw.close();
		
		//Events/incidents to times
		Map<String, LocalDate> incidentTimes = new HashMap<String, LocalDate>();
		bw = new BufferedWriter(new FileWriter(topicfile.replace("topic", "event_time")));
		for (String incId : incTimes.keySet()) {
			List<LocalDate> dates = new ArrayList<LocalDate>();
			dates.add(Collections.min(docTimes.get(incId)));
			if (!incTimes.get(incId).isEmpty()) {
				dates.add(Collections.min(incTimes.get(incId)));
			}
			LocalDate minDate = Collections.min(dates);
			incidentTimes.put(incId, minDate);
			bw.write(incId + "\t" + minDate.toString() + "\n");
		}
		bw.close();
		
		//Events/incidents to locations
		bw = new BufferedWriter(new FileWriter(topicfile.replace("topic", "event_location")));
		BufferedWriter bwDebug = new BufferedWriter(new FileWriter(topicfile.replace("topics", "event_location_debug")));
		for (String incId : incCities.keySet()) {
			for (String c : incCities.get(incId)) {
				if (!c.equals("null")) bw.write(incId + "\t" + "city" + "\t" + c + "\n");
			}
			for (String s : incStates.get(incId)) {
				if (!s.equals("null")) bw.write(incId + "\t" + "state" + "\t" + s + "\n");
			}
			for (String l : incLocations.get(incId)) {
				bwDebug.write(incId + "\t" + l + "\n");
			}
		}
		bw.close();
		bwDebug.close();
		
		//Events/incidents to participants
		Map<String, List<Participant>> incParticipants = new HashMap<String, List<Participant>>();
		bw = new BufferedWriter(new FileWriter(topicfile.replace("topic", "event_participant")));
		bwDebug = new BufferedWriter(new FileWriter(topicfile.replace("topics", "event_participant_debug")));
		for (String incId : incPersons.keySet()) {
			Map<String, String> names = new HashMap<String, String>();
			
			////////First clean names in Spacy output...
			
			Set<String> fullNames = new HashSet<String>();
			for (String doc : incDocs.get(incId)) {
				BufferedReader bname = new BufferedReader(new FileReader(babeldir.replace("babel", "spacy") + doc + ".txt"));
				
				String nline = bname.readLine();
				while (nline != null) {
					nline = nline.replace(", ", "; ");
					String[] colss = nline.split("; ");
					if (colss[4].equals("PERSON")) {
						String name = colss[1].replace("NEWLINE", "");
						if (!name.trim().isEmpty())
//							fullNames.add(name);
							fullNames.add(name + "\t" + doc);
					}
					nline = bname.readLine();
				}
				
				bname.close();
			}
			
			Set<String> cleanFullNames = new HashSet<String>();
			//Remove duplicates!
			while(!fullNames.isEmpty()) {
	            String longer = ""; String include = "", prov = "";
	            for(String name : fullNames) {
//	                if(name.length() > longer.length()) {
//	                    longer = name;
//	                }
	                if(name.split("\t")[0].length() > longer.length()) {
	                    longer = name.split("\t")[0];
	                    prov = name.split("\t")[1];
	                }
	            }
	            Iterator<String> iter = fullNames.iterator();
	            while(iter.hasNext()){
//	            	String curr = iter.next();
	            	String curr = iter.next().split("\t")[0];
	                if(!longer.equals(curr)) {
	                	if (longer.contains(curr)){
	                		iter.remove();
	                	} else {
	                		String firstLonger = longer.split(" ")[0];
	                		String firstCurr = curr.split(" ")[0];
	                		String lastLonger = longer.split(" ")[longer.split(" ").length-1];
	                		String lastCurr = curr.split(" ")[curr.split(" ").length-1];
	                		if (firstLonger.equals(firstCurr)
	                				&& lastLonger.equals(lastCurr)) {
	                			iter.remove();
	                		}
	                	}
	                }
	            }
//	            cleanFullNames.add(longer);
	            cleanFullNames.add(longer + "\t" + prov);
//	            while(fullNames.contains(longer)) {
//	            	fullNames.remove(longer);
//	            }
	            while(fullNames.contains(longer + "\t" + prov)) {
	            	fullNames.remove(longer + "\t" + prov);
	            }
	        }
			
			
			////////Then get the involved names from Senna output...
			
			Set<String> participants = new HashSet<String>();
			for (String p : incPersons.get(incId)) {
				String[] cols = p.split("\t");
				String firstName = cols[0];
				String middleName = cols[1];
				String lastName = cols[2];
				String role = cols[3];
				String doc = cols[4];
				String sent = cols[5];
				String pred = cols[6];
				if (firstName .equals("null")
						&& middleName.equals("null")
						&& lastName.equals("Jr.")
						) {
					//error!
				
				} else if (lastName.endsWith(".com")) {
					//error!
					
				} else {
					if (lastName.equals("Jr.")) {
						if (middleName.equals("null")) {
							lastName = firstName;
							firstName = "null";
						} else {
							if (middleName.split(" ").length > 1) {
								String[] middle = middleName.split(" ");
								for (int i=0; i<middle.length-1; i++) middleName = " " + middle[i];
								middleName = middleName.substring(1);
								lastName = middle[middle.length-1];
							} else {
								lastName = middleName;
								middleName = "null";
							}
						}
					}
					firstName = firstName.replace(".", "").trim();
					middleName = middleName.replace(".", "").trim();
					lastName = lastName.replace(".", "").trim();
					participants.add(firstName + " " + middleName + " " + lastName);
					
					if (!incParticipants.containsKey(incId)) incParticipants.put(incId, new ArrayList<Participant>());
					Participant par = new Participant(firstName, middleName, lastName, role);
					incParticipants.get(incId).add(par);
					
					bw.write(incId + "\t" + firstName + "\t" + middleName + "\t" + lastName + "\t" + role + "\t" + pred + "\n");
					bwDebug.write(incId + "\t" + firstName + "\t" + middleName + "\t" + lastName + "\t" + role + "\t"
							+ doc + "\t"
							+ sent + "\t"
							+ pred + "\t" + "\n");
				}
			}
			
			////////Then combine them...
			
			for (String fname : cleanFullNames) {
				
				String doc = fname.split("\t")[1];
				
				fname = fname.split("\t")[0];
				fname = fname.replace("’s", "");
				fname = fname.replace("'s", "");
				fname = fname.replace("”", "");
				fname = fname.replace("“", "");
				fname = fname.replace("'", "");
				fname = fname.replace("’", "");
				fname = fname.replace("\"", "");
				fname = fname.replace("  ", " ");
				fname = fname.replace("   ", " ");
				fname = fname.replace("--", "");
				fname = fname.replace("—", "");
				fname = fname.trim();
				
				if (!fname.contains("- old")
						&& !fname.contains("- Old")
						&& !fname.contains("—")
						&& !fname.contains("@")
						&& !fname.contains("/")
						&& !fname.contains(":")
						&& !fname.contains("©")
						) {
//					String[] namesplit = fname.split(" ");
					String[] namesplit = fname.split("\t")[0].split(" ");
					String firstName, middleName, lastName;
					
					if (namesplit.length == 1) {
						firstName = "null";
						middleName = "null";
						lastName = fname;
					} else if (namesplit.length == 2) {
						firstName = namesplit[0];
						middleName = "null";
						lastName = namesplit[1];
					} else {
						if (namesplit[1].equals("-")) {
							firstName = "null";
							middleName = "null";
							lastName = namesplit[0] + "-" + namesplit[1]; 
						} else if (namesplit[2].equals("-")) {
							firstName = namesplit[0];
							middleName = "null";
							if (namesplit.length == 4) {
								lastName = namesplit[1] + "-" + namesplit[3];
							} else {
								lastName = namesplit[1];
							}
						} else {
							firstName = namesplit[0];
							middleName = " ";
							for (int x=1; x<namesplit.length-1; x++) middleName += " " + namesplit[x];
							middleName = middleName.substring(1);
							lastName = namesplit[namesplit.length-1];
						}
					}
					
					if (firstName .equals("null")
							&& middleName.equals("null")
							&& lastName.equals("Jr.")
							) {
						//error!
					
					} else if (lastName.endsWith(".com")) {
						//error!
						
					} else {
						if (lastName.equals("Jr.")) {
							if (middleName.equals("null")) {
								lastName = firstName;
								firstName = "null";
							} else {
								if (middleName.split(" ").length > 1) {
									String[] middle = middleName.split(" ");
									for (int i=0; i<middle.length-1; i++) middleName = " " + middle[i];
									middleName = middleName.substring(1);
									lastName = middle[middle.length-1];
								} else {
									lastName = middleName;
									middleName = "null";
								}
							}
						}
					}
					firstName = firstName.replace(".", "").trim();
					middleName = middleName.replace(".", "").trim();
					lastName = lastName.replace(".", "").trim();
					
					String full = firstName.trim() + " " + middleName.trim() + " " + lastName.trim();
					String full2 = firstName.trim() + " " + "null" + " " + lastName.trim();
					String full3 = firstName.trim() + " " + "null" + " " + "null";
					String full4 = "null" + " " + "null" + " " + lastName.trim();
					
					if (!participants.contains(full)
							&& !participants.contains(full2)
							&& !participants.contains(full3)
							&& !participants.contains(full4)
							) {
							
						if (!incParticipants.containsKey(incId)) incParticipants.put(incId, new ArrayList<Participant>());
						Participant par = new Participant(firstName, middleName, lastName, "unknown");
						incParticipants.get(incId).add(par);
						
//						participants.add(firstName.trim() + " " + middleName.trim() + " " + lastName.trim());
						bw.write(incId + "\t" + firstName + "\t" + middleName + "\t" + lastName + "\t" + "unknown" + "\t" + "null" + "\n");
						bwDebug.write(incId + "\t" + firstName + "\t" + middleName + "\t" + lastName + "\t" + "unknown" + "\t" + doc + "\tnull" + "\tnull" + "\n");
						
					}
				}
			}
		}
		bw.close();
		bwDebug.close();
		
		
		//Events/incidents to number of victims
		bw = new BufferedWriter(new FileWriter(topicfile.replace("topic", "event_num_victim")));
		for (String incId : incNumVictims.keySet()) {
			bw.write(incId + "\t" + incNumVictims.get(incId) + "\n");
		}
		bw.close();
		
		
		////////////////Now, let's generate annotated document (docs.conll) ////////////////		
		
		//Write event mentions to docs.conll
		BufferedReader input = new BufferedReader(new FileReader(filepath));
		BufferedWriter out = new BufferedWriter(new FileWriter(filepath.replace("input/s1/", "")));
		
		wIdx = 0;
		int docIdx = 1;
		
		Map<Integer, String> mentionIncidentId = new HashMap<Integer, String>();
		Map<Integer, Span> mentionSpan = new HashMap<Integer, Span>();
		line = input.readLine();
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				
				String[] cols = line.split("\t");
				if (cols[2].equals("DCT")) dctStr = cols[1];
				
				if (mentionIncidentId.containsKey(wIdx)) {
					Span menSpan = mentionSpan.get(wIdx);
					int length = menSpan.getEndOffset() - menSpan.getStartOffset();
//					String eventIdx = "2" + mentionIncidentId.get(wIdx) + docIdx;
					String eventIdx = "2" + mentionIncidentId.get(wIdx);
					
					if (length == 1) {
						out.write(cols[0] + "\t" + cols[1] + "\t" + cols[2] + "\t" + "(" + eventIdx + ")" + "\n");
					
					} else {
						if (menSpan.getText().toLowerCase().startsWith("gunshot wound")) {
							out.write(cols[0] + "\t" + cols[1] + "\t" + cols[2] + "\t" + "(" + eventIdx + ")" + "\n");
							line = input.readLine(); wIdx ++;
							cols = line.split("\t");
							out.write(cols[0] + "\t" + cols[1] + "\t" + cols[2] + "\t" + "" + eventIdx + "\n");
						} else {
							out.write(cols[0] + "\t" + cols[1] + "\t" + cols[2] + "\t" + "(" + eventIdx + "\n");
							for (int x=0; x<length-2; x++) {
								line = input.readLine(); wIdx ++;
								cols = line.split("\t");
								out.write(cols[0] + "\t" + cols[1] + "\t" + cols[2] + "\t" + "" + eventIdx + "\n");
							}
							line = input.readLine(); wIdx ++;
							cols = line.split("\t");
							out.write(cols[0] + "\t" + cols[1] + "\t" + cols[2] + "\t" + "" + eventIdx + ")" + "\n");
						}
					}
				} else {
					out.write(line.trim() + "\n");
				}
				
				wIdx ++;
				
			} else {
				if (line.startsWith("#begin")) {
					
					out.write(line.trim() + "\n");
					
					wIdx = 0;
					docIdx ++;
					
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
					
//					System.out.println(filename);
					
					for (String eventType : eventTypes) {
						
						for (Integer mentionStart : mentions.get(filename).get(eventType).keySet()) {
							Span men = mentions.get(filename).get(eventType).get(mentionStart);
						
							Set<Predicate> predicates = mapPredicate.get(filename).get(eventType);
							
							if (docIncidents.containsKey(filename)) {
								String incidentId = docIncidents.get(filename).get(eventTypes.indexOf(eventType));
								
								if (!incidentId.equals("null")) {
									mentionIncidentId.put(mentionStart, incidentId);
									mentionSpan.put(mentionStart, men);
								}
								
//								if (!incidentId.equals("null")) {
//									for (Predicate ppp : predicates) {
//										for (Span sss : ppp.getArguments()) {
//											if (men.isIncludedIn(sss)) {
//												mentionIncidentId.put(mentionStart, incidentId);
//												mentionSpan.put(mentionStart, mentions.get(eventType).get(filename).get(mentionStart));
//											}
//										}
//									}
//								}
							}
						}
					}
					
//					System.out.println(mentionIncidentId);
				
				} else if (line.startsWith("#end")) {
					
					out.write(line.trim() + "\n");
					
					mentionIncidentId.clear();
					mentionSpan.clear();
					
				}
			}
			line = input.readLine();
		}
		input.close();
		out.close();
		
		
		
		////////////////Now that we have all the mappings, let's answer some questions! ////////////////
		
		//Extract questions
		br = new BufferedReader(new FileReader(questionpath));
		String jsonStr = "";
		line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    JSONObject obj = new JSONObject(jsonStr);
	    JSONObject answers = new JSONObject();
	    
	    // get the first result
	    Iterator<String> keys = obj.keys();
	    
	    Set<String> incidentsType = new HashSet<String>();
	    Set<String> incidentsCity = new HashSet<String>();
	    Set<String> incidentsState = new HashSet<String>();
	    Set<String> incidentsPart = new HashSet<String>();
	    Set<String> incidentsTime = new HashSet<String>();
	    
	    int numQuestion = 0;
	    int numAnswered = 0;
	    
	    while( keys.hasNext() ) {
	    	
	    	numQuestion ++;
	    	
	    	incidentsType.clear();
		    incidentsCity.clear();
		    incidentsState.clear();
		    incidentsPart.clear();
		    incidentsTime.clear();
	    	
	        String key = (String)keys.next();
	        JSONObject question = obj.getJSONObject(key);
	        
	        String eventType = question.getString("event_type");
	        for (String incId : allIncidents) {
	        	String type = "none";
	        	if (incId.startsWith("1")) {
	        		type = "injuring";
	        	} else if (incId.startsWith("2")) {
	        		type = "killing";
	        	} else if (incId.startsWith("3")) {
	        		type = "fire";
	        	} else if (incId.startsWith("4")) {
	        		type = "displace";
	        	}
	        	if (type.equals(eventType)) {
	        		incidentsType.add(incId);
	        	}
	        }
	        System.out.println(key + ": " + incidentsType.size());
	        
	        if (question.has("location")) {
	        	JSONObject location = question.getJSONObject("location");
	        	if (location.has("city")) {
	        		URI ct = new URI(location.getString("city"));
	        		for (String inc : incCities.keySet()) {
	        			for (String ctt : incCities.get(inc)) {
	        				URI cityy = new URI(ctt); 
		        			if (cityy.equals(ct)) {
		    	        		incidentsCity.add(inc);
		        			}
	        			}
	        		}
	        		System.out.println("  --location: " + incidentsCity.size());
	        		incidentsType.retainAll(incidentsCity);
	        	}
	        	if (location.has("state")) {
	        		URI st = new URI(location.getString("state"));
	        		for (String inc : incStates.keySet()) {
	        			for (String stt : incStates.get(inc)) {
	        				URI statee = new URI(stt);
		        			if (statee.equals(st)) {
		    	        		incidentsState.add(inc);
		        			}
	        			}
	        		}
	        		System.out.println("  --location: " + incidentsState.size());
	        		incidentsType.retainAll(incidentsState);
	        	}
	        }
	        
	        if (question.has("participant")) {
	        	JSONObject participant = question.getJSONObject("participant");
	        	if (participant.has("full_name")) {
	        		String fullname = participant.getString("full_name");
	        		for (String inc : incParticipants.keySet()) {
	        			for (Participant part : incParticipants.get(inc)) {
	        				if (subtask == 1 || subtask == 3) {
	        					if (fullname.equals(part.getFullName())) {
	        						incidentsPart.add(inc);
	        					}
	        				} else if (subtask == 2) {
	        					if (fullname.equals(part.getFullName())
	        							&& !part.getRole().equals("unknown")
	        							) {
	        						incidentsPart.add(inc);
	        					}
	        				} 
	        			}
	        		}
	        		System.out.println("  --participant: " + incidentsTime.size());
	        		incidentsType.retainAll(incidentsPart);
	        	}
	        	if (participant.has("first")) {
	        		String first = participant.getString("first");
	        		for (String inc : incParticipants.keySet()) {
	        			for (Participant part : incParticipants.get(inc)) {
	        				if (subtask == 1 || subtask == 3) {
	        					if (first.equals(part.getFirstName())) {
	        						incidentsPart.add(inc);
	        					}
	        				} else if (subtask == 2) {
	        					if (first.equals(part.getFirstName())
	        							&& !part.getRole().equals("unknown")
	        							) {
	        						incidentsPart.add(inc);
	        					}
	        				}
	        			}
	        		}
	        		System.out.println("  --participant: " + incidentsPart.size());
	        		incidentsType.retainAll(incidentsPart);
	        	}
	        	if (participant.has("last")) {
	        		String last = participant.getString("last");
	        		for (String inc : incParticipants.keySet()) {
	        			for (Participant part : incParticipants.get(inc)) {
	        				if (subtask == 1 || subtask == 3) {
	        					if (last.equals(part.getLastName())) {
	        						incidentsPart.add(inc);
	        					}
	        				} else if (subtask == 2) {
	        					if (last.equals(part.getLastName())
	        							&& !part.getRole().equals("unknown")
	        							) {
	        						incidentsPart.add(inc);
	        					}
	        				}
	        			}
	        		}
	        		System.out.println("  --participant: " + incidentsPart.size());
	        		incidentsType.retainAll(incidentsPart);
	        	}
	        }
	        
	        if (question.has("time")) {
	        	JSONObject time = question.getJSONObject("time");
	        	if (time.has("year")) {
	        		String year = time.getString("year");
	        		for (String inc : incidentTimes.keySet()) {
	        			if (Integer.parseInt(year) == incidentTimes.get(inc).getYear()) {
	        				incidentsTime.add(inc);
	        			}
	        		}
	        		System.out.println("  --time: " + incidentsTime.size());
	        		incidentsType.retainAll(incidentsTime);
	        	}
	        	if (time.has("month")) {
	        		String month = time.getString("month").split("/")[0];
	        		String year = time.getString("month").split("/")[1];
	        		for (String inc : incidentTimes.keySet()) {
	        			if (Integer.parseInt(month) == incidentTimes.get(inc).getMonthValue()
	        					&& Integer.parseInt(year) == incidentTimes.get(inc).getYear()) {
	        				incidentsTime.add(inc);
	        			}
	        		}
	        		System.out.println("  --time: " + incidentsTime.size());
	        		incidentsType.retainAll(incidentsTime);
	        	}
	        	if (time.has("day")) {
	        		String day = time.getString("day").split("/")[0];
	        		String month = time.getString("day").split("/")[1];
	        		String year = time.getString("day").split("/")[2];
	        		for (String inc : incidentTimes.keySet()) {
	        			if (Integer.parseInt(day) == incidentTimes.get(inc).getDayOfMonth()
	        					&& Integer.parseInt(month) == incidentTimes.get(inc).getMonthValue()
	        					&& Integer.parseInt(year) == incidentTimes.get(inc).getYear()) {
	        				incidentsTime.add(inc);
	        			}
	        		}
	        		System.out.println("  --time: " + incidentsTime.size());
	        		incidentsType.retainAll(incidentsTime);
	        	}
	        }
	        
	        if (incidentsType.size() > 0){
	        	numAnswered ++;
	        	
//	        	JSONObject incident = new JSONObject();
	        	JSONArray documents = new JSONArray();
	        	for (String incId : incidentsType) {
//	        		JSONArray documents = new JSONArray();
	        		for (String docId : incDocs.get(incId)) {
		        		documents.put(docId);
		        	}
//		        	incident.put(incId, documents);
		        }
		        
		        JSONObject answer = new JSONObject();
		        if (subtask == 1) {
		        	answer.put("numerical_answer", 1);
		        } else if (subtask == 2 || subtask == 3) {
		        	answer.put("numerical_answer", incidentsType.size());
		        }
//		        answer.put("answer_docs", incident);
		        answer.put("answer_docs", documents);
		        
		        int num_injured = 0;
		        int num_killed = 0;
		        if (subtask == 3) {
		        	JSONObject incident2 = new JSONObject();
		        	for (String inc : incidentsType) {
		        		num_injured = 0;
		        		num_killed = 0;
		        		if (inc.startsWith("1")) {
		        			if (incNumVictims.containsKey(inc)) {
		        				num_injured += incNumVictims.get(inc);
		        			} else {
		        				if (incParticipants.containsKey(inc)) {
			        				for (Participant part : incParticipants.get(inc)) {
				        				if (part.getRole().equals("victim")) num_injured ++;
				        			}
		        				}
		        			}
		        		} else if (inc.startsWith("2")) {
		        			if (incNumVictims.containsKey(inc)) {
		        				num_killed += incNumVictims.get(inc);
		        			} else {
		        				if (incParticipants.containsKey(inc)) {
			        				for (Participant part : incParticipants.get(inc)) {
				        				if (part.getRole().equals("victim")) num_killed ++;
				        			}
		        				}
		        			}
		        		}
		        		JSONObject numbers = new JSONObject();
		        		numbers.put("num_injured", num_injured);
		        		numbers.put("num_killed", num_killed);
		        		
		        		incident2.put(inc, numbers);
		        	}
		        	answer.put("part-info", incident2);
		        	
		        }
		        
		        answers.put(key, answer);
		        
	        }
	        
	    }
	    System.out.println(numAnswered + "--" + numQuestion + "--" + numAnswered/(double)numQuestion);
	    
	    try (FileWriter jsonfile = new FileWriter(babeldir.replace("babel/", "s"+subtask+"_answers.json"))) {
	    	jsonfile.write(answers.toString(4));
		}
	    
	    
	    ////////////////DONE! Phew... ////////////////
		
	}
	
	public void extractWordNet(String filepath, String sentencedir, String babeldir, String sennadir, String topicfile, int startDoc) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "", towrite = "";
//		StringBuilder sentences = new StringBuilder();
		
		int limit = 10000;
		int start = startDoc;
		int num = 1;
		int predcol = 0;
		
		String dctStr = "";
		Map<String, List<Predicate>> events = new HashMap<String, List<Predicate>>();
		Set<String> babelSenses = new HashSet<String>();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(topicfile.replace(".tsv", "_wordnet.tsv")));
		
		line = br.readLine();
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				String[] cols = line.split("\t");
				if (cols[2].equals("DCT")) dctStr = cols[1];
				
			} else {
				boolean firstLine = false;
				
				if (line.startsWith("#begin")) {
					if (num > (start + limit - 1)) {
						break;
					}
					
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
//					sentences.setLength(0);
					
					events.clear();
				
				} else if (line.startsWith("#end")) {
					if (num >= start) {
						
						System.out.println(num + ": " + filename);
						
	//					System.out.println("Analyze Senna output...");
						List<String> words = extractWordList(sentencedir + filename + ".txt");
						Map<Integer, Integer> sentMapping = extractSentenceMapping(sennadir + filename + ".txt", words);
						
	//					System.out.println("Analyze BabelFly output...");
						BufferedReader brr = new BufferedReader(new FileReader(babeldir + filename + ".tsv"));
						String linee = brr.readLine();
						
						String vInjure = "00069879-v";
						String nInjury = "14285662-n";
						String aInjured = "01317954-a";
						
						String vKill = "01323958-v";
						String vDie = "00358431-v";
						String nKilling = "00219012-n";
						String aDead = "00095280-a";
						String aDeadly = "00993667-a";
						String rFatally = "00506577-r";
						
						String nFire = "07302836-n";
						String nFlame = "13480848-n";
						String nFirefighter = "10091651-n";
						String nBurn = "14289590-n";
						String vBurn = "00378664-v";
						
						String vDisplace = "02402825-v";
						
						double threshold = 4.0;

						while (linee != null) {
							
							double killingRelScore = 0.0;
							double injuringRelScore = 0.0;
							double fireRelScore = 0.0;
							double displaceRelScore = 0.0;
							
							String[] cols = linee.split("\t");
							
//							System.out.println(linee);
							
							Span s = new Span(cols[0], Integer.parseInt(cols[1].split(",")[0]), Integer.parseInt(cols[1].split(",")[1]));
							Integer sentIdx = sentMapping.get(s.getStartOffset()); s.setSentIndex(sentIdx);
							
							String babelSyn = cols[2];
							String wnSyn = cols[4];
							wnSyn = wnSyn.substring(1, wnSyn.length()-1);	// remove []
							if (!wnSyn.equals("")
									&& !cols[3].equals("Named Entity")) {
								wnSyn = wnSyn.split(",")[0].trim();
								char wnPos = wnSyn.charAt(wnSyn.length()-1);
								wnSyn = wnSyn.substring(3, wnSyn.length()-1) + "-" + wnPos;	//remove prefix 'wn:' and add '-' before pos
								
								if (!babelSenses.contains(babelSyn)) {
									
									SemanticRelatedness rel, rel2, rel3, rel4;
									if (wnPos == 'a') {
										rel = new SemanticRelatedness(wnSyn, aInjured);
										injuringRelScore = rel.getHsoScore();
										
										rel = new SemanticRelatedness(wnSyn, aDead);
										rel2 = new SemanticRelatedness(wnSyn, aDeadly);
										killingRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
										
									} else if (wnPos == 'v') {
										rel = new SemanticRelatedness(wnSyn, vInjure);
										injuringRelScore = rel.getHsoScore();
										
										rel = new SemanticRelatedness(wnSyn, vKill);
										rel2 = new SemanticRelatedness(wnSyn, vDie);
										killingRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
										
										rel = new SemanticRelatedness(wnSyn, vDisplace);
										displaceRelScore = rel.getHsoScore();
										
										rel = new SemanticRelatedness(wnSyn, vBurn);
										fireRelScore = rel.getHsoScore();
										
									} else if (wnPos == 'n') {
										rel = new SemanticRelatedness(wnSyn, nInjury);
										injuringRelScore = rel.getHsoScore();
										
										rel = new SemanticRelatedness(wnSyn, nKilling);
										killingRelScore = rel.getHsoScore();
										
										rel = new SemanticRelatedness(wnSyn, nFire);
										rel2 = new SemanticRelatedness(wnSyn, nFlame);
										rel3 = new SemanticRelatedness(wnSyn, nFirefighter);
										rel4 = new SemanticRelatedness(wnSyn, nBurn);
										
										injuringRelScore = Math.max(rel.getHsoScore(), rel4.getHsoScore());
										fireRelScore = Math.max(rel.getHsoScore(), rel2.getHsoScore());
										fireRelScore = Math.max(fireRelScore, rel3.getHsoScore());
										fireRelScore = Math.max(fireRelScore, rel4.getHsoScore());
									
									} else if (wnPos == 'r') {
					//					rel = new SemanticRelatedness(wnSyn, nInjury);
					//					injuringRelScore = rel.getHsoScore();
										
										rel = new SemanticRelatedness(wnSyn, rFatally);
										killingRelScore = rel.getHsoScore();
									}
									babelSenses.add(babelSyn);
									
									bw.write(cols[7].toLowerCase() + "\t" + cols[2] + 
											"\t" + injuringRelScore + 
											"\t" + killingRelScore + 
											"\t" + fireRelScore + 
											"\t" + displaceRelScore + "\n");
								}
							
							}
									
							linee = brr.readLine();
						}
						
						brr.close();
					}
					
					num ++;
					
				}
			}
			line = br.readLine();
		}
		
		bw.close();
	}
	
	public void extractBabelNet(String filepath, String sentencedir, String babeldir, String sennadir, String topicfile, int startDoc) throws IOException, InvalidBabelSynsetIDException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line, filename = "", towrite = "";
//		StringBuilder sentences = new StringBuilder();
		
		int limit = 10000;
		int start = startDoc;
		int num = 1;
		int predcol = 0;
		
		boolean injuring = false;
		boolean killing = false;
		boolean fire = false;
		boolean displace = false;
		
		String dctStr = "";
		Set<String> babelSenses = new HashSet<String>();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(topicfile.replace(".tsv", "_babelnet.tsv")));
		
		BabelNet bn = BabelNet.getInstance();
		
		line = br.readLine();
		
		while (line != null) {
			if (!line.startsWith("#begin") && !line.startsWith("#end")) {
				String[] cols = line.split("\t");
				if (cols[2].equals("DCT")) dctStr = cols[1];
				
			} else {
				boolean firstLine = false;
				
				if (line.startsWith("#begin")) {
					if (num > (start + limit - 1)) {
						break;
					}
					
					filename = line.split(" ")[2].substring(1);
					filename = filename.substring(0, filename.length()-2);
//					sentences.setLength(0);
				
				} else if (line.startsWith("#end")) {
					if (num >= start) {
						
						System.out.println(num + ": " + filename);
						
	//					System.out.println("Analyze Senna output...");
						List<String> words = extractWordList(sentencedir + filename + ".txt");
						Map<Integer, Integer> sentMapping = extractSentenceMapping(sennadir + filename + ".txt", words);
						
	//					System.out.println("Analyze BabelFly output...");
						BufferedReader brr = new BufferedReader(new FileReader(babeldir + filename + ".tsv"));
						String linee = brr.readLine();
						
						while (linee != null) {
							
							String[] cols = linee.split("\t");
							
//							System.out.println(linee);
							
							Span s = new Span(cols[0], Integer.parseInt(cols[1].split(",")[0]), Integer.parseInt(cols[1].split(",")[1]));
							Integer sentIdx = sentMapping.get(s.getStartOffset()); s.setSentIndex(sentIdx);
							
							String babelSyn = cols[2];
							
							injuring = false;
							killing = false;
							fire = false;
							displace = false;
							
							if (!babelSenses.contains(babelSyn)
									&& !cols[0].equals("NEWLINE")
									&& !cols[3].equals("Named Entity")) {
								
								BabelSynset by = bn.getSynset(new BabelSynsetID(cols[2]));
								for(BabelSynsetIDRelation edge : by.getEdges()) {
									if (edge.getBabelSynsetIDTarget().equals("bn:00089751v")
											|| edge.getBabelSynsetIDTarget().equals("bn:00000694n")
											) {
										injuring = true;
									}
						            if (edge.getBabelSynsetIDTarget().equals("bn:00090098v")
											|| edge.getBabelSynsetIDTarget().equals("bn:00032175n")
											|| edge.getBabelSynsetIDTarget().equals("bn:00115341r")
											|| edge.getBabelSynsetIDTarget().equals("bn:00100948a")
											|| edge.getBabelSynsetIDTarget().equals("bn:00084343v")
											|| edge.getBabelSynsetIDTarget().equals("bn:00025582n")
											) {
						            	killing = true;
						            }
						            if (edge.getBabelSynsetIDTarget().equals("bn:00034623n")
						            		|| edge.getBabelSynsetIDTarget().equals("bn:00014012n")
						            		|| edge.getBabelSynsetIDTarget().equals("bn:00084282v")
						            		) {
						            	fire = true;
						            }
						            if (edge.getBabelSynsetIDTarget().equals("bn:00084451v")
						            		|| edge.getBabelSynsetIDTarget().equals("bn:15601034n")
						            		) {
						            	displace = true;
						            }
						        }
								towrite = cols[7].toLowerCase() + "\t" + cols[2];
								if (injuring) towrite += "\t1.0"; else towrite += "\t0.0";
								if (killing) towrite += "\t1.0"; else towrite += "\t0.0";
								if (fire) towrite += "\t1.0"; else towrite += "\t0.0";
								if (displace) towrite += "\t1.0"; else towrite += "\t0.0";
								bw.write(towrite + "\n");
								babelSenses.add(babelSyn);
							}
									
							linee = brr.readLine();
						}
						
						brr.close();
						
					}
					
					num ++;
					
				}
			}
			line = br.readLine();
		}
		
		bw.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		SemEvalDataParser parser = new SemEvalDataParser();
		
//		BabelNet bn = BabelNet.getInstance();
		
		//Parse SemEval CoNLL format into Mate tools input format, one article per file
//		parser.writeMateSentences("./data/trial_data_final/input/s1/docs.conll");
		
		//Parse SemEval CoNLL format into sentence (raw text) format, one article per file
//		parser.writeSentences("./data/trial_data_final/input/s1/docs.conll", "./data/trial_data_final/sentences_v2/");		//done!
//		parser.writeSentences("./data/test_data/input/s1/docs.conll", "./data/test_data/sentences_v2/");					//done!
		
		//Parse SemEval CoNLL format into sentence (raw text) format, then run BabelFly on the sentences
//		parser.writeBabelOutput("./data/trial_data_final/input/s1/docs.conll", "./data/trial_data_final/babel/");
//		parser.writeBabelOutput("./data/test_data/input/s1/docs.conll", "./data/test_data/babel/");
		
//		parser.writeBabelOutputOnlyTitle("./data/trial_data_final/input/s1/docs.conll");
		
		for (int i=1; i<4; i++) {
		
			parser.combineSennaBabelOutput("./data/trial_data_final/input/s1/docs.conll", 
					"./data/trial_data_final/sentences/",
					"./data/trial_data_final/babel/", 
					"./data/trial_data_final/senna_v2/",
					"./data/trial_data_final/topics_v1.tsv", 
					1, "./data/trial_data_final/input/s"+i+"/questions.json", i);
			
			parser.combineSennaBabelOutput("./data/test_data/input/s1/docs.conll", 
					"./data/test_data/sentences/",
					"./data/test_data/babel/", 
					"./data/test_data/senna_v2/", 
					"./data/test_data/topics_v1.tsv",
					1, "./data/test_data/input/s"+i+"/questions.json", i);
		}
		
//		parser.extractWordNet("./data/trial_data_final/input/s1/docs.conll", 
//				"./data/trial_data_final/sentences/",
//				"./data/trial_data_final/babel/", 
//				"./data/trial_data_final/senna/",
//				"./data/trial_data_final/topics_v1.tsv", 1);
//		parser.extractWordNet("./data/test_data/input/s1/docs.conll", 
//				"./data/test_data/sentences/",
//				"./data/test_data/babel/", 
//				"./data/test_data/senna/",
//				"./data/test_data/topics_v1.tsv", 1);		
//		parser.extractBabelNet("./data/trial_data_final/input/s1/docs.conll", 
//				"./data/trial_data_final/sentences/",
//				"./data/trial_data_final/babel/", 
//				"./data/trial_data_final/senna/",
//				"./data/trial_data_final/topics_v1.tsv", 1);
//		parser.extractBabelNet("./data/test_data/input/s1/docs.conll", 
//				"./data/trial_data_final/sentences/",
//				"./data/test_data/babel/", 
//				"./data/test_data/senna/",
//				"./data/test_data/topics_v1.tsv", 1);
		
//		parser.getTopicsFromTitle("./data/trial_data_final/input/s1/docs.conll");
		
//		parser.getTopicsFromArticle("./data/trial_data_final/input/s1/docs.conll", 
//				"./data/trial_data_final/babel/", 
//				"./data/trial_data_final/topics.tsv");
//		parser.getTopicsFromArticle("./data/test_data/input/s1/docs.conll", 
//				"./data/test_data/babel/", 
//				"./data/test_data/topics.tsv");		
		
		// Gets a BabelSynset from a concept identifier (Babel synset ID).
//		BabelSynset by = bn.getSynset(new BabelSynsetID("bn:00084270v"));
//		BabelSense bs = by.getMainSense(Language.EN);
//		System.out.println(bs.getSimpleLemma() + "---" + bs.getWordNetOffset());
		
		
	}

}
