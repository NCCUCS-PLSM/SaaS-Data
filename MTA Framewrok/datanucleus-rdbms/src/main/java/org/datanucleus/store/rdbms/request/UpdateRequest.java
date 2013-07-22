/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved. 
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
2003 Erik Bengtson  - optimistic transaction support
2004 Andy Jefferson - coding standards
2004 Andy Jefferson - conversion to use Logger
2005 Andy Jefferson - added handling for updating FK in related object
2006 Andy Jefferson - changed to extend VersionCheckRequest
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.request;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.VersionHelper;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.exceptions.NotYetFlushedException;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingCallbacks;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.art.utb.UTbServiceHelper;
import org.datanucleus.store.rdbms.fieldmanager.OldValueParameterSetter;
import org.datanucleus.store.rdbms.mapping.RDBMSMapping;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Class to provide a means of update of records in a data store.
 * Extends basic request class implementing the execute method to do a JDBC update operation.
 */
public class UpdateRequest extends Request
{
    /** SQL statement for the update. */
    private final String updateStmt;

    /** SQL statement for the update when using optimistic txns. */
    private final String updateStmtOptimistic;

    /** callback mappings will have their postUpdate method called after the update */
    private final MappingCallbacks[] callbacks;
    
    /** the index for the expression in the update sql statement. */
    private StatementMappingDefinition stmtMappingDefinition;

    /** Numbers of all fields to be updated (except PK). */
    private final int[] updateFieldNumbers;

    /** Numbers of WHERE clause fields. */
    private final int[] whereFieldNumbers;

    /** MetaData for the class. */
    protected AbstractClassMetaData cmd = null;

    /** MetaData for the version handling. */
    protected VersionMetaData versionMetaData = null;

    /** Whether we should make checks on optimistic version before updating. */
    protected boolean versionChecks = false;
    
    //todo arthur add
    private StringBuffer where;
    private  StringBuffer columnAssignments;

    /**
     * Constructor, taking the table. Uses the structure of the datastore
     * table to build a basic query. The query will be of the form
     * <PRE>
     * UPDATE table-name SET param1=?,param2=? WHERE id1=? AND id2=?
     *</PRE>
     *or (when also performing version checks)
     * <PRE>
     * UPDATE table-name SET param1=?,param2=?,version={newvers} WHERE id1=? AND id2=? AND version={oldvers}
     *</PRE>
     *
     * @param table The Class Table representing the datastore table to update
     * @param reqFieldMetaData MetaData of the fields to update
     * @param cmd ClassMetaData of objects being updated
     * @param clr ClassLoader resolver
     */
    public UpdateRequest(DatastoreClass table, AbstractMemberMetaData[] reqFieldMetaData, AbstractClassMetaData cmd, ClassLoaderResolver clr)
    {
        super(table);

        this.cmd = cmd;
        versionMetaData = table.getVersionMetaData();
        if (versionMetaData != null && versionMetaData.getVersionStrategy() != VersionStrategy.NONE)
        {
            // Only apply a version check if we have a strategy defined
            versionChecks = true;
        }

        // Set up the basic mapping information
        stmtMappingDefinition = new StatementMappingDefinition(); // Populated using the subsequent lines
        UpdateMappingConsumer consumer = new UpdateMappingConsumer(cmd);

        // Fields to update
        if (versionMetaData != null)
        {
            if (versionMetaData.getFieldName() != null)
            {
                // Version field
                // TODO If the passed fields arent included in the statement (e.g SCO collection) update version?
                AbstractMemberMetaData[] updateFmds = new AbstractMemberMetaData[reqFieldMetaData.length + 1];
                for (int i=0;i<reqFieldMetaData.length;i++)
                {
                    updateFmds[i] = reqFieldMetaData[i];
                }
                updateFmds[updateFmds.length-1] = cmd.getMetaDataForMember(versionMetaData.getFieldName());
                table.provideMappingsForMembers(consumer, updateFmds, false);
            }
            else
            {
                // Surrogate version column
                table.provideMappingsForMembers(consumer, reqFieldMetaData, false);
                table.provideVersionMappings(consumer);
            }
        }
        else
        {
            // No version field
            table.provideMappingsForMembers(consumer, reqFieldMetaData, false);
        }

        // WHERE clause - add identity
        consumer.setWhereClauseConsumption(true);
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            table.providePrimaryKeyMappings(consumer);
        }
        else if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            table.provideDatastoreIdMappings(consumer);
        }
        else
        {
            AbstractMemberMetaData[] mmds = cmd.getManagedMembers();
            table.provideMappingsForMembers(consumer, mmds, false);
        }

        updateStmt = consumer.getStatement();

        // Add on the optimistic discriminator (if appropriate) to get the update statement for optimistic txns
        if (versionMetaData != null)
        {
            if (versionMetaData.getFieldName() != null)
            {
                // Version field
                // TODO If the passed fields arent included in the statement (e.g SCO collection) update version?
                AbstractMemberMetaData[] updateFmds = new AbstractMemberMetaData[1];
                updateFmds[0] = cmd.getMetaDataForMember(versionMetaData.getFieldName());
                table.provideMappingsForMembers(consumer, updateFmds, false);
            }
            else
            {
                // Surrogate version column
                table.provideVersionMappings(consumer);
            }
        }
        updateStmtOptimistic = consumer.getStatement();

        callbacks = (MappingCallbacks[])consumer.getMappingCallbacks().toArray(new MappingCallbacks[consumer.getMappingCallbacks().size()]);
        whereFieldNumbers = consumer.getWhereFieldNumbers();
        updateFieldNumbers = consumer.getUpdateFieldNumbers();
        
        //todo arthur add
        this.columnAssignments = consumer.columnAssignments;
        this.where = consumer.where;
    }

    /**
     * Method performing the update of the record in the datastore. 
     * Takes the constructed update query and populates with the specific record information.
     * @param sm The state manager for the record to be updated
     */
    public void execute(ObjectProvider sm)
    {
        // Choose the statement based on whether optimistic or not
        String stmt = null;
        ExecutionContext ec = sm.getExecutionContext();
        boolean optimisticChecks =
            (versionMetaData != null && ec.getTransaction().getOptimistic() && versionChecks);
        if (optimisticChecks)
        {
            stmt = updateStmtOptimistic;
        }
        else
        {
            stmt = updateStmt;
        }

        if (stmt != null)
        {
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                // Debug info about fields being updated
                StringBuffer fieldStr = new StringBuffer();
                for (int i=0;i<updateFieldNumbers.length;i++)
                {
                    if (fieldStr.length() > 0)
                    {
                        fieldStr.append(",");
                    }
                    fieldStr.append(cmd.getMetaDataForManagedMemberAtAbsolutePosition(updateFieldNumbers[i]).getName());
                }

                if (versionMetaData != null && versionMetaData.getFieldName() == null)
                {
                    if (fieldStr.length() > 0)
                    {
                        fieldStr.append(",");
                    }
                    fieldStr.append("[VERSION]");
                }

                // Debug information about what we are updating
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("052214", 
                    sm.toPrintableID(), fieldStr.toString(), table));
            }

            RDBMSStoreManager storeMgr = (RDBMSStoreManager)table.getStoreManager();

            //todo arthur add
            Object sourceObj = sm.getObject();
            if(UTbServiceHelper.isMTAAnnoated(sourceObj.getClass().getAnnotations())){
            	UTbServiceHelper.update(sourceObj , this.columnAssignments , this.where);
            	//end here
            }else{
            	//datanucleus code
            	
                boolean batch = false;
                // TODO Set the batch flag based on whether we have no other SQL being invoked in here just our UPDATE
                
                try
                {
                    ManagedConnection mconn = storeMgr.getConnection(ec);
                    SQLController sqlControl = storeMgr.getSQLController();

                    try
                    {
                        // Perform the update
                        PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, stmt, batch);
                        try
                        {
                            Object currentVersion = sm.getTransactionalVersion();
                            Object nextVersion = null;
                            if (versionMetaData != null)
                            {
                                // Set the next version in the object
                                if (versionMetaData.getFieldName() != null)
                                {
                                    // Version field
                                    AbstractMemberMetaData verfmd = cmd.getMetaDataForMember(table.getVersionMetaData().getFieldName());
                                    if (currentVersion instanceof Number)
                                    {
                                        // Cater for Integer-based versions
                                        currentVersion = Long.valueOf(((Number)currentVersion).longValue());
                                    }
                                    nextVersion = VersionHelper.getNextVersion(versionMetaData.getVersionStrategy(), currentVersion);
                                    if (verfmd.getType() == Integer.class || verfmd.getType() == int.class)
                                    {
                                        // Cater for Integer-based versions TODO Generalise this
                                        nextVersion = Integer.valueOf(((Long)nextVersion).intValue());
                                    }
                                    sm.replaceField(verfmd.getAbsoluteFieldNumber(), nextVersion);
                                }
                                else
                                {
                                    // Surrogate version column
                                    nextVersion = VersionHelper.getNextVersion(versionMetaData.getVersionStrategy(), currentVersion);
                                }
                                sm.setTransactionalVersion(nextVersion);
                            }

                            // SELECT clause - set the required fields to be updated
                            if (updateFieldNumbers != null)
                            {
                                StatementClassMapping mappingDefinition = new StatementClassMapping();
                                StatementMappingIndex[] idxs = stmtMappingDefinition.getUpdateFields();
                                for (int i=0;i<idxs.length;i++)
                                {
                                    if (idxs[i] != null)
                                    {
                                        mappingDefinition.addMappingForMember(i, idxs[i]);
                                    }
                                }
                                sm.provideFields(updateFieldNumbers,
                                    storeMgr.getFieldManagerForStatementGeneration(sm, ps, mappingDefinition, true));
                            }

                            if (versionMetaData != null && versionMetaData.getFieldName() == null)
                            {
                                // SELECT clause - set the surrogate version column to the new version
                                StatementMappingIndex mapIdx = stmtMappingDefinition.getUpdateVersion();
                                for (int i=0;i<mapIdx.getNumberOfParameterOccurrences();i++)
                                {
                                    table.getVersionMapping(false).setObject(ec, ps,
                                        mapIdx.getParameterPositionsForOccurrence(i), nextVersion);
                                }
                            }

                            // WHERE clause - primary key fields
                            if (table.getIdentityType() == IdentityType.DATASTORE)
                            {
                                // a). datastore identity
                                StatementMappingIndex mapIdx = stmtMappingDefinition.getWhereDatastoreId();
                                for (int i=0;i<mapIdx.getNumberOfParameterOccurrences();i++)
                                {
                                    table.getDatastoreObjectIdMapping().setObject(ec, ps,
                                        mapIdx.getParameterPositionsForOccurrence(i), sm.getInternalObjectId());
                                }
                            }
                            else
                            {
                                // b). application/nondurable identity
                                StatementClassMapping mappingDefinition = new StatementClassMapping();
                                StatementMappingIndex[] idxs = stmtMappingDefinition.getWhereFields();
                                for (int i=0;i<idxs.length;i++)
                                {
                                    if (idxs[i] != null)
                                    {
                                        mappingDefinition.addMappingForMember(i, idxs[i]);
                                    }
                                }

                                FieldManager fm = null;
                                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                                {
                                    fm = new OldValueParameterSetter(sm, ps, mappingDefinition, true);
                                }
                                else
                                {
                                    fm = storeMgr.getFieldManagerForStatementGeneration(sm, ps, mappingDefinition, true);
                                }
                                sm.provideFields(whereFieldNumbers, fm);
                            }

                            if (optimisticChecks)
                            {
                                if (currentVersion == null)
                                {
                                    // Somehow the version is not set on this object (not read in ?) so report the bug
                                    String msg = LOCALISER.msg("052201", sm.getInternalObjectId(), table);
                                    NucleusLogger.PERSISTENCE.error(msg);
                                    throw new NucleusException(msg);
                                }
                                // WHERE clause - current version discriminator
                                StatementMappingIndex mapIdx = stmtMappingDefinition.getWhereVersion();
                                for (int i=0;i<mapIdx.getNumberOfParameterOccurrences();i++)
                                {
                                    mapIdx.getMapping().setObject(ec, ps,
                                        mapIdx.getParameterPositionsForOccurrence(i), currentVersion);
                                }
                            }

                            int[] rcs = sqlControl.executeStatementUpdate(ec, mconn, stmt, ps, !batch);
                            if (rcs[0] == 0 && optimisticChecks)
                            {
                                // No object updated so either object disappeared or failed optimistic version checks
                                // TODO Batching : when we use batching here we need to process these somehow
                                String msg = LOCALISER.msg("052203", sm.toPrintableID(), sm.getInternalObjectId(), 
                                    "" + currentVersion);
                                NucleusLogger.PERSISTENCE.error(msg);
                                throw new NucleusOptimisticException(msg, sm.getObject());
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
                    String msg = LOCALISER.msg("052215", sm.toPrintableID(), stmt, StringUtils.getStringFromStackTrace(e));
                    NucleusLogger.DATASTORE_PERSIST.error(msg);
                    List exceptions = new ArrayList();
                    exceptions.add(e);
                    while((e = e.getNextException())!=null)
                    {
                        exceptions.add(e);
                    }
                    throw new NucleusDataStoreException(msg, (Throwable[])exceptions.toArray(new Throwable[exceptions.size()]));
                }
                  
            	
            }

        }
      
        // Execute any mapping actions now that we have done the update
        for (int i=0; i<callbacks.length; ++i)
        {
            try
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("052216", sm.toPrintableID(),
                        ((JavaTypeMapping)callbacks[i]).getMemberMetaData().getFullFieldName()));
                }
                callbacks[i].postUpdate(sm);
            }
            catch (NotYetFlushedException e)
            {
                sm.updateFieldAfterInsert(e.getPersistable(),
                    ((JavaTypeMapping)callbacks[i]).getMemberMetaData().getAbsoluteFieldNumber());
            }
        }
    }

    /**
     * Mapping Consumer used for generating the UPDATE statement for an object in a table.
     * This statement will be of the form
     * <PRE>
     * UPDATE table-name SET param1=?,param2=? WHERE id1=? AND id2=?
     * </PRE>
     * or (when also performing version checks)
     * <PRE>
     * UPDATE table-name SET param1=?,param2=?,version={newvers} WHERE id1=? AND id2=? AND version={oldvers}
     * </PRE>
     */
    private class UpdateMappingConsumer implements MappingConsumer
    {
        /** Flag for initialisation state of the consumer. */
        boolean initialized = false;

        /** Current parameter index. */
        int paramIndex = 1;

        /** Numbers of all fields to be updated. */
        List updateFields = new ArrayList();

        /** Numbers of all WHERE clause fields. */
        List whereFields = new ArrayList();

        List mc = new ArrayList();

        /** for UPDATE statement **/
        StringBuffer columnAssignments = new StringBuffer();

        Map assignedColumns = new HashMap();

        /** Where clause for the statement. Built during the consumption process. */
        StringBuffer where = new StringBuffer();

        /** MetaData for the class of the object */
        private final AbstractClassMetaData cmd;

        private boolean whereClauseConsumption = false;

        /**
         * Constructor
         * @param clr the ClassLoaderResolver
         */
        public UpdateMappingConsumer(AbstractClassMetaData cmd)
        {
            super();
            this.cmd = cmd;
        }

        public void setWhereClauseConsumption(boolean whereClause)
        {
            this.whereClauseConsumption = whereClause;
        }

        public void preConsumeMapping(int highest)
        {
            if (!initialized)
            {
                stmtMappingDefinition.setWhereFields(new StatementMappingIndex[highest]);
                stmtMappingDefinition.setUpdateFields(new StatementMappingIndex[highest]);
                initialized = true;
            }
        }

        /**
         * Consumes a mapping for a field.
         * @param m The mapping.
         * @param mmd MetaData for the field
         */
        public void consumeMapping(JavaTypeMapping m, AbstractMemberMetaData fmd)
        {
            if (!fmd.getAbstractClassMetaData().isSameOrAncestorOf(cmd))
            {
                return;
            }
            if (m.includeInUpdateStatement())
            {
                // Check if the field is "updatable" (either using JPA column, or JDO extension)
                if (fmd.hasExtension("updateable") && fmd.getValueForExtension("updateable").equalsIgnoreCase("false"))
                {
                    return;
                }
                ColumnMetaData[] colmds = fmd.getColumnMetaData();
                if (colmds != null && colmds.length > 0)
                {
                    for (int i=0;i<colmds.length;i++)
                    {
                        if (!colmds[i].getUpdateable())
                        {
                            // Not to be updated
                            return;
                        }
                    }
                }

                Integer abs_field_num = Integer.valueOf(fmd.getAbsoluteFieldNumber());
                int parametersIndex[] = new int[m.getNumberOfDatastoreMappings()];
                StatementMappingIndex sei = new StatementMappingIndex(m);
                sei.addParameterOccurrence(parametersIndex);

                if (whereClauseConsumption)
                {
                    // Where fields
                    VersionMetaData vermd = cmd.getVersionMetaDataForTable();
                    if (vermd != null && vermd.getFieldName() != null && fmd.getName().equals(vermd.getFieldName()))
                    {
                        // Version field to be put in WHERE clause (only non-PK field that will come here)
                        stmtMappingDefinition.setWhereVersion(sei);
                        parametersIndex[0] = paramIndex++;
                        String inputParam = ((RDBMSMapping)m.getDatastoreMapping(0)).getUpdateInputParameter();
                        String condition = " AND " + m.getDatastoreMapping(0).getDatastoreField().getIdentifier() + "=" + inputParam;
                        where.append(condition);
                    }
                    else
                    {
                        stmtMappingDefinition.getWhereFields()[fmd.getAbsoluteFieldNumber()] = sei; 
                        for (int j=0; j<parametersIndex.length; j++)
                        {
                            if (where.length() > 0)
                            {
                                where.append(" AND ");
                            }
                            String condition = m.getDatastoreMapping(j).getDatastoreField().getIdentifier() + 
                                "=" + ((RDBMSMapping)m.getDatastoreMapping(j)).getUpdateInputParameter();
                            where.append(condition);

                            if (!whereFields.contains(abs_field_num))
                            {
                                whereFields.add(abs_field_num);
                            }
                            parametersIndex[j] = paramIndex++;
                        }
                    }
                }
                else
                {
                    // Update fields
                    stmtMappingDefinition.getUpdateFields()[fmd.getAbsoluteFieldNumber()] = sei;
                    for (int j = 0; j < parametersIndex.length; j++)
                    {
                        // check if the column was not already assigned
                        Column c = (Column)m.getDatastoreMapping(j).getDatastoreField();
                        DatastoreIdentifier columnId = c.getIdentifier();
                        boolean columnExists = assignedColumns.containsKey(columnId.toString());
                        if (columnExists)
                        {
                            parametersIndex[j] = ((Integer)assignedColumns.get(columnId.toString())).intValue();
                        }

                        String param = ((RDBMSMapping)m.getDatastoreMapping(j)).getUpdateInputParameter();
                        if (!columnExists)
                        {
                            if (columnAssignments.length() > 0)
                            {
                                columnAssignments.append(",");
                            }
                            columnAssignments.append(columnId).append("=").append(param);
                        }

                        if (param.indexOf("?") > -1)
                        {
                            // only add fields to be replaced by the real values only if the param value has ?
                            if (!updateFields.contains(abs_field_num))
                            {
                                updateFields.add(abs_field_num);
                            }
                            parametersIndex[j] = paramIndex++;
                        }
                        if (!columnExists)
                        {
                            assignedColumns.put(columnId.toString(), Integer.valueOf(fmd.getAbsoluteFieldNumber()));
                        }
                    }
                }
            }

            if (m instanceof MappingCallbacks)
            {
                mc.add(m);
            }
        }

        /**
         * Consumes a mapping associated to "special" columns.
         * @param m The mapping.
         * @param mappingType the Mapping type
         */
        public void consumeMapping(JavaTypeMapping m, int mappingType)
        {
            if (mappingType == MappingConsumer.MAPPING_TYPE_VERSION)
            {
                // Surrogate version column
                String inputParam = ((RDBMSMapping)m.getDatastoreMapping(0)).getUpdateInputParameter();

                if (whereClauseConsumption)
                {
                    where.append(" AND " + m.getDatastoreMapping(0).getDatastoreField().getIdentifier() + 
                        "=" + inputParam);

                    StatementMappingIndex versStmtIdx = new StatementMappingIndex(m);
                    versStmtIdx.addParameterOccurrence(new int[]{paramIndex++});
                    stmtMappingDefinition.setWhereVersion(versStmtIdx);
                }
                else
                {
                    String condition = m.getDatastoreMapping(0).getDatastoreField().getIdentifier() + "=" + inputParam;
                    if (columnAssignments.length() > 0)
                    {
                        columnAssignments.append(", ");
                    }
                    columnAssignments.append(condition);

                    StatementMappingIndex versStmtIdx = new StatementMappingIndex(m);
                    versStmtIdx.addParameterOccurrence(new int[]{paramIndex++});
                    stmtMappingDefinition.setUpdateVersion(versStmtIdx);
                }
            }
            else if (mappingType == MappingConsumer.MAPPING_TYPE_DATASTORE_ID)
            {
                // Surrogate datastore-id column
                where.append(key.getColumns().get(0).getIdentifier() + "=?");

                StatementMappingIndex datastoreIdIdx = new StatementMappingIndex(m);
                datastoreIdIdx.addParameterOccurrence(new int[]{paramIndex++});
                stmtMappingDefinition.setWhereDatastoreId(datastoreIdIdx);
            }
        }

        /**
         * Consumer a datastore field without mapping.
         * @param fld The datastore field
         */
        public void consumeUnmappedDatastoreField(DatastoreField fld)
        {
            // Do nothing since we don't handle unmapped columns
        }

        /**
         * @return Returns the mappingCallbacks.
         */
        public List getMappingCallbacks()
        {
            return mc;
        }

        /**
         * Accessor for the numbers of fields to be updated (excluding PK fields).
         * @return array of absolute field numbers
         */
        public int[] getUpdateFieldNumbers()
        {
            int[] fieldNumbers = new int[updateFields.size()];
            for (int i = 0; i < updateFields.size(); ++i)
            {
                fieldNumbers[i] = ((Integer) updateFields.get(i)).intValue();
            }
            return fieldNumbers;
        }

        /**
         * Accessor for the numbers of fields in the WHERE clause.
         * @return array of absolute WHERE clause field numbers
         */
        public int[] getWhereFieldNumbers()
        {
            int[] fieldNumbers = new int[whereFields.size()];
            for (int i = 0; i < whereFields.size(); i++)
            {
                fieldNumbers[i] = ((Integer) whereFields.get(i)).intValue();
            }
            return fieldNumbers;
        }

        /**
         * Accessor for the basic update SQL statement.
         * <PRE>
         * UPDATE TABLE SET COL1 = ?, COL2 = ? WHERE COL3 = ? AND COL4 = ? 
         * </PRE>
         * @return The update SQL statement
         */
        public String getStatement()
        {
            if (columnAssignments.length() < 1)
            {
                return null;
            }
            return "UPDATE " + table.toString() + " SET " + columnAssignments + " WHERE " + where;
        }
    }
}
