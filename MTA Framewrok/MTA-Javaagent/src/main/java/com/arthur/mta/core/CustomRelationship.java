package com.arthur.mta.core;

public interface CustomRelationship {
	
	int getId();
	int getMasterObjectId();
	String getMasterObjectName();
	int getDetailObjectId();
	String getDetailObjectName();
	
	
	void setId(int id);
	void setMasterObjectId(int id);
	void setMasterObjectName(String name);
	void setDetailObjectId(int id);
	void setDetailObjectName(String name);
	
	

}
