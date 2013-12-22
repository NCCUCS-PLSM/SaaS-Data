package com.arthur.mta.utbdbservice.boundary;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.boundary.IFieldDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.domain.Object;

@Repository("objectDao")
public class ObjectDao extends Dao implements IObjectDao {

	public void save(Object object) {
		
		String sql = "INSERT INTO `Objects` (`TenantId`, `ObjectName` , `IsDefault` ,`IsCustomizable`) values (";
		sql +=  object.getTenantId() +",'" + object.getObjectName() +"' , '"+ object.getIsDefault()+"' ,'1')";

		
		this.update(sql);
		
	}

	public void update(Object object) {
		
		String sql = "UPDATE  `Objects` SET ";
		sql += " `ObjectName`='" + object.getObjectName()  +"'  ";
		sql +=  " WHERE `ObjectId`="+ object.getObjectId().toString() + " and `TenantId`=" + object.getTenantId();
		
		this.update(sql);
		
	}

	public void delete(Object object) {
		
		String sql = "UPDATE  `Objects` SET  `IsDeleted`='1' ";
		sql +=  " WHERE `ObjectId`="+ object.getObjectId().toString();
		
		this.update(sql);
		
	}

	public com.arthur.mta.utbdbservice.domain.Object findByObjectId(Integer objectId) {
		
		int tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
		String sql =  "Select * from Objects where ObjectId =" + objectId +" and TenantId =" + tenantId ;
		List rows = this.queryResult(sql);
		com.arthur.mta.utbdbservice.domain.Object obj =null;
		
		 if(rows !=null){
			for (java.lang.Object object : rows) {
				obj = createObject(((Map)object));			
			}
			
			if(obj != null){
				 IFieldDao dao = (IFieldDao) CTX.get().getBean("fieldDao");
				 obj.setFields( dao.findByObjectId(objectId, tenantId)) ;	
			}
		 }
		 
		 return obj;
	}
	
	public List<com.arthur.mta.utbdbservice.domain.Object> findCustomObjects() {
		
		int tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
		String sql =  "Select * from Objects where  TenantId =" + tenantId  +" AND IsCustomizable='1' ";
		List rows = this.queryResult(sql);
		List<com.arthur.mta.utbdbservice.domain.Object> objs = new ArrayList<com.arthur.mta.utbdbservice.domain.Object>();
		
		 if(rows !=null){
			IFieldDao dao = (IFieldDao) CTX.get().getBean("fieldDao");
			for (java.lang.Object object : rows) {
				com.arthur.mta.utbdbservice.domain.Object obj = createObject(((Map)object));			
				obj.setFields( dao.findByObjectId(obj.getObjectId(), tenantId)) ;	
				objs.add(obj);
			}
		 }
		 
		 return objs;
	}

	public com.arthur.mta.utbdbservice.domain.Object findByObjectName(String objectName) {
		
		int tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
		String sql =  "Select * from Objects where ObjectName ='" + objectName +"' and TenantId =" + tenantId ;
		List  rows=this.queryResult(sql);
		com.arthur.mta.utbdbservice.domain.Object obj =null;
	
		 if(rows !=null){
			for (java.lang.Object object : rows) {
				obj = createObject(((Map)object));			
			}
			if(obj != null){
			 IFieldDao dao = (IFieldDao) CTX.get().getBean("fieldDao");
			 obj.setFields( dao.findByObjectId(obj.getObjectId(), tenantId)) ;
			}
		 }
	
		 return obj;
		 
	
	}
	

	private com.arthur.mta.utbdbservice.domain.Object createObject(Map map){
		
		com.arthur.mta.utbdbservice.domain.Object obj = 
				new com.arthur.mta.utbdbservice.domain.Object();
		obj.setObjectId((Integer) map.get("ObjectId")); 
	    obj.setTenantId((Integer) map.get("TenantId"));
	    obj.setObjectName( map.get("ObjectName").toString());
	    obj.setIsDefault( map.get("IsDefault").toString().charAt(0));
	    
	    return obj;
		
	}

	public List<Object> findByObjectIds(int[] objectIds) {
		
		String objIds = "";
		for (int i = 0; i < objectIds.length; i++) {
			objIds += objectIds[i] + ",";
		}
		objIds = objIds.substring(0,objIds.length() -1);
		
		int tenantId =  TenantContextKeeper.getContext().getTenantUser().getTenantId();
		String sql =  "Select * from Objects where  TenantId =" + tenantId + " and ObjectId in (" +objIds+")";
		List  rows=this.queryResult(sql);
		List<com.arthur.mta.utbdbservice.domain.Object> objs = 
				new ArrayList<com.arthur.mta.utbdbservice.domain.Object>();
	
		 if(rows !=null){
			for (java.lang.Object object : rows) {
				objs.add(createObject(((Map)object)));			
			}
		 }
	
		 return objs;
	}


}
