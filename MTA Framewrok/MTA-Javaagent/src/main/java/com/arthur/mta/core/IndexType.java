package com.arthur.mta.core;

public class IndexType {

	private final String key;
	private final int value;
	public static final IndexType NotIndexed = new IndexType("NotIndexed" , 0);
	public static final IndexType Primarykey = new IndexType("Primarykey",1);
	public static final IndexType Indexed = new IndexType("Indexed" , 2);
	
	private IndexType(final String key , final int value ){
			this.key = key;
	        this.value = value;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public int getValue(){
		return this.value;
	}
	
    public static IndexType getType(final int value) {

    	if(value == 0){
    		return NotIndexed;
    	}else if(value == 1){
    		return Primarykey;
    	}else if(value == 2){
    		return Indexed;
    	}
    	
		return null;
    	
    }
    
}
