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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.query.ResultObjectFactory;
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
import org.datanucleus.store.rdbms.table.ArrayTable;
import org.datanucleus.util.ClassUtils;

/**
 * RDBMS-specific implementation of a Join ArrayStore
 */
public class RDBMSJoinArrayStore extends AbstractArrayStore
{
    /** JDBC statement to use for retrieving keys of the map (locking). */
    private String iteratorStmtLocked = null;

    /** JDBC statement to use for retrieving keys of the map (not locking). */
    private String iteratorStmtUnlocked = null;

    private StatementClassMapping iteratorMappingClass = null;
    private StatementParameterMapping iteratorMappingParams = null;

    /**
     * Constructor for an RDBMS implementation of a join array store.
     * @param mmd Metadata for the owning field/property
     * @param arrayTable The Join table
     * @param clr ClassLoader resolver
     */
    public RDBMSJoinArrayStore(AbstractMemberMetaData mmd, ArrayTable arrayTable, ClassLoaderResolver clr)
    {
        super((RDBMSStoreManager) arrayTable.getStoreManager(), clr);

        this.containerTable = arrayTable;
        setOwner(arrayTable.getOwnerMemberMetaData(), clr);

        this.ownerMapping = arrayTable.getOwnerMapping();
        this.elementMapping = arrayTable.getElementMapping();
        this.orderMapping = arrayTable.getOrderMapping();
        this.relationDiscriminatorMapping = arrayTable.getRelationDiscriminatorMapping();
        this.relationDiscriminatorValue = arrayTable.getRelationDiscriminatorValue();

        this.elementType = arrayTable.getElementType();
        this.elementsAreEmbedded = arrayTable.isEmbeddedElement();
        this.elementsAreSerialised = arrayTable.isSerialisedElement();

        if (this.elementsAreSerialised)
        {
            this.elementInfo = null;
        }
        else
        {
            Class element_class=clr.classForName(elementType);
            if (ClassUtils.isReferenceType(element_class))
            {
                // Array of reference types (interfaces/Objects)
                String[] implNames = MetaDataUtils.getInstance().getImplementationNamesForReferenceField(ownerMemberMetaData,
                    FieldRole.ROLE_ARRAY_ELEMENT, clr, storeMgr.getMetaDataManager());
                elementInfo = new ElementInfo[implNames.length];
                for (int i=0;i<implNames.length;i++)
                {
                    DatastoreClass table = storeMgr.getDatastoreClass(implNames[i], clr);
                    AbstractClassMetaData cmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(implNames[i], clr);
                    elementInfo[i] = new ElementInfo(cmd, table);
                }
            }
            else
            {
                // Collection of PC or non-PC
                emd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(element_class, clr);
                if (emd != null)
                {
                    this.elementType  = emd.getFullClassName();
                    if (!elementsAreEmbedded && !elementsAreSerialised)
                    {
                        elementInfo = getElementInformationForClass();
                        if (elementInfo != null && elementInfo.length > 1)
                        {
                            throw new NucleusUserException(LOCALISER.msg("056045", 
                                ownerMemberMetaData.getFullFieldName()));
                        }
                    }
                    else
                    {
                        elementInfo = null;
                    }
                }
                else
                {
                    elementInfo = null;
                }
            }
        }
    }

    /**
     * Method to return an iterator to the array.
     * @param ownerSM StateManager for the owner of the array
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
                        if (elementsAreEmbedded || elementsAreSerialised)
                        {
                            // No ResultObjectFactory needed - handled by SetStoreIterator
                            return new RDBMSArrayStoreIterator(ownerSM, rs, null, this);
                        }
                        else if (elementMapping instanceof ReferenceMapping)
                        {
                            // No ResultObjectFactory needed - handled by SetStoreIterator
                            return new RDBMSArrayStoreIterator(ownerSM, rs, null, this);
                        }
                        else
                        {
                            ResultObjectFactory rof = storeMgr.newResultObjectFactory(emd, 
                                iteratorMappingClass, false, null, clr.classForName(elementType));
                            return new RDBMSArrayStoreIterator(ownerSM, rs, rof, this);
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
     * Method to generate an SQLStatement for iterating through elements of the set.
     * Selects the join table.
     * Populates the iteratorMappingDef and iteratorMappingParams.
     * @param ownerSM StateManager for the owner object
     * @return The SQLStatement
     */
    protected SQLStatement getSQLStatementForIterator(ObjectProvider ownerSM)
    {
        SQLStatement sqlStmt = null;

        final ClassLoaderResolver clr = ownerSM.getExecutionContext().getClassLoaderResolver();
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        if (elementsAreEmbedded || elementsAreSerialised)
        {
            // Element = embedded, serialised (maybe Non-PC)
            // Just select the join table since we're going to return the embedded/serialised columns from it
            sqlStmt = new SQLStatement(storeMgr, containerTable, null, null);
            sqlStmt.setClassLoaderResolver(clr);

            // Select the element column - first select is assumed by SetStoreIterator
            sqlStmt.select(sqlStmt.getPrimaryTable(), elementMapping, null);
        }
        else if (elementMapping instanceof ReferenceMapping)
        {
            // Element = Reference type (interface/Object)
            // Just select the join table since we're going to return the implementation id columns only
            sqlStmt = new SQLStatement(storeMgr, containerTable, null, null);
            sqlStmt.setClassLoaderResolver(clr);

            // Select the reference column(s) - first select is assumed by SetStoreIterator
            sqlStmt.select(sqlStmt.getPrimaryTable(), elementMapping, null);
        }
        else
        {
            // Element = PC
            // Join to the element table(s)
            iteratorMappingClass = new StatementClassMapping();
            for (int i = 0; i < elementInfo.length; i++)
            {
                // TODO This will only work if all element types have a discriminator
                final int elementNo = i;
                final Class elementCls = clr.classForName(elementInfo[elementNo].getClassName());
                SQLStatement elementStmt = null;
                if (elementInfo[elementNo].getDiscriminatorStrategy() != null &&
                    elementInfo[elementNo].getDiscriminatorStrategy() != DiscriminatorStrategy.NONE)
                {
                    // The element uses a discriminator so just use that in the SELECT
                    String elementType = ownerMemberMetaData.getCollection().getElementType();
                    if (ClassUtils.isReferenceType(clr.classForName(elementType)))
                    {
                        String[] clsNames = storeMgr.getNucleusContext().getMetaDataManager().getClassesImplementingInterface(
                            elementType, clr);
                        Class[] cls = new Class[clsNames.length];
                        for (int j = 0; j < clsNames.length; j++)
                        {
                            cls[j] = clr.classForName(clsNames[j]);
                        }

                        StatementGenerator stmtGen = new DiscriminatorStatementGenerator(storeMgr, clr, cls, 
                            true, null, null, containerTable, null, elementMapping);
                        if (allowNulls)
                        {
                            stmtGen.setOption(StatementGenerator.OPTION_ALLOW_NULLS);
                        }
                        elementStmt = stmtGen.getStatement();
                    }
                    else
                    {
                        StatementGenerator stmtGen = new DiscriminatorStatementGenerator(storeMgr, clr, elementCls,
                            true, null, null, containerTable, null, elementMapping);
                        if (allowNulls)
                        {
                            stmtGen.setOption(StatementGenerator.OPTION_ALLOW_NULLS);
                        }
                        elementStmt = stmtGen.getStatement();
                    }
                    iterateUsingDiscriminator = true;
                }
                else
                {
                    // No discriminator, but subclasses so use UNIONs
                    StatementGenerator stmtGen = new UnionStatementGenerator(storeMgr, clr, elementCls, true, null,
                        null, containerTable, null, elementMapping);
                    stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
                    if (allowNulls) 
                    {
                        stmtGen.setOption(StatementGenerator.OPTION_ALLOW_NULLS);
                    }
                    iteratorMappingClass.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
                    elementStmt = stmtGen.getStatement();
                }

                if (sqlStmt == null)
                {
                    sqlStmt = elementStmt;
                }
                else
                {
                    sqlStmt.union(elementStmt);
                }
            }

            // Select the required fields
            SQLTable elementSqlTbl = sqlStmt.getTable(elementInfo[0].getDatastoreClass(),
                sqlStmt.getPrimaryTable().getGroupName());
            SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, iteratorMappingClass,
                ownerSM.getExecutionContext().getFetchPlan(), elementSqlTbl, emd, 0);
        }

        // Apply condition on join-table owner field to filter by owner
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
                for (int k=0;k<paramPositions.length;k++)
                {
                    paramPositions[k] = inputParamNum++;
                }
                ownerIdx.addParameterOccurrence(paramPositions);
            }
        }
        else
        {
            int[] paramPositions = new int[ownerMapping.getNumberOfDatastoreMappings()];
            for (int k=0;k<paramPositions.length;k++)
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