package dgvis.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import dgvis.beans.Vertex;
import dgvis.database.DatabaseConnector;
import dgvis.util.Common;


public class Parser {
	
	public static int FILE_NO_ROWS; 
	public Parser() {
		
	}
		
	public Map<Integer,Vertex> getVertexList(File file,ServletContext context){
		Map<Integer,Vertex> vertexMap = new HashMap<Integer,Vertex>();
		//List<Integer> outNeighbors, inNeighbors, inNeighborsWeights, outNeighborsWeights;
		//int weight, index;
		try {
			
			String dateCell, timeCell, srcCell, destCell;
			int src=0,dest=0;
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			
			
		    int r=0; // row number
		    
		    
			br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] row = line.split(cvsSplitBy);
                dateCell = row[0];
                timeCell = row[3];
                timeCell = timeCell.substring(1, timeCell.length()-1);	// that is if the time is written between double quotes
                srcCell = row[1];
                destCell = row[2];
                
                dateCell= dateCell.trim();
                if(dateCell!=null && !dateCell.isEmpty()
                		&& timeCell!=null && !timeCell.trim().isEmpty() && timeCell.length()>0 // to avoid crap time format
                		&& srcCell!=null && !srcCell.trim().isEmpty() && Common.isInteger(srcCell,10)
                		&& destCell!=null && !destCell.trim().isEmpty() && Common.isInteger(destCell,10)){
                	src = Integer.parseInt(srcCell);
                	dest = Integer.parseInt(destCell);
            		
                	HandleOutEdges(vertexMap, src, dest, 1);
                	HandleInEdges(vertexMap, src, dest, 1);
                }
                
                r++;

            }
		    FILE_NO_ROWS = r;	// set total number of rows exist in file
		    br.close();
		} catch(Exception ioe) {
		    ioe.printStackTrace();
		}
		return vertexMap;
	}
	
	List<Integer> outNeighbors, inNeighbors, inNeighborsWeights, outNeighborsWeights;
	int weight, index;
	public void HandleOutEdges(Map<Integer,Vertex> vertexMap, int src,int dest, int w){
		Vertex srcVertex=vertexMap.get(src);
		if(srcVertex!=null){
    		outNeighbors = srcVertex.getOutNeighbors();
    		if(outNeighbors!=null){
    			index = outNeighbors.indexOf(dest);
    			if(index!=-1){	// already exists ==> increase weight
    				weight = srcVertex.getOutNeighborsWeights().get(index);
    				srcVertex.getOutNeighborsWeights().set(index, weight + w);
    			}else{
    				outNeighbors.add(dest);
    				srcVertex.getOutNeighborsWeights().add(w);
    			}
				
    		}else{
    			outNeighbors = new ArrayList<Integer>();
    			outNeighborsWeights = new ArrayList<Integer>();
    			outNeighbors.add(dest);
    			outNeighborsWeights.add(w);	
    			srcVertex.setOutNeighbors(outNeighbors);
    			srcVertex.setOutNeighborsWeights(outNeighborsWeights);
    		}
    	}else{
    		srcVertex = new Vertex(src, "");
    		outNeighbors = new ArrayList<Integer>();
    		outNeighborsWeights = new ArrayList<Integer>();
    		outNeighbors.add(dest);
    		outNeighborsWeights.add(w);	
    		srcVertex.setOutNeighbors(outNeighbors);
    		srcVertex.setOutNeighborsWeights(outNeighborsWeights);
    		vertexMap.put(src, srcVertex);
    	}
	}
	
	public void HandleInEdges(Map<Integer,Vertex> vertexMap, int src,int dest, int w){
		Vertex destVertex=vertexMap.get(dest);
		if(destVertex!=null){
    		inNeighbors =  destVertex.getInNeighbors();
    		if(inNeighbors!=null){
    			index = inNeighbors.indexOf(src);
    			if(index!=-1){	// already exists ==> increase weight
    				weight = destVertex.getInNeighborsWeights().get(index);
    				destVertex.getInNeighborsWeights().set(index, weight + w);
    			}else{
    				inNeighbors.add(src);
    				destVertex.getInNeighborsWeights().add(w);
    			}
    			
    		}else{
    			inNeighbors = new ArrayList<Integer>();
    			inNeighborsWeights = new ArrayList<Integer>();
    			inNeighbors.add(src);
    			inNeighborsWeights.add(w);
    			destVertex.setInNeighbors(inNeighbors);
    			destVertex.setInNeighborsWeights(inNeighborsWeights);
    		}
    	}else{
    		destVertex = new Vertex(dest, "");
    		inNeighbors =  new ArrayList<Integer>();
    		inNeighborsWeights = new ArrayList<Integer>();
    		inNeighbors.add(src);
    		inNeighborsWeights.add(w);
    		destVertex.setInNeighbors(inNeighbors);
    		destVertex.setInNeighborsWeights(inNeighborsWeights);
    		vertexMap.put(dest, destVertex);
    	}
	}
	

}
