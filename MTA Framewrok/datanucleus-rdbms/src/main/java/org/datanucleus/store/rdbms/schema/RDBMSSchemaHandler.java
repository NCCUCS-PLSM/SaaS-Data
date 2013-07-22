/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.adapter.DatabaseAdapter;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.table.Table;
import org.datanucleus.store.schema.StoreSchemaData;
import org.datanucleus.store.schema.StoreSchemaHandler;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Handler for RDBMS schema information.
 * Provides access to the following types of schema data
 * <ul>
 * <li><b>types</b> : type information for the datastore columns</li>
 * <li><b>tables</b> : hierarchy of schema-tables-columns.</li>
 * <li><b>foreign-keys</b> : FK info for a table</li>
 * <li><b>primary-keys</b> : PK info for a table</li>
 * <li><b>indices</b> : Indices info for a table</li>
 * <li><b>columns</b> : Columns info for a table</li>
 * </ul>
 */
public class RDBMSSchemaHandler implements StoreSchemaHandler
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.store.rdbms.Localisation", RDBMSStoreManager.class.getClassLoader());

    /** Time within which column info is valid (millisecs). Set to 5 mins. */
    protected final long COLUMN_INFO_EXPIRATION_MS = 5*60*1000;

    protected final RDBMSStoreManager storeMgr;

    /** 
     * Map of schema data, keyed by its symbolic name where the data is cached. 
     * Can be "types", "tables" etc. The "tables" cached here are "known tables" and not
     * just all tables for the catalog/schema.
     */
    protected Map<String, StoreSchemaData> schemaDataByName = new HashMap();

    public RDBMSSchemaHandler(RDBMSStoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
    }

    /**
     * Method to clear out any cached schema information.
     */
    public void clear()
    {
        schemaDataByName.clear();
    }

    /**
     * Method to create the schema with the supplied name.
     * @param connection Connection to the datastore
     * @param schemaName Name of the schema
     */
    public void createSchema(Object connection, String schemaName)
    {
        throw new NucleusUserException("DataNucleus doesnt currently support creation of schemas for RDBMS");
    }

    /**
     * Method to delete the schema with the supplied name.
     * @param connection Connection to the datastore
     * @param schemaName Name of the schema
     */
    public void deleteSchema(Object connection, String schemaName)
    {
        throw new NucleusUserException("DataNucleus doesnt currently support deletion of schemas for RDBMS");
    }

    /**
     * Accessor for schema data store under the provided name and defined by the specified values.
     * When there are no "values" the following are supported usages:-
     * <ul>
     * <li><b>types</b> : return the JDBC/SQL types for the datastore. Returns an RDBMSTypesInfo
     *     which contains the JDBCTypeInfo, which in turn contains the SQLTypeInfo.
     *     Types information is loaded on the first call and is cached thereafter.</li>
     * <li><b>tables</b> : return all currently loaded tables, with their columns. 
     *     Returns an RDBMSSchemaInfo. When a table has been loaded for more than a period of
     *     time and is requested again we discard the cached info and go to the datastore in case
     *     it has been updated.</li>
     * </ul>
     * When there is only one "value" the following are supported usages:-
     * <ul>
     * <li><b>foreign-keys</b> : return all foreign keys for a Table, where the Table is passed in.
     *     Returns an RDBMSTableFKInfo</li>
     * <li><b>primary-keys</b> : return all primary keys for a Table, where the Table is passed in.
     *     Returns an RDBMSTablePFKInfo</li>
     * <li><b>indices</b> : return all indices for a Table, where the Table is passed in.
     *     Returns an RDBMSTableIndexInfo</li>
     * <li><b>columns</b> : return all columns for a Table, where the Table is passed in.
     *     Returns an RDBMSTableInfo.</li> 
     * </ul>
     * When there are two "values" the following are supported usages:-
     * <ul>
     * <li><b>columns</b> : return column info for the supplied Table and column name.
     *     Returns an RDBMSTableInfo.</li>
     * <li><b>tables</b> : return table information for the supplied catalog and schema names.
     *     Returns an RDBMSSchemaInfo</li>
     * </ul>
     * When there are 3 "values" the following are supported usages:-
     * <ul>
     * <li><b>foreign-keys</b> : return all foreign keys for a Table, where the catalog+schema+table is passed in.
     *     Returns an RDBMSTableFKInfo</li>
     * <li><b>primary-keys</b> : return all primary keys for a Table, where the catalog+schema+table is passed in.
     *     Returns an RDBMSTablePFKInfo</li>
     * <li><b>indices</b> : return all indices for a Table, where the catalog+schema+table is passed in.
     *     Returns an RDBMSTableIndexInfo</li>
     * <li><b>columns</b> : return all columns for a Table, where the catalog+schema+table is passed in.
     *     Returns an RDBMSTableInfo.</li> 
     * </ul>
     * @param connection Connection to the datastore
     * @param name Name of the schema component to return.
     * @param values Value(s) to use as qualifier(s) for selecting the schema component
     * @return Schema data definition for this name
     */
    public StoreSchemaData getSchemaData(Object connection, String name, Object[] values)
    {
        if (values == null)
        {
            if (name.equalsIgnoreCase("types"))
            {
                // Types information
                StoreSchemaData info = schemaDataByName.get("types");
                if (info == null)
                {
                    // No types info defined yet so load it
                    info = getRDBMSTypesInfo((Connection)connection);
                }
                return info;
            }
            else if (name.equalsIgnoreCase("tables"))
            {
                // Tables-columns information
                StoreSchemaData info = schemaDataByName.get("tables");
                if (info == null)
                {
                    // TODO Initialise tables if not yet defined ?
                }
                return info;
            }
            else
            {
                throw new NucleusException("Attempt to get schema information for component " + name +
                    " but this is not supported by RDBMSSchemaHandler");
            }
        }
        else if (values.length == 1)
        {
            if (name.equalsIgnoreCase("foreign-keys") && values[0] instanceof Table)
            {
                // Get Foreign keys for a table, where the value is a Table
                return getRDBMSTableFKInfoForTable((Connection)connection, (Table)values[0]);
            }
            else if (name.equalsIgnoreCase("primary-keys") && values[0] instanceof Table)
            {
                // Get Primary keys for a table, where the value is a Table
                return getRDBMSTablePKInfoForTable((Connection)connection, (Table)values[0]);
            }
            else if (name.equalsIgnoreCase("indices") && values[0] instanceof Table)
            {
                // Get Indices for a table, where the value is a Table
                return getRDBMSTableIndexInfoForTable((Connection)connection, (Table)values[0]);
            }
            else if (name.equalsIgnoreCase("columns") && values[0] instanceof Table)
            {
                // Get columns for a table, where the value is a Table
                return getRDBMSTableInfoForTable((Connection)connection, (Table)values[0]);
            }
            else
            {
                return getSchemaData(connection, name, null);
            }
        }
        else if (values.length == 2)
        {
            if (name.equalsIgnoreCase("tables"))
            {
                // Get all tables for the specified catalog/schema (value is catalog name, value2 is schema name)
                return getRDBMSSchemaInfoForCatalogSchema((Connection)connection, (String)values[0], 
                    (String)values[1]);
            }
            if (name.equalsIgnoreCase("column") && values[0] instanceof Table && values[1] instanceof String)
            {
                // Get column info for specified column of a table (value is table, value2 is column name)
                return getRDBMSColumnInfoForColumn((Connection)connection, (Table)values[0], (String)values[1]);
            }
            else
            {
                return getSchemaData(connection, name, null);
            }
        }
        else if (values.length == 3)
        {
            if (name.equalsIgnoreCase("columns") && 
                values[0] instanceof String && values[1] instanceof String && values[2] instanceof String)
            {
                // Get column info for catalog + schema + tableName
                return getRDBMSTableInfoForTable((Connection)connection, 
                    (String)values[0], (String)values[1], (String)values[2]);
            }
            else if (name.equalsIgnoreCase("indices") && 
                values[0] instanceof String && values[1] instanceof String && values[2] instanceof String)
            {
                // Get index info for catalog + schema + tableName
                return getRDBMSTableIndexInfoForTable((Connection)connection, 
                    (String)values[0], (String)values[1], (String)values[2]);
            }
            else if (name.equalsIgnoreCase("primary-keys") && 
                values[0] instanceof String && values[1] instanceof String && values[2] instanceof String)
            {
                // Get PK info for catalog + schema + tableName
                return getRDBMSTablePKInfoForTable((Connection)connection, 
                    (String)values[0], (String)values[1], (String)values[2]);
            }
            else if (name.equalsIgnoreCase("foreign-keys") && 
                values[0] instanceof String && values[1] instanceof String && values[2] instanceof String)
            {
                // Get FK info for catalog + schema + tableName
                return getRDBMSTableFKInfoForTable((Connection)connection, 
                    (String)values[0], (String)values[1], (String)values[2]);
            }
        }

        throw new NucleusException("Attempt to get schema information for component " + name +
            " but this is not supported by RDBMSSchemaHandler");
    }

    /**
     * Accessor for the StoreManager we handle the schema for.
     * @return Store Manager.
     */
    public StoreManager getStoreManager()
    {
        return storeMgr;
    }

    /**
     * Returns the type of a database table/view in the datastore.
     * Uses DatabaseMetaData.getTables() to extract this information.
     * @param conn Connection to the database.
     * @param table The table/view
     * @return The table type (consistent with the return from DatabaseMetaData.getTables())
     * @throws NucleusDataStoreException if an error occurs obtaining the information
     */
    public String getTableType(Connection conn, Table table)
    throws SQLException
    {
        String tableType = null;

        // Calculate the catalog/schema names since we need to search fully qualified
        RDBMSAdapter dba = (RDBMSAdapter)storeMgr.getDatastoreAdapter();
        String[] c = splitTableIdentifierName(dba.getCatalogSeparator(), table.getIdentifier().getIdentifierName());
        String catalogName = table.getCatalogName();
        String schemaName = table.getSchemaName();
        String tableName = table.getIdentifier().getIdentifierName();
        if (c[0] != null)
        {
            catalogName = c[0];
        }
        if (c[1] != null)
        {
            schemaName = c[1];
        }
        if (c[2] != null)
        {
            tableName = c[2];
        }
        catalogName = getIdentifierForUseWithDatabaseMetaData(catalogName);
        schemaName = getIdentifierForUseWithDatabaseMetaData(schemaName);
        tableName = getIdentifierForUseWithDatabaseMetaData(tableName);

        try
        {
            ResultSet rs = conn.getMetaData().getTables(catalogName, schemaName, tableName, null);
            try
            {
                boolean insensitive = identifiersCaseInsensitive();
                while (rs.next())
                {
                    if ((insensitive && tableName.equalsIgnoreCase(rs.getString(3))) ||
                        (!insensitive && tableName.equals(rs.getString(3))))
                    {
                        tableType = rs.getString(4).toUpperCase();
                        break;
                    }
                }
            }
            finally
            {
                rs.close();
            }
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(
                "Exception thrown finding table type using DatabaseMetaData.getTables()", sqle);
        }

        return tableType;
    }

    /**
     * Convenience method to read and cache the types information for this datastore.
     * @param conn Connection to the datastore
     * @return The RDBMSTypesInfo
     */
    protected RDBMSTypesInfo getRDBMSTypesInfo(Connection conn)
    {
        RDBMSTypesInfo info = new RDBMSTypesInfo();
        try
        {
            if (conn == null)
            {
                // No connection provided so nothing to return
                return null;
            }

            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getTypeInfo();
            try
            {
                DatabaseAdapter dba = (DatabaseAdapter)storeMgr.getDatastoreAdapter();
                while (rs.next())
                {
                    SQLTypeInfo sqlType = dba.newSQLTypeInfo(rs);
                    if (sqlType != null)
                    {
                        String key = "" + sqlType.getDataType();
                        JDBCTypeInfo jdbcType = (JDBCTypeInfo)info.getChild(key);
                        if (jdbcType == null)
                        {
                            // New SQL type for new JDBC type
                            jdbcType = new JDBCTypeInfo(sqlType.getDataType());
                            jdbcType.addChild(sqlType);
                            info.addChild(jdbcType);
                        }
                        else
                        {
                            // New SQL type for existing JDBC type
                            jdbcType.addChild(sqlType);
                        }
                    }
                }
            }
            finally
            {
                rs.close();
            }
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(
                "Exception thrown retrieving type information from datastore", sqle);
        }

        // Cache it
        schemaDataByName.put("types", info);

        return info;        
    }

    /**
     * Convenience method to get the ForeignKey info for the specified table from the datastore.
     * @param conn Connection to use
     * @param table The table
     * @return The foreign key info
     */
    protected RDBMSTableFKInfo getRDBMSTableFKInfoForTable(Connection conn, Table table)
    {
        // Calculate the catalog/schema names since we need to search fully qualified
        RDBMSAdapter dba = (RDBMSAdapter)storeMgr.getDatastoreAdapter();
        String[] c = splitTableIdentifierName(dba.getCatalogSeparator(), table.getIdentifier().getIdentifierName());
        String catalogName = table.getCatalogName();
        String schemaName = table.getSchemaName();
        String tableName = table.getIdentifier().getIdentifierName();
        if (c[0] != null)
        {
            catalogName = c[0];
        }
        if (c[1] != null)
        {
            schemaName = c[1];
        }
        if (c[2] != null)
        {
            tableName = c[2];
        }
        catalogName = getIdentifierForUseWithDatabaseMetaData(catalogName);
        schemaName = getIdentifierForUseWithDatabaseMetaData(schemaName);
        tableName = getIdentifierForUseWithDatabaseMetaData(tableName);

        return getRDBMSTableFKInfoForTable(conn, catalogName, schemaName, tableName);
    }

    /**
     * Convenience method to get the ForeignKey info for the specified table from the datastore.
     * @param conn Connection to use
     * @param catalogName Catalog
     * @param schemaName Schema
     * @param tableName Name of the table
     * @return The foreign key info
     */
    protected RDBMSTableFKInfo getRDBMSTableFKInfoForTable(Connection conn,
            String catalogName, String schemaName, String tableName)
    {
        // We don't cache FK info, so retrieve it directly
        RDBMSTableFKInfo info = new RDBMSTableFKInfo(catalogName, schemaName, tableName);

        RDBMSAdapter dba = (RDBMSAdapter)storeMgr.getDatastoreAdapter();
        try
        {
            ResultSet rs = conn.getMetaData().getImportedKeys(catalogName, schemaName, tableName);
            try
            {
                while (rs.next())
                {
                    ForeignKeyInfo fki = dba.newFKInfo(rs);
                    if (!info.getChildren().contains(fki))
                    {
                        // Ignore any duplicate FKs
                        info.addChild(fki);
                    }
                }
            }
            finally
            {
                rs.close();
            }
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(
                "Exception thrown while querying foreign keys for table=" + tableName, sqle);
        }
        return info;
    }

    /**
     * Convenience method to get the PrimaryKey info for the specified table from the datastore.
     * @param conn Connection to use
     * @param table The table
     * @return The primary key info
     */
    protected RDBMSTablePKInfo getRDBMSTablePKInfoForTable(Connection conn, Table table)
    {
        // Calculate the catalog/schema names since we need to search fully qualified
        RDBMSAdapter dba = (RDBMSAdapter)storeMgr.getDatastoreAdapter();
        String[] c = splitTableIdentifierName(dba.getCatalogSeparator(), table.getIdentifier().getIdentifierName());
        String catalogName = table.getCatalogName();
        String schemaName = table.getSchemaName();
        String tableName = table.getIdentifier().getIdentifierName();
        if (c[0] != null)
        {
            catalogName = c[0];
        }
        if (c[1] != null)
        {
            schemaName = c[1];
        }
        if (c[2] != null)
        {
            tableName = c[2];
        }
        catalogName = getIdentifierForUseWithDatabaseMetaData(catalogName);
        schemaName = getIdentifierForUseWithDatabaseMetaData(schemaName);
        tableName = getIdentifierForUseWithDatabaseMetaData(tableName);

        return getRDBMSTablePKInfoForTable(conn, catalogName, schemaName, tableName);
    }

    /**
     * Convenience method to get the PrimaryKey info for the specified table from the datastore.
     * @param conn Connection to use
     * @param catalogName Catalog
     * @param schemaName Schema
     * @param tableName Name of the table
     * @return The primary key info
     */
    protected RDBMSTablePKInfo getRDBMSTablePKInfoForTable(Connection conn,
            String catalogName, String schemaName, String tableName)
    {
        // We don't cache PK info, so retrieve it directly
        RDBMSTablePKInfo info = new RDBMSTablePKInfo(catalogName, schemaName, tableName);

        try
        {
            ResultSet rs = conn.getMetaData().getPrimaryKeys(catalogName, schemaName, tableName);
            try
            {
                while (rs.next())
                {
                    PrimaryKeyInfo pki = new PrimaryKeyInfo(rs);
                    if (!info.getChildren().contains(pki))
                    {
                        // Ignore any duplicate PKs
                        info.addChild(pki);
                    }
                }
            }
            finally
            {
                rs.close();
            }
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(
                "Exception thrown while querying primary keys for table=" + tableName, sqle);
        }
        return info;
    }

    /**
     * Convenience method to get the index info for the specified table from the datastore.
     * Returns ALL indexes regardless of whether unique or not.
     * @param conn Connection to use
     * @param table The table
     * @return The index info
     */
    protected RDBMSTableIndexInfo getRDBMSTableIndexInfoForTable(Connection conn, Table table)
    {
        // Calculate the catalog/schema names since we need to search fully qualified
        RDBMSAdapter dba = (RDBMSAdapter)storeMgr.getDatastoreAdapter();
        String[] c = splitTableIdentifierName(dba.getCatalogSeparator(), 
            table.getIdentifier().getIdentifierName());
        String catalogName = table.getCatalogName();
        String schemaName = table.getSchemaName();
        String tableName = table.getIdentifier().getIdentifierName();
        if (c[0] != null)
        {
            catalogName = c[0];
        }
        if (c[1] != null)
        {
            schemaName = c[1];
        }
        if (c[2] != null)
        {
            tableName = c[2];
        }
        catalogName = getIdentifierForUseWithDatabaseMetaData(catalogName);
        schemaName = getIdentifierForUseWithDatabaseMetaData(schemaName);
        tableName = getIdentifierForUseWithDatabaseMetaData(tableName);

        return getRDBMSTableIndexInfoForTable(conn, catalogName, schemaName, tableName);
    }

    /**
     * Convenience method to get the index info for the catalog+schema+tableName in the datastore.
     * Returns ALL indexes regardless of whether unique or not.
     * @param conn Connection to use
     * @param catalogName Catalog
     * @param schemaName Schema
     * @param tableName Name of the table
     * @return The index info
     */
    protected RDBMSTableIndexInfo getRDBMSTableIndexInfoForTable(Connection conn, 
            String catalogName, String schemaName, String tableName)
    {
        // We don't cache Index info, so retrieve it directly
        RDBMSTableIndexInfo info = new RDBMSTableIndexInfo(catalogName, schemaName, tableName);
        RDBMSAdapter dba = (RDBMSAdapter)storeMgr.getDatastoreAdapter();
        try
        {
            // Note : the table name has no quotes here.
            String schemaNameTmp = schemaName;
            if (schemaName == null && storeMgr.getSchemaName() != null)
            {
                // This is a hack for the DatabaseAdapter method that requires a schema for Oracle
                schemaNameTmp = storeMgr.getSchemaName();
                schemaNameTmp = getIdentifierForUseWithDatabaseMetaData(schemaNameTmp);
            }
            ResultSet rs = dba.getExistingIndexes(conn, catalogName, schemaNameTmp, tableName);
            if (rs == null)
            {
                rs = conn.getMetaData().getIndexInfo(catalogName, schemaName, tableName, false, true);
            }
            try
            {
                while (rs.next())
                {
                    IndexInfo idxInfo = new IndexInfo(rs);
                    if (!info.getChildren().contains(idxInfo))
                    {
                        // Ignore any duplicate indices
                        info.addChild(idxInfo);
                    }
                }
            }
            finally
            {
                if (rs != null)
                {
                    Statement st = rs.getStatement();
                    rs.close();
                    if (st != null)
                    {
                        st.close();
                    }
                }
            }
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(
                "Exception thrown while querying indices for table=" + tableName, sqle);
        }

        return info;
    }

    /**
     * Convenience method to retrieve schema information for all tables in the specified catalog/schema.
     * @param conn Connection
     * @param catalog Catalog
     * @param schema Schema
     * @return Schema information
     */
    protected RDBMSSchemaInfo getRDBMSSchemaInfoForCatalogSchema(Connection conn, String catalog, 
            String schema)
    {
        if (storeMgr.getBooleanProperty("datanucleus.rdbms.omitDatabaseMetaDataGetColumns"))
        {
            // User has requested to omit calls to DatabaseMetaData.getColumns due to JDBC ineptness
            return null;
        }

        RDBMSSchemaInfo schemaInfo = new RDBMSSchemaInfo(catalog, schema);
        ResultSet rs = null;
        try
        {
            String catalogName = getIdentifierForUseWithDatabaseMetaData(catalog);
            String schemaName = getIdentifierForUseWithDatabaseMetaData(schema);
            rs = ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getColumns(conn, 
                catalogName, schemaName, null, null);
            while (rs.next())
            {
                // Construct a fully-qualified name for the table in this row of the ResultSet
                String colCatalogName = rs.getString(1);
                String colSchemaName = rs.getString(2);
                String colTableName = rs.getString(3);
                if (StringUtils.isWhitespace(colTableName))
                {
                    // JDBC driver should return the table name as a minimum
                    // TODO Localise
                    throw new NucleusDataStoreException(
                        "Invalid 'null' table name identifier returned by database. " +
                        "Check with your JDBC driver vendor (ref:DatabaseMetaData.getColumns).");
                }
                if (rs.wasNull() || (colCatalogName != null && colCatalogName.length() < 1))
                {
                    colCatalogName = null;
                }
                if (rs.wasNull() || (colSchemaName != null && colSchemaName.length() < 1))
                {
                    colSchemaName = null;
                }

                String tableKey = getTableKeyInRDBMSSchemaInfo(catalog, schema, colTableName);
                RDBMSTableInfo table = (RDBMSTableInfo)schemaInfo.getChild(tableKey);
                if (table == null)
                {
                    // No current info for table so add it
                    table = new RDBMSTableInfo(colCatalogName, colSchemaName, colTableName);
                    table.addProperty("table_key", tableKey);
                    schemaInfo.addChild(table);
                }

                RDBMSColumnInfo col = ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).newRDBMSColumnInfo(rs);
                table.addChild(col);
            }
        }
        catch (SQLException sqle)
        {
            // TODO Localise
            throw new NucleusDataStoreException(
                "Exception thrown obtaining schema column information from datastore", sqle);
        }
        finally
        {
            try
            {
                if (rs != null)
                {
                    Statement stmt = rs.getStatement();
                    rs.close();
                    if (stmt != null)
                    {
                        stmt.close();
                    }
                }
            }
            catch (SQLException sqle)
            {
                // TODO Localise
                throw new NucleusDataStoreException(
                    "Exception thrown closing results of DatabaseMetaData.getColumns()", sqle);
            }
        }

        return schemaInfo;
    }

    /**
     * Convenience method to get the column info for the specified table from the datastore.
     * @param conn Connection to use
     * @param table The table
     * @return The table info containing the columns
     */
    protected RDBMSTableInfo getRDBMSTableInfoForTable(Connection conn, Table table)
    {
        String[] c = splitTableIdentifierName(((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getCatalogSeparator(), table.getIdentifier().getIdentifierName());
        String catalogName = table.getCatalogName();
        String schemaName = table.getSchemaName();
        String tableName = table.getIdentifier().getIdentifierName();
        if (c[0] != null)
        {
            catalogName = c[0];
        }
        if (c[1] != null)
        {
            schemaName = c[1];
        }
        if (c[2] != null)
        {
            tableName = c[2];
        }
        catalogName = getIdentifierForUseWithDatabaseMetaData(catalogName);
        schemaName = getIdentifierForUseWithDatabaseMetaData(schemaName);
        tableName = getIdentifierForUseWithDatabaseMetaData(tableName);

        return getRDBMSTableInfoForTable(conn, catalogName, schemaName, tableName);
    }

    /**
     * Convenience method to get the column info for the catalog+schema+tableName in the datastore.
     * @param conn Connection to use
     * @param catalogName Catalog
     * @param schemaName Schema
     * @param tableName Name of the table
     * @return The table info containing the columns
     */
    protected RDBMSTableInfo getRDBMSTableInfoForTable(Connection conn, String catalogName,
            String schemaName, String tableName)
    {
        RDBMSSchemaInfo info = (RDBMSSchemaInfo)getSchemaData(conn, "tables", null);
        if (info == null)
        {
            // No schema info defined yet
            info = new RDBMSSchemaInfo(storeMgr.getCatalogName(), storeMgr.getSchemaName());
            schemaDataByName.put("tables", info);
        }

        // Check existence
        String tableKey = getTableKeyInRDBMSSchemaInfo(catalogName, schemaName, tableName);
        RDBMSTableInfo tableInfo = (RDBMSTableInfo)info.getChild(tableKey);
        if (tableInfo != null)
        {
            long time = ((Long)tableInfo.getProperty("time")).longValue();
            long now = System.currentTimeMillis();
            if (now < time + COLUMN_INFO_EXPIRATION_MS)
            {
                // Table info is still valid so just return it
                return tableInfo;
            }
        }

        // Refresh all existing tables plus this requested one
        boolean insensitiveIdentifiers = identifiersCaseInsensitive();
        Collection tableNames = new HashSet();
        Collection tables = storeMgr.getManagedTables(catalogName, schemaName);
        if (tables.size() > 0)
        {
            Iterator iter = tables.iterator();
            while (iter.hasNext())
            {
                Table tbl = (Table)iter.next();
                tableNames.add(insensitiveIdentifiers ? tbl.getIdentifier().getIdentifierName().toLowerCase() : tbl.getIdentifier().getIdentifierName());
            }
        }
        tableNames.add(insensitiveIdentifiers ? tableName.toLowerCase() : tableName);

        refreshTableData(conn, catalogName, schemaName, tableNames);

        tableInfo = (RDBMSTableInfo)info.getChild(tableKey);
        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            if (tableInfo == null || tableInfo.getNumberOfChildren() == 0)
            {
                NucleusLogger.DATASTORE_SCHEMA.info(LOCALISER.msg("050030", tableName));
            }
            else
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("050032", 
                    tableName, "" + tableInfo.getNumberOfChildren()));
            }
        }
        return tableInfo;
    }

    /**
     * Convenience method to get the column info from the datastore for the column in the specified table.
     * @param conn Connection to use
     * @param table The table
     * @param columnName Name of the column
     * @return The column info for the table+column
     */
    protected RDBMSColumnInfo getRDBMSColumnInfoForColumn(Connection conn, Table table, String columnName)
    {
        RDBMSColumnInfo colInfo = null;

        RDBMSTableInfo tableInfo = getRDBMSTableInfoForTable(conn, table);
        if (tableInfo != null)
        {
            colInfo = (RDBMSColumnInfo)tableInfo.getChild(columnName);
            if (colInfo == null)
            {
                // We have the table but this column isn't present!
                // TODO Try retrieving from the datastore. Maybe recently added
            }
        }

        return colInfo;
    }

    /**
     * Convenience method for refreshing the table-column information for the tables specified
     * within the defined catalog/schema.
     * @param connection Connection to the datastore
     * @param catalog Catalog to refresh
     * @param schema Schema to refresh
     * @param tables Collection of table names (String) to refresh
     */
    private void refreshTableData(Object connection, String catalog, String schema, Collection tableNames)
    {
        if (storeMgr.getBooleanProperty("datanucleus.rdbms.omitDatabaseMetaDataGetColumns"))
        {
            // User has requested to omit calls to DatabaseMetaData.getColumns due to JDBC ineptness
            return;
        }
        if (tableNames == null || tableNames.size() == 0)
        {
            return;
        }

        RDBMSSchemaInfo info = (RDBMSSchemaInfo)getSchemaData(connection, "tables", null);
        if (info == null)
        {
            info = new RDBMSSchemaInfo(storeMgr.getCatalogName(), storeMgr.getSchemaName());
            schemaDataByName.put("tables", info);
        }

        // Get timestamp to mark the tables that are refreshed
        Long now = Long.valueOf(System.currentTimeMillis());

        // Retrieve all column info for the required catalog/schema
        ResultSet rs = null;
        HashSet tablesProcessed = new HashSet();
        try
        {
            Connection conn = (Connection)connection;
            String catalogName = getIdentifierForUseWithDatabaseMetaData(catalog);
            String schemaName = getIdentifierForUseWithDatabaseMetaData(schema);
            if (tableNames.size() == 1)
            {
                // Single table to retrieve so restrict the query
                String tableName = getIdentifierForUseWithDatabaseMetaData((String)tableNames.iterator().next());
                if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("050028", 
                        tableName, catalogName, schemaName));
                }
                rs = ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getColumns(conn, 
                    catalogName, schemaName, tableName, null);
            }
            else
            {
                // Multiple tables so just retrieve all for this catalog/schema
                if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("050028", 
                        StringUtils.collectionToString(tableNames), catalogName, schemaName));
                }
                rs = ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getColumns(conn, 
                    catalogName, schemaName, null, null);
            }

            boolean insensitiveIdentifiers = identifiersCaseInsensitive();
            while (rs.next())
            {
                // Construct a fully-qualified name for the table in this row of the ResultSet
                String colCatalogName = rs.getString(1);
                String colSchemaName = rs.getString(2);
                String colTableName = rs.getString(3);
                if (StringUtils.isWhitespace(colTableName))
                {
                    // JDBC driver should return the table name as a minimum
                    // TODO Localise
                    throw new NucleusDataStoreException(
                        "Invalid 'null' table name identifier returned by database. " +
                        "Check with your JDBC driver vendor (ref:DatabaseMetaData.getColumns).");
                }

                if (rs.wasNull() || (colCatalogName != null && colCatalogName.length() < 1))
                {
                    colCatalogName = null;
                }
                if (rs.wasNull() || (colSchemaName != null && colSchemaName.length() < 1))
                {
                    colSchemaName = null;
                }

                String colTableNameToCheck = colTableName;
                if (insensitiveIdentifiers)
                {
                    // Cater for case-insensitive identifiers
                    colTableNameToCheck = colTableName.toLowerCase();
                }
                if (tableNames.contains(colTableNameToCheck))
                {
                    // Required table, so refresh/add it
                    String tableKey = getTableKeyInRDBMSSchemaInfo(catalog, schema, colTableName);
                    RDBMSTableInfo table = (RDBMSTableInfo)info.getChild(tableKey);
                    if (tablesProcessed.add(tableKey))
                    {
                        // Table met for first time in this refresh
                        if (table == null)
                        {
                            // No current info for table
                            table = new RDBMSTableInfo(colCatalogName, colSchemaName, colTableName);
                            table.addProperty("table_key", tableKey);
                            info.addChild(table);
                        }
                        else
                        {
                            // Current info, so clean out columns
                            table.clearChildren();
                        }
                        table.addProperty("time", now);
                    }

                    RDBMSColumnInfo col = 
                        ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).newRDBMSColumnInfo(rs);
                    table.addChild(col);
                }
            }
        }
        catch (SQLException sqle)
        {
            // TODO Localise
            throw new NucleusDataStoreException(
                "Exception thrown obtaining schema column information from datastore", sqle);
        }
        finally
        {
            try
            {
                if (rs != null)
                {
                    Statement stmt = rs.getStatement();
                    rs.close();
                    if (stmt != null)
                    {
                        stmt.close();
                    }
                }
            }
            catch (SQLException sqle)
            {
                // TODO Localise
                throw new NucleusDataStoreException(
                    "Exception thrown closing results of DatabaseMetaData.getColumns()", sqle);
            }
        }

        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("050029", catalog, schema,
                "" + tablesProcessed.size(), "" + (System.currentTimeMillis() - now.longValue())));
        }
    }

    /**
     * Convenience accessor for the key that we use to store a tables information in RDBMSSchemaInfo.
     * @param catalog The catalog name
     * @param schema The schema name
     * @param table The table name
     * @return Its key (fully-qualified table name)
     */
    private String getTableKeyInRDBMSSchemaInfo(String catalog, String schema, String table)
    {
        DatastoreIdentifier fullyQualifiedTableName = 
            storeMgr.getIdentifierFactory().newDatastoreContainerIdentifier(table);
        fullyQualifiedTableName.setCatalogName(catalog);
        fullyQualifiedTableName.setSchemaName(schema);
        return fullyQualifiedTableName.getFullyQualifiedName(true);
    }

    /**
     * Method to split a fully-qualified database table name into its
     * constituent parts (CATALOG.SCHEMA.TABLE). This is typically used where a user
     * has specified a table name as fully qualified in the MetaData.
     * @param separator Separator character
     * @param name The fully qualified name.
     * @return The separated parts of the name (catalog, schema, table)
     **/
    private static String[] splitTableIdentifierName(String separator, String name)
    {
        String[] result = new String[3];

        int p = name.indexOf(separator);
        if (p < 0)
        {
            // Table name specified
            result[2] = name;
        }
        else
        {
            int p1 = name.indexOf(separator, p + separator.length());
            if (p1 < 0)
            {
                // Schema and Table name specified
                // TODO What if the RDBMS only supports catalog ... this should be the catalog here!!
                result[1] = name.substring(0, p);
                result[2] = name.substring(p + separator.length());
            }
            else
            {
                // Catalog, Schema and Table name specified
                result[0] = name.substring(0, p);
                result[1] = name.substring(p + separator.length(), p1);
                result[2] = name.substring(p1 + separator.length());
            }
        }
        if (result[1] != null &&
            result[1].length() < 1)
        {
            result[1] = null;
        }
        if (result[0] != null &&
            result[0].length() < 1)
        {
            result[0] = null;
        }
        return result;
    }

    /**
     * Convenience method to convert the passed identifier into the correct case for use with this
     * datastore adapter, and removing any quote characters.
     * @param identifier The raw identifier
     * @return The identifier for use
     */
    private String getIdentifierForUseWithDatabaseMetaData(String identifier)
    {
        if (identifier == null)
        {
            return null;
        }
        return identifier.replace(storeMgr.getDatastoreAdapter().getIdentifierQuoteString(), "");
        // TODO Really ought to do the case conversion so that we check in the case of the adapter
        // This is needed where the user has provided an identifier but in the wrong case
        // When you enable this the JDO2 TCK will likely go incredibly slow since Derby use of
        // DatabaseMetaData.getColumns() see "http://issues.apache.org/jira/browse/DERBY-1996"
/*        return JDBCUtils.getIdentifierNameStripped(
            storeMgr.getIdentifierFactory().getIdentifierInAdapterCase(identifier), 
            storeMgr.getDatastoreAdapter());*/
    }

    /**
     * Convenience method to return if identifiers for this datastore should be treated as case insensitive.
     * For example MySQL on Linux supports case-sensitive, whereas MySQL on Windows is case insensitive.
     * @return Whether case insensitive.
     */
    private boolean identifiersCaseInsensitive()
    {
        DatastoreAdapter dba = storeMgr.getDatastoreAdapter();
        if (!dba.supportsOption(RDBMSAdapter.IDENTIFIERS_MIXEDCASE_SENSITIVE) &&
            !dba.supportsOption(RDBMSAdapter.IDENTIFIERS_MIXEDCASE_QUOTED_SENSITIVE))
        {
            return true;
        }
        return false;
    }
}