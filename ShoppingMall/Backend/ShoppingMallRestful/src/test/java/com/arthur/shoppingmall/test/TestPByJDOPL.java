package com.arthur.shoppingmall.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.datanucleus.util.NucleusLogger;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.shoppingmall.boundary.ProductRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.DataNucleusHelper;

public class TestPByJDOPL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		User u1 = new User("Apple", "12345",1,1);
		TenantContextKeeper.getContext().setTenantUser(u1);
		
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
	  	PersistenceManager pm = pmf.getPersistenceManager();

	  	
		TestPByJDOPL t  = new TestPByJDOPL();
		for (int i = 0; i < 10 ; i++) {
			String pid="";
			if(i==0)pid ="p130520135408-10614461347997";
			if(i==1)pid ="p130520135409-10615434421089";
			if(i==2)pid ="p130520135409-10615560010472";
			if(i==3)pid ="p130520135409-10615657896872";
			if(i==4)pid ="p130520135409-10615746871006";
			if(i==5)pid ="p130520135410-10615834084729";
			if(i==6)pid ="p130520135410-10615923046204";
			if(i==7)pid ="p130520135410-10615997769497";
			if(i==8)pid ="p130520135410-10616070739600";
			if(i==9)pid ="p130520135410-10616142308311";
			
			
			
			long startTime = System.nanoTime();    

			//pRepo.getProduct("p1340858745670");
			//t.select(pm , pid);
			//t.insert(pm);
			//t.update(pm, pid);
			t.delete(pm, pid);
			
			long estimatedTime = System.nanoTime() - startTime;
			double difference =estimatedTime/1e6;
			System.out.println(difference);
		}
	
	}
	
	

	private void select(PersistenceManager pm , String pid){
		
	     Query q = pm.newQuery(
	        	"SELECT FROM com.arthur.shoppingmall.domain.Product WHERE productId == '"+ pid +"' ");
			      
		 q.execute();
		          		          
	}
	
	private void insert(PersistenceManager pm){
		
		int Min = 100;
		int Max = 200;
		int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
		
		DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
	    Date date2 = new Date();
	    System.out.println();
		String pid = "p"+dateFormat.format(date2)+"-"+System.nanoTime();
		
		Product  p = new Product();
		p.setProductId(pid);
		p.setProductName(Long.toHexString(Double.doubleToLongBits(Math.random())));
		p.setUnitPrice(rndId);
		
		pm.makePersistent(p);
		
	}
	
	private void update(PersistenceManager pm , String pId){
		
		int Min = 100;
		int Max = 200;
		int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
		
		Product  p = new Product();
		p.setProductId(pId);
		p.setProductName(Long.toHexString(Double.doubleToLongBits(Math.random())));
		p.setUnitPrice(rndId);
		
		pm.makePersistent(p);
		
	}
	
	private void delete(PersistenceManager pm , String pId){
		
		
		Query q = pm.newQuery(
	        	"SELECT FROM com.arthur.shoppingmall.domain.Product WHERE productId == '"+pId+"' ");
		
		q.deletePersistentAll();
		
		
		
	}
	

}
