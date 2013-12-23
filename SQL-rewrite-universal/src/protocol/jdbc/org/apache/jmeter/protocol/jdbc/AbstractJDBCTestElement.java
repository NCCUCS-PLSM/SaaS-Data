/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.jdbc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * A base class for all JDBC test elements handling the basics of a SQL request.
 * 
 */
public abstract class AbstractJDBCTestElement extends AbstractTestElement
		implements TestStateListener {
	private static final long serialVersionUID = 235L;

	private static final Logger log = LoggingManager.getLoggerForClass();

	private static final String COMMA = ","; // $NON-NLS-1$
	private static final char COMMA_CHAR = ',';

	private static final String UNDERSCORE = "_"; // $NON-NLS-1$

	// String used to indicate a null value
	private static final String NULL_MARKER = JMeterUtils.getPropDefault(
			"jdbcsampler.nullmarker", "]NULL["); // $NON-NLS-1$

	// For callableStatement
	private static final String INOUT = "INOUT"; // $NON-NLS-1$
	private static final String OUT = "OUT"; // $NON-NLS-1$

	// TODO - should the encoding be configurable?
	protected static final String ENCODING = "UTF-8"; // $NON-NLS-1$

	// key: name (lowercase) from java.sql.Types; entry: corresponding int value
	private static final Map<String, Integer> mapJdbcNameToInt;
	// read-only after class init

	static {
		// based on e291. Getting the Name of a JDBC Type from javaalmanac.com
		// http://javaalmanac.com/egs/java.sql/JdbcInt2Str.html
		mapJdbcNameToInt = new HashMap<String, Integer>();

		// Get all fields in java.sql.Types and store the corresponding int
		// values
		Field[] fields = java.sql.Types.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			try {
				String name = fields[i].getName();
				Integer value = (Integer) fields[i].get(null);
				mapJdbcNameToInt.put(
						name.toLowerCase(java.util.Locale.ENGLISH), value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e); // should not happen
			}
		}
	}

	// Query types (used to communicate with GUI)
	// N.B. These must not be changed, as they are used in the JMX files
	// $NON-NLS-1$(no national language support) 代表此字串不能被翻譯成其他語言
	static final String SELECT = "Select Statement"; // $NON-NLS-1$
	// TODO INSERT
	static final String INSERT = "Insert Statement"; // $NON-NLS-1$
	// TODO DELETE
	static final String DELETE = "Delete Statement"; // $NON-NLS-1$
	// TODO UPDATE 需要被修改
	static final String UPDATE = "Update Statement"; // $NON-NLS-1$
	// TODO CREATE
	static final String CREATE = "Create Statement"; // $NON-NLS-1$

	static final String CALLABLE = "Callable Statement"; // $NON-NLS-1$
	static final String PREPARED_SELECT = "Prepared Select Statement"; // $NON-NLS-1$
	static final String PREPARED_UPDATE = "Prepared Update Statement"; // $NON-NLS-1$
	static final String COMMIT = "Commit"; // $NON-NLS-1$
	static final String ROLLBACK = "Rollback"; // $NON-NLS-1$
	static final String AUTOCOMMIT_FALSE = "AutoCommit(false)"; // $NON-NLS-1$
	static final String AUTOCOMMIT_TRUE = "AutoCommit(true)"; // $NON-NLS-1$

	static final int COLUMN_LENGTH = 20;
	static String valuesString;
	static {
		valuesString = "Value0";
		for (int i = 1; i <= COLUMN_LENGTH; i++) {
			valuesString += ",Value" + i;
		}
	}

	private String query = ""; // $NON-NLS-1$

	private String dataSource = ""; // $NON-NLS-1$

	private String queryType = SELECT;
	private String queryArguments = ""; // $NON-NLS-1$
	private String queryArgumentsTypes = ""; // $NON-NLS-1$
	private String variableNames = ""; // $NON-NLS-1$
	private String resultVariable = "";

	/************************************************************
	 * 轉換SQL語句需要的meta-data
	 ************************************************************/
	// TODO 模擬每個tenant輸入sql statement之後轉換的效能
	// 所以應該固定一個租戶，
	// 之後的租戶數量增大影響的只是需要花費在越大的表格中找到此一租戶的時間
	private int TenantId = 1;
	private int ObjectId;
	private String dataGuid;
	private String tableName;
	private List<?> columnList;
	private String itemList;
	private String[] valueArray;
	private String where;
	private String[] defaultValues;

	// config table policy file
	private final String PfilePath = "/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TablePolicy.txt";
	private final String FfilePath = "/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/Foreignkey.txt";
	/**
	 * Cache of PreparedStatements stored in a per-connection basis. Each entry
	 * of this cache is another Map mapping the statement string to the actual
	 * PreparedStatement. At one time a Connection is only held by one thread
	 */
	private static final Map<Connection, Map<String, PreparedStatement>> perConnCache = new ConcurrentHashMap<Connection, Map<String, PreparedStatement>>();

	/**
	 * Creates a JDBCSampler.
	 */
	protected AbstractJDBCTestElement() {
	}

	/**
	 * Execute the test element.
	 * 
	 * @param conn
	 *            a {@link SampleResult} in case the test should sample;
	 *            <code>null</code> if only execution is requested
	 * @throws UnsupportedOperationException
	 *             if the user provided incorrect query type
	 * @throws JSQLParserException
	 */
	// TODO 多租戶語句轉換機制在此實作
	protected byte[] execute(Connection conn) throws SQLException,
			UnsupportedEncodingException, IOException,
			UnsupportedOperationException, JSQLParserException {
		// log.warn("executing jdbc");
		Statement stmt = null;

		try {
			// Based on query return value, get results
			String _queryType = getQueryType();
			String _sql = getQuery();
			// log.info(_sql);
			if (SELECT.equals(_queryType)) {
				stmt = conn.createStatement();
				ResultSet rs = null;
				try {
					rs = stmt.executeQuery(getQuery());
					return getStringFromResultSet(rs).getBytes(ENCODING);
				} finally {
					close(rs);
				}
			} else if (INSERT.equals(_queryType)) {
				// TODO INSERT statement rewriting
//				TenantId = 1; 
//				TenantId = getTenantId(); 
				
				stmt = conn.createStatement();
				log.info("LOGICAL SQL: " + _sql);
				// Step 1. get Logical sql statement
				CCJSqlParserManager pm = new CCJSqlParserManager();
				Insert jsqlinsert = (Insert) pm.parse(new StringReader(_sql));
				// Step 2. transform logical sql to physical sql
				// Step 2a. jsqlparser get each field
				tableName = jsqlinsert.getTable().toString();
				columnList = jsqlinsert.getColumns();
				itemList = jsqlinsert.getItemsList().toString();
				itemList = itemList.replace("(", "").replace(")", "");
				valueArray = itemList.split(",");
				itemList = itemList.substring(itemList.indexOf(",")+2);
				Iterator<?> columnItr = columnList.iterator();
				if (columnItr.hasNext()) 
					columnItr.next(); 
				TenantId = Integer.valueOf(valueArray[0]);
				
				int dupliCnt = 0;
				do { // 檢查是否有重複的DataGuid
						// TODO 不知道是否真的有必要？
					dataGuid = dataGuidGenerate();
					String testDuplic_sql = "SELECT COUNT(DataGuid) FROM Data"
							+ " WHERE DataGuid='" + dataGuid + "'";
					ResultSet resultSet = stmt.executeQuery(testDuplic_sql);
					if (resultSet.next())
						dupliCnt = resultSet.getInt("COUNT(DataGuid)");
				} while (dupliCnt > 0);
				ObjectId = objectNameTrans(conn, TenantId, tableName);
				// Step 2b. Insert data into Data table
				String data_sql = "INSERT INTO Data"
						+ "(DataGuid,TenantId,ObjectId,Name,IsDeleted,"
						+ valuesString + ") VALUES('" + dataGuid + "','"
						+ TenantId + "','" + ObjectId + "','" + tableName
						+ "','" + 0 + "'," + itemList;
				for (int i = 0; i < COLUMN_LENGTH - valueArray.length + 2; i++) {
					data_sql += ",null";
				}
				data_sql += ")";
				log.info("INSERT DATA SQL: " + data_sql);
				stmt.executeUpdate(data_sql);
				// Step 2c-1. Insert data into Uniquefields table
				String columnTmp = "";
				int columnCnt = 0; // current fieldNum
				String indexType;
				String unique_sql = "";
				// Step 2d-1. Insert data into Associations table
				// TODO IndexType==2 代表為 foreign key
				// TODO 這樣測資程式也需要修改...
				// TODO AssociationId有bug...
				int SfieldNum = 0, TfieldNum = 0;
				int SfieldId = 0, TfieldId = 0;
				int SobjectId = 0, TobjectId = 0;
				String SdataGuid = "";
				String association_sql = "";
				String relation_sql = "";
				int associationId = 0;
				
				while (columnItr.hasNext()) {
					// 1. check this field's IndexType
					// IndexType==1
					indexType = "";
					columnTmp = columnItr.next().toString().trim();
					String check_type_sql = "SELECT IndexType FROM Fields "
							+ "WHERE TenantId=" + TenantId + " AND ObjectId="
							+ ObjectId + " AND FieldName='" + columnTmp + "'";
					ResultSet rs = stmt.executeQuery(check_type_sql);
					if (rs.next())
						indexType = rs.getString("IndexType");
					if (indexType.equals("1")) {
						unique_sql = "INSERT INTO UniqueFields"
								+ "(DataGuid,TenantId,ObjectId,FieldNum,stringValue)"
								+ " VALUES('" + dataGuid + "'," + TenantId
								+ "," + ObjectId + "," + columnCnt + ","
								+ valueArray[columnCnt+1] + ")";
						stmt.executeUpdate(unique_sql);
						log.info("INSERT UNIQUE SQL: " + unique_sql);
					} else if (indexType.equals("2")) {
						// 2. check this field's IndexType
						TobjectId = ObjectId;
						// 逆推回去找SourceObjectId
						// 有create所以不能寫死
						// indexType==2代表目前這筆資料為target
						// 找出fieldName為目前的欄位以及indexType==1的SourceObjectId
						String sobjectId_sql = "SELECT ObjectId FROM Fields "
								+ "WHERE TenantId=" + TenantId
								+ " AND FieldName='" + columnTmp + "'"
								+ " AND IndexType=" + 1;
						rs = stmt.executeQuery(sobjectId_sql);
						if (rs.next())
							SobjectId = rs.getInt("ObjectId");
						// 找SourceFieldId, SourceFieldNum
						TfieldNum = columnCnt;
						SfieldNum = fieldNameTrans(conn, TenantId, SobjectId,
								columnTmp);
						String getTfieldId_sql = "SELECT FieldId FROM Fields "
								+ "WHERE TenantId=" + TenantId
								+ " AND FieldName='" + columnTmp + "'"
								+ " AND ObjectId=" + TobjectId;
						rs = stmt.executeQuery(getTfieldId_sql);
						if (rs.next())
							TfieldId = rs.getInt("FieldId");
						String getSfieldId_sql = "SELECT FieldId FROM Fields "
								+ "WHERE TenantId=" + TenantId
								+ " AND FieldName='" + columnTmp + "'"
								+ " AND ObjectId=" + SobjectId;
						rs = stmt.executeQuery(getSfieldId_sql);
						if (rs.next())
							SfieldId = rs.getInt("FieldId");
						// TODO Concurrent Tenant每個取到的associationId都會一樣
//						String countAssociation_sql = "SELECT COUNT(AssociationId) FROM Associations"; 
//						rs = stmt.executeQuery(countAssociation_sql);
//						if (rs.next()) 
//							associationId = rs.getInt("COUNT(AssociationId)");
						association_sql = "SELECT AssociationId FROM Associations "
								+ "WHERE TenantId=" + TenantId 
								+ " AND SourceObjectId=" + SobjectId
								+ " AND TargetObjectId=" + TobjectId 
								+ " AND SourceFieldId=" + SfieldId 
								+ " AND TargetFieldId=" + TfieldId; 
						rs = stmt.executeQuery(association_sql);
						if (rs.next()) 
							associationId = rs.getInt("AssociationId");
						// 3. SourceDataGuid 先抓出目前field value
						// 再加上Value+SfieldNum == value找出guid
						String valueTmp = valueArray[TfieldNum+1];
						String sdataguid_sql = "SELECT DataGuid FROM Data "
								+ "WHERE TenantId=" + TenantId
								+ " AND ObjectId=" + SobjectId + " AND Value"
								+ SfieldNum + "=" + valueTmp;
						rs = stmt.executeQuery(sdataguid_sql);
						if (rs.next())
							SdataGuid = rs.getString("DataGuid");
						relation_sql = "INSERT INTO Relationships"
								+ "(AssociationId,SourceDataGuid,TargetDataGuid,TenantId,"
								+ "SourceObjectId,TargetObjectId)" + " VALUES("
								+ associationId + ",'" + SdataGuid + "','"
								+ dataGuid + "'," + TenantId + "," + SobjectId
								+ "," + TobjectId + ")";
						log.info("INSERT RELATIONSHIPS SQL: " + relation_sql);
						stmt.executeUpdate(relation_sql);
					}
					columnCnt++;
				}
				return "INSERT DONE.".getBytes(ENCODING);
			} else if (DELETE.equals(_queryType)) {
				// TODO DELETE statement rewriting
				// 先以文字檔txt記錄 policy以及default value
				// 行有餘力再加入 jackson library
				// CONSTRAINT 是影響的是foreign key，發現source data做出變動，
				// target data也根據policy做出相對應的變動
//				TenantId = 1; 
//				TenantId = getTenantId(); 
				
				stmt = conn.createStatement();
				String policy = "CASCADE";
				ResultSet rs;
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
				defaultValues = new String[100];
				int cnt = 0;
				if (config.size() > 1)
					policy = (String) config.get(1);
				if (policy.equals("SET DEFAULT")) {
					for (int i = 3; i < config.size(); i++) {
						defaultValues[cnt++] = (String) config.get(i);
					}
				}
				// Step 2b. transform where condition
				String[] sarr = where.split(" ");
				TenantId = Integer.valueOf(sarr[2]); 
				where = where.substring(17); 
				ObjectId = objectNameTrans(conn, TenantId, tableName);
				for (int i = 4; i < sarr.length; i += 4) { // 抓欄位名稱
					String fieldNum_sql = "SELECT FieldNum FROM Fields "
							+ "WHERE TenantId=" + TenantId + " AND ObjectId="
							+ ObjectId + " AND FieldName='" + sarr[i] + "'";
					rs = stmt.executeQuery(fieldNum_sql);
					if (rs.next())
						where = where.replace(sarr[i],
								"Value" + (rs.getInt("FieldNum") - 1));
				}
				// Step 2c. delete data from data table
				// 刪除的資料當作是Source data
				// TODO 刪除的資料當作是Target data先不管在Relationships, Associations的資料
				// 等到他的Source data被刪後會一併作處理
				String getGuid_sql = "SELECT DataGuid FROM Data "
						+ "WHERE TenantId=" + TenantId + " AND ObjectId="
						+ ObjectId + " AND " + where;
				rs = stmt.executeQuery(getGuid_sql);
				while (rs.next())
					SdataGuidList.add(rs.getString("DataGuid"));
				String data_sql = "DELETE FROM Data" + " WHERE TenantId="
						+ TenantId + " AND ObjectId=" + ObjectId + " AND "
						+ where;
				stmt.executeUpdate(data_sql);
				log.info("DELETE DATA SQL: " + data_sql);
				for (int i = 0; i < SdataGuidList.size(); i++) {
					String getTGuid_sql = "SELECT TargetDataGuid FROM Relationships "
							+ "WHERE SourceDataGuid='"
							+ SdataGuidList.get(i)
							+ "'";
					rs = stmt.executeQuery(getTGuid_sql);
					while (rs.next()) {
						TdataGuidList.add(rs.getString("TargetDataGuid"));
					}
				}
				// if PKs exists
				// Step 2d-1. delete data from Uniquefields table
				for (int i = 0; i < SdataGuidList.size(); i++) {
					String unique_sql = "DELETE FROM UniqueFields "
							+ "WHERE DataGuid='" + SdataGuidList.get(i) + "'";
					stmt.executeUpdate(unique_sql);
					log.info("DELETE UNIQUE SQL: " + unique_sql);
				}
				if (TdataGuidList.size() > 0) {
					// if IndexType==1 : Primary keys exists
					// Step 2d-2. delete data from Relatoinships table
					for (int i = 0; i < SdataGuidList.size(); i++) {
						String relation_sql = "DELETE FROM Relationships "
								+ "WHERE SourceDataGuid='"
								+ SdataGuidList.get(i) + "'";
						stmt.executeUpdate(relation_sql);
						log.info("DELETE RELATIONSHIPS SQL: " + relation_sql);
					}
					if (policy.equals("CASCADE")) { // 1. CASCADE policy
						// (optional)如果刪除的是parent data
						// Step 2d-4. delete child data from data table
						for (int i = 0; i < TdataGuidList.size(); i++) {
							String childdata_sql = "DELETE FROM Data "
									+ "WHERE DataGuid='" + TdataGuidList.get(i)
									+ "'";
							stmt.executeUpdate(childdata_sql);
							log.info("CASCADE DELETE CHILD DATA SQL: "
									+ childdata_sql);
						}
						// if IndexType==2 : Foreign key
						// 不用管，只有當source被刪除，才要連帶處理所有的target data
					} else if (policy.equals("SET NULL")) { // 2. SET NULL
															// policy
						// (optional)如果刪除的是parent data
						// Step 2d-3. update child data from data table to null
						for (int i = 0; i < TdataGuidList.size(); i++) {
							String childdata_sql = "UPDATE Data " + "SET Value"
									+ (TfieldNumList.get(i) - 1) + "=NULL"
									+ "WHERE DataGuid='" + TdataGuidList.get(i)
									+ "'";
							stmt.executeUpdate(childdata_sql);
							log.info("SET NULL DELETE CHILD DATA SQL: "
									+ childdata_sql);
						}
					} else if (policy.equals("SET DEFAULT")) { // 3. SET DEFAULT
																// policy
						// (optional)如果刪除的是parent data
						// Step 2d-3. update child data from data table to
						// default
						// value
						int vcnt = 0;
						for (int i = 0; i < TdataGuidList.size(); i++) {
							if (i > 0
									&& TfieldNumList.get(i) != TfieldNumList
											.get(i - 1))
								vcnt++;
							String childdata_sql = "UPDATE Data " + "SET Value"
									+ (TfieldNumList.get(i) - 1) + "='"
									+ defaultValues[vcnt]
									+ "' WHERE DataGuid='"
									+ TdataGuidList.get(i) + "'";
							stmt.executeUpdate(childdata_sql);
							log.info("SET DEFAULT DELETE CHILD DATA SQL: "
									+ childdata_sql);
						}
					}
				}
				return "Delete DONE.".getBytes(ENCODING);
			} else if (UPDATE.equals(_queryType)) {
				// 先以文字檔txt記錄 policy以及default value
				// 行有餘力再加入 jackson library
//				TenantId = 1; 
//				TenantId = getTenantId(); 
				
				stmt = conn.createStatement();
				String policy = "CASCADE";
				ResultSet rs;
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
				tableName = jsqlupdate.getTable().toString();
				
				columns = jsqlupdate.getColumns();
				expressions = jsqlupdate.getExpressions();
				Iterator<?> columnItr = columns.iterator();
				Iterator<?> expressionItr = expressions.iterator();
				if (jsqlupdate.getWhere() != null)
					where = jsqlupdate.getWhere().toString();
				// get policy config
				List<?> config = loadFileP(tableName, PfilePath);
				defaultValues = new String[100];
				int cnt = 0;
				if (config.size() > 1)
					policy = (String) config.get(2);
				if (policy.equals("SET DEFAULT")) {
					for (int i = 3; i < config.size(); i++) {
						defaultValues[cnt++] = (String) config.get(i);
					}
				}
				// Step 2b. transform where condition
				String[] sarr = where.split(" ");
				TenantId = Integer.valueOf(sarr[2]);
				where = where.substring(17);
				ObjectId = objectNameTrans(conn, TenantId, tableName);
				for (int i = 4; i < sarr.length; i += 4) {
					String fieldNum_sql = "SELECT FieldNum FROM Fields "
							+ "WHERE TenantId=" + TenantId + " AND ObjectId="
							+ ObjectId + " AND FieldName='" + sarr[i] + "'";
					rs = stmt.executeQuery(fieldNum_sql);
					if (rs.next())
						where = where.replace(sarr[i],
								"Value" + (rs.getInt("FieldNum") - 1));
				}
				// Step 2c. update data from data table
				String getGuid_sql = "SELECT DataGuid FROM Data "
						+ "WHERE TenantId=" + TenantId + " AND ObjectId="
						+ ObjectId + " AND " + where;
				rs = stmt.executeQuery(getGuid_sql);
				while (rs.next())
					SdataGuidList.add(rs.getString("DataGuid"));
				String exprTmp = "";
				String fieldNameTmp = "";
				int fieldNumTmp = 0;
				String valueTmp = "";
				while (columnItr.hasNext() && expressionItr.hasNext()) {
					fieldNameTmp = columnItr.next().toString().trim();
					fieldNumTmp = fieldNameTrans(conn, TenantId, ObjectId,
							fieldNameTmp);
					valueTmp = expressionItr.next().toString().trim();
					exprTmp += "Value" + (fieldNumTmp - 1) + "=" + valueTmp;
					// TODO PK fieldNUm list
					String getindexType_sql = "SELECT IndexType FROM Fields "
							+ "WHERE TenantId=" + TenantId + " AND ObjectId="
							+ ObjectId + " AND FieldName='" + fieldNameTmp
							+ "'";
					rs = stmt.executeQuery(getindexType_sql);
					if (rs.next() && rs.getString("IndexType").equals("1"))
						pkMap.put(fieldNumTmp, valueTmp);
					if (columnItr.hasNext())
						exprTmp += ",";
				}
				String data_sql = "UPDATE Data SET " + exprTmp
						+ " WHERE TenantId=" + TenantId + " AND ObjectId="
						+ ObjectId + " AND " + where;
				stmt.executeUpdate(data_sql);
				log.info("UPDATE DATA SQL: " + data_sql);
				if (pkMap.size() > 0) { // 如果更新欄位中包含pk，則child data才要做更新
					// TODO 不知道是否有更新到有關連性的欄位 check fields table
					log.info("PK must be updated");
					for (int i = 0; i < SdataGuidList.size(); i++) {
						String getTGuid_sql = "SELECT AssociationId,TargetDataGuid FROM Relationships "
								+ "WHERE SourceDataGuid='"
								+ SdataGuidList.get(i) + "'";
						rs = stmt.executeQuery(getTGuid_sql);
						while (rs.next()) {
							associationIdList.add(rs.getInt("AssociationId"));
							TdataGuidList.add(rs.getString("TargetDataGuid"));
						}
					}
					// get TargetFieldNum list
					for (int i = 0; i < associationIdList.size(); i++) {
						String getfieldNum_sql = "SELECT TargetFieldNum FROM Associations "
								+ "WHERE AssociationId="
								+ associationIdList.get(i);
						rs = stmt.executeQuery(getfieldNum_sql);
						if (rs.next()) 
							TfieldNumList.add(rs.getInt("TargetFieldNum"));
					}
					// TODO if columns exists PK
					// Step 2d-1. udpate data from UniqueFields table
					// 與delete不同要注意唯一性的問題，若存在則reject
					// fieldNum是為了區分多個primary key的情況
					Set<Integer> set = pkMap.keySet();
					for (int i = 0; i < SdataGuidList.size(); i++) {
						Iterator<Integer> pkset = set.iterator();
						while (pkset.hasNext()) {
							int key = pkset.next();
							String unique_sql = "UPDATE UniqueFields SET "
									+ "stringValue=" + pkMap.get(key)
									+ " WHERE DataGuid='"
									+ SdataGuidList.get(i) + "'"
									+ " AND FieldNum=" + key;
							stmt.executeUpdate(unique_sql);
							log.info("UDPATE UNIQUE SQL: " + unique_sql);
						}
					}
					// TODO update parent's data then child's data must be
					// updated
					if (policy.equals("CASCADE")) { // 1. CASCADE policy
						// 每一個relation record只會記錄一個欄位的關聯性資料
						// 所以每一個list item對一個pkMap的值即可 
						Iterator<Integer> Tset = set.iterator();
						for (int i = 0; i < TdataGuidList.size(); i++) {
							String childdata_sql = "UPDATE Data SET " + "Value"
									+ (TfieldNumList.get(i) - 1) + "="
									+ pkMap.get(Tset.next())
									+ "WHERE DataGuid='" + TdataGuidList.get(i)
									+ "'";
							stmt.executeUpdate(childdata_sql);
							log.info("CASCADE UPDATE CHILD DATA SQL: "
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
							String childdata_sql = "UPDATE Data SET " + "Value"
									+ (TfieldNumList.get(i) - 1) + "=NULL"
									+ "WHERE DataGuid='" + TdataGuidList.get(i)
									+ "'";
							stmt.executeUpdate(childdata_sql);
							log.info("SET NULL UPDATE CHILD DATA SQL: "
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
							String childdata_sql = "UPDATE Data " + "SET Value"
									+ (TfieldNumList.get(i) - 1) + "='"
									+ defaultValues[vcnt]
									+ "' WHERE DataGuid='"
									+ TdataGuidList.get(i) + "'";
							stmt.executeUpdate(childdata_sql);
							log.info("SET DEFAULT UPDATE CHILD DATA SQL: "
									+ childdata_sql);
						}
					}
				}
				return "Update DONE.".getBytes(ENCODING);
			} else if (CREATE.equals(_queryType)) {
				String tenantStr = _sql.substring(0, _sql.indexOf("CREATE"));
				String[] sarrtmp = tenantStr.split(" ");
				TenantId = Integer.valueOf(sarrtmp[2].trim()); 
//				TenantId = getTenantId(); 
				_sql = _sql.substring(_sql.indexOf("CREATE"));
				// TODO CREATE statement rewriting
				stmt = conn.createStatement();
				ResultSet rs;
				List<?> columnDefinitions;
				List<?> indexes;
				ArrayList<String> pkList = new ArrayList<String>();
				ArrayList<String> fkList;
				int objectCnt = 0, fieldCnt = 0;
				int indexType = 0;
				// Step 1. get logical sql statement
				CCJSqlParserManager pm = new CCJSqlParserManager();
				CreateTable jsqlcreateTable = (CreateTable) pm
						.parse(new StringReader(_sql));
				// TODO JSqlParser目前僅支援PRIMARY KEY，其他CONSTRAINT都還沒有
				// Step 2. transform logical sql to physical sql
				// Step 2a. jsqlparser get each field
				tableName = jsqlcreateTable.getTable().toString();
				columnDefinitions = jsqlcreateTable.getColumnDefinitions();
				indexes = jsqlcreateTable.getIndexes();
				Iterator<?> columnItr = columnDefinitions.iterator();
				Iterator<?> indexItr = indexes.iterator();
				int isDuplic = 0;
				// Step 2b. insert Tenant's object data into objects table
//				String checkDupli_sql = "SELECT COUNT(ObjectId) FROM Objects WHERE ObjectName='"
//						+ tableName + "'";
//				rs = stmt.executeQuery(checkDupli_sql);
//				if (rs.next())
//					isDuplic = rs.getInt("COUNT(ObjectId)");
//				if (isDuplic <= 0) {
//					String getobjectCnt_sql = "SELECT COUNT(ObjectId) FROM Objects";
//					rs = stmt.executeQuery(getobjectCnt_sql);
//					if (rs.next())
//						objectCnt = rs.getInt("COUNT(ObjectId)");
					String object_sql = "INSERT INTO Objects"
							+ "(TenantId,ObjectName)" + " VALUES("
							+ TenantId + ",'" + tableName
							+ "')";
					stmt.executeUpdate(object_sql);
					log.info("CREATE TABLE INSERT OBJECTS SQL: " + object_sql);
					// Step 2c. insert Tenant's field data into fields table
					// PRIMARY KEY:IndexType==1
					// FOREIGN KEY:IndexType==2 TODO 但 JSqlParser不支援
					while (indexItr.hasNext()) {
						String tmp = indexItr.next().toString();
						String[] lineTmp = tmp.split(" ");
						tmp = lineTmp[2].replace("(", "").replace(")", "");
						pkList.add(tmp);
						for (int i = 0; i < 4; i++)
							if (indexItr.hasNext())
								indexItr.next();
					}
					// 要先手動在Foreignkey.txt設定foreign key位置
					fkList = loadFileF(tableName, FfilePath);
//					String getfieldCnt_sql = "SELECT COUNT(FieldId) FROM Fields";
//					rs = stmt.executeQuery(getfieldCnt_sql);
//					if (rs.next())
//						fieldCnt = rs.getInt("COUNT(FieldId)");
					int numTmp = 0;
					while (columnItr.hasNext()) {
						indexType = 0;
						String tmp = columnItr.next().toString();
						String[] lineTmp = tmp.split(" ");
						// check pk
						for (int i = 0; i < pkList.size(); i++) {
							if (lineTmp[0].equals(pkList.get(i)))
								indexType = 1;
						}
						// check fk
						for (int i = 0; i < fkList.size(); i++) {
							if (lineTmp[0].equals(fkList.get(i)))
								indexType = 2;
						}
						String field_sql = "INSERT INTO Fields"
								+ "(TenantId,ObjectId,FieldName,"
								+ "FieldType,FieldNum,IndexType)" + " VALUES("
								+ TenantId + ","
								+ objectNameTrans(conn, TenantId, tableName)
								+ ",'" + lineTmp[0] + "','string'," + numTmp
								+ "," + indexType + ")";
						stmt.executeUpdate(field_sql);
						log.info("CREATE TABLE INSERT FIELDS SQL: " + field_sql);
						numTmp++;
					}
//				}
				return "Create Table DONE.".getBytes(ENCODING);
			} else if (CALLABLE.equals(_queryType)) {
				CallableStatement cstmt = getCallableStatement(conn);
				int out[] = setArguments(cstmt);
				// A CallableStatement can return more than 1 ResultSets
				// plus a number of update counts.
				boolean hasResultSet = cstmt.execute();
				String sb = resultSetsToString(cstmt, hasResultSet, out);
				return sb.getBytes(ENCODING);
			} else if (PREPARED_SELECT.equals(_queryType)) {
				PreparedStatement pstmt = getPreparedStatement(conn);
				setArguments(pstmt);
				ResultSet rs = null;
				try {
					rs = pstmt.executeQuery();
					return getStringFromResultSet(rs).getBytes(ENCODING);
				} finally {
					close(rs);
				}
			} else if (PREPARED_UPDATE.equals(_queryType)) {
				PreparedStatement pstmt = getPreparedStatement(conn);
				setArguments(pstmt);
				pstmt.executeUpdate();
				String sb = resultSetsToString(pstmt, false, null);
				return sb.getBytes(ENCODING);
			} else if (ROLLBACK.equals(_queryType)) {
				conn.rollback();
				return ROLLBACK.getBytes(ENCODING);
			} else if (COMMIT.equals(_queryType)) {
				conn.commit();
				return COMMIT.getBytes(ENCODING);
			} else if (AUTOCOMMIT_FALSE.equals(_queryType)) {
				conn.setAutoCommit(false);
				return AUTOCOMMIT_FALSE.getBytes(ENCODING);
			} else if (AUTOCOMMIT_TRUE.equals(_queryType)) {
				conn.setAutoCommit(true);
				return AUTOCOMMIT_TRUE.getBytes(ENCODING);
			} else { // User provided incorrect query type
				throw new UnsupportedOperationException(
						"Unexpected query type: " + _queryType);
			}
		} finally {
			close(stmt);
		}
	}

	// TODO Data Guid generate
	// 會步會有重複?
	private String dataGuidGenerate() {
		String dataGuid = "";
		String lexicon = "abcedfghijkmnopqrstuvwxyz0123456789";
		Random r = new Random();
		for (int i = 0; i < 36; i++) {
			if (i == 8 || i == 12 || i == 16 || i == 20) {
				dataGuid += "-";
			} else {
				dataGuid += lexicon.charAt(r.nextInt(lexicon.length()));
			}
		}

		return dataGuid;
	}

	// TODO Object Name Transformation - return int
	private int objectNameTrans(Connection conn, int tenantId, String objName)
			throws SQLException {
		Statement stmt = conn.createStatement();
		String object_sql = "SELECT ObjectId FROM Objects "
				+ "WHERE ObjectName='" + objName + "' AND TenantId=" + tenantId;
		ResultSet rs = stmt.executeQuery(object_sql);
		rs.next();
		return rs.getInt("ObjectId");
	}

	// TODO Field Name Transformation - return String
	private int fieldNameTrans(Connection conn, int tenantId, int objectId,
			String fieldName) throws SQLException {
		Statement stmt = conn.createStatement();
		String field_sql = "SELECT FieldNum FROM Fields " + "WHERE TenantId="
				+ tenantId + " AND ObjectId=" + objectId + " AND FieldName='"
				+ fieldName + "'";
		ResultSet rs = stmt.executeQuery(field_sql);
		rs.next();
		return rs.getInt("FieldNum");
	}

	// TODO Input policy file
	private List<?> loadFileP(String objectName, String filePath) {
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

	// TODO Input table foreign key file
	private ArrayList<String> loadFileF(String objectName, String filePath) {
		String tmp;
		ArrayList<String> line = new ArrayList<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(filePath));
			while ((tmp = in.readLine()) != null) {
				String[] sarrtmp = tmp.split(":");
				if (sarrtmp[0].equals(objectName)) {
					// TODO split sarrtmp[1] to multiple fields
					String[] sarr = sarrtmp[1].split(",");
					for (int i = 0; i < sarr.length; i++)
						line.add(sarr[i]);
				}
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}

	// TODO Write log file
	// d
	private String resultSetsToString(PreparedStatement pstmt, boolean result,
			int[] out) throws SQLException, UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		int updateCount = 0;
		if (!result) {
			updateCount = pstmt.getUpdateCount();
		}
		do {
			if (result) {
				ResultSet rs = null;
				try {
					rs = pstmt.getResultSet();
					sb.append(getStringFromResultSet(rs)).append("\n"); // $NON-NLS-1$
				} finally {
					close(rs);
				}
			} else {
				sb.append(updateCount).append(" updates.\n");
			}
			result = pstmt.getMoreResults();
			if (!result) {
				updateCount = pstmt.getUpdateCount();
			}
		} while (result || (updateCount != -1));
		if (out != null && pstmt instanceof CallableStatement) {
			ArrayList<Object> outputValues = new ArrayList<Object>();
			CallableStatement cs = (CallableStatement) pstmt;
			sb.append("Output variables by position:\n");
			for (int i = 0; i < out.length; i++) {
				if (out[i] != java.sql.Types.NULL) {
					Object o = cs.getObject(i + 1);
					outputValues.add(o);
					sb.append("[");
					sb.append(i + 1);
					sb.append("] ");
					sb.append(o);
					sb.append("\n");
				}
			}
			String varnames[] = getVariableNames().split(COMMA);
			if (varnames.length > 0) {
				JMeterVariables jmvars = getThreadContext().getVariables();
				for (int i = 0; i < varnames.length && i < outputValues.size(); i++) {
					String name = varnames[i].trim();
					if (name.length() > 0) { // Save the value in the variable
												// if present
						Object o = outputValues.get(i);
						jmvars.put(name, o == null ? null : o.toString());
					}
				}
			}
		}
		return sb.toString();
	}

	private int getTenantId() throws NumberFormatException, IOException { 
		// TODO runtime改變TenantId的暫時解法，不知道Tenant一多會不會影響效能
		// Runtime input TenantId
		FileReader fr = new FileReader("/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TenantCnt.txt");
		BufferedReader in = new BufferedReader(fr);
		int tmp = Integer.parseInt(in.readLine());
		in.close();
		fr.close();
		// Runtime output TenantId
		FileWriter fout = new FileWriter("/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/TenantCnt.txt");
		BufferedWriter out = new BufferedWriter(fout);
		out.write(Integer.toString(tmp+1));
		out.flush();
		out.close();
		fout.close();
		
		return tmp; 
	}
	
	private int getAssociationId() throws NumberFormatException, IOException { 
		// ****
		// 改從 AssociationCnt.txt 存取associationId 
		// ****
		// Runtime input AssociationId
		FileReader fr = new FileReader("/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/AssociationCnt.txt");
		BufferedReader in = new BufferedReader(fr);
		int tmp = Integer.parseInt(in.readLine()); 
		// Runtime output AssociationId
		FileWriter fout = new FileWriter("/Users/roger/Documents/Multitenant workspace/apache-jmeter-2.9/AssociationCnt.txt"); 
		BufferedWriter out = new BufferedWriter(fout);
		out.write(Integer.toString(tmp+1));
		out.flush(); 
		out.close(); 
		fout.close(); 
		in.close(); 
		fr.close();
		
		return tmp; 
	}
	
	private int[] setArguments(PreparedStatement pstmt) throws SQLException,
			IOException {
		if (getQueryArguments().trim().length() == 0) {
			return new int[] {};
		}
		String[] arguments = CSVSaveService.csvSplitString(getQueryArguments(),
				COMMA_CHAR);
		String[] argumentsTypes = getQueryArgumentsTypes().split(COMMA);
		if (arguments.length != argumentsTypes.length) {
			throw new SQLException("number of arguments (" + arguments.length
					+ ") and number of types (" + argumentsTypes.length
					+ ") are not equal");
		}
		int[] outputs = new int[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			String argument = arguments[i];
			String argumentType = argumentsTypes[i];
			String[] arg = argumentType.split(" ");
			String inputOutput = "";
			if (arg.length > 1) {
				argumentType = arg[1];
				inputOutput = arg[0];
			}
			int targetSqlType = getJdbcType(argumentType);
			try {
				if (!OUT.equalsIgnoreCase(inputOutput)) {
					if (argument.equals(NULL_MARKER)) {
						pstmt.setNull(i + 1, targetSqlType);
					} else {
						pstmt.setObject(i + 1, argument, targetSqlType);
					}
				}
				if (OUT.equalsIgnoreCase(inputOutput)
						|| INOUT.equalsIgnoreCase(inputOutput)) {
					CallableStatement cs = (CallableStatement) pstmt;
					cs.registerOutParameter(i + 1, targetSqlType);
					outputs[i] = targetSqlType;
				} else {
					outputs[i] = java.sql.Types.NULL; // can't have an output
														// parameter type null
				}
			} catch (NullPointerException e) { // thrown by Derby JDBC (at
												// least) if there are no "?"
												// markers in statement
				throw new SQLException("Could not set argument no: " + (i + 1)
						+ " - missing parameter marker?");
			}
		}
		return outputs;
	}

	private static int getJdbcType(String jdbcType) throws SQLException {
		Integer entry = mapJdbcNameToInt.get(jdbcType
				.toLowerCase(java.util.Locale.ENGLISH));
		if (entry == null) {
			try {
				entry = Integer.decode(jdbcType);
			} catch (NumberFormatException e) {
				throw new SQLException("Invalid data type: " + jdbcType);
			}
		}
		return (entry).intValue();
	}

	private CallableStatement getCallableStatement(Connection conn)
			throws SQLException {
		return (CallableStatement) getPreparedStatement(conn, true);

	}

	private PreparedStatement getPreparedStatement(Connection conn)
			throws SQLException {
		return getPreparedStatement(conn, false);
	}

	private PreparedStatement getPreparedStatement(Connection conn,
			boolean callable) throws SQLException {
		Map<String, PreparedStatement> preparedStatementMap = perConnCache
				.get(conn);
		if (null == preparedStatementMap) {
			preparedStatementMap = new ConcurrentHashMap<String, PreparedStatement>();
			// As a connection is held by only one thread, we cannot already
			// have a
			// preparedStatementMap put by another thread
			perConnCache.put(conn, preparedStatementMap);
		}
		PreparedStatement pstmt = preparedStatementMap.get(getQuery());
		if (null == pstmt) {
			if (callable) {
				pstmt = conn.prepareCall(getQuery());
			} else {
				pstmt = conn.prepareStatement(getQuery());
			}
			// PreparedStatementMap is associated to one connection so
			// 2 threads cannot use the same PreparedStatement map at the same
			// time
			preparedStatementMap.put(getQuery(), pstmt);
		}
		pstmt.clearParameters();
		return pstmt;
	}

	private static void closeAllStatements(
			Collection<PreparedStatement> collection) {
		for (PreparedStatement pstmt : collection) {
			close(pstmt);
		}
	}

	/**
	 * Gets a Data object from a ResultSet.
	 * 
	 * @param rs
	 *            ResultSet passed in from a database query
	 * @return a Data object
	 * @throws java.sql.SQLException
	 * @throws UnsupportedEncodingException
	 */
	private String getStringFromResultSet(ResultSet rs) throws SQLException,
			UnsupportedEncodingException {
		ResultSetMetaData meta = rs.getMetaData();

		StringBuilder sb = new StringBuilder();

		int numColumns = meta.getColumnCount();
		for (int i = 1; i <= numColumns; i++) {
			sb.append(meta.getColumnName(i));
			if (i == numColumns) {
				sb.append('\n');
			} else {
				sb.append('\t');
			}
		}

		JMeterVariables jmvars = getThreadContext().getVariables();
		String varnames[] = getVariableNames().split(COMMA);
		String resultVariable = getResultVariable().trim();
		List<Map<String, Object>> results = null;
		if (resultVariable.length() > 0) {
			results = new ArrayList<Map<String, Object>>();
			jmvars.putObject(resultVariable, results);
		}
		int j = 0;
		while (rs.next()) {
			Map<String, Object> row = null;
			j++;
			for (int i = 1; i <= numColumns; i++) {
				Object o = rs.getObject(i);
				if (results != null) {
					if (row == null) {
						row = new HashMap<String, Object>(numColumns);
						results.add(row);
					}
					row.put(meta.getColumnName(i), o);
				}
				if (o instanceof byte[]) {
					o = new String((byte[]) o, ENCODING);
				}
				sb.append(o);
				if (i == numColumns) {
					sb.append('\n');
				} else {
					sb.append('\t');
				}
				if (i <= varnames.length) { // i starts at 1
					String name = varnames[i - 1].trim();
					if (name.length() > 0) { // Save the value in the variable
												// if present
						jmvars.put(name + UNDERSCORE + j,
								o == null ? null : o.toString());
					}
				}
			}
		}
		// Remove any additional values from previous sample
		for (int i = 0; i < varnames.length; i++) {
			String name = varnames[i].trim();
			if (name.length() > 0 && jmvars != null) {
				final String varCount = name + "_#"; // $NON-NLS-1$
				// Get the previous count
				String prevCount = jmvars.get(varCount);
				if (prevCount != null) {
					int prev = Integer.parseInt(prevCount);
					for (int n = j + 1; n <= prev; n++) {
						jmvars.remove(name + UNDERSCORE + n);
					}
				}
				jmvars.put(varCount, Integer.toString(j)); // save the current
															// count
			}
		}

		return sb.toString();
	}

	public static void close(Connection c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (SQLException e) {
			log.warn("Error closing Connection", e);
		}
	}

	public static void close(Statement s) {
		try {
			if (s != null) {
				s.close();
			}
		} catch (SQLException e) {
			log.warn("Error closing Statement " + s.toString(), e);
		}
	}

	public static void close(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			log.warn("Error closing ResultSet", e);
		}
	}

	public String getQuery() {
		return query;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(80);
		sb.append("["); // $NON-NLS-1$
		sb.append(getQueryType());
		sb.append("] "); // $NON-NLS-1$
		sb.append(getQuery());
		sb.append("\n");
		sb.append(getQueryArguments());
		sb.append("\n");
		sb.append(getQueryArgumentsTypes());
		return sb.toString();
	}

	/**
	 * @param query
	 *            The query to set.
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * @return Returns the dataSource.
	 */
	public String getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource
	 *            The dataSource to set.
	 */
	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @return Returns the queryType.
	 */
	public String getQueryType() {
		return queryType;
	}

	/**
	 * @param queryType
	 *            The queryType to set.
	 */
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	public String getQueryArguments() {
		return queryArguments;
	}

	public void setQueryArguments(String queryArguments) {
		this.queryArguments = queryArguments;
	}

	public String getQueryArgumentsTypes() {
		return queryArgumentsTypes;
	}

	public void setQueryArgumentsTypes(String queryArgumentsType) {
		this.queryArgumentsTypes = queryArgumentsType;
	}

	/**
	 * @return the variableNames
	 */
	public String getVariableNames() {
		return variableNames;
	}

	/**
	 * @param variableNames
	 *            the variableNames to set
	 */
	public void setVariableNames(String variableNames) {
		this.variableNames = variableNames;
	}

	/**
	 * @return the resultVariable
	 */
	public String getResultVariable() {
		return resultVariable;
	}

	/**
	 * @param resultVariable
	 *            the variable name in which results will be stored
	 */
	public void setResultVariable(String resultVariable) {
		this.resultVariable = resultVariable;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.jmeter.testelement.TestStateListener#testStarted()
	 */
	@Override
	public void testStarted() {
		testStarted("");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.jmeter.testelement.TestStateListener#testStarted(java.lang.String)
	 */
	@Override
	public void testStarted(String host) {
		cleanCache();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.jmeter.testelement.TestStateListener#testEnded()
	 */
	@Override
	public void testEnded() {
		testEnded("");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.jmeter.testelement.TestStateListener#testEnded(java.lang.String)
	 */
	@Override
	public void testEnded(String host) {
		cleanCache();
	}

	/**
	 * Clean cache of PreparedStatements
	 */
	private static final void cleanCache() {
		for (Map<String, PreparedStatement> element : perConnCache.values()) {
			closeAllStatements(element.values());
		}
		perConnCache.clear();
	}

}
