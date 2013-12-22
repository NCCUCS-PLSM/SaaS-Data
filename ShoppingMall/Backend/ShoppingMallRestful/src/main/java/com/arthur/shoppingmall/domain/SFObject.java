package com.arthur.shoppingmall.domain;

import java.util.ArrayList;
import java.util.List;

public class SFObject {
	
	private Integer objectId;
	private Integer tenantId;
	private  String objectName;
	private List<SFField> fields;
	
	public List<SFField> getFields() {
		return fields;
	}

	public Integer getObjectId() {
		return objectId;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public String getObjectName() {
		return objectName;
	}

	
	public SFObject(){
		
	}
	
	public SFObject(Integer objectId , Integer tenantId ,String objectName){
		this.objectId = objectId;
		this.tenantId = tenantId;
		this.objectName = objectName;
		fields = new ArrayList<SFField>();
	}
	
	public void addSFField(SFField field){
		this.fields.add(field);
	}

	
}
