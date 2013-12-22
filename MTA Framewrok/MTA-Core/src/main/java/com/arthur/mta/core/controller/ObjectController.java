package com.arthur.mta.core.controller;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.core.dto.FieldDto;
import com.arthur.mta.core.dto.ObjectDto;
//import com.arthur.mta.utbdbservice.boundary.IObjectDao;
//import com.arthur.mta.utbdbservice.domain.Field;
//import com.arthur.mta.utbdbservice.util.CTX;


public class ObjectController {
	
	private Integer tenantId;
	public ObjectController(Integer tenantId){
		this.tenantId = tenantId;
	}
	
	public ObjectDto retrieveObject(String objectName ){
		
		 // IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		//	com.arthur.mta.utbdbservice.domain.Object obj =  dao.findByObjectName(objectName, tenantId);
		
		//return transferToDto(obj);
		return null;
			
	}
	
	public void saveObject(ObjectDto objectDto){
		
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
		 
	}
	
	private ObjectDto transferToDto(){
		
		/*
		ObjectDto objDto = new ObjectDto(object.getObjectId() , object.getTenantId() , object.getObjectName());
		for (Field f : object.getFields()) {
			FieldDto fDto = new FieldDto();
			
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
		return null;
	}
	/*
	private com.arthur.mta.utbdbservice.domain.Object transfer(){
		
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
		
		return null;
	}*/
	

}
