/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.scostore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.Transaction;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.metadata.MapMetaData;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.EmbeddedKeyPCMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingHelper;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.mapped.mapping.SerialisedMapping;
import org.datanucleus.store.mapped.mapping.SerialisedPCMapping;
import org.datanucleus.store.mapped.mapping.SerialisedReferenceMapping;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.JDBCUtils;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.mapping.RDBMSMapping;
import org.datanucleus.store.rdbms.query.StatementParameterMapping;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.StatementGenerator;
import org.datanucleus.store.rdbms.sql.UnionStatementGenerator;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.store.rdbms.table.JoinTable;
import org.datanucleus.store.rdbms.table.MapTable;
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.scostore.SetStore;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * RDBMS-specific implementation of an {@link MapStore} using join table.
 */
public class RDBMSJoinMapStore extends AbstractMapStore
{
    private String putStmt;
    private String updateStmt;
    private String removeStmt;
    private String clearStmt;

    /** JDBC statement to use for retrieving keys of the map (locking). */
    private String getStmtLocked = null;

    /** JDBC statement to use for retrieving keys of the map (not locking). */
    private String getStmtUnlocked = null;

    private StatementClassMapping getMappingDef = null;
    private StatementParameterMapping getMappingParams = null;

    private SetStore keySetStore = null;
    private SetStore valueSetStore = null;
    private SetStore entrySetStore = null;
    
    /**
     * when the element mappings columns can't be part of the primary key
     * by datastore limitations like BLOB types.
     * An adapter mapping is used to be a kind of "index"
     */
    protected final JavaTypeMapping adapterMapping;    

    protected ClassLoaderResolver clr;

    /**
     * Constructor for the backing store of a join map for RDBMS.
     * @param mapTable Join table for the Map
     * @param clr The ClassLoaderResolver
     */
    public RDBMSJoinMapStore(MapTable mapTable, ClassLoaderResolver clr)
    {
        super((RDBMSStoreManager) mapTable.getStoreManager());

        this.clr = clr;
        this.mapTable = mapTable;
        setOwner(mapTable.getOwnerMemberMetaData(), clr);

        this.ownerMapping = mapTable.getOwnerMapping();
        this.keyMapping = mapTable.getKeyMapping();
        this.valueMapping = mapTable.getValueMapping();
        this.adapterMapping = mapTable.getOrderMapping();

        this.keyType = mapTable.getKeyType();
        this.keysAreEmbedded = mapTable.isEmbeddedKey();
        this.keysAreSerialised = mapTable.isSerialisedKey();
        this.valueType = mapTable.getValueType();
        this.valuesAreEmbedded = mapTable.isEmbeddedValue();
        this.valuesAreSerialised = mapTable.isSerialisedValue();

        Class key_class=clr.classForName(keyType);
        kmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(key_class, clr);
        Class value_class=clr.classForName(valueType);
        if (ClassUtils.isReferenceType(value_class))
        {
            NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("056066", value_class.getName()));
            vmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForImplementationOfReference(value_class,null,clr);
            if (vmd != null)
            {
                this.valueType = value_class.getName();
                // TODO This currently just grabs the cmd of the first implementation. It needs to
                // get the cmds for all implementations, so we can have a handle to all possible elements.
                // This would mean changing the SCO classes to have multiple valueTable/valueMapping etc.
                valueTable = storeMgr.getDatastoreClass(vmd.getFullClassName(), clr);
            }
        }
        else
        {
            vmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(value_class, clr);
            if (vmd != null)
            {
                this.valueType = vmd.getFullClassName();
                if (valuesAreEmbedded)
                {
                    valueTable = null;
                }
                else
                {
                    valueTable = storeMgr.getDatastoreClass(valueType, clr);
                }
            }
        }

        initialise();

        putStmt = getPutStmt();
        updateStmt = getUpdateStmt();
        removeStmt = getRemoveStmt();
        clearStmt = getClearStmt();
    }

    /**
     * Method to put all elements from a Map into our Map.
     * @param sm State Manager for the Map
     * @param m The Map to add
     **/
    public void putAll(ObjectProvider sm, Map m)
    {
        if (m == null || m.size() == 0)
        {
            return;
        }

        HashSet puts = new HashSet();
        HashSet updates = new HashSet();

        Iterator i = m.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry e = (Map.Entry)i.next();
            Object key = e.getKey();
            Object value = e.getValue();

            // Make sure the related objects are persisted (persistence-by-reachability)
            validateKeyForWriting(sm, key);
            validateValueForWriting(sm, value);

            // Check if this is a new entry, or an update
            try
            {
                Object oldValue = getValue(sm, key);
                if (oldValue != value)
                {
                    updates.add(e);
                }
            }
            catch (NoSuchElementException nsee)
            {
                puts.add(e);
            }
        }

        boolean batched = allowsBatching();

        // Put any new entries
        if (puts.size() > 0)
        {
            try
            {
                ExecutionContext ec = sm.getExecutionContext();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                try
                {
                    // Loop through all entries
                    Iterator iter = puts.iterator();
                    while (iter.hasNext())
                    {
                        // Add the row to the join table
                        Map.Entry entry = (Map.Entry)iter.next();
                        internalPut(sm, mconn, batched, entry.getKey(), entry.getValue(), (!iter.hasNext()));
                    }
                }
                finally
                {
                    mconn.release();
                }
            }
            catch (MappedDatastoreException e)
            {
                throw new NucleusDataStoreException(LOCALISER.msg("056016", e.getMessage()), e);
            }
        }

        // Update any changed entries
        if (updates.size() > 0)
        {
            try
            {
                ExecutionContext ec = sm.getExecutionContext();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                try
                {
                    // Loop through all entries
                    Iterator iter = updates.iterator();
                    while (iter.hasNext())
                    {
                        // Update the row in the join table
                        Map.Entry entry = (Map.Entry)iter.next();
                        internalUpdate(sm, mconn, batched, entry.getKey(), entry.getValue(), !iter.hasNext());
                    }
                }
                finally
                {
                    mconn.release();
                }
            }
            catch (MappedDatastoreException mde)
            {
                throw new NucleusDataStoreException(LOCALISER.msg("056016", mde.getMessage()), mde);
            }
        }
    }

    /**
     * Method to put an item in the Map.
     * @param sm State Manager for the map.
     * @param key The key to store the value against
     * @param value The value to store.
     * @return The value stored.
     **/
    public Object put(ObjectProvider sm, Object key, Object value)
    {
        validateKeyForWriting(sm, key);
        validateValueForWriting(sm, value);

        boolean exists = false;
        Object oldValue;
        try
        {
            oldValue = getValue(sm, key);
            exists = true;
        }
        catch (NoSuchElementException e)
        {
            oldValue = null;
            exists = false;
        }

        if (oldValue != value)
        {
            // Value changed so update the map
            try
            {
                ExecutionContext ec = sm.getExecutionContext();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                try
                {
                    if (exists)
                    {
                        internalUpdate(sm, mconn, false, key, value, true);
                    }
                    else
                    {
                        internalPut(sm, mconn, false, key, value, true);
                    }
                }
                finally
                {
                    mconn.release();
                }
            }
            catch (MappedDatastoreException e)
            {
                throw new NucleusDataStoreException(LOCALISER.msg("056016", e.getMessage()), e);
            }
        }

        MapMetaData mapmd = ownerMemberMetaData.getMap();
        if (mapmd.isDependentValue() && !mapmd.isEmbeddedValue() && oldValue != null)
        {
            // Delete the old value if it is no longer contained and is dependent
            if (!containsValue(sm, oldValue))
            {
                sm.getExecutionContext().deleteObjectInternal(oldValue);
            }
        }

        return oldValue;
    }

    /**
     * Method to remove an item from the map.
     * @param sm State Manager for the map.
     * @param key Key of the item to remove.
     * @return The value that was removed.
     **/
    public Object remove(ObjectProvider sm, Object key)
    {
        if (!validateKeyForReading(sm, key))
        {
            return null;
        }

        Object oldValue;
        boolean exists;
        try
        {
            oldValue = getValue(sm, key);
            exists = true;
        }
        catch (NoSuchElementException e)
        {
            oldValue = null;
            exists = false;
        }

        ExecutionContext ec = sm.getExecutionContext();
        if (exists)
        {
            removeInternal(sm, key);
        }

        MapMetaData mapmd = ownerMemberMetaData.getMap();
        ApiAdapter api = ec.getApiAdapter();
        if (mapmd.isDependentKey() && !mapmd.isEmbeddedKey() && api.isPersistable(key))
        {
            // Delete the key if it is dependent
            ec.deleteObjectInternal(key);
        }

        if (mapmd.isDependentValue() && !mapmd.isEmbeddedValue() && api.isPersistable(oldValue))
        {
            if (!containsValue(sm, oldValue))
            {
                // Delete the value if it is dependent and is not keyed by another key
                ec.deleteObjectInternal(oldValue);
            }
        }

        return oldValue;
    }

    /**
     * Method to clear the map of all values.
     * @param ownerSM State Manager for the map.
     */
    public void clear(ObjectProvider ownerSM)
    {
        Collection dependentElements = null;
        if (ownerMemberMetaData.getMap().isDependentKey() || ownerMemberMetaData.getMap().isDependentValue())
        {
            // Retain the PC dependent keys/values that need deleting after clearing
            dependentElements = new HashSet();
            ApiAdapter api = ownerSM.getExecutionContext().getApiAdapter();
            Iterator iter = entrySetStore().iterator(ownerSM);
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                MapMetaData mapmd = ownerMemberMetaData.getMap();
                if (api.isPersistable(entry.getKey()) && mapmd.isDependentKey() && !mapmd.isEmbeddedKey())
                {
                    dependentElements.add(entry.getKey());
                }
                if (api.isPersistable(entry.getValue()) && mapmd.isDependentValue() && !mapmd.isEmbeddedValue())
                {
                    dependentElements.add(entry.getValue());
                }
            }
        }
        clearInternal(ownerSM);

        if (dependentElements != null && dependentElements.size() > 0)
        {
            // Delete all dependent objects
            ownerSM.getExecutionContext().deleteObjects(dependentElements.toArray());
        }
    }

    /**
     * Accessor for the keys in the Map.
     * @return The keys
     **/
    public synchronized SetStore keySetStore()
    {
        if (keySetStore == null)
        {
            keySetStore = newMapKeySetStore();
        }
        return keySetStore;
    }

    /**
     * Accessor for the values in the Map.
     * @return The values.
     **/
    public synchronized SetStore valueSetStore()
    {
        if (valueSetStore == null)
        {
            valueSetStore = newMapValueSetStore();
        }
        return valueSetStore;
    }

    /**
     * Accessor for the map entries in the Map.
     * @return The map entries.
     */
    public synchronized SetStore entrySetStore()
    {
        if (entrySetStore == null)
        {
            entrySetStore = newMapEntrySetStore();
        }
        return entrySetStore;
    }

    public JavaTypeMapping getAdapterMapping()
    {
        return adapterMapping;
    }

    /**
     * Generate statement to add an item to the Map.
     * Adds a row to the link table, linking container with value object.
     * <PRE>
     * INSERT INTO MAPTABLE (VALUECOL, OWNERCOL, KEYCOL)
     * VALUES (?, ?, ?)
     * </PRE>
     * @return Statement to add an item to the Map.
     */
    private String getPutStmt()
    {
        StringBuffer stmt = new StringBuffer("INSERT INTO ");
        stmt.append(mapTable.toString());
        stmt.append(" (");
        for (int i=0; i<valueMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(valueMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
        }

        for (int i=0; i<ownerMapping.getNumberOfDatastoreMappings(); i++)
        {
            stmt.append(",");
            stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
        }
        if (adapterMapping != null)
        {
            for (int i=0; i<adapterMapping.getNumberOfDatastoreMappings(); i++)
            {
                stmt.append(",");
                stmt.append(adapterMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
            }
        }

        for (int i=0; i<keyMapping.getNumberOfDatastoreMappings(); i++)
        {
            stmt.append(",");
            stmt.append(keyMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
        }

        stmt.append(") VALUES (");
        for (int i=0; i<valueMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(((RDBMSMapping)valueMapping.getDatastoreMapping(i)).getInsertionInputParameter());
        }

        for (int i=0; i<ownerMapping.getNumberOfDatastoreMappings(); i++)
        {
            stmt.append(",");
            stmt.append(((RDBMSMapping)ownerMapping.getDatastoreMapping(i)).getInsertionInputParameter());
        }
        if (adapterMapping != null)
        {
            for (int i=0; i<adapterMapping.getNumberOfDatastoreMappings(); i++)
            {
                stmt.append(",");
                stmt.append(((RDBMSMapping)adapterMapping.getDatastoreMapping(i)).getInsertionInputParameter());
            }
        }
        for (int i=0; i<keyMapping.getNumberOfDatastoreMappings(); i++)
        {
            stmt.append(",");
            stmt.append(((RDBMSMapping)keyMapping.getDatastoreMapping(i)).getInsertionInputParameter());
        }
        stmt.append(") ");

        return stmt.toString();
    }

    /**
     * Generate statement to update an item in the Map.
     * Updates the link table row, changing the value object for this key.
     * <PRE>
     * UPDATE MAPTABLE
     * SET VALUECOL=?
     * WHERE OWNERCOL=?
     * AND KEYCOL=?
     * </PRE>
     * @return Statement to update an item in the Map.
     */
    private String getUpdateStmt()
    {
        StringBuffer stmt = new StringBuffer("UPDATE ");
        stmt.append(mapTable.toString());
        stmt.append(" SET ");
        for (int i=0; i<valueMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(valueMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
            stmt.append(" = ");
            stmt.append(((RDBMSMapping)valueMapping.getDatastoreMapping(i)).getUpdateInputParameter());
        }
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
        BackingStoreHelper.appendWhereClauseForMapping(stmt, keyMapping, null, false);

        return stmt.toString();
    }

    /**
     * Generate statement to remove an item from the Map.
     * Deletes the link from the join table, leaving the value object in its
     * own table.
     * <PRE>
     * DELETE FROM MAPTABLE
     * WHERE OWNERCOL=?
     * AND KEYCOL=?
     * </PRE>
     * @return Return an item from the Map.
     */
    private String getRemoveStmt()
    {
        StringBuffer stmt = new StringBuffer("DELETE FROM ");
        stmt.append(mapTable.toString());
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
        BackingStoreHelper.appendWhereClauseForMapping(stmt, keyMapping, null, false);

        return stmt.toString();
    }

    /**
     * Generate statement to clear the Map.
     * Deletes the links from the join table for this Map, leaving the value
     * objects in their own table(s).
     * <PRE>
     * DELETE FROM MAPTABLE
     * WHERE OWNERCOL=?
     * </PRE>
     * @return Statement to clear the Map.
     */
    private String getClearStmt()
    {
        StringBuffer stmt = new StringBuffer("DELETE FROM ");
        stmt.append(mapTable.toString());
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);

        return stmt.toString();
    }

    /**
     * Method to retrieve a value from the Map given the key.
     * @param ownerSM State Manager for the owner of the map.
     * @param key The key to retrieve the value for.
     * @return The value for this key
     * @throws NoSuchElementException if the value for the key was not found
     */
    protected Object getValue(ObjectProvider ownerSM, Object key)
    throws NoSuchElementException
    {
        if (!validateKeyForReading(ownerSM, key))
        {
            return null;
        }

        ExecutionContext ec = ownerSM.getExecutionContext();
        if (getStmtLocked == null)
        {
            synchronized (this) // Make sure this completes in case another thread needs the same info
            {
                // Generate the statement, and statement mapping/parameter information
                SQLStatement sqlStmt = getSQLStatementForGet(ownerSM);
                getStmtUnlocked = sqlStmt.getSelectStatement().toSQL();
                sqlStmt.addExtension("lock-for-update", true);
                getStmtLocked = sqlStmt.getSelectStatement().toSQL();
            }
        }

        Transaction tx = ec.getTransaction();
        String stmt = (tx.lockReadObjects() ? getStmtLocked : getStmtUnlocked);
        Object value = null;
        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                // Create the statement and supply owner/key params
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, stmt);
                StatementMappingIndex ownerIdx = getMappingParams.getMappingForParameter("owner");
                int numParams = ownerIdx.getNumberOfParameterOccurrences();
                for (int paramInstance=0;paramInstance<numParams;paramInstance++)
                {
                    ownerIdx.getMapping().setObject(ec, ps,
                        ownerIdx.getParameterPositionsForOccurrence(paramInstance), ownerSM.getObject());
                }
                StatementMappingIndex keyIdx = getMappingParams.getMappingForParameter("key");
                numParams = keyIdx.getNumberOfParameterOccurrences();
                for (int paramInstance=0;paramInstance<numParams;paramInstance++)
                {
                    keyIdx.getMapping().setObject(ec, ps,
                        keyIdx.getParameterPositionsForOccurrence(paramInstance), key);
                }

                try
                {
                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, stmt, ps);
                    try
                    {
                        boolean found = rs.next();
                        if (!found)
                        {
                            throw new NoSuchElementException();
                        }

                        if (valuesAreEmbedded || valuesAreSerialised)
                        {
                            int param[] = new int[valueMapping.getNumberOfDatastoreMappings()];
                            for (int i = 0; i < param.length; ++i)
                            {
                                param[i] = i + 1;
                            }

                            if (valueMapping instanceof SerialisedPCMapping ||
                                valueMapping instanceof SerialisedReferenceMapping ||
                                valueMapping instanceof EmbeddedKeyPCMapping)
                            {
                                // Value = Serialised
                                int ownerFieldNumber = ((JoinTable)mapTable).getOwnerMemberMetaData().getAbsoluteFieldNumber();
                                value = valueMapping.getObject(ec, rs, param, ownerSM, ownerFieldNumber);
                            }
                            else
                            {
                                // Value = Non-PC
                                value = valueMapping.getObject(ec, rs, param);
                            }
                        }
                        else if (valueMapping instanceof ReferenceMapping)
                        {
                            // Value = Reference (Interface/Object)
                            int param[] = new int[valueMapping.getNumberOfDatastoreMappings()];
                            for (int i = 0; i < param.length; ++i)
                            {
                                param[i] = i + 1;
                            }
                            value = valueMapping.getObject(ec, rs, param);
                        }
                        else
                        {
                            // Value = PC
                            ResultObjectFactory rof = storeMgr.newResultObjectFactory(vmd, 
                                getMappingDef, false, null, clr.classForName(valueType));
                            value = rof.getObject(ec, rs);
                        }

                        JDBCUtils.logWarnings(rs);
                    }
                    finally
                    {
                        rs.close();
                    }
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056014", stmt), e);
        }
        return value;
    }

    /**
     * Method to return an SQLStatement for retrieving the value for a key.
     * Selects the join table and optionally joins to the value table if it has its own table.
     * @param ownerSM StateManager for the owning object
     * @return The SQLStatement
     */
    protected SQLStatement getSQLStatementForGet(ObjectProvider ownerSM)
    {
        SQLStatement sqlStmt = null;

        final ClassLoaderResolver clr = ownerSM.getExecutionContext().getClassLoaderResolver();
        final Class valueCls = clr.classForName(this.valueType);
        if (valuesAreEmbedded || valuesAreSerialised)
        {
            // Value is stored in join table
            sqlStmt = new SQLStatement(storeMgr, mapTable, null, null);
            sqlStmt.setClassLoaderResolver(clr);
            sqlStmt.select(sqlStmt.getPrimaryTable(), valueMapping, null);
        }
        else
        {
            // Value is stored in own table
            getMappingDef = new StatementClassMapping();
            UnionStatementGenerator stmtGen =
                new UnionStatementGenerator(storeMgr, clr, valueCls, true, null, null, mapTable, null, valueMapping);
            stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
            getMappingDef.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
            sqlStmt = stmtGen.getStatement();

            // Select the value field(s)
            SQLTable valueSqlTbl = sqlStmt.getTable(valueTable, sqlStmt.getPrimaryTable().getGroupName());
            SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, getMappingDef,
                ownerSM.getExecutionContext().getFetchPlan(), valueSqlTbl, vmd, 0);
        }

        // Apply condition on owner field to filter by owner
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        SQLTable ownerSqlTbl =
            SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), ownerMapping);
        SQLExpression ownerExpr = exprFactory.newExpression(sqlStmt, ownerSqlTbl, ownerMapping);
        SQLExpression ownerVal = exprFactory.newLiteralParameter(sqlStmt, ownerMapping, null, "OWNER");
        sqlStmt.whereAnd(ownerExpr.eq(ownerVal), true);

        // Apply condition on key
        if (keyMapping instanceof SerialisedMapping)
        {
            // if the keyMapping contains a BLOB column (or any other column not supported by the database
            // as primary key), uses like instead of the operator OP_EQ (=)
            // in future do not check if the keyMapping is of ObjectMapping, but use the database 
            // adapter to check the data types not supported as primary key
            // if object mapping (BLOB) use like
            SQLExpression keyExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), keyMapping);
            SQLExpression keyVal = exprFactory.newLiteralParameter(sqlStmt, keyMapping, null, "KEY");
            sqlStmt.whereAnd(new org.datanucleus.store.rdbms.sql.expression.BooleanExpression(keyExpr,
                Expression.OP_LIKE, keyVal), true);
        }
        else
        {
            SQLExpression keyExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), keyMapping);
            SQLExpression keyVal = exprFactory.newLiteralParameter(sqlStmt, keyMapping, null, "KEY");
            sqlStmt.whereAnd(keyExpr.eq(keyVal), true);
        }

        // Input parameter(s) - owner, key
        int inputParamNum = 1;
        StatementMappingIndex ownerIdx = new StatementMappingIndex(ownerMapping);
        StatementMappingIndex keyIdx = new StatementMappingIndex(keyMapping);
        if (sqlStmt.getNumberOfUnions() > 0)
        {
            // Add parameter occurrence for each union of statement
            for (int j=0;j<sqlStmt.getNumberOfUnions()+1;j++)
            {
                int[] ownerPositions = new int[ownerMapping.getNumberOfDatastoreMappings()];
                for (int k=0;k<ownerPositions.length;k++)
                {
                    ownerPositions[k] = inputParamNum++;
                }
                ownerIdx.addParameterOccurrence(ownerPositions);

                int[] keyPositions = new int[keyMapping.getNumberOfDatastoreMappings()];
                for (int k=0;k<keyPositions.length;k++)
                {
                    keyPositions[k] = inputParamNum++;
                }
                keyIdx.addParameterOccurrence(keyPositions);
            }
        }
        else
        {
            int[] ownerPositions = new int[ownerMapping.getNumberOfDatastoreMappings()];
            for (int k=0;k<ownerPositions.length;k++)
            {
                ownerPositions[k] = inputParamNum++;
            }
            ownerIdx.addParameterOccurrence(ownerPositions);

            int[] keyPositions = new int[keyMapping.getNumberOfDatastoreMappings()];
            for (int k=0;k<keyPositions.length;k++)
            {
                keyPositions[k] = inputParamNum++;
            }
            keyIdx.addParameterOccurrence(keyPositions);
        }
        getMappingParams = new StatementParameterMapping();
        getMappingParams.addMappingForParameter("owner", ownerIdx);
        getMappingParams.addMappingForParameter("key", keyIdx);

        return sqlStmt;
    }

    protected void clearInternal(ObjectProvider ownerSM)
    {
        try
        {
            ExecutionContext ec = ownerSM.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, clearStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    BackingStoreHelper.populateOwnerInStatement(ownerSM, ec, ps, jdbcPosition, this);
                    sqlControl.executeStatementUpdate(ec, mconn, clearStmt, ps, true);
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056013",clearStmt),e);
        }
    }

    protected void removeInternal(ObjectProvider sm, Object key)
    {
        ExecutionContext ec = sm.getExecutionContext();
        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, removeStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    BackingStoreHelper.populateKeyInStatement(ec, ps, key, jdbcPosition, keyMapping);
                    sqlControl.executeStatementUpdate(ec, mconn, removeStmt, ps, true);
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056012",removeStmt),e);
        }
    }

    protected SetStore newMapKeySetStore()
    {
        return new RDBMSMapKeySetStore((MapTable)mapTable, this, clr);
    }

    protected SetStore newMapValueSetStore()
    {
        return new RDBMSMapValueSetStore((MapTable)mapTable, this, clr);
    }

    protected SetStore newMapEntrySetStore()
    {
        return new RDBMSMapEntrySetStore((MapTable)mapTable, this, clr);
    }

    /**
     * Method to process an "update" statement (where the key already has a value in the join table).
     * @param ownerSM StateManager for the owner
     * @param conn The Connection
     * @param batched Whether we are batching it
     * @param key The key
     * @param value The new value
     * @param executeNow Whether to execute the statement now or wait til any batch
     * @throws MappedDatastoreException Thrown if an error occurs
     */
    protected void internalUpdate(ObjectProvider ownerSM, ManagedConnection conn, boolean batched, Object key, Object value,
                                  boolean executeNow) throws MappedDatastoreException
    {
        ExecutionContext ec = ownerSM.getExecutionContext();
        SQLController sqlControl = storeMgr.getSQLController();
        try {
            PreparedStatement ps = sqlControl.getStatementForUpdate(conn, updateStmt, false);
            try
            {
                int jdbcPosition = 1;
                if (valueMapping != null)
                {
                    jdbcPosition = BackingStoreHelper.populateValueInStatement(ec, ps, value, 
                        jdbcPosition, valueMapping);
                }
                else
                {
                    jdbcPosition = BackingStoreHelper.populateEmbeddedValueFieldsInStatement(ownerSM, value,
                        ps, jdbcPosition, (JoinTable)mapTable, this);
                }
                jdbcPosition = BackingStoreHelper.populateOwnerInStatement(ownerSM, ec, ps, jdbcPosition, this);
                jdbcPosition = BackingStoreHelper.populateKeyInStatement(ec, ps, key, jdbcPosition, keyMapping);

                if (batched)
                {
                    ps.addBatch();
                }
                else
                {
                    sqlControl.executeStatementUpdate(ec, conn, updateStmt, ps, true);
                }
            }
            finally
            {
                sqlControl.closeStatement(conn, ps);
            }
        }
        catch (SQLException e)
        {
            throw new MappedDatastoreException(getUpdateStmt(), e);
        }
    }

    /**
     * Method to process a "put" statement (where the key has no value in the join table).
     * @param ownerSM StateManager for the owner
     * @param conn The Connection
     * @param batched Whether we are batching it
     * @param key The key
     * @param value The value
     * @param executeNow Whether to execute the statement now or wait til batching
     * @return The return codes from any executed statement
     * @throws MappedDatastoreException Thrown if an error occurs
     */
    protected int[] internalPut(ObjectProvider ownerSM, ManagedConnection conn, boolean batched, Object key, Object value, boolean executeNow)
        throws MappedDatastoreException
    {
        ExecutionContext ec = ownerSM.getExecutionContext();
        SQLController sqlControl = storeMgr.getSQLController();
        try
        {
            PreparedStatement ps = sqlControl.getStatementForUpdate(conn, putStmt, false);
            try
            {
                int jdbcPosition = 1;
                if (valueMapping != null)
                {
                    jdbcPosition = BackingStoreHelper.populateValueInStatement(ec, ps, value,
                        jdbcPosition, valueMapping);
                }
                else
                {
                    jdbcPosition = BackingStoreHelper.populateEmbeddedValueFieldsInStatement(ownerSM, value,
                        ps, jdbcPosition, (JoinTable)mapTable, this);
                }
                jdbcPosition = BackingStoreHelper.populateOwnerInStatement(ownerSM, ec, ps, jdbcPosition, this);
                if (adapterMapping != null)
                {
                    // Only set the adapter mapping if we have a new object
                    long nextIDAdapter = getNextIDForAdapterColumn(ownerSM);
                    adapterMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, adapterMapping),
                        Long.valueOf(nextIDAdapter));
                    jdbcPosition += adapterMapping.getNumberOfDatastoreMappings();
                }
                jdbcPosition = BackingStoreHelper.populateKeyInStatement(ec, ps, key, jdbcPosition, keyMapping);

                // Execute the statement
                return sqlControl.executeStatementUpdate(ec, conn, putStmt, ps, true);
            }
            finally
            {
                sqlControl.closeStatement(conn, ps);
            }
        }
        catch (SQLException e)
        {
            throw new MappedDatastoreException(getPutStmt(), e);
        }
    }

    /**
     * Accessor for the higher id when elements primary key can't be part of
     * the primary key by datastore limitations like BLOB types can't be
     * primary keys.
     * @param sm State Manager for container
     * @return The next id
     */
    private int getNextIDForAdapterColumn(ObjectProvider sm)
    {
        int nextID;
        try
        {
            ExecutionContext ec = sm.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                String stmt = getMaxAdapterColumnIdStmt();
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, stmt);

                try
                {
                    int jdbcPosition = 1;
                    BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, stmt, ps);
                    try
                    {
                        if (!rs.next())
                        {
                            nextID = 1;
                        }
                        else
                        {
                            nextID = rs.getInt(1)+1;
                        }

                        JDBCUtils.logWarnings(rs);
                    }
                    finally
                    {
                        rs.close();
                    }
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056020",getMaxAdapterColumnIdStmt()),e);
        }

        return nextID;
    }
    /**
     * Generate statement for obtaining the maximum id.
     * <PRE>
     * SELECT MAX(SCOID) FROM MAPTABLE
     * WHERE OWNERCOL=?
     * </PRE>
     * @return The Statement returning the higher id
     */
    private String getMaxAdapterColumnIdStmt()
    {
        StringBuffer stmt = new StringBuffer("SELECT MAX(" + 
            adapterMapping.getDatastoreMapping(0).getDatastoreField().getIdentifier().toString() + ")");
        stmt.append(" FROM ");
        stmt.append(mapTable.toString());
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);

        return stmt.toString();
    }
}