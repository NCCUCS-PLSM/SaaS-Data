package com.mysql.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mysql.jdbc.DatabaseMetaData.IterateBlock;
import com.mysql.jdbc.DatabaseMetaData.TypeDescriptor;

public class MultitenantDatabaseMetaData extends DatabaseMetaData{

	public MultitenantDatabaseMetaData(Connection connToSet,
			String databaseToSet) {
		super(connToSet, databaseToSet);
		// TODO Auto-generated constructor stub
	}

	public java.sql.ResultSet getColumns(final String catalog,
			final String schemaPattern, final String tableNamePattern,
			String columnNamePattern,String tenantId) throws SQLException {
		int count = 0;
		final ArrayList rows = new ArrayList();
		Field[] fields = new Field[23];
		fields[0] = new Field("", "TABLE_CAT", Types.CHAR, 255);
		fields[1] = new Field("", "TABLE_SCHEM", Types.CHAR, 0);
		fields[2] = new Field("", "TABLE_NAME", Types.CHAR, 255);
		fields[3] = new Field("", "COLUMN_NAME", Types.CHAR, 32);
		fields[4] = new Field("", "DATA_TYPE", Types.SMALLINT, 5);
		fields[5] = new Field("", "TYPE_NAME", Types.CHAR, 16);
		fields[6] = new Field("", "COLUMN_SIZE", Types.INTEGER, Integer
				.toString(Integer.MAX_VALUE).length());
		fields[7] = new Field("", "BUFFER_LENGTH", Types.INTEGER, 10);
		fields[8] = new Field("", "DECIMAL_DIGITS", Types.INTEGER, 10);
		fields[9] = new Field("", "NUM_PREC_RADIX", Types.INTEGER, 10);
		fields[10] = new Field("", "NULLABLE", Types.INTEGER, 10);
		fields[11] = new Field("", "REMARKS", Types.CHAR, 0);
		fields[12] = new Field("", "COLUMN_DEF", Types.CHAR, 0);
		fields[13] = new Field("", "SQL_DATA_TYPE", Types.INTEGER, 10);
		fields[14] = new Field("", "SQL_DATETIME_SUB", Types.INTEGER, 10);
		fields[15] = new Field("", "CHAR_OCTET_LENGTH", Types.INTEGER, Integer
				.toString(Integer.MAX_VALUE).length());
		fields[16] = new Field("", "ORDINAL_POSITION", Types.INTEGER, 10);
		fields[17] = new Field("", "IS_NULLABLE", Types.CHAR, 3);
		fields[18] = new Field("", "SCOPE_CATALOG", Types.CHAR, 255);
		fields[19] = new Field("", "SCOPE_SCHEMA", Types.CHAR, 255);
		fields[20] = new Field("", "SCOPE_TABLE", Types.CHAR, 255);
		fields[21] = new Field("", "SOURCE_DATA_TYPE", Types.SMALLINT, 10);
		fields[22] = new Field("", "IS_AUTOINCREMENT", Types.CHAR, 3);


		while(count <= 1)
		{
			System.out.println("NowCount:"+count);
			final String tableNamePatternTmp;
			if(count == 0)
			{
				tableNamePatternTmp = tableNamePattern.replace(tenantId, "")+"CommonFields";
				System.out.println("tableNamePatternTmp:"+tableNamePatternTmp);
			}
			else
			{

				tableNamePatternTmp = tenantId+tableNamePattern;
				System.out.println("tableNamePatternTmp:"+tableNamePatternTmp);

			}

			System.out.println(tableNamePatternTmp);
			//System.out.println(queryBuf.toString().replace(tableNamePatternTmp.toLowerCase(),tmp));
			if (columnNamePattern == null) {
				if (this.conn.getNullNamePatternMatchesAll()) {
					columnNamePattern = "%";
				} else {
					throw SQLError.createSQLException(
							"Column name pattern can not be NULL or empty.",
							SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
				}
			}

			final String colPattern = columnNamePattern;




			final Statement stmt = this.conn.getMetadataSafeStatement();

			try {

				new IterateBlock(getCatalogIterator(catalog)) {
					void forEach(Object catalogStr) throws SQLException {

						ArrayList tableNameList = new ArrayList();

						if (tableNamePatternTmp == null) {
							// Select from all tables
							java.sql.ResultSet tables = null;

							try {
								tables = getTables(catalog, schemaPattern, "%",
										new String[0]);

								while (tables.next()) {
									String tableNameFromList = tables
											.getString("TABLE_NAME");
									tableNameList.add(tableNameFromList);
								}
							} finally {
								if (tables != null) {
									try {
										tables.close();
									} catch (Exception sqlEx) {
										AssertionFailedException
												.shouldNotHappen(sqlEx);
									}

									tables = null;
								}
							}
						} else {
							java.sql.ResultSet tables = null;

							try {
								tables = getTables(catalog, schemaPattern,
										tableNamePatternTmp, new String[0]);

								while (tables.next()) {
									String tableNameFromList = tables
											.getString("TABLE_NAME");
									tableNameList.add(tableNameFromList);
								}
							} finally {
								if (tables != null) {
									try {
										tables.close();
									} catch (SQLException sqlEx) {
										AssertionFailedException
												.shouldNotHappen(sqlEx);
									}

									tables = null;
								}
							}
						}

						java.util.Iterator tableNames = tableNameList.iterator();

						while (tableNames.hasNext()) {
							String tableName = (String) tableNames.next();

							ResultSet results = null;

							try {
								StringBuffer queryBuf = new StringBuffer("SHOW ");

								if (conn.versionMeetsMinimum(4, 1, 0)) {
									queryBuf.append("FULL ");
								}

								queryBuf.append("COLUMNS FROM ");
								queryBuf.append(quotedId);
								queryBuf.append(tableName);
								queryBuf.append(quotedId);
								queryBuf.append(" FROM ");
								queryBuf.append(quotedId);
								queryBuf.append(catalogStr.toString());
								queryBuf.append(quotedId);
								queryBuf.append(" LIKE '");
								queryBuf.append(colPattern);
								queryBuf.append("'");

								// Return correct ordinals if column name pattern is
								// not '%'
								// Currently, MySQL doesn't show enough data to do
								// this, so we do it the 'hard' way...Once _SYSTEM
								// tables are in, this should be much easier
								boolean fixUpOrdinalsRequired = false;
								Map ordinalFixUpMap = null;

								if (!colPattern.equals("%")) {
									fixUpOrdinalsRequired = true;

									StringBuffer fullColumnQueryBuf = new StringBuffer(
											"SHOW ");

									if (conn.versionMeetsMinimum(4, 1, 0)) {
										fullColumnQueryBuf.append("FULL ");
									}

									fullColumnQueryBuf.append("COLUMNS FROM ");
									fullColumnQueryBuf.append(quotedId);
									fullColumnQueryBuf.append(tableName);
									fullColumnQueryBuf.append(quotedId);
									fullColumnQueryBuf.append(" FROM ");
									fullColumnQueryBuf.append(quotedId);
									fullColumnQueryBuf
											.append(catalogStr.toString());
									fullColumnQueryBuf.append(quotedId);

									results = stmt.executeQuery(fullColumnQueryBuf
											.toString());

									ordinalFixUpMap = new HashMap();

									int fullOrdinalPos = 1;

									while (results.next()) {
										String fullOrdColName = results
												.getString("Field");

										ordinalFixUpMap.put(fullOrdColName,
												new Integer(fullOrdinalPos++));
									}
								}



								//results = stmt.executeQuery(queryBuf.toString().replace(tableNamePatternTmp.toLowerCase(),tmp));
								//System.out.println(results.findColumn("TenantId"));
								//results.getMetaData().getColumnCount()
								results = stmt.executeQuery(queryBuf.toString());
								int ordPos = 1;
								int resultcount = 0;
								while (results.next()) {
									resultcount++;
									if(resultcount > 2)
									{
										byte[][] rowVal = new byte[23][];
										rowVal[0] = s2b(catalog); // TABLE_CAT
										rowVal[1] = null; // TABLE_SCHEM (No schemas
										// in MySQL)

										rowVal[2] = s2b(tableName); // TABLE_NAME
										rowVal[3] = results.getBytes("Field");
										//rowVal[3] = results.getString(columnIndex)
										//System.out.println(rowVal[2]);
										TypeDescriptor typeDesc = new TypeDescriptor(
												results.getString("Type"), results
														.getString("Null"));

										rowVal[4] = Short.toString(typeDesc.dataType)
												.getBytes();

										// DATA_TYPE (jdbc)
										rowVal[5] = s2b(typeDesc.typeName); // TYPE_NAME
										// (native)
										rowVal[6] = typeDesc.columnSize == null ? null
												: s2b(typeDesc.columnSize.toString());
										rowVal[7] = s2b(Integer
												.toString(typeDesc.bufferLength));
										rowVal[8] = typeDesc.decimalDigits == null ? null
												: s2b(typeDesc.decimalDigits.toString());
										rowVal[9] = s2b(Integer
												.toString(typeDesc.numPrecRadix));
										rowVal[10] = s2b(Integer
												.toString(typeDesc.nullability));

										//
										// Doesn't always have this field, depending on
										// version
										//
										//
										// REMARK column
										//
										try {
											if (conn.versionMeetsMinimum(4, 1, 0)) {
												rowVal[11] = results
														.getBytes("Comment");
											} else {
												rowVal[11] = results.getBytes("Extra");
											}
										} catch (Exception E) {
											rowVal[11] = new byte[0];
										}

										// COLUMN_DEF
										rowVal[12] = results.getBytes("Default");

										rowVal[13] = new byte[] { (byte) '0' }; // SQL_DATA_TYPE
										rowVal[14] = new byte[] { (byte) '0' }; // SQL_DATE_TIME_SUB

										if (StringUtils.indexOfIgnoreCase(
												typeDesc.typeName, "CHAR") != -1
												|| StringUtils.indexOfIgnoreCase(
														typeDesc.typeName, "BLOB") != -1
												|| StringUtils.indexOfIgnoreCase(
														typeDesc.typeName, "TEXT") != -1
												|| StringUtils.indexOfIgnoreCase(
														typeDesc.typeName, "BINARY") != -1) {
											rowVal[15] = rowVal[6]; // CHAR_OCTET_LENGTH
										} else {
											rowVal[15] = null;
										}

										// ORDINAL_POSITION
										if (!fixUpOrdinalsRequired) {
											rowVal[16] = Integer.toString(ordPos++)
													.getBytes();
										} else {
											String origColName = results
													.getString("Field");
											Integer realOrdinal = (Integer) ordinalFixUpMap
													.get(origColName);

											if (realOrdinal != null) {
												rowVal[16] = realOrdinal.toString()
														.getBytes();
											} else {
												throw SQLError
														.createSQLException(
																"Can not find column in full column list to determine true ordinal position.",
																SQLError.SQL_STATE_GENERAL_ERROR);
											}
										}

										rowVal[17] = s2b(typeDesc.isNullable);

										// We don't support REF or DISTINCT types
										rowVal[18] = null;
										rowVal[19] = null;
										rowVal[20] = null;
										rowVal[21] = null;

										rowVal[22] = s2b("");

										String extra = results.getString("Extra");

										if (extra != null) {
											rowVal[22] = s2b(StringUtils
													.indexOfIgnoreCase(extra,
															"auto_increment") != -1 ? "YES"
													: "NO");
										}

										rows.add(rowVal);

									}

								}
							} finally {
								if (results != null) {
									try {
										results.close();
									} catch (Exception ex) {
										;
									}

									results = null;
								}
							}
						}
					}
				}.doForAll();
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
			count++;
			System.out.println(count);


		}

		java.sql.ResultSet results = buildResultSet(fields, rows);
		return results;



	}
}
