package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.shoppingmall.boundary.ProductRepo;
import com.arthur.shoppingmall.domain.User;

public class TestPerformance {

	@Test
	public void test() {
		
		User u1 = new User("Apple", "12345",1,1);
		TenantContextKeeper.getContext().setTenantUser(u1);
		
		long startTime = System.nanoTime();    
		
		ProductRepo pRepo = new ProductRepo();
		pRepo.getProduct("p1340858745670");
		
		
		long estimatedTime = System.nanoTime() - startTime;
		
		
		double difference =estimatedTime/1e6;
		System.out.println(difference);
	}
	
	

}
