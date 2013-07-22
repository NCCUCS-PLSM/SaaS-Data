package com.arthur.mta.core.customization;

import java.util.List;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomFieldImpl;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomObjectImpl;
import com.arthur.mta.core.CustomRelationship;
import com.arthur.mta.core.CustomRelationshipImpl;
import com.arthur.mta.core.MultiTenantUser;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.CustomObjectController;

public class CustomizationHandler {

	private static CustomObjectController ctrl = new CustomObjectController();
	
	public static CustomObject newCustomObject(){
		
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		
		CustomObject co = new CustomObjectImpl();
		co.setTenantId( user.getTenantId());
		
		return co;
	}
	
	public static CustomField newCustomField(){
		CustomField cf = new CustomFieldImpl();
		return cf;
	}
	
	public static CustomRelationship newCustomRelationship(){
		CustomRelationship cr = new CustomRelationshipImpl();
		return cr;
	}
	
	public static void saveCustomObjectMetaData(CustomObject customObject){
		ctrl.saveCustomObjectMetaData(customObject);
	}
	
	public static void saveCustomObject(CustomObject customObject){
		ctrl.saveCustomObject(customObject);
	}
	
	public static void deleteCustomObject(CustomObject customObject){
		ctrl.deleteCustomObject(customObject);
	}
	
	public static CustomObject findCustomObjectMetaData(int customObjectId){
		return ctrl.findCustomObjectMetaData(customObjectId);
	}
	
	public static CustomObject findCustomObjectMetaData(String customObjectName){
		return ctrl.findCustomObjectMetaData(customObjectName);
	}
	
	public static List<CustomObject> findCustomObjectsMetaData(){
		return ctrl.findCustomObjectsMetaData();
	}
	
	public static CustomObject findCustomObject(int customObjectId , String pkValue){
		return ctrl.findCustomObject(customObjectId , pkValue);
	}
	
	public static List<CustomObject> findCustomObjects(int customObjectId){
		return ctrl.findCustomObjects(customObjectId );
	}
	
	public static List<CustomObject> findCustomObjectsByIndexes(int customObjectId , String value){
		return ctrl.findCustomObjectsByIndexes(customObjectId , value );
	}
	
	public static CustomObject findCustomObjectRelationships(int customObjectId){
		return ctrl.findCustomObjectRelationships(customObjectId );
	}
	
	public static void saveCustomObjectRelatioinship(CustomObject customObject){
		ctrl.saveCustomObjectRelatioinship(customObject);
	}
	
	
}
