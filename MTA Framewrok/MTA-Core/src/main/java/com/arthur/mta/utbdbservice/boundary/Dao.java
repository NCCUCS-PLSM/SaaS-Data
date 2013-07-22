package com.arthur.mta.utbdbservice.boundary;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.arthur.mta.utbdbservice.util.CTX;

public abstract class Dao {
	
	 List<Map<String,Object>> queryResult(String sql){
		
		DataSource dataSource =(DataSource) CTX.get().getBean("dataSource");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<Map<String,Object>> result = jdbcTemplate.queryForList(sql);
		
		return result ;
		
	}
    
    
      void update(String sql){
    	
		//todo arthur need refactoring
		DataSource dataSource =(DataSource) CTX.get().getBean("dataSource");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update(sql);
		
	
	}

}
