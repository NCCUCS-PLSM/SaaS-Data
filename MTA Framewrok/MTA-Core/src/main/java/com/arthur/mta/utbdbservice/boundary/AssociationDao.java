package com.arthur.mta.utbdbservice.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Repository;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.domain.Association;



@Repository("associationDao")
public class AssociationDao  extends Dao  implements IAssociationDao{
	
	
	public void save(Association association) {
		
		String sql = "INSERT INTO `Associations` (`TenantId` ,`ObjectId`, `TargetObjectId` , `FieldId` , `TargetFieldId` , `FieldNum` ,`TargetFieldNum` ) values (";
		sql +=  association.getTenantId() +" , "+ association.getObjectId().toString() + "," ;
		sql += association.getTargetObjectId() +"," + association.getFieldId() +","+ association.getTargetFieldId() + "," ;
		sql += association.getFieldNum() + ","+ association.getTargetFieldNum()+")";

		super.update(sql);
		
		
		
	}

	public void update(Association association) {
		// TODO Auto-generated method stub
		
	}

	public void delete(Association association) {
		// TODO Auto-generated method stub
		
	}

	public Association findById(Integer associationId) {
		        
		String sql =  "Select * from Associations where AssociationId =" + associationId ;
		List rows = super.queryResult(sql);
		
		Association association = null;
		for (java.lang.Object object : rows) {
			association = createAssociation(((Map)object));			
		}
		
		return association;
		
	}

	public List findByTargetObjId(Integer objectId , Integer tenantId) {
		
		String sql =  "Select * from Associations where TenantId ="+tenantId + " And  TargetObjectId = " + objectId ;
		List rows = super.queryResult(sql);
		
		List<Association> associations = new ArrayList<Association>();
		for (java.lang.Object object : rows) {
			associations.add(createAssociation(((Map)object)));			
		}
		
		return associations;
	}
	
	public Association findByParentChildObjId(Integer parentId , Integer childId , Integer tenantId) {
		
		String sql =  "Select * from Associations where TenantId ="+tenantId +" AND ObjectId = " + parentId + " and TargetObjectId = " + childId ;
		List rows = super.queryResult(sql);
		
		List<Association> associations = new ArrayList<Association>();
		for (java.lang.Object object : rows) {
			associations.add(createAssociation(((Map)object)));			
		}
		
		if(associations.size() > 0 ){
			return associations.get(0);
		}
		
		return null;
	}
	
	public Association findByObjIdFieldId(Integer objectId , Integer fieldId ) {
		
		String sql =  "Select * from Associations where TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId() +
				" AND ObjectId = " + objectId + " and FieldId = " + fieldId ;
		List rows = super.queryResult(sql);
		
		List<Association> associations = new ArrayList<Association>();
		for (java.lang.Object object : rows) {
			associations.add(createAssociation(((Map)object)));			
		}
		
		if(associations.size() > 0 ){
			return associations.get(0);
		}
		
		return null;
	}
	
	public List<Association> findByTargetObjIdFieldId(Integer objectId , Integer fieldId ){
		
		String sql =  "Select * from Associations where TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId() +
				" AND TargetObjectId = " + objectId + " and TargetFieldId = " + fieldId ;
		List rows = super.queryResult(sql);
		
		List<Association> associations = new ArrayList<Association>();
		for (java.lang.Object object : rows) {
			associations.add(createAssociation(((Map)object)));			
		}
		

		return associations;
		
	}
	
	public List<Association> find(Integer objectId) {
		
		String sql =  "Select * from Associations where TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId() +
				" AND  ObjectId = " + objectId + " OR TargetObjectId = " + objectId  ;
		List rows = super.queryResult(sql);
		
		List<Association> associations = new ArrayList<Association>();
		for (java.lang.Object object : rows) {
			associations.add(createAssociation(((Map)object)));			
		}
		

		
		return associations;
	}
	
	public List findByObjId(Integer objectId) {
		String sql =  "Select * from Associations where TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId()
				+ " And  ObjectId = " + objectId ;
		List rows = super.queryResult(sql);
		
		List<Association> associations = new ArrayList<Association>();
		for (java.lang.Object object : rows) {
			associations.add(createAssociation(((Map)object)));			
		}
		
		return associations;
	}
	
	private Association createAssociation(Map map){
		
		Association association = new Association();
		association.setAssocationId((Integer) map.get("assocationId")); 
		association.setTenantId((Integer) map.get("TenantId"));
		association.setObjectId((Integer) map.get("objectId"));
		association.setTargetObjectId((Integer) map.get("targetObjectId")); 
		association.setFieldId((Integer) map.get("fieldId"));
		association.setTargetFieldId( (Integer)map.get("targetFieldId"));
		association.setFieldNum((Integer) map.get("fieldNum")); 
		association.setTargetFieldNum((Integer) map.get("targetFieldNum"));
		
		
		return association;
		
	}
	
	



}
