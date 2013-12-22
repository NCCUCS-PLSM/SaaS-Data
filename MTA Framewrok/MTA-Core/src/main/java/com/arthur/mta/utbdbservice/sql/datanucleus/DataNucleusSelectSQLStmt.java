package com.arthur.mta.utbdbservice.sql.datanucleus;

import java.util.ArrayList;

import com.arthur.mta.utbdbservice.sql.AbstractSelectSQLStmt;
import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.WhereClause;
import com.arthur.mta.utbdbservice.sqlbuilder.SqlStmtBuilder;

 class DataNucleusSelectSQLStmt extends AbstractSelectSQLStmt{
	 
	 public DataNucleusSelectSQLStmt(){
		
	 }
	 
	 public void addSelect(SelectExpression<?> selectExpr) {
		 super.select = selectExpr;
	}

	public void addWhere(WhereClause where) {
		super.where = where;
	}
	

	public void addFrom(TableExpression tableExpr) {
		super.tbExpr = tableExpr;
	}


	public String toSql(){
		   
		//todo need refactoring
		DataNucleusTableExpr tbExpr = (DataNucleusTableExpr) super.tbExpr;
		com.arthur.mta.utbdbservice.domain.Object obj = super.getSourceObject(tbExpr.getTableName());
			
		StringBuilder sqlStmt = new StringBuilder();
		
				
		SqlStmtBuilder stmtBuilder = null;
		if(super.tbExpr.getJoins()!=null){
			stmtBuilder = SqlStmtBuilder.getJoinBuilder(obj, super.select , tbExpr ,super.where);
		}else{
			stmtBuilder = SqlStmtBuilder.getSelectBuilder(obj, super.select ,super.where,super.orderByExpr ,
					super.rangeOffset,super.rangeCount);
		}
				
				
		sqlStmt.append(super.procGenerate(stmtBuilder));
	
				
		//System.out.println(sqlStmt.toString());
		
		return sqlStmt.toString();
				
	}
	
	  public void setRange(long offset, long count)
	  {
	        this.rangeOffset = offset;
	        this.rangeCount = count;
	  }

	public void addOrderBy(OrderByExpression orderByExpr) {
		super.orderByExpr = orderByExpr;
		
	}



}
