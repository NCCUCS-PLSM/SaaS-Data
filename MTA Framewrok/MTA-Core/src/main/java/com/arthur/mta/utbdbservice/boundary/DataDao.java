package com.arthur.mta.utbdbservice.boundary;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;


import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.boundary.IDataDao;
import com.arthur.mta.utbdbservice.domain.Data;
import com.arthur.mta.utbdbservice.domain.Field;



@Repository("dataDao")
public class DataDao extends Dao implements IDataDao {

	public void save(Data data) {
		
		//todo arthur remove isddeleted
		String sql = "INSERT INTO `Data` (`DataGuid` ,`TenantId`, `ObjectId`, `Name`,  `Value0`, `Value1`, `Value2`, `Value3` ";
		sql +=" , `Value4`, `Value5`, `Value6`, `Value7` , `Value8`, `Value9`, `Value10`, `Value11` , `Value12`, `Value13`, `Value14`, `Value15`";
		sql +=" , `Value16`, `Value17`, `Value18`, `Value19` ) VALUES ('" + data.getDataGuid() +"',";
		sql += data.getTenantId().toString() + "," + data.getObjectId().toString() +",'" + data.getName() +"',";
		sql += "'"+ data.getValue0() +"','"+ data.getValue1() +"','"+ data.getValue2() +"','"+ data.getValue3() +"','";
		sql +=  data.getValue4() +"','"+ data.getValue5() +"','"+ data.getValue6() +"','"+ data.getValue7() +"','";
		sql +=  data.getValue8() +"','"+ data.getValue9() +"','"+ data.getValue10() +"','"+ data.getValue11() +"','";
		sql +=  data.getValue12() +"','"+ data.getValue13() +"','"+ data.getValue14() +"','"+ data.getValue15() +"','";
		sql +=  data.getValue16() +"','"+ data.getValue17() +"','"+ data.getValue18() +"','"+ data.getValue19() +"')";
		
		
		this.update(sql);

	}

	public void update(Data data) {
		//todo need check maybe don't update so many column
		String sql = "UPDATE  `Data` SET  `Value0`='" + data.getValue0() +"', `Value1`='" + data.getValue1() +"', `Value2`='" + data.getValue2() +"', ";
		sql += " `Value3`='" + data.getValue3() +"', `Value4`='" + data.getValue4() +"', `Value5`='" + data.getValue5() +"', ";
		sql += " `Value6`='" + data.getValue6() +"', `Value7`='" + data.getValue7() +"', `Value8`='" + data.getValue8() +"', ";
		sql += " `Value9`='" + data.getValue9() +"', `Value10`='" + data.getValue10() +"', `Value11`='" + data.getValue11() +"', ";
		sql += " `Value12`='" + data.getValue12() +"', `Value13`='" + data.getValue13() +"', `Value14`='" + data.getValue14() +"', ";
		sql += " `Value15`='" + data.getValue15() +"', `Value16`='" + data.getValue16() +"', `Value17`='" + data.getValue17() +"', ";
		sql += " `Value18`='" + data.getValue18() +"', `Value19`='" + data.getValue19() +"'  ";
		sql +=  " WHERE `DataGuid`='" + data.getDataGuid() +"' ";
		
		this.update(sql);
		 	 
	}

	public void delete(String where) {
		
		String sql = "Delete FROM  `Data` ";
		sql +=  " WHERE " + where;
		
		this.update(sql);
		
	}

	public Data findByDataGuid(String dataGuid) {
		/*
		 List data = hibernateTemplate.find(
	                "from Data where DataGuid ='" + dataGuid +"'");
	        return (Data) data.get(0);*/
	        
		String sql =  "Select * from Data where DataGuid ='" + dataGuid +"'";
		List rows = this.queryResult(sql);
		
		Data data = null;
		for (java.lang.Object object : rows) {
			data = createData(((Map)object));			
		}
		
		return data;
	
	}
	
	public Data findByPkField(Field pkField , String pkVal) {
		/*
		 List data = hibernateTemplate.find(
	                "from Data where DataGuid ='" + dataGuid +"'");
	        return (Data) data.get(0);*/
	        
		String sql =  "Select * from Data where TenantId =" + TenantContextKeeper.getContext().getTenantUser().getTenantId()
				+" AND ObjectId="+ pkField.getObjectId() + " AND " + pkField.columnName() + "='"+ pkVal +"'";
		List rows = this.queryResult(sql);
		
		Data data = null;
		for (java.lang.Object object : rows) {
			data = createData(((Map)object));			
		}
		
		return data;
	
	}
	
	public List<Data> findByObjectId(int objectId) {
		
		String sql =  "Select * from Data where TenantId="+  TenantContextKeeper.getContext().getTenantUser().getTenantId() +" and ObjectId="+ objectId;
		List rows = this.queryResult(sql);
		
		List<Data> dataList = new ArrayList<Data>();
		for (java.lang.Object object : rows) {
			Data data = createData(((Map)object));		
			dataList.add(data);
		}
		
		return dataList;
	}
	
	public List<Data> findByIndexesDataGuid(int objectId, String indexesClauses) {
		
		String sql =  "SELECT * FROM Data WHERE TenantId="+  TenantContextKeeper.getContext().getTenantUser().getTenantId() 
				+" AND ObjectId="+ objectId +" AND " + indexesClauses ;
		List rows = this.queryResult(sql);
		
		List<Data> dataList = new ArrayList<Data>();
		for (java.lang.Object object : rows) {
			Data data = createData(((Map)object));		
			dataList.add(data);
		}
		
		return dataList;
	}
	
	private Data createData(Map map){
		
		Data data = new Data();
		data.setDataGuid( map.get("dataGuid").toString()); 
		data.setTenantId((Integer) map.get("TenantId"));
		data.setObjectId((Integer) map.get("objectId"));
		data.setName( map.get("name").toString()); 
		//data.setIsDeleted( map.get("isDeleted").toString());
		
		if(map.get("value0")!= null)data.setValue0(map.get("value0").toString());
		if(map.get("value1")!= null)data.setValue1(map.get("value1").toString());
		if(map.get("value2")!= null)data.setValue2(map.get("value2").toString());
		if(map.get("value3")!= null)data.setValue3(map.get("value3").toString());
		if(map.get("value4")!= null)data.setValue4(map.get("value4").toString());
		
		if(map.get("value5")!= null)data.setValue5(map.get("value5").toString());
		if(map.get("value6")!= null)data.setValue6(map.get("value6").toString());
		if(map.get("value7")!= null)data.setValue7(map.get("value7").toString());
		if(map.get("value8")!= null)data.setValue8(map.get("value8").toString());
		if(map.get("value9")!= null)data.setValue9(map.get("value9").toString());
		
		if(map.get("value10")!= null)data.setValue10(map.get("value10").toString());
		if(map.get("value11")!= null)data.setValue11(map.get("value11").toString());
		if(map.get("value12")!= null)data.setValue12(map.get("value12").toString());
		if(map.get("value13")!= null)data.setValue13(map.get("value13").toString());
		if(map.get("value14")!= null)data.setValue14(map.get("value14").toString());
		
		if(map.get("value15")!= null)data.setValue15(map.get("value15").toString());
		if(map.get("value16")!= null)data.setValue16(map.get("value16").toString());
		if(map.get("value17")!= null)data.setValue17(map.get("value17").toString());
		if(map.get("value18")!= null)data.setValue18(map.get("value18").toString());
		if(map.get("value19")!= null)data.setValue19(map.get("value19").toString());
		
		
		return data;
		
	}


	 




}
