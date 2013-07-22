package com.arthur.mta.core.dto;

import java.util.ArrayList;
import java.util.List;


public class ObjectDto {
	
	
	private Integer objectId;
	private Integer tenantId;
    private String objectName;
	private List<FieldDto>  fields ;;
    
	public ObjectDto(){
		
	}
	
	public ObjectDto(Integer objectId , Integer tenantId , String objectName){
		this.objectId = objectId;
		this.tenantId = tenantId;
		this.objectName = objectName;
		fields = new ArrayList<FieldDto>();
	}
	

	public Integer getObjectId() {
		return objectId;
	}
    
	public void setObjectId(Integer objectId) {
		this.objectId = objectId;
	}
	
	
	public Integer getTenantId() {
		return tenantId;
	}
	
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	
	public String getObjectName() {
		return objectName;
	}
	
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	
	public List<FieldDto> getFields() {
		return this.fields;
	}

	public void setFields(List<FieldDto>  fields) {
		this.fields = fields;
	}
	
	public void addField(FieldDto  fieldDto) {
		this.fields.add(fieldDto);
	}
	
	
	
	
	

}
