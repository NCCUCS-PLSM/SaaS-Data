package com.arthur.shoppingmall.util;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;



public class JsonHelper {

	public static String toJson(String callbackName , List<?> objs){
		
		StringBuilder sb = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();
		sb.append(callbackName + "(");
		try {
			sb.append(mapper.writeValueAsString(objs) );
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sb.append(")");
		
		return sb.toString();
		
		
	}
	
	public static String toJson(String callbackName , Object objs){
		
		StringBuilder sb = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();
		sb.append(callbackName + "(");
		try {
			sb.append(mapper.writeValueAsString(objs) );
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sb.append(")");
		
		return sb.toString();
		
		
	}

}
