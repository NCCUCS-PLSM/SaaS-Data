package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

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
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.DataNucleusHelper;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestFetchReq {


	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security
	
	 static {
	       // MTAJavaAgent.initialize();
	    }
	
	@Test
	public void test() {
	
		//MTApplicationContext ctx = new MTApplicationContext("com.arthur.shoppingmall.domain");
		//ctx.init();
		//Product pp = new Product();
		
		this.login(1);
		
	  	  PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	  PersistenceManager pm = pmf.getPersistenceManager();
	  	  

	      try
	      {

	            
	         Query q = pm.newQuery(
	        		   "SELECT FROM com.arthur.shoppingmall.domain.Order WHERE orderId == 'o1340182784641' ");
			      
		          List<Order> result = (List<Order>)q.execute();
		          		          
		         Order p = result.get(0);
		         for (OrderLineitem ol : p.getOrderLineitems()) {
					System.out.println(ol.getSubTotal());
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
