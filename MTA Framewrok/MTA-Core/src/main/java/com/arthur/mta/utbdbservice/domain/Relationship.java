package com.arthur.mta.utbdbservice.domain;


public class Relationship {
	
	
	private Integer associationId;
	private String dataGuid;
	private String targetDataGuid;
	private Integer tenantId;
	private Integer sourceObjectId;
	private Integer targetObjectId;
	
	public String getTargetDataGuid() {
		return targetDataGuid;
	}
	public void setTargetDataGuid(String targetDataGuid) {
		this.targetDataGuid = targetDataGuid;
	}

	public Integer getAssociationId() {
		return associationId;
	}
	public void setAssociationId(Integer associationId) {
		this.associationId = associationId;
	}
	

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
	

	public Integer getSourceObjectId() {
		return sourceObjectId;
	}
	public void setSourceObjectId(Integer sourceObjectId) {
		this.sourceObjectId = sourceObjectId;
	}
	

	public Integer getTargetObjectId() {
		return targetObjectId;
	}
	public void setTargetObjectId(Integer targetObjectId) {
		this.targetObjectId = targetObjectId;
	}
	
	public Relationship(){
		
	}
	
	public Relationship(Integer associationId , String dataGuid , String targetDataGuid , Integer tenantId , 
			Integer sourceObjectId ,Integer targetObjectId ){
		
		this.associationId = associationId;
		this.dataGuid = dataGuid;
		this.targetDataGuid = targetDataGuid;
		this.tenantId = tenantId;
		this.sourceObjectId = sourceObjectId;
		this.targetObjectId = targetObjectId;

	}
	
	
	

}
