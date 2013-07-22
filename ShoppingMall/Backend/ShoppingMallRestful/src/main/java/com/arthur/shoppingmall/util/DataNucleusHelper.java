package com.arthur.shoppingmall.util;

import java.util.Properties;

public class DataNucleusHelper {
	
	public static Properties createProperties(){
		
		Properties properties = new Properties();
		properties.setProperty("javax.jdo.PersistenceManagerFactoryClass",
		                "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		
		properties.setProperty("datanucleus.metadata.validate","false");

		// Enable these lines if persisting to RDBMS 
		properties.setProperty("javax.jdo.option.ConnectionDriverName","com.mysql.jdbc.Driver");
		properties.setProperty("javax.jdo.option.ConnectionURL","jdbc:mysql://127.0.0.1:3306/UTB");
		properties.setProperty("javax.jdo.option.ConnectionUserName","root");
		properties.setProperty("javax.jdo.option.ConnectionPassword","p@ssw0rd1");
		properties.setProperty("javax.jdo.option.Mapping","mysql");

		
		//#schema creation
		properties.setProperty("datanucleus.autoCreateSchema","false");
		properties.setProperty("datanucleus.autoCreateTables","false");
		properties.setProperty("datanucleus.autoCreateColumns","false");
		properties.setProperty("datanucleus.autoCreateConstraints","false");
		
		//#schema validation
		properties.setProperty("datanucleus.validateTables","false");
		properties.setProperty("datanucleus.validateConstraints","false");
		properties.setProperty("datanucleus.validateColumns","false");
		properties.setProperty("datanucleus.rdbms.CheckExistTablesOrViews","false");
		properties.setProperty("datanucleus.rdbms.initializeColumnInfo","None");
		properties.setProperty("datanucleus.identifier.case","PreserveCase");
		
		//properties.setProperty("datanucleus.tenantId",tenantId);

		properties.setProperty("datanucleus.connectionPoolingType", "DBCP");
		properties.setProperty("datanucleus.connectionPool.maxIdle", "10");
		properties.setProperty("datanucleus.connectionPool.minIdle", "3");
		properties.setProperty("datanucleus.connectionPool.maxActive", "5");
		properties.setProperty("datanucleus.connectionPool.maxWait", "60");
		
		properties.setProperty("datanucleus.connectionPool.maxStatements", "20");
		properties.setProperty("datanucleus.connectionPool.testSQL", "SELECT 1");
		properties.setProperty("datanucleus.connectionPool.timeBetweenEvictionRunsMillis", "2400000");
		properties.setProperty("datanucleus.connectionPool.minEvictableIdleTimeMillis", "18000");
		

		return properties;
		
		
	}

	
}
