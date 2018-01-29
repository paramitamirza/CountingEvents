

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.simple.Sentence;

public class Numbers {
	
	private static String[] digitsArr = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", 
			"eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
	private static String[] tensArr = {"", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
	private static String[] ordinalsArr = {"", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth", 
			"eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth", "twentieth"};
	private static String[] tenOrdinalsArr = {"", "tenth", "twentieth", "thirtieth", "fortieth", "fiftieth", "sixtieth", "seventieth", "eightieth", "ninetieth"};
	
	public static List<String> digits = Arrays.asList(digitsArr);
	public static List<String> tens = Arrays.asList(tensArr);
	public static List<String> ordinals = Arrays.asList(ordinalsArr);
	public static List<String> tenOrdinals = Arrays.asList(tenOrdinalsArr);
	
	public static Map<String, Integer> hundreds = new HashMap<String, Integer>();
	
	public static Long getInteger(String numStr) {
		hundreds.put("hundred", 100);
		hundreds.put("thousand", 1000);
		hundreds.put("million", 1000000);
		
		long number = -999; 
		if (numStr.contains(",")) numStr = numStr.replace(",", "");
		if (numStr.contains("-")) numStr = numStr.replace("-", "_");
		if (numStr.contains("and_")) numStr = numStr.replace("and_", "_");
		String[] words = numStr.split("_");
		
		if (words.length == 4) {
			if (digits.contains(words[0]) && hundreds.containsKey(words[1])
					&& tens.contains(words[2]) && digits.contains(words[3])) {
				number = (digits.indexOf(words[0]) * hundreds.get(words[1])) + (tens.indexOf(words[2]) * 10) + digits.indexOf(words[3]);
			} else if (digits.contains(words[0]) && hundreds.containsKey(words[1])
					&& tens.contains(words[2]) && ordinals.contains(words[3])) {
				number = (digits.indexOf(words[0]) * hundreds.get(words[1])) + (tens.indexOf(words[2]) * 10) + ordinals.indexOf(words[3]);
			}
		} else if (words.length == 3) {
			if (hundreds.containsKey(words[0])
					&& tens.contains(words[1]) && digits.contains(words[2])) {
				number = (1 * hundreds.get(words[0])) + (tens.indexOf(words[1]) * 10) + digits.indexOf(words[2]);
			} else if (hundreds.containsKey(words[0])
					&& tens.contains(words[1]) && ordinals.contains(words[2])) {
				number = (1 * hundreds.get(words[0])) + (tens.indexOf(words[1]) * 10) + ordinals.indexOf(words[2]);
			}
		} else if (words.length == 2) {
			if (tens.contains(words[0]) && digits.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + digits.indexOf(words[1]);
			} else if (tens.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = (tens.indexOf(words[0]) * 10) * hundreds.get(words[1]);
			} else if (digits.contains(words[0]) && hundreds.containsKey(words[1])) {
				number = digits.indexOf(words[0]) * hundreds.get(words[1]);
			} else if (words[0].matches("^-?\\d+\\.?\\d*$") && hundreds.containsKey(words[1])) {
				number = new Float(Float.parseFloat(words[0]) * hundreds.get(words[1])).longValue();
			} else if (tens.contains(words[0]) && ordinals.contains(words[1])) {
				number = (tens.indexOf(words[0]) * 10) + ordinals.indexOf(words[1]);
			}
		} else {
			if (tens.contains(numStr)) number = tens.indexOf(numStr) * 10;
			else if (digits.contains(numStr)) number = digits.indexOf(numStr);
			else if (hundreds.containsKey(numStr)) number = hundreds.get(numStr);
			else if (numStr.matches("^-?\\d+$")) number = new Long(Long.parseLong(numStr));
			else if (ordinals.contains(numStr)) number = ordinals.indexOf(numStr);
			else if (tenOrdinals.contains(numStr)) number = tenOrdinals.indexOf(numStr) * 10;
			else if (numStr.matches("^-?\\d+st$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
			else if (numStr.matches("^-?\\d+nd$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
			else if (numStr.matches("^-?\\d+rd$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
			else if (numStr.matches("^-?\\d+th$")) number = new Long(Long.parseLong(numStr.substring(0, numStr.length()-2)));
		}
		
		return number;
	}

}
