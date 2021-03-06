package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.javaagent.MTAJavaAgent;
import com.arthur.shoppingmall.CustomObjectSrv;
import com.arthur.shoppingmall.ICustomObjectSrv;
import com.arthur.shoppingmall.IMaintainProductSrv;
import com.arthur.shoppingmall.IOrderSrv;
import com.arthur.shoppingmall.MaintainProductSrv;
import com.arthur.shoppingmall.OrderSrv;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.JsonHelper;
import com.arthur.shoppingmall.view.OrderDto;
import com.arthur.shoppingmall.view.OrderLineitemDto;
import com.arthur.shoppingmall.view.ProductDto;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestService {

	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security
	
	 static {
	      //  MTAJavaAgent.initialize();
	    }
	 
	 @Test 
	public void testOrders(){
		
		 login(1);
		IOrderSrv srv = new OrderSrv();
		List<OrderDto> dtos = srv.listOrders();
		
		for (OrderDto orderDto : dtos) {
			System.out.println(orderDto.getOrderId());
		}
		
	}
	 
	 @Test 
	 public void testOd(){
		 
		login(1);
		IOrderSrv srv = new OrderSrv();
		List<OrderLineitemDto> dtos = srv.listOrderLineitems("2");
		for (OrderLineitemDto orderLineitemDto : dtos) {
			
		}
		logout();
		 
	 }
	 
	 @Test 
	 public void testAddCustomObj(){
		
		 login(1);
		 
		 ICustomObjectSrv srv = new CustomObjectSrv();
		 srv.addCustomObject("TestCustomObj");
		 
		 logout();
		 
	 }
	 
	 

	@Test
	public void test() {
		
		
		login(1);
		IMaintainProductSrv srv = new MaintainProductSrv();
		ProductDto dto = srv.retrieveProduct("p1340858745670");
		
		System.out.println( JsonHelper.toJson("aa", dto));	
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
