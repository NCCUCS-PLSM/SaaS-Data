package com.arthur.shoppingmall.view;

public class ViewField {
	
	private String fieldName;
	private String fieldType;
	private String fieldValue;
	private String controlType;
	
	public ViewField(String fieldName ,  String fieldType , String fieldValue , String controlType){
		
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.fieldValue = fieldValue;
		this.controlType = controlType;
		
	}
	
	public ViewField(String fieldName ,  String fieldType , String fieldValue){
		
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.fieldValue = fieldValue;
		transferToControlType();
	}
	
	public String getControlType() {
		return controlType;
	}

	public void transferToControlType(){
		
		if(this.fieldType.equals("string") || this.fieldType.equals("int")  ){
			this.controlType = "textbox";
		}
		
		if(this.fieldType.equals("datetime")  ){
			this.controlType = "calendar";
		}
		
		if(this.fieldType.equals("imagestring")  ){
			this.controlType = "image";
		}
		
	}
	
	
	public String getFieldName() {
		return fieldName;
	}
	public String getFieldType() {
		return fieldType;
	}
	public String getFieldValue() {
		return fieldValue;
	}
	
	
	

}
