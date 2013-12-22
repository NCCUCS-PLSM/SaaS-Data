package com.arthur.mta.utbdbservice.sqlbuilder;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.Term;
import com.arthur.mta.utbdbservice.sql.WhereClause;


 class JoinBuilder extends  SqlStmtBuilder{
	
		private com.arthur.mta.utbdbservice.domain.Object object;
		private SelectExpression<?> selectExpr;
		private WhereClause whereClause;
		private TableExpression tblExpr;

		public JoinBuilder(com.arthur.mta.utbdbservice.domain.Object object , SelectExpression<?> selectExpr
				,TableExpression tblExpr  , WhereClause whereClause){
			
			this.object = object;
			this.selectExpr = selectExpr;
			this.whereClause = whereClause;
			this.tblExpr = tblExpr;
			
		}
		
		@Override
		public void generate() {
			
			appendLine("Select "+ doQueryFields(selectExpr) +" from (");
			//todo now ensure one is correct
			for (Join join : this.tblExpr.getJoins()) {
				appendLine(join.toSql(this.selectExpr));	
			}
			
			appendLine(")");
			
			this.generateWhere();
		
		}
		
		private  String doQueryFields(SelectExpression<?> selectExpr){
			
			String qf ="";
			for (Join join : this.tblExpr.getJoins()) {
				for (Term t : selectExpr.getTerms()) {
					if(t.getTableAlias()!=null){
						if(t.getTableAlias().equals(join.getJoinedTable().getTableAlias())){
							qf += join.getJoinedTable().getTableName() +"."+ t.getColumnName() + ",";
						}else if(t.getTableAlias().equals(join.getTable().getTableAlias())){
							qf += join.getTable().getTableName() +"."+ t.getColumnName() + ",";
						}
					}else{
						qf += t.getColumnName() + ",";
					}
				}
			}
			
			return qf.substring(0, qf.length() -1);
		}
				

		private void generateWhere() {
			
			appendLine(" where ");

			StringBuilder result = new StringBuilder();

			//result.append(" 1=1 ");
			
			if(whereClause != null){
				whereClause.setIsJoin(true);
				whereClause.setObject(object);
				whereClause.setTableExpr(this.tblExpr);
				result.append(whereClause.toSql());
			}
				
			
			appendLine( result.toString() );
			
		}

	
	 
	   

	 
	}
