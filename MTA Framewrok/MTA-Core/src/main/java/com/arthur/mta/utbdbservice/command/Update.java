package com.arthur.mta.utbdbservice.command;

import java.util.List;

import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.command.ObjectProvider;

public class Update extends Command {
	

	public Update(ObjectProvider objectProvider) {
		super(objectProvider);

	}
	
	public Update(ObjectProvider objectProvider,  String [] whereFields ) {
		super(objectProvider,whereFields );
		// TODO Auto-generated constructor stub
	}

	@Override
	public List excute() {
	
		com.arthur.mta.utbdbservice.domain.UpdateData svD = 
				new com.arthur.mta.utbdbservice.domain.UpdateData(this);
		
		svD.excute();
		
		return null;
	}

}
