package com.arthur.shoppingmall.boundary;

import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.util.DataNucleusHelper;


public class ProductRepo extends Repo {
	
	public List<Product> getProducts(){	

		
	    Query q = super.getPersistenceManager().newQuery(
      		    "SELECT FROM com.arthur.shoppingmall.domain.Product ");
	      
        List<Product> products = (List<Product>)q.execute();
    

		return products;
		
		

	}
	
	public Product getProduct(String productId){	
		

		Query q = super.getPersistenceManager().newQuery(
      		    "SELECT FROM com.arthur.shoppingmall.domain.Product where productId=='"+ productId +"'");
	      
        List<Product> products = (List<Product>)q.execute();
		return products.get(0);
		
		
	}
	
	public void save(Product product){
		/*
		Operation operation = new Save(new OperationObject(product , product.fetchFields() ), 
				this.tenantId);
		operation.excute();
		*/
	}
	


}
