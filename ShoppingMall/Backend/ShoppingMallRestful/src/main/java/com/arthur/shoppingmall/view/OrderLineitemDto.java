package com.arthur.shoppingmall.view;

public class OrderLineitemDto {
	
	private String orderLineitemId;
	private String productName;
	private String unitPrice;
	private String qty;
	private  double subTotal;
	
	public OrderLineitemDto(String orderLineitemId,String productName,String unitPrice,String qty , double subTotal){
		this.orderLineitemId = orderLineitemId;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.qty = qty;
		this.subTotal = subTotal;
	}
	
	public String getOrderLineitemId() {
		return orderLineitemId;
	}
	public String getProductName() {
		return productName;
	}
	public String getUnitPrice() {
		return unitPrice;
	}
	public String getQty() {
		return qty;
	}

	public double getSubTotal() {
		return subTotal;
	}
	
	
	

}
