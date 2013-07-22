package com.arthur.mta.utbdbservice.domain;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.FieldType;
import com.arthur.mta.core.IndexType;


public class Field {

	private Integer fieldId;
	private Integer objectId;
	private Integer tenantId;
	private String 	fieldName;
    private FieldType fieldType;	
    private String 	fieldNum;
	private IndexType  indexType;
	


    public Integer getFieldId() {
		return fieldId;
	}
	public void setFieldId(Integer fieldId) {
		this.fieldId = fieldId;
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
	
	
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	

	public FieldType getFieldType() {
		return fieldType;
	}
	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}
	

	public String getFieldNum() {
		return fieldNum;
	}
	
	public void setFieldNum(String fieldNum) {
		this.fieldNum = fieldNum;
	}
	

	public IndexType getIndexType() {
		return indexType;
	}
	public void setIndexType(IndexType indexType) {
		this.indexType = indexType;
	}
	

    
	public String columnName4Select(){
		
		StringBuffer result = new StringBuffer("Value");
		result.append(Integer.toString(Integer.parseInt(this.fieldNum)));
		result.append(" as " + this.fieldName);
		return result.toString();
	}
	
	public String columnName(){
		StringBuffer result = new StringBuffer("Value");
		result.append(Integer.toString(Integer.parseInt(this.fieldNum)));
		return result.toString();
	}
	

	
    
}
