package com.arthur.mta.utbdbservice.sql.datanucleus;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.TableExpression;


 class DataNucleusTableExpr implements TableExpression {
	

	private String tableName;
	private String tableAlias;
	private List<Join> joins;
	
	public DataNucleusTableExpr(){
			
	}
	

	public DataNucleusTableExpr(String tableName , String tableAlias){
		
		this.tableName = tableName;
		this.tableAlias = tableAlias;

	}
	
	public DataNucleusTableExpr(String tblExpr) {
		
		String [] elements =  tblExpr.split("\\.");		
		this.tableName =  elements[elements.length -1] ;
		this.tableAlias =elements[0];
		
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}


	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}

	public String getTableName() {
		return tableName;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public List<Join> getJoins() {
		return joins;
	}


	public void addJoin(Join join) {
		if(joins == null){
			this.joins = new ArrayList<Join>();
		}
		
		this.joins.add(join);
	}



	
	

}
