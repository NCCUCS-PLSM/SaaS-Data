package com.arthur.mta.utbdbservice.command;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomObjectImpl;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.command.ObjectProvider;

public abstract class Command {
	
	private Integer tenantId ;
	private ObjectProvider objectProvider;
	private String [] whereFields;
	private Map<String , String> whereFieldValues = new HashMap<String,String>();
	
	
	public Map<String, String> getWhereFieldValues() {
		return whereFieldValues;
	}

	public Integer getTenantId(){
		return this.tenantId;
	}
	
	public ObjectProvider getObjectProvider(){
		return objectProvider;
	}
	
	
	public Command(ObjectProvider objectProvider){
		  
		this.tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
		this.objectProvider = objectProvider;
		if(this.getClass().equals(Update.class)|| this.getClass().equals(Delete.class)){
			if(this.objectProvider.getObject().getClass().equals(CustomObjectImpl.class)){
				CustomField cf =((CustomObject)this.objectProvider.getObject()).getPkCustomField();
				this.whereFieldValues.put(cf.getName(), cf.getValue());
			}
		}
		
	}
	
	public Command(ObjectProvider operationObject , String [] whereFields){
		  
		this.tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
		this.objectProvider = operationObject;
		this.whereFields = whereFields;
		this.createWhereFieldValues();
		
	}

	abstract public List excute();
	
	private void createWhereFieldValues(){
		
		Class<? extends Object> c = objectProvider.getObject().getClass();
		//this.objectName = c.getSimpleName();

		for (int i = 0; i < this.whereFields.length; i++) {
			
			Object value = null;
			if(this.whereFields[i].equals("TENANT")){
				value = this.tenantId;
			}else{
				value = invokeField(this.whereFields[i],c);
			}
			
			if(value != null){
				this.whereFieldValues.put(this.whereFields[i],  value.toString());
			}
			
		}
		
		
	}
	
	private Object invokeField(String fieldName , Class<? extends Object> c ){
		
		Object value = null;
		
		try {
			
			Field privateField = c.getDeclaredField(fieldName);
			privateField.setAccessible(true);
			value= privateField.get(objectProvider.getObject());
			

		} catch (NoSuchFieldException e) {
			/*
			Object extendMap = invokeMethod("ExtendField",c);
			Map<String,String> map = (Map<String,String>)extendMap;
			value = map.get(fieldName);
			*/
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return value;
	}
	
}
