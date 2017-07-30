package dgvis.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import dgvis.beans.Vertex;

public class Common {
	
	public static double getJaccardDistance(List<Integer> a, List<Integer>b){
		if(a != null && b!= null){
			List<Integer> intersection = new ArrayList<Integer>(a);
			intersection.retainAll(b);
		    return 1.0 - (intersection.size() / (double)(a.size() + b.size() - intersection.size()));
		}
		return 1.0;
	}
	/*
	public static double getWeightedJaccardDistance(List<Integer> a, List<Integer>b, List<Integer>aWeights, List<Integer>bWeights){
		if(a != null && b!= null){
			List<Integer> intersection = new ArrayList<Integer>(a);
			intersection.retainAll(b);
			double sum=0.0;
			int index1, index2;
			for(int e:intersection){
				index1 = a.indexOf(e);
				index2 = b.indexOf(e);
				sum+=Math.min(aWeights.get(index1), bWeights.get(index2))/(double)Math.max(aWeights.get(index1), bWeights.get(index2));
			}
		    return 1.0 - (sum / (double)(a.size() + b.size() - intersection.size()));
		}
		return 1.0;
	}
	*/
	public static double getWeightedJaccardDistance2(List<Integer> a, List<Integer>b, List<Integer>aWeights, List<Integer>bWeights){
		if(a != null && b!= null){
			List<Integer> all = union(a,b);
			
			double nominatorSum=0.0, denominatorSum=0.0;
			int index1, index2;
			for(int e:all){
				index1 = a.indexOf(e);
				index2 = b.indexOf(e);
				
				int w1 = index1==-1?0:aWeights.get(index1);
				int w2 = index2==-1?0:bWeights.get(index2);
				
				nominatorSum+=Math.min(w1, w2);
				denominatorSum+=Math.max(w1, w2);
			}
		    return 1.0 - (nominatorSum /(double)denominatorSum);
		}
		return 1.0;
	}
	
	public static String getCommaSeparatedString(List<String> elements){
		if (elements.size() > 0) {
		    StringBuilder nameBuilder = new StringBuilder();

		    for (String n : elements) {
		        nameBuilder.append(n).append(",");
		        // can also do the following
		        // nameBuilder.append("'").append(n.replace("'", "''")).append("',");
		    }

		    nameBuilder.deleteCharAt(nameBuilder.length() - 1);

		    return nameBuilder.toString();
		} else {
		    return "";
		}
	}
	
	public static int countOutEdges(Vertex v){
		int count = 0;
		if(v.getOutNeighbors()!=null){
			for(int i = 0; i < v.getOutNeighbors().size(); i++){
				count += v.getOutNeighborsWeights().get(i);
			}
		}
		
		return count;
	}

	public static int countInEdges(Vertex v){
		int count = 0;
		if(v.getInNeighbors()!=null){
			for(int i = 0; i < v.getInNeighbors().size(); i++){
				count += v.getInNeighborsWeights().get(i);
			}
		}
		
		return count;
	}
	
	public static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}
	
	
	public int normalize(int a, int b, int max, int min, int x){
		return (((b-a)*(x-min))/(max-min))+a;
	}
	
	public static  int getBrightness(Color c) {
	    return (int) Math.sqrt(
	      c.getRed() * c.getRed() * .241 +
	      c.getGreen() * c.getGreen() * .691 +
	      c.getBlue() * c.getBlue() * .068);
	}
	
	public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }
	
	private static final NavigableMap<Long, String> suffixes = new TreeMap<> ();
	static {
	  suffixes.put(1_000L, "k");
	  suffixes.put(1_000_000L, "M");
	  suffixes.put(1_000_000_000L, "G");
	  suffixes.put(1_000_000_000_000L, "T");
	  suffixes.put(1_000_000_000_000_000L, "P");
	  suffixes.put(1_000_000_000_000_000_000L, "E");
	}

	public static String format(long value) {
	  //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
	  if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
	  if (value < 0) return "-" + format(-value);
	  if (value < 1000) return Long.toString(value); //deal with easy case

	  Entry<Long, String> e = suffixes.floorEntry(value);
	  Long divideBy = e.getKey();
	  String suffix = e.getValue();

	  long truncated = value / (divideBy / 10); //the number part of the output times 10
	  boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
	  return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
	}

}
