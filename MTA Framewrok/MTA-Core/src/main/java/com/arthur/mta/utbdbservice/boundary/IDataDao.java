package com.arthur.mta.utbdbservice.boundary;

import java.util.List;

import com.arthur.mta.utbdbservice.domain.Data;
import com.arthur.mta.utbdbservice.domain.Field;

public interface IDataDao {

	public void save(Data data);
	public void update(Data data);
	public void delete(String where);
	public Data findByDataGuid(String dataGuid);
	public List<Data> findByObjectId(int objectId);
	public Data findByPkField(Field pkField , String pkVal) ;
	public List<Data> findByIndexesDataGuid(int objectId , String indexesClauses);
	
}
