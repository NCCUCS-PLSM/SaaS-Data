package com.arthur.mta.utbdbservice.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateChecker {

	public static Date isValidDate(String dateToValidate ,String dateFromat){
		 
		if(dateToValidate == null){
			return null;
		}
 
		SimpleDateFormat sdf = new SimpleDateFormat(dateFromat);
		sdf.setLenient(false);
 
		try {
 
			//if not valid, it will throw ParseException
			Date date = sdf.parse(dateToValidate);
			//System.out.println(date);
			return date;
 
		} catch (ParseException e) {
			if(!dateFromat.equals("yyyy/MM/dd")){
				return isValidDate(dateToValidate, "yyyy/MM/dd");
			}else{
				//e.printStackTrace();
				return null;
			}

		}
 
		
	}
	
	
}
