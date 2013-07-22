package com.arthur.mta.utbdbservice.sql;

import java.util.List;



public abstract class AbstractWhereClause implements WhereClause{
	
	protected String field;
	protected String value;
	protected String tableAlias;
	protected String compareOperator;
	protected Boolean barcketed = false;
	protected com.arthur.mta.utbdbservice.domain.Object object;
	protected TableExpression tblExpr;
	protected boolean isJoin = false ;
	
	public void setObject(com.arthur.mta.utbdbservice.domain.Object object) {
		this.object = object;
	}
	
	public void setTableExpr(TableExpression tblExpr){
		this.tblExpr = tblExpr;
	}
	
	public String getField() {
		return field;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getCompareOperator() {
		return compareOperator;
	}
	
	public Boolean getBarcketed() {
		return barcketed;
	}
	
	public void barcketed(Boolean barcketed) {
		this.barcketed = barcketed;
	}
	
	public WhereClause getChild(int index) {
		// TODO Auto-generated method stub
		return null;
	}
	public int getLastChildIndex() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public int getTenantId() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void setIsJoin(boolean isJoin) {
		this.isJoin = isJoin;
	}
	
	

}
