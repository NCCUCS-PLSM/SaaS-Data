package com.arthur.mta.utbdbservice.sql;

/* Select Grammar
SELECT [ TOP term ] [ DISTINCT | ALL ] selectExpression [,...]
FROM tableExpression [,...] [ WHERE expression ]
[ GROUP BY expression [,...] ] [ HAVING expression ]
[ { UNION [ ALL ] | MINUS | EXCEPT | INTERSECT } select ] [ ORDER BY order [,...] ]
[ LIMIT expression [ OFFSET expression ] [ SAMPLE_SIZE rowCountInt ] ]
[ FOR UPDATE ]
*/

public interface SelectSQLStatement {
	
	void addSelect(SelectExpression<?> selectExpr);
	
	void addFrom(TableExpression tableExpr);

	void addWhere(WhereClause where);
	
	String toSql();
	
	void setRange(long offset, long count);
	
	void addOrderBy(OrderByExpression orderByExpr);

}
