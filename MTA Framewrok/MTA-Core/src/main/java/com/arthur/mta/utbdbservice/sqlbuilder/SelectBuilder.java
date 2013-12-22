package com.arthur.mta.utbdbservice.sqlbuilder;

import java.util.ArrayList;
import java.util.List;


import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.Term;
import com.arthur.mta.utbdbservice.sql.WhereClause;


 class SelectBuilder extends  SqlStmtBuilder{

	private com.arthur.mta.utbdbservice.domain.Object object;
	private SelectExpression<?> selectExpr;
	private WhereClause whereClause;
	private OrderByExpression orderByExpr;
	private long offset;
	private long count;

	
	public SelectBuilder(com.arthur.mta.utbdbservice.domain.Object object ,  SelectExpression<?> selectExpr ,
			WhereClause whereClause, OrderByExpression orderExpr ,long offset, long count){
		this.object = object;
		this.selectExpr = selectExpr;
		this.whereClause = whereClause;
		this.offset = offset;
		this.count = count;
		this.orderByExpr = orderExpr;
	}
	
	@Override
	public void generate() {
		appendLine("Select * from (");
	
		if(selectExpr.getTerms().size() > 0){
			List<String> colNames = new ArrayList<String>();
			for (Term t : selectExpr.getTerms()) {
				colNames.add(t.getColumnName());
			}
			String strs[] = new String[1];
			String [] queryFields = colNames.toArray(strs);
			
			appendLine("( Select " +  object.columnList(queryFields) + " from Data "); 
		}else{
			appendLine("( Select  " +  object.columnList() + " from Data "); 
		}
	     		
		this.generateWhere();
		
		appendLine(")");
		
		if(orderByExpr!=null){
			appendLine(generateOrderBy(orderByExpr, object))			;
			
		}
			
		if(this.offset != -1){
			appendLine("limit "+ this.offset + "," + this.count);
		}
		
	}
	
	private String generateOrderBy(OrderByExpression orderByExpr, com.arthur.mta.utbdbservice.domain.Object object){
		
		String orStr = "";
		if(orderByExpr.getTerm().getColumnName().equals("pkfield")){
			orStr = " ORDER BY "+ object.findPkField().getFieldName();
		}else{
			orStr = " ORDER BY "+ orderByExpr.getTerm().getColumnName();
		}
		
		if(orderByExpr.isDesc()){
			orStr += " DESC ";
		}else{
			orStr += " ASC ";
		}
		
		return orStr;
	}


	private void generateWhere() {
		
		appendLine(" where ");

		StringBuilder result = new StringBuilder();

		if(whereClause != null){
			
			whereClause.setObject(object);
			
			if(!whereClause.isContainUniqueField())
				result.append(" TenantId ="+  TenantContextKeeper.getContext().getTenantUser().getTenantId() +
						" AND  ObjectId =" + object.getObjectId() + " AND " );
				
			result.append(whereClause.toSql());
		}else{
			result.append(" TenantId ="+  TenantContextKeeper.getContext().getTenantUser().getTenantId() +
					" AND  ObjectId =" + object.getObjectId()  );
		}
		
		//result.append(" AND `IsDeleted`=0) as `" + object.getObjectName() +"`" );
		
		result.append(" ) as `" + object.getObjectName() +"`" );
		
		appendLine( result.toString());
		
	}
	


	
	

	
	

}
