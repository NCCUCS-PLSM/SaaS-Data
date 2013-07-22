package com.arthur.shoppingmall.view;

/**
 * @author arthur
 *
 */
public class UserDto {

	private int id;
	private int tenantId;
	private String tenantName;
	
	public UserDto(int id , int tenantId , String tenantName){
		this.id	= id;
		this.tenantId = tenantId;
		this.tenantName = tenantName;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getTenantId() {
		return tenantId;
	}
	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}
	
	

}
