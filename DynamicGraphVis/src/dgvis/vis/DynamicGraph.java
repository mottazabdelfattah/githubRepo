package dgvis.vis;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import org.apache.poi.hssf.record.VerticalPageBreakRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import dgvis.beans.ImgVertices;
import dgvis.beans.Vertex;
import dgvis.clustering.Cluster;
import dgvis.clustering.visualization.ClusterComponent;
import dgvis.database.DatabaseHelper;
import dgvis.extraction.Parser;
import dgvis.util.CannyEdgeDetector;
import dgvis.util.Common;
import dgvis.util.MapUtil;


public class DynamicGraph implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String START_DATE= "2000-01-01 00:00";
	public static final String END_DATE= "2002-01-01 00:00";
	public static final int PIXEL_OFFSET = 0;
	public static final int PIXEL_VERTEX_GAP = 2;
	public static final int[] PIXEL_TIME_GAP = new int[]{1,2,2};
	public static final int[] STRIP_WIDTH = new int[]{10,10,10};
	public static final int[] EMPTY_TIME_STEPS = new int[]{0,0,0};	// introduce artificial empty time steps between months/days/hours regardless the value of PIXEL_TIME_GAP
	public static final int MINUTE_LEVEL = 0;
	public static final int HOUR_LEVEL = 1;
	public static final int DAY_LEVEL = 2;
	public static final int MONTH_LEVEL = 3;
	public static final int YEAR_LEVEL = 4;
	
	public static final int TIME_LEVELS = 3;
	
	public static final int INTER_RELATION = 1;
	public static final int INTRA_RELATION = 2;
	
	public static final int DENDROGRAM_CANVAS_WIDTH = 550;
	
	
	public static final int CLUSTER_OFFSET = 1;
	public static final int DEFAULT_CLUSTER_LEVEL = 0;
	//public static int IMAGE_HEIGHT;	// image height is constant regardless the time level
	
	//public static int  GLOBAL_MAX_PIXEL_WEIGHT_T0=0; // max pixel weight overall the image
	public static final int Buffered_Readers_GAP = 10000; 	// gap between two consequtives buffered readers
	public static final int NUMBER_OF_CHAR_PER_LINE = 35;	// apporx. number of characters per line
	BufferedReader[] fileBfs;
	
	public static final int K_SIZE =1;
	public static final int N_SMOOTH = 5;	// number of times image will be smoothed
	float[] bins = {0.0f,0.25f,0.5f,0.75f,1.0f};
	public static int[] timeUnits = new int[]{60,60,24};	// array used when converting between time-scales
	int contourLevels = 5;
	int imgPartWidth = 1000;
	
	private Map<Integer,Vertex> vertexMapGlobal = null;
	DatabaseHelper dbHelper = null;
	private ServletContext context;
	public DynamicGraph(ServletContext context){
		this.context =  context;
		dbHelper = new DatabaseHelper(context);
		
	}
	
	
	public void setVertexMapGlobal(Map<Integer,Vertex> map){
		this.vertexMapGlobal = map;
	}
	
	public Map<Integer,Vertex> getVertexMapGlobal(){
		return this.vertexMapGlobal;
	}
	
	// this method require data set to be sorted at least by days
	public void buildImage(File file){
		try {
			
			int imageHeight = PIXEL_OFFSET + vertexMapGlobal.size() * PIXEL_VERTEX_GAP;
			context.setAttribute("IMAGE_HEIGHT", imageHeight);
			
		    String dateCell="", timeCell, destCell, srcCell;		    
		    int dest=0,time=0, src=0;
		    //String date="";
		    
		    // initialization code
		    String key="";
		    Connection connection = (Connection)context.getAttribute("db");
		    String[] insertQueries = new String[TIME_LEVELS];
		    PreparedStatement[] insertPS = new PreparedStatement[TIME_LEVELS];
		    List<Map<String,Integer>> vertexEdgesWeightsMaps = new ArrayList<Map<String,Integer>>();
		    for(int t=0;t<TIME_LEVELS;t++){
		    	vertexEdgesWeightsMaps.add(new HashMap<String,Integer>());
		    	
		    	insertQueries[t] = "insert into tbl_img_t"+t+"_vertices (vid,img_part,affected_pixels,pixels_weight) values (?,?,?,?) ON DUPLICATE KEY update affected_pixels=concat(affected_pixels, ?), pixels_weight=concat(pixels_weight,?)";
			    insertPS[t] = connection.prepareStatement(insertQueries[t]);
		    }
		    
		    
		    
		    final int batchSize = 50000;
		    int count = 0;
		    
		    connection.setAutoCommit(false);
		    
		    //setFileBufferedReader(Parser.FILE_NO_ROWS, file);	// set file reading buffers
		    
		    String oldDate = "";
		    String line = "";
		    BufferedReader br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
				
		        if(line != null) {
		        	String[] row = line.split(",");
	                dateCell = row[0];
	                timeCell = row[3];
	                timeCell = timeCell.substring(1, timeCell.length()-1);	// that is if the time is written between double quotes
	                destCell = row[2];
	                srcCell = row[1];
	                dateCell= dateCell.trim();
	                
	                if(dateCell!=null && !dateCell.isEmpty()
	                		&& timeCell!=null && !timeCell.trim().isEmpty() && timeCell.length()>0 // to avoid crap time format
	                		&& srcCell!=null && !srcCell.trim().isEmpty() && Common.isInteger(srcCell,10)
	                		&& destCell!=null && !destCell.trim().isEmpty() && Common.isInteger(destCell,10)){
	                
		                if(/*++count > batchSize*/!oldDate.isEmpty()  && !oldDate.equals(dateCell)) {	// push to DB
					    	// build the insert statement
					    	pushToDB(connection, insertPS, vertexEdgesWeightsMaps);
					    	count = 0 ; //reset count
		                }
	                
	                
	                	src = Integer.parseInt(srcCell);
	                	dest = Integer.parseInt(destCell);
		            	time = Integer.parseInt(timeCell);
		            	
		            	oldDate = dateCell;
		            	int srcOrder = vertexMapGlobal.get(src).getOrder();
		            	
		            	
		            	for(int t=TIME_LEVELS-1;t>=0;t--){	// loop over time levels
		            		int timeLevel = t;

		                	Point startPoint = getEdgeStartPixelCoordinates(dateCell,time,srcOrder,timeLevel);
		                	int destOrder = vertexMapGlobal.get(dest).getOrder();
		                	int endYCoordinate = PIXEL_OFFSET + (destOrder*PIXEL_VERTEX_GAP);
		            		
		            		// handle edges weights for the current vertex
		            		key = src+","+startPoint.x+","+startPoint.y+","+endYCoordinate;
		            		Integer eWeight = vertexEdgesWeightsMaps.get(t).get(key);
		            		if(eWeight==null){
		            			vertexEdgesWeightsMaps.get(t).put(key, 1);
		            		}else{
		            			vertexEdgesWeightsMaps.get(t).put(key, ++eWeight);
		            		}
		            	}
			        }
		        }
		    		
		    }
		    
	    	pushToDB(connection, insertPS, vertexEdgesWeightsMaps);
		    connection.setAutoCommit(true);
		    br.close();
		    
		} catch(Exception ioe) {
		    ioe.printStackTrace();
		}
		
	}
	
	private Point getEdgeStartPixelCoordinates(String edgeDate, int edgeTime, int vOrder, int timeLevel){
		String timeStr=""+edgeTime;
		
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
		
		edgeDate = edgeDate+" "+timeStr;
		int startYCoordinate = PIXEL_OFFSET + (vOrder*PIXEL_VERTEX_GAP);
		int timeSteps = getTimeStepsCount(edgeDate,timeLevel);
		int totalSteps = timeSteps + getTotalSizeEmptyTS(timeLevel, edgeDate);//getTimeStepsCount(edgeDate,timeLevel+1) * SEPARATE_TIME_UNITS[timeLevel];
		int startXCoordinate = PIXEL_OFFSET + (totalSteps*PIXEL_TIME_GAP[timeLevel]);
		return new Point(startXCoordinate, startYCoordinate);
	}
	
	public static int getTimeStepsCount(String edgeDate, int timeLevel){
		DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm");
		DateTime startTime = fmt.withZone(DateTimeZone.forID("UTC")).parseDateTime(START_DATE);
		DateTime endTime = fmt.withZone(DateTimeZone.forID("UTC")).parseDateTime(edgeDate);
		PeriodType periodType;
		Period difference;
		int timeSteps=0;
		if(timeLevel==MINUTE_LEVEL){
			periodType = PeriodType.minutes();
			difference = new Period(startTime, endTime, periodType);
			timeSteps = difference.getMinutes();
		}else if(timeLevel==HOUR_LEVEL){
			periodType = PeriodType.hours();
			difference = new Period(startTime, endTime, periodType);
			timeSteps = difference.getHours();
		}else if(timeLevel==DAY_LEVEL){
			periodType = PeriodType.days();
			difference = new Period(startTime, endTime, periodType);
			timeSteps = difference.getDays();
		}else if(timeLevel==MONTH_LEVEL){
			periodType = PeriodType.months();
			difference = new Period(startTime, endTime, periodType);
			timeSteps = difference.getMonths();
		}else if(timeLevel==YEAR_LEVEL){
			periodType = PeriodType.years();
			difference = new Period(startTime, endTime, periodType);
			timeSteps = difference.getYears();
		}
		
		return timeSteps;
	
	}
	
	// this method returns the last hour before the required hour according to the given xPosition
	private static DateTime getNearestStartPoistion(int xCoordinate){
		int tmpTimeSteps, tmpXCoordinate;
		int timeLevel =  MINUTE_LEVEL;
		DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm");
		DateTime startTime = fmt.withZone(DateTimeZone.forID("UTC")).parseDateTime(DynamicGraph.START_DATE);
		int t = TIME_LEVELS-1;
		
		while(t >= 0){
			
			if(t == DAY_LEVEL)
				startTime = startTime.plusMonths(1);
			else if(t == HOUR_LEVEL)
				startTime = startTime.plusDays(1);
			else if(t == MINUTE_LEVEL)
				startTime = startTime.plusHours(1);
			
			int totalEmptyTS = getTotalSizeEmptyTS(timeLevel, startTime.toString(fmt));
			tmpTimeSteps = getTimeStepsCount(startTime.toString(fmt),timeLevel);
			tmpXCoordinate = PIXEL_OFFSET + (tmpTimeSteps + totalEmptyTS)*PIXEL_TIME_GAP[timeLevel];
			
			if(tmpXCoordinate >= xCoordinate){
				if(t == DAY_LEVEL)
					startTime = startTime.minusMonths(1);
				else if(t == HOUR_LEVEL)
					startTime = startTime.minusDays(1);
				else if(t == MINUTE_LEVEL)
					startTime = startTime.minusHours(1);
				
				t--;
			}
			
			
			
		}
		
		
		
		return startTime;
	
	}
	
	
	// this method get actual time steps count taking without empty time steps, returns -1 in empty time steps
	public static int getTimeStepsCountWithoutEmptyTS(int xCoordinate, int timeLevel){
		int timeSteps=0;
		int tmpTimeSteps, tmpXCoordinate;
		
		DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm");
		DateTime startTime = getNearestStartPoistion(xCoordinate);
		
		DateTime startTimeMinusOneMinute = startTime.minusMinutes(1);	// get last day of first month
		int totalEmptyTS = getTotalSizeEmptyTS(timeLevel, startTime.toString(fmt));//SEPARATE_TIME_UNITS[timeLevel];
		int emptyTSCurrentStep = totalEmptyTS;
		while(true){
			tmpTimeSteps = getTimeStepsCount(startTimeMinusOneMinute.toString(fmt),timeLevel);
			tmpXCoordinate = PIXEL_OFFSET + (tmpTimeSteps + totalEmptyTS)*PIXEL_TIME_GAP[timeLevel];
			
			if(tmpXCoordinate >= xCoordinate){
				int emptyTSWidth = emptyTSCurrentStep * PIXEL_TIME_GAP[timeLevel];
				xCoordinate = Math.min(tmpXCoordinate - emptyTSWidth, xCoordinate);	// Truncate x-coordinates in gap area to last x-coordinate of the previous seg.
				int diff = (tmpXCoordinate - emptyTSWidth - xCoordinate) / PIXEL_TIME_GAP[timeLevel];
				timeSteps = tmpTimeSteps - diff;
				break;
			}
			startTime = startTime.plusHours(1);
			startTimeMinusOneMinute = startTimeMinusOneMinute.plusHours(1);
			emptyTSCurrentStep = getTotalSizeEmptyTS(timeLevel, startTime.toString(fmt)) - totalEmptyTS;
			totalEmptyTS += emptyTSCurrentStep;//SEPARATE_TIME_UNITS[timeLevel];
		}
		
		return timeSteps;
	
	}
	
	public static String getDateTime(int timeSteps, int timeLevel){
		DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm");
		DateTime startTime = fmt.withZone(DateTimeZone.forID("UTC")).parseDateTime(DynamicGraph.START_DATE);
		String date = "";
		DateTimeFormatter customeFormatter = new DateTimeFormatterBuilder()
			    .appendDayOfWeekShortText()
			    .appendLiteral(',')
			    .appendLiteral(' ')
			    .appendMonthOfYearShortText()
			    .appendLiteral(' ')
			    .appendDayOfMonth(2)
			    .appendLiteral(',')
			    .appendLiteral(' ')
			    .appendYear(4, 4)
			    .appendLiteral(' ')
			    .appendHourOfDay(2)
			    .appendLiteral(':')
			    .appendMinuteOfHour(2)
			    .toFormatter()
			    .withLocale(Locale.US);
		if(timeLevel==MINUTE_LEVEL){
			startTime = startTime.plusMinutes(timeSteps);
			date = startTime.toString(customeFormatter);
		}else if(timeLevel==HOUR_LEVEL){
			startTime = startTime.plusHours(timeSteps);
			date = startTime.toString(customeFormatter);
		}else if(timeLevel==DAY_LEVEL){
			startTime = startTime.plusDays(timeSteps);
			date = startTime.toString(DateTimeFormat.mediumDate());
		}else if(timeLevel==MONTH_LEVEL){
			startTime = startTime.plusMonths(timeSteps);
			date = startTime.toString(DateTimeFormat.mediumDate());
		}else if(timeLevel==YEAR_LEVEL){
			startTime = startTime.plusYears(timeSteps);
			date = startTime.toString(DateTimeFormat.mediumDate());
		}
		
		return date;
	}
	
	// accumulative weight over all edges that path thru the pixel
	public List<Float> getMaxPixelWeight(int timeLevel){
		List<Float> maxPixelWeightList= new ArrayList<Float>();
		int imageWidth= getImageWidth(timeLevel);
		int imageHeight= (int) context.getAttribute("IMAGE_HEIGHT");
		int overlapSize = K_SIZE * N_SMOOTH;
		int segmentWidth = 200;
		int startX = 0;
		int segmentEnd = startX + segmentWidth;	// we take filterSize from the next segment
		int boundedSegWidth = 0;
		int segmentStart = 0;
		int oldPart = -1;
		List<HashMap<Integer, Integer>> listWeightMap = new ArrayList<HashMap<Integer,Integer>>();	// define a map for each edge weight separately
		
		try {
			List<ImgVertices> rs=null;
			while(startX<imageWidth){
				segmentStart = Math.max(0, startX-overlapSize);	// we take filterSize from the previous segment
				segmentEnd = Math.min(imageWidth, startX + segmentWidth + overlapSize);	// we take filterSize from the next segment
				boundedSegWidth = segmentEnd - segmentStart;
				
				int imgPartLower = segmentStart/imgPartWidth;
				int imgPartUpper = segmentEnd/imgPartWidth;
				
				if(imgPartLower!=oldPart){	// execute only when we are in a new part
					rs = getImageEdgesDB(timeLevel, imgPartLower, imgPartUpper);
					oldPart = imgPartLower;
				}
				
				
				
				float[][][] smoothedImgList = getSmoothedSegment(rs, segmentStart, segmentEnd, 0, imageHeight, imageHeight, listWeightMap, boundedSegWidth, timeLevel,0, null);
			    
				for(int i=0;i<smoothedImgList.length;i++){	// compute max pixel weight per weight image
				    float localMaxPixelWeight = 0;	// max pixel weight per segment
				    float[][] smoothedImg = smoothedImgList[i];
				    for(int x=0;x<smoothedImg.length;x++){
				    	for(int y=0;y<smoothedImg[x].length;y++){
					    	if(smoothedImg[x][y]>localMaxPixelWeight){	// determine the maximum pixel weight for this image segment
					    		localMaxPixelWeight = smoothedImg[x][y];
					    	}
				    	}
				    }
				    
				    if(i<maxPixelWeightList.size() && localMaxPixelWeight>maxPixelWeightList.get(i)){
				    	maxPixelWeightList.set(i, localMaxPixelWeight);
				    }else if(i>=maxPixelWeightList.size()){	// initialize
				    	maxPixelWeightList.add(localMaxPixelWeight);
				    }
				}
				startX+=segmentWidth;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return maxPixelWeightList;
	}
	

	public String getImageSegment(int startX, int segWidth, int startY, int segHeight, int timeLevel, boolean showContours, int minEdgeWeight, List<Integer> selectedYCoordinates){
		List<Float> maxPixelWeightList=(List<Float>) context.getAttribute("MAX_PIXEL_WEIGHT"+timeLevel);
		//System.out.println(maxPixelWeightList);
		int maxPixelWeight = (int) Math.ceil(maxPixelWeightList.get(minEdgeWeight-1));
		int imageHeight= (int) context.getAttribute("IMAGE_HEIGHT");//234*DynamicGraph.PIXEL_VERTEX_GAP;////398;
		//System.out.println(maxPixelWeight);
		
		
		int imageWidth = getImageWidth(timeLevel);//(int) context.getAttribute("IMAGE_WIDTH"+prevLevel);
		String imageInByte = null;
		if(startX<imageWidth && segWidth > 0){

			// we handle overlapping areas between segments to be able to compute smooth filter function afterwards
			int overlapSize = K_SIZE * N_SMOOTH;
			int segmentStart = Math.max(0, startX-overlapSize);	// we take filterSize from the previous segment
			int segmentEnd = Math.min(imageWidth, startX + segWidth + overlapSize);	// we take filterSize from the next segment
			int boundedSegWidth = segmentEnd - segmentStart;
			int endY =  startY + segHeight;
			
			List<HashMap<Integer,Integer>> listWeightMap = new ArrayList<HashMap<Integer,Integer>>();
			try {
				int imgPartLower = segmentStart/imgPartWidth;
				int imgPartUpper = segmentEnd/imgPartWidth;
			    List<ImgVertices> rs = getImageEdgesDB(timeLevel, imgPartLower, imgPartUpper);
				
			    float[][][] smoothedImgList = getSmoothedSegment(rs, segmentStart, segmentEnd, startY, endY, imageHeight, listWeightMap, boundedSegWidth, timeLevel, minEdgeWeight, selectedYCoordinates);
			    
				
			    if(smoothedImgList.length>0){
			    	float[][] smoothedImg = smoothedImgList[0];
					// normalized the segment
					for(int x=0;x<smoothedImg.length;x++){
						for(int y=0;y<smoothedImg[x].length;y++){
							smoothedImg[x][y] = (float) Math.log10(smoothedImg[x][y]+1)/(float)Math.log10(maxPixelWeight+1);
						}
					}
					
					float[][] contours = new float[boundedSegWidth][imageHeight];
					if(showContours){
						//compute contours
						float maxBinVlue = bins[bins.length-1];
						float increamentValue = maxBinVlue/(float)contourLevels;
						for(float i=0.01f;i<maxBinVlue;i+=increamentValue){
							float[][] thresholdedImg = threshold(smoothedImg,i);	// threshold
						
							// compute gradient 
							int left,right,top,down;
							for(int x=0;x<thresholdedImg.length;x++){
								for(int y=0;y<thresholdedImg[x].length;y++){
									// handle boundaries (no change)
									left = x==0?x:x-1;
									right = x==thresholdedImg.length-1?thresholdedImg.length-1:x+1;
									top = y==0?y:y-1;
									down = y==thresholdedImg[x].length-1?thresholdedImg[x].length-1:y+1;
									float gradient = Math.abs(thresholdedImg[right][y]-thresholdedImg[left][y]) + Math.abs(thresholdedImg[x][top]-thresholdedImg[x][down]);
									if(gradient>0)
										contours[x][y] = gradient;
								}
							}
						}
					}
					
					// write contours over the image pixels
					BufferedImage imgColored = new BufferedImage(boundedSegWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
					for(int x=0 ;x<imgColored.getWidth();x++){	
						for(int y=0;y<imgColored.getHeight();y++){
							if(showContours && contours[x][y]>0){
								imgColored.setRGB(x, y, Color.BLACK.getRGB());
							}else{ 
								imgColored.setRGB(x, y, getPixelColor(smoothedImg[x][y]));
							}
							
						}
					}
					
					BufferedImage reconstructedImg;
					if(overlapSize>0){
					// get actual segment without overlapping areas
						int imgWidth = boundedSegWidth < segWidth ? segmentEnd-segmentStart : segWidth;	// to avoid the empty black area at last segment
						reconstructedImg = new BufferedImage(imgWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
						int initX = overlapSize;	// default
						int maxX = imgColored.getWidth()-overlapSize;	//default
						if(segmentStart==0)	// first segment
							initX = 0;
						if(segmentEnd==imageWidth)	// last segment
							maxX = imgColored.getWidth();
						
						for(int x=initX ;x<maxX;x++){	// loop only through the actual segment width without the overlapping
							for(int y=0;y<imgColored.getHeight();y++){
								reconstructedImg.setRGB(x-initX, y, imgColored.getRGB(x, y));
							}
						}
					}else{
						reconstructedImg = imgColored;
					}
					
					
					
					//write images
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write(reconstructedImg, "png", baos);
					baos.flush();
					imageInByte = Base64.getEncoder().encodeToString(baos.toByteArray());
					baos.close();
			    }
			    
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return imageInByte;
	}
	
	
	
	private float[][][] getSmoothedSegment(List<ImgVertices> rs, int segmentStart, int segmentEnd, int startY, int endY, int imageHeight, List<HashMap<Integer,Integer>> listWeightMap, 
			int boundedSegWidth, int timelevel, int minEdgeWeight, List<Integer> selectedYCoordinates){
		
		
		for(ImgVertices mv : rs){
			int vid = mv.getVid();
	    	String[] ePoints = mv.getAffectedPixels().split(";");
	    	String[] eWights = mv.getPixelsWeight().split(";");
	    	
	    	for(int i=0;i<ePoints.length;i++){
	    		if(ePoints[i]!=null && !ePoints[i].trim().isEmpty() && Integer.parseInt(eWights[i])>=minEdgeWeight){
	    			String[] coordinates = ePoints[i].split(",");
	    			int x0 = Integer.parseInt(coordinates[0]);
	    			int x1 = x0+STRIP_WIDTH[timelevel];	
	    			int y0 = Integer.parseInt(coordinates[1]);
	    			int y1 = Integer.parseInt(coordinates[2]);
	    			
	    			if(!isFilteredOut(x0, y0, x1, y1, segmentStart, segmentEnd, startY, endY, selectedYCoordinates, INTRA_RELATION)){	// check if edge pass filtration criteria
	    				List<Point> allEdgePoints = getAllPointsOnLine(x0,y0,x1,y1);
	    				
	    				for(Point p:allEdgePoints){
	    					if( (p.x >= segmentStart && p.x < segmentEnd) && (p.y>=startY && p.y < endY)){		// only counts pixel the belongs to current segment
		    					int pixelID = (p.x - segmentStart) * imageHeight + p.y;		// segmentStart to get correct pixel order in the fixed-size array regardless segment location in image
		    					int pixelWeight = Integer.parseInt(eWights[i]);
		    					int w=0;
		    					do{	// compute a map corresponding to each edge weight
		    						HashMap<Integer,Integer> wMap = null;
		    						if(w < listWeightMap.size()){
		    							wMap = listWeightMap.get(w);
		    						}else{
		    							wMap = new HashMap<Integer,Integer>();
		    							listWeightMap.add(wMap);
		    						}
		    						Integer pixelCummulativeWeight = wMap.get(pixelID);
			    					if(pixelCummulativeWeight == null){
			    						wMap.put(pixelID, pixelWeight);
			    					}else{
			    						wMap.put(pixelID, pixelCummulativeWeight.intValue()+pixelWeight);
			    					}
			    					w++;
		    					}while(w<pixelWeight && minEdgeWeight==0);
		    					
		    					
	    					}
	    				}
		    			
	    			}
	    		}
	    	}
	    	
		}
			
		
	    
		
    	float[][][] greyImgList = new float[listWeightMap.size()][boundedSegWidth][imageHeight];	//array of images for each edge weight
    	
    	for(int i=0;i<listWeightMap.size();i++){
		    for (Map.Entry<Integer, Integer> entry : listWeightMap.get(i).entrySet()) 
			{
		    	int pixelId = entry.getKey();
		    	int x = (int) Math.floor(pixelId/imageHeight);
				int y = pixelId%imageHeight;
				greyImgList[i][x][y] =  entry.getValue();
			}
		    listWeightMap.get(i).clear();
    	
		
			// Apply Reconstruction Filter
		    float[][] smoothedImg = greyImgList[i];
			for(int k=0;k<N_SMOOTH;k++){
				smoothedImg = applyBoxFiler(smoothedImg);
			}
			greyImgList[i] = smoothedImg;	// re-assign after smoothing
    	}
		
		return greyImgList;
	}
	
	
	public Map<Integer,Vertex> getRegionVertices(int startX, int selectionWidth, int startY, int selectionHeight, int timeLevel, int minEdgeWeight, List<Integer> selectedYCoordinates, int relationType){
		int imageWidth = getImageWidth(timeLevel);
		Map<Integer,Vertex> vertexMap = new HashMap<Integer,Vertex>();
		Parser p = new Parser();
		
		//System.out.println("startX: "+startX+", selectionWidth: "+selectionWidth+", startY: "+startY+", selectionHeight: "+selectionHeight+", timeLevel: "+timeLevel+", minEdgeWeight: "+minEdgeWeight+", selectedYCoordinates: "+selectedYCoordinates+", relationType: "+relationType);
		try{
			if(startX<imageWidth && selectionWidth > 0){
				int endX = Math.min(imageWidth, startX + selectionWidth);
				int endY = startY + selectionHeight;
				
				int imgPartLower = startX/imgPartWidth;
				int imgPartUpper = endX/imgPartWidth;
				List<ImgVertices> rs = getImageEdgesDB(timeLevel, imgPartLower, imgPartUpper);
				for(ImgVertices mv : rs){

			    	String[] ePoints = mv.getAffectedPixels().split(";");
			    	String[] eWights = mv.getPixelsWeight().split(";");
			    	int vid = mv.getVid();
			    	for(int i=0;i<ePoints.length;i++){
			    		int w = Integer.parseInt(eWights[i]);
			    		if(ePoints[i]!=null && !ePoints[i].trim().isEmpty() && w>=minEdgeWeight){
			    			String[] coordinates = ePoints[i].split(",");
			    			int x0 = Integer.parseInt(coordinates[0]);
			    			int x1 = x0+STRIP_WIDTH[timeLevel];	
			    			int y0 = Integer.parseInt(coordinates[1]);
			    			int y1 = Integer.parseInt(coordinates[2]);
			    			if(!isFilteredOut(x0, y0, x1, y1, startX, endX,  startY, endY ,selectedYCoordinates, relationType)){
			    				int dest = translateYCoordinateToVertex(y1);
			    				p.HandleOutEdges(vertexMap, vid, dest, w);
			    				p.HandleInEdges(vertexMap, vid, dest, w);
			    			}
			    		}
			    	}
			    
				}
				
				
			}
		}catch(SQLException ex){
			ex.printStackTrace();
		}
		
		return vertexMap;
		
	}
	
	
	private boolean isFilteredOut(int x0, int y0, int x1, int y1, int startX, int endX, int startY, int endY, List<Integer> selectedYCoordinates, int relationType){
		if(	(selectedYCoordinates == null || (relationType == INTRA_RELATION && selectedYCoordinates.contains(y0) && selectedYCoordinates.contains(y1) ) 
				|| (relationType == INTER_RELATION && ( selectedYCoordinates.contains(y0) || selectedYCoordinates.contains(y1) ) ) ) &&		// if vertex is filtered
			( (x0 >= startX && x0 <endX) || (x1 >=startX && x1 <endX) || (x0 <startX && x1 > endX) ) && 	// if edge belongs to the current segment (including edges that are shared between segments)
			( (startY == -1) || (y0 >= startY && x0 < endY) || (y1 >= startY && y1 < endY) || (y0 <startY && y1 > endY) ) 
		){		 
			return false;
		}else
			return true;
	}
	
	private List<Point> getAllPointsOnLine(int x0, int y0, int x1, int y1)
    {
		List<Point> points = new ArrayList<Point>();
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy, e2;

        while(true)
        {
            points.add(new Point(x0, y0));

            if (x0 == x1 && y0 == y1) break;

            e2 = 2 * err;

            // EITHER horizontal OR vertical step (but not both!)
            if (e2 > dy)
            {
                err += dy;
                x0 += sx;
            }
            else if (e2 < dx)
            { // <--- this "else" makes the difference
                err += dx;
                y0 += sy;
            }
        }
        
        return points;
    }
	
	
	// this function place bufferRreader every Buffered_Readers_GAP to accelerate reading process
	public void setFileBufferedReader(int totalRowsSize, File file) throws IOException{
		int bfCount = (int) Math.ceil(totalRowsSize / (double)Buffered_Readers_GAP);
		fileBfs = new BufferedReader[bfCount];
		
		for(int j=0;j<fileBfs.length;j++){	// initialize all buffers
			fileBfs[j] = new BufferedReader(new FileReader(file));
		}
		
		int i=0; // index to fileBfs array
		for(int r = 0; r < totalRowsSize; ++r){
			if(r % Buffered_Readers_GAP==0){
				fileBfs[i].mark(Buffered_Readers_GAP*NUMBER_OF_CHAR_PER_LINE);
				i++;
			}
			if(i==fileBfs.length)
				break;
			for(int j=i;j<fileBfs.length;j++){
				fileBfs[j].readLine();
			}
			
		}

	}
	
	private String getLineByNumber(int line, File file) throws IOException{
		int start = 0;
		int end = start + Buffered_Readers_GAP;
		String lineIWant = null;
		for(int i=0;i<fileBfs.length;i++){
			if(line>=start && line<end){
				BufferedReader bf = fileBfs[i];
				for(int r = start; r < line; ++r)
					bf.readLine();
				lineIWant = bf.readLine();
				bf.reset();
				break;
			}else{
				start+=Buffered_Readers_GAP;
				end+=Buffered_Readers_GAP;
			}
		}
		
		return lineIWant;
	}
	
	// same method read lines sequentially (slow)
	private String getLineByNumberSq(int line, File file) throws IOException{
		int start = 0;
		String lineIWant = null;
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		
        while ((lineIWant = br.readLine()) != null) {
			if(start==line){
				break;
			}else{
				start++;
			}
		}
		br.close();
		return lineIWant;
	}
	
	
	private float[][] applyBoxFiler(float[][] img){
		float[][] filtered = new float[img.length][img[0].length];
		float sumColor;
		int pxCount;
		
		for(int x=0;x<img.length;x++){
			for(int y=0;y<img[x].length;y++){
				sumColor=0;
				pxCount=0;
				
				for(int i=x-K_SIZE;i<=x+K_SIZE;i++){
					for(int j=y-K_SIZE;j<=y+K_SIZE;j++){
						if(i>=0 && i<img.length && j>0 && j<img[x].length){
							sumColor+= img[i][j];
							pxCount++;
						}
						
					}
				}
				float avg = sumColor/(float)pxCount;
				filtered[x][y]= avg;
			}
		}
		return filtered;
	}
	
	
	private float interpolate(float a, float b, float proportion) {
	    return (a + ((b - a) * proportion));
	}

	/**
	 * Returns an interpolated color, between <code>a</code> and <code>b</code>
	 * proportion = 0, results in color a
	 * proportion = 1, results in color b
	 */
	private int interpolateColor(Color a, Color b, float proportion) {

	    if (proportion > 1 || proportion < 0) {
	        throw new IllegalArgumentException("proportion must be [0 - 1]");
	    }
	    float[] hsva = new float[3];
	    float[] hsvb = new float[3];
	    float[] hsv_output = new float[3];

	    
	    Color.RGBtoHSB(a.getRed(),a.getGreen(),a.getBlue(),hsva);
	    Color.RGBtoHSB(b.getRed(),b.getGreen(),b.getBlue(),hsvb);
	    for (int i = 0; i < 3; i++) {
	        hsv_output[i] = interpolate(hsva[i], hsvb[i], proportion);
	    }

	    int alpha_a = a.getAlpha();
	    int alpha_b = b.getAlpha();
	    float alpha_output = interpolate(alpha_a, alpha_b, proportion);
	    
	    float hue = hsv_output[0];
	    float saturation = hsv_output[1];
	    float brightness = hsv_output[2];

	    //return Color.HSVToColor((int) alpha_output, hsv_output);
	    int rgb = Color.HSBtoRGB(hue, saturation, brightness);
	    /*
	    int red = (rgb >> 16) & 0xFF;
	    int green = (rgb >> 8) & 0xFF;
	    int blue = rgb & 0xFF;
	    */
	    return rgb;

	}
	
	
	private int getPixelColor(float pixelWeight){
		// fill pixel array
	    //Color[] colors = {Color.GRAY, Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN};
	    //Color[] colors = {new Color (254,217,118),new Color (254,178,76),new Color (253,141,60),new Color (240,59,32),new Color (189,0,38)};
	    Color[] colors = {Color.WHITE,Color.GRAY,Color.RED,Color.YELLOW,Color.GREEN};
	    int rgbInt = 0;
		if(pixelWeight==bins[bins.length-1]){
			 return  colors[colors.length-1].getRGB();
		}else{
			 for(int b=0;b<bins.length-1;b++){
				 if(pixelWeight>=bins[b] && pixelWeight<bins[b+1]){
					 Color colorA = colors[b];
		    		 Color colorB = colors[b+1];
		    		 float propotion = (float) ((pixelWeight - bins[b]) / (bins[b+1] - bins[b]));
		    		 rgbInt = interpolateColor(colorA, colorB, propotion);
		    		 break;
				 }
			 }
		}
		return rgbInt;
	}
	
	private float[][] threshold(float[][] smoothedImg, float threshold){
		float[][] result =  new float[smoothedImg.length][smoothedImg[0].length];
		for(int x=0;x<smoothedImg.length;x++){
			for(int y=0;y<smoothedImg[x].length;y++){
				if(smoothedImg[x][y]>threshold){
					result[x][y]=1.0f;
				}
			}
		}
		
		return result;
		
	}
	
	
	private List<ImgVertices> getImageEdgesDB(int timeLevel, int imgPartLower, int imgPartUpper) throws SQLException{
		Connection connection = (Connection)context.getAttribute("db");
	    String sql = "select * from tbl_img_t"+timeLevel+"_vertices where img_part>="+imgPartLower+" and img_part<="+imgPartUpper;
	    PreparedStatement ps = connection.prepareStatement(sql);
	    ResultSet rs = ps.executeQuery();
	    List<ImgVertices> result = new ArrayList<ImgVertices>();
	    ImgVertices mv = null;
	    while(rs.next()){
	    	mv = new ImgVertices();
	    	mv.setVid(rs.getInt("vid"));
	    	mv.setImgPart(rs.getInt("img_part"));
	    	mv.setAffectedPixels(rs.getString("affected_pixels"));
	    	mv.setPixelsWeight(rs.getString("pixels_weight"));
	    	mv.setCanvasId(rs.getInt("canvas_id"));
	    	result.add(mv);
	    }
	    
	    rs.close();
	    ps.close();
	    return result;
	}
	
	private void pushToDB(Connection connection, PreparedStatement[] insertPS, List<Map<String,Integer>> vertexEdgesWeightsMaps) throws SQLException{
		Map<String,List<StringBuilder>> aggregatedSqlQueriesPerVertex = new HashMap<String,List<StringBuilder>>();
		
		
		for(int t=0;t<TIME_LEVELS;t++){
			
			String key = "", newMapKey="";
    		Map<String,Integer> vertexEdgesWeights = vertexEdgesWeightsMaps.get(t);
    		
	    	for(Map.Entry<String, Integer> entry: vertexEdgesWeights.entrySet()){
	    		StringBuilder pixelsIdsStr = new StringBuilder();
	    		StringBuilder pixelsWeightsStr = new StringBuilder();
	    		
    			// compute the pixel location in image (which part) (to avoid very large column data in db)
    			key  = entry.getKey();
    			int startX = Integer.parseInt(key.split(",")[1]);
    			int index = startX/imgPartWidth;	// each 1000 pixel width considered a part
    			
    			int vid = Integer.parseInt(key.split(",")[0]);
    			pixelsIdsStr.append(key.substring(key.indexOf(",")+1,key.length())).append(";");
    			pixelsWeightsStr.append(entry.getValue()).append(";");
    			
    			newMapKey =  vid+"_"+index;
    			List<StringBuilder> vertexLists = aggregatedSqlQueriesPerVertex.get(newMapKey);
    			if(vertexLists==null){
    				vertexLists = new ArrayList<StringBuilder>();
    				vertexLists.add(pixelsIdsStr);
    				vertexLists.add(pixelsWeightsStr);
    				aggregatedSqlQueriesPerVertex.put(newMapKey, vertexLists);
    			}else{
    				vertexLists.get(0).append(pixelsIdsStr);
    				vertexLists.get(1).append(pixelsWeightsStr);
    			}

	    	}
	    	
	    	// insert into db
	    	for(Map.Entry<String, List<StringBuilder>> entry: aggregatedSqlQueriesPerVertex.entrySet()){
	    		int vid = Integer.parseInt(entry.getKey().split("_")[0]);
	    		int part = Integer.parseInt(entry.getKey().split("_")[1]);
	    		insertPS[t].setInt(1,vid);
	    		insertPS[t].setInt(2,part);
	    		insertPS[t].setString(3,entry.getValue().get(0).toString());
	    		insertPS[t].setString(4,entry.getValue().get(1).toString());
	    		insertPS[t].setString(5,entry.getValue().get(0).toString());
	    		insertPS[t].setString(6,entry.getValue().get(1).toString());
	    		insertPS[t].addBatch();
	    	}
	    	
	    	insertPS[t].executeBatch();
			connection.commit();
	    	
	    	vertexEdgesWeightsMaps.get(t).clear();
	    	aggregatedSqlQueriesPerVertex.clear();
		
			
		}
	}
	
	
	
	public int translateVertexToYCoordinate(int vertexId){
		Map<Integer,Vertex> vertexMap = (Map<Integer,Vertex>) context.getAttribute("VERTEX_MAP");
		int vOrder = vertexMap.get(vertexId).getOrder();
		return PIXEL_OFFSET + (vOrder*PIXEL_VERTEX_GAP);
		
	}
	
	public int translateYCoordinateToVertex(int y){
		String[] vertexList = (String[]) context.getAttribute("VERTEX_LIST");
		int vOrder = (y - PIXEL_OFFSET) / PIXEL_VERTEX_GAP;
		int vertexId = Integer.parseInt(vertexList[vOrder].split("\\|")[0]);
		return vertexId;
		
	}
	
	public String translateVertextoPipeSeparatedStr(Vertex v, int inEdgesCount, int outEdgesCount){
		 return v.getId()+"|"+v.getName()+"|"+inEdgesCount+"|"+outEdgesCount;//+"|"+getVertexClusterName(v.getId(),DEFAULT_CLUSTER_LEVEL); 
	}
	
	public int getImageWidth(int timeLevel){
		int emptyTimeSteps = getTotalSizeEmptyTS(timeLevel, END_DATE);
		return PIXEL_OFFSET + (getTimeStepsCount(END_DATE,timeLevel) + emptyTimeSteps - 1) * PIXEL_TIME_GAP[timeLevel] + STRIP_WIDTH[timeLevel];
	}
	

	private static int getTotalSizeEmptyTS(int timeLevel, String edgeDate){
		int emptyTimeSteps = 0;
		int tmp = 0;
		for(int t = DynamicGraph.TIME_LEVELS - 1; t >= timeLevel; t--){
			tmp = emptyTimeSteps;
			if(t < DynamicGraph.TIME_LEVELS - 1){	//	avoid conversion from previous level if we are in the top level
				emptyTimeSteps = emptyTimeSteps * DynamicGraph.timeUnits[t+1];
			}
			emptyTimeSteps += (tmp + getTimeStepsCount(edgeDate,t+1)) * EMPTY_TIME_STEPS[t]; 
		}
		
		return emptyTimeSteps;
	}

	
	
	public void getClustersJASONHierarchy(StringBuilder JASONString, ClusterComponent comp, int x){

		
		int YCoordinate = 0;
		int startOrder = 0;
		int endOrder = 0;
		List<String> leafs = null;
		Cluster tCluster = comp.getCluster();
		double xCoordinate = Math.abs(comp.getInitPoint().getX());
		if(tCluster.isLeaf()){
			YCoordinate = PIXEL_OFFSET +  (((Map<Integer,Vertex>) context.getAttribute("VERTEX_MAP")).get(Integer.parseInt(tCluster.getName())).getOrder()) * PIXEL_VERTEX_GAP ;
			JASONString.append("{").append("\"name\":\"").append(tCluster.getName()).append("\",");
			JASONString.append("\"xcoordinate\":\"").append((xCoordinate)*DENDROGRAM_CANVAS_WIDTH).append("\",");
			JASONString.append("\"ycoordinate\":\"").append(YCoordinate).append("\"");
			//System.out.println("clutser: "+comp.getCluster().getName()+", pointX: "+comp.getInitPoint().getX());
        	return;
			
		}else{
			
			leafs = tCluster.getClusterLeafsByTraversing();
			startOrder = ((Map<Integer,Vertex>) context.getAttribute("VERTEX_MAP")).get(Integer.parseInt(leafs.get(0))).getOrder();
			endOrder = ((Map<Integer,Vertex>) context.getAttribute("VERTEX_MAP")).get(Integer.parseInt(leafs.get(leafs.size()-1))).getOrder();
        	YCoordinate = PIXEL_OFFSET +  ( (endOrder + startOrder) / 2) * PIXEL_VERTEX_GAP ;
        	
        	JASONString.append("{").append("\"name\":\"").append(tCluster.getName()).append("\",");
        	JASONString.append("\"ycoordinate\":\"").append(YCoordinate).append("\",");
        	
        	if(xCoordinate == 0)
        		xCoordinate = 1.0;
        	JASONString.append("\"xcoordinate\":\"").append((xCoordinate)*DENDROGRAM_CANVAS_WIDTH).append("\",");
        	JASONString.append("\"children\":").append("[");
        	//System.out.println("clutser: "+comp.getCluster().getName()+", pointX: "+comp.getInitPoint().getX());
        	for(int i = 0; i < tCluster.getChildren().size(); i++){
        		
        		//Cluster c = tCluster.getChildren().get(i);
        		ClusterComponent chldComp = comp.getChildren().get(i);
        		getClustersJASONHierarchy(JASONString, chldComp,x-20);
				
        		JASONString.append("}");
				if(i < tCluster.getChildren().size() -1 )
					JASONString.append(",");
			}
			JASONString.append("]");
		}
		
		return;
	
	}
	
	
	
	
}
