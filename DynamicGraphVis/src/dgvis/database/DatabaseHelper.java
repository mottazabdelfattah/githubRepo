package dgvis.database;
import java.awt.Point;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import dgvis.util.Common;


public class DatabaseHelper extends DatabaseConnector{
	
	public DatabaseHelper(ServletContext context) {
		super(context);
	}
	
	
	public void updateVerticesPixels(int vid, List<Point> points, int timeLevel, int imageWidth){
		String sql="select * from tbl_img_t0_vertices where vid=?";
		List<String> affectedPixels;
		List<String> pixlesWeight;
		try {
			ArrayList<Object> parameters = new ArrayList<Object>();
			parameters.add(vid);
			
			ResultSet rs = queryDatabase(sql, parameters);
			if(rs.next()){
				affectedPixels = new ArrayList<String>(Arrays.asList(rs.getString("affected_pixels").split(",")));
				pixlesWeight = new ArrayList<String>(Arrays.asList(rs.getString("pixels_weight").split(",")));
				
				for(Point p:points){
					int newPixelId = (p.y * imageWidth) + p.x;
					int newPixelIndex =  affectedPixels.indexOf(newPixelId+"");
					if(newPixelIndex!=-1){	// pixel already exists
						int oldWieght = Integer.parseInt(pixlesWeight.get(newPixelIndex));
						pixlesWeight.set(newPixelIndex, ++oldWieght+"");
					}else{	// pixel not exist
						affectedPixels.add(newPixelId+"");
						pixlesWeight.add("1");
					}
				}
				parameters.clear();
				sql = "update tbl_img_t0_vertices set affected_pixels=? ,pixels_weight=? where vid=?";
				parameters.add(Common.getCommaSeparatedString(affectedPixels));
				parameters.add(Common.getCommaSeparatedString(pixlesWeight));
				parameters.add(vid);
			}else{
				affectedPixels = new ArrayList<String>();
				pixlesWeight = new ArrayList<String>();
				for(Point p:points){
					int newPixelId = (p.y * imageWidth) + p.x;
					affectedPixels.add(newPixelId+"");
					pixlesWeight.add("1");
				}
				parameters.clear();
				sql = "insert into tbl_img_t0_vertices (vid,affected_pixels,pixels_weight) values (?,?,?)";
				parameters.add(vid);
				parameters.add(Common.getCommaSeparatedString(affectedPixels));
				parameters.add(Common.getCommaSeparatedString(pixlesWeight));
			}
			
			updateDatabase(sql,parameters,false);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void initializeDBTables(){
		
		try {
			updateDatabase("TRUNCATE TABLE tbl_img_t0_vertices", null, false);
			updateDatabase("TRUNCATE TABLE tbl_img_t1_vertices", null, false);
			updateDatabase("TRUNCATE TABLE tbl_img_t2_vertices", null, false);
			//updateDatabase("TRUNCATE TABLE tbl_img_t0_pixels", null, false);
			updateDatabase("TRUNCATE TABLE tbl_vertex_rows", null, false);
			/*
			ArrayList<Object> parameters = new ArrayList<Object>();
			String sql = "";
			for(int i=0;i<imageWidth*imageHeight;i++){
				sql = "insert into tbl_img_t0_pixels (pid,affected_vertices,vertices_weight) values (?,?,?)";
				parameters.add(i);
				parameters.add("");
				parameters.add("");
				
				updateDatabase(sql,parameters,false);
				
				parameters.clear();
			}
			*/
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public Map<Integer,String> getAllVertices(){
		Map<Integer,String> allVertices = new HashMap<Integer,String>();
		String sql="select * from tbl_airport";
		ResultSet rs;
		try {
			rs = queryDatabase(sql, null);
			while(rs.next()){
				allVertices.put(rs.getInt("code"), rs.getString("description"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		return allVertices;
	}

}
