package com.arthur.mta.utbdbservice.sql.datanucleus;

import com.arthur.mta.utbdbservice.sql.Term;

 class DataNucleusTerm implements Term{

	private String columnName;
	private String tableAlias;
	
	public DataNucleusTerm(String columnName){
		this.columnName = columnName;
	}
	
	public DataNucleusTerm(String tableAlias ,String columnName){
		this.columnName = columnName;
		this.tableAlias = tableAlias;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}

	public String getColumnName() {
		return this.columnName;
	}

	public String getTableAlias() {
		return this.tableAlias;
	}

}
