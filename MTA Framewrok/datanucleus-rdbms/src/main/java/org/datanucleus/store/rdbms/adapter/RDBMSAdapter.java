/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
2008 Andy Jefferson - added options Strings
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.adapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

import org.datanucleus.store.StoreManager;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.IdentifierFactory;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.key.CandidateKey;
import org.datanucleus.store.rdbms.key.ForeignKey;
import org.datanucleus.store.rdbms.key.Index;
import org.datanucleus.store.rdbms.key.PrimaryKey;
import org.datanucleus.store.rdbms.schema.ForeignKeyInfo;
import org.datanucleus.store.rdbms.schema.RDBMSColumnInfo;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.store.rdbms.table.Table;
import org.datanucleus.store.rdbms.table.TableImpl;
import org.datanucleus.store.rdbms.table.ViewImpl;

/**
 * Adapter for relational databases.
 */
public interface RDBMSAdapter extends DatastoreAdapter
{
    /** Whether the RDBMS supports SQL VIEWs. */
    public static final String VIEWS = "Views";

    /** Whether the RDBMS supports UNION syntax. */
    public static final String UNION_SYNTAX = "Union_Syntax";

    /**
     * Union combines the results of two or more queries into a single result set.
     * Union include only distinct rows and Union all may include duplicates.
     * When using the UNION statement, keep in mind that, by default, it performs the equivalent of 
     * a SELECT DISTINCT on the final result set. In other words, UNION takes the results of two like
     * recordsets, combines them, and then performs a SELECT DISTINCT in order to eliminate any 
     * duplicate rows. This process occurs even if there are no duplicate records in the final recordset. 
     * If you know that there are duplicate records, and this presents a problem for your application, 
     * then by all means use the UNION statement to eliminate the duplicate rows. On the other hand, 
     * if you know that there will never be any duplicate rows, or if there are, and this presents no 
     * problem to your application, then you should use the UNION ALL statement instead of the UNION 
     * statement. The advantage of the UNION ALL is that is does not perform the SELECT DISTINCT 
     * function, which saves a lot of unnecessary SQL Server resources from being using.
     */
    public static final String USE_UNION_ALL = "UseUnionAll";

    /**
     * Whether the RDBMS supports use of EXISTS syntax.
     */
    public static final String EXISTS_SYNTAX = "Exists_Syntax";

    /**
     * Whether this datastore supports ALTER TABLE DROP constraints
     */
    public static final String ALTER_TABLE_DROP_CONSTRAINT_SYNTAX = "AlterTableDropConstraint_Syntax";

    /**
     * Whether this datastore supports ALTER TABLE DROP FOREIGN KEY constraints
     */
    public static final String ALTER_TABLE_DROP_FOREIGN_KEY_CONSTRAINT = "AlterTableDropForeignKey_Syntax";

    /**
     * Whether this datastore supports deferred constraints.
     */
    public static final String DEFERRED_CONSTRAINTS = "DeferredConstraints";

    /**
     * Whether this datastore supports using DISTINCT when using SELECT ... FOR UPDATE.
     */
    public static final String DISTINCT_WITH_SELECT_FOR_UPDATE = "DistinctWithSelectForUpdate";

    /**
     * Whether the database server supports persist of an unassigned character ("0x0").
     * If not, any unassigned character will be replaced by " " (space) on persist.
     */
    public static final String PERSIST_OF_UNASSIGNED_CHAR = "PersistOfUnassignedChar";

    /**
     * Some databases store character strings in CHAR(XX) columns and when read back in have been padded
     * with spaces.
     */
    public static final String CHAR_COLUMNS_PADDED_WITH_SPACES = "CharColumnsPaddedWithSpaces";

    /**
     * Some databases, Oracle, treats an empty string (0 length) equals null.
     */
    public static final String NULL_EQUALS_EMPTY_STRING = "NullEqualsEmptyString";

    /**
     * Whether this datastore supports batching of statements.
     */
    public static final String STATEMENT_BATCHING = "StatementBatching";

    /**
     * Whether this datastore supports the use of "CHECK" in CREATE TABLE statements (DDL).
     */
    public static final String CHECK_IN_CREATE_STATEMENTS = "CheckInCreateStatements";

    /**
     * Whether this datastore supports the use of CHECK after the column definitions in the
     * CREATE TABLE statements (DDL). for example
     * <PRE>
     * CREATE TABLE MYTABLE
     * (
     *     COL_A int,
     *     COL_B char(1),
     *     PRIMARY KEY (COL_A),
     *     CHECK (COL_B IN ('Y','N'))
     * )
     * </PRE>
     */
    public static final String CHECK_IN_END_CREATE_STATEMENTS = "CheckInEndCreateStatements";

    /**
     * Whether this datastore supports the use of UNIQUE after the column
     * definitions in CREATE TABLE statements (DDL). For example
     * <PRE>
     * CREATE TABLE MYTABLE
     * (
     *     COL_A int,
     *     COL_B char(1),
     *     PRIMARY KEY (COL_A),
     *     UNIQUE (COL_B ...)
     * )
     * </PRE> 
     */
    public static final String UNIQUE_IN_END_CREATE_STATEMENTS = "UniqueInEndCreateStatements";

    /**
     * Whether this datastore supports the use of FOREIGN KEY after the column
     * definitions in CREATE TABLE statements (DDL). For example
     * <PRE>
     * CREATE TABLE MYTABLE
     * (
     *     COL_A int,
     *     COL_B char(1),
     *     FOREIGN KEY (COL_A) REFERENCES TBL2(COL_X)
     * )
     * </PRE> 
     */
    public static final String FK_IN_END_CREATE_STATEMENTS = "FKInEndCreateStatements";

    /**
     * Whether the datastore supports specification of the primary key in CREATE TABLE statements.
     */
    public static final String PRIMARYKEY_IN_CREATE_STATEMENTS = "PrimaryKeyInCreateStatements";

    /**
     * Whether the datastore supports "Statement.getGeneratedKeys".
     */
    public static final String GET_GENERATED_KEYS_STATEMENT = "GetGeneratedKeysStatement";

    /**
     * Whether we support NULLs in candidate keys.
     */
    public static final String NULLS_IN_CANDIDATE_KEYS = "NullsInCandidateKeys";

    /**
     * Whether the database support NULLs in the column options for table creation.
     */
    public static final String NULLS_KEYWORD_IN_COLUMN_OPTIONS = "ColumnOptions_NullsKeyword";

    /**
     * Whether we support DEFAULT tag in CREATE TABLE statements
     */
    public static final String DEFAULT_KEYWORD_IN_COLUMN_OPTIONS = "ColumnOptions_DefaultKeyword";

    /**
     * Whether we support DEFAULT tag together with NOT NULL in CREATE TABLE statements.
     * <pre>CREATE TABLE X ( MEMORY_SIZE BIGINT DEFAULT 0 NOT NULL )</pre>
     * Some databases only support <i>DEFAULT {ConstantExpression | NULL}</i>
     */
    public static final String DEFAULT_KEYWORD_WITH_NOT_NULL_IN_COLUMN_OPTIONS = "ColumnOptions_DefaultWithNotNull";

    /**
     * Whether any DEFAULT tag will be before any NULL/NOT NULL in the column options.
     */
    public static final String DEFAULT_BEFORE_NULL_IN_COLUMN_OPTIONS = "ColumnOptions_DefaultBeforeNull";

    /**
     * Accessor for whether the RDBMS supports ANSI join syntax.
     */
    public static final String ANSI_JOIN_SYNTAX = "ANSI_Join_Syntax";

    /**
     * Accessor for whether the RDBMS supports ANSI cross-join syntax.
     */
    public static final String ANSI_CROSSJOIN_SYNTAX = "ANSI_CrossJoin_Syntax";

    /**
     * Accessor for whether the RDBMS supports cross-join as "INNER 1=1" syntax.
     */
    public static final String CROSSJOIN_ASINNER11_SYNTAX = "ANSI_CrossJoinAsInner11_Syntax";

    /**
     * Whether we support auto-increment/identity keys with nullability specification.
     */
    public static final String AUTO_INCREMENT_KEYS_NULL_SPECIFICATION = "AutoIncrementNullSpecification";

    /**
     * Whether we support auto-increment/identity keys with column type specification.
     */
    public static final String AUTO_INCREMENT_COLUMN_TYPE_SPECIFICATION = "AutoIncrementColumnTypeSpecification";

    /**
     * Whether this adapter requires any specification of primary key in the column definition of CREATE TABLE.
     */
    public static final String AUTO_INCREMENT_PK_IN_CREATE_TABLE_COLUMN_DEF = "AutoIncrementPkInCreateTableColumnDef";

    /**
     * Whether this datastore supports SELECT ... FOR UPDATE.
     */
    public static final String LOCK_WITH_SELECT_FOR_UPDATE = "LockWithSelectForUpdate";

    /**
     * Whether the lock option (when doing SELECT FOR UPDATE) is to be placed after the FROM.
     */
    public static final String LOCK_OPTION_PLACED_AFTER_FROM = "LockOptionAfterFromClause";

    /**
     * Whether the lock option (when doing SELECT FOR UPDATE) is to be placed within the JOIN clause.
     */
    public static final String LOCK_OPTION_PLACED_WITHIN_JOIN = "LockOptionWithinJoinClause";

    /**
     * Accessor for whether setting a BLOB value allows use of PreparedStatement.setString()
     */
    public static final String BLOB_SET_USING_SETSTRING = "BlobSetUsingSetString";

    /**
     * Accessor for whether setting a CLOB value allows use of PreparedStatement.setString()
     */
    public static final String CLOB_SET_USING_SETSTRING = "ClobSetUsingSetString";

    /**
     * Whether to create indexes before foreign keys.
     */
    public static final String CREATE_INDEXES_BEFORE_FOREIGN_KEYS = "CreateIndexesBeforeForeignKeys";

    /**
     * Whether to include any ORDER BY columns in a SELECT.
     */
    public static final String INCLUDE_ORDERBY_COLS_IN_SELECT = "IncludeOrderByColumnsInSelect";

    /**
     * Whether DATETIME stores milliseconds.
     */
    public static final String DATETIME_STORES_MILLISECS = "DateTimeStoresMillisecs";

    /**
     * Whether this database supports joining outer and inner queries using columns.
     * i.e can you refer to a column of the outer query in a subquery when the outer query table
     * is not the primary table of the outer query (i.e joined)
     */
    public static final String ACCESS_PARENTQUERY_IN_SUBQUERY_JOINED = "AccessParentQueryInSubquery";

    /**
     * In SAPDB any orderby has to be using the index(es) of any SELECT column(s) rather than
     * their name(s).
     */
    public static final String ORDERBY_USING_SELECT_COLUMN_INDEX = "OrderByUsingSelectColumnIndex";

    /**
     * Whether this datastore supports stored procedures.
     */
    public static final String STORED_PROCEDURES = "StoredProcs";

    public static final String FK_UPDATE_ACTION_CASCADE = "FkUpdateActionCascade";
    public static final String FK_UPDATE_ACTION_DEFAULT = "FkUpdateActionDefault";
    public static final String FK_UPDATE_ACTION_NULL = "FkUpdateActionNull";
    public static final String FK_UPDATE_ACTION_RESTRICT = "FkUpdateActionRestrict";

    public static final String FK_DELETE_ACTION_CASCADE = "FkDeleteActionCascade";
    public static final String FK_DELETE_ACTION_DEFAULT = "FkDeleteActionDefault";
    public static final String FK_DELETE_ACTION_NULL = "FkDeleteActionNull";
    public static final String FK_DELETE_ACTION_RESTRICT = "FkDeleteActionRestrict";

    public static final String TX_ISOLATION_NONE = "TxIsolationNone";
    public static final String TX_ISOLATION_READ_COMMITTED = "TxIsolationReadCommitted";
    public static final String TX_ISOLATION_READ_UNCOMMITTED = "TxIsolationReadUncommitted";
    public static final String TX_ISOLATION_REPEATABLE_READ = "TxIsolationReadRepeatableRead";
    public static final String TX_ISOLATION_SERIALIZABLE = "TxIsolationSerializable";

    /** Whether this datastore supports multiple table update statements. */
    public static final String UPDATE_MULTITABLE = "UpdateMultiTable";

    /**
     * Accessor for whether this database adapter supports the specified transaction isolation.
     * @param level The isolation level (as defined by Connection enums).
     * @return Whether it is supported.
     */
    boolean supportsTransactionIsolation(int level);

    /**
     * Method to return the SQL to append to the end of the SELECT statement to handle
     * restriction of ranges using the LIMIT keyword. Defaults to an empty string (not supported).
     * SELECT param ... WHERE {LIMIT}
     * @param offset The offset to return from
     * @param count The number of items to return
     * @return The SQL to append to allow for ranges using LIMIT.
     */
    String getRangeByLimitEndOfStatementClause(long offset, long count);

    /**
     * Method to return the column name to use when handling ranges via
     * a rownumber on the select using the original method (DB2). Defaults to an empty string (not supported).
     * @return The row number column.
     */
    String getRangeByRowNumberColumn();

    /**
     * Method to return the column name to use when handling ranges via
     * a rownumber on the select using the second method (Oracle). Defaults to an empty string (not supported).
     * @return The row number column.
     */
    String getRangeByRowNumberColumn2();

    /**
     * Accessor for table and column information for a catalog/schema in this datastore.
     * @param conn Connection to use
     * @param catalog The catalog (null if none)
     * @param schema The schema (null if none)
     * @param table The table (null if all)
     * @param columnNamePattern The column name (null if all)
     * @return ResultSet containing the table/column information
     * @throws SQLException Thrown if an error occurs
     */
    ResultSet getColumns(Connection conn, String catalog, String schema, String table, String columnNamePattern)
    throws SQLException;

    /**
     * Method to retutn the INSERT statement to use when inserting into a table that has no
     * columns specified. This is the case when we have a single column in the table and that column
     * is autoincrement/identity (and so is assigned automatically in the datastore).
     * @param table The table
     * @return The statement for the INSERT
     */
    String getInsertStatementForNoColumns(Table table);
    
    /**
     * Returns the precision value to be used when creating string columns of "unlimited" length.
     * Usually, if this value is needed it is provided in the database metadata.
     * However, for some types in some databases the value must be computed.
     * @param typeInfo the typeInfo object for which the precision value is needed.
     * @return the precision value to be used when creating the column, or -1 if no value should be used.
     */
    int getUnlimitedLengthPrecisionValue(SQLTypeInfo typeInfo);

    /**
     * Accessor for the auto-increment/identity sql statement for this datastore.
     * @param table Table (that the autoincrement is for)
     * @param columnName (that the autoincrement is for)
     * @return The statement for getting the latest auto-increment/identity key
     **/
    String getAutoIncrementStmt(Table table, String columnName);

    /**
     * Accessor for the auto-increment/identity keyword for generating DDLs.
     * (CREATE TABLEs...).
     * @return The keyword for a column using auto-increment/identity
     **/
    String getAutoIncrementKeyword();

    /**
     * Returns the appropriate SQL to drop the given table.
     * It should return something like:
     * <p>
     * <pre>
     * DROP TABLE FOO CASCADE
     * </pre>
     *
     * @param table The table to drop.
     * @return The text of the SQL statement.
     */
    String getDropTableStatement(DatastoreContainerObject table);

    /**
     * Method to return the basic SQL for a DELETE TABLE statement.
     * Returns a String like <code>DELETE FROM tbl t1</code>. Doesn't include any where clause.
     * @param tbl The SQLTable to delete
     * @return The delete table string
     */
    String getDeleteTableStatement(SQLTable tbl);

    /**
     * Returns the appropriate SQL to add a candidate key to its table.
     * It should return something like:
     * <p>
     * <pre>
     * ALTER TABLE FOO ADD CONSTRAINT FOO_CK UNIQUE (BAZ)
     * </pre>
     *
     * @param ck An object describing the candidate key.
     * @param factory Identifier factory
     * @return  The text of the SQL statement.
     */
    String getAddCandidateKeyStatement(CandidateKey ck, IdentifierFactory factory);

    /**
     * Method to return whether the specified JDBC type is valid for use in a PrimaryKey.
     * @param datatype The JDBC type.
     * @return Whether it is valid for use in the PK
     */
    boolean isValidPrimaryKeyType(int datatype);
    
    /**
     * Accessor for the SQL statement to add a column to a table.
     * @param table The table
     * @param col The column
     * @return The SQL necessary to add the column
     */
    String getAddColumnStatement(DatastoreContainerObject table, Column col);

    /**
     * Returns the appropriate SQL to add an index to its table.
     * It should return something like:
     * <p>
     * <pre>
     * CREATE INDEX FOO_N1 ON FOO (BAR,BAZ)
     * CREATE UNIQUE INDEX FOO_U1 ON FOO (BAR,BAZ)
     * </pre>
     *
     * @param idx An object describing the index.
     * @param factory Identifier factory
     * @return The text of the SQL statement.
     */
    String getCreateIndexStatement(Index idx, IdentifierFactory factory);

    /**
     * Provide the existing indexes in the database for the table
     * @param conn the JDBC connection
     * @param catalog the catalog name
     * @param schema the schema name
     * @param table the table name
     * @return a ResultSet with the format @see DatabaseMetaData#getIndexInfo(java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
     * @throws SQLException
     */
    ResultSet getExistingIndexes(Connection conn, String catalog, String schema, String table)
    throws SQLException;
    
    /**
     * Returns the appropriate SQL to create the given table having the given
     * columns. No column constraints or key definitions should be included.
     * It should return something like:
     * <p>
     * <pre>
     * CREATE TABLE FOO (BAR VARCHAR(30), BAZ INTEGER)
     * </pre>
     *
     * @param table The table to create.
     * @param columns The columns of the table.
     * @param props Properties for controlling the table creation
     * @param factory Identifier factory
     * @return The text of the SQL statement.
     */
    String getCreateTableStatement(TableImpl table, Column[] columns, Properties props, IdentifierFactory factory);

    /**
     * Returns the appropriate SQL to add a primary key to its table.
     * It should return something like:
     * <p>
     * <pre>
     * ALTER TABLE FOO ADD CONSTRAINT FOO_PK PRIMARY KEY (BAR)
     * </pre>
     *
     * @param pk An object describing the primary key.
     * @param factory Identifier factory
     * @return The text of the SQL statement.
     */
    String getAddPrimaryKeyStatement(PrimaryKey pk, IdentifierFactory factory);

    /**
     * Returns the appropriate SQL to add a foreign key to its table.
     * It should return something like:
     * <p>
     * <pre>
     * ALTER TABLE FOO ADD CONSTRAINT FOO_FK1 FOREIGN KEY (BAR, BAZ) REFERENCES ABC (COL1, COL2)
     * </pre>
     * @param fk An object describing the foreign key.
     * @param factory Identifier factory
     * @return  The text of the SQL statement.
     */
    String getAddForeignKeyStatement(ForeignKey fk, IdentifierFactory factory);

    /**
     * Returns the appropriate SQL to drop the given view.
     * It should return something like:
     * <p>
     * <pre>
     * DROP VIEW FOO
     * </pre>
     *
     * @param view The view to drop.
     * @return The text of the SQL statement.
     */
    String getDropViewStatement(ViewImpl view);

    /**
     * Some databases, Oracle, treats an empty string (0 length) equals null
     * @return returns a surrogate to replace the empty string in the database
     * otherwise it would be treated as null
     */
    String getSurrogateForEmptyStrings();

    /**
     * Accessor for the transaction isolation level to use during schema creation.
     * @return The transaction isolation level for schema generation process
     */
    int getTransactionIsolationForSchemaCreation();

    /**
     * Accessor for the "required" transaction isolation level if it has to be a certain value
     * for this adapter.
     * @return Transaction isolation level (-1 implies no restriction)
     */
    int getRequiredTransactionIsolationLevel();

    /**
     * Accessor for the Catalog Name for this datastore.
     * @param conn Connection to the datastore
     * @return The catalog name
     * @throws SQLException Thrown if error occurs in determining the
     * catalog name.
     **/
    String getCatalogName(Connection conn)
    throws SQLException;

    /**
     * Accessor for the Schema Name for this datastore.
     * @param conn Connection to the datastore
     * @return The schema name
     * @throws SQLException Thrown if error occurs in determining the
     * schema name.
     **/
    String getSchemaName(Connection conn)
    throws SQLException;

    /**
     * The option to specify in "SELECT ... FROM TABLE ... WITH (option)" to lock instances
     * Null if not supported.
     * @return The option to specify with "SELECT ... FROM TABLE ... WITH (option)"
     **/
    String getSelectWithLockOption();

    /**
     * The function to creates a unique value of type uniqueidentifier.
     * @return The function. e.g. "SELECT NEWID()"
     **/
    String getSelectNewUUIDStmt();

    /**
     * Accessor for the sequence statement to get the next id for this 
     * datastore.
     * @param sequence_name Name of the sequence 
     * @return The statement for getting the next id for the sequence
     **/
    String getSequenceNextStmt(String sequence_name);

    /**
     * Accessor for the sequence create statement for this datastore.
     * @param sequence_name Name of the sequence 
     * @param min Minimum value for the sequence
     * @param max Maximum value for the sequence
     * @param start Start value for the sequence
     * @param increment Increment value for the sequence
     * @param cache_size Cache size for the sequence
     * @return The statement for getting the next id from the sequence
     */
    String getSequenceCreateStmt(String sequence_name, Integer min, Integer max, Integer start,
            Integer increment, Integer cache_size);

    /**
     * Iterator for the reserved words constructed from the method
     * DataBaseMetaData.getSQLKeywords + standard SQL reserved words
     * @return an Iterator with a set of reserved words
     */
    Iterator iteratorReservedWords();

    /**
     * Accessor for a statement that will return the statement to use to get the datastore date.
     * @return SQL statement to get the datastore date
     */
    String getDatastoreDateStatement();
    
    /**
     * Creates a CHECK constraint definition based on the given values
     * e.g. <pre>CHECK ("COLUMN" IN ('VAL1','VAL2') OR "COLUMN" IS NULL)</pre>
     * @param identifier Column identifier
     * @param values valid values
     * @param nullable whether the datastore identifier is null
     * @return The check constraint
     */
    String getCheckConstraintForValues(DatastoreIdentifier identifier, Object[] values, boolean nullable);

    /**
     * Create a new SQL type info from the current row of the passed ResultSet.
     * Allows an adapter to override particular types where the JDBC driver is known to be buggy.
     * @param rs ResultSet
     * @return The SQL type info
     */
    public SQLTypeInfo newSQLTypeInfo(ResultSet rs);

    /**
     * Create a new column info from the current row of the passed ResultSet.
     * Allows an adapter to override particular column information where the JDBC driver is known
     * to be buggy.
     * @param rs Result Set
     * @return The column info
     */
    public RDBMSColumnInfo newRDBMSColumnInfo(ResultSet rs);

    /**
     * Method to return ForeignKeyInfo for the current row of the ResultSet which will have been
     * obtained from a call to DatabaseMetaData.getImportedKeys() or DatabaseMetaData.getExportedKeys().
     * @param rs The result set returned from DatabaseMetaData.get??portedKeys()
     * @return The foreign key info 
     */
    public ForeignKeyInfo newFKInfo(ResultSet rs);

    /**
     * Convenience method to allow adaption of an ordering string before applying it.
     * This is useful where the datastore accepts some conversion adapter around the ordering column
     * for example.
     * @param storeMgr StoreManager
     * @param orderString The basic ordering string
     * @param sqlExpr The sql expression being represented here
     * @return The adapted ordering string
     */
    public String getOrderString(StoreManager storeMgr, String orderString, SQLExpression sqlExpr);

    /**
     * Method to return if it is valid to select the specified mapping for the specified statement
     * for this datastore adapter. Sometimes, dependent on the type of the column(s), and what other
     * components are present in the statement, it may be invalid to select the mapping.
     * @param stmt The statement
     * @param m The mapping that we want to select
     * @return Whether it is valid
     */
    public boolean validToSelectMappingInStatement(SQLStatement stmt, JavaTypeMapping m);

    /**
     * return whether this exception represents a cancelled statement.
     * @param sqle the exception
     * @return whether it is a cancel
     */
    public boolean isStatementCancel(SQLException sqle);

    /**
     * return whether this exception represents a timed out statement.
     * @param sqle the exception
     * @return whether it is a timeout
     */
    public boolean isStatementTimeout(SQLException sqle);
}