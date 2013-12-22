package com.arthur.shoppingmall.domain;



import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.arthur.shoppingmall.view.ProductView;
import com.arthur.mta.core.annotations.MultiTenantable;

/**
 * @author arthur
 *
 */

@MultiTenantable
@PersistenceCapable(identityType =IdentityType.DATASTORE)
public class Product  {
	
	@PrimaryKey
	private String productId;
	@Persistent
	private String productName;
	@Persistent
	private double unitPrice;
	@Persistent
	private String thumbnailImage;

	public Product() {
		
	}
	
	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public double getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(double unitPrice) {
		this.unitPrice = unitPrice;
	}
	
	
	public String getThumbnailImage() {
		return thumbnailImage;
	}

	public void setThumbnailImage(String thumbnailImage) {
		this.thumbnailImage = thumbnailImage;
	}
	
	
	/*
	public Map<String, String> getExtendField() {
		//return extendField;
		return null;
	}*/

	/*
	public ProductView generateProductView() {

		
		ProductView pv = new ProductView();
		//todo arthur need modify fieldtype
		pv.addViewField(new ViewField("ProductId", "string", this.productId ,"label"));
		pv.addViewField(new ViewField("ProductName", "string", this.productName));
		pv.addViewField(new ViewField("UnitPrice", "string", Double.toString(this.unitPrice)));
		
		String value= "";
		for (CustomField f : this.cfResult.getFields()) {

			if(! f.getType().equals("childrelationship") && ! f.getType().equals("lookuprelationship")
					&& ! f.getType().endsWith("parentrelationship")){
				
				pv.addViewField(new ViewField(f.getName(), f.getType(), value));

			}

		}

		return pv;
		
		return null;

	}
	*/
	
	
	/*
	public String []  fetchFields(){
		
		Integer length = 3+ this.extendField.keySet().size();
		String [] fields = new String [length];
		fields[0] = "ProductId";
		fields[1] = "ProductName";
		fields[2] = "UnitPrice";
		
		int count = 3;
		Iterator<String> it =  this.extendField.keySet().iterator();  
		  while (it.hasNext()){  
			  fields[count] =(String)it.next(); 
			  count +=1;	      
		  }
		  
		return fields;
	}
	*/

	

}
