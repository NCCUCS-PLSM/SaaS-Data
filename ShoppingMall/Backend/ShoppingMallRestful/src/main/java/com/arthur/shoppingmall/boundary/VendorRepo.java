package com.arthur.shoppingmall.boundary;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.core.controller.TenantController;
import com.arthur.mta.core.dto.TenantDto;
import com.arthur.shoppingmall.view.VendorDto;

public class VendorRepo {
		
	public VendorRepo(){
	
	}
	
	public List<VendorDto> getVenodrs(){
	
		List<VendorDto> vDtos = new ArrayList<VendorDto>();
		
		TenantController tCtrl  = new TenantController();
		List<TenantDto> tDto =  tCtrl.retrieveTenants();
		for (TenantDto tenantDto : tDto) {
			vDtos.add(new VendorDto(tenantDto.getTenantId(), tenantDto.getTenantName()));
		}
		
		return vDtos;
		
		
	}
	
	

}
