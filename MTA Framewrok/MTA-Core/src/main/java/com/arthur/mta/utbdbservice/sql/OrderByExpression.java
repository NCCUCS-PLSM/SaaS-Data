package com.arthur.mta.utbdbservice.sql;


public interface OrderByExpression {
	
	void addTerm(String term);
	Term getTerm();
	void setDirection(boolean direction);
	boolean isDesc();

}
