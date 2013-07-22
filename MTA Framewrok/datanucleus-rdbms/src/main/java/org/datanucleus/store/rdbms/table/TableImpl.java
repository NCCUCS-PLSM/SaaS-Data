/**********************************************************************
Copyright (c) 2002 Mike Martin and others. All rights reserved.
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
2003 Erik Bengtson  - removed unused variable
2003 Andy Jefferson - localised
2003 Andy Jefferson - addition of checkExists, made getExpectedPK protected
2004 Andy Jefferson - rewritten to remove many levels of inheritance
2004 Andy Jefferson - added validation of columns, and auto create option
2004 Andy Jefferson - separated out validatePK
2008 Andy Jefferson - changed all schema accessors to use StoreSchemaHandler
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.store.exceptions.NoTableManagedException;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.IdentifierFactory;
import org.datanucleus.store.mapped.IdentifierType;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.exceptions.MissingColumnException;
import org.datanucleus.store.rdbms.exceptions.MissingTableException;
import org.datanucleus.store.rdbms.exceptions.NotATableException;
import org.datanucleus.store.rdbms.exceptions.UnexpectedColumnException;
import org.datanucleus.store.rdbms.exceptions.WrongPrimaryKeyException;
import org.datanucleus.store.rdbms.key.CandidateKey;
import org.datanucleus.store.rdbms.key.ForeignKey;
import org.datanucleus.store.rdbms.key.Index;
import org.datanucleus.store.rdbms.key.PrimaryKey;
import org.datanucleus.store.rdbms.schema.ForeignKeyInfo;
import org.datanucleus.store.rdbms.schema.IndexInfo;
import org.datanucleus.store.rdbms.schema.PrimaryKeyInfo;
import org.datanucleus.store.rdbms.schema.RDBMSColumnInfo;
import org.datanucleus.store.rdbms.schema.RDBMSSchemaHandler;
import org.datanucleus.store.rdbms.schema.RDBMSTableFKInfo;
import org.datanucleus.store.rdbms.schema.RDBMSTableIndexInfo;
import org.datanucleus.store.rdbms.schema.RDBMSTablePKInfo;
import org.datanucleus.store.schema.StoreSchemaHandler;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Class representing a table in a datastore (RDBMS).
 * Provides a series of methods for validating the aspects of the table, namely
 * <UL>
 * <LI>validate - Validate the table</LI>
 * <LI>validateColumns - Validate the columns in the table</LI>
 * <LI>validatePrimaryKey - Validate the primary key</LI>
 * <LI>validateIndices - Validate the indices on the table</LI>
 * <LI>validateForeignKeys - Validate all FKs for the table</LI>
 * <LI>validateConstraints - Validate the indices and FKs.</LI>
 * </UL>
 */
public abstract class TableImpl extends AbstractTable
{
    /**
     * Constructor.
     * @param name The name of the table (in SQL).
     * @param storeMgr The StoreManager for this table.
     */
    public TableImpl(DatastoreIdentifier name, RDBMSStoreManager storeMgr)
    {
        super(name, storeMgr);
    }

    /**
     * Pre-initilize. For things that must be initialized right after constructor 
     * @param clr the ClassLoaderResolver
     */
    public void preInitialize(final ClassLoaderResolver clr)
    {
        assertIsUninitialized();
        //nothing to do here
    }    

    /**
     * Post initilize. For things that must be set after all classes have been initialized before 
     * @param clr the ClassLoaderResolver
     */
    public void postInitialize(final ClassLoaderResolver clr)
    {
        assertIsInitialized();
        //nothing to do here
    }    

    /**
     * Accessor for the primary key for this table. 
     * Will always return a PrimaryKey but if we have defined no columns,
     * the pk.size() will be 0.
     * @return The primary key.
     */
    public PrimaryKey getPrimaryKey()
    {
        PrimaryKey pk = new PrimaryKey(this);
        Iterator i = columns.iterator();
        while (i.hasNext())
        {
            Column col = (Column) i.next();
            if (col.isPrimaryKey())
            {
                pk.addDatastoreField(col);
            }
        }

        return pk;
    }

    /**
     * Method to validate the table in the datastore.
     * @param conn The JDBC Connection
     * @param validateColumnStructure Whether to validate the column structure, or just the column existence
     * @param autoCreate Whether to update the table to fix any validation errors. Only applies to missing columns.
     * @param autoCreateErrors Exceptions found in the "auto-create" process
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    public boolean validate(Connection conn, boolean validateColumnStructure, boolean autoCreate, Collection autoCreateErrors)
    throws SQLException
    {
        assertIsInitialized();

        // Check existence and validity
        RDBMSSchemaHandler handler = (RDBMSSchemaHandler)storeMgr.getSchemaHandler();
        String tableType = handler.getTableType(conn, this);
        if (tableType == null)
        {
            throw new MissingTableException(getCatalogName(), getSchemaName(), this.toString());
        }
        else if (!tableType.equals("TABLE"))
        {
            throw new NotATableException(this.toString(), tableType);
        }

        long startTime = System.currentTimeMillis();
        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("057032", this));
        }

        // Validate the column(s)
        validateColumns(conn, validateColumnStructure, autoCreate, autoCreateErrors);

        // Validate the primary key(s)
        try
        {
            validatePrimaryKey(conn);
        }
        catch (WrongPrimaryKeyException wpke)
        {
            if (autoCreateErrors != null)
            {
                autoCreateErrors.add(wpke);
            }
            else
            {
                throw wpke;
            }
        }

        state = TABLE_STATE_VALIDATED;
        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("045000", (System.currentTimeMillis() - startTime)));
        }

        return false;
    }

    /**
     * Utility to validate the columns of the table.
     * Will throw a MissingColumnException if a column is not found (and
     * is not required to auto create it)
     * @param conn Connection to use for validation
     * @param validateColumnStructure Whether to validate down to the structure of the columns, or just their existence
     * @param autoCreate Whether to auto create any missing columns
     * @param autoCreateErrors Exceptions found in the "auto-create" process
     * @return Whether it validates
     * @throws SQLException Thrown if an error occurs in the validation process
     */
    public boolean validateColumns(Connection conn, boolean validateColumnStructure, boolean autoCreate, Collection autoCreateErrors)
    throws SQLException
    {
        HashMap unvalidated = new HashMap(columnsByName);
        List tableColInfo = storeMgr.getColumnInfoForTable(this, conn);
        Iterator i = tableColInfo.iterator();
        while (i.hasNext())
        {
            RDBMSColumnInfo ci = (RDBMSColumnInfo) i.next();

            // Create an identifier to use for the real column - use "CUSTOM" because we dont want truncation
            DatastoreIdentifier colName = storeMgr.getIdentifierFactory().newDatastoreFieldIdentifier(ci.getColumnName(), 
                this.storeMgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(String.class), FieldRole.ROLE_CUSTOM);
            Column col = (Column) unvalidated.get(colName);
            if (col != null)
            {
                if (validateColumnStructure)
                {
                    col.initializeColumnInfoFromDatastore(ci);
                    col.validate(ci);
                    unvalidated.remove(colName);
                }
                else
                {
                    unvalidated.remove(colName);
                }
            }
        }

        if (unvalidated.size() > 0)
        {
            if (autoCreate)
            {
                // Add all missing columns
                List stmts = new ArrayList();
                Set columnKeys = unvalidated.entrySet();
                Iterator<Map.Entry> columnsIter = columnKeys.iterator();
                while (columnsIter.hasNext())
                {
                    Map.Entry entry = columnsIter.next();
                    Column col = (Column)entry.getValue();
                    String addColStmt = dba.getAddColumnStatement(this, col);
                    stmts.add(addColStmt);
                    NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("057031", col.getIdentifier(), this.toString()));
                }

                try
                {
                    executeDdlStatementList(stmts, conn);
                }
                catch (SQLException sqle)
                {
                    if (autoCreateErrors != null)
                    {
                        autoCreateErrors.add(sqle);
                    }
                    else
                    {
                        throw sqle;
                    }
                }

                // Invalidate the cached information for this table since it now has a new column
                storeMgr.invalidateColumnInfoForTable(this);
            }
            else
            {
                MissingColumnException mce = new MissingColumnException(this, unvalidated.values());
                if (autoCreateErrors != null)
                {
                    autoCreateErrors.add(mce);
                }
                else
                {
                    throw mce;
                }
            }
        }
        state = TABLE_STATE_VALIDATED;

        return true;
    }

    /**
     * Utility to load the structure/metadata of primary key columns of the table.
     * @param conn Connection to use for validation
     * @throws SQLException Thrown if an error occurs in the initialization process
     */
    public void initializeColumnInfoForPrimaryKeyColumns(Connection conn)
    throws SQLException
    {
        Iterator<Column> i = columnsByName.values().iterator();
        while (i.hasNext())
        {
            Column col = i.next();
            if (col.isPrimaryKey())
            {
                RDBMSColumnInfo ci = storeMgr.getColumnInfoForColumnName(this, conn, col.getIdentifier());
                if (ci != null)
                {
                    col.initializeColumnInfoFromDatastore(ci);
                }
            }
        }
    }

    /**
     * Initialize the default value for columns if null using the values from the datastore.
     * @param conn The JDBC Connection
     * @throws SQLException Thrown if an error occurs in the default initialisation.
     */
    public void initializeColumnInfoFromDatastore(Connection conn)
    throws SQLException
    {
        HashMap columns = new HashMap(columnsByName);
        Iterator i = storeMgr.getColumnInfoForTable(this, conn).iterator();
        while (i.hasNext())
        {
            RDBMSColumnInfo ci = (RDBMSColumnInfo) i.next();
            DatastoreIdentifier colName = storeMgr.getIdentifierFactory().newIdentifier(IdentifierType.COLUMN, ci.getColumnName());
            Column col = (Column) columns.get(colName);
            if (col != null)
            {
                col.initializeColumnInfoFromDatastore(ci);
            }
        }
    }

    /**
     * Utility method to validate the primary key of the table.
     * Will throw a WrongPrimaryKeyException if the PK is incorrect.
     * TODO Add an auto_create parameter on this
     * @param conn Connection to use
     * @return Whether it validates
     * @throws SQLException When an error occurs in the valdiation
     */
    protected boolean validatePrimaryKey(Connection conn)
    throws SQLException
    {
        Map actualPKs = getExistingPrimaryKeys(conn);
        PrimaryKey expectedPK = getPrimaryKey();
        if (expectedPK.size() == 0)
        {
            if (!actualPKs.isEmpty())
            {
                throw new WrongPrimaryKeyException(this.toString(), expectedPK.toString(), StringUtils.collectionToString(actualPKs.values()));
            }
        }
        else
        {
            if (actualPKs.size() != 1 ||
                !actualPKs.values().contains(expectedPK))
            {
                throw new WrongPrimaryKeyException(this.toString(), expectedPK.toString(), StringUtils.collectionToString(actualPKs.values()));
            }
        }

        return true;
    }

    /**
     * Method to validate any constraints, and auto create them if required.
     * @param conn The JDBC Connection
     * @param autoCreate Whether to auto create the constraints if not existing
     * @param autoCreateErrors Errors found in the "auto-create" process
     * @param clr The ClassLoaderResolver
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    public boolean validateConstraints(Connection conn, boolean autoCreate, Collection autoCreateErrors, ClassLoaderResolver clr)
    throws SQLException
    {
        assertIsInitialized();

        boolean cksWereModified;
        boolean fksWereModified;
        boolean idxsWereModified;
        if (dba.supportsOption(RDBMSAdapter.CREATE_INDEXES_BEFORE_FOREIGN_KEYS))
        {
            idxsWereModified = validateIndices(conn, autoCreate, autoCreateErrors, clr);
            fksWereModified = validateForeignKeys(conn, autoCreate, autoCreateErrors, clr);
            cksWereModified = validateCandidateKeys(conn, autoCreate, autoCreateErrors);
        }
        else
        {
            cksWereModified = validateCandidateKeys(conn, autoCreate, autoCreateErrors);
            fksWereModified = validateForeignKeys(conn, autoCreate, autoCreateErrors, clr);
            idxsWereModified = validateIndices(conn, autoCreate, autoCreateErrors, clr);
        }

        return fksWereModified || idxsWereModified || cksWereModified;
    }

    /**
     * Method used to create all constraints for a brand new table.
     * @param conn The JDBC Connection
     * @param autoCreateErrors Errors found in the "auto-create" process
     * @param clr The ClassLoaderResolver
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    public boolean createConstraints(Connection conn, Collection autoCreateErrors, ClassLoaderResolver clr)
    throws SQLException
    {
        assertIsInitialized();

        boolean cksWereModified;
        boolean fksWereModified;
        boolean idxsWereModified;
        if (dba.supportsOption(RDBMSAdapter.CREATE_INDEXES_BEFORE_FOREIGN_KEYS))
        {
            idxsWereModified = createIndices(conn, autoCreateErrors, clr, Collections.EMPTY_MAP);
            fksWereModified = createForeignKeys(conn, autoCreateErrors, clr, Collections.EMPTY_MAP);
            cksWereModified = createCandidateKeys(conn, autoCreateErrors, Collections.EMPTY_MAP);
        }
        else
        {
            cksWereModified = createCandidateKeys(conn, autoCreateErrors, Collections.EMPTY_MAP);
            fksWereModified = createForeignKeys(conn, autoCreateErrors, clr, Collections.EMPTY_MAP);
            idxsWereModified = createIndices(conn, autoCreateErrors, clr, Collections.EMPTY_MAP);
        }

        return fksWereModified || idxsWereModified || cksWereModified;
    }

    /**
     * Method to validate any foreign keys on this table in the datastore, and
     * auto create any that are missing where required.
     * 
     * @param conn The JDBC Connection
     * @param autoCreate Whether to auto create the FKs if not existing
     * @param autoCreateErrors Errors found during the auto-create process
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    private boolean validateForeignKeys(Connection conn, boolean autoCreate, Collection autoCreateErrors, ClassLoaderResolver clr)
    throws SQLException
    {
        boolean dbWasModified = false;

        // Validate and/or create all foreign keys.
        Map actualForeignKeysByName = null;
        int numActualFKs = 0;
        if (storeMgr.getCompleteDDL())
        {
            actualForeignKeysByName = new HashMap();
        }
        else
        {
            actualForeignKeysByName = getExistingForeignKeys(conn);
            numActualFKs = actualForeignKeysByName.size();
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058103", "" + numActualFKs, this));
            }
        }

        // Auto Create any missing foreign keys
        if (autoCreate)
        {
            dbWasModified = createForeignKeys(conn, autoCreateErrors, clr, actualForeignKeysByName);
        }
        else
        {
            Map stmtsByFKName = getSQLAddFKStatements(actualForeignKeysByName, clr);
            if (stmtsByFKName.isEmpty())
            {
                if (numActualFKs > 0)
                {
                    if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                    {
                        NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058104", "" + numActualFKs,this));
                    }
                }
            }
            else
            {
                // We support existing schemas so don't raise an exception.
                NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058101",
                    this, stmtsByFKName.values()));
            }
        }
        return dbWasModified;
    }

    /**
     * Method to create any foreign keys on this table in the datastore
     * 
     * @param conn The JDBC Connection
     * @param autoCreateErrors Errors found during the auto-create process
     * @param actualForeignKeysByName the current foreign keys 
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    private boolean createForeignKeys(Connection conn, Collection autoCreateErrors, ClassLoaderResolver clr, Map actualForeignKeysByName)
    throws SQLException
    {
        // Auto Create any missing foreign keys
        Map stmtsByFKName = getSQLAddFKStatements(actualForeignKeysByName, clr);
        Statement stmt = conn.createStatement();
        try
        {
            Iterator i = stmtsByFKName.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry e = (Map.Entry) i.next();
                String fkName = (String) e.getKey();
                String stmtText = (String) e.getValue();
                if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058100",
                        fkName, getCatalogName(), getSchemaName()));
                }

                try
                {
                    executeDdlStatement(stmt, stmtText);
                }
                catch (SQLException sqle)
                {
                    if (autoCreateErrors != null)
                    {
                        autoCreateErrors.add(sqle);
                    }
                    else
                    {
                        throw sqle;
                    }
                }
            }
        }
        finally
        {
            stmt.close();
        }
        return !stmtsByFKName.isEmpty();
    }
    
    /**
     * Method to validate any indices for this table, and auto create any
     * missing ones where required.
     * @param conn The JDBC Connection
     * @param autoCreate Whether to auto create any missing indices
     * @param autoCreateErrors Errors found during the auto-create process
     * @return Whether the database was changed
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    private boolean validateIndices(Connection conn, boolean autoCreate, Collection autoCreateErrors, ClassLoaderResolver clr)
    throws SQLException
    {
        boolean dbWasModified = false;

        // Retrieve the number of indexes in the datastore for this table.
        Map actualIndicesByName = null;
        int numActualIdxs = 0;
        if (storeMgr.getCompleteDDL())
        {
            actualIndicesByName = new HashMap();
        }
        else
        {
            actualIndicesByName = getExistingIndices(conn);
            Iterator<Map.Entry> iter = actualIndicesByName.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = iter.next();
                Index idx = (Index)entry.getValue();
                if (idx.getDatastoreContainerObject().getIdentifier().toString().equals(identifier.toString()))
                {
                    // Table of the index is the same as this table so must be ours
                    ++numActualIdxs;
                }
            }
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058004", "" + numActualIdxs, this));
            }
        }

        // Auto Create any missing indices
        if (autoCreate)
        {
            dbWasModified = createIndices(conn, autoCreateErrors, clr, actualIndicesByName);
        }
        else
        {
            Map stmtsByIdxName = getSQLCreateIndexStatements(actualIndicesByName, clr);
            if (stmtsByIdxName.isEmpty())
            {
                if (numActualIdxs > 0)
                {
                    if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                    {
                        NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058005", "" + numActualIdxs, this));
                    }
                }
            }
            else
            {
                // We support existing schemas so don't raise an exception.
                NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058003",
                    this, stmtsByIdxName.values()));
            }
        }
        return dbWasModified;
    }

    /**
     * Method to create any indices for this table
     * @param conn The JDBC Connection
     * @param autoCreateErrors Errors found during the auto-create process
     * @param actualIndicesByName the actual indices by name
     * @return Whether the database was changed
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    private boolean createIndices(Connection conn, Collection autoCreateErrors, ClassLoaderResolver clr, Map actualIndicesByName)
    throws SQLException
    {
        // Auto Create any missing indices
        Map stmtsByIdxName = getSQLCreateIndexStatements(actualIndicesByName, clr);
        Statement stmt = conn.createStatement();
        try
        {
            Iterator i = stmtsByIdxName.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry e = (Map.Entry) i.next();
                String idxName = (String) e.getKey();
                String stmtText = (String) e.getValue();
                if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058000", 
                        idxName, getCatalogName(), getSchemaName()));
                }

                try
                {
                    executeDdlStatement(stmt, stmtText);
                }
                catch (SQLException sqle)
                {
                    if (autoCreateErrors != null)
                    {
                        autoCreateErrors.add(sqle);
                    }
                    else
                    {
                        throw sqle;
                    }
                }
            }
        }
        finally
        {
            stmt.close();
        }
        return !stmtsByIdxName.isEmpty();
    }
    
    /**
     * Method to validate any Candidate keys on this table in the datastore, and
     * auto create any that are missing where required.
     * 
     * @param conn The JDBC Connection
     * @param autoCreate Whether to auto create the Candidate Keys if not existing
     * @param autoCreateErrors Errors found during the auto-create process
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    private boolean validateCandidateKeys(Connection conn, boolean autoCreate, Collection autoCreateErrors)
    throws SQLException
    {
        boolean dbWasModified = false;

        // Validate and/or create all candidate keys.
        Map actualCandidateKeysByName = null;
        int numActualCKs = 0;
        if (storeMgr.getCompleteDDL())
        {
            actualCandidateKeysByName = new HashMap();
        }
        else
        {
            actualCandidateKeysByName = getExistingCandidateKeys(conn);
            numActualCKs = actualCandidateKeysByName.size();
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058204", "" + numActualCKs, this));
            }
        }

        // Auto Create any missing candidate keys
        if (autoCreate)
        {
            dbWasModified = createCandidateKeys(conn, autoCreateErrors, actualCandidateKeysByName);
        }
        else
        {
            //validate only
            Map stmtsByCKName = getSQLAddCandidateKeyStatements(actualCandidateKeysByName);
            if (stmtsByCKName.isEmpty())
            {
                if (numActualCKs > 0)
                {
                    if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                    {
                        NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058205", 
                            "" + numActualCKs,this));
                    }
                }
            }
            else
            {
                // We support existing schemas so don't raise an exception.
                NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058201",
                    this, stmtsByCKName.values()));
            }
        }

        return dbWasModified;
    }

    /**
     * Method to create any Candidate keys on this table in the datastore, and
     * auto create any that are missing where required.
     * 
     * @param conn The JDBC Connection
     * @param autoCreateErrors Errors found during the auto-create process
     * @return Whether the database was modified
     * @throws SQLException Thrown when an error occurs in the JDBC calls
     */
    private boolean createCandidateKeys(Connection conn, Collection autoCreateErrors, Map actualCandidateKeysByName)
    throws SQLException
    {
        Map stmtsByCKName = getSQLAddCandidateKeyStatements(actualCandidateKeysByName);
        Statement stmt = conn.createStatement();
        try
        {
            Iterator i = stmtsByCKName.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry e = (Map.Entry) i.next();
                String ckName = (String) e.getKey();
                String stmtText = (String) e.getValue();
                if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058200",
                        ckName, getCatalogName(), getSchemaName()));
                }

                try
                {
                    executeDdlStatement(stmt, stmtText);
                }
                catch (SQLException sqle)
                {
                    if (autoCreateErrors != null)
                    {
                        autoCreateErrors.add(sqle);
                    }
                    else
                    {
                        throw sqle;
                    }
                }
            }
        }
        finally
        {
            stmt.close();
        }
        return !stmtsByCKName.isEmpty();
    }

    /**
     * Method to drop the constraints for the table from the datastore.
     * @param conn The JDBC Connection
     * @throws SQLException Thrown when an error occurs in the JDBC call.
     */
    public void dropConstraints(Connection conn)
    throws SQLException
    {
        assertIsInitialized();

        boolean drop_using_constraint = dba.supportsOption(RDBMSAdapter.ALTER_TABLE_DROP_CONSTRAINT_SYNTAX);
        boolean drop_using_foreign_key = dba.supportsOption(RDBMSAdapter.ALTER_TABLE_DROP_FOREIGN_KEY_CONSTRAINT);
        if (!drop_using_constraint && !drop_using_foreign_key)
        {
            return;
        }

        /*
         * There's no need to drop indices; we assume they'll go away quietly
         * when the table is dropped.
         */
        HashSet fkNames = new HashSet();
        StoreSchemaHandler handler = storeMgr.getSchemaHandler();
        RDBMSTableFKInfo fkInfo = (RDBMSTableFKInfo)handler.getSchemaData(conn, "foreign-keys", 
            new Object[] {this});
        Iterator iter = fkInfo.getChildren().iterator();
        while (iter.hasNext())
        {
            ForeignKeyInfo fki = (ForeignKeyInfo)iter.next();
            // JDBC drivers can return null names for foreign keys, so we then skip the DROP CONSTRAINT.
            String fkName = (String)fki.getProperty("fk_name");
            if (fkName != null)
            {
                fkNames.add(fkName);
            }
        }
        int numFKs = fkNames.size();
        if (numFKs > 0)
        {
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("058102", "" + numFKs, this));
            }
            iter = fkNames.iterator();
            IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
            Statement stmt = conn.createStatement();
            try
            {
                while (iter.hasNext())
                {
                    String constraintName = (String) iter.next();
                    String stmtText = null;
                    if (drop_using_constraint)
                    {
                        stmtText = "ALTER TABLE " + toString() + " DROP CONSTRAINT " + idFactory.getIdentifierInAdapterCase(constraintName);
                    }
                    else
                    {
                        stmtText = "ALTER TABLE " + toString() + " DROP FOREIGN KEY " + idFactory.getIdentifierInAdapterCase(constraintName);
                    }

                    executeDdlStatement(stmt, stmtText);
                }
            }
            finally
            {
                stmt.close();
            }
        }
    }

    // ----------------------- Internal Implementation Methods -----------------

    /**
     * Accessor for the expected foreign keys for this table in the datastore.
     * Currently only checks the columns for referenced tables (i.e relationships) and returns those.
     * @param clr The ClassLoaderResolver
     * @return List of foreign keys.
     */
    public List<ForeignKey> getExpectedForeignKeys(ClassLoaderResolver clr)
    {
        assertIsInitialized();

        /*
         * The following HashSet is to avoid the duplicate usage of columns that
         * have already been used in conjunction with another column
         */
        Set colsInFKs = new HashSet();
        ArrayList foreignKeys = new ArrayList();
        Iterator i = columns.iterator();
        while (i.hasNext())
        {
            Column col = (Column) i.next();
            if (!colsInFKs.contains(col))
            {
                try
                {
                    DatastoreClass referencedTable = storeMgr.getDatastoreClass(col.getStoredJavaType(), clr);
                    if (referencedTable != null)
                    {
                        for (int j = 0; j < col.getJavaTypeMapping().getNumberOfDatastoreMappings(); j++)
                        {
                            colsInFKs.add(col.getJavaTypeMapping().getDatastoreMapping(j).getDatastoreField());
                        }
                        ForeignKey fk = new ForeignKey(col.getJavaTypeMapping(), dba, referencedTable, true);
                        foreignKeys.add(fk);
                    }
                }
                catch (NoTableManagedException e)
                {
                    //expected when no table exists
                }
            }
        }
        return foreignKeys;
    }

    /**
     * Accessor for the expected candidate keys for this table in the datastore.
     * Currently returns an empty list.
     * @return List of candidate keys.
     */
    protected List getExpectedCandidateKeys()
    {
        assertIsInitialized();

        ArrayList candidateKeys = new ArrayList();
        return candidateKeys;
    }

    /**
     * Accessor for the indices for this table in the datastore.
     * @param clr The ClassLoaderResolver
     * @return Set of indices expected.
     */
    protected Set getExpectedIndices(ClassLoaderResolver clr)
    {
        assertIsInitialized();

        HashSet indices = new HashSet();

        /*
         * For each foreign key, add to the list an index made up of the "from"
         * column(s) of the key, *unless* those columns also happen to be 
         * equal to the primary key (then they are indexed anyway).
         * Ensure that we have separate indices for foreign key columns 
         * if the primary key is the combination of foreign keys, e.g. in join tables.
         * This greatly decreases deadlock probability e.g. on Oracle.
         */
        PrimaryKey pk = getPrimaryKey();
        Iterator i = getExpectedForeignKeys(clr).iterator();
        while (i.hasNext())
        {
            ForeignKey fk = (ForeignKey) i.next();
            if (!pk.getColumnList().equals(fk.getColumnList()))
            {
                indices.add(new Index(fk));
            }
        }

        return indices;
    }

    /**
     * Accessor for the primary keys for this table in the datastore.
     * @param conn The JDBC Connection
     * @return Map of primary keys
     * @throws SQLException Thrown when an error occurs in the JDBC call.
     */
    private Map getExistingPrimaryKeys(Connection conn)
    throws SQLException
    {
        HashMap primaryKeysByName = new HashMap();
        if (tableExistsInDatastore(conn))
        {
            StoreSchemaHandler handler = storeMgr.getSchemaHandler();
            RDBMSTablePKInfo tablePkInfo = (RDBMSTablePKInfo)handler.getSchemaData(conn, "primary-keys", 
                new Object[] {this});
            IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
            Iterator pkColsIter = tablePkInfo.getChildren().iterator();
            while (pkColsIter.hasNext())
            {
                PrimaryKeyInfo pkInfo = (PrimaryKeyInfo)pkColsIter.next();
                String pkName = (String)pkInfo.getProperty("pk_name");
                DatastoreIdentifier pkIdentifier;
    
                if (pkName == null)
                {
                    pkIdentifier = idFactory.newPrimaryKeyIdentifier(this);
                }
                else
                {
                    pkIdentifier = idFactory.newIdentifier(IdentifierType.COLUMN, pkName);
                }
    
                PrimaryKey pk = (PrimaryKey) primaryKeysByName.get(pkIdentifier);
                if (pk == null)
                {
                    pk = new PrimaryKey(this);
                    pk.setName(pkIdentifier.getIdentifierName());
                    primaryKeysByName.put(pkIdentifier, pk);
                }
    
                int keySeq = (((Short)pkInfo.getProperty("key_seq")).shortValue()) - 1;
                String colName = (String)pkInfo.getProperty("column_name");
                DatastoreIdentifier colIdentifier = idFactory.newIdentifier(IdentifierType.COLUMN, colName);
                Column col = columnsByName.get(colIdentifier);
    
                if (col == null)
                {
                    throw new UnexpectedColumnException(this.toString(), colIdentifier.getIdentifierName(), this.getSchemaName(), this.getCatalogName());
                }
                pk.setDatastoreField(keySeq, col);
            }
        }
        return primaryKeysByName;
    }

    /**
     * Accessor for the foreign keys for this table.
     * @param conn The JDBC Connection
     * @return Map of foreign keys
     * @throws SQLException Thrown when an error occurs in the JDBC call.
     */
    private Map getExistingForeignKeys(Connection conn)
    throws SQLException
    {
        HashMap foreignKeysByName = new HashMap();
        if (tableExistsInDatastore(conn))
        {
            StoreSchemaHandler handler = storeMgr.getSchemaHandler();
            IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
            RDBMSTableFKInfo tableFkInfo = (RDBMSTableFKInfo)handler.getSchemaData(conn, "foreign-keys", 
                new Object[] {this});
            Iterator fksIter = tableFkInfo.getChildren().iterator();
            while (fksIter.hasNext())
            {
                ForeignKeyInfo fkInfo = (ForeignKeyInfo)fksIter.next();
                DatastoreIdentifier fkIdentifier;
                String fkName = (String)fkInfo.getProperty("fk_name");
                if (fkName == null)
                {
                    fkIdentifier = idFactory.newForeignKeyIdentifier(this, foreignKeysByName.size());
                }
                else
                {
                    fkIdentifier = idFactory.newIdentifier(IdentifierType.FOREIGN_KEY, fkName);
                }
    
                short deferrability = ((Short)fkInfo.getProperty("deferrability")).shortValue();
                boolean initiallyDeferred = deferrability == DatabaseMetaData.importedKeyInitiallyDeferred;
                ForeignKey fk = (ForeignKey) foreignKeysByName.get(fkIdentifier);
                if (fk == null)
                {
                    fk = new ForeignKey(initiallyDeferred);
                    fk.setName(fkIdentifier.getIdentifierName());
                    foreignKeysByName.put(fkIdentifier, fk);
                }
    
                String pkTableName = (String)fkInfo.getProperty("pk_table_name");
                AbstractTable refTable = (AbstractTable)storeMgr.getDatastoreClass(
                    idFactory.newDatastoreContainerIdentifier(pkTableName));
                if (refTable != null)
                {
                    String fkColumnName = (String)fkInfo.getProperty("fk_column_name");
                    String pkColumnName = (String)fkInfo.getProperty("pk_column_name");
                    DatastoreIdentifier colName = idFactory.newIdentifier(IdentifierType.COLUMN, fkColumnName);
                    DatastoreIdentifier refColName = idFactory.newIdentifier(IdentifierType.COLUMN, pkColumnName);
                    DatastoreField col = columnsByName.get(colName);
                    DatastoreField refCol = refTable.columnsByName.get(refColName);
                    if (col != null && refCol != null)
                    {
                        fk.addDatastoreField(col, refCol);
                    }
                    else
                    {
                        //TODO throw exception?
                    }
                }
            }
        }
        return foreignKeysByName;
    }

    /**
     * Accessor for the candidate keys for this table.
     * @param conn The JDBC Connection
     * @return Map of candidate keys
     * @throws SQLException Thrown when an error occurs in the JDBC call.
     */
    private Map getExistingCandidateKeys(Connection conn)
    throws SQLException
    {
        HashMap candidateKeysByName = new HashMap();

        if (tableExistsInDatastore(conn))
        {
            StoreSchemaHandler handler = storeMgr.getSchemaHandler();
            RDBMSTableIndexInfo tableIndexInfo = (RDBMSTableIndexInfo)handler.getSchemaData(conn, "indices", 
                new Object[] {this});
            IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
            Iterator indexIter = tableIndexInfo.getChildren().iterator();
            while (indexIter.hasNext())
            {
                IndexInfo indexInfo = (IndexInfo)indexIter.next();
                boolean isUnique = !((Boolean)indexInfo.getProperty("non_unique")).booleanValue();
                if (isUnique)
                {
                    // Only utilise unique indexes
                    short idxType = ((Short)indexInfo.getProperty("type")).shortValue();
                    if (idxType == DatabaseMetaData.tableIndexStatistic)
                    {
                        // Ignore
                        continue;
                    }
    
                    String keyName = (String)indexInfo.getProperty("index_name");
                    DatastoreIdentifier idxName = idFactory.newIdentifier(IdentifierType.CANDIDATE_KEY, keyName);
                    CandidateKey key = (CandidateKey) candidateKeysByName.get(idxName);
                    if (key == null)
                    {
                        key = new CandidateKey(this);
                        key.setName(keyName);
                        candidateKeysByName.put(idxName, key);
                    }
    
                    // Set the column
                    int colSeq = ((Short)indexInfo.getProperty("ordinal_position")).shortValue() - 1;
                    DatastoreIdentifier colName = idFactory.newIdentifier(IdentifierType.COLUMN, 
                        (String)indexInfo.getProperty("column_name"));
                    Column col = columnsByName.get(colName);
                    if (col != null)
                    {
                        key.setDatastoreField(colSeq, col);
                    }
                }
            }
        }
        return candidateKeysByName;
    }

    
    /**
     * Accessor for indices on the actual table.
     * @param conn The JDBC Connection
     * @return Map of indices (keyed by the index name)
     * @throws SQLException Thrown when an error occurs in the JDBC call.
     */
    private Map getExistingIndices(Connection conn)
    throws SQLException
    {
        HashMap indicesByName = new HashMap();

        if (tableExistsInDatastore(conn))
        {
            StoreSchemaHandler handler = storeMgr.getSchemaHandler();
            RDBMSTableIndexInfo tableIndexInfo = (RDBMSTableIndexInfo)handler.getSchemaData(conn, "indices", 
                new Object[] {this});
            IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
            Iterator indexIter = tableIndexInfo.getChildren().iterator();
            while (indexIter.hasNext())
            {
                IndexInfo indexInfo = (IndexInfo)indexIter.next();
                short idxType = ((Short)indexInfo.getProperty("type")).shortValue();
                if (idxType == DatabaseMetaData.tableIndexStatistic)
                {
                    // Ignore
                    continue;
                }
    
                String indexName = (String)indexInfo.getProperty("index_name");
                DatastoreIdentifier indexIdentifier = idFactory.newIdentifier(IdentifierType.CANDIDATE_KEY, 
                    indexName);
                Index idx = (Index) indicesByName.get(indexIdentifier);
                if (idx == null)
                {
                    boolean isUnique = !((Boolean)indexInfo.getProperty("non_unique")).booleanValue();
                    idx = new Index(this, isUnique, null);
                    idx.setName(indexName);
                    indicesByName.put(indexIdentifier, idx);
                }
    
                // Set the column
                int colSeq = ((Short)indexInfo.getProperty("ordinal_position")).shortValue() - 1;
                DatastoreIdentifier colName = idFactory.newIdentifier(IdentifierType.COLUMN, 
                    (String)indexInfo.getProperty("column_name"));
                Column col = columnsByName.get(colName);
                if (col != null)
                {
                    idx.setColumn(colSeq, col);
                }
            }
        }
        return indicesByName;
    }

    /**
     * Accessor for the SQL CREATE statements for this table.
     * @param props Properties for controlling the table creation
     * @return List of statements.
     */
    protected List getSQLCreateStatements(Properties props)
    {
        assertIsInitialized();

        Column[] cols = null;

        // Pass 1 : populate positions defined in metadata as vendor extension "index"
        Iterator<Column> iter = columns.iterator();
        while (iter.hasNext())
        {
            Column col = iter.next();
            ColumnMetaData colmd = col.getColumnMetaData();
            Integer colPos = (colmd != null ? colmd.getPosition() : null);
            if (colPos != null)
            {
                int index = colPos.intValue();
                if (index < columns.size() && index >= 0)
                {
                    if (cols == null)
                    {
                        cols = new Column[columns.size()];
                    }
                    if (cols[index] != null)
                    {
                        throw new NucleusUserException("Column index " + index +
                            " has been specified multiple times : " + cols[index] + " and " + col);
                    }
                    cols[index] = col;
                }
            }
        }

        // Pass 2 : fill in spaces for columns with undefined positions
        if (cols != null)
        {
            iter = columns.iterator();
            while (iter.hasNext())
            {
                Column col = iter.next();
                ColumnMetaData colmd = col.getColumnMetaData();
                Integer colPos = (colmd != null ? colmd.getPosition() : null);
                if (colPos == null)
                {
                    // No index set for this column, so assign to next free position
                    for (int i=0;i<cols.length;i++)
                    {
                        if (cols[i] == null)
                        {
                            cols[i] = col;
                        }
                    }
                }
            }
        }
        else
        {
            cols = columns.toArray(new Column[columns.size()]);
        }

        ArrayList stmts = new ArrayList();        
        stmts.add(dba.getCreateTableStatement(this, cols, props, storeMgr.getIdentifierFactory()));

        PrimaryKey pk = getPrimaryKey();
        if (pk.size() > 0)
        {
            // Some databases define the primary key on the create table
            // statement so we don't have a Statement for the primary key here.
            String pkStmt = dba.getAddPrimaryKeyStatement(pk, storeMgr.getIdentifierFactory());
            if (pkStmt != null)
            {
                stmts.add(pkStmt);
            }
        }

        return stmts;
    }

    /**
     * Get SQL statements to add expected Foreign Keys that are not yet at the
     * table. If the returned Map is empty, the current FK setup is correct.
     * @param actualForeignKeysByName Actual Map of foreign keys
     * @param clr The ClassLoaderResolver
     * @return a Map with the SQL statements
     */
    protected Map getSQLAddFKStatements(Map actualForeignKeysByName, ClassLoaderResolver clr)
    {
        assertIsInitialized();

        HashMap stmtsByFKName = new HashMap();
        List expectedForeignKeys = getExpectedForeignKeys(clr);
        Iterator i = expectedForeignKeys.iterator();
        int n = 1;
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        while (i.hasNext())
        {
            ForeignKey fk = (ForeignKey) i.next();
            if (!actualForeignKeysByName.containsValue(fk))
            {
                // If no name assigned, make one up
                if (fk.getName() == null)
                {
                    // Use the ForeignKeyIdentifier to generate the name
                    DatastoreIdentifier fkName;
                    do
                    {
                        fkName = idFactory.newForeignKeyIdentifier(this, n++);
                    }
                    while (actualForeignKeysByName.containsKey(fkName));
                    fk.setName(fkName.getIdentifierName());
                }
                String stmtText = dba.getAddForeignKeyStatement(fk, idFactory);
                if (stmtText != null)
                {
                    stmtsByFKName.put(fk.getName(), stmtText);
                }
            }
        }

        return stmtsByFKName;
    }

    /**
     * Get SQL statements to add expected Candidate Keys that are not yet on the
     * table. If the returned Map is empty, the current Candidate Key setup is correct.
     * @param actualCandidateKeysByName Actual Map of candidate keys
     * @return a Map with the SQL statements
     */
    protected Map getSQLAddCandidateKeyStatements(Map actualCandidateKeysByName)
    {
        assertIsInitialized();

        HashMap stmtsByCKName = new HashMap();
        List expectedCandidateKeys = getExpectedCandidateKeys();
        Iterator i = expectedCandidateKeys.iterator();
        int n = 1;
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        while (i.hasNext())
        {
            CandidateKey ck = (CandidateKey) i.next();
            if (!actualCandidateKeysByName.containsValue(ck))
            {
                // If no name assigned, make one up
                if (ck.getName() == null)
                {
                    // Use the CandidateKeyIdentifier to generate the name
                    DatastoreIdentifier ckName;
                    do
                    {
                        ckName = idFactory.newCandidateKeyIdentifier(this, n++);
                    }
                    while (actualCandidateKeysByName.containsKey(ckName));
                    ck.setName(ckName.getIdentifierName());
                }
                String stmtText = dba.getAddCandidateKeyStatement(ck, idFactory);
                if (stmtText != null)
                {
                    stmtsByCKName.put(ck.getName(), stmtText);
                }
            }
        }

        return stmtsByCKName;
    }

    /**
     * Utility to check if an index is necessary.
     * @param requiredIdx The index
     * @param actualIndices The actual indexes
     * @return Whether the index is needed (i.e not present in the actual indexes)
     */
    private boolean isIndexReallyNeeded(Index requiredIdx, Collection actualIndices)
    {
        Iterator i = actualIndices.iterator();
        if (requiredIdx.getName() != null)
        {
            // Compare the index name since it is defined
            IdentifierFactory idFactory = requiredIdx.getDatastoreContainerObject().getStoreManager().getIdentifierFactory();
            String reqdName = idFactory.getIdentifierInAdapterCase(requiredIdx.getName()); // Allow for user input in incorrect case
            while (i.hasNext())
            {
                Index actualIdx = (Index) i.next();
                String actualName = idFactory.getIdentifierInAdapterCase(actualIdx.getName()); // Allow for DB returning no quotes
                if (actualName.equals(reqdName) &&
                        actualIdx.getDatastoreContainerObject().getIdentifier().toString().equals(requiredIdx.getDatastoreContainerObject().getIdentifier().toString()))
                {
                    // There already is an index of that name for the same table in the actual list so not needed
                    return false;
                }
            }
        }
        else
        {
            // Compare against the index table and columns since we have no index name yet
            while (i.hasNext())
            {
                Index actualIdx = (Index) i.next();
                if (actualIdx.toString().equals(requiredIdx.toString()) &&
                    actualIdx.getDatastoreContainerObject().getIdentifier().toString().equals(requiredIdx.getDatastoreContainerObject().getIdentifier().toString()))
                {
                    // There already is an index of that name for the same table in the actual list so not needed
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Accessor for the CREATE INDEX statements for this table.
     * @param actualIndicesByName Map of actual indexes
     * @param clr The ClassLoaderResolver
     * @return Map of statements
     */
    protected Map getSQLCreateIndexStatements(Map actualIndicesByName, ClassLoaderResolver clr)
    {
        assertIsInitialized();
        HashMap stmtsByIdxName = new HashMap();
        Set expectedIndices = getExpectedIndices(clr);

        int n = 1;
        Iterator i = expectedIndices.iterator();
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        while (i.hasNext())
        {
            Index idx = (Index) i.next();
            if (isIndexReallyNeeded(idx, actualIndicesByName.values()))
            {
                // If no name assigned, make one up
                if (idx.getName() == null)
                {
                    // Use IndexIdentifier to generate the name.
                    DatastoreIdentifier idxName;
                    do
                    {
                        idxName = idFactory.newIndexIdentifier(this, idx.getUnique(), n++);
                        idx.setName(idxName.getIdentifierName());
                    }
                    while (actualIndicesByName.containsKey(idxName));
                }

                String stmtText = dba.getCreateIndexStatement(idx, idFactory);
                stmtsByIdxName.put(idx.getName(), stmtText);
            }
        }
        return stmtsByIdxName;
    }

    /**
     * Accessor for the DROP statements for this table.
     * @return List of statements
     */
    protected List getSQLDropStatements()
    {
        assertIsInitialized();
        ArrayList stmts = new ArrayList();
        stmts.add(dba.getDropTableStatement(this));
        return stmts;
    }
}