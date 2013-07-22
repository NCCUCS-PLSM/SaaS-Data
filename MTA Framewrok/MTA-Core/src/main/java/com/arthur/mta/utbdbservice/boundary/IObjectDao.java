package com.arthur.mta.utbdbservice.boundary;

import java.util.List;

import com.arthur.mta.utbdbservice.domain.Object;

public interface IObjectDao {
		
		public void save(Object object);
		public void update(Object object);
		public void delete(Object object);
		public com.arthur.mta.utbdbservice.domain.Object findByObjectId(Integer objectId );
		public com.arthur.mta.utbdbservice.domain.Object findByObjectName(String objectName );
		public List<com.arthur.mta.utbdbservice.domain.Object> findCustomObjects();
		public List<com.arthur.mta.utbdbservice.domain.Object> findByObjectIds(int [] objectIds );
		
}
