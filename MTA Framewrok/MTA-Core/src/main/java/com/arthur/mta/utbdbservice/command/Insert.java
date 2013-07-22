package com.arthur.mta.utbdbservice.command;

import java.util.ArrayList;
import java.util.List;

import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.command.ObjectProvider;

public class Insert extends Command {
	
	public Insert(ObjectProvider objectProvider ){
		 super(objectProvider);
		 
	}
	
	@Override
	public List excute(){
		
		com.arthur.mta.utbdbservice.domain.InsertData svD = 
				new com.arthur.mta.utbdbservice.domain.InsertData(this);
		
		String procDataPk = svD.excute();
		
		List<String> pk = new ArrayList<String>();
		pk.add(procDataPk);

		 return pk;
		
	}
	

}
