package com.arthur.shoppingmall.domain;


import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.arthur.mta.core.annotations.MultiTenantable;
import com.arthur.shoppingmall.boundary.ProductRepo;

@MultiTenantable
@PersistenceCapable(identityType =IdentityType.DATASTORE)
public class OrderLineitem {

	@PrimaryKey
	@Persistent
	private String orderLineitemId;
	
	@Persistent
	private String orderId;
	
	@Persistent
	private String productId;
	
	@Persistent
	private String qty;
	
	@Persistent
	private Product product;
	
	private double subTotal;
	
	//private Integer tenantId;
	
	
	public OrderLineitem(){
		
	}

	public OrderLineitem(Integer tenantId , String productId , String qty ){
		this.productId = productId;
		this.qty = qty;
		linkProduct(productId);
		calculateSubTotal();
	}
	
	private void linkProduct(String productId){
		
		ProductRepo pRepo = new ProductRepo();
		this.product = pRepo.getProduct(productId);
		
	}
	
	private void calculateSubTotal(){

        Double sTotal = Integer.parseInt(qty) * this.product.getUnitPrice();
        this.subTotal = sTotal;

	}
	
	
	public double getSubTotal() {
		return subTotal;
	}

	public String getOrderLineitemId() {
		return orderLineitemId;
	}

	
	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	
	
	public String getProductId() {
		return productId;
	}

	public String getQty() {
		return qty;
	}

	public void setOrderLineitemId(String orderLineitemId) {
		this.orderLineitemId = orderLineitemId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public void setQty(String qty) {
		this.qty = qty;
	}
	
	public Product getProduct() {
		return product;
	}

	public void setSubTotal(double subTotal) {
		this.subTotal = subTotal;
	}


}
