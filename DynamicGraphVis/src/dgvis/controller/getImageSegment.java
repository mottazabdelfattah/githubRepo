package dgvis.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import dgvis.beans.Vertex;
import dgvis.clustering.Cluster;
import dgvis.clustering.visualization.ClusterComponent;
import dgvis.clustering.visualization.DendrogramPanel;
import dgvis.clustering.visualization.VCoord;
import dgvis.util.Common;
import dgvis.vis.DynamicGraph;

/**
 * Servlet implementation class getImageSegment
 */
@WebServlet("/getImageSegment")
public class getImageSegment extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static String GET_IMAGE_SEGMENT_ACTION = "getImageSegment";
    private static String GET_SELECTION_DATE_ACTION= "getSelectionDate";
    private static String GET_INITIAL_SETIINGS_ACTION= "getInitialSettings";
    private static String GET_REGION_VERTICES_ACTION= "getRegionVertices";
    private static String FILTER_SELECTED_VERTICES_ACTION= "filterSelectedVertices";
    /**
     * @see HttpServlet#HttpServlet()
     */
	DynamicGraph dg;
    public getImageSegment() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
    	// TODO Auto-generated method stub
    	super.init(config);
    	
    }
    @Override
    public void init() throws ServletException {
    	dg=new DynamicGraph(getServletContext());
    	dg.setVertexMapGlobal((Map<Integer,Vertex>) getServletContext().getAttribute("VERTEX_MAP"));
    }
    

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		JSONObject json = null;
		String actionName = request.getParameter("actionName");
		
		if(actionName.equals(GET_IMAGE_SEGMENT_ACTION) || actionName.equals(GET_SELECTION_DATE_ACTION) || actionName.equals(GET_REGION_VERTICES_ACTION) || actionName.equals(FILTER_SELECTED_VERTICES_ACTION)){
		
			int timeLevel = Integer.parseInt(request.getParameter("timeLevel"));
			int parentSelectionWidth = 0;
			if(request.getParameter("parentSelectionWidth") != null){
				parentSelectionWidth = Integer.parseInt(request.getParameter("parentSelectionWidth"));
			}
			
			int timeStepsToStart = 0;
			int selectionTimesteps = 0;
			String[] startXHeirarchy = request.getParameterValues("startXHeirarchy[]");
			
			if(startXHeirarchy!=null && startXHeirarchy.length>0){
				int tmp = 0; 
				int tmp2 = 0;

				int t = DynamicGraph.TIME_LEVELS - 1;
				for(int i = startXHeirarchy.length -1; i >= 0; i--){
					
					int s = Integer.parseInt(startXHeirarchy[i]);
					int endX = s + parentSelectionWidth;
					
					if(t < DynamicGraph.TIME_LEVELS - 1){	//	avoid conversion from previous level if we are in the top level
						s += ((tmp * DynamicGraph.timeUnits[t+1]) + (tmp * DynamicGraph.EMPTY_TIME_STEPS[t])) * DynamicGraph.PIXEL_TIME_GAP[t];
						endX += ((tmp2 * DynamicGraph.timeUnits[t+1]) + (tmp2 * DynamicGraph.EMPTY_TIME_STEPS[t])) * DynamicGraph.PIXEL_TIME_GAP[t];
					}
					
					tmp = (int) Math.ceil(((s - DynamicGraph.PIXEL_OFFSET)/(double)DynamicGraph.PIXEL_TIME_GAP[t]));
					
					tmp2 = tmp;				// default
					if(t <= timeLevel+1)	// consider only the current level and the direct parent level when computing selection width
						tmp2 = (int) Math.ceil(((endX - DynamicGraph.PIXEL_OFFSET)/(double)DynamicGraph.PIXEL_TIME_GAP[t]));
					
					t--;
				}
				timeStepsToStart = tmp;
				selectionTimesteps = tmp2 - tmp;

			}
			
			 	
			if(actionName.equals(GET_IMAGE_SEGMENT_ACTION) || actionName.equals(GET_REGION_VERTICES_ACTION) || actionName.equals(FILTER_SELECTED_VERTICES_ACTION)){
				int startX = DynamicGraph.PIXEL_OFFSET + ((timeStepsToStart)*DynamicGraph.PIXEL_TIME_GAP[timeLevel]);
				int startY = Integer.parseInt(request.getParameter("startY"));
				
				
				int minEdgeWeight = Integer.parseInt(request.getParameter("minEdgeWeight"));
				String[] selectedVertices = request.getParameterValues("selectedVertices[]");
				
				// translate vertices to corresponding y-coordinates
				List<Integer> selectedYCoordinates = null;
				if(selectedVertices != null){
					selectedYCoordinates = new ArrayList<Integer>();
					for(String str : selectedVertices){
						selectedYCoordinates.add(dg.translateVertexToYCoordinate(Integer.parseInt(str)));
					}
				}
				
				
				
				if(actionName.equals(GET_IMAGE_SEGMENT_ACTION)){
					boolean showContours = true;
					int segHeight = Integer.parseInt(request.getParameter("segHeight"));
					int segWidth = Integer.parseInt(request.getParameter("segWidth"));
					if(parentSelectionWidth > 0){
						//int selectionTimesteps = (parentSelectionWidth / DynamicGraph.PIXEL_TIME_GAP[timeLevel+1]) * DynamicGraph.timeUnits[timeLevel+1];
						int totalSegmentsWidth = (selectionTimesteps) * DynamicGraph.PIXEL_TIME_GAP[timeLevel]; //	sum of widths of all segments per single selection
						int diff = totalSegmentsWidth - (Integer.parseInt(startXHeirarchy[0]) + segWidth);
						segWidth = diff < 0 ? (segWidth + diff) : segWidth;
					}
					
					String imgData= dg.getImageSegment(startX, segWidth, startY, segHeight, timeLevel, showContours, minEdgeWeight, selectedYCoordinates);
					json = new JSONObject();
				    try {
				    	
				    	json.put("imgData", imgData);
					    response.setContentType("application/json");
					    response.getWriter().write(json.toString());
				    } catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else if(actionName.equals(GET_REGION_VERTICES_ACTION)){
					int selectionWidth = Integer.parseInt(request.getParameter("selectionWidth"));
					int selectionHeight = Integer.parseInt(request.getParameter("selectionHeight"));
					
					Map<Integer,Vertex> inOutEdgeMap = dg.getRegionVertices(startX, selectionWidth, startY, selectionHeight, timeLevel, minEdgeWeight, selectedYCoordinates, DynamicGraph.INTRA_RELATION);
					List<String> vertexList = new ArrayList<String>();
					
					for(Map.Entry<Integer, Vertex> e: inOutEdgeMap.entrySet()){
						Vertex v = e.getValue();
						int inEdgesCount = Common.countInEdges(v);
						int outEdgesCount = Common.countOutEdges(v);
						
						vertexList.add(dg.translateVertextoPipeSeparatedStr(v, inEdgesCount, outEdgesCount));
					}
					
					json = new JSONObject();
				    try {
				    	
				    	json.put("vertexList", vertexList);
					    response.setContentType("application/json");
					    response.getWriter().write(json.toString());
				    } catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else if(actionName.equals(FILTER_SELECTED_VERTICES_ACTION)){
					
					int relationType = Integer.parseInt(request.getParameter("relationType"));
					List<String> vertexList = new ArrayList<String>();
					
					
					int selectionWidth = 0;
					int selectionHeight = 0;
					if(parentSelectionWidth > 0){
						//int selectionTimesteps = (parentSelectionWidth / DynamicGraph.PIXEL_TIME_GAP[timeLevel+1]) * DynamicGraph.timeUnits[timeLevel+1];
						selectionHeight = Integer.parseInt(request.getParameter("parentSelectionHeight"));
						selectionWidth = (selectionTimesteps) * DynamicGraph.PIXEL_TIME_GAP[timeLevel]; 
					}else{
						// root level
						selectionWidth = dg.getImageWidth(timeLevel);
						selectionHeight=(int) getServletContext().getAttribute("IMAGE_HEIGHT");
					}
					
					Map<Integer,Vertex> inOutEdgeMap = dg.getRegionVertices(startX, selectionWidth, startY, selectionHeight, timeLevel, minEdgeWeight, selectedYCoordinates, relationType);
					for(Map.Entry<Integer, Vertex> e: inOutEdgeMap.entrySet()){
						Vertex v = e.getValue();
						int inEdgesCount = Common.countInEdges(v);
						int outEdgesCount = Common.countOutEdges(v);
						
						vertexList.add(dg.translateVertextoPipeSeparatedStr(v, inEdgesCount, outEdgesCount));
						
					}
					
					json = new JSONObject();
					try{
						json.put("vertexList", vertexList);
						response.setContentType("application/json");
						response.getWriter().write(json.toString());
					}catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
			    
			}else if(actionName.equals(GET_SELECTION_DATE_ACTION)){
				json = new JSONObject();
				
				//System.out.println("timeStepsToStart: "+timeStepsToStart);
				int cursorX = timeStepsToStart*DynamicGraph.PIXEL_TIME_GAP[timeLevel];
				timeStepsToStart = DynamicGraph.getTimeStepsCountWithoutEmptyTS(cursorX, timeLevel);	// get actual time steps without empty time steps 
				String dt = DynamicGraph.getDateTime(timeStepsToStart,timeLevel);
				try {
					 
					json.put("dateTimeString", dt);
					response.setContentType("application/json");
					response.getWriter().write(json.toString());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else if(actionName.equals(GET_INITIAL_SETIINGS_ACTION)){
			json = new JSONObject();
			try {
				
				
				StringBuilder clusterJASONHierarchy = new StringBuilder("");
				Cluster rootCluster = (Cluster)getServletContext().getAttribute("ROOT_CLUSTER");
				DendrogramPanel dp = new DendrogramPanel();
				ClusterComponent clusterComp = dp.createComponent(rootCluster);
				dg.getClustersJASONHierarchy(clusterJASONHierarchy, clusterComp, DynamicGraph.DENDROGRAM_CANVAS_WIDTH);
				clusterJASONHierarchy.append("}");
				
				//int maxDepth = getClusterDepth(rootCluster);
				
				// prepare default vertex list
				int maxOutEdgeCount = 0, maxInEdgeCount = 0;
				Map<Integer,Vertex> vertexMap = (Map<Integer,Vertex>) getServletContext().getAttribute("VERTEX_MAP");
				String[] vertexList = new String[vertexMap.size()];
				
				for(Map.Entry<Integer, Vertex> e: vertexMap.entrySet()){
					Vertex v = e.getValue();
					int inEdgesCount = Common.countInEdges(v);
					int outEdgesCount = Common.countOutEdges(v);
					vertexList[v.getOrder()] = dg.translateVertextoPipeSeparatedStr(v, inEdgesCount, outEdgesCount);
					
					if(outEdgesCount > maxOutEdgeCount){
						maxOutEdgeCount = outEdgesCount;
					}
					
					if(inEdgesCount > maxInEdgeCount){
						maxInEdgeCount = inEdgesCount;
					}
				}
				getServletContext().setAttribute("VERTEX_LIST", vertexList);
				
				int [] maxEdgeWeight = new int[DynamicGraph.TIME_LEVELS];
				for(int i=0; i<DynamicGraph.TIME_LEVELS; i++){
					maxEdgeWeight[i] = ((List<Float>) getServletContext().getAttribute("MAX_PIXEL_WEIGHT"+i)).size();
				}
				json.put("imgHeight", getServletContext().getAttribute("IMAGE_HEIGHT"));
				json.put("maxEdgeWeight", maxEdgeWeight);
				json.put("clusterJASONHierarchy", clusterJASONHierarchy);
				json.put("dendrogramCanvasWidth", DynamicGraph.DENDROGRAM_CANVAS_WIDTH);
				json.put("vertexList", vertexList);
				json.put("maxInEdgeCount", maxInEdgeCount);
				json.put("maxOutEdgeCount", maxOutEdgeCount);
				json.put("pixelVerticalGap", DynamicGraph.PIXEL_VERTEX_GAP);
				
				
				
				
				response.setContentType("application/json");
				response.getWriter().write(json.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	
	
}
