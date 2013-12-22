package com.arthur.mta.core;

public class CustomRelationshipImpl implements CustomRelationship{

	
	private int id;
	private int matsertObjId;
	private String masterObjName;
	private int detailObjId;
	private String detailObjName;
	
	public CustomRelationshipImpl(){
	}
	
	public CustomRelationshipImpl(int id , int masterObjId  ,int detailObjId  ){
		this.id = id;
		this.matsertObjId = masterObjId;
		this.detailObjId = detailObjId;
	}
	
	public CustomRelationshipImpl(int id , int masterObjId , String masterObjName ,
			int detailObjId ,String detailObjName ){
		this.id = id;
		this.matsertObjId = masterObjId;
		this.masterObjName = masterObjName;
		this.detailObjId = detailObjId;
		this.detailObjName = detailObjName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMasterObjectId() {
		return this.matsertObjId;
	}

	public String getMasterObjectName() {
		return this.masterObjName;
	}

	public int getDetailObjectId() {
		return this.detailObjId;
	}

	public String getDetailObjectName() {
		return this.detailObjName;
	}
	
	public void setMasterObjectId(int id) {
		this.matsertObjId = id;
	}

	public void setMasterObjectName(String name) {
		this.masterObjName = name;
	}

	public void setDetailObjectId(int id) {
		this.detailObjId= id;
	}

	public void setDetailObjectName(String name) {
		this.detailObjName = name;
	}

	
	

}
