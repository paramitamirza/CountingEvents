import java.util.List;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.impl.Vector;
import edu.cmu.lti.ws4j.impl.VectorPairs;

public class SemanticRelatedness {
	
	private double hsoScore;
	private double pathScore;
	private double wupScore;
	
	public SemanticRelatedness() {
		
	}
	
	public SemanticRelatedness(String synOffset1, String synOffset2) {
		ILexicalDatabase db = new NictWordNet();
		RelatednessCalculator hso = new HirstStOnge(db);
		RelatednessCalculator path = new Path(db);
		RelatednessCalculator wup = new WuPalmer(db);	//might work with threshold 0.5
		
		char posCh1 = synOffset1.charAt(synOffset1.length()-1);
		char posCh2 = synOffset1.charAt(synOffset2.length()-1);
		
		POS pos1, pos2;
		switch(posCh1) {
			case 'a': pos1 = POS.a;
			case 'n': pos1 = POS.n;
			case 'v': pos1 = POS.v;
			case 'r': pos1 = POS.r;
			default: pos1 = null;
		}
		switch(posCh2) {
			case 'a': pos2 = POS.a;
			case 'n': pos2 = POS.n;
			case 'v': pos2 = POS.v;
			case 'r': pos2 = POS.r;
			default: pos2 = null;
		}
		
		Concept concept1 = new Concept(synOffset1, pos1, null, null);
		Concept concept2 = new Concept(synOffset2, pos2, null, null);
		
		Relatedness hsoRel = hso.calcRelatednessOfSynset(concept1, concept2);
		Relatedness pathRel = path.calcRelatednessOfSynset(concept1, concept2);
		Relatedness wupRel = wup.calcRelatednessOfSynset(concept1, concept2);
		
		this.setHsoScore(hsoRel.getScore());
		this.setPathScore(pathRel.getScore());
		this.setWupScore(wupRel.getScore());
	}
	
	public static void main(String[] args) throws Exception {
		
		ILexicalDatabase db = new NictWordNet();
		RelatednessCalculator rc = new HirstStOnge(db);
//		RelatednessCalculator rc = new Path(db);
//		RelatednessCalculator rc = new WuPalmer(db);	//might work with threshold 0.5
		
		List<Concept> test = (List<Concept>) db.getAllConcepts("deadly", "a");
		System.out.println(test);
		
		List<Concept> test2 = (List<Concept>) db.getAllConcepts("fatal", "a");
		System.out.println(test2);
		
		Concept deadly = new Concept("00993667-a", POS.a, null, null);
		Concept fatal = new Concept("00993529-a", POS.a, null, null);
		Concept dead = new Concept("00095280-a", POS.a, null, null);
		
//		Concept deadly = test.get(0);
//		Concept fatal = test2.get(0);
		
		Concept shoot = new Concept("01137138-v", POS.v, null, null);
		Concept hit = new Concept("01405044-v", POS.v, null, null);
		Concept injure = new Concept("00069879-v", POS.v, null, null);
		Concept investigate = new Concept("00789138-v", POS.v, null, null);
		
		Concept news = new Concept("06681177-n", POS.n, null, null);
		Concept injury = new Concept("14285662-n", POS.n, null, null);
		Concept death = new Concept("07355491-n", POS.n, null, null);
		Concept killing = new Concept("00219012-n", POS.n, null, null);
		Concept murder = new Concept("00220522-n", POS.n, null, null);
		
		Concept deadlyAdv = new Concept("00304898-r", POS.r, null, null);
		Concept fatally = new Concept("00506577-r", POS.r, null, null);
		
		Relatedness rel = rc.calcRelatednessOfSynset(fatal, deadly);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(fatal, dead);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(shoot, injure);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(hit, injure);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(investigate, injure);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(news, injury);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(injury, injure);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(fatal, killing);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(killing, death);
		System.out.println(rel.getScore());
		rel = rc.calcRelatednessOfSynset(murder, killing);
		System.out.println(rel.getScore());
		
		rel = rc.calcRelatednessOfSynset(fatally, fatal);
		System.out.println(rel.getScore());
		
		System.out.println("---");
		SemanticRelatedness semrel = new SemanticRelatedness("01323958-v", "02636810-v");
		System.out.println(semrel.getHsoScore());
		
	}

	public double getHsoScore() {
		return hsoScore;
	}

	public void setHsoScore(double hsoScore) {
		this.hsoScore = hsoScore;
	}

	public double getPathScore() {
		return pathScore;
	}

	public void setPathScore(double pathScore) {
		this.pathScore = pathScore;
	}

	public double getWupScore() {
		return wupScore;
	}

	public void setWupScore(double wupScore) {
		this.wupScore = wupScore;
	}

}
