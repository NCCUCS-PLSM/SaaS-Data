package com.arthur.mta.utbdbservice.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Repository;

import com.arthur.mta.core.FieldType;
import com.arthur.mta.core.IndexType;
import com.arthur.mta.utbdbservice.boundary.IFieldDao;
import com.arthur.mta.utbdbservice.domain.Field;


@Repository("fieldDao")
public class FieldDao extends Dao implements IFieldDao {


	public void save(Field field) {
		
		String sql = "INSERT INTO `Fields` (`TenantId` ,`ObjectId`, `FieldName` , `FieldType` , `FieldNum` , `IndexType`  ) values (";
		sql +=  field.getTenantId() +" , "+ field.getObjectId().toString() + ",'" ;
		sql += field.getFieldName() +"','" + field.getFieldType().getValue() +"','"+ field.getFieldNum() + "','" ;
		sql += field.getIndexType().getValue() + "')";

		this.update(sql);
		
	}

	public void update(Field field) {
		
		String sql = "UPDATE  `Fields` SET ";
		sql += " `FieldName`='" + field.getFieldName()  +"' ";
		sql += " , `FieldType`='" + field.getFieldType().getValue()  +"' ";
		sql += " , `IndexType`='" + field.getIndexType().getValue()  +"' ";
		sql +=  " WHERE `FieldId`="+ field.getFieldId() + " and `TenantId`=" + field.getTenantId();
		
		this.update(sql);
		
	}

	public void delete(Field field) {
		// TODO Auto-generated method stub
		
	}

	public Field findByFieldId(Integer fieldId) {
		
		String sql =  "Select * from Fields  where FieldId ="+fieldId  ;
		
		List rows =this.queryResult(sql);
		List<Field> fields  = new ArrayList<Field>();
		for (java.lang.Object object : rows) {
			fields.add(createField(((Map)object)));
		}
		if(fields.size() > 0)
			return fields.get(0);
		else
			return null;
	}

	public List<Field>  findByObjectId(Integer objectId, Integer tenantId) {
		
		String sql =  "Select * from Fields  where  TenantId ="+tenantId  + " AND ObjectId = " + objectId + " Order by FieldNum ";
		List rows =this.queryResult(sql);
		
		List<Field> fields  = new ArrayList<Field>();
		for (java.lang.Object object : rows) {
			fields.add(createField(((Map)object)));
		}
		

		 return fields;
		
	}
	
	public Field findByNum(Integer objectId, Integer tenantId, Integer fieldNum) {
		String sql =  "Select * from Fields  where TenantId ="+tenantId +" AND ObjectId = " + objectId   ;
		sql +=" AND FieldNum="+fieldNum;
		List rows =this.queryResult(sql);
		List<Field> fields  = new ArrayList<Field>();
		for (java.lang.Object object : rows) {
			fields.add(createField(((Map)object)));
		}
		if(fields.size() > 0)
			return fields.get(0);
		else
			return null;
	}
	
	private Field createField(Map map){
		
		Field f = new Field();
		f.setFieldId((Integer) map.get("FieldId")); 
		f.setObjectId((Integer) map.get("ObjectId")); 
		f.setTenantId((Integer) map.get("TenantId")); 
		f.setFieldName( map.get("FieldName").toString()); 
		f.setFieldType(FieldType.getType(Integer.parseInt(map.get("FieldType").toString()))); 
		f.setFieldNum( map.get("FieldNum").toString()); 
		f.setIndexType(IndexType.getType(Integer.parseInt(map.get("IndexType").toString()))); 
		
	    return f;
		
	}
	


	



}
