package com.arthur.mta.utbdbservice.domain;

import java.util.List;
import java.util.Map;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.boundary.IDataDao;
import com.arthur.mta.utbdbservice.boundary.IIndexDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IRelationshipDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.command.Delete;
import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.sql.AbstractSQLFactory;
import com.arthur.mta.utbdbservice.sql.AbstractWhereClause;
import com.arthur.mta.utbdbservice.sql.SQLFactoryTypes;
import com.arthur.mta.utbdbservice.sql.WhereClause;

public class DeleteData {
	
	private Command cmd;
	private com.arthur.mta.utbdbservice.domain.Object object;
	private AbstractSQLFactory sqlFactory;
	
	public DeleteData(Command command){
		
		this.cmd = command;
		sqlFactory = AbstractSQLFactory.getFactory(SQLFactoryTypes.UtbDbSrv);
	}
	
	public void excute(){
		

		//todo need add transaction
		Delete delete = (Delete)cmd;
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		object = dao.findByObjectName(delete.getObjectProvider().getObjectName());
		
		String where = this.doWhere();
		String [] ary = where.split("=");
		String dataGuid=ary[1].substring(1 , ary[1].length()-1);
		
		IRelationshipDao relationshipDao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
		List<Relationship> relList = relationshipDao.findRelationByTargetDataGuid(dataGuid);
		
		if(relList.size() == 0){
			
			
			relationshipDao.delete(dataGuid);
			IIndexDao indexDao = (IIndexDao) CTX.get().getBean("indexDao");
			indexDao.delete(dataGuid);

			IUniqueFieldDao uniqueFieldDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
			uniqueFieldDao.delete(dataGuid);
			
			
			
			//relationshipDao.deleteTarget(dataGuid);
			
			/*
			IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
			List<Association> assocs =
					aDao.findByTargetObjId(object.getObjectId(), TenantContextKeeper.getContext().getTenantUser().getTenantId());
			
			if(assocs !=null){
				IRelationshipDao relationshipDao = (IRelationshipDao) CTX.get().getBean("relationshipDao");
				for (Association association : assocs) {
					relationshipDao.deleteTarget(association.getAssocationId() , dataGuid);
				}

			}*/
		
			IDataDao dataDao = (IDataDao) CTX.get().getBean("dataDao");
			dataDao.delete(where);
		}
		
		
		
		
	}
	
	private String doWhere(){
		
		WhereClause whereClause = sqlFactory.createWhereClauseComposite();
		whereClause.setObject(this.object);
		Map<String,String> fieldVaules = this.cmd.getWhereFieldValues();
		int count =0;
		for(Map.Entry<String, String> e :fieldVaules.entrySet()){
			whereClause.add(sqlFactory.createWhereClause(e.getKey()+" = "+e.getValue()));
			count +=1;
			if(count < fieldVaules.size()) whereClause.addLogicOperator("AND");
			
		}
		
		return whereClause.toSql();
	}
	
	

}
