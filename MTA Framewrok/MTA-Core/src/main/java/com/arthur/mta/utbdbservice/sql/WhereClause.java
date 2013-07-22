package com.arthur.mta.utbdbservice.sql;

import java.util.List;

public interface WhereClause{
	
  void add(WhereClause whereClause);
  
  void addLogicOperator(String operator);
  
  String toSql();
  
  int getTenantId();
  
  void barcketed(Boolean isBarcketed);
  
  WhereClause getChild(int index);
  
  int getLastChildIndex();
   
  void setObject(com.arthur.mta.utbdbservice.domain.Object object);
  
  void setTableExpr(TableExpression tblExpr);
   
  void setIsJoin(boolean isJoin);
  
  boolean isContainUniqueField();

   
}
