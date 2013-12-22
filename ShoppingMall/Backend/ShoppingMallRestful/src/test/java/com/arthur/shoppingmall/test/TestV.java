package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.List;

import javax.annotation.Resource;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.query.typesafe.TypesafeQuery;
import org.datanucleus.util.NucleusLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.ResponseBody;


import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;

import com.arthur.mta.javaagent.MTAJavaAgent;
import com.arthur.shoppingmall.boundary.OrderLineitemRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.DataNucleusHelper;


//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestV {

	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security
	
	 static {
	        MTAJavaAgent.initialize();
	    }
	
	@Test
	public void test() {
	
		PersistenceManagerFactory pmf = 
				JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	
		
		//MTApplicationContext ctx = new MTApplicationContext("com.arthur.shoppingmall.domain");
		//ctx.init();
		//Product pp = new Product();
		
		this.login(1);
		  
	       // Transaction tx=pm.currentTransaction();

	      //tx = pm.currentTransaction();
	      try
	      {
	          //tx.begin();
	          //JDOPersistenceManager jdopm = (JDOPersistenceManager)pm;
	          
	          //TypesafeQuery<Order> tq = jdopm.newTypesafeQuery(Order.class);
	          //QOrder cand = QOrder.candidate();
	    	  
	        Query q = pmf.getPersistenceManager().newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Product where productId=='3' && unitPrice > 100 ");
	        
	        List<Product> result = (List<Product>)q.execute();
	        Product p = result.get(0);
		    System.out.println(p.getProductId());
		    System.out.println(p.getProductName());
		    CustomObject co = (CustomObject)p;
		    System.out.println("The tenant Id is " + co.getTenantId());
		    System.out.println("The tenant Id is " + co.getName());   
		    System.out.println(co.getId());
		    
		       List<CustomField> cfs = co.getCustomFields();
		       for (CustomField customField : cfs) {
		    	   System.out.println(customField.getName());
		    	   System.out.println(customField.getValue());
		    	   System.out.println(customField.getType().getValue());
		    	   if(customField.getType().getValue() == 5){
		    		   for (CustomObject rCo : customField.getValues()) {
						System.out.print(rCo.getName());
						for (CustomField customField2 : rCo.getCustomFields()) {
							   System.out.println(customField2.getName());
					    	   System.out.println(customField2.getValue());
						}
					}
		    		   
		    	   }
		    	   
		       }
	        
	        
	    //	  OrderLineitemRepo oRepo = new OrderLineitemRepo();
	  		//List<OrderLineitem> result = oRepo.getOrderLineitems("o1342680800284");
	  		
	    	 // Query q = pm.newQuery(
		      	//	    "SELECT FROM com.arthur.shoppingmall.domain.OrderLineitem where orderId =='o1342680800284'" );
	    	  
		     //     List<OrderLineitem> result = (List<OrderLineitem>)q.execute();
		          		          
		
		          /*
		         OrderLineitem ol = result.get(0);
		         System.out.println(ol.getOrderLineitemId());
		         System.out.println(ol.getSubTotal());
		         System.out.println(ol.getQty());
		       
		     
		       
		      
		       */
		       //System.out.println(((Product)co).getProductName());
		       
		      
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
	    	  pmf.getPersistenceManager().close();
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
