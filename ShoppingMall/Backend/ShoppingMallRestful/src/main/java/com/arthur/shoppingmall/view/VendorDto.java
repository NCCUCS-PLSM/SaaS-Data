package com.arthur.shoppingmall.view;

public class VendorDto {
	
	private Integer vendorId;
	private String vendorName;
	
	public VendorDto(){
		
	}
	
	public VendorDto(Integer vendorId ,String vendorName ){
		
		this.vendorId = vendorId;
		this.vendorName = vendorName;
	}
	
	public Integer getVendorId() {
		return vendorId;
	}
	public String getVendorName() {
		return vendorName;
	}
	
	

}
