package com.arthur.mta.core;

import java.util.List;

public class ProxyCustomObject implements CustomObject{

	private CustomObjectImpl cusObj ;
	public synchronized int getTenantId() {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		return cusObj.getTenantId();
	}

	
	public synchronized List<CustomField> getCustomFields() {	
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		return this.cusObj.getCustomFields();
	}

	
	public synchronized void addCustomField(CustomField field) {
		
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		this.cusObj.addCustomField(field);
		
	}


	public synchronized void setTenantId(int tenantId) {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		cusObj.setTenantId(tenantId);
	}


	public synchronized void setName(String name) {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		cusObj.setName(name);
		
	}


	public synchronized String getName() {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		return cusObj.getName();
	}
	
	public synchronized int getId() {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		return cusObj.getId();
	}

	public synchronized  void setId(int id) {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		cusObj.setId(id);
	}
	
	public synchronized CustomField getPkCustomField() {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		return cusObj.getPkCustomField();
	}


	public List<CustomRelationship> getCustomRelationships() {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		return this.cusObj.getCustomRelationships();
	}


	public void addCustomRelationship(CustomRelationship relationship) {
		if(this.cusObj == null){
			this.cusObj= new CustomObjectImpl();
		}
		this.cusObj.addCustomRelationship(relationship);
		
	}

}
