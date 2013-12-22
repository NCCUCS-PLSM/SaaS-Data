package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.arthur.shoppingmall.IUserSrv;
import com.arthur.shoppingmall.UserSrv;
import com.arthur.shoppingmall.boundary.UserRepo;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.view.UserDto;

public class TestNonMTARepo {

	@Test
	public void test() {
		
		UserRepo repo = new UserRepo();
		List<User> uses = repo.getUsers();
		for (User user : uses) {
			System.out.println(user.getUsername());
		}
		
	}
	
	@Test
	public void test1() {
		IUserSrv src = new UserSrv(); 
	
		UserDto u = src.getUser(573);
		
		System.out.println(u.getId());
		System.out.println(u.getTenantName());
		Date d = new Date();
		System.out.println(System.currentTimeMillis()+1);
		System.out.println(System.currentTimeMillis()+2);
		System.out.println(System.currentTimeMillis()+3);
	}

}
