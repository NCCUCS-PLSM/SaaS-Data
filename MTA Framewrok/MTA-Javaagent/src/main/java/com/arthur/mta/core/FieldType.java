package com.arthur.mta.core;

public class FieldType {

	private final String key;
	private final int value;
	public static final FieldType String = new FieldType("String" , 0);
	public static final FieldType Number = new FieldType("Number" , 1);
	public static final FieldType DataTime = new FieldType("DataTime" , 2);
	public static final FieldType Imagestring = new FieldType("Imagestring" , 3);
	public static final FieldType Labelstring = new FieldType("Labelstring" , 4);
	public static final FieldType CustomRelationship = new FieldType("CustomRelationship" , 5);
	public static final FieldType RichText = new FieldType("RichText" , 6);
	
	private FieldType(final String key , final int value ){
		this.key = key;
	    this.value = value;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public int getValue(){
		return this.value;
	}
    
    public static FieldType getType(final int value) {

    	if(value == 0){
    		return String;
    	}else if(value == 1){
    		return Number;
    	}else if(value == 2){
        		return DataTime;
        }else if(value == 3){
    		return Imagestring;
        }else if(value == 4){
    		return Labelstring;
        }else if(value == 5){
        	return CustomRelationship;
        }else if(value == 6){
        	return RichText;
        }			
    	
		return null;
    	
    }
    
}
