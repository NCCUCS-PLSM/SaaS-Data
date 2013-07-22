package com.arthur.mta.utbdbservice.sqlbuilder;


import java.util.List;

import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.WhereClause;

 public abstract class SqlStmtBuilder {
	
	
	   private StringBuilder stmtBuilder = new StringBuilder();

	   public String getStmt() {
	      return stmtBuilder.toString();
	   }

	   abstract public void generate();

	   protected void appendLine(String stmt ) {
		   stmtBuilder.append(stmt);
	     
	   }
	   
	   
	   //todo arthur need refactoring
	   public static SqlStmtBuilder getJoinBuilder(com.arthur.mta.utbdbservice.domain.Object object ,  SelectExpression<?> selectExpr 
				,TableExpression tblExpr , WhereClause whereClause){
			
			return new JoinBuilder(object, selectExpr, tblExpr, whereClause);
		}
	   
	   public static SqlStmtBuilder getSelectBuilder(com.arthur.mta.utbdbservice.domain.Object object , 
			   SelectExpression<?> selectExpr , WhereClause whereClause ,  OrderByExpression orderExpr ,long offset, long count ){
			
			return new SelectBuilder(object, selectExpr, whereClause,orderExpr ,offset,count);
		}
	   
 
 }
