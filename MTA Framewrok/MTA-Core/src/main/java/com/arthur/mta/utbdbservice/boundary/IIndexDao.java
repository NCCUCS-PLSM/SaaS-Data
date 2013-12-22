package com.arthur.mta.utbdbservice.boundary;

import java.util.List;

import com.arthur.mta.utbdbservice.domain.Index;


public interface IIndexDao {
	
	public void save(Index index);
	public void update(Index index);
	public void delete(String dataGuid);

	public List<Index> findByStringValue( Integer objectId , String fieldNum , String stringValue , String compareOperator  );
	public List<Index> findByNumValue( Integer objectId , String fieldNum , String numValue ,String compareOperator);
	public List<Index> findByDateValue( Integer objectId , String fieldNum , String dateValue ,String compareOperator);
	public com.arthur.mta.utbdbservice.domain.Index  findByFieldlNum(Integer objectId, String fieldNum , String dataGuid);

}
