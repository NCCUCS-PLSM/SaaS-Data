package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.Resource;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.datanucleus.util.NucleusLogger;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;



import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.javaagent.MTAJavaAgent;
import com.arthur.shoppingmall.IOrderSrv;
import com.arthur.shoppingmall.OrderSrv;
import com.arthur.shoppingmall.boundary.OrderLineitemRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.DataNucleusHelper;
import com.arthur.shoppingmall.view.OrderLineitemDto;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestOrder {

	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security
	
	 static {
	      //  MTAJavaAgent.initialize();
	    }
	 @Test
	 public void testOl(){
		 
		 login(1);
			IOrderSrv srv = new OrderSrv();
			List<OrderLineitemDto> dtos = srv.listOrderLineitems("o1372266764238");
			logout();
		 
	 }
	 
	 @Test
		public void testIndexField() {
					
			this.login(1);
			
		  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
		  	  PersistenceManager pm = pmf.getPersistenceManager();
		  	  
		      try
		      {
		       
		         Query q = pm.newQuery(
		        		   "SELECT FROM com.arthur.shoppingmall.domain.Order WHERE orderAmount >100 && orderDate > best_before_limit " +
		                 "PARAMETERS Date best_before_limit import java.util.Date  ");
		         
		         //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		        Date date = null;
		 		try {
		 			date = new SimpleDateFormat("yyyy/MM/dd").parse("2013/06/1");
		 			// System.out.println(date);
		 		} catch (ParseException e) {
		 			// TODO Auto-generated catch block
		 		}
		   
		        	      
			     List<Order> result = (List<Order>)q.execute(date);
			     for (Order o : result) {
			    	 System.out.println("OrderId : " + o.getOrderId());
	  	        	  System.out.println("OrderDate : " +  o.getOrderDate());
	  	        	  System.out.println("OrderAmount : " + o.getOrderAmount());
	  	        	  System.out.println("Customer : " +  o.getCustomer());
			    	 
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
		         // if (tx.isActive())
		          //{
		         //     tx.rollback();
		         // }
		          pm.close();
		      }
			}
	 
	@Test
	public void test() {
	
		//MTApplicationContext ctx = new MTApplicationContext("com.arthur.shoppingmall.domain");
		//ctx.init();
		//Product pp = new Product();
		
		this.login(1);
		
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  
	
	       // Transaction tx=pm.currentTransaction();

	      //tx = pm.currentTransaction();
	      try
	      {
	          //tx.begin();
	          //JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          //ASCENDING
	          //TypesafeQuery<Order> tq = jdopm.newTypesafeQuery(Order.class);
	          //QOrder cand = QOrder.candidate();
	            //WHERE orderId == 'o1342680779513' 
	         Query q = pm.newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Order ORDER BY orderDate ASC ");
			      
	         
	         q.setRange(0,10);
	         
		          List<Order> result = (List<Order>)q.execute();
		          		        
		          /*
		         Order p = result.get(0);
		         for (OrderLineitem ol : p.getOrderLineitems()) {
					System.out.println(ol.getSubTotal());
				}
		      
		       System.out.println(p.getCustomer());
		       System.out.println(p.getOrderId());
		          */
		       /*
		       CustomObject co = (CustomObject)p;
		       
		       System.out.println("The tenant Id is " + co.getTenantId());       
		      	 
		       List<CustomField> cfs = co.getCustomFields();
		       for (CustomField customField : cfs) {
		    	   System.out.println(customField.getName());
		    	   System.out.println(customField.getValue());
		       }
		       System.out.println(((Order)co).getOrderDate());
		       */
		      
	         // tx.commit();
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
	         // if (tx.isActive())
	          //{
	         //     tx.rollback();
	         // }
	          pm.close();
	      }
		}
	
	@Test
	public void testOL() {
		this.login(1);
		IOrderSrv srv = new OrderSrv();
		List<OrderLineitemDto> dtos = srv.listOrderLineitems("2");
		for (OrderLineitemDto orderLineitemDto : dtos) {
			
			System.out.println(orderLineitemDto.getProductName());
			System.out.println(orderLineitemDto.getUnitPrice());
		}
		
	}
	
	
	@Test
	public void testJoin(){
		
		this.login(1);
		
		
		  String queryString = "SELECT FROM com.arthur.shoppingmall.domain.Order EXCLUDE SUBCLASSES ";
          queryString  += " WHERE orderLineitems.contains(ol) && (ol.productId  >'1')";
          
          PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
          
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
	
	private void logout(){

		  TenantContextKeeper.clearContext();

	}
	

}
