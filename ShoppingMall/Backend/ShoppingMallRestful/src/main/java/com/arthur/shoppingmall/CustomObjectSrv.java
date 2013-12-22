package com.arthur.shoppingmall;

import java.util.List;

import org.springframework.stereotype.Component;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomRelationship;
import com.arthur.mta.core.FieldType;
import com.arthur.mta.core.IndexType;
import com.arthur.mta.core.customization.CustomizationHandler;

@Component
public class CustomObjectSrv implements ICustomObjectSrv {

	@Override
	public List<CustomObject> listCustomObjects() {
		
		return CustomizationHandler.findCustomObjectsMetaData();
	}

	@Override
	public List<CustomObject> listCustomObjects(int customObjectId) {
		return CustomizationHandler.findCustomObjects(customObjectId);
	}
	
	@Override
	public List<CustomObject> listCustomObjects(int customObjectId,
			String keyword) {
		return CustomizationHandler.findCustomObjectsByIndexes(customObjectId, keyword);
	}

	@Override
	public void addCustomObject(String name) {
		
		CustomObject co = CustomizationHandler.newCustomObject();
		co.setName(name);
		
		CustomField cf = CustomizationHandler.newCustomField();
		cf.setName(name+"Id");
		cf.setFieldNum("0");
		cf.setType(FieldType.Number);
		cf.setIndexType(IndexType.Primarykey);
		
		co.addCustomField(cf);
		
		CustomizationHandler.saveCustomObjectMetaData(co);
		
	}
	
	public void saveCustomRelationships(int customObjectId , String customObjectIds){
		
		CustomObject co = CustomizationHandler.findCustomObjectRelationships(customObjectId);
		String [] ary = customObjectIds.split(",");
		for (int i = 0; i < ary.length; i++) {
			boolean isNew = true;
			int dtlObjId = Integer.parseInt(ary[i]);
			for (CustomRelationship cr : co.getCustomRelationships()) {
				if(cr.getMasterObjectId() == co.getId() && cr.getDetailObjectId() == dtlObjId){
					isNew = false;
					break;
				}
			}
			
			if(isNew){
				CustomRelationship cusRel = CustomizationHandler.newCustomRelationship();
				cusRel.setDetailObjectId(dtlObjId);
				CustomObject dtlObj = CustomizationHandler.findCustomObjectMetaData(dtlObjId);
				cusRel.setDetailObjectName(dtlObj.getName());
				cusRel.setMasterObjectId(co.getId());
				cusRel.setMasterObjectName(co.getName());
				co.addCustomRelationship(cusRel);
			}
		
		}
		
		CustomizationHandler.saveCustomObjectRelatioinship(co);
		
	}

	@Override
	public CustomObject getCustomObjectMetaData(int customObjectId) {
		return CustomizationHandler.findCustomObjectMetaData(customObjectId);
	}

	@Override
	public void saveCustomObjectMetaData(CustomObject customObject) {
		CustomizationHandler.saveCustomObjectMetaData(customObject);
	}

	@Override
	public void saveCustomObject(CustomObject customObject) {
		CustomizationHandler.saveCustomObject(customObject);
	}

	@Override
	public CustomObject getCustomObject(int customObjectId,
			String customObjectPkVal) {
		return CustomizationHandler.findCustomObject(customObjectId, customObjectPkVal);
	}

	@Override
	public CustomObject getCustomObjectMetaData(String customObjectName) {
		return CustomizationHandler.findCustomObjectMetaData(customObjectName);
	}

	@Override
	public CustomObject getCustomObjectRelationship(String customObjectName) {
		CustomObject co = CustomizationHandler.findCustomObjectMetaData(customObjectName);
		return CustomizationHandler.findCustomObjectRelationships(co.getId());
	}

	@Override
	public CustomObject getCustomObjectRelationship(int customObjectId) {
		return CustomizationHandler.findCustomObjectRelationships(customObjectId);
	}

	@Override
	public void deleteCustomRelationships(int customObjectId, String pkVal) {
		
		CustomObject co = CustomizationHandler.findCustomObjectMetaData(customObjectId);
		co.getPkCustomField().setValue(pkVal);
		CustomizationHandler.deleteCustomObject(co);

	}


	
}
