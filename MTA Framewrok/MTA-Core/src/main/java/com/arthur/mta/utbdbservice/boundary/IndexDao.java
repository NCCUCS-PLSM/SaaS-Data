package com.arthur.mta.utbdbservice.boundary;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.domain.Index;
import com.arthur.mta.utbdbservice.util.CTX;



@Repository("indexDao")
public class IndexDao extends Dao implements IIndexDao{

	public void save(com.arthur.mta.utbdbservice.domain.Index index) {
			
		String sql = "INSERT INTO `Indexes` (`DataGuid` ,`TenantId`, `ObjectId`, `FieldNum`," ;

		if(index.getStringValue()!=null)
			sql += "`StringValue`)";
		else{
			if(index.getNumValue()!=null)
				sql += "`NumValue`)";
			else
				sql += "`DateValue`)";
		}
		
		sql +="	VALUES ('" + index.getDataGuid() +"',";
		sql += index.getTenantId().toString() + "," + index.getObjectId().toString() +"," + index.getFieldNum() +",";
		
		if(index.getStringValue()!=null)
			sql += "'" + index.getStringValue() +"')";
		else{
			if(index.getNumValue()!=null)
				sql += "'" + index.getNumValue() +"')";
			else
				sql += "'" + index.getDateValue() +"')";
		}

		this.update(sql);
		
		
	}

	public void update(com.arthur.mta.utbdbservice.domain.Index index) {
		
		String sql = "UPDATE  `Indexes` SET ";
		if(index.getStringValue()!=null)
			sql += " `StringValue`='" + index.getStringValue() +"'";
		else{
			if(index.getNumValue()!=null)
				sql += " `NumValue`='" + index.getNumValue() +"'";
			else
				sql += " `DateValue`='" + index.getDateValue() +"'";
		}
		
		sql +=  " WHERE `TenantId`="+ index.getTenantId().toString() +" AND " ;
		sql +=  " `ObjectId`=" + index.getObjectId().toString() +" AND `FieldNum`=" + index.getFieldNum() ;
		sql +=" AND `DataGuid`='" + index.getDataGuid() +"'";
				
		
		
		this.update(sql);
		
	}

	public void delete(String dataGuid) {
		
		String sql = "Delete FROM  `Indexes` ";
		sql +=  " WHERE DataGuid='"+ dataGuid+"' ";
		
		this.update(sql);
		
	}
	
	public com.arthur.mta.utbdbservice.domain.Index  findByFieldlNum(Integer objectId, String fieldNum , String dataGuid) {

		
			 String sql =  "Select * from Indexes  where  TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId() 
					 +" AND ObjectId =" + objectId  + " and FieldNum =" + fieldNum +" and DataGuid='"+dataGuid+"'";
			 
				List rows =this.queryResult(sql);
				
				List<Index> ifields  = new ArrayList<Index>();
				for (java.lang.Object object : rows) {
					ifields.add(createUniqueField(((Map)object)));
				}
				
				if(ifields.size() > 0){
					return ifields.get(0);
				}
				
		
				return null;
	
	}

	public List<com.arthur.mta.utbdbservice.domain.Index>  findByStringValue(
			Integer objectId, String fieldNum, String stringValue , String compareOperator) {

		if(stringValue != null && !stringValue.isEmpty()){
			 String sql =  "Select * from Indexes  where  TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId() 
					 +" AND ObjectId =" + objectId  + " and FieldNum =" + fieldNum +
					 " and StringValue"+ compareOperator +"'"+ stringValue +"'" ;

				List rows =this.queryResult(sql);
				
				List<Index> ifields  = new ArrayList<Index>();
				for (java.lang.Object object : rows) {
					ifields.add(createUniqueField(((Map)object)));
				}
				
			 
			return ifields;
		};
		
		return null;
	
	}

	public List<com.arthur.mta.utbdbservice.domain.Index>  findByNumValue( Integer objectId, String fieldNum,
			String numValue , String compareOperator) {
		
		 if(this.isNumeric(numValue)){
			 String sql =  "Select * from Indexes  where  TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId()
					 + " AND ObjectId=" + objectId + " and FieldNum =" + fieldNum 
					 + " and NumValue"+ compareOperator + numValue  ;
			 
				List rows =this.queryResult(sql);
				List<Index> ifields  = new ArrayList<Index>();
				for (java.lang.Object object : rows) {
					ifields.add(createUniqueField(((Map)object)));
				}
			 
			return ifields;
		 }
		 
		 return null;
	

	}

	public List<com.arthur.mta.utbdbservice.domain.Index> findByDateValue( Integer objectId, String fieldNum,
			String dateValue , String compareOperator) {

		Date date = this.isValidDate(dateValue,"EEE MMM dd HH:mm:ss zzz yyyy");
		if(date!=null){
			
			 //System.out.println(d);
			 DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			 dateValue = sdf.format(date);
			
			 String sql =  "Select * from Indexes  where  TenantId ="+ TenantContextKeeper.getContext().getTenantUser().getTenantId()
					 + " AND ObjectId=" + objectId + " and FieldNum =" + fieldNum 
					 + " and DateValue"+ compareOperator+"'"+ dateValue  +"'";
			 
				List rows =this.queryResult(sql);
				List<Index> ifields  = new ArrayList<Index>();
				for (java.lang.Object object : rows) {
					ifields.add(createUniqueField(((Map)object)));
				}
			 
				return ifields;
		}
		
		return null;

	}
	
	private Index createUniqueField(Map map){
		
		Index uf = new Index();
		
		uf.setDataGuid(map.get("DataGuid").toString()); 
		uf.setObjectId((Integer) map.get("ObjectId")); 
		uf.setTenantId((Integer) map.get("TenantId")); 
		uf.setFieldNum((Integer) map.get("FieldNum")); 
		if(map.get("StringValue") != null)uf.setStringValue( map.get("StringValue").toString()); 
		
		if(map.get("NumValue") != null){
			BigDecimal d = new BigDecimal(map.get("NumValue").toString());
			uf.setNumValue(d); 
		}
		if(map.get("DateValue") != null)uf.setDateValue( map.get("DateValue").toString()); 

		
	    return uf;
		
	}
	
	private Date isValidDate(String dateToValidate ,String dateFromat){
		 
		
	
		if(dateToValidate == null){
			return null;
		}
 
		SimpleDateFormat sdf = new SimpleDateFormat(dateFromat);
		sdf.setLenient(false);
 
		try {
 
			//if not valid, it will throw ParseException
			Date date = sdf.parse(dateToValidate);
			//System.out.println(date);
			return date;
 
		} catch (ParseException e) {
			if(!dateFromat.equals("yyyy/MM/dd")){
				return this.isValidDate(dateToValidate, "yyyy/MM/dd");
			}else{
				//e.printStackTrace();
				return null;
			}

		}
 
		
	}
	
	private  boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    double d = Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}
	


}
