package dgvis.clustering;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import dgvis.beans.Vertex;

public class MinLAVertixOrder {
	public static void successiveOrder(int [] vertixOrder, Map<Integer,Vertex> vertexMap){
		int initVertId = vertixOrder[0];
		vertexMap.get(initVertId).setOrder(0);
		int r=1, l=-1;
		for(int i=1;i<vertixOrder.length;i++){
			int leftCost = increament(vertixOrder, vertexMap, i, l);
			int rightCost = increament(vertixOrder, vertexMap, i, r);
			if(leftCost<rightCost){
				vertexMap.get(vertixOrder[i]).setOrder(l);
				l--;
			}else{
				vertexMap.get(vertixOrder[i]).setOrder(r);
				r++;
			}
				
		}
		
		// remap
		int o=0;
		for(Map.Entry<Integer, Vertex> entry:vertexMap.entrySet()){
			o = entry.getValue().getOrder();
			entry.getValue().setOrder((o-l)-1);
		}
	}
	
	private static int increament(int[] vertixOrder, Map<Integer,Vertex> vertexMap, int i, int x){
		int cost = 0;
		int vertId = vertixOrder[i];
		Vertex vert = vertexMap.get(vertId);
		int idx,odx;
		for(int j=0; j<i; j++){
			List<Integer> inNeighbors = vert.getInNeighbors();
			List<Integer> outNeighbors = vert.getOutNeighbors();
			idx = inNeighbors!=null?inNeighbors.indexOf(vertixOrder[j]):-1;
			odx = outNeighbors!=null?outNeighbors.indexOf(vertixOrder[j]):-1;
			if(idx!=-1){
				cost+=Math.abs(x-vertexMap.get(vertixOrder[j]).getOrder())*vert.getInNeighborsWeights().get(idx);	// consider weight of the edge
			}
			if(odx!=-1){
				cost+=Math.abs(x-vertexMap.get(vertixOrder[j]).getOrder())*vert.getOutNeighborsWeights().get(odx);	// consider weight of the edge
			}
		}
		return cost;
	}
	
	
	
	public static Cluster reorderClusterHierarchy(Cluster cluster, Map<Integer,Vertex> vertexMap){
		Queue<Cluster> clusterQueue =  new LinkedList<Cluster>();
		clusterQueue.add(cluster);
		Cluster reorderedHierarchy =  cluster.copy();//new Cluster(cluster.getName());
		ArrayList<Cluster> leftRightChildren = null;// = new Cluster(cluster.getName());
		ArrayList<Cluster> rightLeftChildren = null;// = new Cluster(cluster.getName());
		
		int costLeftRight = 0;
		int costRightleft = 0;
		//ArrayList<Cluster> children = null;
		
		while(!clusterQueue.isEmpty()){
			
			//leftRightCluster = reorderedHierarchy.copy();
			//rightLeftCluster = reorderedHierarchy.copy();
			Cluster tCluster = clusterQueue.poll();
			leftRightChildren = new ArrayList<Cluster>();
			rightLeftChildren = new ArrayList<Cluster>();
			
			if(!tCluster.isLeaf()){
				
				Cluster left = tCluster.getChildren().get(0);
				Cluster right = tCluster.getChildren().get(1);
				
				leftRightChildren.add(left);
				leftRightChildren.add(right);
				
				rightLeftChildren.add(right);
				rightLeftChildren.add(left);
					
				costLeftRight = getEdgeCrossingCost(reorderedHierarchy, vertexMap);	// cost before switching
				
				Cluster found = getClusterByName(tCluster.getName(), reorderedHierarchy);
				
				if(found != null){
					found.setChildren(rightLeftChildren);
				}
				
				costRightleft = getEdgeCrossingCost(reorderedHierarchy, vertexMap);	// cost after switching
				
				if(costLeftRight < costRightleft){
					found.setChildren(leftRightChildren);
				}
				
				clusterQueue.add(left);
				clusterQueue.add(right);
			}
			
			
		}
		
		return reorderedHierarchy;
		
	}
	
	private static int getEdgeCrossingCost(Cluster cluster,  Map<Integer,Vertex> vertexMap){
		int cost = 0;
		List<String> leafs = cluster.getClusterLeafsByTraversing();
		
		for(int i = 0; i < leafs.size(); i++){
			
			int vid = Integer.parseInt(leafs.get(i));
			Vertex vert = vertexMap.get(vid);
			List<Integer> inNeighbors = vert.getInNeighbors();
			List<Integer> inNeighborsWeights = vert.getInNeighborsWeights();
			
		//	List<Integer> outNeighbors = vert.getOutNeighbors();
		//	List<Integer> outNeighborsWeights = vert.getOutNeighborsWeights();
			
			if(inNeighbors !=null){
				for(int j = 0; j < inNeighbors.size(); j++){
					int neighborId = inNeighbors.get(j);
					int neighborPos = leafs.indexOf(neighborId+"");
					cost+=Math.abs(i - neighborPos)*inNeighborsWeights.get(j);	// consider weight of the edge
				}
			}
			
		}
		
		return cost;
	}
	
	private static Cluster getClusterByName(String name, Cluster cluster){
		Queue<Cluster> clusterQueue = new LinkedList<Cluster>();
		clusterQueue.add(cluster);
		while(!clusterQueue.isEmpty()){
			Cluster tCluster = clusterQueue.poll();
			
			if(tCluster.getName().equals(name))
				return tCluster;
			
			if(!tCluster.isLeaf()){
				
				for(int c = tCluster.getChildren().size()-1; c>=0 ; c--){
					clusterQueue.add(tCluster.getChildren().get(c));
				}
			}
		}
		return null;
	}
	
}
