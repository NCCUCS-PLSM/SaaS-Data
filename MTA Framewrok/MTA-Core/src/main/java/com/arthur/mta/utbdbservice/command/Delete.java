package com.arthur.mta.utbdbservice.command;


import java.util.List;

import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.command.ObjectProvider;

public class Delete extends Command {
	
	public Delete(ObjectProvider objectProvider) {
		super(objectProvider);
		// TODO Auto-generated constructor stub
	}
	
	public Delete(ObjectProvider objectProvider,  String [] whereFields ) {
		super(objectProvider,whereFields);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List excute() {
	
		com.arthur.mta.utbdbservice.domain.DeleteData svD = 
				new com.arthur.mta.utbdbservice.domain.DeleteData(this);
		
		svD.excute();
		
		return null;
	}
	
	
	
	

}
