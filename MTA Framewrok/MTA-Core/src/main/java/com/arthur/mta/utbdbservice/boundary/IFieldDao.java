package com.arthur.mta.utbdbservice.boundary;

import java.util.List;

import com.arthur.mta.utbdbservice.domain.Field;

public interface IFieldDao {
	
	public void save(Field field);
	public void update(Field field);
	public void delete(Field field);
	public Field findByFieldId(Integer fieldId);
	public List<Field>  findByObjectId(Integer objectId , Integer tenantId);
	public Field  findByNum(Integer objectId , Integer tenantId,Integer fieldNum);

}
