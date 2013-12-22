package com.arthur.mta.utbdbservice.sql.utb;

import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.UniqueField;
import com.arthur.mta.utbdbservice.sql.AbstractWhereClause;
import com.arthur.mta.utbdbservice.sql.WhereClause;

 class UtbDbSrvClause extends AbstractWhereClause {

	private String clauseString = "";
	
	public UtbDbSrvClause(String clauseString){
		this.clauseString = clauseString;
		this.init();
	}
	
	
	public void add(WhereClause whereClause) {
		throw new UnsupportedOperationException();
	}
	
	public void addLogicOperator(String operator) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	public String toSql() {
		//todo need refactoring

		StringBuilder result = new StringBuilder();
		
		Field field = super.object.findField(this.field);
		
		if(field !=null){
			if(field.getIndexType().getValue() != 0 && this.compareOperator.equals("=")){
				IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
				//todo arthur here
				UniqueField uField = 
						uDao.findByStringValue(object.getTenantId(), object.getObjectId(), field.getFieldNum(), this.value);
				//todo operator
				if(uField != null){
					result.append(" DataGuid ='"+ uField.getDataGuid() +"'");
				}else{
					result.append( field.columnName() + this.compareOperator + ""+ this.value +"");
				}
			}else{
					result.append( field.columnName() + this.compareOperator + ""+ this.value +"");
			}
		}else{
			
			if(this.field.equals("TENANT")){
				result.append( "TenantId" + this.compareOperator + ""+ this.value +"");
			}
		
		}
	
		return result.toString();
	}
	
	public int getTenantId(){
		
		//todo arthur TENANT , datanucleus use string
		if(this.field.equals("TENANT")){
			return Integer.parseInt(this.value.replace("'", ""));
		}
		
		return 0;
	}
	
	private void init(){
		
		String [] ary = this.clauseString.split(" ");
		this.field = ary[0];
		this.compareOperator = ary[1].trim();
		this.value = ary[2].trim();

		
	}


	public boolean isContainUniqueField() {
		// TODO Auto-generated method stub
		return false;
	}

}
