/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.request;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.identity.SingleFieldIdentity;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.identity.OID;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.state.AbstractStateManager;
import org.datanucleus.state.lock.LockManager;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.PersistableMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.expression.BooleanExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Request to locate a series of records in the data store (all present in the same table). 
 * Performs an SQL statement like
 * <pre>
 * SELECT ID [,FIELD1,FIELD2] FROM CANDIDATE_TABLE WHERE ID = ? OR ID = ? OR ID = ?
 * </pre>
 */
public class LocateBulkRequest extends BulkRequest
{
    AbstractClassMetaData cmd = null;

    /** Definition of input mappings in the SQL statement. */
    private StatementClassMapping[] mappingDefinitions;

    /** Result mapping for the SQL statement. */
    private StatementClassMapping resultMapping;

    /**
     * Constructor, taking the table. Uses the structure of the datastore table to build a basic query.
     * @param table The Class Table representing the datastore table to retrieve
     */
    public LocateBulkRequest(DatastoreClass table)
    {
        super(table);
    }

    protected String getStatement(DatastoreClass table, ObjectProvider[] ops, boolean lock)
    {
        RDBMSStoreManager storeMgr = (RDBMSStoreManager)table.getStoreManager();
        ClassLoaderResolver clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        cmd = storeMgr.getMetaDataManager().getMetaDataForClass(table.getType(), clr);

        SQLStatement sqlStatement = new SQLStatement(storeMgr, table, null, null);

        // SELECT fields we require
        resultMapping = new StatementClassMapping();

        // a). PK fields
        if (table.getIdentityType() == IdentityType.DATASTORE)
        {
            JavaTypeMapping datastoreIdMapping = table.getDatastoreObjectIdMapping();
            SQLExpression expr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(), 
                datastoreIdMapping);
            int[] cols = sqlStatement.select(expr, null);
            StatementMappingIndex datastoreIdx = new StatementMappingIndex(datastoreIdMapping);
            datastoreIdx.setColumnPositions(cols);
            resultMapping.addMappingForMember(StatementClassMapping.MEMBER_DATASTORE_ID, datastoreIdx);
        }
        else if (table.getIdentityType() == IdentityType.APPLICATION)
        {
            int[] pkNums = cmd.getPKMemberPositions();
            for (int i=0;i<pkNums.length;i++)
            {
                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkNums[i]);
                JavaTypeMapping pkMapping = table.getMemberMappingInDatastoreClass(mmd);
                if (pkMapping == null)
                {
                    pkMapping = table.getMemberMapping(mmd);
                }
                SQLExpression expr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(),
                    pkMapping);
                int[] cols = sqlStatement.select(expr, null);
                StatementMappingIndex pkIdx = new StatementMappingIndex(pkMapping);
                pkIdx.setColumnPositions(cols);
                resultMapping.addMappingForMember(mmd.getAbsoluteFieldNumber(), pkIdx);
            }
        }
        else
        {
            throw new NucleusUserException("Cannot locate objects using nondurable identity");
        }

        JavaTypeMapping verMapping = table.getVersionMapping(false);
        if (verMapping != null)
        {
            VersionMetaData currentVermd = table.getVersionMetaData();
            if (currentVermd != null && currentVermd.getFieldName() == null)
            {
                // Surrogate version column
                SQLExpression expr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(), verMapping);
                int[] cols = sqlStatement.select(expr, null);
                StatementMappingIndex mapIdx = new StatementMappingIndex(verMapping);
                mapIdx.setColumnPositions(cols);
                resultMapping.addMappingForMember(StatementClassMapping.MEMBER_VERSION, mapIdx);
            }
        }

        int[] nonPkFieldNums = cmd.getNonPKMemberPositions();
        if (nonPkFieldNums != null)
        {
            for (int i=0;i<nonPkFieldNums.length;i++)
            {
                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(nonPkFieldNums[i]);
                JavaTypeMapping mapping = table.getMemberMapping(mmd);
                if (mapping != null && mapping.includeInFetchStatement())
                {
                    if (mapping instanceof PersistableMapping)
                    {
                        // Ignore 1-1/N-1 for now
                        continue;
                    }

                    SQLExpression expr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(), mapping);
                    int[] cols = sqlStatement.select(expr, null);
                    StatementMappingIndex mapIdx = new StatementMappingIndex(mapping);
                    mapIdx.setColumnPositions(cols);
                    resultMapping.addMappingForMember(mmd.getAbsoluteFieldNumber(), mapIdx);
                }
            }
        }

        // Add WHERE clause restricting to tenant (if any)
        if (table.getMultitenancyMapping() != null)
        {
            // Add restriction on multi-tenancy
            JavaTypeMapping tenantMapping = table.getMultitenancyMapping();
            SQLExpression tenantExpr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(), 
                tenantMapping);
            SQLExpression tenantVal = exprFactory.newLiteral(sqlStatement, tenantMapping,
                storeMgr.getStringProperty(PropertyNames.PROPERTY_TENANT_ID));
            sqlStatement.whereAnd(tenantExpr.eq(tenantVal), true);
        }

        // Add WHERE clause restricting to the identities of the objects
        mappingDefinitions = new StatementClassMapping[ops.length];
        int inputParamNum = 1;
        for (int i=0;i<ops.length;i++)
        {
            mappingDefinitions[i] = new StatementClassMapping();
            if (table.getIdentityType() == IdentityType.DATASTORE)
            {
                // Datastore identity value for input
                JavaTypeMapping datastoreIdMapping = table.getDatastoreObjectIdMapping();
                SQLExpression expr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(), 
                    datastoreIdMapping);
                SQLExpression val = exprFactory.newLiteralParameter(sqlStatement, datastoreIdMapping, null, "ID");
                sqlStatement.whereOr(expr.eq(val), true);

                StatementMappingIndex datastoreIdx = new StatementMappingIndex(datastoreIdMapping);
                mappingDefinitions[i].addMappingForMember(StatementClassMapping.MEMBER_DATASTORE_ID, datastoreIdx);
                datastoreIdx.addParameterOccurrence(new int[] {inputParamNum++});
            }
            else if (table.getIdentityType() == IdentityType.APPLICATION)
            {
                // Application identity value(s) for input
                BooleanExpression pkExpr = null;
                int[] pkNums = cmd.getPKMemberPositions();
                for (int j=0;j<pkNums.length;j++)
                {
                    AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkNums[j]);
                    JavaTypeMapping pkMapping = table.getMemberMappingInDatastoreClass(mmd);
                    if (pkMapping == null)
                    {
                        pkMapping = table.getMemberMapping(mmd);
                    }
                    SQLExpression expr = exprFactory.newExpression(sqlStatement, sqlStatement.getPrimaryTable(),
                        pkMapping);
                    SQLExpression val = exprFactory.newLiteralParameter(sqlStatement, pkMapping, null, "PK" + j);
                    BooleanExpression fieldEqExpr = expr.eq(val);
                    if (pkExpr == null)
                    {
                        pkExpr = fieldEqExpr;
                    }
                    else
                    {
                        pkExpr = pkExpr.and(fieldEqExpr);
                    }

                    StatementMappingIndex pkIdx = new StatementMappingIndex(pkMapping);
                    mappingDefinitions[i].addMappingForMember(mmd.getAbsoluteFieldNumber(), pkIdx);
                    int[] inputParams = new int[pkMapping.getNumberOfDatastoreMappings()];
                    for (int k=0;k<pkMapping.getNumberOfDatastoreMappings();k++)
                    {
                        inputParams[k] = inputParamNum++;
                    }
                    pkIdx.addParameterOccurrence(inputParams);
                }
                pkExpr = (BooleanExpression)pkExpr.encloseInParentheses();
                sqlStatement.whereOr(pkExpr, true);
            }
        }

        // Generate the appropriate JDBC statement allowing for locking
        if (lock)
        {
            sqlStatement.addExtension("lock-for-update", Boolean.TRUE);
            return sqlStatement.getSelectStatement().toSQL();
        }
        else
        {
            return sqlStatement.getSelectStatement().toSQL();
        }
    }

    /**
     * Method performing the location of the records in the datastore. 
     * @param ops ObjectProviders to be located
     * @throws NucleusObjectNotFoundException with nested exceptions for each of missing objects (if any)
     */
    public void execute(ObjectProvider[] ops)
    {
        if (ops == null || ops.length == 0)
        {
            return;
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            // Debug information about what we are retrieving
            StringBuffer str = new StringBuffer();
            for (int i=0;i<ops.length;i++)
            {
                if (i > 0)
                {
                    str.append(", ");
                }
                str.append(ops[i].getInternalObjectId());
            }
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("052223", str.toString(), table));
        }

        ExecutionContext ec = ops[0].getExecutionContext();
        RDBMSStoreManager storeMgr = (RDBMSStoreManager)table.getStoreManager();
        AbstractClassMetaData cmd = ops[0].getClassMetaData();
        boolean locked = ec.getSerializeReadForClass(cmd.getFullClassName());
        short lockType = ec.getLockManager().getLockMode(ops[0].getObjectId());
        if (lockType != LockManager.LOCK_MODE_NONE)
        {
            if (lockType == LockManager.LOCK_MODE_PESSIMISTIC_READ ||
                lockType == LockManager.LOCK_MODE_PESSIMISTIC_WRITE)
            {
                // Override with pessimistic lock
                locked = true;
            }
        }
        String statement = getStatement(table, ops, locked);

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, statement);

                try
                {
                    // Provide the primary key field(s)
                    for (int i=0;i<ops.length;i++)
                    {
                        if (cmd.getIdentityType() == IdentityType.DATASTORE)
                        {
                            StatementMappingIndex datastoreIdx = mappingDefinitions[i].getMappingForMemberPosition(
                                StatementClassMapping.MEMBER_DATASTORE_ID);
                            for (int j=0;j<datastoreIdx.getNumberOfParameterOccurrences();j++)
                            {
                                table.getDatastoreObjectIdMapping().setObject(ec, ps,
                                    datastoreIdx.getParameterPositionsForOccurrence(j), ops[i].getInternalObjectId());
                            }
                        }
                        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
                        {
                            ops[i].provideFields(cmd.getPKMemberPositions(),
                                storeMgr.getFieldManagerForStatementGeneration(ops[i], ps, mappingDefinitions[i], false));
                        }
                    }

                    // Execute the statement
                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, statement, ps);
                    try
                    {
                        ObjectProvider[] missingOps = processResults(rs, ops);
                        if (missingOps != null && missingOps.length > 0)
                        {
                            NucleusObjectNotFoundException[] nfes = new NucleusObjectNotFoundException[missingOps.length];
                            for (int i=0;i<nfes.length;i++)
                            {
                                nfes[i] = new NucleusObjectNotFoundException("Object not found", missingOps[i].getInternalObjectId());
                            }
                            throw new NucleusObjectNotFoundException("Some objects were not found. Look at nested exceptions for details", nfes);
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
        catch (SQLException sqle)
        {
            String msg = LOCALISER.msg("052220", ops[0].toPrintableID(), statement, sqle.getMessage());
            NucleusLogger.DATASTORE_RETRIEVE.warn(msg);
            List exceptions = new ArrayList();
            exceptions.add(sqle);
            while ((sqle = sqle.getNextException()) != null)
            {
                exceptions.add(sqle);
            }
            throw new NucleusDataStoreException(msg, 
                (Throwable[])exceptions.toArray(new Throwable[exceptions.size()]));
        }
    }

    private ObjectProvider[] processResults(ResultSet rs, ObjectProvider[] ops)
    throws SQLException
    {
        List<ObjectProvider> missingOps = new ArrayList<ObjectProvider>();
        for (int i=0;i<ops.length;i++)
        {
            missingOps.add(ops[i]);
        }

        ExecutionContext ec = ops[0].getExecutionContext();
        while (rs.next())
        {
            FieldManager resultFM = table.getStoreManager().getFieldManagerForResultProcessing(ec, rs, resultMapping, cmd);
            Object id = null;
            Object key = null;
            if (cmd.getIdentityType() == IdentityType.DATASTORE)
            {
                StatementMappingIndex idx = resultMapping.getMappingForMemberPosition(StatementClassMapping.MEMBER_DATASTORE_ID);
                JavaTypeMapping idMapping = idx.getMapping();
                key = idMapping.getObject(ec, rs, idx.getColumnPositions());
                if (key instanceof OID)
                {
                    // If mapping is OIDMapping then returns an OID rather than the column value
                    key = ((OID)key).getKeyValue();
                }
            }
            else if (cmd.getIdentityType() == IdentityType.APPLICATION)
            {
                if (cmd.usesSingleFieldIdentityClass())
                {
                    int[] pkFieldNums = cmd.getPKMemberPositions();
                    AbstractMemberMetaData pkMmd =
                        cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[0]);
                    if (pkMmd.getType() == int.class)
                    {
                        key = resultFM.fetchIntField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == short.class)
                    {
                        key = resultFM.fetchShortField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == long.class)
                    {
                        key = resultFM.fetchLongField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == char.class)
                    {
                        key = resultFM.fetchCharField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == boolean.class)
                    {
                        key = resultFM.fetchBooleanField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == byte.class)
                    {
                        key = resultFM.fetchByteField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == double.class)
                    {
                        key = resultFM.fetchDoubleField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == float.class)
                    {
                        key = resultFM.fetchFloatField(pkFieldNums[0]);
                    }
                    else if (pkMmd.getType() == String.class)
                    {
                        key = resultFM.fetchStringField(pkFieldNums[0]);
                    }
                    else
                    {
                        key = resultFM.fetchObjectField(pkFieldNums[0]);
                    }
                }
                else
                {
                    id = IdentityUtils.getApplicationIdentityForResultSetRow(ec, cmd, null, true, resultFM);
                }
            }

            // Find which ObjectProvider this row is for
            ObjectProvider op = null;
            for (ObjectProvider missingOp : missingOps)
            {
                Object opId = missingOp.getInternalObjectId();
                if (cmd.getIdentityType() == IdentityType.DATASTORE)
                {
                    Object opKey = ((OID)opId).getKeyValue();
                    if (opKey.getClass() != key.getClass())
                    {
                        opKey = TypeConversionHelper.convertTo(opKey, key.getClass());
                    }
                    if (opKey.equals(key))
                    {
                        op = missingOp;
                        break;
                    }
                }
                else if (cmd.getIdentityType() == IdentityType.APPLICATION)
                {
                    if (cmd.usesSingleFieldIdentityClass())
                    {
                        Object opKey = ((SingleFieldIdentity)opId).getKeyAsObject();
                        if (opKey.equals(key))
                        {
                            op = missingOp;
                            break;
                        }
                    }
                    else
                    {
                        if (opId.equals(id))
                        {
                            op = missingOp;
                            break;
                        }
                    }
                }
            }
            if (op != null)
            {
                // Mark ObjectProvider as processed
                missingOps.remove(op);

                // Load up any unloaded fields that we have selected
                int[] selectedMemberNums = resultMapping.getMemberNumbers();
                int[] unloadedMemberNums = AbstractStateManager.getFlagsSetTo(op.getLoadedFields(), selectedMemberNums, false);
                if (unloadedMemberNums != null && unloadedMemberNums.length > 0)
                {
                    op.replaceFields(unloadedMemberNums, resultFM);
                }

                // Load version if present and not yet set
                if (op.getTransactionalVersion() == null && table.getVersionMapping(false) != null)
                {
                    VersionMetaData currentVermd = table.getVersionMetaData();
                    Object datastoreVersion = null;
                    if (currentVermd != null && currentVermd.getFieldName() == null)
                    {
                        // Surrogate version
                        StatementMappingIndex verIdx =
                            resultMapping.getMappingForMemberPosition(StatementClassMapping.MEMBER_VERSION);
                        datastoreVersion = table.getVersionMapping(true).getObject(ec, rs,
                            verIdx.getColumnPositions());
                    }
                    else
                    {
                        datastoreVersion = op.provideField(cmd.getAbsolutePositionOfMember(currentVermd.getFieldName()));
                    }
                    op.setVersion(datastoreVersion);
                }
            }
        }

        if (!missingOps.isEmpty())
        {
            return missingOps.toArray(new ObjectProvider[missingOps.size()]);
        }
        return null;
    }
}