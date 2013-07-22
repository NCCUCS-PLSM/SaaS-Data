package com.arthur.mta.utbdbservice.sql.datanucleus;

import com.arthur.mta.utbdbservice.sql.AbstractSQLFactory;
import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.SelectSQLStatement;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.WhereClause;

public class DataNucleusSQLFactory  extends AbstractSQLFactory{

	@Override
	public SelectSQLStatement createSelectSQLStatement() {
		return new DataNucleusSelectSQLStmt();
	}

	@Override
	public SelectExpression<?> createSelectExpression() {
		return new DataNucleusSelectExpr();
	}

	@Override
	public TableExpression createTableExpression() {
		return new DataNucleusTableExpr();
	}

	@Override
	public WhereClause createWhereClause(String clause ) {
		return new DataNucleusClause(clause);
	}

	@Override
	public WhereClause createWhereClauseComposite() {
		return new DataNucleusWhereClause();
	}

	@Override
	public TableExpression createTableExpression(String tableExpr) {
		return new DataNucleusTableExpr(tableExpr);
	}
	
	@Override
	public Join createJoin() {
		return new DataNucleusJoin();
	}

	@Override
	public OrderByExpression createOrderByExpression() {
		return new DataNucleusOrderByExpr();
	}

}
