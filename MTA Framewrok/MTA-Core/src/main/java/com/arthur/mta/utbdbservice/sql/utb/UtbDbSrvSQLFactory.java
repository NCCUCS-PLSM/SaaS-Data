package com.arthur.mta.utbdbservice.sql.utb;

import com.arthur.mta.utbdbservice.sql.AbstractSQLFactory;
import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.SelectSQLStatement;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.WhereClause;

public class UtbDbSrvSQLFactory  extends AbstractSQLFactory{

	@Override
	public WhereClause createWhereClause(String clause) {
		return new UtbDbSrvClause(clause);
	}

	@Override
	public WhereClause createWhereClauseComposite() {
		return new UtbDbSrvWhereClause();
	}

	@Override
	public SelectSQLStatement createSelectSQLStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SelectExpression createSelectExpression() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TableExpression createTableExpression() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TableExpression createTableExpression(String tableExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Join createJoin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderByExpression createOrderByExpression() {
		// TODO Auto-generated method stub
		return null;
	}


}
