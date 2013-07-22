package com.arthur.shoppingmall.view;

public class OrderDto {
	
	private String orderId;
	private String customer;
	private String orderDate;
	private double orderAmount;
	
	public OrderDto(){
		
	}
	
	public OrderDto(String orderId, String customer, String orderDate, double orderAmount){
		this.orderId = orderId;
		this.customer = customer;
		this.orderDate = orderDate;
		this.orderAmount =  orderAmount;
	}
	
	public String getOrderId() {
		return orderId;
	}
	public String getCustomer() {
		return customer;
	}
	public String getOrderDate() {
		return orderDate;
	}

	public double getOrderAmount() {
		return orderAmount;
	}
	
	public String toJson(){
		
		String json = "[";
		
		
		
		return json;
	}
	
	
	
	

}
