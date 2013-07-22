package com.arthur.mta.utbdbservice.sql;

import java.util.List;

import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.sqlbuilder.SqlStmtBuilder;

public abstract class AbstractSelectSQLStmt implements SelectSQLStatement {
	
	protected SelectExpression<?> select;
	protected WhereClause where;
	protected TableExpression tbExpr;
	protected OrderByExpression orderByExpr;
	protected long rangeOffset;
	protected long rangeCount;
	
	protected String procGenerate(SqlStmtBuilder builder) {
		  return createStmt(builder);
	}

	private String createStmt(SqlStmtBuilder builder) {
			   
		 builder.generate();
		 return builder.getStmt();
	}
		   
	protected com.arthur.mta.utbdbservice.domain.Object getSourceObject(String objectName){
			   
	   IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
	   com.arthur.mta.utbdbservice.domain.Object obj = dao.findByObjectName(objectName);
			   
	   return obj;
			   
	}
	

}
