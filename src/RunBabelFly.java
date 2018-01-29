
public class RunBabelFly {
	
	public static void main(String[] args) throws Exception {
			
			SemEvalDataParser parser = new SemEvalDataParser();
			
			parser.writeBabelOutput(args[0], args[1]);
						
	}

}
