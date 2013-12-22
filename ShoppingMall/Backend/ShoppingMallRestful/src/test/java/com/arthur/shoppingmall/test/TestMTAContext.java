package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.core.customization.CustomizationHandler;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.User;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestMTAContext {
	
	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security

	@Test
	public void test() {
		/*
		MTApplicationContext ctx = new MTApplicationContext("com.arthur.shoppingmall.domain");
		ctx.init();
		
		Order o = new Order();
		CustomObject co = (CustomObject)o;
		*/
		
	}
	
	@Test
	public void testSaveCustomObject(){
		
		this.login(1);
		CustomObject co = CustomizationHandler.newCustomObject();
		System.out.println(co.getTenantId());
		
		
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
