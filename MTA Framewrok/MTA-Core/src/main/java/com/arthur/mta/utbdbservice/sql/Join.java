package com.arthur.mta.utbdbservice.sql;



public interface Join {
		
 void addJoinedTable(String tblExpr);
	 TableExpression getJoinedTable() ;
	 void addTable(String tblExpr) ;
	 TableExpression getTable() ;
	 String toSql(SelectExpression<?> selectExpr);

}
