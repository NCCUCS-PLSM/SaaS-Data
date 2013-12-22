package org.datanucleus.store.rdbms.art.utb;


import java.lang.annotation.Annotation;
import java.sql.ResultSet;
import java.util.List;

import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.rdbms.sql.SQLJoin;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.expression.BooleanExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;

import com.arthur.mta.core.CustomObjectMapper;
import com.arthur.mta.core.CustomObjectTransformer;


import com.arthur.mta.utbdbservice.command.Delete;
import com.arthur.mta.utbdbservice.command.Insert;
import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.command.ObjectProvider;
import com.arthur.mta.utbdbservice.command.Update;
import com.arthur.mta.utbdbservice.sql.AbstractSQLFactory;
import com.arthur.mta.utbdbservice.sql.Join;
import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.SQLFactoryTypes;
import com.arthur.mta.utbdbservice.sql.SelectExpression;

public class UTbServiceHelper {
	
	 //todo arthur add
    public static String generateUTbSql(SQLStatement stmt , String className ){
    	
    	
    	AbstractSQLFactory df = AbstractSQLFactory.getFactory(SQLFactoryTypes.DataNucleus);
    	
    	com.arthur.mta.utbdbservice.sql.SelectSQLStatement artSqlStmt =  df.createSelectSQLStatement();
    	
    	artSqlStmt.setRange(stmt.getRangeOffset(), stmt.getRangeCount());
    	
        com.arthur.mta.utbdbservice.sql.SelectExpression<String> artSelects =  (SelectExpression) df.createSelectExpression();
         for (String term : stmt.getSelects()) {
         	artSelects.addTerm(term);
         }
        
         artSqlStmt.addSelect(artSelects);
         
         if(className!= null){
        	 
        	 com.arthur.mta.utbdbservice.sql.TableExpression tbExpr =  df.createTableExpression(className);
        	
        	 if(stmt.getJoins() != null){
	        	 for (SQLJoin j : stmt.getJoins()) {
	        		Join join = df.createJoin();
	        		join.addJoinedTable(j.getJoinedTable().toString());
	        		join.addTable(j.getTable().toString());
	        		tbExpr.addJoin(join);
	        	 }
        	 }
        	 
        	
             artSqlStmt.addFrom(tbExpr);
         }
         
    
         BooleanExpression be =  stmt.getWhere();
         if(be != null){
        	 com.arthur.mta.utbdbservice.sql.WhereClause where =
             		be.toSQLText().toUTbDBService();

             artSqlStmt.addWhere(where);
         }
         
         if(stmt.getOrderingExpressions() != null){ 
        	 OrderByExpression orderByExpr = df.createOrderByExpression();
        	 SQLExpression[] sqlExprs =  stmt.getOrderingExpressions();
        	 orderByExpr.addTerm(sqlExprs[0].getSubExpression(0).toSQLText().toSQL());
        	 orderByExpr.setDirection(stmt.getOrderingDirections()[0]);
        	 artSqlStmt.addOrderBy(orderByExpr);	 
         }
         
        
        String sqlText = artSqlStmt.toSql();
        System.out.println(sqlText);
        return sqlText ;

    }
    
    public static String insert(Object obj , StringBuffer columnNames){
    	
    	//todo arthur need refactoring
    	String [] fields = 	columnNames.toString().split(",");
    	com.arthur.mta.utbdbservice.command.ObjectProvider ob = new ObjectProvider(obj, fields  );
    	Command operation = new Insert(ob);
    	List<String> pk = operation.excute();
    	return pk.get(0);
    	
    	
    }
    
    public static void update(Object obj , StringBuffer columnAssignments , StringBuffer where ){
    	
    	//todo arthur need refactoring
    	String [] updateFields = 	columnAssignments.toString().split("=\\?");
    	String [] whereFileds = where.toString().split("=\\?");
    	com.arthur.mta.utbdbservice.command.ObjectProvider ob = new ObjectProvider(obj, updateFields  );
    	Command operation = new Update(ob , whereFileds );
    	operation.excute();
    	
    }
    
    public static void delete(Object obj , StringBuffer where ){
    	
    	//todo arthur need refactoring
	  	String [] whereFileds = where.toString().split("=\\?");
    	com.arthur.mta.utbdbservice.command.ObjectProvider ob = new ObjectProvider(obj);
    	Command operation = new Delete(ob , whereFileds);
    	operation.excute();
    
   }
    
   public static Object doCustomField(Object obj , ResultSet resultSet , StatementClassMapping stmtMapping){
	   
	   CustomObjectMapper mapper = new CustomObjectMapper();
	   obj = mapper.mapCustomFields(obj, resultSet,  stmtMapping.getMemberNumbers());
	   return obj;
	  
   }
   
   public static boolean isMTAAnnoated(Annotation[] annotations){
	   
	   for (Annotation annotation : annotations) {
		  if( annotation.annotationType().equals(com.arthur.mta.core.annotations.MultiTenantable.class)){
			  return true;
		  }
	   }
	   
	   
	   return false;
   }
    
	
}
