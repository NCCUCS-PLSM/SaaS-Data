package com.arthur.mta.core;


import java.util.List;

import com.arthur.mta.core.CustomField;


 public class CustomFieldImpl implements CustomField {
	

	private int id;
	private String name;
	private String value;
	private FieldType type;
	private IndexType indexType;
	private String fieldNum;
	private List<CustomObject> values;
	
	public CustomFieldImpl(){
	}
	
	public CustomFieldImpl(int id , String name , String value ,FieldType fieldType ,  IndexType indexType , String fieldNum){
		this.id = id;
		this.name = name;
		this.value = value;
		this.type = fieldType;
		this.indexType = indexType;
		this.fieldNum = fieldNum;
	}
	
	public int getId() {
		
		return this.id;
	}

	public String getName() {
		
		return this.name;
	}
	public String getValue() {
		
		return this.value;
	}
	public FieldType getType() {
		
		return this.type;
	}

	public IndexType getIndexType() {
		return this.indexType;
	}

	public String getFieldNum() {
		return this.fieldNum;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	public void setIndexType(IndexType indexType) {
		this.indexType = indexType;
	}

	public void setFieldNum(String fieldNum) {
		this.fieldNum = fieldNum;
	}

	public List<CustomObject> getValues() {
		return values;
	}

	public void setValues(List<CustomObject> values) {
		this.values = values;
	}
	
	
	
	


	

}
