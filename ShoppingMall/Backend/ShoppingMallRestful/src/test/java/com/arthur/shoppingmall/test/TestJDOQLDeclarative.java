package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.datanucleus.util.NucleusLogger;
import org.junit.Before;
import org.junit.Test;

import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.util.DataNucleusHelper;

public class TestJDOQLDeclarative {

	 PersistenceManagerFactory pmf ;
	@Before
	public void setUp() throws Exception {
		
		pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
		
	}

	@Test
	public void testProductIdequal() {
		// Create a PersistenceManagerFactory for this datastore
	  	
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	    
	          Query q = pm.newQuery(Product.class);
	          q.setFilter("productId == '1'");
	          List<Product> results = (List<Product>)q.execute();
	          
	         
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
