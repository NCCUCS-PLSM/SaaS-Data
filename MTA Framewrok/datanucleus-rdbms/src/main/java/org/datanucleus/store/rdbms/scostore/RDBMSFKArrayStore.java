/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ArrayMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.mapped.mapping.MappingHelper;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.mapping.RDBMSMapping;
import org.datanucleus.store.rdbms.query.StatementParameterMapping;
import org.datanucleus.store.rdbms.sql.DiscriminatorStatementGenerator;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.StatementGenerator;
import org.datanucleus.store.rdbms.sql.UnionStatementGenerator;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * RDBMS-specific implementation of an FK ArrayStore.
 */
public class RDBMSFKArrayStore extends AbstractArrayStore
{
    /** Statement for nullifying a FK in the element. */
    private String clearNullifyStmt;

    /** Statement for updating a foreign key in a 1-N unidirectional */
    private String updateFkStmt;

    /** JDBC statement to use for retrieving keys of the map (locking). */
    private String iteratorStmtLocked = null;

    /** JDBC statement to use for retrieving keys of the map (not locking). */
    private String iteratorStmtUnlocked = null;

    private StatementClassMapping iteratorMappingDef = null;
    private StatementParameterMapping iteratorMappingParams = null;

    /**
     * @param mmd Metadata for the owning field/property
     * @param storeMgr Manager for the datastore
     * @param clr ClassLoader resolver
     */
    public RDBMSFKArrayStore(AbstractMemberMetaData mmd, RDBMSStoreManager storeMgr, ClassLoaderResolver clr)
    {
        super(storeMgr, clr);

        setOwner(mmd, clr);
        ArrayMetaData arrmd = mmd.getArray();
        if (arrmd == null)
        {
            throw new NucleusUserException(LOCALISER.msg("056000", mmd.getFullFieldName()));
        }

        // Load the element class
        elementType = mmd.getType().getComponentType().getName();
        Class element_class = clr.classForName(elementType);

        if (ClassUtils.isReferenceType(element_class))
        {
            // Take the metadata for the first implementation of the reference type
            emd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForImplementationOfReference(element_class,null,clr);
            if (emd != null)
            {
                // Pretend we have a relationship with this one implementation
                elementType = emd.getFullClassName();
            }
        }
        else
        {
            // Check that the element class has MetaData
            emd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(element_class, clr);
        }
        if (emd == null)
        {
            throw new NucleusUserException(LOCALISER.msg("056003", element_class.getName(), mmd.getFullFieldName()));
        }

        elementInfo = getElementInformationForClass();
        if (elementInfo != null && elementInfo.length > 1)
        {
            throw new NucleusUserException(LOCALISER.msg("056045", 
                ownerMemberMetaData.getFullFieldName()));
        }

        elementMapping = elementInfo[0].getDatastoreClass().getIdMapping(); // Just use the first element type as the guide for the element mapping
        elementsAreEmbedded = false; // Can't embed element when using FK relation
        elementsAreSerialised = false; // Can't serialise element when using FK relation

        // Get the field in the element table (if any)
        String mappedByFieldName = mmd.getMappedBy();
        if (mappedByFieldName != null)
        {
            // 1-N FK bidirectional
            // The element class has a field for the owner.
            AbstractMemberMetaData eofmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForMember(element_class, clr, mappedByFieldName);
            if (eofmd == null)
            {
                throw new NucleusUserException(LOCALISER.msg("056024", mmd.getFullFieldName(), 
                    mappedByFieldName, element_class.getName()));
            }

            // Check that the type of the element "mapped-by" field is consistent with the owner type
            if (!clr.isAssignableFrom(eofmd.getType(), mmd.getAbstractClassMetaData().getFullClassName()))
            {
                throw new NucleusUserException(LOCALISER.msg("056025", mmd.getFullFieldName(), 
                    eofmd.getFullFieldName(), eofmd.getTypeName(), mmd.getAbstractClassMetaData().getFullClassName()));
            }

            String ownerFieldName = eofmd.getName();
            ownerMapping = elementInfo[0].getDatastoreClass().getMemberMapping(eofmd);
            if (ownerMapping == null)
            {
                throw new NucleusUserException(LOCALISER.msg("056046", 
                    mmd.getAbstractClassMetaData().getFullClassName(), mmd.getName(), elementType, ownerFieldName));
            }
            if (isEmbeddedMapping(ownerMapping))
            {
                throw new NucleusUserException(LOCALISER.msg("056026",
                    ownerFieldName, elementType, eofmd.getTypeName(), mmd.getClassName()));
            }
        }
        else
        {
            // 1-N FK unidirectional
            // The element class knows nothing about the owner (but the table has external mappings)
            ownerMapping = elementInfo[0].getDatastoreClass().getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
            if (ownerMapping == null)
            {
                throw new NucleusUserException(LOCALISER.msg("056047",
                    mmd.getAbstractClassMetaData().getFullClassName(), mmd.getName(), elementType));
            }
        }

        orderMapping = elementInfo[0].getDatastoreClass().getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
        if (orderMapping == null)
        {
            throw new NucleusUserException(LOCALISER.msg("056048", 
                mmd.getAbstractClassMetaData().getFullClassName(), mmd.getName(), elementType));
        }

        relationDiscriminatorMapping = elementInfo[0].getDatastoreClass().getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK_DISCRIM);
        if (relationDiscriminatorMapping != null)
        {
            relationDiscriminatorValue = mmd.getValueForExtension("relation-discriminator-value");
            if (relationDiscriminatorValue == null)
            {
                // No value defined so just use the field name
                relationDiscriminatorValue = mmd.getFullFieldName();
            }
        }

        // TODO Cater for multiple element tables
        containerTable = elementInfo[0].getDatastoreClass();
        if (mmd.getMappedBy() != null && ownerMapping.getDatastoreContainer() != containerTable)
        {
            // Element and owner don't have consistent tables so use the one with the mapping
            // e.g collection is of subclass, yet superclass has the link back to the owner
            containerTable = ownerMapping.getDatastoreContainer();
        }
    }

    /**
     * Update a FK and element position in the element.
     * @param ownerSM StateManager for the owner
     * @param element The element to update
     * @param owner The owner object to set in the FK
     * @param index The index position (or -1 if not known)
     * @return Whether it was performed successfully
     */
    private boolean updateElementFk(ObjectProvider ownerSM, Object element, Object owner, int index)
    {
        if (element == null)
        {
            return false;
        }

        boolean retval;
        String updateFkStmt = getUpdateFkStmt();
        ExecutionContext ec = ownerSM.getExecutionContext();
        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, updateFkStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    if (elementInfo.length > 1)
                    {
                        DatastoreClass table = storeMgr.getDatastoreClass(element.getClass().getName(), clr);
                        if (table != null)
                        {
                            ps.setString(jdbcPosition++, table.toString());
                        }
                        else
                        {
                            NucleusLogger.PERSISTENCE.info(">> FKArrayStore.updateElementFK : " +
                                "need to set table in statement but dont know table where to store " + element);
                        }
                    }
                    if (owner == null)
                    {
                        ownerMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, ownerMapping), null);
                        jdbcPosition += ownerMapping.getNumberOfDatastoreMappings();
                    }
                    else
                    {
                        jdbcPosition = BackingStoreHelper.populateOwnerInStatement(ownerSM, ec, ps, jdbcPosition, this);
                    }
                    jdbcPosition = BackingStoreHelper.populateOrderInStatement(ec, ps, index, jdbcPosition, orderMapping);
                    if (relationDiscriminatorMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                    }
                    jdbcPosition = 
                        BackingStoreHelper.populateElementInStatement(ec, ps, element, jdbcPosition, elementMapping);

                    sqlControl.executeStatementUpdate(ec, mconn, updateFkStmt, ps, true);
                    retval = true;
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
            throw new NucleusDataStoreException(LOCALISER.msg("056027", updateFkStmt), e);
        }

        return retval;
    }

    /**
     * Generate statement for updating the owner, index columns in an inverse 1-N. 
     * Will result in the statement
     * <PRE>
     * UPDATE ELEMENTTABLE SET FK_COL_1=?, FK_COL_2=?, FK_IDX=? [,DISTINGUISHER=?]
     * WHERE ELEMENT_ID=?
     * </PRE>
     * when we have a single element table, and
     * <PRE>
     * UPDATE ? SET FK_COL_1=?, FK_COL_2=?, FK_IDX=? [,DISTINGUISHER=?]
     * WHERE ELEMENT_ID=?
     * </PRE>
     * when we have multiple element tables possible.
     * @return Statement for updating the owner/index of an element in an inverse 1-N
     */
    private String getUpdateFkStmt()
    {
        if (updateFkStmt == null)
        {
            StringBuffer stmt = new StringBuffer("UPDATE ");
            if (elementInfo.length > 1)
            {
                stmt.append("?");
            }
            else
            {
                stmt.append(elementInfo[0].getDatastoreClass().toString());
            }
            stmt.append(" SET ");
            for (int i = 0; i < ownerMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(",");
                }
                stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                stmt.append(" = ");
                stmt.append(((RDBMSMapping) ownerMapping.getDatastoreMapping(i)).getUpdateInputParameter());
            }
            for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
            {
                stmt.append(",");
                stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                stmt.append(" = ");
                stmt.append(((RDBMSMapping) orderMapping.getDatastoreMapping(i)).getUpdateInputParameter());
            }
            if (relationDiscriminatorMapping != null)
            {
                for (int i = 0; i < relationDiscriminatorMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(
                        relationDiscriminatorMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append(" = ");
                    stmt.append(((RDBMSMapping) relationDiscriminatorMapping.getDatastoreMapping(i)).getUpdateInputParameter());
                }
            }

            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, elementMapping, null, true);

            updateFkStmt = stmt.toString();
        }

        return updateFkStmt;
    }

    /**
     * Method to clear the Array.
     * This is called when the container object is being deleted and the elements are to be removed (maybe for dependent field).
     * @param ownerSM The state manager
     */
    public void clear(ObjectProvider ownerSM)
    {
        boolean deleteElements = false;
        if (ownerMemberMetaData.getArray().isDependentElement())
        {
            // Elements are dependent and can't exist on their own, so delete them all
            NucleusLogger.DATASTORE.debug(LOCALISER.msg("056034"));
            deleteElements = true;
        }
        else
        {
            if (ownerMapping.isNullable() && orderMapping.isNullable())
            {
                // Field is not dependent, and nullable so we null the FK
                NucleusLogger.DATASTORE.debug(LOCALISER.msg("056036"));
                deleteElements = false;
            }
            else
            {
                // Field is not dependent, and not nullable so we just delete the elements
                NucleusLogger.DATASTORE.debug(LOCALISER.msg("056035"));
                deleteElements = true;
            }
        }

        if (deleteElements)
        {
            ownerSM.getExecutionContext().getApiAdapter().isLoaded(ownerSM, ownerMemberMetaData.getAbsoluteFieldNumber()); // Make sure the field is loaded
            Object[] value = (Object[]) ownerSM.provideField(ownerMemberMetaData.getAbsoluteFieldNumber());
            if (value != null && value.length > 0)
            {
                ownerSM.getExecutionContext().deleteObjects(value);
            }
        }
        else
        {
            // TODO If the relation is bidirectional we need to clear the owner in the element
            String clearNullifyStmt = getClearNullifyStmt();
            try
            {
                ExecutionContext ec = ownerSM.getExecutionContext();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                SQLController sqlControl = storeMgr.getSQLController();
                try
                {
                    PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, clearNullifyStmt, false);
                    try
                    {
                        int jdbcPosition = 1;
                        jdbcPosition = BackingStoreHelper.populateOwnerInStatement(ownerSM, ec, ps, jdbcPosition, this);
                        if (relationDiscriminatorMapping != null)
                        {
                            BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                        }
                        sqlControl.executeStatementUpdate(ec, mconn, clearNullifyStmt, ps, true);
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
                throw new NucleusDataStoreException(LOCALISER.msg("056013", clearNullifyStmt), e);
            }
        }
    }

    /**
     * Generates the statement for clearing items by nulling the owner link out. The statement will be
     * <PRE>
     * UPDATE ARRAYTABLE SET OWNERCOL=NULL, INDEXCOL=NULL [,DISTINGUISHER=NULL]
     * WHERE OWNERCOL=? [AND DISTINGUISHER=?]
     * </PRE>
     * when there is only one element table, and will be
     * <PRE>
     * UPDATE ? SET OWNERCOL=NULL, INDEXCOL=NULL [,DISTINGUISHER=NULL]
     * WHERE OWNERCOL=? [AND DISTINGUISHER=?]
     * </PRE>
     * when there is more than 1 element table.
     * @return The Statement for clearing items for the owner.
     */
    protected String getClearNullifyStmt()
    {
        if (clearNullifyStmt == null)
        {
            StringBuffer stmt = new StringBuffer("UPDATE ");
            if (elementInfo.length > 1)
            {
                stmt.append("?");
            }
            else
            {
                stmt.append(elementInfo[0].getDatastoreClass().toString());
            }
            stmt.append(" SET ");
            for (int i = 0; i < ownerMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(", ");
                }
                stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString() + " = NULL");
            }
            for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
            {
                stmt.append(", ");
                stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString() + " = NULL");
            }
            if (relationDiscriminatorMapping != null)
            {
                for (int i = 0; i < relationDiscriminatorMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(", ");
                    stmt.append(
                        relationDiscriminatorMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString() + " = NULL");
                }
            }

            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            if (relationDiscriminatorMapping != null)
            {
                BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
            }

            clearNullifyStmt = stmt.toString();
        }
        return clearNullifyStmt;
    }

    /**
     * Method to set the array for the specified owner to the passed value.
     * @param ownerSM State Manager for the owner
     * @param array the array
     * @return Whether the array was updated successfully
     */
    public boolean set(ObjectProvider ownerSM, Object array)
    {
        if (array == null)
        {
            return true;
        }

        // Check that all elements are inserted
        for (int i=0;i<Array.getLength(array);i++)
        {
            validateElementForWriting(ownerSM.getExecutionContext(), Array.get(array, i), null);
        }

        // Update the FK and position of all elements
        int length = Array.getLength(array);
        for (int i=0;i<length;i++)
        {
            Object obj = Array.get(array, i);
            updateElementFk(ownerSM, obj, ownerSM.getObject(), i);
        }

        return true;
    }

    /**
     * Accessor for an iterator for the set.
     * @param ownerSM State Manager for the set.
     * @return Iterator for the set.
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
                        ResultObjectFactory rof = null;
                        if (elementsAreEmbedded || elementsAreSerialised)
                        {
                            throw new NucleusException("Cannot have FK array with non-persistent objects");
                        }
                        else
                        {
                            rof = storeMgr.newResultObjectFactory(emd, iteratorMappingDef, false, null,
                                clr.classForName(elementType));
                        }

                        return new RDBMSArrayStoreIterator(ownerSM, rs, rof, this);
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
     * Method to generate an SQLStatement for iterating through elements of the set.
     * Selects the element table.
     * Populates the iteratorMappingDef and iteratorMappingParams.
     * @param ownerSM StateManager for the owner object
     * @return The SQLStatement
     */
    protected SQLStatement getSQLStatementForIterator(ObjectProvider ownerSM)
    {
        if (elementInfo == null || elementInfo.length == 0)
        {
            return null;
        }

        SQLStatement sqlStmt = null;

        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        final ClassLoaderResolver clr = ownerSM.getExecutionContext().getClassLoaderResolver();
        iteratorMappingDef = new StatementClassMapping();
        if (elementInfo[0].getDatastoreClass().getDiscriminatorMetaData() != null &&
            elementInfo[0].getDatastoreClass().getDiscriminatorMetaData().getStrategy() != DiscriminatorStrategy.NONE)
        {
            String elementType = ownerMemberMetaData.getArray().getElementType();
            if (ClassUtils.isReferenceType(clr.classForName(elementType)))
            {
                String[] clsNames =
                    storeMgr.getNucleusContext().getMetaDataManager().getClassesImplementingInterface(elementType, clr);
                Class[] cls = new Class[clsNames.length];
                for (int i=0; i<clsNames.length; i++)
                {
                    cls[i] = clr.classForName(clsNames[i]);
                }
                sqlStmt = new DiscriminatorStatementGenerator(storeMgr, clr, cls, true, null, null).getStatement();
            }
            else
            {
                sqlStmt = new DiscriminatorStatementGenerator(storeMgr, clr,
                    clr.classForName(elementInfo[0].getClassName()), true, null, null).getStatement();
            }
            iterateUsingDiscriminator = true;

            // Select the required fields
            SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingDef,
                ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
        }
        else
        {
            for (int i=0;i<elementInfo.length;i++)
            {
                final Class elementCls = clr.classForName(this.elementInfo[i].getClassName());
                UnionStatementGenerator stmtGen = new UnionStatementGenerator(storeMgr, clr, elementCls, true, null, null);
                stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
                iteratorMappingDef.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
                SQLStatement subStmt = stmtGen.getStatement();

                // Select the required fields (of the element class)
                if (sqlStmt == null)
                {
                    SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(subStmt, iteratorMappingDef,
                        ownerSM.getExecutionContext().getFetchPlan(), subStmt.getPrimaryTable(), emd, 0);
                }
                else
                {
                    SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(subStmt, null,
                        ownerSM.getExecutionContext().getFetchPlan(), subStmt.getPrimaryTable(), emd, 0);
                }

                if (sqlStmt == null)
                {
                    sqlStmt = subStmt;
                }
                else
                {
                    sqlStmt.union(subStmt);
                }
            }
        }

        // Apply condition to filter by owner
        SQLTable ownerSqlTbl =
            SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), ownerMapping);
        SQLExpression ownerExpr = exprFactory.newExpression(sqlStmt, ownerSqlTbl, ownerMapping);
        SQLExpression ownerVal = exprFactory.newLiteralParameter(sqlStmt, ownerMapping, null, "OWNER");
        sqlStmt.whereAnd(ownerExpr.eq(ownerVal), true);

        if (relationDiscriminatorMapping != null)
        {
            // Apply condition on distinguisher field to filter by distinguisher (when present)
            SQLTable distSqlTbl =
                SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), relationDiscriminatorMapping);
            SQLExpression distExpr = exprFactory.newExpression(sqlStmt, distSqlTbl, relationDiscriminatorMapping);
            SQLExpression distVal = exprFactory.newLiteral(sqlStmt, relationDiscriminatorMapping, relationDiscriminatorValue);
            sqlStmt.whereAnd(distExpr.eq(distVal), true);
        }

        if (orderMapping != null)
        {
            // Order by the ordering column, when present
            SQLTable orderSqlTbl =
                SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
            SQLExpression[] orderExprs = new SQLExpression[orderMapping.getNumberOfDatastoreMappings()];
            boolean descendingOrder[] = new boolean[orderMapping.getNumberOfDatastoreMappings()];
            orderExprs[0] = exprFactory.newExpression(sqlStmt, orderSqlTbl, orderMapping);
            sqlStmt.setOrdering(orderExprs, descendingOrder);
        }

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