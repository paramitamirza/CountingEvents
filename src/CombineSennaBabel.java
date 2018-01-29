
public class CombineSennaBabel {
	
	public static void main(String[] args) throws Exception {
		
		SemEvalDataParser parser = new SemEvalDataParser();
		
		parser.extractWordNet(args[0], 
				args[1], 
				args[2],
				args[3],
				args[4],
				Integer.parseInt(args[5]));
		
		parser.extractBabelNet(args[0], 
				args[1], 
				args[2],
				args[3],
				args[4],
				Integer.parseInt(args[5]));
		
//		parser.combineSennaBabelOutput(args[0], 
//				args[1], 
//				args[2],
//				args[3],
//				args[4],
//				Integer.parseInt(args[5]));
		
		
	}

}
