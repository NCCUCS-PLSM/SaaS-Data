package tw.edu.nccu.jsqlparser;

import java.io.StringReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

public class InsertTest {

	public static void main(String[] args) throws JSQLParserException { 
		int TenantId = 1;
		int ObjectId = 1;
		String dataGuid = "hello";
		String tableName;
		List<?> columnList;
		String itemList;
		String[] valueArray;
		String where;
		String[] defaultValues;
		String valuesString;
		
		valuesString = "Value0";
		for (int i = 1; i <= 20; i++) {
			valuesString += ",Value" + i;
		}
		CCJSqlParserManager pm = new CCJSqlParserManager();  
		String columnTmp = "";
		int columnCnt = 1; 
		String _sql = "INSERT INTO OrderLineitem(TenantId,OrderLineitemId,OrderId,ProductId,Qty,SubTotal) "
				+ "VALUES(1,'ol100','o100','p100',100,12345)"; 
		Insert jsqlinsert = (Insert) pm.parse(new StringReader(
				_sql));
		// Step 2. transform logical sql to physical sql
		// Step 2a. jsqlparser get each field
		tableName = jsqlinsert.getTable().toString();
		columnList = jsqlinsert.getColumns();
		itemList = jsqlinsert.getItemsList().toString();
		itemList = itemList.replace("(", "").replace(")", ""); 
		valueArray = itemList.split(",");
		itemList = itemList.substring(itemList.indexOf(",")+2);
		System.out.println(itemList);
		
//		System.out.println(itemList);
//		ObjectId = objectNameTrans(conn, TenantId, tableName);
		// Step 2b. Insert data into Data table
		String data_sql = "INSERT INTO data"
				+ "(DataGuid,TenantId,ObjectId,Name,isDeleted,"
				+ valuesString + ") VALUES('" + dataGuid + "','"
				+ TenantId + "','" + ObjectId + "','" + tableName + "','" + 0 
				+ "'," + itemList;
		for (int i=0; i<20-(valueArray.length)+1; i++) {
			data_sql += ",null";
		}
		data_sql += ")";
		System.out.println("INSERT DATA SQL: " + data_sql);
		//stmt.executeUpdate(data_sql);
		// Step 2c-1. Insert data into Uniquefields table
		String indexType;
		String unique_sql = "";
		// Step 2d-1. Insert data into Associations table
		// TODO IndexType==2 代表為 foreign key
		// TODO 這樣測資程式也需要修改...
		// TODO AssociationId有bug...
		int SfieldNum = 0, TfieldNum = 0;
		int SfieldId = 0, TfieldId = 0;
		int SobjectId = 0, TobjectId = 0;
		String SdataGuid = "hello";
		String association_sql = "";
		String relation_sql = "";
		int associationId = 1;
		Iterator<?> columnItr = columnList.iterator();
		while (columnItr.hasNext()) {
			// 1. check this field's IndexType
			// IndexType==1
			indexType = "1";
			columnTmp = columnItr.next().toString().trim();
			String check_type_sql = "SELECT IndexType FROM fields "
					+ "WHERE TenantId=" + TenantId + " AND ObjectId="
					+ ObjectId + " AND FieldName='" + columnTmp + "'";
			System.out.println(columnTmp);
//			ResultSet rs = stmt.executeQuery(check_type_sql);
//			if (rs.next())
//				indexType = rs.getString("IndexType");
			if (indexType.equals("1")) {
				unique_sql = "INSERT INTO UniqueFields"
						+ "(DataGuid,TenantId,ObjectId,FieldNum,stringValue)"
						+ " VALUES(" + dataGuid + "," + TenantId + ","
						+ ObjectId + "," + columnCnt + ","
						+ valueArray[columnCnt] + ")";
				//stmt.executeUpdate(unique_sql);
				System.out.println("INSERT UNIQUE SQL: " + unique_sql);
			} else if (indexType.equals("2")) {
				// 2. check this field's IndexType
				TobjectId = ObjectId;
				// 逆推回去找SourceObjectId
				// 有create所以不能寫死
				// indexType==2代表目前這筆資料為target
				// 找出fieldName為目前的欄位以及indexType==1的SourceObjectId
				String sobjectId_sql = "SELECT ObjectId FROM fields "
						+ "WHERE TenantId=" + TenantId
						+ " AND FieldName='" + columnTmp + "'"
						+ " AND IndexType=" + 1;
//				rs = stmt.executeQuery(sobjectId_sql);
//				if (rs.next())
//					SobjectId = rs.getInt("ObjectId");
				// 找SourceFieldId, SourceFieldNum
//				TfieldId = fieldNameTrans(conn, TenantId, ObjectId,
//						columnTmp);
				TfieldNum = columnCnt;
//				SfieldId = fieldNameTrans(conn, TenantId, SobjectId,
//						columnTmp);
				String sfieldNum_sql = "SELECT FieldNum FROM fields "
						+ "WHERE TenantId=" + TenantId
						+ " AND FieldName='" + columnTmp + "'"
						+ " AND ObjectId=" + SobjectId;
//				rs = stmt.executeQuery(sfieldNum_sql);
//				if (rs.next()) {
//					SfieldNum = rs.getInt("FieldNum");
//				}
				association_sql = "INSERT INTO Associations"
						+ "(AssociationId,TenantId,SourceObjectId,TargetObjectId,"
						+ "SourceFieldId,TargetFieldId,SourceFieldNum,TargetFieldNUm)"
						+ " VALUES(" + associationId + "," + TenantId
						+ "," + SobjectId + "," + TobjectId + ","
						+ SfieldId + "," + TfieldId + "," + SfieldNum
						+ "," + TfieldNum + ")";
				//stmt.executeUpdate(association_sql);
				System.out.println("INSERT ASSOCIATIONS SQL: " + association_sql);
				// 3. SourceDataGuid 先抓出目前field value
				// 再加上Value+SfieldNum == value找出guid
				String valueTmp = valueArray[TfieldNum];
				String sdataguid_sql = "SELECT DataGuid FROM data "
						+ "WHERE TenantId=" + TenantId
						+ " AND ObjectId=" + SobjectId + " AND Value"
						+ SfieldNum + "='" + valueTmp + "'"; 
//				rs = stmt.executeQuery(sdataguid_sql);
//				if (rs.next())
//					SdataGuid = rs.getString("DataGuid");
				relation_sql = "INSERT INTO Relationships"
						+ "(AssociationId,SourceDataGuid,TargetDataGuid,TenantId,"
						+ "SourceObjectId,TargetObjectId)" + " VALUES("
						+ associationId + ",'" + SdataGuid + "','"
						+ dataGuid + "'," + TenantId + "," + SobjectId
						+ "," + TobjectId + ")";
				//stmt.executeUpdate(relation_sql);
				System.out.println("INSERT RELATIONSHIPS SQL: " + relation_sql);
				associationId++;
			}
			columnCnt++; 
		}
	}
}