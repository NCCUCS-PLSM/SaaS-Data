package com.arthur.shoppingmall.test;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.arthur.mta.utbdbservice.util.CTX;

public class TestPByPrivateTable {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		DataSource dataSource =(DataSource) CTX.get().getBean("dataSource");
		TestPByPrivateTable t = new 	TestPByPrivateTable();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		
		//100001
		for (int i = 0; i < 10; i++) {
			long startTime = System.nanoTime();    
			int id = i+100022;
			
			//t.select( jdbcTemplate , id);
			//t.insert(jdbcTemplate);
			//t.update(jdbcTemplate , id);
			t.delete(jdbcTemplate , id);
			
			long estimatedTime = System.nanoTime() - startTime;
			double difference =estimatedTime/1e6;
			System.out.println(difference);
		}
		
	}
	
	private void select(JdbcTemplate jdbcTemplate , int id) {
		/*
		 List data = hibernateTemplate.find(
	                "from Data where DataGuid ='" + dataGuid +"'");
	        return (Data) data.get(0);*/
	        
		String sql =  "Select * from PT_Product where ProductId =" + id +"";
		
		
		List rows =jdbcTemplate.queryForList(sql);
		
		/*
		
		for (java.lang.Object object : rows) {
			System.out.println(object);		
		}*/
	
	}
	
	private void insert(JdbcTemplate jdbcTemplate){
		
		
		int Min = 100;
		int Max = 200;
		int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
		
	
		 String sql =  " INSERT INTO `UTB`.`PT_Product` (`Name`, `Price`, `PT_Productcol`, `PT_Productcol1`, `PT_Productcol2`, `PT_Productcol3`) VALUES "; 
		 sql +="('"+UUID.randomUUID().toString()+"', "+rndId+", '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"', '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"', '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"', '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"')";

		jdbcTemplate.update(sql);
		
	}
	
	private void update(JdbcTemplate jdbcTemplate,int id){
		
		
		int Min = 100;
		int Max = 200;
		int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
		
	
		 String sql =  " Update `UTB`.`PT_Product`  set  Price=" + rndId ; 
		 sql +=" where ProductId="+id;

			
		jdbcTemplate.update(sql);
		
	}
	
	private void delete(JdbcTemplate jdbcTemplate , int id){
		
	
		 String sql =  " Delete from `UTB`.`PT_Product` where ProductId="+id;
		
		
		jdbcTemplate.update(sql);
		
	}
	

}
