package com.arthur.shoppingmall.domain;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;


@PersistenceCapable(identityType =IdentityType.DATASTORE , table="Tenants")
public class Tenant {
	
	@PrimaryKey
	@Persistent
	private int tenantId;
	 
	@Persistent
	private String tenantName;
	
	
	public Tenant(){
		
	}
	
	public Tenant(int tenantId , String tenantName){
		this.tenantId = tenantId;
		this.tenantName = tenantName;
	}

	public int getTenantId() {
		return tenantId;
	}

	public String getTenantName() {
		return tenantName;
	}
	
	 public String toString()
	 {
	     return "TenantId: " + this.tenantId + " TenantName=" + this.tenantName;
	 }
		
	

}

