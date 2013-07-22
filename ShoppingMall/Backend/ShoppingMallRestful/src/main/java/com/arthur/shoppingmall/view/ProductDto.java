package com.arthur.shoppingmall.view;

import java.util.List;

import com.arthur.mta.core.CustomField;

public class ProductDto {
	
	private int objectId;
	public int getObjectId() {
		return objectId;
	}

	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	private String productId;
	private String productName;
	private String unitPrice;
	private String thumbnailImage;
	private List<CustomField> cusFields;
	
	public ProductDto(){
		
	}
	
	public ProductDto(int objectId , String productId,String productName,String unitPrice ,String thumbnailImage , List<CustomField> customFields){
		this.objectId = objectId;
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.thumbnailImage = thumbnailImage;
		this.cusFields = customFields;
	}
	
	public String getProductId() {
		return productId;
	}
	public String getProductName() {
		return productName;
	}
	public String getUnitPrice() {
		return unitPrice;
	}
	
	public List<CustomField> getCustomFields(){
		return cusFields;
	}
	public String getThumbnailImage() {
		return thumbnailImage;
	}

	public void setThumbnailImage(String thumbnailImage) {
		this.thumbnailImage = thumbnailImage;
	}
	

}
