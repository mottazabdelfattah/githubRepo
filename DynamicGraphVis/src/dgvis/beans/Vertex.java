package dgvis.beans;

import java.util.List;

public class Vertex {
	private int id;
	private String name;
	List<Integer> outNeighbors;		// out-neighbors regardless the time
	List<Integer> inNeighbors;		// in-neighbors regardless the time
	private int order;
	List<Integer> outNeighborsWeights;
	List<Integer> inNeighborsWeights;
	//List<Edge> outEdges;
	
	public Vertex(int id, String name){
		this.id = id;
		this.name = name;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public List<Integer> getOutNeighbors() {
		return outNeighbors;
	}

	public void setOutNeighbors(List<Integer> outNeighbors) {
		this.outNeighbors = outNeighbors;
	}

	public List<Integer> getInNeighbors() {
		return inNeighbors;
	}

	public void setInNeighbors(List<Integer> inNeighbors) {
		this.inNeighbors = inNeighbors;
	}

	public List<Integer> getOutNeighborsWeights() {
		return outNeighborsWeights;
	}

	public void setOutNeighborsWeights(List<Integer> outNeighborsWeights) {
		this.outNeighborsWeights = outNeighborsWeights;
	}

	public List<Integer> getInNeighborsWeights() {
		return inNeighborsWeights;
	}

	public void setInNeighborsWeights(List<Integer> inNeighborsWeights) {
		this.inNeighborsWeights = inNeighborsWeights;
	}

//	public List<Edge> getOutEdges() {
//		return outEdges;
//	}
//
//	public void setOutEdges(List<Edge> outEdges) {
//		this.outEdges = outEdges;
//	}

	
	
}
