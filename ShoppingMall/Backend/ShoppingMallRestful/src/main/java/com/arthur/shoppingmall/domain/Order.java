package com.arthur.shoppingmall.domain;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.arthur.mta.core.annotations.MultiTenantable;

@MultiTenantable
@PersistenceCapable(identityType =IdentityType.DATASTORE) 
public class Order {
	
	@PrimaryKey
	private String orderId;
	@Persistent
	private Date orderDate;
	@Persistent
	private double orderAmount;
	@Persistent
	private String customer;
	@Persistent
	private List<OrderLineitem> orderLineitems;

	public Order(){
		orderLineitems = new ArrayList<OrderLineitem>();
	}
	

	public Order(Integer tenantId , String customer ){
		
		orderDate =  new java.util.Date();
		this.customer = customer;
		orderLineitems = new ArrayList<OrderLineitem>();
	}
	
	public void calculateTotalAmount()
    {

          Double oAmount = (double) 0;
          for (OrderLineitem item : orderLineitems) {
        	  oAmount += item.getSubTotal();
          }
          	
          this.orderAmount = oAmount;
    }
	
	public void addLineitem(OrderLineitem item){
		orderLineitems.add(item);
	}
	
	
	public String getCustomer() {
		return customer;
	}

	public double getOrderAmount() {
		return orderAmount;
	}



	public String getOrderId() {
		return orderId;
	}


	public Date getOrderDate() {
		return orderDate;
	}

	public List<OrderLineitem> getOrderLineitems() {
		return orderLineitems;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public void setOrderAmount(double orderAmount) {
		this.orderAmount = orderAmount;
	}
	

	

}
