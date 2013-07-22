package com.arthur.shoppingmall;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.jdo.Query;

import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.query.typesafe.TypesafeQuery;
import org.datanucleus.util.NucleusLogger;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.context.TenantContextKeeper;


import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.DataNucleusHelper;


public class dataR {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		dataR t = new dataR();
		//t.testOrderLineitem();
		//t.testOrder();
		//t.testSaveProduct();
		//t.testSave();
		t.login(1);
		for (int i = 0; i < 10000; i++) {
			System.out.println("Count: "+i);
			t.testProductOr();
		}
		
		//t.testExtendField();
		//t.testDelete();
		//t.testJoin();
		//t.testUpdate();
	}
	
	
private void login(int tid) {
		
		User u1 = null;
		String userName = "";
		switch (tid) {
		case 1:
			u1 = new User("Apple", "12345",1,1);
			break;
		case 2:
			u1 = new User("Orange", "12345",1,2);
			break;
		case 3:
			u1 = new User("Banana", "12345",3,3);
			break;
		}
		
		TenantContextKeeper.getContext().setTenantUser(u1);
		  
			
	}
	
	private void testUpdate(){
		
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	    
	      try
	      {
	         //p1340858745670
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  productId=='p1340858745670' ");
	        
	          List<Product> result = (List<Product>)q.execute();
	        		
	          for (Product p  : result) {
	        	  
	        	  System.out.println(p.getProductId());
	        	  System.out.println(p.getProductName());
	        	  System.out.println(p.getUnitPrice());
	        	  
	        	  double price = 123.00;
	        	  p.setUnitPrice(price);
	        	  p.setProductName("ABCDE");
	        	
	  	        	  
	  			}

	          
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
	     
	          pm.close();
	      }
	      System.out.println("");
	      System.out.println("");
		
		
	}
	
	private void testDelete(){
		
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	    
	      try
	      {
	         //p1340182225572
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  productId>='1' ");
	        
	          List<Product> result = (List<Product>)q.execute();
	        		
	          for (Product p  : result) {
	        	  
	        	  System.out.println(p.getProductId());
	        	  System.out.println(p.getProductName());
	        	  pm.deletePersistent(p);
	  	        	  
	  			}

	          
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
	     
	          pm.close();
	      }
	      System.out.println("");
	      System.out.println("");
		
	}
	
	private void testExtendField(){
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	        
	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  unitPrice  < 20 || productName >='T' ");
	        	
	          List<Product> result = (List<Product>)q.execute();
	        		
	          for (Product p  : result) {
	        	  System.out.println(p.getProductId());
	        	  System.out.println(p.getProductName());
	        	 /* 
	        	 CustomFieldResult o =  p.getCustomFieldResult();
	        	 List<CustomField> fields = o.getFields();
	        	 for (CustomField customField : fields) {
					System.out.println(customField.getName());
					System.out.println(customField.getValue());
					System.out.println(customField.getType());
				}
				*/
	        
	  	        	//System.out.println("ProductId : " + p.getProductId());
	  	        	//System.out.println("Name : " + p.getProductName());
	        	  
	  	        	/*
	  	        	List<CustomField> exs = p.getExtendFields();
	  	        	for (CustomField extendField : exs) {
	  	        		System.out.println(extendField.fieldName());
					}*/
	  	        	  
	  	        	  
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
	
	private void testJoin(){
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          String queryString = "SELECT FROM com.arthur.shoppingmall.domain.Order EXCLUDE SUBCLASSES ";
	          queryString  += " WHERE orderLineitems.contains(ol) && (ol.productId  >'1')";
	    
	          
	          Query q = pm.newQuery(queryString);
	        		List<Order> result = (List<Order>)q.execute();
	        		
	        	    for (Order o  : result) {

	  	        	  System.out.println("OrderId : " + o.getOrderId());
	  	        	  System.out.println("OrderDate : " +  o.getOrderDate());
	  	        	  System.out.println("OrderAmount : " + o.getOrderAmount());
	  	        	  System.out.println("Customer : " +  o.getCustomer());
	  	        	  
	  	        	  List<OrderLineitem> ols = o.getOrderLineitems();
	  	        	  for (OrderLineitem orderLineitem : ols) {
	  	        		  System.out.println("Item : " +  orderLineitem.getOrderLineitemId());  	 
	  	        		  System.out.println("Qty : " + orderLineitem.getQty());
	  		        	  System.out.println("SubTotal : " + orderLineitem.getSubTotal());
	  	        	  }
	  	        	  
	  	        	  
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
	
	private void testProductOr(){
		
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  unitPrice  > 20  ");
	        		List<Product> results = (List<Product>)q.execute();

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

	private void testSaveProduct(){
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	          java.util.Date date= new java.util.Date();

		      String productId = "p" + Long.toString(date.getTime());
		      
		      Product p = new Product();
		      p.setProductId(productId);
		      p.setProductName("Product"+ Long.toString(date.getTime()));
		      p.setUnitPrice(120);
		
		          
		      jdopm.makePersistent(p);


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
	
	private void testSave(){
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	          java.util.Date date= new java.util.Date();
	        
	         
	     	 String orderId = "o" + Long.toString(date.getTime());
	     	 String orderLineitmeId = "ol" + Long.toString(date.getTime());
	     	 
	     	 
	          OrderLineitem ol = new OrderLineitem();
	          ol.setOrderId(orderId);
	          ol.setOrderLineitemId(orderLineitmeId);
	          ol.setProductId("1");
	          ol.setQty("2");
	          ol.setSubTotal(100);
	          
	          
	          Order o = new Order();
	          o.setOrderId(orderId);
	          o.setCustomer("ABCDEFG");
	          o.setOrderAmount(1200);
	        
	          Date d = null;
	          Calendar cal = GregorianCalendar.getInstance();
	          cal.set(2012, 6, 13);
	          d = cal.getTime();
	          
	          o.setOrderDate(d);
	          o.addLineitem(ol);
	          
	          jdopm.makePersistent(o);


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
	
	private void testOrder(){
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	          TypesafeQuery<Order> tq = jdopm.newTypesafeQuery(Order.class);
	        
	          
	          /*
	          List<Order> results = 
	        		    tq.filter(cand.tenantId.eq(1).and(cand.orderId.gt("0")).
	        		    		or(cand.orderDate.gt("2012/04/12")).and(cand.customer.eq("1"))).executeList();
	        		  */          
	        		    		
	          /*
	          List<Order> results = 
	        		    tq.filter(cand.tenantId.eq(1).and(cand.orderId.gt("0")).
	        		    		or(cand.orderDate.gt("2012/04/12").and(cand.customer.eq("1")))).executeList();	        		  	
	        		 */ 
	          
	          /*
	          List<Order> results = 
	        		    tq.filter(cand.orderId.gt("1")).executeList();	
	          */
	          
	          Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Order WHERE orderId == '1' ");
	        		List<Order> results = (List<Order>)q.execute();  
	          
	          /*
	          Query q = pm.newQuery("select from " + Order.class.getName() 
	        		  + "  where "
	        		  + "orderLineitems.contains(c) && c.orderId == orderId ");
	        		q.declareVariables(OrderLineitem.class.getName() + " c");
	        		
	        	Object o = q.execute();
	        	*/	         
	 /*
	          List<Order> results = 
	        		    tq.filter(cand.tenantId.eq(1).and(cand.orderId.gt("0").
	        		    		or(cand.orderDate.gt("2012/04/12")).and(cand.customer.eq("1")))).executeList();
	         */	   
	          
	          //List<Order> results = 
	        	//	    tq.filter(cand.tenantId.eq(1).and(cand.orderId.gt("0"))).executeList();

	        	
	          for (Order o  : results) {

	        	  System.out.println("OderID : " + o.getOrderId());
	        	  System.out.println("OrderDate : " +  o.getOrderDate());
	        	  System.out.println("OrderAmount : " + o.getOrderAmount());
	        	  System.out.println("Customer : " +  o.getCustomer());
	        	  List<OrderLineitem> ols = o.getOrderLineitems();
	        	  for (OrderLineitem orderLineitem : ols) {
	        		  System.out.println("Item : " +  orderLineitem.getOrderLineitemId());  	 
	        		  System.out.println("Qty : " + orderLineitem.getQty());
		        	  System.out.println("SubTotal : " + orderLineitem.getSubTotal());
	        	  }
	        	  
	        	  
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

	private void testOrderLineitem(){
		
		// Create a PersistenceManagerFactory for this datastore
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("datanucleus.properties");
	  	  
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	        Transaction tx=pm.currentTransaction();

	      tx = pm.currentTransaction();
	      try
	      {
	          tx.begin();
	          JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	          TypesafeQuery<OrderLineitem> tq = jdopm.newTypesafeQuery(OrderLineitem.class);
	         // QOrderLineitem cand = QOrderLineitem.candidate();
	          
	          
	          List<OrderLineitem> results = 
	              tq.executeList();
	          
	       
	          for (OrderLineitem o  : results) {

	        	  //System.out.println("OderID : " + o.getOrderId());
	        	  System.out.println("OrderLineitemId : " +  o.getOrderLineitemId());
	        	  System.out.println("Qty : " + o.getQty());
	        	  System.out.println("SubTotal : " + o.getSubTotal());
	        	  
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
