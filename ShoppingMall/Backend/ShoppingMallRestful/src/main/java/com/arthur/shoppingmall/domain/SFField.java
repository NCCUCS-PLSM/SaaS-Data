package com.arthur.shoppingmall.domain;

public class SFField {
	
	private Integer fieldId;
	private String fieldName;
	private String fieldType;
	private String fieldNum;
	private String indexType;
	
	public SFField(){
		
	}
	
	public SFField(Integer fieldId ,String fieldName, String fieldType ,String fieldNum ,String indexType ){
		
			this.fieldId = fieldId;
			this.fieldName = fieldName;
			this.fieldType = fieldType;
			this.fieldNum = fieldNum;
			this.indexType = indexType;
	}
	
	public String transferControlType(String controlType){
		
		if(controlType.equals("label") || controlType.equals("textbox"))
			return "string";
		if(controlType.equals("image"))
			return "imagestring";
		else
			return "string";
		
	}
	
	
	public Integer getFieldId() {
		return fieldId;
	}
	public void setFieldId(Integer fieldId) {
		this.fieldId = fieldId;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public String getFieldType() {
		return fieldType;
	}
	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}
	public String getFieldNum() {
		return fieldNum;
	}
	public void setFieldNum(String fieldNum) {
		this.fieldNum = fieldNum;
	}
	public String getIndexType() {
		return indexType;
	}
	public void setIndexType(String indexType) {
		this.indexType = indexType;
	}
	
	


}