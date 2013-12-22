package com.arthur.mta.utbdbservice.command;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomObjectImpl;
import com.arthur.mta.core.context.TenantContextKeeper;


public class ObjectProvider {
	
	private String objectName;
	private Object object;
	private String [] fields;
	private Map<String , String> fieldValues = new HashMap<String,String>();

	
	public ObjectProvider(Object object ){
		
		this.object = object;		
		Class<? extends Object> c = this.object.getClass();
		
		if(c.equals(CustomObjectImpl.class)){
			this.objectName = ((CustomObject)object).getName();
			this.createFieldValues4CustomObject();
		}else{
			this.objectName = c.getSimpleName();
		}

	}
	
	public ObjectProvider(Object object ,  String [] fields ){
		//this.objectName = objectName;
		this.object = object;
		this.fields = fields;
		createFieldValues();
	}
	
	public Object getObject() {
		return object;
	}

	public String getObjectName() {
		return objectName;
	}

	public String[] getFields() {
		return fields;
	}
	
	public Map<String,String> getFieldValues() {
		return fieldValues;
	}
	
	private void createFieldValues(){
		
		Class<? extends Object> c = this.object.getClass();
		this.objectName = c.getSimpleName();

		for (int i = 0; i < this.fields.length; i++) {
			
			Object value = null;
			if(this.fields[i].equals("TENANT")){
				value =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
			}else{
				value = invokeField(this.fields[i],c);
			}
			
			if(value != null){
				this.fieldValues.put(this.fields[i],  value.toString());
			}
			
		}
	}
	
	private void createFieldValues4CustomObject(){
		
		CustomObject co = ((CustomObject)object);
		
		for (CustomField cf : co.getCustomFields()) {
			this.fieldValues.put(cf.getName(),  cf.getValue());
		}
	
	}
	
	private Object invokeField(String fieldName , Class<? extends Object> c ){
		
		Object value = null;
		
		try {
			
			Field privateField = c.getDeclaredField(fieldName);
			privateField.setAccessible(true);
			value= privateField.get(object);
			

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
	
	/*
	private Object invokeMethod(String fieldName , Class<? extends Object> c ){
		
		Object value = null;
		
		try {
			
			Method getName = c.getMethod("get" + fieldName);
			value= getName.invoke(object);
			

		} catch (NoSuchMethodException e) {
			
			Object extendMap = invokeMethod("ExtendField",c);
			Map<String,String> map = (Map<String,String>)extendMap;
			value = map.get(fieldName);
			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return value;
	}
	*/
	

	

}
