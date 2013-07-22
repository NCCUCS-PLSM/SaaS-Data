package com.arthur.mta.utbdbservice.boundary;

import java.util.List;

import com.arthur.mta.utbdbservice.domain.Tenant;

public interface ITenantDao {
	
	public void save(Tenant tenant);
	public void update(Tenant tenant);
	public void delete(Tenant tenant);
	public Tenant findByTenantId(Integer tenantId);
	public List findAll();
}
