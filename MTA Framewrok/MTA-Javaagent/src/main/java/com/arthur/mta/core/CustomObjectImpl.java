package com.arthur.mta.core;

import java.util.ArrayList;
import java.util.List;


public class CustomObjectImpl implements CustomObject {

	private List<CustomField> cusFields;
	private List<CustomRelationship> cusRelationships;
	private int tenantId;
	private String name;
	private int id;
	
	public CustomObjectImpl(){
		cusFields = new ArrayList<CustomField>();
		cusRelationships = new ArrayList<CustomRelationship>();
	}
	
	public void addCustomField(CustomField customField){
		this.cusFields.add(customField);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getTenantId() {
		return tenantId;
	}

	public List<CustomField> getCustomFields() {
		return cusFields;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public CustomField getPkCustomField(){
		
		for (CustomField cf : this.cusFields) {
			if(cf.getIndexType().getValue() == 1){
				return cf;
			}
		}
		
		return null;
		
	}

	public List<CustomRelationship> getCustomRelationships() {
		return cusRelationships;
	}

	public void addCustomRelationship(CustomRelationship relationship) {
		this.cusRelationships.add(relationship);
		
	}

}
