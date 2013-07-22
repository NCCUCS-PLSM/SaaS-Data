package com.arthur.shoppingmall.boundary;

import java.util.List;

import javax.jdo.Query;
import com.arthur.shoppingmall.domain.User;


public class UserRepo extends Repo  {

	public List<User> getUsers(){	

	    Query q = super.getPersistenceManager().newQuery(
      		    "SELECT FROM com.arthur.shoppingmall.domain.User ");
	      
        List<User> users = (List<User>)q.execute();
        
		return users;

	}
	
	public User getUser(int userId){	

	    Query q = super.getPersistenceManager().newQuery(
      		    "SELECT FROM com.arthur.shoppingmall.domain.User where id=="+ userId);
	      
        List<User> users = (List<User>)q.execute();
        
		return users.get(0);

	}

}
