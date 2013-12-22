package com.arthur.mta.core.controller;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.core.dto.TenantDto;
//import com.arthur.mta.utbdbservice.boundary.ITenantDao;
//import com.arthur.mta.utbdbservice.domain.Tenant;
//import com.arthur.mta.utbdbservice.util.CTX;

public class TenantController {
	
	public List<TenantDto> retrieveTenants(){
	
		List<TenantDto> dtos = new ArrayList<TenantDto>();
		/*
		 ITenantDao dao = (ITenantDao) CTX.get().getBean("tenantDao");
			List tenants =  dao.findAll();
			
			for (Object obj : tenants) {
				dtos.add(transferToDto((Tenant)obj));
			}
			*/
		return dtos;
	}
	
	private TenantDto transferToDto(){
		
		TenantDto tDto = null ;//= new TenantDto( tenant.getTenantId() , tenant.getTenantName() );
		
		return tDto;
	}

}
