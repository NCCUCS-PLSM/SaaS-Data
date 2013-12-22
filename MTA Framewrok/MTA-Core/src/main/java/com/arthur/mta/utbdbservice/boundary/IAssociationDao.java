package com.arthur.mta.utbdbservice.boundary;

import java.util.List;

import com.arthur.mta.utbdbservice.domain.Association;


public interface IAssociationDao {
	
	public void save(Association association);
	public void update(Association association);
	public void delete(Association association);
	public Association findById(Integer associationId);
	public List findByTargetObjId(Integer objectId , Integer tenantId);
	public Association findByParentChildObjId(Integer parentId , Integer childId , Integer tenantId);
	public List<Association> find(Integer objectId) ;
	public Association findByObjIdFieldId(Integer objectId , Integer fieldId );
	public List<Association> findByTargetObjIdFieldId(Integer objectId , Integer fieldId );
	public List findByObjId(Integer objectId );
}
