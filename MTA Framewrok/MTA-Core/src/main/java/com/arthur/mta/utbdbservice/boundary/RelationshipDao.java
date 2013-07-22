package com.arthur.mta.utbdbservice.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.arthur.mta.utbdbservice.boundary.IRelationshipDao;
import com.arthur.mta.utbdbservice.domain.Relationship;


@Repository("relationshipDao")
public class RelationshipDao extends Dao implements IRelationshipDao {


	
	public void save(Relationship relationship) {
	
		String sql = "INSERT INTO `Relationships` (`AssociationId` ,`DataGuid` ,`TargetDataGuid`,`TenantId`, `ObjectId`, `TargetObjectId` ) " ;
		sql +="	VALUES ( "+ relationship.getAssociationId()  +",'" + relationship.getDataGuid() +"','"+relationship.getTargetDataGuid() +"',";
		sql += relationship.getTenantId().toString() + "," + relationship.getSourceObjectId().toString() +"," + relationship.getTargetObjectId() +" )";
		
		
		this.update(sql);
		
	}

	public void update(Relationship relationship) {
	
		String sql = "UPDATE  `Relationships` SET ";
		sql += " `TargetDataGuid`='" + relationship.getTargetDataGuid() +"' ";
		sql +=  " WHERE `AssociationId`="+ relationship.getAssociationId() + " and `DataGuid`='" + relationship.getDataGuid() +"'";
		
		this.update(sql);
		
		
	}

	public void delete(String dataGuid) {
		
		String sql = "Delete FROM  `Relationships` ";
		sql +=  " WHERE `DataGuid`='"+ dataGuid +"'";
		
		this.update(sql);
		
	}
	
	public void delete(int associationId , String dataGuid) {
		
		String sql = "Delete FROM  `Relationships` ";
		sql +=  " WHERE  `AssociationId`="+ associationId + " and `DataGuid`='"+ dataGuid +"'";
		
		this.update(sql);
		
	}
	
	public void deleteTarget( String dataGuid) {
		String sql = "Delete FROM  `Relationships` ";
		sql +=  " WHERE `TargetDataGuid`='"+ dataGuid +"'";
		
		this.update(sql);
		
	}

	
	public List<Relationship> findRelationTIdOidTObjId( Integer tenantId , Integer objectId , Integer targetObjectId){
		
		String sql =  "Select * from Relationships where TenantId =" + tenantId +" AND ObjectId="+objectId+ " AND TargetObjectId="+targetObjectId ;
		List rows =this.queryResult(sql);
		List<Relationship> rels = new ArrayList<Relationship>();
		
		for (Object object : rows) {
			rels.add(createRelation((Map)object));
		}
		
	     return rels;
	}
	
	public Relationship findRelationByPk(Integer associationId , String dataGuid ){
		
		String sql =  "Select * from Relationships where AssociationId =" + associationId +" AND DataGuid='"+dataGuid+ "'"; 
		List rows =this.queryResult(sql);
		Relationship rel = null;
		
		if(rows.size() > 0){
			for (Object object : rows) {
				rel = createRelation((Map)object);
			}
		}
	
	     return rel;
		
	}
	
	public List<Relationship> findRelationByTargetDataGuid(Integer associationId , String dataGuid ){
		
		String sql =  "Select * from Relationships where AssociationId =" + associationId +" AND TargetDataGuid='"+dataGuid+ "'"; 
		List rows =this.queryResult(sql);
		List<Relationship> rels = null;
		
		if(rows.size() > 0){
			rels = new ArrayList<Relationship>();
			for (Object object : rows) {
				rels.add(createRelation((Map)object));
			}
		}
	
	     return rels;
		
	}
	
	public List<Relationship> findRelationByTargetDataGuid(String dataGuid ){
		
		String sql =  "Select * from Relationships where TargetDataGuid='"+dataGuid+ "'"; 
		List rows =this.queryResult(sql);
		List<Relationship> rels = null;
		
		if(rows.size() > 0){
			rels = new ArrayList<Relationship>();
			for (Object object : rows) {
				rels.add(createRelation((Map)object));
			}
		}
	
	     return rels;
		
	}
	

	
	private Relationship createRelation(Map map){
		
		
		Relationship rel = new Relationship();
		rel.setAssociationId((Integer)map.get("AssociationId"));
		rel.setDataGuid(map.get("DataGuid").toString());
		rel.setTargetDataGuid(map.get("TargetDataGuid").toString());
		rel.setTargetObjectId((Integer)map.get("TenantId"));
		rel.setSourceObjectId((Integer)map.get("ObjectId"));
		rel.setTargetObjectId((Integer)map.get("TargetObjectId"));
		
		return rel;
		
	}


	
	
	
	

}
