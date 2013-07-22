package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.core.customization.CustomizationHandler;

import com.arthur.mta.javaagent.MTAJavaAgent;
import com.arthur.shoppingmall.boundary.OrderLineitemRepo;
import com.arthur.shoppingmall.boundary.OrderRepo;
import com.arthur.shoppingmall.boundary.ProductRepo;
import com.arthur.shoppingmall.boundary.TenantRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.User;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestRepo {

	 static {
	        MTAJavaAgent.initialize();
	    }
	 
	 
	 //@Resource(name = "authenticationManager")
		//private AuthenticationManager authenticationManager; // specific for Spring Security
	@Test
	 public void testtenantRepo(){
			for (int i = 0; i < 10000; i++) {
				System.out.println("Count: " + i);
				 TenantRepo pRepo = new TenantRepo();
				 pRepo.getTenant(1);
			}
		 
	 }
	 
		@Test
		public void testProductCon() {
			this.login(1);
			for (int i = 0; i < 10000; i++) {
				System.out.println("Count: " + i);
				ProductRepo pRepo = new ProductRepo();
				List<Product> products = pRepo.getProducts();
				for (Product product : products) {
					CustomObject co = (CustomObject)product;
					System.out.println("The tenantId is "+ co.getTenantId());
					System.out.println(product.getProductId());
					System.out.println(product.getProductName());
					System.out.println(product.getUnitPrice());
				}
			}
	
			
		}
	 
	 
	@Test
	public void testProduct() {
		this.login(1);
		ProductRepo pRepo = new ProductRepo();
		List<Product> products = pRepo.getProducts();
		for (Product product : products) {
			CustomObject co = (CustomObject)product;
			System.out.println("The tenantId is "+ co.getTenantId());
			System.out.println(product.getProductId());
			System.out.println(product.getProductName());
			System.out.println(product.getUnitPrice());
		}
		
	}
	
	@Test
	public void testOrder() {
		this.login(1);
		OrderRepo oRepo = new OrderRepo();
		List<Order> orders = oRepo.getOrders();
		for (Order o : orders) {
			CustomObject co = (CustomObject)o;
			System.out.println("The tenantId is "+ co.getTenantId());
			System.out.println(o.getOrderId());
			System.out.println(o.getCustomer());
			System.out.println(o.getOrderAmount());
			
			for (OrderLineitem ol :  o.getOrderLineitems()) {
				System.out.println("This is the lineitem");
				System.out.println(ol.getOrderLineitemId());
				System.out.println(ol.getSubTotal());
				System.out.println(ol.getQty());
		
			}
		}
		
	
		
	}
	
	@Test
	public void testOrderLineitem() {
		this.login(1);
		OrderLineitemRepo oRepo = new OrderLineitemRepo();
		List<OrderLineitem> orders = oRepo.getOrderLineitems("o1342680800284");
		for (OrderLineitem o : orders) {
			CustomObject co = (CustomObject)o;
			System.out.println("The tenantId is "+ co.getTenantId());
			System.out.println(o.getOrderLineitemId());
			System.out.println(o.getQty());
	
		}
		
	}
	
	@Test
	public void testSaveOrder(){
		this.login(1);
		
		OrderRepo oRepo = new OrderRepo();
	
		int tenantId = TenantContextKeeper.getContext().getTenantUser().getTenantId();
		
		
			Order o = new Order(tenantId,"May");
			String oid = "o"+System.currentTimeMillis();
			System.out.println(oid);
			o.setOrderId(oid);
		

			OrderLineitem item = new OrderLineitem(tenantId , "1" ,"2");
			item.setOrderId(o.getOrderId());
			item.setOrderLineitemId("ol" + System.currentTimeMillis()+ "1");
			o.addLineitem(item);	
					
			
			o.calculateTotalAmount();
			
			oRepo.addOrder(o);
		this.logout();
		
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
