package com.arthur.mta.core.context;

import com.arthur.mta.core.MultiTenantUser;

public class TenantContextImpl implements TenantContext {
	
	
	private MultiTenantUser tenantUser; 


	public MultiTenantUser getTenantUser() {
		return tenantUser;
	}

	public void setTenantUser(MultiTenantUser tenantUser) {
		this.tenantUser = tenantUser;
		
	}

}
