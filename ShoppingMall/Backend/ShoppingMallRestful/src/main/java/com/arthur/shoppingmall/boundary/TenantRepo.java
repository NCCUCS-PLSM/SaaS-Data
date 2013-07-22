package com.arthur.shoppingmall.boundary;

import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.datastore.JDOConnection;

import com.arthur.shoppingmall.domain.Tenant;
import com.arthur.shoppingmall.util.DataNucleusHelper;



public class TenantRepo extends Repo  {

	public Tenant getTenant(int tenantId){	

		Query q = super.getPersistenceManager().newQuery(
		      		    "SELECT FROM com.arthur.shoppingmall.domain.Tenant where  tenantId =="+ tenantId);
			      
	  List<Tenant> ts = (List<Tenant>)q.execute();
		

		return ts.get(0);

	}
}
