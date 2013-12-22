package com.arthur.mta.utbdbservice.sql.datanucleus;

import java.util.List;

import com.arthur.mta.utbdbservice.sql.OrderByExpression;
import com.arthur.mta.utbdbservice.sql.Term;

public class DataNucleusOrderByExpr implements OrderByExpression {

	private Term term  = null;
	private boolean isDesc = false;
	public void addTerm(String term) {
		String [] elements =  term.split("\\.");
		
		if(elements[1].contains("INTEGER_IDX")){
			this.term = (Term)(new DataNucleusTerm("pkfield"));
		}else{
			this.term = (Term)(new DataNucleusTerm(elements[1]));
		}
		
		
	}

	public Term getTerm() {
		return this.term;
	}

	public void setDirection(boolean direction) {
		isDesc = direction;
	}

	public boolean isDesc() {
		return isDesc;
	}


}
