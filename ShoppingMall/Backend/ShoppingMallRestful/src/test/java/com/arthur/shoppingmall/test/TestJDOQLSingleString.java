package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.query.typesafe.TypesafeQuery;
import org.datanucleus.util.NucleusLogger;
import org.junit.Before;
import org.junit.Test;


import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.util.DataNucleusHelper;

public class TestJDOQLSingleString {
	
	PersistenceManagerFactory pmf ;
	@Before
	public void setUp() throws Exception {
		
		// Create a PersistenceManagerFactory for this datastore
		pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
		
	}

	@Test
	public void testProductIdequal() {
		
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	       Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	    
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE productId == '1' ");
	        		List<Product> results = (List<Product>)q.execute();
	       
	        		 System.out.println("testProductIdequal ================");
	          for (Product p  : results) {
	        	 
	        	  System.out.println("ProductId : " + p.getProductId());
	        	  System.out.println("ProductName : " +  p.getProductName());
	        	  System.out.println("UnitPrice : " + p.getUnitPrice());
	        	  System.out.println("==============================");
	        	  
				}
	        
	          tx.commit();
	      }
	      catch (Error er)
	      {
	          NucleusLogger.GENERAL.error(">> Error querying objects", er);
	          return;
	      }
	      catch (Exception e)
	      {
	          NucleusLogger.GENERAL.error(">> Exception querying objects", e);
	          return;
	      }
	      finally
	      {
	          if (tx.isActive())
	          {
	              tx.rollback();
	          }
	          pm.close();
	      }
	      System.out.println("");
	      System.out.println("");
	}
	
	@Test
	public void testUnitPriceGtLt() {
		
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	       Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	    
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE unitPrice > 40 && unitPrice < 300 ");
	        		List<Product> results = (List<Product>)q.execute();
	       
	        		System.out.println("testUnitPriceGtLt ======");
	          for (Product p  : results) {
	        	
	        	  System.out.println("ProductId : " + p.getProductId());
	        	  System.out.println("ProductName : " +  p.getProductName());
	        	  System.out.println("UnitPrice : " + p.getUnitPrice());
	        	  System.out.println("==============================");
	        	  
				}
	        
	          tx.commit();
	      }
	      catch (Error er)
	      {
	          NucleusLogger.GENERAL.error(">> Error querying objects", er);
	          return;
	      }
	      catch (Exception e)
	      {
	          NucleusLogger.GENERAL.error(">> Exception querying objects", e);
	          return;
	      }
	      finally
	      {
	          if (tx.isActive())
	          {
	              tx.rollback();
	          }
	          pm.close();
	      }
	      System.out.println("");
	      System.out.println("");
	}
	
	
	@Test
	public void testProductIdOr() {
		
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	       Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	    
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  unitPrice  < 20 || productName >='T' ");
	        		List<Product> results = (List<Product>)q.execute();
	       
	        		System.out.println("testProductIdOr ======");
	          for (Product p  : results) {
	        	
	        	  System.out.println("ProductId : " + p.getProductId());
	        	  System.out.println("ProductName : " +  p.getProductName());
	        	  System.out.println("UnitPrice : " + p.getUnitPrice());
	        	  System.out.println("==============================");
	        	  
				}
	        
	          tx.commit();
	      }
	      catch (Error er)
	      {
	          NucleusLogger.GENERAL.error(">> Error querying objects", er);
	          return;
	      }
	      catch (Exception e)
	      {
	          NucleusLogger.GENERAL.error(">> Exception querying objects", e);
	          return;
	      }
	      finally
	      {
	          if (tx.isActive())
	          {
	              tx.rollback();
	          }
	          pm.close();
	      }
	      System.out.println("");
	      System.out.println("");
	}
	

}
