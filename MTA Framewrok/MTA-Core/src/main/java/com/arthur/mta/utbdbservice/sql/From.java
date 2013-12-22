package com.arthur.mta.utbdbservice.sql;

import java.util.List;

public interface From<T> {
	
	//FROM {tblExpr} [joinInfo {tblExpr} ON ...] ...
	void addTable(T tblExpr);
	

}
