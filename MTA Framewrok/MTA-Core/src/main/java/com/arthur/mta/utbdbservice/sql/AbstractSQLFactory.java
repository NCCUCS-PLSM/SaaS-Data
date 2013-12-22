package com.arthur.mta.utbdbservice.sql;

import com.arthur.mta.utbdbservice.sql.datanucleus.DataNucleusSQLFactory;
import com.arthur.mta.utbdbservice.sql.utb.UtbDbSrvSQLFactory;


public abstract class AbstractSQLFactory {
	
	
	private static final DataNucleusSQLFactory dataNucleusFactory = new DataNucleusSQLFactory();
	private static final UtbDbSrvSQLFactory utbFactory = new UtbDbSrvSQLFactory();

	public static final AbstractSQLFactory getFactory(SQLFactoryTypes sqlfactory)
	  {
	    switch (sqlfactory)
	    {
	      case DataNucleus:
	        return dataNucleusFactory;

	      case UtbDbSrv:
	        return utbFactory;
	       
	    }
	    
		return null; 
	    
	 } 
	
	
	public abstract SelectSQLStatement createSelectSQLStatement();
	
	public abstract SelectExpression<?> createSelectExpression();
	
	public abstract TableExpression createTableExpression();
	
	public abstract TableExpression createTableExpression(String tableExpr);
	
	public abstract WhereClause createWhereClause(String clause );
	
	public abstract WhereClause createWhereClauseComposite();
	
	public abstract Join createJoin();
	
	public abstract OrderByExpression createOrderByExpression();
}
