package com.arthur.mta.utbdbservice.domain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.util.DateChecker;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.boundary.IDataDao;
import com.arthur.mta.utbdbservice.boundary.IIndexDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IRelationshipDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.command.Insert;
import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.domain.Association;
import com.arthur.mta.utbdbservice.domain.Data;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.Relationship;
import com.arthur.mta.utbdbservice.domain.UniqueField;

public class InsertData {
	
	private Command cmd;
	private Data data;
	private com.arthur.mta.utbdbservice.domain.Object object;
	private boolean isLogicalObjectAiPk = false;
	
	 //TODO need refactoring
	
	public InsertData(Command command){
		
		this.cmd = command;
	}
	
	public String excute(){
		
		//todo arthur need handle
		//Index UniqueField RelationShip
		//And DataType Problem
		//todo need add transaction
		
		Insert insert = (Insert)cmd;
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		object = dao.findByObjectName(insert.getObjectProvider().getObjectName());
	
		this.data = new Data();
		String logicalPk = doData();
		
		//TODO need modify
		doUniqueField();
		doIndexField();
		doRelatioinShip();
		
		return logicalPk;
		
	}
	
	private Integer doLogicalObjectAiPk(){
		this.isLogicalObjectAiPk = true;
		IUniqueFieldDao dao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		UniqueField uf =  (UniqueField) dao.findMaxSno(this.object.getTenantId(), this.object.getObjectId());
		
		return uf.getNumValue() +1;
		
	}
	
	private String doData(){
		
		String pk = addData();
		return pk;
		
	}
	
	private String addData(){
		
		String logicalPk = "";
		Map<String,String> fieldVaules = this.cmd.getObjectProvider().getFieldValues();
		
		  //todo logical domain obj pk constraint and composite pks
		  for(Map.Entry<String, String> e :fieldVaules.entrySet()){
				Field field = object.findField(e.getKey().toString());
				
				if(field != null){
					//if(!field.getIndexType().equals("0") && (e.getValue()==null)){
					if(field.getIndexType().getValue() == 1){
						if( e.getValue()==null || e.getValue() == "0"){
							//todo  auto increment pk
							logicalPk = doLogicalObjectAiPk().toString();
							e.setValue(logicalPk);
						}
					}
					if(field.getFieldType().getValue() == 2){
						Date date = DateChecker.isValidDate(e.getValue(),"EEE MMM dd HH:mm:ss zzz yyyy");
						if(date!=null){
							 DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							 e.setValue(sdf.format(date));
						}
					}
					data = setDataProperty(data,field.columnName() , e.getValue());
					
				}
			
			}
		  	  
	
		  data.setDataGuid(UUID.randomUUID().toString());
		  //data.setIsDeleted("0");
		  data.setName(object.getObjectName());
		  data.setObjectId(object.getObjectId());
		  data.setTenantId(object.getTenantId());
		  
		  IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
		  
		  //TODO need refactoring
		  dao.save(data);
		  
		  return logicalPk;
		
	}
	
	
	
	private void doUniqueField(){
		
		Map<String,String> fieldVaules = this.cmd.getObjectProvider().getFieldValues();
		IUniqueFieldDao dao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		
		for(Map.Entry<String, String> e :fieldVaules.entrySet()){
			Field field = object.findField(e.getKey().toString());
			if(field != null){
				if(field.getIndexType().getValue() == 1){
					//todo arthur type value
					int numPkVal = (this.isLogicalObjectAiPk == true ? Integer.parseInt(e.getValue()) : 0);
					UniqueField uf = new UniqueField(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
							Integer.parseInt(field.getFieldNum()) , e.getValue() , numPkVal ,"");
					
					dao.save(uf);
					break;
				}
			}
		}
	}
	
	private void doIndexField(){
		
		Map<String,String> fieldVaules = this.cmd.getObjectProvider().getFieldValues();
		IIndexDao iDao = (IIndexDao) CTX.get().getBean("indexDao");
		
		for(Map.Entry<String, String> e :fieldVaules.entrySet()){
			Field field = object.findField(e.getKey().toString());
			if(field != null){
				if(field.getIndexType().getValue() == 2){
					
					Index index = null;
					
					if(field.getFieldType().getValue() == 0 || field.getFieldType().getValue() == 3 || field.getFieldType().getValue() ==4)
						index = new Index(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
										Integer.parseInt(field.getFieldNum()) , e.getValue() , null ,null);
					
					if(field.getFieldType().getValue() == 1){
						BigDecimal d = new BigDecimal(e.getValue());
						index = new Index(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
								Integer.parseInt(field.getFieldNum()) , null ,d ,null);
					}
					
					if(field.getFieldType().getValue() == 2)
						index = new Index(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
								Integer.parseInt(field.getFieldNum()) , null , null ,e.getValue());
					

					iDao.save(index);
				}
			}
			
				 
		}
		
	}
	

	private void doRelatioinShip(){

		//todo arthur need check if correct
		IRelationshipDao dao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
		IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
		
		List<Association> assocaitons=
				(List<Association>)aDao.find(object.getObjectId());
		
		for (Association a : assocaitons) {
			
			//use fk to select data get dataguid
			String targetPk= fetchFieldValue( a.getFieldId());
			//todo type
			UniqueField uField = 
					uDao.findByStringValue(object.getTenantId(), a.getTargetObjectId(), a.getTargetFieldNum().toString(), targetPk);
			
			//source relatioin index
			//Relationship relation = dao.findRelationByPk(a.getAssocationId(),  this.data.getDataGuid());
			if(uField != null){
				Relationship relation = new Relationship(a.getAssocationId() , this.data.getDataGuid() ,uField.getDataGuid() ,
						object.getTenantId() ,object.getObjectId(), a.getTargetObjectId());
				dao.save(relation);
			}		
					
			//doParentRelationIndex TODO 1 to many
			/*
			Relationship targetRelation = dao.findRelationByPk(a.getAssocationId(),  uField.getDataGuid());
				targetRelation = new Relationship(a.getAssocationId() , uField.getDataGuid() , object.getTenantId() ,
						a.getTargetObjectId() , object.getObjectId());
				dao.save(targetRelation);
			 */
					
				
		}
			
		
	}
	
	
	private String fetchFieldValue(Integer fieldId){
		
		Map<String,String> fieldVaules = this.cmd.getObjectProvider().getFieldValues();
		 for(Map.Entry<String, String> e :fieldVaules.entrySet()){
				Field field = object.findField(e.getKey().toString());
				if(field != null){
					if(fieldId.equals(field.getFieldId())){
						return e.getValue().toString();
					}			
				}
				
			}
		
		return "";
	}
	
	
	private Data setDataProperty(Data data , String columnName , String value){
		
		Class<? extends Data> c = data.getClass();
		Method setName;
	    Class[] arguments = new Class[] { String.class };
		 
		try {
					
			setName = c.getMethod("set" + columnName ,arguments );
			setName.invoke(data ,  new String[] {value});

			 
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		}
		
		return data;
		
	}
	

	
	


}
