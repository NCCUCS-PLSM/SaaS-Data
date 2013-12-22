package com.arthur.mta.core.dto;

public class TenantDto {
	
	private Integer tenantId;
	private String tenantName;
	
	public TenantDto(){
		
	}
	
	public TenantDto(Integer tenantId ,String tenantName ){
		
		this.tenantId = tenantId;
		this.tenantName = tenantName;
	}
	
	public Integer getTenantId() {
		return tenantId;
	}
	public String getTenantName() {
		return tenantName;
	}

}
