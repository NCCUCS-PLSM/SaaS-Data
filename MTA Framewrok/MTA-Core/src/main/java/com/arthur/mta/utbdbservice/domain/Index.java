package com.arthur.mta.utbdbservice.domain;

import java.math.BigDecimal;


public class Index {
	
	private String dataGuid;
	private Integer tenantId;
	private Integer objectId;
	private Integer fieldNum;
	private String stringValue;
	private BigDecimal  numValue;
	private String dateValue;
	

	public String getDataGuid() {
		return dataGuid;
	}
	public void setDataGuid(String dataGuid) {
		this.dataGuid = dataGuid;
	}
	
	
	public Integer getTenantId() {
		return tenantId;
	}
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	

	public Integer getObjectId() {
		return objectId;
	}
	public void setObjectId(Integer objectId) {
		this.objectId = objectId;
	}
	

	public Integer getFieldNum() {
		return fieldNum;
	}
	
	public void setFieldNum(Integer fieldNum) {
		this.fieldNum = fieldNum;
	}

	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	

	public BigDecimal getNumValue() {
		return numValue;
	}
	public void setNumValue(BigDecimal numValue) {
		this.numValue = numValue;
	}
	

	public String getDateValue() {
		return dateValue;
	}
	public void setDateValue(String dateValue) {
		this.dateValue = dateValue;
	}
	
	public Index(){
		
	}
	
	public Index(String dataGuid ,Integer tenantId, Integer objectId 
			,Integer fieldNum ,String stringValue ,BigDecimal numValue , String dateValue){
		
		this.dataGuid = dataGuid;
		this.tenantId = tenantId;
		this.objectId = objectId;
		this.fieldNum = fieldNum;
		
		this.stringValue = stringValue;
		this.numValue = numValue;
		this.dateValue = dateValue;
		
	}
	

}
