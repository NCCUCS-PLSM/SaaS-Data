package com.arthur.mta.utbdbservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IRelationshipDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.domain.Association;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.Relationship;
import com.arthur.mta.utbdbservice.domain.UniqueField;



public class ObjectController {
	
	private Integer tenantId;
	public ObjectController(){	
		
		this.tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
	}
	
	public com.arthur.mta.utbdbservice.domain.Object retrieveObject(String objectName ){
		
		  IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
			com.arthur.mta.utbdbservice.domain.Object obj =  dao.findByObjectName(objectName);
		
		return obj;
			
	}
	
	public List<CustomObject> retrieveDetailCustomObjectsByTargetObjIdFieldId(int objectId , int fieldId ,String pkVal , 
			String fieldValue, String detailObjName){
		
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		IRelationshipDao rDao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
		IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
		List<Association> assocList = aDao.findByTargetObjIdFieldId(objectId, fieldId);
	
		if(assocList != null){
			for (Association assoc : assocList) {
				int dObjId = 0;
				if(fieldValue != null && !fieldValue.isEmpty() && !fieldValue.equals("null") && !fieldValue.equals("undefined")){
					dObjId =Integer.parseInt(fieldValue);
				}else{
					com.arthur.mta.utbdbservice.domain.Object dObj = oDao.findByObjectName(detailObjName);	
					dObjId = dObj.getObjectId();
				}
				
				if(assoc.getObjectId() == dObjId ){
					UniqueField uf = uDao.findByStringValue(TenantContextKeeper.getContext().getTenantUser().getTenantId(),
							assoc.getTargetObjectId(), assoc.getTargetFieldNum().toString(),  pkVal);
					List<Relationship> rels = rDao.findRelationByTargetDataGuid(assoc.getAssocationId(), uf.getDataGuid());
					CustomObjectController ctrl = new CustomObjectController();
					List<CustomObject> cos = new ArrayList<CustomObject>();
					if(rels != null){
						for (Relationship relationship : rels) {
							CustomObject co = ctrl.findCustomObject2(relationship.getDataGuid());
							cos.add(co);
						}
						return cos;
					}
				}
			}	
		}
		
		return null;
	}
	
	public List<CustomObject> retrieveMasterCustomObjectByObjIdFieldId(int objectId , int fieldId , String fieldValue){
		
		//IRelationshipDao rDao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
		//IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
		
		Association assoc = aDao.findByObjIdFieldId(objectId, fieldId);
		if(assoc != null){
			List<CustomObject> cos = new ArrayList<CustomObject>();
			CustomObjectController ctrl = new CustomObjectController();
			if(fieldValue != null && !fieldValue.isEmpty()){
				CustomObject co = ctrl.findCustomObject2(assoc.getTargetObjectId(), fieldValue);
				if(co != null) cos.add(co);
			}
			return cos;
		}
		
		return null;
	}
	
	
	//public void saveObject(ObjectDto objectDto){
		
	/*
		IFieldDao dao = (IFieldDao) CTX.get().getBean("fieldDao");
		com.arthur.dbservice.domain.Object obj = transfer(objectDto);
		for (Field f : obj.getFields()) {
			if(f.getFieldId() != null){
				dao.update(f);
			}else{
				dao.save(f);
			}
			
		}
		 */
		 
	//}
	
	//private ObjectDto transferToDto(com.arthur.mta.utbdbservice.domain.Object object){
		
		/*
		ObjectDto objDto = new ObjectDto(object.getObjectId() , object.getTenantId() , object.getObjectName());
		for(Map.Entry<String, Field> entry : object.getFields().entrySet()) {
			FieldDto fDto = new FieldDto();
			Field f = entry.getValue();
			fDto.setFieldId(f.getFieldId());
			fDto.setObjectId(f.getObjectId());
			fDto.setTenantId(f.getTenantId());
			fDto.setFieldName(f.getFieldName());
			fDto.setFieldType(f.getFieldType());
			fDto.setFieldNum(f.getFieldNum());
			fDto.setIndexType(f.getIndexType());
			
			objDto.addField(fDto);
			
		}
		
		
		return objDto;
		*/
	//	return null;
		
	//}
	
//	private com.arthur.mta.utbdbservice.domain.Object transfer(ObjectDto objDto){
		
		/*
		com.arthur.mta.utbdbservice.domain.Object obj = new com.arthur.mta.utbdbservice.domain.Object(
				objDto.getObjectId() , objDto.getTenantId() , objDto.getObjectName());
		
		List<Field> fields = new ArrayList<Field>();
		for (FieldDto fDto : objDto.getFields()) {
			Field f = new Field();
			
			f.setFieldId(fDto.getFieldId());
			f.setObjectId(fDto.getObjectId());
			f.setTenantId(fDto.getTenantId());
			f.setFieldName(fDto.getFieldName());
			f.setFieldType(fDto.getFieldType());
			f.setFieldNum(fDto.getFieldNum());
			f.setIndexType(fDto.getIndexType());
			
			fields.add(f);
			
		}
		
		obj.setFields(fields);
		
		return obj;
		*/
		
		//return null;
	//}
	

}
