import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class SemEvalCombineDocuments {
	
	public static void combineAnswers() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("./data/trial_data_final/similar_doc_0.1.tsv"));
		Map<String, Double> docSims = new HashMap<String, Double>();
		
		String line = br.readLine();
		while (line != null) {
			
//			System.out.println(line);
			String[] cols = line.split("\t");
			docSims.put(cols[0] + "," + cols[1], Double.parseDouble(cols[2]));
			
			line = br.readLine();
		}
		
		br.close();
		
		br = new BufferedReader(new FileReader("./data/trial_data_final/similar_ent_0.1.tsv"));
		Map<String, Double> entSims = new HashMap<String, Double>();
		
		line = br.readLine();
		while (line != null) {
			
//			System.out.println(line);
			String[] cols = line.split("\t");
			entSims.put(cols[0] + "," + cols[1], Double.parseDouble(cols[2]));
			
			line = br.readLine();
		}
		
		br.close();
		
		br = new BufferedReader(new FileReader("/local/home/paramita/git/CountingEvents/data/trial_data_final/submission_v4/s2/answers.json"));
		String jsonStr = "";
		line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    JSONObject obj = new JSONObject(jsonStr);
	    JSONObject newobj = new JSONObject();
	 
	    // get the first result
	    Iterator<String> keys = obj.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject answer = obj.getJSONObject(key);
	        JSONArray docs = answer.getJSONArray("answer_docs");
	        Integer numAnswers = answer.getInt("numerical_answer");
	        
	        Map<Integer, String> documents = new HashMap<Integer, String>();
	        for (int i=0; i<docs.length(); i++) {
	        	String doc = docs.getString(i);
	        	documents.put(i, doc);
	        }
	        
	        int count = 0;
	        List<Integer> currCluster = new ArrayList<Integer>();
	        while (!documents.isEmpty()) {
	        	count ++;
	        	currCluster.clear();
	        	
	        	Iterator<Map.Entry<Integer, String>> it = documents.entrySet().iterator();
	        	Map.Entry<Integer, String> pair = it.next();
	        	currCluster.add(pair.getKey());
	        	
	        	while (it.hasNext()) {
	        	    Map.Entry<Integer, String> next = it.next();
	        	    double docSim = 0.0;
	        	    double entSim = 0.0;
	        	    if (docSims.containsKey(pair.getValue() + "," + next.getValue())) {
	        	    	docSim = docSims.get(pair.getValue() + "," + next.getValue()); 
	        	    }
	        	    if (entSims.containsKey(pair.getValue() + "," + next.getValue())) {
	        	    	entSim = entSims.get(pair.getValue() + "," + next.getValue()); 
	        	    }
	        	    
	        	    if (((docSim + entSim) / 2) > 0.40) {
        	    		currCluster.add(next.getKey());
        	    	}
	        	}
	        	System.out.println(key + "----" + count + "----" + currCluster);
	        	for (Integer id : currCluster) {
	        		System.out.println("   -" + documents.get(id));
	        		documents.remove(id);
	        	}
	        }
	        
	        answer.remove("numerical_answer");
	        answer.put("numerical_answer", count);
	        
	        newobj.put(key, answer);
	    }
	    
	    br.close();
	    
	    File file = new File("/local/home/paramita/git/CountingEvents/data/trial_data_final/submission_v4/s2/answers.json");
	    File file2 = new File("/local/home/paramita/git/CountingEvents/data/trial_data_final/submission_v4/s2/answers_old.json");
	    boolean success = file.renameTo(file2);
	    
	    try (FileWriter jsonfile = new FileWriter("/local/home/paramita/git/CountingEvents/data/trial_data_final/submission_v4/s2/answers.json")) {
	    	jsonfile.write(newobj.toString(4));
		}
		
		br.close();
	}
	
	public static void compareAnswers(String answerFile1, String answerFile2, String questionFile) throws IOException {
		
		Map<String, List<String>> documents1 = new HashMap<String, List<String>>();
		
		BufferedReader br = new BufferedReader(new FileReader(answerFile1));
		String jsonStr = "";
		String line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    JSONObject obj = new JSONObject(jsonStr);
	 
	    // get the first result
	    Iterator<String> keys = obj.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject answer = obj.getJSONObject(key);
	        JSONArray docs = answer.getJSONArray("answer_docs");
	        Integer numAnswers = answer.getInt("numerical_answer");
	        
	        List<String> docList = new ArrayList<String>();
	        for (int i=0; i<docs.length(); i++) {
	        	String doc = docs.getString(i);
	        	docList.add(doc);
	        }
	        
	        documents1.put(key, docList);
	        
	    }
	    
	    Map<String, List<String>> documents2 = new HashMap<String, List<String>>();
		
		br = new BufferedReader(new FileReader(answerFile2));
		jsonStr = "";
		line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    obj = new JSONObject(jsonStr);
	 
	    // get the first result
	    keys = obj.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject answer = obj.getJSONObject(key);
	        JSONArray docs = answer.getJSONArray("answer_docs");
	        Integer numAnswers = answer.getInt("numerical_answer");
	        
	        List<String> docList = new ArrayList<String>();
	        for (int i=0; i<docs.length(); i++) {
	        	String doc = docs.getString(i);
	        	docList.add(doc);
	        }
	        
	        documents2.put(key, docList);
	        
	    }
	    
	    br = new BufferedReader(new FileReader(questionFile));
		jsonStr = "";
		line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    obj = new JSONObject(jsonStr);
	 
	    // get the first result
	    keys = obj.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject question = obj.getJSONObject(key);
	        String verbose = question.getString("verbose_question");
	        
	        int doc1Size = 0;
	        if (documents1.containsKey(key)) doc1Size = documents1.get(key).size();
	        int doc2Size = 0;
	        if (documents2.containsKey(key)) doc2Size = documents2.get(key).size();
	        
	        System.out.println(key + "\t" + verbose + "\t" + doc1Size + "\t" + doc2Size + "\t" + documents1.get(key) + "\t" + documents2.get(key));
	        
	    }
		
	}
	
	public static void compareAnswersGold(String answerGold, String answerFile2, String questionFile) throws IOException {
		
		BufferedWriter bw = new BufferedWriter(new FileWriter("/local/home/paramita/git/CountingEvents/data/test_data_gold/input/s1/compare_gold.tsv"));
		
		Map<String, List<String>> documents1 = new HashMap<String, List<String>>();
		
		BufferedReader br = new BufferedReader(new FileReader(answerGold));
		String jsonStr = "";
		String line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    JSONObject obj = new JSONObject(jsonStr);
	 
	    // get the first result
	    Iterator<String> keys = obj.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject answer = obj.getJSONObject(key);
	        
	        JSONObject docs = answer.getJSONObject("answer_docs");
	        
	        List<String> docList = new ArrayList<String>();
	        
	        Iterator<String> answerKeys = docs.keys();	        
	        while( answerKeys.hasNext() ) {
	        	String answerKey = (String)answerKeys.next();
	        	JSONArray docsPerKey = docs.getJSONArray(answerKey);		        
		        
		        for (int i=0; i<docsPerKey.length(); i++) {
		        	String doc = docsPerKey.getString(i);
		        	docList.add(doc);
		        }	        
	        }
	        Integer numAnswers = answer.getInt("numerical_answer");
	        
	        documents1.put(key, docList);
	        
	    }
	    
	    Map<String, List<String>> documents2 = new HashMap<String, List<String>>();
		
		br = new BufferedReader(new FileReader(answerFile2));
		jsonStr = "";
		line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    obj = new JSONObject(jsonStr);
	 
	    // get the first result
	    keys = obj.keys();
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject answer = obj.getJSONObject(key);
	        JSONArray docs = answer.getJSONArray("answer_docs");
	        Integer numAnswers = answer.getInt("numerical_answer");
	        
	        List<String> docList = new ArrayList<String>();
	        for (int i=0; i<docs.length(); i++) {
	        	String doc = docs.getString(i);
	        	docList.add(doc);
	        }
	        
	        documents2.put(key, docList);
	        
	    }
	    
	    br = new BufferedReader(new FileReader(questionFile));
		jsonStr = "";
		line = br.readLine();
		while (line != null) {
			jsonStr += line;
			line = br.readLine();
		}
		
		// build a JSON object
	    obj = new JSONObject(jsonStr);
	 
	    // get the first result
	    keys = obj.keys();
	    
	    int numQ = 0;
	    
	    while( keys.hasNext() ) {
	        String key = (String)keys.next();
	        JSONObject question = obj.getJSONObject(key);
	        String verbose = question.getString("verbose_question");
	        
	        String loc = "";
	        if (question.has("location")) loc = "location";
	        String part = "";
	        if (question.has("participant")) part = "participant";
	        String time = "";
	        if (question.has("time")) time = "time";
	        
	        int doc1Size = 0;
	        if (documents1.containsKey(key)) doc1Size = documents1.get(key).size();
	        int doc2Size = 0;
	        if (documents2.containsKey(key)) doc2Size = documents2.get(key).size();
	        
	        List<String> a = new ArrayList<String>();
	        if (documents1.containsKey(key)) a = documents1.get(key);
	        List<String> b = new ArrayList<String>();
	        if (documents2.containsKey(key)) b = documents2.get(key);
	        
	        //intersection
	        List<String> intersection = new ArrayList<String> (a.size() > b.size() ?a.size():b.size());
	        intersection.addAll(a);
	        intersection.retainAll(b);

	        //difference a-b
	        List<String> diff = new ArrayList<String> (a.size());
	        diff.addAll(a);
	        diff.removeAll(b);
	        
//	        System.out.println(key + "\t" + verbose + 
//	        		"\t" + loc + "\t" + part + "\t" + time +  
//	        		"\t" + doc1Size + "\t" + doc2Size + 
//	        		"\t" + intersection.size() + "\t" + diff.size() + 
//	        		"\t" + documents1.get(key) + "\t" + documents2.get(key));
	        
	        bw.write(key + "\t" + verbose + 
	        		"\t" + loc + "\t" + part + "\t" + time +  
	        		"\t" + doc1Size + "\t" + doc2Size + 
	        		"\t" + intersection.size() + "\t" + diff.size() + 
	        		"\t" + documents1.get(key) + "\t" + documents2.get(key) + "\n");
	        numQ ++;
	        
	    }
		System.out.println(numQ);
		bw.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		compareAnswersGold("/local/home/paramita/git/CountingEvents/data/test_data_gold/dev_data/s3/answers.json", 
				"/local/home/paramita/git/CountingEvents/data/test_data/posteval_022/s3/answers.json", 
				"/local/home/paramita/git/CountingEvents/data/test_data_gold/input/s3/questions.json");
	}

}
