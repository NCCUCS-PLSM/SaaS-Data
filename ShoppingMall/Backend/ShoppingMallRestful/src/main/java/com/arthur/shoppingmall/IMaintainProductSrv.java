package com.arthur.shoppingmall;

import java.util.List;
import java.util.Map;

import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.view.ProductDto;
import com.arthur.shoppingmall.view.ProductView;

public interface IMaintainProductSrv {
		
	public void saveProduct(ProductView productView);
	public List<ProductDto> listProducts();
	public ProductDto retrieveProduct( String productId);
	public ProductView fetchProductSchema();
	//public void saveSchema(Map<String,Object> schemaInfo);

}
