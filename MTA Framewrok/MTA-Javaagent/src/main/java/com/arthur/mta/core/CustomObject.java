package com.arthur.mta.core;

import java.util.List;

public interface CustomObject {

	int getId();
	void setId(int id);
	int getTenantId();
	void setTenantId(int tenantId);
	String getName();
	void setName(String name);
	List<CustomField> getCustomFields();
	void addCustomField(CustomField field);
	CustomField getPkCustomField();
	List<CustomRelationship> getCustomRelationships();
	void addCustomRelationship(CustomRelationship relationship);

}
