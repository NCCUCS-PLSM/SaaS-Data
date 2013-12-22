package com.arthur.mta.core.context;

import com.arthur.mta.core.MultiTenantUser;



public interface TenantContext {
	
	MultiTenantUser getTenantUser();
	
	void setTenantUser(MultiTenantUser tenant);

}
