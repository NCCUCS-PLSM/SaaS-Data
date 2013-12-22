package com.arthur.shoppingmall.test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.util.NucleusLogger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.util.DataNucleusHelper;

public class Tenant1JdoqlTest {
	
	private PersistenceManager pm ;

	
	@Before
	public void setUp() throws Exception {
		
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
		pm = pmf.getPersistenceManager();
	}
	
	@After
	public void tearDown() throws Exception {
		pm.close();
	}

	@Test
	public void testUpdate(){
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
	
	@Test
	public void testDelte(){
		
		  //p1340182225572
        Query q = pm.newQuery(
      		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  productId =='p1340278107903' ");
      
        List<Product> result = (List<Product>)q.execute();
      		
        for (Product p  : result) {
      	  
      	  System.out.println(p.getProductId());
      	  System.out.println(p.getProductName());
      	  pm.deletePersistent(p);
	        	  
			}
		
	}
	
	
	@Test
	public void testJoin(){
		
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
		
	}
	
	
	@Test
	public void testSave(){
	
			
	     JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	      java.util.Date date= new java.util.Date();
	      String productId = "p" + Long.toString(date.getTime());
	      
	      Product p = new Product();
	      p.setProductId(productId);
	      p.setProductName("Product"+ Long.toString(date.getTime()));
	      p.setUnitPrice(120);
	      jdopm.makePersistent(p);

	      
	      Query q = pm.newQuery(
	      		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE productId == '"+ productId +"' ");
		      
	      	List<Product> result = (List<Product>)q.execute();
	      	Assert.assertEquals(productId, result.get(0).getProductId());
		
	}

	@Test
	public void testAssociationSave(){
	
			
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
	      
	      Date d = new Date();
	      o.setOrderDate(d);
	      
	      o.addLineitem(ol);
	          
	      jdopm.makePersistent(o);
	      
	      Query q = pm.newQuery(
      		    "SELECT FROM com.arthur.shoppingmall.domain.Order WHERE orderId == '"+ orderId +"' ");
	      
          List<Order> result = (List<Order>)q.execute();
      	  Assert.assertEquals(orderId, result.get(0).getOrderId());
      		  
		
	}
	
	
	@Test
	public void testAssociationQuery() {
		
		String orderId = "1";
		   Query q = pm.newQuery(
	      		    "SELECT FROM com.arthur.shoppingmall.domain.Order WHERE orderId == '"+ orderId +"' ");
		      
	          List<Order> result = (List<Order>)q.execute();
	          
	          Order order = result.get(0);
	      	  Assert.assertEquals(orderId, order.getOrderId());
	      	  
	      	  List<OrderLineitem> ols  = order.getOrderLineitems();
	      	  for (OrderLineitem orderLineitem : ols) {
				System.out.println(orderLineitem.getOrderLineitemId());
				System.out.println(orderLineitem.getOrderId());
				System.out.println(orderLineitem.getSubTotal());
	      	  }
	      	  
	      	  Assert.assertEquals(2, ols.size());

	      
	}
	
	
	@Test
	public void testQueryProduct1() {
		
	       Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  unitPrice  > 20 & productName >='T' ");
	        		List<Product> result = (List<Product>)q.execute();
	       
	   //System.out.println(result.size());
	   int count = 0;
	   for (Product p : result) {
		 if(p.getProductId().equals("3"))  count +=1;
		 if(p.getProductId().equals("4"))  count +=1;
		 if(p.getProductId().equals("5"))  count +=1;
		 if(p.getProductId().equals("6"))  count +=1;
		 if(p.getProductId().equals("7"))  count +=1;
	   }
	   Assert.assertEquals(5, count);
	 
	        		
	        
	}
	
	@Test
	public void testProductAnd() {
		
	       Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  unitPrice  < 20 & productName >='T' ");
	        		List<Product> result = (List<Product>)q.execute();
	       
	   System.out.println(result.size());
	   Assert.assertTrue(result.size() == 3);
	        		
	        
	}

	@Test
	public void testProductOr() {
		
	       Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE  unitPrice  < 20 | productName >='T' ");
	        		List<Product> result = (List<Product>)q.execute();
	       
	   System.out.println(result.size());
	  // Assert.assertTrue(result.size() == 9);
	        		
	        
	}
	
	@Test
	public void testProductAndOr() {
		
	       Query q = pm.newQuery(
	        		    "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE productId == '2' | (unitPrice  < 20 & productName >='T') ");
	        		List<Product> result = (List<Product>)q.execute();
	       
	   System.out.println(result.size());
	   for (Product product : result) {
		   System.out.println(product.getProductId());
		   System.out.println(product.getUnitPrice());
		   System.out.println(product.getProductName());
	   }
	   
	   Assert.assertTrue(result.size() == 4);
	        		
	        
	}
	
	
	@Test
	public void testProductUnitPriceGreat() {
		
	     Query q = pm.newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE unitPrice > 50 ");
	     
	     List<Product> result = (List<Product>)q.execute();
	        		
	      for (Product product : result) {
	    	  //System.out.println(product.getUnitPrice());
	    	  Assert.assertTrue(product.getUnitPrice() > 50);
	      } 

	}
	
	@Test
	public void testProductUnitPriceLess() {
		
	     Query q = pm.newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE unitPrice < 50 ");
	     
	     List<Product> result = (List<Product>)q.execute();
	        		
	      for (Product product : result) {
	    	  //System.out.println(product.getUnitPrice());
	    	  Assert.assertTrue(product.getUnitPrice() < 50);
	      } 

	}
	
	
	@Test
	public void testProductUnitPriceGreatEqual() {
		
	     Query q = pm.newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE unitPrice >= 100 ");
	     
	     List<Product> result = (List<Product>)q.execute();
	        		
	      for (Product product : result) {
	    	  //System.out.println(product.getUnitPrice());
	    	  Assert.assertTrue(product.getUnitPrice() >= 100);
	      } 

	}
	
	@Test
	public void testProductUnitPriceLessEqual() {
		
	     Query q = pm.newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Product WHERE unitPrice <= 100 ");
	     
	     List<Product> result = (List<Product>)q.execute();
	        		
	      for (Product product : result) {
	    	  //System.out.println(product.getUnitPrice());
	    	  Assert.assertTrue(product.getUnitPrice() <= 100);
	      } 

	}
	
	
	

}
