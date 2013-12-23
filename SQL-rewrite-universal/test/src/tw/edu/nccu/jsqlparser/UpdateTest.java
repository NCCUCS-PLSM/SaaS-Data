package tw.edu.nccu.jsqlparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.update.Update;

public class UpdateTest {
	public static void main(String[] args) throws JSQLParserException, IOException {
//		FileReader fr = new FileReader(
//				"/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TenantCnt.txt");
//		BufferedReader in = new BufferedReader(fr);
//		int TenantId = Integer.parseInt(in.readLine());
//		in.close();
//		fr.close();
		// Runtime output TenantId
//		FileWriter fout = new FileWriter(
//				"/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TenantCnt.txt");
//		BufferedWriter out = new BufferedWriter(fout);
//		out.write(Integer.toString(TenantId + 1));
//		out.flush();
//		out.close();
//		fout.close();

		// stmt = conn.createStatement();
		int TenantId = 1; 
		String policy = "CASCADE";
		// ResultSet rs;
		String _sql = "Update Product SET ProductId='p200' WHERE ProductId='p3'";
		List<?> columns;
		List<?> expressions;
		ArrayList<String> SdataGuidList = new ArrayList<String>();
		ArrayList<String> TdataGuidList = new ArrayList<String>();
		ArrayList<Integer> associationIdList = new ArrayList<Integer>();
		ArrayList<Integer> TfieldNumList = new ArrayList<Integer>();
		Map<Integer, String> pkMap = new HashMap<Integer, String>();
		// Step 1. get logical sql statement
		CCJSqlParserManager pm = new CCJSqlParserManager();
		Update jsqlupdate = (Update) pm.parse(new StringReader(_sql));
		// Step 2. transform logical sql to physical sql
		// Step 2a. jsqlparser get each field
		String tableName = jsqlupdate.getTable().toString();
		int ObjectId = 1;
		String where = "";
		// ObjectId = objectNameTrans(conn, TenantId, tableName);
		columns = jsqlupdate.getColumns();
		expressions = jsqlupdate.getExpressions();
		Iterator<?> columnItr = columns.iterator();
		Iterator<?> expressionItr = expressions.iterator();
		if (jsqlupdate.getWhere() != null)
			where = jsqlupdate.getWhere().toString();
		// get policy config
		String PfilePath = "/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TablePolicy.txt";
		String[] defaultValues = new String[100];
		List<?> config = loadFileP("OrderLineitem", PfilePath);
		int cnt = 0;
		if (config.size() > 1) 
			policy = (String) config.get(2);
		System.out.println(config.size()+ " " + policy);
		if (policy.equals("SET DEFAULT")) {
			for (int i = 3; i < config.size(); i++) {
				defaultValues[cnt++] = config.get(i).toString();
			}
		}
		// Step 2b. transform where condition
		String[] sarr = where.split(" ");
		for (int i = 0; i < sarr.length; i+=4) {
			String fieldNum_sql = "SELECT FieldNum FROM fields "
					+ "WHERE TenantId=" + TenantId + " AND ObjectId="
					+ ObjectId + " AND FieldName='" + sarr[i] + "'";
			System.out.println(fieldNum_sql);
			// rs = stmt.executeQuery(fieldNum_sql);
			// if (rs.next())
			 where = where.replace(sarr[i], "Value0");
		}
		// Step 2c. update data from data table
		String getGuid_sql = "SELECT DataGuid FROM data " + "WHERE TenantId="
				+ TenantId + " AND ObjectId=" + ObjectId + " AND " + where;
		System.out.println(getGuid_sql);
		// rs = stmt.executeQuery(getGuid_sql);
		// while (rs.next())
		// SdataGuidList.add(rs.getString("DataGuid"));
		String exprTmp = "";
		String fieldNameTmp = "";
		int fieldNumTmp = 0;
		String valueTmp = "";
		while (columnItr.hasNext() && expressionItr.hasNext()) {
			fieldNameTmp = columnItr.next().toString().trim();
			// fieldNumTmp = fieldNameTrans(conn, TenantId, ObjectId,
			// fieldNameTmp);
			
			valueTmp = expressionItr.next().toString().trim();
			exprTmp += "Value" + fieldNumTmp + "=" + valueTmp;
			// TODO PK fieldNUm list
			String getindexType_sql = "SELECT IndexType FROM fields "
					+ "WHERE TenantId=" + TenantId + " AND ObjectId="
					+ ObjectId + " AND FieldName='" + fieldNameTmp + "'";
			System.out.println(getindexType_sql);
			// rs = stmt.executeQuery(getindexType_sql);
			// if (rs.next() && rs.getString("IndexType").equals("1"))
			pkMap.put(fieldNumTmp, valueTmp);
			if (columnItr.hasNext())
				exprTmp += ",";
		}
		String data_sql = "UPDATE data SET " + exprTmp + " WHERE " + where;
		// stmt.executeUpdate(data_sql);
		System.out.println("UDPATE DATA SQL: " + data_sql);
		if (pkMap.size() > 0) { // 如果更新欄位中包含pk，則child data才要做更新
			// TODO 不知道是否有更新到有關連性的欄位 check fields table
			System.out.println("in"); 
			for (int i = 0; i < SdataGuidList.size(); i++) {
				String getTGuid_sql = "SELECT AssociationId,TargetDataGuid FROM Relationships "
						+ "WHERE SourceDataGuid=" + SdataGuidList.get(i);
				// rs = stmt.executeQuery(getTGuid_sql);
				// while (rs.next()) {
				// associationIdList.add(rs.getInt("AssociationId"));
				// TdataGuidList.add(rs.getString("TargetDataGuid"));
			}
		}
		// get TargetFieldNum list
		for (int i = 0; i < associationIdList.size(); i++) {
			String getfieldNum_sql = "SELECT TargetFieldNum FROM Associations "
					+ "WHERE AssociationId=" + associationIdList.get(i);
			// rs = stmt.executeQuery(getfieldNum_sql);
			// if (rs.next())
			// TfieldNumList.add(rs.getInt("TargetFieldNum"));
		}
		// TODO if columns exists PK
		// Step 2d-1. udpate data from UniqueFields table
		// 與delete不同要注意唯一性的問題，若存在則reject
		// fieldNum是為了區分多個primary key的情況
		Set<Integer> set = pkMap.keySet();
		System.out.println(pkMap.get(0));
		for (int i = 0; i < SdataGuidList.size(); i++) {
			Iterator<Integer> pkset = set.iterator();
			while (pkset.hasNext()) {
				int key = pkset.next();
				String unique_sql = "UPDATE UniqueFields SET " + "stringValue="
						+ pkMap.get(key) + " WHERE DataGuid="
						+ SdataGuidList.get(i) + " AND FieldNum=" + key;
				// stmt.executeUpdate(unique_sql);
				System.out.println("UDPATE UNIQUE SQL: " + unique_sql);
			}
		}
		// TODO update parent's data then child's data must be
		// updated
		if (policy.equals("CASCADE")) { // 1. CASCADE policy
			// 每一個relation record只會記錄一個欄位的關聯性資料
			// 所以每一個list item對一個pkMap的值即可
			Iterator<Integer> Tset = set.iterator();
			for (int i = 0; i < TdataGuidList.size(); i++) {
				String childdata_sql = "UPDATE data SET " + "Value"
						+ TfieldNumList.get(i) + "=" + pkMap.get(Tset.next())
						+ "WHERE DataGuid=" + TdataGuidList.get(i);
				// stmt.executeUpdate(childdata_sql);
				System.out.println("CASCADE UPDATE CHILD DATA SQL: "
						+ childdata_sql);
				if (!Tset.hasNext())
					Tset = set.iterator();
			}
		} else if (policy.equals("SET NULL")) { // 2. SET NULL
												// policy
			// 每一個relation record只會記錄一個欄位的關聯性資料
			// 所以每一個list item對一個pkMap的值即可
			Iterator<Integer> Tset = set.iterator();
			for (int i = 0; i < TdataGuidList.size(); i++) {
				String childdata_sql = "UPDATE data SET " + "Value"
						+ TfieldNumList.get(i) + "=NULL" + "WHERE DataGuid="
						+ TdataGuidList.get(i);
				// stmt.executeUpdate(childdata_sql);
				System.out.println("SET NULL UPDATE CHILD DATA SQL: "
						+ childdata_sql);
				if (!Tset.hasNext())
					Tset = set.iterator();
			}
		} else if (policy.equals("SET DEFAULT")) { // 3. SET DEFAULT
													// policy
			int vcnt = 0;
			for (int i = 0; i < TdataGuidList.size(); i++) {
				if (i > 0 && TfieldNumList.get(i) != TfieldNumList.get(i - 1))
					vcnt++;
				String childdata_sql = "UPDATE data " + "SET Value"
						+ TfieldNumList.get(i) + "=" + defaultValues[vcnt]
						+ "WHERE DataGuid=" + TdataGuidList.get(i);
				// stmt.executeUpdate(childdata_sql);
				System.out.println("SET DEFAULT UPDATE CHILD DATA SQL: "
						+ childdata_sql);
			}
		}
	}

	// TODO Input policy file
	static List<?> loadFileP(String objectName, String filePath) {
		String tmp;
		List<?> line = null;

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
