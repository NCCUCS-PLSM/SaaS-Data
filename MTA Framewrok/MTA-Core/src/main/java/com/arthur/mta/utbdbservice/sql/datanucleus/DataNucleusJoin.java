package com.arthur.mta.utbdbservice.sql.datanucleus;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IRelationshipDao;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.Relationship;

import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.TableExpression;
import com.arthur.mta.utbdbservice.sql.Term;

 class DataNucleusJoin implements Join{

	 private TableExpression tblExpr;
	 private TableExpression joinedTblExpr;

	 
	public void addJoinedTable(String tblExpr) {
		
		String [] elements =  tblExpr.split(" ");
		
		String tableName = elements[0];
		if(elements[0].contains("`")){
			tableName = elements[0].split("`")[1];
		}
		this.joinedTblExpr = new DataNucleusTableExpr(tableName, elements[1]);
		
	}

	public TableExpression getJoinedTable() {

		return this.joinedTblExpr;
	}

	public void addTable(String tblExpr) {
		
		String [] elements =  tblExpr.split(" ");
		this.tblExpr = new DataNucleusTableExpr(elements[0], elements[1]);
		
	}

	public TableExpression getTable() {
		
		return this.tblExpr;
	}
	
	//todo do i need this?
	public void addExpression() {
		// TODO Auto-generated method stub
		
	}
	
	//todo need check para
	public String toSql(SelectExpression<?> selectExpr){
		
		  IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
			com.arthur.mta.utbdbservice.domain.Object joinedObject =  
					dao.findByObjectName(this.joinedTblExpr.getTableName());
			com.arthur.mta.utbdbservice.domain.Object obj = 
					dao.findByObjectName(this.tblExpr.getTableName());
		
			List<Relationship> relations = getRelationships(joinedObject.getObjectId(),obj.getObjectId());
			
			StringBuilder result = new StringBuilder();
			result.append(this.joinedTblToSql(joinedObject ,doQueryFields(joinedTblExpr.getTableAlias() , selectExpr) ,relations));
			result.append(this.tblToSql( obj , doQueryFields(tblExpr.getTableAlias() , selectExpr),relations));
			result.append(this.conditionToSql(joinedObject , obj));

			return result.toString();
		
	}
	
	private  String [] doQueryFields(String TableAlias ,SelectExpression<?> selectExpr){
		
		List<String> colNames = new ArrayList<String>();
		for (Term t : selectExpr.getTerms()) {
			if(t.getTableAlias()!=null){
				if(t.getTableAlias().equals(TableAlias)){
					colNames.add(t.getColumnName());
				}
			}
		}
		
		if(colNames.size() > 0){
			String strs[] = new String[1];
			return colNames.toArray(strs);
		}else{
			return null;
		}
		
	}
	
	private String joinedTblToSql(com.arthur.mta.utbdbservice.domain.Object joinedObject ,
			String [] queryFields , List<Relationship> relations ){
		
		StringBuilder result = new StringBuilder();
		result.append("( Select  "); 
		if(queryFields != null){
			result.append(joinedObject.columnList(queryFields));
		}else{
			result.append(joinedObject.columnList());
		}
		
		String dataGuid = "";
		for (Relationship relationship : relations) {
			dataGuid += "'"+ relationship.getDataGuid()+"',";
		}
		dataGuid = dataGuid.substring(0 , dataGuid.length() -1);
		
		result.append(" , TenantId  from Data "); 
		result.append(" where DataGuid in (" + dataGuid);
		result.append(" ) ");
		result.append(") as `" + joinedObject.getObjectName() +"`" );
		
		return result.toString();
		
		
	}
	
	private List<Relationship> getRelationships(Integer objectId , Integer targetObjectId){
		
		IRelationshipDao dao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
		List<Relationship>rels = dao.findRelationTIdOidTObjId
				(TenantContextKeeper.getContext().getTenantUser().getTenantId(), objectId, targetObjectId);
		
		return rels;
	}
	
	private String tblToSql(com.arthur.mta.utbdbservice.domain.Object obj , 
			String [] queryFields , List<Relationship> relations){
		
		StringBuilder result = new StringBuilder();
		
		String dataGuid = "";
		for (Relationship relationship : relations) {
			dataGuid += "'"+ relationship.getTargetDataGuid()+"',";
		}
		dataGuid = dataGuid.substring(0 , dataGuid.length() -1);
		
		result.append(" inner join ");
		result.append("( Select ");
		if(queryFields != null){
			result.append(obj.columnList(queryFields));
		}else{
			result.append(obj.columnList());
		}
		result.append(" from Data " );
		result.append(" where DataGuid in (");
		result.append(dataGuid + ") ) as `" + obj.getObjectName()+"` ");
		
		return result.toString();
	}
	
	private String conditionToSql(com.arthur.mta.utbdbservice.domain.Object joinedObject , com.arthur.mta.utbdbservice.domain.Object obj){
		
		IAssociationDao dao = (IAssociationDao) CTX.get().getBean("associationDao");
			com.arthur.mta.utbdbservice.domain.Association associtaion =  
					dao.findByParentChildObjId(joinedObject.getObjectId(), obj.getObjectId(), joinedObject.getTenantId());
			
		if(associtaion == null)
			return conditionToSql(obj,joinedObject);
		
		if(associtaion != null){
			StringBuilder result = new StringBuilder();
			result.append(" on ");
			Field joinedTblField = joinedObject.findFieldByNum(associtaion.getFieldNum().toString());
			Field tblField = obj.findFieldByNum(associtaion.getTargetFieldNum().toString());
			
			result.append("`"+joinedObject.getObjectName() +"`."+ joinedTblField.getFieldName() +"=`"+ obj.getObjectName() +"`."+ tblField.getFieldName() );
			
			return result.toString();
		}
		
		return "";
		
	}

	


	

}
