import java.util.ArrayList;
import java.util.List;

public class Predicate {

	private Span predicate;
	private List<Span> arguments;
	private List<Span> persons;
	
	public Predicate() {
		arguments = new ArrayList<Span>();
		persons = new ArrayList<Span>();
	}
	
	public Predicate(Predicate p) {
		this.predicate = p.predicate;
		this.arguments = p.arguments;
		this.persons = p.persons;
	}
	
	public Predicate(String predText, int sentIdx, int startOffset, int endOffset) {
		predicate.setText(predText);
		predicate.setSentIndex(sentIdx);
		predicate.setStartOffset(startOffset);
		predicate.setEndOffset(endOffset);
		arguments = new ArrayList<Span>();
		persons = new ArrayList<Span>();
	}
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result;
        return result;
    }
	
	@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Predicate other = (Predicate) obj;
        if (predicate != other.predicate)
            return false;
        return true;
    }
	
	public String toString() {
		String predStr = "";
		if (predicate != null) predStr += predicate.toString();
		
		for (Span arg : arguments) {
			predStr += "\t" + arg.toString();
		}
		
		return predStr;
		
	}
	
	public String toString2() {
		String predStr = "";
		if (predicate != null) predStr += predicate.getText() + 
				"\t" + predicate.getSentIndex() + 
				"\t" + predicate.getStartOffset() + "," + predicate.getEndOffset();
		
		List<String> argTypes = new ArrayList<String>();
		argTypes.add("S-AM-NEG");
		argTypes.add("A0"); argTypes.add("A1"); argTypes.add("A2"); argTypes.add("A3"); argTypes.add("A4");
		argTypes.add("AM-TMP"); argTypes.add("AM-LOC"); argTypes.add("AM-MNR"); 
		
		Span[] argValues = new Span[argTypes.size()];
				
		for (Span arg : arguments) {
			if (argTypes.contains(arg.getType())) {
				argValues[argTypes.indexOf(arg.getType())] = arg;
			}
		}
		for (Span arg : argValues) {
			if (arg != null) {
				predStr += "\t" + arg.toString();
			} else {
				predStr += "\t" + "null";
			}
		}
		
		return predStr;
		
	}
	
	public Span getPredicate() {
		return predicate;
	}
	
	public void setPredicate(Span predicate) {
		this.predicate = predicate;
	}
	
	public List<Span> getArguments() {
		return arguments;
	}
	
	public void setArguments(List<Span> arguments) {
		this.arguments = arguments;
	}

	public List<Span> getPersons() {
		return persons;
	}

	public void setPersons(List<Span> persons) {
		this.persons = persons;
	}
}
