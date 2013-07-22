package com.arthur.mta.utbdbservice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.arthur.mta.utbdbservice.boundary.IDataDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.domain.Data;
import com.arthur.mta.utbdbservice.domain.UniqueField;
import com.arthur.mta.utbdbservice.util.CTX;

 class GenerateData {

	 private static final int NUM_THREADS = 10;
	 private static final int NUM_ITERATIONS = 50000;


	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		  DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		   //get current date time with Date()
		   Date date = new Date();
		   System.out.println(dateFormat.format(date));

	
			   GenerateData gd = new GenerateData();
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
	    	IUniqueFieldDao dao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
	    	
	      for (int i = 0; i < NUM_ITERATIONS; i++) {
	    	  generateUniqueField(dao);
	    	  try {
				Thread.sleep(40);
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
	    
	    
	    
	    public void generate(){
			
			int Min = 100;
			int Max = 200;
			int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
			
			 Data data = new Data();
			 String guid =UUID.randomUUID().toString();
			 data.setDataGuid(guid);
			
			 data.setName(Long.toHexString(Double.doubleToLongBits(Math.random())));
			 data.setObjectId(rndId);
			 data.setTenantId(rndId);
			 java.util.Date now = new java.util.Date();
			 String de =(new java.text.SimpleDateFormat().format(now));
			data.setValue0(de );
			data.setValue1( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue2( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue3( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue4( Long.toHexString(Double.doubleToLongBits(Math.random())));
			
			data.setValue5( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue6( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue7( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue8( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue9( Long.toHexString(Double.doubleToLongBits(Math.random())));
			
			data.setValue10( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue11( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue12( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue13( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue14( Long.toHexString(Double.doubleToLongBits(Math.random())));
			
			data.setValue15( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue16( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue17( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue18( Long.toHexString(Double.doubleToLongBits(Math.random())));
			data.setValue19( Long.toHexString(Double.doubleToLongBits(Math.random())));
			
			
			IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
			dao.save(data);
			  //System.out.println(guid);
			
		}
	    
	    
	    public void generateUniqueField(IUniqueFieldDao dao){
			
			int Min = 100;
			int Max = 200;
			int rndId = Min + (int)(Math.random() * ((Max - Min) + 1));
			int rndId2 = Min + (int)(Math.random() * ((Max - Min) + 1));
			
			 UniqueField data = new UniqueField();
			 String guid =UUID.randomUUID().toString();
			 data.setDataGuid(guid);
			
			 data.setObjectId(rndId);
			 data.setTenantId(rndId);
			 data.setFieldNum(0);
			 
			 //java.util.Date now = new java.util.Date();
			 //String de =(new java.text.SimpleDateFormat().format(now));
			 //data.setDateValue(de);
			
			
			 data.setStringValue(Long.toHexString(Double.doubleToLongBits(Math.random())));
			// data.setNumValue(rndId2);
		
			dao.save(data);
			  //System.out.println(guid);
			
		}

	 
	  }
	
	

}
