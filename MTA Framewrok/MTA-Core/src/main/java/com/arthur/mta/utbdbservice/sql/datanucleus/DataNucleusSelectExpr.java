package com.arthur.mta.utbdbservice.sql.datanucleus;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.utbdbservice.sql.SelectExpression;
import com.arthur.mta.utbdbservice.sql.Term;


 class DataNucleusSelectExpr  implements  SelectExpression<String> {

	private List<Term > terms = new ArrayList<Term>(); 
	public void addTerm(String term) {
		
		if (!term.contains("AS")) {
			terms.add(doTerm(term));
		}else{
			terms.add(new DataNucleusTerm(term));
		}
		
	}
	
	public List<Term> getTerms() {
		return terms;	
	}
	
	private Term doTerm(String term){
		String [] elements =  term.split("\\.");
		return new DataNucleusTerm(elements[0] , elements[elements.length -1]);
	}



}
