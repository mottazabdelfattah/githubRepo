package dgvis.beans;

public class ImgVertices {
	private int vid;
	private int imgPart;
	private String affectedPixels;
	private String pixelsWeight;
	private int canvasId;
	public int getVid() {
		return vid;
	}
	public void setVid(int vid) {
		this.vid = vid;
	}
	public int getImgPart() {
		return imgPart;
	}
	public void setImgPart(int imgPart) {
		this.imgPart = imgPart;
	}
	public String getAffectedPixels() {
		return affectedPixels;
	}
	public void setAffectedPixels(String affectedPixels) {
		this.affectedPixels = affectedPixels;
	}
	public String getPixelsWeight() {
		return pixelsWeight;
	}
	public void setPixelsWeight(String pixelsWeight) {
		this.pixelsWeight = pixelsWeight;
	}
	public int getCanvasId() {
		return canvasId;
	}
	public void setCanvasId(int canvasId) {
		this.canvasId = canvasId;
	}
}
