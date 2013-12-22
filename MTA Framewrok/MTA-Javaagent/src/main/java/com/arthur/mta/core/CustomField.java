package com.arthur.mta.core;

import java.util.List;

public interface CustomField {
	
	int getId();
	String getName();
	String getValue();
	List<CustomObject> getValues();
	FieldType getType();
	IndexType getIndexType();
	String 	getFieldNum();
	
	void setId(int id);
	void setName(String name);
	void setValue(String value);
	void setValues(List<CustomObject> values);
	void setType(FieldType type);
	void setIndexType(IndexType indexType);
	void setFieldNum(String fieldNum);
	
}
