package com.arthur.shoppingmall.boundary;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.arthur.shoppingmall.util.DataNucleusHelper;

public abstract class Repo {
	
	private  PersistenceManagerFactory pmf = 
			JDOHelper.getPersistenceManagerFactory(DataNucleusHelper.createProperties());
       
    public  PersistenceManager getPersistenceManager() {
	        return pmf.getPersistenceManager();
    }
    
}
