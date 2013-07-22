package com.arthur.mta.utbdbservice.sql;

import java.util.List;

public interface SelectExpression<T> {
	
	void addTerm(T term);
	List<Term> getTerms();
	
}
