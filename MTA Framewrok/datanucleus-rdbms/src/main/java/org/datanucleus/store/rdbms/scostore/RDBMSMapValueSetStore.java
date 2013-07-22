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
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.MapMetaData.MapType;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingHelper;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.JDBCUtils;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.query.StatementParameterMapping;
import org.datanucleus.store.rdbms.sql.DiscriminatorStatementGenerator;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.StatementGenerator;
import org.datanucleus.store.rdbms.sql.UnionStatementGenerator;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.store.rdbms.table.MapTable;
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.util.ClassUtils;

/**
 * RDBMS-specific implementation of a SetStore for map values.
 */
class RDBMSMapValueSetStore extends AbstractSetStore
{
    protected final MapStore mapStore;

    protected final JavaTypeMapping keyMapping;

    private String findKeyStmt;

    /** JDBC statement to use for retrieving keys of the map (locking). */
    private String iteratorStmtLocked = null;

    /** JDBC statement to use for retrieving keys of the map (not locking). */
    private String iteratorStmtUnlocked = null;

    private StatementClassMapping iteratorMappingDef = null;
    private StatementParameterMapping iteratorMappingParams = null;

    /**
     * Constructor where a join table is used to store the map relation.
     * @param mapTable Table used by the map
     * @param mapStore Backing store used by the map
     * @param clr The ClassLoaderResolver
     * @param storeMgr Manager for the datastore
     */
    RDBMSMapValueSetStore(MapTable mapTable, MapStore mapStore, ClassLoaderResolver clr)
    {
        super((RDBMSStoreManager) mapTable.getStoreManager(), clr);

        this.containerTable = mapTable;
        this.mapStore = mapStore;
        this.ownerMapping = mapTable.getOwnerMapping();
        this.keyMapping = mapTable.getKeyMapping();
        this.elementMapping = mapTable.getValueMapping();
        this.elementType = elementMapping.getType();
        this.ownerMemberMetaData = mapTable.getOwnerMemberMetaData();

        initialize(clr);

        if (keyMapping != null)
        {
            findKeyStmt = getFindKeyStmt();
        }
        else
        {
            findKeyStmt = null;
        }
    }

    /**
     * Constructor when we have the key stored as an FK in the value, or the value stored as an FK in the key.
     * @param mapTable Table handling the map relation (can be key table or value table)
     * @param mapStore Backing store for the map
     * @param clr ClassLoader resolver
     * @param ownerMapping mapping back to the owner
     * @param valueMapping mapping to the key/value
     * @param ownerMmd Metadata for the owning member
     */
    RDBMSMapValueSetStore(DatastoreClass mapTable, MapStore mapStore, ClassLoaderResolver clr, 
        JavaTypeMapping ownerMapping, JavaTypeMapping valueMapping, AbstractMemberMetaData ownerMmd)
    {
        super((RDBMSStoreManager) mapTable.getStoreManager(), clr);

        this.containerTable = mapTable;
        this.mapStore = mapStore;
        this.ownerMapping = ownerMapping;
        this.keyMapping = null;
        this.elementMapping = valueMapping;
        this.ownerMemberMetaData = ownerMmd;

        initialize(clr);

        if (keyMapping != null)
        {
            findKeyStmt = getFindKeyStmt();
        }
        else
        {
            findKeyStmt = null;
        }
    }

    /**
     * Initialise Method.
     * @param clr ClassLoader resolver
     */
    private void initialize(ClassLoaderResolver clr)
    {
        elementType = elementMapping.getType();
        elementsAreEmbedded = isEmbeddedMapping(elementMapping);
        elementsAreSerialised = isEmbeddedMapping(elementMapping);

        Class valueCls = clr.classForName(elementType);
        if (ClassUtils.isReferenceType(valueCls))
        {
            emd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForImplementationOfReference(valueCls,null,clr);
        }
        else
        {
            emd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(valueCls, clr);
        }
        if (emd != null)
        {
            elementType = emd.getFullClassName();
            elementInfo = getElementInformationForClass();
        }
    }

    /**
     * Method to add a value to the Map. Not supported.
     * @param sm State Manager for the owner
     * @param element The value to add
     * @return Whether it was added correctly.
     */
    public boolean add(ObjectProvider sm, Object element, int size)
    {
        throw new UnsupportedOperationException("Cannot add to a map through its values collection");
    }

    /**
     * Method to add entries to the Map. Not supported.
     * @param sm State Manager for the owner
     * @param elements The values to add
     * @return Whether it was added correctly.
     */
    public boolean addAll(ObjectProvider sm, Collection elements, int size)
    {
        throw new UnsupportedOperationException("Cannot add to a map through its values collection");
    }

    /**
     * Method to remove a value from the Map.
     * @param sm State Manager for the owner
     * @param element The value to remove
     * @return Whether it was removed correctly.
     */
    public boolean remove(ObjectProvider sm, Object element, int size, boolean allowDependentField)
    {
        if (!validateElementForReading(sm, element))
        {
            return false;
        }

        return remove(sm, element);
    }

    /**
     * Method to remove values from the map via the value collection.
     * @param sm StateManager for the owner
     */
    public boolean removeAll(ObjectProvider sm, Collection elements, int size)
    {
        throw new NucleusUserException("Cannot remove values from a map through its values collection");
    }

    /**
     * Method to clear the map.
     * @param sm StateManager for the owner
     */
    public void clear(ObjectProvider sm)
    {
        if (canClear())
        {
            throw new NucleusUserException("Cannot clear a map through its values collection");
        }

        super.clear(sm);
    }

    protected boolean canClear()
    {
        return false;
    }

    protected boolean remove(ObjectProvider sm, Object element)
    {
        if (findKeyStmt == null)
        {
            throw new UnsupportedOperationException("Cannot remove from a map through its values collection");
        }

        Object key = null;
        boolean keyExists = false;
        ExecutionContext ec = sm.getExecutionContext();

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();

            try
            {
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, findKeyStmt);

                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    BackingStoreHelper.populateElementInStatement(ec, ps, element, jdbcPosition, elementMapping);

                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, findKeyStmt, ps);
                    try
                    {
                        if (rs.next())
                        {
                            key = keyMapping.getObject(ec, rs, MappingHelper.getMappingIndices(1,keyMapping));
                            keyExists = true;
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
            throw new NucleusDataStoreException("Request failed to check if set contains an element: " + findKeyStmt, e);
        }

        if (keyExists)
        {
            mapStore.remove(sm, key);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Generate statement to find the first key for a value in the Map.
     * <PRE>
     * SELECT KEYCOL FROM SETTABLE
     * WHERE OWNERCOL=?
     * AND ELEMENTCOL = ?
     * </PRE>
     * @return Statement to find keys in the Map.
     */
    private String getFindKeyStmt()
    {
        StringBuffer stmt = new StringBuffer("SELECT ");
        for (int i=0; i<keyMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(keyMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
        }
        stmt.append(" FROM ");
        stmt.append(containerTable.toString());
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
        BackingStoreHelper.appendWhereClauseForMapping(stmt, elementMapping, null, false);

        return stmt.toString();
    }

    /**
     * Accessor for an iterator for the set.
     * @param ownerSM State Manager for the set. 
     * @return Iterator for the set.
     **/
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
                        ResultObjectFactory rof = null;
                        if (elementsAreEmbedded || elementsAreSerialised)
                        {
                            // No ResultObjectFactory needed - handled by SetStoreIterator
                            return new RDBMSSetStoreIterator(ownerSM, rs, null, this);
                        }
                        else
                        {
                            rof = storeMgr.newResultObjectFactory(emd, iteratorMappingDef, false, null,
                                        clr.classForName(elementType));
                            return new RDBMSSetStoreIterator(ownerSM, rs, rof, this);
                        }
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
            throw new NucleusDataStoreException(LOCALISER.msg("056006", stmt),e);
        }
        catch (MappedDatastoreException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056006", stmt),e);
        }
    }

    /**
     * Method to generate an SQLStatement for iterating through values of the map.
     * Populates the iteratorMappingDef and iteratorMappingParams.
     * Creates a statement that selects the value table(s), and adds any necessary join to the containerTable
     * if that is not the value table. If the value is embedded then selects the table it is embedded in.
     * Adds a restriction on the ownerMapping of the containerTable so we can restrict to the owner object.
     * @param ownerSM StateManager for the owner object
     * @return The SQLStatement
     */
    protected SQLStatement getSQLStatementForIterator(ObjectProvider ownerSM)
    {
        SQLStatement sqlStmt = null;

        final ClassLoaderResolver clr = ownerSM.getExecutionContext().getClassLoaderResolver();
        final Class valueCls = clr.classForName(elementType);
        SQLTable containerSqlTbl = null;
        MapType mapType = getOwnerMemberMetaData().getMap().getMapType();
        if (emd != null && emd.getDiscriminatorStrategyForTable() != null &&
            emd.getDiscriminatorStrategyForTable() != DiscriminatorStrategy.NONE)
        {
            // Map<?, PC> where value has discriminator
            if (ClassUtils.isReferenceType(valueCls))
            {
                // Take the metadata for the first implementation of the reference type
                String[] clsNames = storeMgr.getNucleusContext().getMetaDataManager().getClassesImplementingInterface(elementType, clr);
                Class[] cls = new Class[clsNames.length];
                for (int j=0; j<clsNames.length; j++)
                {
                    cls[j] = clr.classForName(clsNames[j]);
                }
                StatementGenerator stmtGen = new DiscriminatorStatementGenerator(storeMgr, clr, cls, true, null, null);
                sqlStmt = stmtGen.getStatement();
            }
            else
            {
                StatementGenerator stmtGen = new DiscriminatorStatementGenerator(storeMgr, clr, valueCls, true, null, null);
                sqlStmt = stmtGen.getStatement();
            }
            iterateUsingDiscriminator = true;

            if (mapType == MapType.MAP_TYPE_VALUE_IN_KEY)
            {
                // Join to key table and select value fields
                JavaTypeMapping valueIdMapping = sqlStmt.getPrimaryTable().getTable().getIdMapping();
                containerSqlTbl = sqlStmt.innerJoin(sqlStmt.getPrimaryTable(), valueIdMapping,
                    containerTable, null, elementMapping, null, null);

                iteratorMappingDef = new StatementClassMapping();
                SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                    ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
            }
            else if (mapType == MapType.MAP_TYPE_KEY_IN_VALUE)
            {
                // Select value fields
                containerSqlTbl = sqlStmt.getPrimaryTable();

                iteratorMappingDef = new StatementClassMapping();
                SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                    ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
            }
            else
            {
                // Join to join table and select value fields
                JavaTypeMapping valueIdMapping = sqlStmt.getPrimaryTable().getTable().getIdMapping();
                containerSqlTbl = sqlStmt.innerJoin(sqlStmt.getPrimaryTable(), valueIdMapping,
                    containerTable, null, elementMapping, null, null);

                iteratorMappingDef = new StatementClassMapping();
                SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                    ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
            }
        }
        else
        {
            if (mapType == MapType.MAP_TYPE_VALUE_IN_KEY)
            {
                if (emd != null)
                {
                    // TODO Allow for null value [change to select the key table and left outer join to the key]
                    // Select of value table, joining to key table
                    iteratorMappingDef = new StatementClassMapping();
                    UnionStatementGenerator stmtGen = new UnionStatementGenerator(storeMgr, clr, valueCls, true, null, null);
                    stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
                    iteratorMappingDef.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
                    sqlStmt = stmtGen.getStatement();

                    JavaTypeMapping valueIdMapping = sqlStmt.getPrimaryTable().getTable().getIdMapping();
                    containerSqlTbl = sqlStmt.innerJoin(sqlStmt.getPrimaryTable(), valueIdMapping,
                        containerTable, null, elementMapping, null, null);

                    SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                        ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
                }
                else
                {
                    // Select of value in key table
                    sqlStmt = new SQLStatement(storeMgr, containerTable, null, null);
                    sqlStmt.setClassLoaderResolver(clr);
                    containerSqlTbl = sqlStmt.getPrimaryTable();
                    sqlStmt.select(sqlStmt.getPrimaryTable(), elementMapping, null);
                }
            }
            else if (mapType == MapType.MAP_TYPE_KEY_IN_VALUE)
            {
                // Select of value in value table (allow union of possible value types)
                iteratorMappingDef = new StatementClassMapping();
                UnionStatementGenerator stmtGen = new UnionStatementGenerator(storeMgr, clr, valueCls, true, null, null);
                stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
                iteratorMappingDef.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
                sqlStmt = stmtGen.getStatement();
                containerSqlTbl = sqlStmt.getPrimaryTable();

                SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                    ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
            }
            else
            {
                if (emd != null)
                {
                    // TODO Allow for null value [change to select the join table and left outer join to the key]
                    // Select of value table, joining to key table
                    iteratorMappingDef = new StatementClassMapping();
                    UnionStatementGenerator stmtGen = new UnionStatementGenerator(storeMgr, clr, valueCls, true, null, null);
                    stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
                    iteratorMappingDef.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
                    sqlStmt = stmtGen.getStatement();

                    JavaTypeMapping valueIdMapping = sqlStmt.getPrimaryTable().getTable().getIdMapping();
                    containerSqlTbl = sqlStmt.innerJoin(sqlStmt.getPrimaryTable(), valueIdMapping,
                        containerTable, null, elementMapping, null, null);

                    SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                        ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
                }
                else
                {
                    // Select of value in join table
                    sqlStmt = new SQLStatement(storeMgr, containerTable, null, null);
                    containerSqlTbl = sqlStmt.getPrimaryTable();
                    sqlStmt.select(sqlStmt.getPrimaryTable(), elementMapping, null);
                }
            }
        }

        // Apply condition on owner field to filter by owner
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        SQLTable ownerSqlTbl =
            SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, containerSqlTbl, ownerMapping);
        SQLExpression ownerExpr = exprFactory.newExpression(sqlStmt, ownerSqlTbl, ownerMapping);
        SQLExpression ownerVal = exprFactory.newLiteralParameter(sqlStmt, ownerMapping, null, "OWNER");
        sqlStmt.whereAnd(ownerExpr.eq(ownerVal), true);

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
}