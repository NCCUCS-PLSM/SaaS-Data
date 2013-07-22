package com.arthur.shoppingmall.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;


import com.arthur.mta.utbdbservice.util.CTX;

public class GeneratePtData {

	 private static final int NUM_THREADS = 9;
	 private static final int NUM_ITERATIONS = 10000;


	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		  DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		   //get current date time with Date()
		   Date date = new Date();
		   System.out.println(dateFormat.format(date));

		   GeneratePtData gd = new GeneratePtData();
		   gd.run();

		
	}
	
	 void run() {
		    for (int i = 0; i < NUM_THREADS; i++) {
		      new AddDataOperation(i).start();
		    }
		  }
	
	
	class AddDataOperation extends Thread {

	    int threadNum;

	    AddDataOperation(int threadNum) {
	      this.threadNum = threadNum;
	    }

	    @Override
	    public void run() {
	    	DataSource dataSource =(DataSource) CTX.get().getBean("dataSource");
	      for (int i = 0; i < NUM_ITERATIONS; i++) {
	    	  generate(dataSource);
	    	  try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	      Date date2 = new Date();
	      System.out.println(dateFormat.format(date2));
	      System.out.println("Thread Complete: " + threadNum);
	    }
	    
	    
	    
	    public void generate(DataSource dataSource){
			
			int Min = 100;
			int Max = 200;
			int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
			
		
			 String sql =  " INSERT INTO `UTB`.`PT_Product` (`Name`, `Price`, `PT_Productcol`, `PT_Productcol1`, `PT_Productcol2`, `PT_Productcol3`) VALUES "; 
			 sql +="('"+UUID.randomUUID().toString()+"', "+rndId+", '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"', '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"', '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"', '"+Long.toHexString(Double.doubleToLongBits(Math.random()))+"')";

				
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			jdbcTemplate.update(sql);
			
			  //System.out.println(guid);
			
		}

	 
	  }

}
