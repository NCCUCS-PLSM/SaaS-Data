package com.arthur.shoppingmall.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;


import com.arthur.mta.utbdbservice.util.CTX;

public class TestPBySQL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		DataSource dataSource =(DataSource) CTX.get().getBean("dataSource");
		TestPBySQL t = new 	TestPBySQL();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		for (int i = 0; i < 10; i++) {
			String pid="";
			if(i==0)pid ="p130520023038-5830477272937";
			if(i==1)pid ="p130520023038-5830870167376";
			if(i==2)pid ="p130520023038-5830935210273";
			if(i==3)pid ="p130520023038-5830989790384";
			if(i==4)pid ="p130520023038-5831043516709";
			if(i==5)pid ="p130520023038-5831095517143";
			if(i==6)pid ="p130520023038-5831138001804";
			if(i==7)pid ="p130520023038-5831183108425";
			if(i==8)pid ="p130520023038-5831223614691";
			if(i==9)pid ="p130520023038-5831262849707";
						
			long startTime = System.nanoTime();    
			t.select(jdbcTemplate, pid);
			//t.insert(jdbcTemplate);
			//t.update(jdbcTemplate, pid);
			//t.delete(jdbcTemplate, pid);
			
			long estimatedTime = System.nanoTime() - startTime;
			double difference =estimatedTime/1e6;
			System.out.println(difference);
		}
		

	}
	
	
	private void select(JdbcTemplate jdbcTemplate , String pid ) {

		
		String sql =  "Select DataGuid from UniqueFields where TenantId=1 and ObjectId=3 and FieldNum=0 and StringValue='" + pid +"'";
		List rows =jdbcTemplate.queryForList(sql);
		String dataGuid="";
		for (java.lang.Object object : rows) {
			Map map = (Map)object;
			dataGuid = map.get("DataGuid").toString();
		}
	        
		String sql2 =  "Select * from Data where DataGuid ='" + dataGuid +"'";
		jdbcTemplate.queryForList(sql2);
		

	
	}
	
	private void insert(JdbcTemplate jdbcTemplate ){
		
		int Min = 100;
		int Max = 200;
		int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
		
		DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
	    Date date2 = new Date();
	    System.out.println();
		String pid = "p"+dateFormat.format(date2)+"-"+System.nanoTime();
	
		String dataGuid= UUID.randomUUID().toString();
		String sql = "INSERT INTO `Data` (`DataGuid` ,`TenantId`, `ObjectId`, `Name`,  `Value0`, `Value1`, `Value2` ";
		sql +=" ) VALUES ('" + dataGuid  +"',";
		sql +=  " 1 , 3 , 'Product' ,'"+pid+"' ,'" + Long.toHexString(Double.doubleToLongBits(Math.random())) +"','" + rndId +"')";

		jdbcTemplate.update(sql);
		
		
		String sql2 = "INSERT INTO `UniqueFields` (`TenantId`, `ObjectId`,`DataGuid` , `FieldNum`, `StringValue` ) " ;
		sql2 +="	VALUES (1,3,'" + dataGuid +"',0,'"+pid +"')";
		
		jdbcTemplate.update(sql2);
		
		
		
	}
	

	
	private void update(JdbcTemplate jdbcTemplate  , String pid){
		
		int Min = 100;
		int Max = 200;
		int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
		
		
		
		String sql =  "Select DataGuid from UniqueFields where TenantId=1 and ObjectId=3 and FieldNum=0 and StringValue='" + pid +"'";
		List rows =jdbcTemplate.queryForList(sql);
		String dataGuid="";
		for (java.lang.Object object : rows) {
			Map map = (Map)object;
			dataGuid = map.get("DataGuid").toString();
		}
		
		
		String sql2 = "Update `Data`  set Value2='"+ rndId +"'";
		sql2 +=" where DataGuid='"+ dataGuid +"'";
		
		//jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update(sql2);
		
		
		
	}
	
	private void delete(JdbcTemplate jdbcTemplate , String pid){
		

		String sql =  "Select DataGuid from UniqueFields where TenantId=1 and ObjectId=3 and FieldNum=0 and StringValue='" + pid +"'";
		List rows =jdbcTemplate.queryForList(sql);
		String dataGuid="";
		for (java.lang.Object object : rows) {
			Map map = (Map)object;
			dataGuid = map.get("DataGuid").toString();
		}
		
		String sql3 = "Delete from `UniqueFields` ";
		sql3 +=" where TenantId=1 and ObjectId=3 and FieldNum=0 and DataGuid='"+ dataGuid +"'";
		jdbcTemplate.update(sql3);
		
		String sql2 = "Delete from `Data` ";
		sql2 +=" where DataGuid='"+ dataGuid +"'";
		jdbcTemplate.update(sql2);
		
	}
	

}
