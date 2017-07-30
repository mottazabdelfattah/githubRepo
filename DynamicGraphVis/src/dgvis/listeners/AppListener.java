package dgvis.listeners;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import dgvis.database.DatabaseConnector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;



/**
 * Application Lifecycle Listener implementation class AppListener
 *
 */
@WebListener
public class AppListener implements ServletContextListener {

    /**
     * Default constructor. 
     */
    public AppListener() {
        // TODO Auto-generated constructor stub
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
    	ServletContext sc = arg0.getServletContext();
    	if(sc.getAttribute("db")!=null){
    		Connection conn = (Connection) sc.getAttribute("db");
    		try {
				conn.close();
				sc.removeAttribute("db");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent arg0) {
    	ServletContext sc = arg0.getServletContext();
   	 
    	if(sc.getAttribute("db")==null){
    		String url = sc.getInitParameter("url");
	    	String user_name = sc.getInitParameter("user_name");
	    	String password = sc.getInitParameter("password");
	    	String database = sc.getInitParameter("database");
	    	DatabaseConnector db = new DatabaseConnector();
	    	db.openConnection(url+database+"?rewriteBatchedStatements=true", user_name, password);
	    	sc.setAttribute("db", db.getConnection());
    	}
    	
    	
    }
    
    
	
}
