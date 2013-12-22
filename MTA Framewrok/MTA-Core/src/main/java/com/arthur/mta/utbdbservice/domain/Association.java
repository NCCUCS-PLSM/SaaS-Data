package com.arthur.mta.utbdbservice.domain;


public class Association {
	
	private Integer assocationId;
	private Integer tenantId;
	private Integer objectId;
	private Integer targetObjectId;
	private Integer fieldId;
	private Integer targetFieldId;
	private Integer fieldNum;
	private Integer targetFieldNum;
	
	public Association(){
		
	}
	

	public Integer getAssocationId() {
		return assocationId;
	}
	public void setAssocationId(Integer assocationId) {
		this.assocationId = assocationId;
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
	

	public Integer getTargetObjectId() {
		return targetObjectId;
	}
	public void setTargetObjectId(Integer targetObjectId) {
		this.targetObjectId = targetObjectId;
	}
	

	public Integer getFieldId() {
		return fieldId;
	}
	public void setFieldId(Integer fieldId) {
		this.fieldId = fieldId;
	}
	

	public Integer getTargetFieldId() {
		return targetFieldId;
	}
	public void setTargetFieldId(Integer targetFieldId) {
		this.targetFieldId = targetFieldId;
	}


	public Integer getFieldNum() {
		return fieldNum;
	}

	public void setFieldNum(Integer fieldNum) {
		this.fieldNum = fieldNum;
	}


	public Integer getTargetFieldNum() {
		return targetFieldNum;
	}

	public void setTargetFieldNum(Integer targetFieldNum) {
		this.targetFieldNum = targetFieldNum;
	}
	
	
	

}
