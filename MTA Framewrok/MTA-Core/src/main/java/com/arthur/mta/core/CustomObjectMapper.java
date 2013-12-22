package com.arthur.mta.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.core.customization.CustomizationHandler;
import com.arthur.mta.utbdbservice.ObjectController;
import com.arthur.mta.utbdbservice.domain.Association;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.util.DateChecker;


public class CustomObjectMapper {
	
	   public Object mapCustomFields(Object obj , ResultSet resultSet , int [] mappingFields ) {
	    			   
	    	Class<?> pcClass = obj.getClass();
			try {
				
				CustomObject coMd = CustomizationHandler.findCustomObjectMetaData(pcClass.getSimpleName());
	
				Method setTenantId = pcClass.getMethod("setTenantId"  , int.class );
				setTenantId.invoke(obj ,  TenantContextKeeper.getContext().getTenantUser().getTenantId());
				
				Method setId = pcClass.getMethod("setId"  , int.class );
				setId.invoke(obj , coMd.getId());
				
				Method setName = pcClass.getMethod("setName"  , String.class );
				setName.invoke(obj , coMd.getName());
				
				Class<?>[] arguments = new Class[] { CustomField.class };
				Method addField = pcClass.getMethod("addCustomField"  ,arguments );
					
				ObjectController oCtrl = new ObjectController();
				com.arthur.mta.utbdbservice.domain.Object dObj = oCtrl.retrieveObject(pcClass.getSimpleName());
				
				String pkVal = "";
				int pkFieldId =0 ;
				//mappingFields no use
			  // int startIndex = mappingFields[mappingFields.length-1];
		       ResultSetMetaData rsmd  = resultSet.getMetaData();
		       int count = rsmd.getColumnCount();
			    for (int i= 1 ; i <= count ; i++) {
			    	for (CustomField cfMd : coMd.getCustomFields()) {
			    		
			    		if(cfMd.getName().equals(rsmd.getColumnName(i))){
			    			
					        Object valObj = resultSet.getObject(i);
					        String value = "";
					        if(valObj!= null) value =valObj.toString();
					        CustomField cf = new CustomFieldImpl(cfMd.getId(),cfMd.getName() ,value, cfMd.getType()
					        		,cfMd.getIndexType() , cfMd.getFieldNum());
					        if(cfMd.getIndexType().getValue() == 1){
					        	pkVal = value;
					        	pkFieldId = cfMd.getId();
					        }
					        if(cfMd.getType().getValue() ==2){
					        	Date date = DateChecker.isValidDate(cf.getValue(),"yyyy-MM-dd HH:mm:ss");
								if(date!=null){
									 DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
									 cf.setValue(sdf.format(date));
								}
					        }
					        if(cfMd.getType().getValue() == 5){
					        	String [] ary = cfMd.getName().split("_");
					        	for (int j = 0; j < ary.length; j++) {
					        		if(ary[j].equals(dObj.getObjectName())){
					        			List<CustomObject> relCustomObjects = null;
					        			if(j==0){
					        				//master
					        				relCustomObjects = 
					        						oCtrl.retrieveDetailCustomObjectsByTargetObjIdFieldId(
					        								dObj.getObjectId(), pkFieldId, pkVal , value , ary[1]);
					        			}else{
					        				relCustomObjects =
					        						oCtrl.retrieveMasterCustomObjectByObjIdFieldId(
					        						dObj.getObjectId(), cfMd.getId(),value);
					        			}
					        			cf.setValues(relCustomObjects);
					        			break;
					        		}
								}
					        	
					        }
					        
					        addField.invoke(obj , cf);
					         break;
			    		}
					}
			    }

			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
			return obj;
			
		}

}
