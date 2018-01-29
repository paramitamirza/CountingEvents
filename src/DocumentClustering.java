import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentClustering {
	
	private static double threshold = 0.5;
	
	public static List<Set<String>> getClusters(String docSimFilepath, String entSimFilepath, String topicFile) throws IOException {
		List<Set<String>> clusters = new ArrayList<Set<String>>();
		
		BufferedReader br = new BufferedReader(new FileReader(docSimFilepath));
		Map<String, Double> docSims = new HashMap<String, Double>();
		
		String line = br.readLine();
		while (line != null) {
			
//			System.out.println(line);
			String[] cols = line.split("\t");
			docSims.put(cols[0] + "," + cols[1], Double.parseDouble(cols[2]));
			docSims.put(cols[1] + "," + cols[0], Double.parseDouble(cols[2]));
			
			line = br.readLine();
		}
		
		br.close();
		
		br = new BufferedReader(new FileReader(entSimFilepath));
		Map<String, Double> entSims = new HashMap<String, Double>();
		
		line = br.readLine();
		while (line != null) {
			
//			System.out.println(line);
			String[] cols = line.split("\t");
			entSims.put(cols[0] + "," + cols[1], Double.parseDouble(cols[2]));
			entSims.put(cols[1] + "," + cols[0], Double.parseDouble(cols[2]));
			
			line = br.readLine();
		}
		
		br.close();
		
		br = new BufferedReader(new FileReader(topicFile));
		Set<String> filenames = new HashSet<String>();
		Map<Integer, String> documents = new HashMap<Integer, String>();
		
		line = br.readLine();
		while (line != null) {
			String[] cols = line.split("\t");
			filenames.add(cols[0]);
			line = br.readLine();
		}
		
		int n = 0;
		for (String filename : filenames) {
			documents.put(n, filename);
//			clusters.put(filename, new HashSet<String>());
			n ++;
		}
		
		List<Integer> currCluster = new ArrayList<Integer>();
        while (!documents.isEmpty()) {
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
        	    
//        	    double sim = ((docSim + entSim) / 2);
        	    double sim = Math.max(docSim, entSim);
        	    if (sim > threshold) {
    	    		currCluster.add(next.getKey());
    	    	}
        	}
        	Set<String> cluster = new HashSet<String>();
        	
        	for (Integer id : currCluster) {
        		cluster.add(documents.get(id));
        		documents.remove(id);
        	}
        	clusters.add(cluster);
        }
		
		return clusters;
	}
	
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
		
		List<String> eventTypes = new ArrayList<String>();
		eventTypes.add("injuring");
		eventTypes.add("killing");
		eventTypes.add("fire");
		eventTypes.add("displace");
		
		for (String eventType : eventTypes) {
			
			Map<String, Set<String>> clusters = new HashMap<String, Set<String>>();
		
			br = new BufferedReader(new FileReader("/local/home/paramita/git/CountingEvents/data/trial_data_final/topics_v1_" + eventType + ".tsv"));
			Set<String> filenames = new HashSet<String>();
			Map<Integer, String> documents = new HashMap<Integer, String>();
			
			line = br.readLine();
			while (line != null) {
				String[] cols = line.split("\t");
				filenames.add(cols[0]);
				line = br.readLine();
			}
			
			int n = 0;
			for (String filename : filenames) {
				documents.put(n, filename);
				clusters.put(filename, new HashSet<String>());
				n ++;
			}
			
			System.out.println(eventType);
			System.out.println(documents.size() + "--" + documents);
			
			List<Integer> currCluster = new ArrayList<Integer>();
	        while (!documents.isEmpty()) {
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
	        	    
//	        	    double sim = ((docSim + entSim) / 2);
	        	    double sim = Math.max(docSim, entSim);
	        	    if (sim > 0.50) {
        	    		currCluster.add(next.getKey());
        	    	}
	        	}
	        	System.out.println(currCluster);
	        	
	        	for (Integer id : currCluster) {
	        		for (Integer idd : currCluster) {
	        			if (id != idd) {
	        				clusters.get(documents.get(id)).add(documents.get(idd));
	        			}
	        		}
	        		System.out.println(clusters.get(documents.get(id)));
	        	}
	        	for (Integer id : currCluster) {
	        		documents.remove(id);
	        	}
	        }
	        
//	        System.out.println(eventType);
//	        System.out.println(clusters);
		}
	}

}
