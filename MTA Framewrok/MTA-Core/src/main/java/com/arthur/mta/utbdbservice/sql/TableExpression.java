package com.arthur.mta.utbdbservice.sql;

import java.util.List;

public interface TableExpression {
	
	public void setTableName(String tableName) ;

	public void setTableAlias(String tableAlias) ;

	public String getTableName() ;

	public String getTableAlias() ;

	public List<Join> getJoins();
	
	void addJoin(Join join);
	
	
}
