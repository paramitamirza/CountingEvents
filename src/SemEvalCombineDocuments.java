import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class SemEvalCombineDocuments {
	
	public static void main(String[] args) throws Exception {
		
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

}
