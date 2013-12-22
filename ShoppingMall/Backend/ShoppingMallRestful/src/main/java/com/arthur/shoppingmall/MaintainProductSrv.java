package com.arthur.shoppingmall;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.arthur.mta.core.CustomObject;
import com.arthur.shoppingmall.boundary.ProductRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.SFField;
import com.arthur.shoppingmall.domain.SFObject;
import com.arthur.shoppingmall.view.ProductDto;
import com.arthur.shoppingmall.view.ProductView;
import com.arthur.shoppingmall.view.ViewField;

@Component
public class MaintainProductSrv implements IMaintainProductSrv{
	
	//private Integer tenantId;
	public MaintainProductSrv(){
		//this.tenantId = tenantId;
	}

	public void saveProduct(ProductView productView) {
		
		String productId = fetchProductId(productView.getFields());
		ProductRepo pRepo = new ProductRepo();
		Product p = null;
		if(productId == null || productId.equals("undefined") || productId.equals("")){
			p = addProduct(productView);
		}else{
			p = pRepo.getProduct(productId);
			p = updateProduct(productView , p);
		}
		
		pRepo.save(p);
		
	}
	
	private Product addProduct(ProductView productView){
		
 
		Product p = new Product();
		
		for (ViewField vf : productView.getFields()) {
			Boolean isExtend = true;
			if(vf.getFieldName().equals("ProductName")) {p.setProductName(vf.getFieldValue());isExtend=false;}
			if(vf.getFieldName().equals("UnitPrice")){ p.setUnitPrice(Double.parseDouble(vf.getFieldValue()));isExtend=false;}
			//todo arthur temp marked
			//if(isExtend) p.addExtendField(vf.getFieldName() ,vf.getFieldValue());
		}
		
		return p;
		
	}

	private Product updateProduct(ProductView productView,Product p){
		
		for (ViewField vf : productView.getFields()) {
			Boolean isExtend = true;
			if(vf.getFieldName().equals("ProductName")){ p.setProductName(vf.getFieldValue());isExtend=false;}
			if(vf.getFieldName().equals("UnitPrice")){ p.setUnitPrice(Double.parseDouble(vf.getFieldValue()));isExtend=false;}
			/*
			if(isExtend) {
				if(!vf.getFieldValue().equals("undefined"))
					
					//todo arthur temp marked
					//p.addExtendField(vf.getFieldName() ,vf.getFieldValue());
				
			}
			*/
		}
		
		return p;
		
	}
	
	private String fetchProductId(List<ViewField> viewFields){
		
		for (ViewField vf : viewFields) {
			if(vf.getFieldName().equals("ProductId")) 
				return vf.getFieldValue();
			
		}
		
		return null;
	}
	
	
	public List<ProductDto> listProducts() {
		
		ProductRepo repo = new ProductRepo();
		List<Product> ps = repo.getProducts();
		List<ProductDto> pDtos = new ArrayList<ProductDto>();
		for (Product p : ps) {
			CustomObject co = (CustomObject)p;
			ProductDto pDto = new ProductDto(co.getId(),p.getProductId() , p.getProductName() ,
					Double.toString(p.getUnitPrice()),p.getThumbnailImage() ,co.getCustomFields());
			pDtos.add(pDto);
		}
		
		return pDtos;
	}

	public ProductDto retrieveProduct(String productId) {
		
		ProductRepo repo = new ProductRepo();
		Product p = repo.getProduct(productId);
		CustomObject co = (CustomObject)p;
		ProductDto pDto = new ProductDto(co.getId() , p.getProductId() , p.getProductName() ,
				Double.toString(p.getUnitPrice()),p.getThumbnailImage() ,co.getCustomFields());
		return pDto ;
		
	}
	
	public ProductView fetchProductSchema(){
		
		ProductRepo repo = new ProductRepo();
		//Product p = repo.getProdcutSchema();
		
		//return p.generateProductView();
		return null;
	}

	/*
	public void saveSchema(Map<String, Object> schemaInfo) {
		//TODO need check
		
		SFObjectRepo objRepo = new SFObjectRepo();
		SFObject obj = objRepo.getSFObject("Product"); 
		List<Map<String,Object>> fields = ((List<Map<String,Object>>)schemaInfo.get("fields"));
		List<SFField> newFields = new ArrayList<SFField>();
	
		for (Map<String, Object> map : fields) {
			if(map.get("isAdd").toString().equals("1")){
				SFField newField = new SFField();
				newField.setFieldName(map.get("fieldName").toString());
				newField.setFieldType(newField.transferControlType(map.get("controlType").toString()));
				newField.setIndexType("0");
				newField.setFieldNum(String.valueOf((obj.getFields().size() + (newFields.size() + 1))));
				newFields.add(newField);			
			}else{
				for (SFField f : obj.getFields()) {
					if (f.getFieldName().equals(map.get("fieldName").toString())) {
						f.setFieldType(f.transferControlType(map.get("controlType").toString()));
						break;
					}
				}
			}
		}
		
		for (SFField f : newFields) {
			obj.addSFField(f);
		}
		
		objRepo.save(obj);
		
	}
	*/

}
