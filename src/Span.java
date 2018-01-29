
public class Span {

	private String text;
	private int sentIndex;
	private int startOffset;
	private int endOffset;
	private String type;
	private String dbpediaUrl;
	
	public Span() {
		
	}
	
	public Span(String text, int start, int end) {
		this();
		this.setText(text);
		this.setStartOffset(start);
		this.setEndOffset(end);
	}
	
	public Span(String text, int start, int end, String type) {
		this();
		this.setText(text);
		this.setStartOffset(start);
		this.setEndOffset(end);
		this.setType(type);
	}
	
	public Span(String text, int sent, int start, int end, String type) {
		this();
		this.setText(text);
		this.setSentIndex(sent);
		this.setStartOffset(start);
		this.setEndOffset(end);
		this.setType(type);
	}
	
	public String toString() {
		String span = "";
		
		if (type != null) span += type + ":";
		span += text + "[";
		if (sentIndex >= 0) span += sentIndex + ":";
		span += startOffset + "," + endOffset + "]";
		if (dbpediaUrl != null) span += " " + dbpediaUrl;
		
		return span;
	}
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + sentIndex;
        result = prime * result + startOffset;
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
        final Span other = (Span) obj;
        if (sentIndex != other.sentIndex)
            return false;
        else {
        	if (startOffset != other.startOffset
        			|| endOffset != other.endOffset) {
        		return false;
        	}
        }
        return true;
    }
	
	public boolean isIncludedIn(Span s) {
		if (this.startOffset >= s.startOffset
				&& this.endOffset <= s.endOffset)
			return true;
		else
			return false;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public int getStartOffset() {
		return startOffset;
	}
	
	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}
	
	public int getEndOffset() {
		return endOffset;
	}
	
	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getSentIndex() {
		return sentIndex;
	}

	public void setSentIndex(int sentIndex) {
		this.sentIndex = sentIndex;
	}

	public String getDbpediaUrl() {
		return dbpediaUrl;
	}

	public void setDbpediaUrl(String dbpediaUrl) {
		this.dbpediaUrl = dbpediaUrl;
	}
	
	
	
}
