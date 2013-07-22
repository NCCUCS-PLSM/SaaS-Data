package com.arthur.shoppingmallweb;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.arthur.mta.javaagent.MTAJavaAgent;

/**
 * Application Lifecycle Listener implementation class AppStartup
 *
 */
public class AppStartup implements ServletContextListener {

    /**
     * Default constructor. 
     */
	 static {
	       MTAJavaAgent.initialize();
	    }
	
    public AppStartup() {

    }

	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent arg0) {
        // TODO Auto-generated method stub
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
        // TODO Auto-generated method stub
    }
	
}
