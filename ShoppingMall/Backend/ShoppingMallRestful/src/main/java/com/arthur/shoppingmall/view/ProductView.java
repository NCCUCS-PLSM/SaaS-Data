package com.arthur.shoppingmall.view;

import java.util.ArrayList;
import java.util.List;

public class ProductView {
	
	private List<ViewField> fields;
	
	public ProductView(){
		fields = new ArrayList<ViewField>();
	}
	
	public void addViewField(ViewField viewField){
		this.fields.add(viewField);
	}

	public List<ViewField> getFields() {
		return fields;
	}
	
	

}
