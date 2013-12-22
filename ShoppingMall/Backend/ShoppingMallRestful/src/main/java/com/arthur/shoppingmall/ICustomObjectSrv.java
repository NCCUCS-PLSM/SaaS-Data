package com.arthur.shoppingmall;

import java.util.List;

import com.arthur.mta.core.CustomObject;

public interface ICustomObjectSrv {

	public List<CustomObject> listCustomObjects();
	public List<CustomObject> listCustomObjects(int customObjectId);
	public List<CustomObject> listCustomObjects(int customObjectId , String keyword);
	public void addCustomObject(String name);
	public CustomObject getCustomObjectMetaData(int customObjectId);
	public CustomObject getCustomObjectMetaData(String customObjectName);
	public void saveCustomObjectMetaData(CustomObject customObject);
	public void saveCustomObject(CustomObject customObject);
	public CustomObject getCustomObject(int customObjectId , String customObjectPkVal);
	public CustomObject getCustomObjectRelationship(String customObjectName) ;
	public CustomObject getCustomObjectRelationship(int customObjectId);
	public void saveCustomRelationships(int customObjectId , String customObjectIds);
	public void deleteCustomRelationships(int customObjectId , String pkVal);
}
