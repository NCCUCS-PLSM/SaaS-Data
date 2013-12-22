package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.arthur.mta.core.MultiTenantUser;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.shoppingmall.domain.User;

public class TestTenantContext {

	@Test
	public void test() {
		
		  new u1().run();
		  //new u2().run();
		  //new u3().run();
		  
	}
	
	class u1 extends Thread {
		
		  @Override
		    public void run() {

			  User u1 = new User("Apple", "12345",1,1);
			  TenantContextKeeper.getContext().setTenantUser(u1);
			  
			  MultiTenantUser u2 = (MultiTenantUser)TenantContextKeeper.getContext().getTenantUser();
			  System.out.println(u2.getTenantId() );
			
			 // TenantContextHolder.clearContext();
			  
			  User u4 = new User("Orange", "12345",1,2);
			  TenantContextKeeper.getContext().setTenantUser(u4);
			  
			  MultiTenantUser u3 = (MultiTenantUser)TenantContextKeeper.getContext().getTenantUser();
			  System.out.println(u3.getTenantId() );

		    }
	}
	
	class u2 extends Thread {
		
		  @Override
		    public void run() {

			  User u1 = new User("Orange", "12345",1,2);
			  TenantContextKeeper.getContext().setTenantUser(u1);
			  
			  MultiTenantUser u2 = (MultiTenantUser)TenantContextKeeper.getContext().getTenantUser();
			  System.out.println(u2.getTenantId());
			
			  TenantContextKeeper.clearContext();

		    }
	}
	
	class u3 extends Thread {
		
		  @Override
		    public void run() {

			  User u1 = new User("Banana", "12345",1,3);
			  TenantContextKeeper.getContext().setTenantUser(u1);
			  
			  MultiTenantUser u2 = (MultiTenantUser)TenantContextKeeper.getContext().getTenantUser();
			  System.out.println(u2.getTenantId() );
			
			  TenantContextKeeper.clearContext();

		    }
	}

}
