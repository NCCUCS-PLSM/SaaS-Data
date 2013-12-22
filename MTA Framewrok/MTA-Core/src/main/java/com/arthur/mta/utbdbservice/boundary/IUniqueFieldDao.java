package com.arthur.mta.utbdbservice.boundary;


import com.arthur.mta.utbdbservice.domain.UniqueField;

public interface IUniqueFieldDao {
	
	public void save(UniqueField uniqueField);
	public void update(UniqueField uniqueField);
	public void delete(String dataGuid);

	public UniqueField findByStringValue(Integer tenantId , Integer objectId , String fieldNum , String stringValue );
	public UniqueField findByNumValue(Integer tenantId , Integer objectId , String fieldNum , String numValue );
	public UniqueField findByDateValue(Integer tenantId , Integer objectId , String fieldNum , String dateValue );
	public Object findMaxSno(Integer tenantId , Integer objectId );

}
