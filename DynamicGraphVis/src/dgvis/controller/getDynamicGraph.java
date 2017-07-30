package dgvis.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.json.JSONException;
import org.json.JSONObject;

import dgvis.beans.Vertex;
import dgvis.clustering.AverageLinkageStrategy;
import dgvis.clustering.Cluster;
import dgvis.clustering.ClusteringAlgorithm;
import dgvis.clustering.MinLAVertixOrder;
import dgvis.clustering.PDistClusteringAlgorithm;
import dgvis.clustering.visualization.DendrogramPanel;
import dgvis.database.DatabaseHelper;
import dgvis.extraction.CustomParser;
import dgvis.extraction.Parser;
import dgvis.util.CannyEdgeDetector;
import dgvis.util.Common;
import dgvis.vis.DynamicGraph;

/**
 * Servlet implementation class getDynamicGraph
 */
@WebServlet("/getDynamicGraph")
public class getDynamicGraph extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public getDynamicGraph() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		DatabaseHelper dbHelper = new DatabaseHelper(getServletContext());
		dbHelper.initializeDBTables();
		
		String path = getServletContext().getRealPath("/");
		//File file = new File (path+"resources/943076781_T_ONTIME_JulyToDec2001.csv");
		File file = new File (path+"resources/2000-2001/two.csv");	// take care, time cell order is different here
		
		/*
		File file =  new File(path+"resources/2000-2001/2000-2001.csv");
		File out =  new File(path+"resources/2000-2001/2000-2001_aggregated.csv");
		CustomParser parser =new CustomParser();
		parser.writeCustomFormat(file, out, getServletContext());
		*/
		
		long startTime = System.currentTimeMillis();
		Parser parser = new Parser();
		Map<Integer,Vertex> vertexMap = parser.getVertexList(file,getServletContext());
		long endTime = System.currentTimeMillis();
		System.out.println("parsing took " + (endTime - startTime) + " milliseconds");
		
		Cluster cluster = new Cluster("Root");
		String[] vertNames = new String[vertexMap.size()]; 	// array hold vertices names 
		double[][] pdist = new double[1][(vertexMap.size()*(vertexMap.size()-1))/2];
		int i=0;
		for (Map.Entry<Integer, Vertex> entry : vertexMap.entrySet()) // filling the array
		{
			vertNames[i] = entry.getKey()+"";
			//entry.getValue().setOrder(i);	// random order
			//cluster.addChild(new Cluster(vertNames[i]));	// all vertices belongs to one cluster
			i++;
			
		}
		
		
		// compute jaccard distance
		startTime = System.currentTimeMillis();
		int k=0; // matrix index
		double outSim,inSim;
		for(i=0;i<vertNames.length-1;i++){
			int vertI = Integer.parseInt(vertNames[i]);
			for(int j=i+1;j<vertNames.length;j++){
				int vertJ = Integer.parseInt(vertNames[j]);
				outSim = Common.getWeightedJaccardDistance2(vertexMap.get(vertI).getOutNeighbors(), vertexMap.get(vertJ).getOutNeighbors(), 
						vertexMap.get(vertI).getOutNeighborsWeights(), vertexMap.get(vertJ).getOutNeighborsWeights());		// out-Neighbors similarity
				inSim = Common.getWeightedJaccardDistance2(vertexMap.get(vertI).getInNeighbors(), vertexMap.get(vertJ).getInNeighbors(),
						vertexMap.get(vertI).getInNeighborsWeights(), vertexMap.get(vertJ).getInNeighborsWeights());		// in-Neighbors similarity
				pdist[0][k] = (outSim + inSim)/2.0;		// similarity is computed as the average between the two similarities
				k++;
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println("compute jaccard distance took " + (endTime - startTime) + " milliseconds");
		System.out.flush();
		
		
		// cluster
		int order = 0;	// leaf order in the tree
		startTime = System.currentTimeMillis();
		ClusteringAlgorithm alg = new PDistClusteringAlgorithm();
		cluster = alg.performClustering(pdist, vertNames, new AverageLinkageStrategy());
		endTime = System.currentTimeMillis();
		System.out.println("cluster took " + (endTime - startTime) + " milliseconds");
		System.out.flush();
		
		
		// reorder vertices according to MinLA successive algorithm
		startTime = System.currentTimeMillis();
		cluster = MinLAVertixOrder.reorderClusterHierarchy(cluster, vertexMap);
		//cluster.appendLeafNames(cluster.getClusterLeafsByTraversing());
		endTime = System.currentTimeMillis();
		System.out.println("MinLA took " + (endTime - startTime) + " milliseconds");
		System.out.flush();
		
		
		for(String leaf : cluster.getClusterLeafsByTraversing()){
			int vid = Integer.parseInt(leaf);
			vertexMap.get(vid).setOrder(order);
			order++;
		}
		
		
		// build the graph
		startTime = System.currentTimeMillis();
		DynamicGraph dg=new DynamicGraph(getServletContext());
		dg.setVertexMapGlobal(vertexMap);
		
		dg.buildImage(file);
		
		endTime = System.currentTimeMillis();
		System.out.println("build graph took " + (endTime - startTime) + " milliseconds");
		System.out.flush();
		
		
		// compute max pixel
		startTime = System.currentTimeMillis();
		for(int t=0;t<DynamicGraph.TIME_LEVELS;t++){
			getServletContext().setAttribute("MAX_PIXEL_WEIGHT"+t, dg.getMaxPixelWeight(t));
		}
		endTime = System.currentTimeMillis();
		System.out.println("max pixel compuation took " + (endTime - startTime) + " milliseconds");
		System.out.flush();
		
		
		// update name of vertices
		Map<Integer,String> allVertics = dbHelper.getAllVertices();
		for(Map.Entry<Integer,Vertex> e :vertexMap.entrySet()){
			e.getValue().setName(allVertics.get(e.getKey()));
		}
		
		getServletContext().setAttribute("VERTEX_MAP",vertexMap);	// save in context
		getServletContext().setAttribute("ROOT_CLUSTER",cluster);	// save in context
		
		request.getRequestDispatcher("result.html").forward(request, response);
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
//	// this method take tree level and return number of clusters up to this level 
//	private List<Cluster> getClusters(Queue<Cluster> clusterQueue, int level){
//		
//		int currentLevel = 0;
//		List<Cluster> clusters = new ArrayList<Cluster>();
//		Queue<Integer> clusterLevels = new LinkedList<Integer>();  
//		
//		if(level == 0){
//			// roor cluster
//			clusters.add(clusterQueue.poll());
//			return clusters;
//		}
//		
//		while(!clusterQueue.isEmpty() && currentLevel < level){
//			Cluster tCluster = clusterQueue.poll();
//			
//			if(tCluster.isLeaf()){
//				clusters.add(tCluster);
//			}else{
//				for(Cluster c : tCluster.getChildren()){
//					clusterQueue.add(c);
//					clusterLevels.add(currentLevel+1);
//					if(currentLevel+1 == level)
//						clusters.add(c);
//				}
//			}
//			currentLevel = clusterLevels.poll();
//			
//		}
//		
//		return clusters;
//	}
	

}
