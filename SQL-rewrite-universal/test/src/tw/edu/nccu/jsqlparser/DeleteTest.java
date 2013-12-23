package tw.edu.nccu.jsqlparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.delete.Delete;

public class DeleteTest {
	public static void main(String[] args) throws JSQLParserException, IOException {
		FileReader fr = new FileReader("/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TenantCnt.txt");
		String _sql = "DELETE FROM Product WHERE TenantId=1 and ProductId='p100'";
		String policy = "CASCADE";
		// config table policy file
		final String PfilePath = "/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TablePolicy.txt";
		BufferedReader in = new BufferedReader(fr); 
		String tmp = in.readLine(); 
		int TenantId = Integer.parseInt(tmp);  
		System.out.println(TenantId);
		fr.close();
		in.close();
		
		FileWriter fout = new FileWriter("/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TenantCnt.txt");
		BufferedWriter out = new BufferedWriter(fout);
		out.write(Integer.toString(2));
		out.flush(); 
		out.close();
		
		int ObjectId = 1;
		String tableName;
		String where = "";
		String[] defaultValues = null;
//		ResultSet rs;
		ArrayList<String> SdataGuidList = new ArrayList<String>();
		ArrayList<String> TdataGuidList = new ArrayList<String>();
		ArrayList<Integer> associationIdList = new ArrayList<Integer>();
		ArrayList<Integer> TfieldNumList = new ArrayList<Integer>();

		// Step 1. get logical sql statement
		CCJSqlParserManager pm = new CCJSqlParserManager();
		Delete jsqldelete = (Delete) pm.parse(new StringReader(_sql));
		// Step 2. transform logical sql to physical sql
		// Step 2a. jsqlparser get each field
		tableName = jsqldelete.getTable().toString();
		if (jsqldelete.getWhere() != null)
			where = jsqldelete.getWhere().toString();
		// get policy config
		List<?> config = loadFileP(tableName, PfilePath);
		int cnt = 0;
		if (config.size() > 1)
			policy = (String) config.get(1);
		if (policy.equals("SET DEFAULT")) {
			for (int i = 3; i < config.size(); i++) {
				defaultValues[cnt++] = (String) config.get(i);
			}
		}
		// Step 2b. transform where condition
		System.out.println(where);
		String[] sarr = where.split(" ");
		System.out.println(where.substring(17));
		for (int i = 0; i < sarr.length; i += 4) { // 抓欄位名稱
			String fieldNum_sql = "SELECT FieldNum FROM fields "
					+ "WHERE TenantId=" + TenantId + " AND ObjectId="
					+ ObjectId + " AND FieldName='" + sarr[i] + "'";
//			rs = stmt.executeQuery(fieldNum_sql);
//			if (rs.next())
			where = where.replace(sarr[i], ("Value" + i));
			System.out.println(sarr[i]);
		}
		// Step 2c. delete data from data table
//		String getGuid_sql = "SELECT DataGuid FROM data " + "WHERE TenantId="
//				+ TenantId + " AND ObjectId=" + ObjectId + " AND " + where;
//		rs = stmt.executeQuery(getGuid_sql);
//		while (rs.next())
			SdataGuidList.add("hello");
		String data_sql = "DELETE FROM data" + " WHERE TenantId=" + TenantId
				+ " AND ObjectId=" + ObjectId + " AND " + where;
//		stmt.executeUpdate(data_sql);
		System.out.println("DELETE DATA SQL: " + data_sql);
//		for (int i = 0; i < SdataGuidList.size(); i++) {
			String getTGuid_sql = "SELECT AssociationId,TargetDataGuid FROM Relationships "
					+ "WHERE SourceDataGuid='" + SdataGuidList.get(0) + "'";
			System.out.println(getTGuid_sql);
//			rs = stmt.executeQuery(getTGuid_sql);
//			while (rs.next()) {
//				associationIdList.add(rs.getInt("AssociationId"));
//				TdataGuidList.add(rs.getString("TargetDataGuid"));
//			}
//		}
		// if PKs exists
		// Step 2d-1. delete data from Uniquefields table
		for (int i = 0; i < SdataGuidList.size(); i++) {
			String unique_sql = "DELETE FROM UniqueFields " 
					+ "WHERE DataGuid='" + SdataGuidList.get(i) + "'"; 
//			stmt.executeUpdate(unique_sql);
			System.out.println("DELETE UNIQUE SQL: " + unique_sql);
			// if IndexType==1 : Primary keys exists
			// Step 2d-2. delete data from Relatoinships table
			String relation_sql = "DELETE FROM Relationships "
					+ "WHERE SourceDataGuid='" + SdataGuidList.get(i) + "'";
//			stmt.executeUpdate(relation_sql);
			System.out.println("DELETE RELATIONSHIPS SQL: " + relation_sql);
		}
		// Step 2d-3. delete data from Associations table
		for (int i = 0; i < associationIdList.size(); i++) {
			String getTfieldNum_sql = "SELECT TargetFieldNum FROM Associations "
					+ "WHERE AssociationId=" + associationIdList.get(i);
//			rs = stmt.executeQuery(getTfieldNum_sql);
//			while (rs.next()) {
//				TfieldNumList.add(rs.getInt("TargetFieldNum"));
//			}
			String association_sql = "DELETE FROM Associations "
					+ "WHERE AssociationId=" + associationIdList.get(i);
//			stmt.executeUpdate(association_sql);
			System.out.println("DELETE ASSOCIATIONS SQL: " + association_sql);
		}
		if (policy.equals("CASCADE")) { // 1. CASCADE policy
			// (optional)如果刪除的是parent data
			// Step 2d-4. delete child data from data table
			for (int i = 0; i < TdataGuidList.size(); i++) {
				String childdata_sql = "DELETE FROM data " + "WHERE DataGuid="
						+ TdataGuidList.get(i);
//				stmt.executeUpdate(childdata_sql);
				System.out.println("CASCADE DELETE CHILD DATA SQL: "
						+ childdata_sql);
			}
			// if IndexType==2 : Foreign key
			// 不用管，只有當source被刪除，才要連帶處理所有的target data
		} else if (policy.equals("SET NULL")) { // 2. SET NULL policy
			// (optional)如果刪除的是parent data
			// Step 2d-3. update child data from data table to null
			for (int i = 0; i < TdataGuidList.size(); i++) {
				String childdata_sql = "UPDATE data " + "SET Value"
						+ TfieldNumList.get(i) + "=NULL" + "WHERE DataGuid="
						+ TdataGuidList.get(i);
//				stmt.executeUpdate(childdata_sql);
				System.out.println("SET NULL DELETE CHILD DATA SQL: "
						+ childdata_sql);
			}
		} else if (policy.equals("SET DEFAULT")) { // 3. SET DEFAULT policy
			// (optional)如果刪除的是parent data
			// Step 2d-3. update child data from data table to default
			// value
			int vcnt = 0;
			for (int i = 0; i < TdataGuidList.size(); i++) {
				if (i > 0 && TfieldNumList.get(i) != TfieldNumList.get(i - 1))
					vcnt++;
				String childdata_sql = "UPDATE data " + "SET Value"
						+ TfieldNumList.get(i) + "=" + "hello"
						+ "WHERE DataGuid=" + TdataGuidList.get(i);
//				stmt.executeUpdate(childdata_sql);
				System.out.println("SET DEFAULT DELETE CHILD DATA SQL: "
						+ childdata_sql);
			}
		}
	}

	// TODO Input policy file
	private static List<?> loadFileP(String objectName, String filePath) {
		String tmp;
		List<?> line = new ArrayList<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(filePath));
			while ((tmp = in.readLine()) != null) {
				String[] sarr = tmp.split(",");
				if (sarr[0].equals(objectName)) {
					line = Arrays.asList(sarr);
					break;
				}
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}
}
