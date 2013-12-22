package com.arthur.mta.utbdbservice.domain;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.arthur.mta.utbdbservice.domain.Field;

public class Object {
	
	private Integer objectId;
	private Integer tenantId;
    private String objectName;
	private Map<String , Field>  fields ;
	private char isDefault;
	
	public Object(){
		
	}
    
	public Object(Integer objectId , Integer tenantId , String objectName , char isDefault){
		this.objectId = objectId;
		this.tenantId = tenantId;
		this.objectName = objectName;
		this.isDefault = isDefault;
	}
	
	public char getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(char isDefault) {
		this.isDefault = isDefault;
	}


	public Integer getObjectId() {
		return objectId;
	}
    
	public void setObjectId(Integer objectId) {
		this.objectId = objectId;
	}
	
	public Integer getTenantId() {
		return tenantId;
	}
	
	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}
	

	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	

	public Map<String , Field> getFields() {
		return this.fields;
	}

	public void setFields(List<Field>  fields) {
		this.fields = new LinkedHashMap<String , Field>();
		for (Field field : fields) {
			this.fields.put(field.getFieldName().toLowerCase(), field);
		}
	}
	
	
	public String columnList(String [] queryFields){
		
		//todo arthur need check convert to lower case
		StringBuffer result = new StringBuffer();
		List<Field> handledFields = new ArrayList<Field>();
		for (int i = 0; i < queryFields.length; i++) {
			Field f = this.fields.get(queryFields[i].toLowerCase());
				if( f != null){
					result.append(f.columnName4Select() + "," );
					handledFields.add(f);
				}else{
					if(queryFields[i].contains("_ID")){
						if(!queryFields[i].contains("_IDX")){
							result.append("value0 as " + queryFields[i] +  ",");
						}
					}else{
						result.append(queryFields[i] +  ",");
					}

				}
		}
		
		for(Map.Entry<String, Field> entry : this.fields.entrySet()) {
			
		    Field f = entry.getValue();
		    boolean isHnadled = false;
		    for (Field field : handledFields) {
				if(f.getFieldId() == field.getFieldId()){
					isHnadled = true;
					break;
				}
			}
		    
		    if(isHnadled == false){
		        result.append(f.columnName4Select() + " ," );
		    }
		}
		
		
		return result.toString().substring(0, result.toString().length() -1);
		
	}
	
	public String columnList(){
		
		StringBuilder result = new StringBuilder();
			
			for(Map.Entry<String, Field> entry : this.fields.entrySet()) {
			    Field f = entry.getValue();
			    result.append(f.columnName4Select() + " ," );
			 
			}
		
		return result.toString().substring(0, result.toString().length() -1);
		
	}
	

	public Field findField(String fieldName){
		
		if(this.fields != null){
			for(Map.Entry<String, Field> entry : this.fields.entrySet()) {
			    //String key = entry.getKey();
			    if (entry.getValue().getFieldName().toLowerCase().equals(fieldName.toLowerCase())) {
					return entry.getValue();
				}
			 
			}
		}
		
		return null;
	}
	
	
	public Field findFieldByNum(String fieldNum){
		
		for(Map.Entry<String, Field> entry : this.fields.entrySet()) {
		    //String key = entry.getKey();
		    if (entry.getValue().getFieldNum().toLowerCase().equals(fieldNum.toLowerCase())) {
				return entry.getValue();
			}		 
		}

		return null;
	}
	
	public Field findPkField(){
		
		for(Map.Entry<String, Field> entry :this.fields.entrySet() ) {
			Field f = entry.getValue();
			if(f.getIndexType().getValue() == 1){
				return f;
			}
		}
		
		return null;
		
	}
	

	

}
