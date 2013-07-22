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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.EmbeddedKeyPCMapping;
import org.datanucleus.store.mapped.mapping.EmbeddedValuePCMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.SerialisedPCMapping;
import org.datanucleus.store.mapped.mapping.SerialisedReferenceMapping;
import org.datanucleus.store.rdbms.JDBCUtils;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.query.StatementParameterMapping;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.store.rdbms.table.JoinTable;
import org.datanucleus.store.rdbms.table.MapTable;
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.scostore.SetStore;

/**
 * RDBMS-specific implementation of a SetStore for map entries.
 */
class RDBMSMapEntrySetStore extends BaseContainerStore implements SetStore
{
    /** Table containing the key and value forming the entry. */
    protected DatastoreContainerObject mapTable;

    /** The backing store for the Map. */
    protected MapStore mapStore;

    /** Mapping for the key. */
    protected JavaTypeMapping keyMapping;

    /** Mapping for the value. */
    protected JavaTypeMapping valueMapping;

    protected ClassLoaderResolver clr;

    private String sizeStmt;

    /** JDBC statement to use for retrieving keys of the map (locking). */
    private String iteratorStmtLocked = null;

    /** JDBC statement to use for retrieving keys of the map (not locking). */
    private String iteratorStmtUnlocked = null;

    private StatementParameterMapping iteratorMappingParams = null;
    private int[] iteratorKeyResultCols = null;
    private int[] iteratorValueResultCols = null;

    /**
     * Constructor for a store of the entries in a map when represented in a join table.
     * @param mapTable Table for the map
     * @param mapStore Store in use by the Map
     * @param clr ClassLoader resolver
     */
    RDBMSMapEntrySetStore(MapTable mapTable, MapStore mapStore, ClassLoaderResolver clr)
    {
        super((RDBMSStoreManager) mapTable.getStoreManager());

        this.mapTable = mapTable;
        this.mapStore = mapStore;
        this.ownerMapping = mapTable.getOwnerMapping();
        this.keyMapping   = mapTable.getKeyMapping();
        this.valueMapping = mapTable.getValueMapping();
        this.ownerMemberMetaData = mapTable.getOwnerMemberMetaData();
        this.clr = clr;
    }

    /**
     * Constructor for a store of the entries in a map when represented by either the key table or value table.
     * @param mapTable The table storing the map relation (key table or value table)
     * @param mapStore The backing store for the map itself
     * @param clr ClassLoader resolver
     * @param ownerMapping Mapping from this table to the owner
     * @param keyMapping Mapping of the key (in this table)
     * @param valueMapping Mapping of the value (in this table)
     * @param ownerMmd Metadata for the owning field/property
     */
    RDBMSMapEntrySetStore(DatastoreContainerObject mapTable, MapStore mapStore, ClassLoaderResolver clr, 
        JavaTypeMapping ownerMapping, JavaTypeMapping keyMapping, JavaTypeMapping valueMapping,
        AbstractMemberMetaData ownerMmd)
    {
        super((RDBMSStoreManager) mapTable.getStoreManager());

        this.mapTable = mapTable;
        this.mapStore = mapStore;
        this.ownerMapping = ownerMapping;
        this.keyMapping   = keyMapping;
        this.valueMapping = valueMapping;
        this.ownerMemberMetaData = ownerMmd;
        this.clr = clr;
    }

    /**
     * Accessor for whether this store has an order mapping to allow for duplicates, or ordering.
     * @return Whether it has an order mapping.
     */
    public boolean hasOrderMapping()
    {
        return false;
    }

    /**
     * Method to update an embedded element.
     * @param sm State Manager of the owner
     * @param element The element to update
     * @param fieldNumber The number of the field to update
     * @param value The value
     * @return Whether the element was modified
     */
    public boolean updateEmbeddedElement(ObjectProvider sm, Object element, int fieldNumber, Object value)
    {
        // Do nothing since of no use here
        return false;
    }

    /**
     * Accessor for the owner mapping.
     * @return The owner mapping
     */
    public JavaTypeMapping getOwnerMapping()
    {
        return ownerMapping;
    }

    protected boolean validateElementType(Object element)
    {
        return element instanceof Entry;
    }

    /**
     * Method to update the collection to be the supplied collection of elements.
     * @param sm StateManager of the object
     * @param coll The collection to use
     */
    public void update(ObjectProvider sm, Collection coll)
    {
        // Crude update - remove existing and add new!
        // TODO Update this to just remove what is not needed, and add what is really new
        clear(sm);
        addAll(sm, coll, 0);
    }

    public boolean contains(ObjectProvider sm, Object element)
    {
        if (!validateElementType(element))
        {
            return false;
        }
        Entry entry = (Entry)element;

        return mapStore.containsKey(sm, entry.getKey());
    }

    /**
     * Method to add an entry to the Map.
     * @param sm State Manager for the owner
     * @param element Entry to add
     * @return Whether it was added
     */
    public boolean add(ObjectProvider sm, Object element, int size)
    {
        throw new UnsupportedOperationException("Cannot add to a map through its entry set");
    }

    /**
     * Method to add entries to the Map.
     * @param sm State Manager for the owner
     * @param elements Entries to add
     * @return Whether they were added
     */
    public boolean addAll(ObjectProvider sm, Collection elements, int size)
    {
        throw new UnsupportedOperationException("Cannot add to a map through its entry set");
    }

    /**
     * Method to remove an entry from the Map.
     * @param sm State Manager for the owner
     * @param element Entry to remove
     * @return Whether it was removed
     */
    public boolean remove(ObjectProvider sm, Object element, int size, boolean allowDependentField)
    {
        if (!validateElementType(element))
        {
            return false;
        }

        Entry entry = (Entry)element;
        Object removed = mapStore.remove(sm, entry.getKey());

        // NOTE: this may not return an accurate result if a null value is being removed
        return removed == null ? entry.getValue() == null : removed.equals(entry.getValue());
    }

    /**
     * Method to remove entries from the Map.
     * @param sm State Manager for the owner
     * @param elements Entries to remove
     * @return Whether they were removed
     */
    public boolean removeAll(ObjectProvider sm, Collection elements, int size)
    {
        if (elements == null || elements.size() == 0)
        {
            return false;
        }

        Iterator iter=elements.iterator();
        boolean modified=false;
        while (iter.hasNext())
        {
            Object element=iter.next();
            Entry entry = (Entry)element;

            Object removed = mapStore.remove(sm, entry.getKey());

            // NOTE: this may not return an accurate result if a null value is being removed.
            modified = removed == null ? entry.getValue() == null : removed.equals(entry.getValue());
        }

        return modified;
    }

    /**
     * Method to clear the Map.
     * @param sm State Manager for the owner.
     */
    public void clear(ObjectProvider sm)
    {
        mapStore.clear(sm);
    }

    public MapStore getMapStore()
    {
        return mapStore;
    }

    public JavaTypeMapping getKeyMapping()
    {
        return keyMapping;
    }

    public JavaTypeMapping getValueMapping()
    {
        return valueMapping;
    }

    public int size(ObjectProvider sm)
    {
        int numRows;

        String stmt = getSizeStmt();
        try
        {
            ExecutionContext ec = sm.getExecutionContext();

            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
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
                            throw new NucleusDataStoreException("Size request returned no result row: " + stmt);
                        }
                        numRows = rs.getInt(1);
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
            throw new NucleusDataStoreException("Size request failed: " + stmt, e);
        }

        return numRows;
    }

    /**
     * Method to return a size statement.
     * <PRE>
     * SELECT COUNT(*) FROM MAP_TABLE WHERE OWNER=? AND KEY IS NOT NULL
     * </PRE>
     * @return The size statement
     */
    private String getSizeStmt()
    {
        if (sizeStmt == null)
        {
            StringBuffer stmt = new StringBuffer("SELECT COUNT(*) FROM ");
            stmt.append(mapTable.toString());
            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            if (keyMapping != null)
            {
                // We don't accept null keys
                for (int i=0; i<keyMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(" AND ");
                    stmt.append(keyMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append(" IS NOT NULL");
                }
            }
            sizeStmt = stmt.toString();
        }
        return sizeStmt;
    }

    /**
     * Method returning an iterator across the entries in the map for this owner object.
     * @param ownerSM StateManager of the owning object
     * @return The iterator for the entries (<pre>map.entrySet().iterator()</pre>).
     */
    public Iterator iterator(ObjectProvider ownerSM)
    {
        ExecutionContext ec = ownerSM.getExecutionContext();
        if (iteratorStmtLocked == null)
        {
            synchronized (this) // Make sure this completes in case another thread needs the same info
            {
                // Generate the statement, and statement mapping/parameter information
                SQLStatement sqlStmt = getSQLStatementForIterator(ownerSM);
                iteratorStmtUnlocked = sqlStmt.getSelectStatement().toSQL();
                sqlStmt.addExtension("lock-for-update", true);
                iteratorStmtLocked = sqlStmt.getSelectStatement().toSQL();
            }
        }

        Transaction tx = ec.getTransaction();
        String stmt = (tx.lockReadObjects() ? iteratorStmtLocked : iteratorStmtUnlocked);
        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                // Create the statement and set the owner
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, stmt);
                StatementMappingIndex ownerIdx = iteratorMappingParams.getMappingForParameter("owner");
                int numParams = ownerIdx.getNumberOfParameterOccurrences();
                for (int paramInstance=0;paramInstance<numParams;paramInstance++)
                {
                    ownerIdx.getMapping().setObject(ec, ps,
                        ownerIdx.getParameterPositionsForOccurrence(paramInstance), ownerSM.getObject());
                }

                try
                {
                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, stmt, ps);
                    try
                    {
                        AbstractMemberMetaData ownerMemberMetaData = null;
                        if (mapTable instanceof JoinTable)
                        {
                            ownerMemberMetaData = ((JoinTable) mapTable).getOwnerMemberMetaData();
                        }

                        return new SetIterator(ownerSM, this, ownerMemberMetaData, rs, 
                            iteratorKeyResultCols, iteratorValueResultCols)
                        {
                            protected boolean next(Object rs) throws MappedDatastoreException
                            {
                                try
                                {
                                    return ((ResultSet) rs).next();
                                }
                                catch (SQLException e)
                                {
                                    throw new MappedDatastoreException("SQLException", e);
                                }
                            }
                        };
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
            throw new NucleusDataStoreException("Iteration request failed: " + stmt, e);
        }
        catch (MappedDatastoreException e)
        {
            throw new NucleusDataStoreException("Iteration request failed: " + stmt, e);
        }
    }

    /**
     * Method to generate an SQLStatement for iterating through entries of the map.
     * Creates a statement that selects the table holding the map definition (key/value mappings).
     * Adds a restriction on the ownerMapping of the containerTable so we can restrict to the owner object.
     * Adds a restriction on the keyMapping not being null.
     * <pre>
     * SELECT KEY, VALUE FROM MAP_TABLE WHERE OWNER_ID=? AND KEY IS NOT NULL
     * </pre>
     * @param ownerSM StateManager for the owner object
     * @return The SQLStatement
     */
    protected SQLStatement getSQLStatementForIterator(ObjectProvider ownerSM)
    {
        SQLStatement sqlStmt = new SQLStatement(storeMgr, mapTable, null, null);
        sqlStmt.setClassLoaderResolver(clr);
        iteratorKeyResultCols = sqlStmt.select(sqlStmt.getPrimaryTable(), keyMapping, null);
        iteratorValueResultCols = sqlStmt.select(sqlStmt.getPrimaryTable(), valueMapping, null);

        // Apply condition on owner field to filter by owner
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        SQLTable ownerSqlTbl =
            SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), ownerMapping);
        SQLExpression ownerExpr = exprFactory.newExpression(sqlStmt, ownerSqlTbl, ownerMapping);
        SQLExpression ownerVal = exprFactory.newLiteralParameter(sqlStmt, ownerMapping, null, "OWNER");
        sqlStmt.whereAnd(ownerExpr.eq(ownerVal), true);

        // Apply condition that key is not null
        SQLExpression keyExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), keyMapping);
        SQLExpression nullExpr = exprFactory.newLiteral(sqlStmt, null, null);
        sqlStmt.whereAnd(keyExpr.ne(nullExpr), true);

        // Input parameter(s) - the owner
        int inputParamNum = 1;
        StatementMappingIndex ownerIdx = new StatementMappingIndex(ownerMapping);
        if (sqlStmt.getNumberOfUnions() > 0)
        {
            // Add parameter occurrence for each union of statement
            for (int j=0;j<sqlStmt.getNumberOfUnions()+1;j++)
            {
                int[] paramPositions = new int[ownerMapping.getNumberOfDatastoreMappings()];
                for (int k=0;k<ownerMapping.getNumberOfDatastoreMappings();k++)
                {
                    paramPositions[k] = inputParamNum++;
                }
                ownerIdx.addParameterOccurrence(paramPositions);
            }
        }
        else
        {
            int[] paramPositions = new int[ownerMapping.getNumberOfDatastoreMappings()];
            for (int k=0;k<ownerMapping.getNumberOfDatastoreMappings();k++)
            {
                paramPositions[k] = inputParamNum++;
            }
            ownerIdx.addParameterOccurrence(paramPositions);
        }
        iteratorMappingParams = new StatementParameterMapping();
        iteratorMappingParams.addMappingForParameter("owner", ownerIdx);

        return sqlStmt;
    }

    /**
     * Inner class representing an iterator for the Set.
     */
    public static abstract class SetIterator implements Iterator
    {
        private final ObjectProvider sm;
        private final ExecutionContext ec;
        private final Iterator delegate;
        private Entry lastElement = null;
        private final RDBMSMapEntrySetStore setStore;

        /**
         * Constructor for iterating the Set of entry.
         * @param sm the StateManager
         * @param rs the ResultSet
         * @param setStore the set store
         * @param ownerMmd the owner member meta data - can be null
         * @throws MappedDatastoreException
         */
        protected SetIterator(ObjectProvider sm, RDBMSMapEntrySetStore setStore, AbstractMemberMetaData ownerMmd,
                Object rs, int[] keyResultCols, int[] valueResultCols) throws MappedDatastoreException
        {
            this.sm = sm;
            this.ec = sm.getExecutionContext();
            this.setStore = setStore;

            ArrayList results = new ArrayList();
            while (next(rs))
            {
                Object key = null;
                Object value = null;

                int ownerFieldNum = -1;
                if (ownerMmd != null)
                {
                    ownerFieldNum = ownerMmd.getAbsoluteFieldNumber();
                }

                JavaTypeMapping keyMapping = setStore.getKeyMapping();
                if (keyMapping instanceof EmbeddedKeyPCMapping ||
                    keyMapping instanceof SerialisedPCMapping ||
                    keyMapping instanceof SerialisedReferenceMapping)
                {
                    key = keyMapping.getObject(ec, rs, keyResultCols, sm, ownerFieldNum);
                }
                else
                {
                    key = keyMapping.getObject(ec, rs, keyResultCols);
                }

                JavaTypeMapping valueMapping = setStore.getValueMapping();
                if (valueMapping instanceof EmbeddedValuePCMapping ||
                    valueMapping instanceof SerialisedPCMapping ||
                    valueMapping instanceof SerialisedReferenceMapping)
                {
                    value = valueMapping.getObject(ec, rs, valueResultCols, sm, ownerFieldNum);
                }
                else
                {
                    value = valueMapping.getObject(ec, rs, valueResultCols);
                }

                results.add(new EntryImpl(sm, key, value, setStore.getMapStore()));
            }

            delegate = results.iterator();
        }

        public boolean hasNext()
        {
            return delegate.hasNext();
        }

        public Object next()
        {
            lastElement = (Entry)delegate.next();

            return lastElement;
        }

        public synchronized void remove()
        {
            if (lastElement == null)
            {
                throw new IllegalStateException("No entry to remove");
            }

            setStore.getMapStore().remove(sm, lastElement.getKey());
            delegate.remove();

            lastElement = null;
        }

        protected abstract boolean next(Object rs) throws MappedDatastoreException;
    }

    /**
     * Inner class representing the entry.
     **/
    private static class EntryImpl implements Entry
    {
        private final ObjectProvider sm;
        private final Object key;
        private final Object value;
        private final MapStore mapStore;

        /**
         * Entry constructor
         * @param sm the StateManager
         * @param key the key
         * @param value the value
         */
        public EntryImpl(ObjectProvider sm, Object key, Object value, MapStore mapStore)
        {
            this.sm = sm;
            this.key = key;
            this.value = value;
            this.mapStore = mapStore;
        }

        public int hashCode()
        {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        public boolean equals(Object o)
        {
            if (o == this)
            {
                return true;
            }
            if (!(o instanceof Entry))
            {
                return false;
            }

            Entry e = (Entry)o;
            return (key == null ? e.getKey() == null : key.equals(e.getKey())) &&
                   (value == null ? e.getValue() == null : value.equals(e.getValue()));
        }

        /**
         * Accessor for the Key.
         * @return The Key.
         **/
        public Object getKey()
        {
            return key;
        }

        /**
         * Accessor for the Value.
         * @return The Value.
         **/
        public Object getValue()
        {
            return value;
        }

        /**
         * Mutator for the Value.
         * @param value The Value.
         * @return the previous value, or <code>null</code> if none.
         **/
        public Object setValue(Object value)
        {
            return mapStore.put(sm, key, value);
        }
    }
}