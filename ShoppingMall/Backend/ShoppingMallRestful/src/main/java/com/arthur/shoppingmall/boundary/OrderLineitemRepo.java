package com.arthur.shoppingmall.boundary;


import java.util.List;
import javax.jdo.Query;

import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;

public class OrderLineitemRepo extends Repo {
	
	public List<OrderLineitem> getOrderLineitems(String orderId){
		
		Query q = super.getPersistenceManager().newQuery(
	      		    "SELECT FROM com.arthur.shoppingmall.domain.OrderLineitem where orderId =='" + orderId +"'" );
		      
	    List<OrderLineitem> orderitmes = (List<OrderLineitem>)q.execute();
	    
		
		return orderitmes;
		

	}
	
	public void addOrder(OrderLineitem orderItem){
		super.getPersistenceManager().makePersistent(orderItem);
		
	}

}
