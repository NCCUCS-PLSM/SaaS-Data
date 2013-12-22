package com.arthur.mta.utbdbservice.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.arthur.mta.utbdbservice.boundary.ITenantDao;
import com.arthur.mta.utbdbservice.domain.Tenant;


@Repository("tenantDao")
public class TenantDao extends Dao implements ITenantDao{

	
	
	public void save(Tenant tenant) {
		//hibernateTemplate.save(tenant);
		
	}

	public void update(Tenant tenant) {
		// hibernateTemplate.update(tenant);
		
	}

	public void delete(Tenant tenant) {
		// hibernateTemplate.delete(tenant);
		
	}

	public Tenant findByTenantId(Integer tenantId) {

		String sql =  "Select * from Tenants  where TenantId=" + tenantId ;
		List rows =this.queryResult(sql);
		Tenant tenant = null;
		
		for (Object object : rows) {
			tenant = this.createTenant((Map)object);
		}
		
		
		return tenant;
	}

	public List findAll() {		

		String sql =  "Select * from Tenants " ;
		List rows =this.queryResult(sql);
		List<Tenant> tenants =new ArrayList<Tenant>();
		
		for (Object object : rows) {
			tenants.add(this.createTenant((Map)object));
		}
		
	     return tenants;
		
	}
	
	private Tenant createTenant(Map map){
		
		Tenant t = new Tenant();
		t.setTenantId((Integer)map.get("TenantId"));
		t.setTenantName(map.get("TenantName").toString());
		
		return t;
		
	}

}
