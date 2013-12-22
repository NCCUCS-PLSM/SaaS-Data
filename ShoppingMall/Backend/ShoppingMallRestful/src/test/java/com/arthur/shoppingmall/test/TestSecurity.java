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
import com.arthur.shoppingmall.IOrderSrv;
import com.arthur.shoppingmall.OrderSrv;
import com.arthur.shoppingmall.boundary.UserRepo;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.JsonHelper;
import com.arthur.shoppingmall.view.OrderDto;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestSecurity {
	
	 static {
	       // MTAJavaAgent.initialize();
	    }
	
	
	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security
	 
	 @Test
	public void testUser(){
		
		UserRepo up = new UserRepo();
		up.getUser(1);
		
	} 

	@Test
	public void test() {
		
		login(1);
		//IOrderSrv srv = new OrderSrv();
		//List<OrderDto> dtos = srv.listOrders();
		//logout();
		//System.out.println(JsonHelper.toJson("a", dtos));
		
		//User user =
  			//	 (User)(UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		//System.out.print(user.getTenantId());
		
		login(2);
		//List<OrderDto> dtos2 = srv.listOrders();
		//logout();
		//System.out.println(JsonHelper.toJson("b", dtos2));
		
		//User user2 =
  			//	 (User)(UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		//System.out.print(user2.getTenantId());
	
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
