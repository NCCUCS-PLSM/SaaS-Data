package com.arthur.mta.utbdbservice.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.UniqueField;

@Repository("uniqueFieldDao")
public class UniqueFieldDao extends Dao implements IUniqueFieldDao{


	
	public void save(com.arthur.mta.utbdbservice.domain.UniqueField uniqueField) {
		
				
		String sql = "INSERT INTO `UniqueFields` (`DataGuid` ,`TenantId`, `ObjectId`, `FieldNum`, `StringValue`, `NumValue`, `DateValue` ) " ;
		sql +="	VALUES ('" + uniqueField.getDataGuid() +"',";
		sql += uniqueField.getTenantId().toString() + "," + uniqueField.getObjectId().toString() +"," + uniqueField.getFieldNum() +",";
		sql += "'" + uniqueField.getStringValue() +"',"+ uniqueField.getNumValue() +",'"+ uniqueField.getDateValue() +"')";
		
		
		this.update(sql);
		
		
	}

	public void update(com.arthur.mta.utbdbservice.domain.UniqueField uniqueField) {
		
		String sql = "UPDATE  `UniqueFields` SET `DataGuid`='" + uniqueField.getDataGuid() +"' ,`TenantId`="+ uniqueField.getTenantId().toString() +" , " ;
		sql +=  " `ObjectId`=" + uniqueField.getObjectId().toString() +", `FieldNum`=" + uniqueField.getFieldNum() +",";
		sql += " `StringValue`='" + uniqueField.getStringValue() +"',`NumValue`='"+ uniqueField.getNumValue() +"' , " +
				"`DateValue`='"+ uniqueField.getDateValue() +"' " ;
		sql +=  " WHERE `DataGuid`='" + uniqueField.getDataGuid() +"' ";
		
		this.update(sql);
		
	}

	public void delete(String dataGuid) {
		
		String sql = "Delete FROM  `UniqueFields` ";
		sql +=  " WHERE DataGuid='"+ dataGuid+"' ";
		
		this.update(sql);
		
	}

	public com.arthur.mta.utbdbservice.domain.UniqueField findByStringValue(
			Integer tenantId, Integer objectId, String fieldNum,
			String stringValue) {

		 
		 String sql =  "Select TenantId,ObjectId,FieldNum,DataGuid,StringValue,NumValue,DateValue from UniqueFields  where  TenantId ="+tenantId +" AND ObjectId =" + objectId 
				 + " and FieldNum =" + fieldNum + " and StringValue ='"+ stringValue +"'" ;
		 
			List rows =this.queryResult(sql);
			
			List<UniqueField> ufields  = new ArrayList<UniqueField>();
			for (java.lang.Object object : rows) {
				ufields.add(createUniqueField(((Map)object)));
			}
			
			if(ufields.size() > 0){
				return (com.arthur.mta.utbdbservice.domain.UniqueField) ufields.get(0);
			}
		 
		return null;
		
	
	}

	public com.arthur.mta.utbdbservice.domain.UniqueField findByNumValue(
			Integer tenantId, Integer objectId, String fieldNum,
			String numValue) {
		
		 
		 String sql =  "Select TenantId,ObjectId,FieldNum,DataGuid,StringValue,NumValue,DateValue from UniqueFields  where  TenantId ="+tenantId +" AND ObjectId=" + objectId 
				  + " and FieldNum =" + fieldNum + " and NumValue ="+ numValue +"" ;
		 
			List rows =this.queryResult(sql);
			List<UniqueField> ufields  = new ArrayList<UniqueField>();
			for (java.lang.Object object : rows) {
				ufields.add(createUniqueField(((Map)object)));
			}
		 
			if(ufields.size() > 0){
				return (com.arthur.mta.utbdbservice.domain.UniqueField) ufields.get(0);
			}

			return null;

	}

	public com.arthur.mta.utbdbservice.domain.UniqueField findByDateValue(
			Integer tenantId, Integer objectId, String fieldNum,
			String dateValue) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object findMaxSno(
			Integer tenantId, Integer objectId) {
		
		//todo arthur need change			
				 String sql =  "Select TenantId,ObjectId,FieldNum,DataGuid,StringValue,NumValue,DateValue from UniqueFields  where TenantId ="+tenantId + " AND ObjectId=" + objectId   
				 + " order by NumValue desc  " ;
		 
			List rows =this.queryResult(sql);
			List<UniqueField> ufields  = new ArrayList<UniqueField>();
			for (java.lang.Object object : rows) {
				ufields.add(createUniqueField(((Map)object)));
			}
		 
		 if(ufields.size() > 0){
			 return  ufields.get(0);
		 }else{
			 
			 UniqueField uf = new UniqueField(); 
			 uf.setNumValue(0);
			 
			 return uf;
						
		 }
		 
		
	}
	


	private UniqueField createUniqueField(Map map){
		
		UniqueField uf = new UniqueField();
		
		uf.setDataGuid(map.get("DataGuid").toString()); 
		uf.setObjectId((Integer) map.get("ObjectId")); 
		uf.setTenantId((Integer) map.get("TenantId")); 
		uf.setFieldNum((Integer) map.get("FieldNum")); 
		if(map.get("StringValue") != null)uf.setStringValue( map.get("StringValue").toString()); 
		if(map.get("NumValue") != null)uf.setNumValue((Integer) map.get("NumValue")); 
		if(map.get("DateValue") != null)uf.setDateValue( map.get("DateValue").toString()); 

		
	    return uf;
		
		
	}

}
