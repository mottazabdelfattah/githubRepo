package dgvis.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dgvis.beans.Vertex;
import dgvis.util.Common;
import dgvis.vis.DynamicGraph;
import jdk.jfr.events.FileWriteEvent;

public class CustomParser {
	public void writeCustomFormat(File file, File out,ServletContext context){
		try {
			
			String dateCell, timeCell, srcCell, destCell;
			int src=0,dest=0,time=0;
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			
			TreeMap<Integer, HashMap<String, Integer>> aggWeightMap = new TreeMap<Integer,HashMap<String, Integer>>();
			
		    int r=0; // row number
		    DynamicGraph dg  = new DynamicGraph(context);
		    
		    
		    PrintWriter pw = new PrintWriter(out);
		    
		    DateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		    Date date = inputFormatter.parse(DynamicGraph.START_DATE);
		    DateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd");
		    String currentDate = outputFormatter.format(date);
			System.out.println(currentDate);
			
			br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
            	if(r==0){
            		pw.println("time start end weight");	//header
            		pw.println(); 	// as format specified by michael
            		r++;
            	}else{
	                // use comma as separator
	                String[] row = line.split(cvsSplitBy);
	                dateCell = row[0].trim();
	                timeCell = row[3];
	                timeCell = timeCell.substring(1, timeCell.length()-1);	// that is if the time is written between double quotes
	                srcCell = row[1];
	                destCell = row[2];
	                
	                if(dateCell!=null && !dateCell.trim().isEmpty()
	                		&& timeCell!=null && !timeCell.trim().isEmpty() && timeCell.length()>0 // to avoid crap time format
	                		&& srcCell!=null && !srcCell.trim().isEmpty() && Common.isInteger(srcCell,10)
	                		&& destCell!=null && !destCell.trim().isEmpty() && Common.isInteger(destCell,10)){
	                	src = Integer.parseInt(srcCell);
	                	dest = Integer.parseInt(destCell);
	                	
	                	
	                	if(!dateCell.equals(currentDate)){	// write the map to file and clear to save memory
	            			for(Map.Entry<Integer, HashMap<String, Integer>> entry:aggWeightMap.entrySet()){
	            				Map<String, Integer> innerMap = entry.getValue();
	            				for(Map.Entry<String, Integer> innerEntry:innerMap.entrySet()){
	            					pw.println(innerEntry.getKey()+" "+innerEntry.getValue());
	            					r++;
	            				}
	            			}
	            			currentDate = dateCell;
	            			aggWeightMap.clear();
	            		}
	                	String timeStr=timeCell;
	                	int diff = 4 - timeStr.length();	// 4 is the max size time string can be
	            		if(diff>0){	// zero-padding for hours
	            			String zeros="";
	            			for(int i=0;i<diff;i++)
	            				zeros+="0";
	            			timeStr = zeros+timeStr;
	            		}else if(timeStr.equals("2400")){
	            			timeStr="2359";
	            		}
	            		timeStr = timeStr.substring(0, 2)+":"+timeStr.substring(2, timeStr.length());
	            		
	            		dateCell = dateCell+" "+timeStr;
	            		int timeStep = dg.getTimeStepsCount(dateCell, DynamicGraph.HOUR_LEVEL)+1;
	            		src = src%10000;
	            		dest = dest%10000;
	            		
	            		// compute weight
	            		String key=timeStep+" "+src+" "+dest+"";
	            		HashMap<String, Integer> innerMap = aggWeightMap.get(timeStep);
		                if(innerMap==null){
		                	innerMap = new HashMap<String,Integer>();
		                	innerMap.put(key, 1);
		                	aggWeightMap.put(timeStep, innerMap);
		                }else{
		                	Integer weight = innerMap.get(key);
		                	if(weight==null){
		                		innerMap.put(key, 1);
			                }else{
			                	innerMap.put(key, ++weight);
			                }
		                }
	            		
	            		
	                }
            	}

            }
            
            //flush the remaining
            for(Map.Entry<Integer, HashMap<String, Integer>> entry:aggWeightMap.entrySet()){
				Map<String, Integer> innerMap = entry.getValue();
				for(Map.Entry<String, Integer> innerEntry:innerMap.entrySet()){
					pw.println(innerEntry.getKey()+" "+innerEntry.getValue());
					r++;
				}
			}
			aggWeightMap.clear();
            pw.flush();
            pw.close();
            System.out.println("number of rows: "+r);
		    
		} catch(Exception ioe) {
		    ioe.printStackTrace();
		}
	}
	// 11650827 records for two years of data
	public void buildLargeDataSetFile(File dir, File out){
		try {
			PrintWriter pw = new PrintWriter(out);
			pw.println("\"FL_DATE\",\"ORIGIN_AIRPORT_ID\",\"DEST_AIRPORT_ID\",\"DEP_TIME\",");
			BufferedReader br = null;
			String line ="";
			int r=0;
			
			for (int f=1;f<=24;f++) {
				File child = new File(dir+"/"+f+".csv");
				if(child.exists()){
					System.out.println(child.getName());
					//if(!child.getName().equals("2000-2001.csv")){
						br = new BufferedReader(new FileReader(child));
						br.readLine();	// skip header
						int i=0;
						while((line=br.readLine())!=null){
							pw.println(line);
							r++;
							if(i==0){
								System.out.println(line);
								i++;
							}
						}
					//}
				}
				
				
			}
			System.out.println("count rows: "+r);
			pw.close();
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	public void writeAggregatedWeight(File file, File out,ServletContext context){
		try {
			
			String timestep, srcCell, destCell, weightCell;
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ",";
			
			HashMap<String, Integer> aggWeightMap = new HashMap<String,Integer>();
		    int r=0; // row number
		    DynamicGraph dg  = new DynamicGraph(context);
		    
		    
		    PrintWriter pw = new PrintWriter(out);
		    
		    int oldTimeStep=1;
			br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
            	if(r==0){
            		pw.println("time start end weight");	//header
            		pw.println(); 	// as format specified by michael
            	}else{

            		
	                // use comma as separator
	                String[] row = line.split(cvsSplitBy);
	                timestep = row[0];
	                srcCell = row[1];
	                destCell = row[2];
	                weightCell = row[3];
	                
            		if(Integer.parseInt(timestep)!=oldTimeStep){	// write the map to file and clear to save memory
            			for(Map.Entry<String, Integer> entry:aggWeightMap.entrySet()){
            				pw.println(entry.getKey()+" "+entry.getValue());
            			}
            			oldTimeStep = Integer.parseInt(timestep);
            			aggWeightMap.clear();
            		}
	                
	                String key=timestep+" "+srcCell+" "+destCell+"";
	                Integer weight = aggWeightMap.get(key);
	                if(weight==null){
	                	aggWeightMap.put(key, 1);
	                }else{
	                	aggWeightMap.put(key, ++weight);
	                }
	                
	                
            	}
            	
                r++;

            }
            
            //flush the remaining
            for(Map.Entry<String, Integer> entry:aggWeightMap.entrySet()){
				pw.println(entry.getKey()+" "+entry.getValue());
			}
			aggWeightMap.clear();
            pw.flush();
            pw.close();
		    
		} catch(Exception ioe) {
		    ioe.printStackTrace();
		}
	}
	*/
	
	
	
	
}
