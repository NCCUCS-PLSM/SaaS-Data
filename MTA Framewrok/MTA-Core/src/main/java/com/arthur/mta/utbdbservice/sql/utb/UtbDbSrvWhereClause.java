package com.arthur.mta.utbdbservice.sql.utb;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.utbdbservice.sql.AbstractWhereClause;
import com.arthur.mta.utbdbservice.sql.WhereClause;


 class UtbDbSrvWhereClause extends AbstractWhereClause  {
	
	private List<WhereClause> children = null;
	private List<String> logicOperators = null;
	
	public UtbDbSrvWhereClause(){
		 this.children = new ArrayList<WhereClause>();
		 this.logicOperators = new ArrayList<String>();
	}
	
	public void add(WhereClause whereClause) {
		this.children.add(whereClause);
	}
	
	public void addLogicOperator(String operator) {
		this.logicOperators.add(operator);
	}

	public String toSql() {
		
		StringBuilder result = new StringBuilder();
		int count = 0;

		for (WhereClause w : children) {
			w.setObject(this.object);
			result.append(w.toSql());
			if(count < this.logicOperators.size()){
				result.append(" " + this.logicOperators.get(count) + " ");
			}
			count +=1;
		}
		
	
		return result.toString();
	}
	
	public int getTenantId(){
		
		for (WhereClause w : children) {
			if(w.getTenantId() > 0){
				return w.getTenantId();
			}
		}
		
		return 0;
	}

	public WhereClause getChild(int index) {
		return this.children.get(index);
	}

	public int getLastChildIndex() {
		return this.children.size() -1;
	}

	public boolean isContainUniqueField() {
		// TODO Auto-generated method stub
		return false;
	}

}
