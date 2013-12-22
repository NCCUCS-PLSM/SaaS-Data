package com.arthur.mta.utbdbservice.domain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.util.DateChecker;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.boundary.IDataDao;
import com.arthur.mta.utbdbservice.boundary.IFieldDao;
import com.arthur.mta.utbdbservice.boundary.IIndexDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IRelationshipDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.command.Insert;
import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.command.Update;
import com.arthur.mta.utbdbservice.domain.Data;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.UniqueField;

public class UpdateData {
	
	private Command cmd;
	private Data data;
	private com.arthur.mta.utbdbservice.domain.Object object;

	
	 //TODO need refactoring
	public UpdateData(Command command){
		
		this.cmd = command;
	}
	
	public void excute(){
		

		Update update = (Update)cmd;
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		object = dao.findByObjectName(update.getObjectProvider().getObjectName());
	
		//todo arthur need to update indexfield
		
		this.data = new Data();
		doData();
		//doIndexField();
		//todo datanucleus pk can't change need check
		//doUniqueField();
		
	}
	

	private void doData(){
		
		UniqueField uf = fetchUniqueField();
		if(uf !=null){
			updateData(uf);
		}

	}

	
	private void updateData(UniqueField uf){
		
	
		 IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
		 IIndexDao iDao = (IIndexDao) CTX.get().getBean("indexDao");
		 this.data = dao.findByDataGuid(uf.getDataGuid());
		
		Map<String,String> fieldVaules = this.cmd.getObjectProvider().getFieldValues();
		  //todo logical domain obj pk constraint and composite pks
		  for(Map.Entry<String, String> e :fieldVaules.entrySet()){
				Field field = object.findField(e.getKey().toString());
				if(field != null){
					
					if(field.getFieldType().getValue() == 2){
						Date date = DateChecker.isValidDate(e.getValue(),"EEE MMM dd HH:mm:ss zzz yyyy");
						if(date!=null){
							 DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							 e.setValue(sdf.format(date));
						}
					}
					
					data = setDataProperty(data,field.columnName() , e.getValue());
					if(field.getFieldType().getValue() == 5){
						updateRelationship(uf.getDataGuid() ,field , e.getValue(),dao);
					}
					if(field.getIndexType().getValue() == 2){
						doIndexField(field,e.getValue(),iDao);
					}
				}
			}
		  
		dao.update(data);
		
		
		  
	}
	private void doIndexField(Field field ,String value , IIndexDao iDao){
		
				
		Index index = iDao.findByFieldlNum(object.getObjectId(), field.getFieldNum(), this.data.getDataGuid());
		boolean isAdd = false;
		if(field.getFieldType().getValue() == 0 || field.getFieldType().getValue() == 3
						|| field.getFieldType().getValue() ==4){
			 						
			if(index != null){		
				index.setStringValue(value);
				iDao.update(index);
			}else{
				isAdd = true;
			}			
						
			if(isAdd){
				index = new Index(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
							Integer.parseInt(field.getFieldNum()) , value , null ,null);
				iDao.save(index);
			}
		}
		
		if(field.getFieldType().getValue() == 1){
			BigDecimal d = new BigDecimal(value);
						
			if(index != null){
				index.setNumValue(d);
				iDao.update(index);
			}else{
				isAdd = true;
			}
						
			if(isAdd){
				index = new Index(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
						Integer.parseInt(field.getFieldNum()) , null ,d ,null);
				iDao.save(index);
			}
		}
					
		if(field.getFieldType().getValue() == 2){
			if(index != null){
				index.setDateValue(value);
				iDao.update(index);
			}else{
				isAdd = true;
			}	
			if(isAdd){
				index = new Index(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
							Integer.parseInt(field.getFieldNum()) , null , null ,value);
				iDao.save(index);
			}
		}
							
	}
	
	private void updateRelationship(String dataGuid , Field field , String value , IDataDao dao){
		
		IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
		Association assoc = aDao.findByObjIdFieldId(field.getObjectId(), field.getFieldId());
		if(assoc != null){
			
			IRelationshipDao rDao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
			Relationship rel = rDao.findRelationByPk(assoc.getAssocationId(), dataGuid);
			IFieldDao fdao = (IFieldDao) CTX.get().getBean("fieldDao");
			Field tarF = fdao.findByFieldId(assoc.getTargetFieldId());
			if(rel != null && tarF != null){
				Data targetData = dao.findByPkField(tarF, value);
				if(targetData != null){
					rel.setTargetDataGuid(targetData.getDataGuid());
					rDao.update(rel);
				}
			}else{
				if(tarF != null){
					Data targetData = dao.findByPkField(tarF, value);
					if(targetData != null){
						Relationship relation = new Relationship(assoc.getAssocationId() , dataGuid
								,targetData.getDataGuid(),assoc.getTenantId(),
								assoc.getObjectId(),assoc.getTargetObjectId());
						rDao.save(relation);
					}
					
				}
				
			}
			
			
		}

	}
	

	private UniqueField fetchUniqueField(){
		
		Map<String,String> fieldVaules = this.cmd.getWhereFieldValues();
		IUniqueFieldDao dao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		
		for(Map.Entry<String, String> e :fieldVaules.entrySet()){
			Field field = object.findField(e.getKey().toString());
			if(field != null){
				if(field.getIndexType().getValue() == 1 && e.getValue()!=null){
					//todo arthur type value		
					UniqueField uf = 
							dao.findByStringValue(object.getTenantId(), object.getObjectId(), field.getFieldNum(), e.getValue().toString());
					
					return uf;
						
				}
			}
				 
		}
		
		return  null;
		
		
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
	

	
	/*
	private void doUniqueField(){
		
		Map<String,String> fieldVaules = this.cmd.getObjectProvider().getFieldValues();
		IUniqueFieldDao dao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		
		for(Map.Entry<String, String> e :fieldVaules.entrySet()){
			Field field = object.findField(e.getKey().toString());
			if(field != null){
				if(field.getIndexType()){
					//todo arthur type value
					UniqueField uf = new UniqueField(this.data.getDataGuid() , object.getTenantId() , object.getObjectId(),
							Integer.parseInt(field.getFieldNum()) , e.getValue() , null ,"");
					
					dao.update(uf);
					break;
				}
			}
		}
		
	}*/

}
