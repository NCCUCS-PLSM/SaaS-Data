package com.arthur.shoppingmall.boundary;



import java.util.List;
import javax.jdo.Query;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;


public class OrderRepo extends Repo  {
	

	public OrderRepo(){
		super();
		
	}
	
	
	public List<Order> getOrders(){
		
		Query q = super.getPersistenceManager().newQuery(
	      		    "SELECT FROM com.arthur.shoppingmall.domain.Order ORDER BY orderDate DESC ");
			      
	    List<Order> orders = (List<Order>)q.execute();
	    
		return orders;

	}
	
	public void addOrder(Order order){
		super.getPersistenceManager().makePersistent(order);
		
	}
	
	public void deleteOrder(Order order){
		super.getPersistenceManager().deletePersistent(order);
		
	}


}
