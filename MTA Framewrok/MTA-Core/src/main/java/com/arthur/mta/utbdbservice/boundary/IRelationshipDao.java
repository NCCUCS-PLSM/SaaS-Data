package com.arthur.mta.utbdbservice.boundary;


import java.util.List;

import com.arthur.mta.utbdbservice.domain.Relationship;

public interface IRelationshipDao {

	public void save(Relationship relationship);
	public void update(Relationship relationship);
	public void delete(int associationId , String dataGuid);
	public List<Relationship> findRelationTIdOidTObjId(Integer tenantId , Integer objectId , Integer targetObjectId);
	public Relationship findRelationByPk(Integer associationId , String dataGuid );
	public List<Relationship> findRelationByTargetDataGuid(Integer associationId , String dataGuid );
	public List<Relationship> findRelationByTargetDataGuid(String dataGuid );
	public void deleteTarget(String targetDataGuid);
	public void delete(String dataGuid);
	
}
