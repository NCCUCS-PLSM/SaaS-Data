package com.arthur.mta.utbdbservice.sql.datanucleus;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.util.DateChecker;
import com.arthur.mta.utbdbservice.boundary.IIndexDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.Index;
import com.arthur.mta.utbdbservice.domain.UniqueField;
import com.arthur.mta.utbdbservice.sql.AbstractWhereClause;
import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.WhereClause;


 class DataNucleusClause extends AbstractWhereClause {

	private String clauseString = "";
	
	public DataNucleusClause(String clauseString){
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
		if(this.barcketed)result.append("(");
		if(field !=null){
			if(field.getIndexType().getValue() ==1 && this.compareOperator.equals("=")){
				IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
				
				UniqueField uField = null;
				if(field.getFieldType().getValue() == 0)
					uField = uDao.findByStringValue(object.getTenantId(), object.getObjectId(), field.getFieldNum(), this.value.substring(1 , this.value.length()-1));
				
				if(field.getFieldType().getValue() == 1)
					uField = uDao.findByNumValue(object.getTenantId(), object.getObjectId(), field.getFieldNum(), this.value);
				
				if(field.getFieldType().getValue() == 2)
					uField = uDao.findByDateValue(object.getTenantId(), object.getObjectId(), field.getFieldNum(), this.value);
				
				
				//todo operator
				if(uField != null){
					result.append(" DataGuid ='"+ uField.getDataGuid() +"'");
				}else{
					result.append( doField(field , true));
				}
			}else{
				if(field.getIndexType().getValue() == 2){
					result.append(doIndexField(field));
				}else{
					result.append( doField(field , false));
				}			
			}
		}else{
			
			//todo need refactoring
			boolean myabeJoinField = true;
			if(this.field.equals("TENANT")){
				result.append( "TenantId" + this.compareOperator + ""+ this.value +"");
				myabeJoinField = false;
			}
			
			//FKList
			if(this.field.contains("_OWN")){
				String [] ary = this.field.split("_");
				Field ownerfield = super.object.findField(ary[1]);
				result.append( ownerfield.columnName() + this.compareOperator + "'"+ this.value +"'");
				myabeJoinField = false;
			}
			
			if(this.field.contains("_IDX")){	
				result.append("1 = 1");
				myabeJoinField = false;
			}
			
			if(myabeJoinField && this.tblExpr.getJoins() != null){
				
				for (Join j : this.tblExpr.getJoins()) {
					
					TableExpression tb = null;
					if(j.getJoinedTable().getTableAlias().equals(this.tableAlias))tb = j.getJoinedTable();
					if(j.getTable().getTableAlias().equals(this.tableAlias))tb = j.getTable();
					
					if(tb != null){
						IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
						   com.arthur.mta.utbdbservice.domain.Object obj = dao.findByObjectName(tb.getTableName());
						   Field f = obj.findField(this.field);
						result.append("`"+ tb.getTableName() +"`."+ f.getFieldName() + this.compareOperator + ""+ this.value +"");
						break;
					}

				}
			}
			
			
		}
		if(this.barcketed)result.append(")");
		return result.toString();
	}
	
	public int getTenantId(){
		
		//todo arthur TENANT , datanucleus use string
		/*
		if(this.field.equals("TENANT")){
			return Integer.parseInt(this.value.replace("'", ""));
		}*/
		
		return com.arthur.mta.core.context.TenantContextKeeper.getContext().getTenantUser().getTenantId();
		

	}
	
	private void init(){
		
		String [] ary = this.clauseString.split(" ");
		String [] expr = ary[0].trim().split("\\.");
		
		this.tableAlias = expr[0];
		this.field = expr[1];
		
		this.compareOperator = ary[1].trim();
		this.value = ary[2].trim();
		if(this.value.equals("Mon")||this.value.equals("Tue")||this.value.equals("Wed")||
				this.value.equals("Thu")||this.value.equals("Fri")||this.value.equals("Sat")||
				this.value.equals("Sun")){
			this.value ="";
			for (int i = 2; i < ary.length; i++) {
				this.value += ary[i].trim() + " ";
			}
			
		}

		
	}
	
	private String doField(Field field , boolean isUF){
		if(super.isJoin){
			if(isUF && this.value.equals("null"))
				return field.getFieldName() + this.compareOperator +"?" ;
			else
				return field.getFieldName() + this.compareOperator + this.value ;
		}else{
			return field.columnName() + this.compareOperator + this.value ;
		}
		
	}
	
	private String doIndexField(Field field ){
		
		IIndexDao iDao = (IIndexDao) CTX.get().getBean("indexDao");
		List<Index> iFields = null;
		if(field.getFieldType().getValue() == 0 || field.getFieldType().getValue() == 3 || field.getFieldType().getValue() ==4)
			iFields = iDao.findByStringValue( object.getObjectId(), field.getFieldNum(), this.value.substring(1 , this.value.length()-1),this.compareOperator);
		
		if(field.getFieldType().getValue() == 1)
			iFields = iDao.findByNumValue( object.getObjectId(), field.getFieldNum(), this.value,this.compareOperator);
		
		if(field.getFieldType().getValue() == 2){
			iFields = iDao.findByDateValue(object.getObjectId(), field.getFieldNum(), this.value,this.compareOperator);
			Date date = DateChecker.isValidDate(this.value,"EEE MMM dd HH:mm:ss zzz yyyy");
			if(date!=null){
				 DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				 this.value ="'"+ sdf.format(date)+"'" ;
			}
		}
		
		if(iFields != null ){
			if(iFields.size() > 0){
				StringBuilder result = new StringBuilder();
				result.append(" DataGuid in (");
				for (Index index : iFields) {
					result.append("'"+index.getDataGuid() + "',");
				}
				
				String retVal = result.toString();
				return retVal.substring(0,retVal.length()-1)+") ";
			}else{
				return this.doField(field, false);
			}
		}else{
			return this.doField(field, false);
		}
		
	}


	public boolean isContainUniqueField() {
		
		Field field = super.object.findField(this.field);
		
		if(field != null){
			if(field.getIndexType() != null){
				if(field.getIndexType().getValue() == 1)
					return true;
			}
		}
		
		return false;
	}
	
	
}
