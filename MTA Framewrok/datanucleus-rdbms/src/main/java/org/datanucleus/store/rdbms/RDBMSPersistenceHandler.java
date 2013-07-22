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
package org.datanucleus.store.rdbms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.InheritanceStrategy;
import org.datanucleus.metadata.Relation;
import org.datanucleus.store.AbstractPersistenceHandler;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.SecondaryDatastoreClass;
import org.datanucleus.store.rdbms.fieldmanager.DynamicSchemaFieldManager;
import org.datanucleus.store.rdbms.request.DeleteRequest;
import org.datanucleus.store.rdbms.request.FetchRequest;
import org.datanucleus.store.rdbms.request.InsertRequest;
import org.datanucleus.store.rdbms.request.LocateBulkRequest;
import org.datanucleus.store.rdbms.request.LocateRequest;
import org.datanucleus.store.rdbms.request.Request;
import org.datanucleus.store.rdbms.request.RequestIdentifier;
import org.datanucleus.store.rdbms.request.RequestType;
import org.datanucleus.store.rdbms.request.UpdateRequest;
import org.datanucleus.store.rdbms.table.ClassView;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.SoftValueMap;

/**
 * Handler for persistence for RDBMS datastores.
 */
public class RDBMSPersistenceHandler extends AbstractPersistenceHandler
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Manager for the store. */
    protected final MappedStoreManager storeMgr;

    /** The cache of database requests. Access is synchronized on the map object itself. */
    private Map<RequestIdentifier, Request> requestsByID = Collections.synchronizedMap(new SoftValueMap());

    /**
     * Constructor.
     * @param storeMgr StoreManager
     */
    public RDBMSPersistenceHandler(StoreManager storeMgr)
    {
        this.storeMgr = (MappedStoreManager)storeMgr;
    }

    /**
     * Method to close the handler and release any resources.
     */
    public void close()
    {
        requestsByID.clear();
        requestsByID = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.AbstractPersistenceHandler#useReferentialIntegrity()
     */
    @Override
    public boolean useReferentialIntegrity()
    {
        // RDBMS need objects inserted in specific orders
        return true;
    }

    // ------------------------------ Insert ----------------------------------

    /**
     * Inserts a persistent object into the database.
     * The insert can take place in several steps, one insert per table that it is stored in.
     * e.g When persisting an object that uses "new-table" inheritance for each level of the inheritance tree
     * then will get an INSERT into each table. When persisting an object that uses "complete-table"
     * inheritance then will get a single INSERT into its table.
     * @param op The ObjectProvider of the object to be inserted.
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public void insertObject(ObjectProvider op)
    {
        // Check if read-only so update not permitted
        storeMgr.assertReadOnlyForUpdateOfObject(op);

        // Check if we need to do any updates to the schema before inserting this object
        checkForSchemaUpdatesForFieldsOfObject(op, op.getLoadedFieldNumbers());

        ExecutionContext ec = op.getExecutionContext();
        ClassLoaderResolver clr = op.getExecutionContext().getClassLoaderResolver();
        String className = op.getClassMetaData().getFullClassName();
        DatastoreClass dc = storeMgr.getDatastoreClass(className, clr);
        if (dc == null)
        {
            if (op.getClassMetaData().getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
            {
                throw new NucleusUserException(LOCALISER.msg("032013", className));
            }
            throw new NucleusException(LOCALISER.msg("032014", className, 
                op.getClassMetaData().getInheritanceMetaData().getStrategy())).setFatal();
        }

        if (ec.getStatistics() != null)
        {
            ec.getStatistics().incrementInsertCount();
        }

        insertTable(dc, op, clr);
    }

    /**
     * Convenience method to handle the insert into the various tables that this object is persisted into.
     * @param table The table to process
     * @param op ObjectProvider for the object being inserted
     * @param clr ClassLoader resolver
     */
    private void insertTable(DatastoreClass table, ObjectProvider op, ClassLoaderResolver clr)
    {
        if (table instanceof ClassView)
        {
            throw new NucleusUserException("Cannot perform InsertRequest on RDBMS view " + table);
        }

        DatastoreClass supertable = table.getSuperDatastoreClass();
        if (supertable != null)
        {
            // Process the superclass table first
            insertTable(supertable, op, clr);
        }

        // Do the actual insert of this table
        getInsertRequest(table, op.getClassMetaData(), clr).execute(op);

        // Process any secondary tables
        Collection<SecondaryDatastoreClass> secondaryTables = table.getSecondaryDatastoreClasses();
        if (secondaryTables != null)
        {
            Iterator<SecondaryDatastoreClass> tablesIter = secondaryTables.iterator();
            while (tablesIter.hasNext())
            {
                // Process the secondary table
                insertTable(tablesIter.next(), op, clr);
            }
        }
    }

    /**
     * Returns a request object that will insert a row in the given table. 
     * The store manager will cache the request object for re-use by subsequent requests to the same table.
     * @param table The table into which to insert.
     * @param cmd ClassMetaData of the object of the request
     * @param clr ClassLoader resolver
     * @return An insertion request object.
     */
    private Request getInsertRequest(DatastoreClass table, AbstractClassMetaData cmd, ClassLoaderResolver clr)
    {
        RequestIdentifier reqID = new RequestIdentifier(table, null, RequestType.INSERT, cmd.getFullClassName());
        Request req = requestsByID.get(reqID);
        if (req == null)
        {
            req = new InsertRequest(table, cmd, clr);
            requestsByID.put(reqID, req);
        }
        return req;
    }

    // ------------------------------ Fetch ----------------------------------

    /**
     * Fetches (fields of) a persistent object from the database.
     * This does a single SELECT on the candidate of the class in question. Will join to inherited
     * tables as appropriate to get values persisted into other tables. Can also join to the tables of
     * related objects (1-1, N-1) as neccessary to retrieve those objects.
     * @param op Object Provider of the object to be fetched.
     * @param memberNumbers The numbers of the members to be fetched.
     * @throws NucleusObjectNotFoundException if the object doesn't exist
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public void fetchObject(ObjectProvider op, int memberNumbers[])
    {
        AbstractMemberMetaData[] mmds = null;
        if (memberNumbers != null && memberNumbers.length > 0)
        {
            int[] memberNumbersToProcess = memberNumbers;
            AbstractClassMetaData cmd = op.getClassMetaData();
            ExecutionContext ec = op.getExecutionContext();
            ClassLoaderResolver clr = ec.getClassLoaderResolver();

            if (storeMgr.getBooleanProperty("datanucleus.rdbms.fetchUnloadedAutomatically"))
            {
                // Option to automatically load up any non-loaded fields as deemed appropriate
                // Here we simply load up any unloaded non-relation or 1-1/N-1 members
                if (!op.getLifecycleState().isDeleted())
                {
                    // Check if this will actually do a SELECT (because we don't want to impose that if not otherwise)
                    boolean fetchPerformsSelect = false;
                    for (int i=0;i<memberNumbers.length;i++)
                    {
                        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNumbers[i]);
                        int relationType = mmd.getRelationType(clr);
                        if (relationType != Relation.ONE_TO_MANY_UNI && relationType != Relation.ONE_TO_MANY_BI &&
                                relationType != Relation.MANY_TO_MANY_BI)
                        {
                            fetchPerformsSelect = true;
                            break;
                        }
                    }

                    if (fetchPerformsSelect)
                    {
                        // Definitely does a SELECT, so try to identify any additional non-relation or 1-1, N-1
                        // members that aren't loaded that could be fetched right now in this call.
                        List<Integer> memberNumberList = new ArrayList<Integer>();
                        for (int i=0;i<memberNumbers.length;i++)
                        {
                            memberNumberList.add(memberNumbers[i]);
                        }

                        // Check if we could retrieve any other unloaded fields in this call
                        boolean[] loadedFlags = op.getLoadedFields();
                        for (int i=0;i<loadedFlags.length;i++)
                        {
                            boolean requested = false;
                            for (int j=0;j<memberNumbers.length;j++)
                            {
                                if (memberNumbers[j] == i)
                                {
                                    requested = true;
                                    break;
                                }
                            }
                            if (!requested && !loadedFlags[i])
                            {
                                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(i);
                                int relType = mmd.getRelationType(clr);
                                if (relType == Relation.NONE || relType == Relation.ONE_TO_ONE_BI || relType == Relation.ONE_TO_ONE_UNI)
                                {
                                    memberNumberList.add(i);
                                }
                            }
                        }
                        memberNumbersToProcess = new int[memberNumberList.size()];
                        int i=0;
                        Iterator<Integer> fieldNumberIter = memberNumberList.iterator();
                        while (fieldNumberIter.hasNext())
                        {
                            memberNumbersToProcess[i++] = fieldNumberIter.next();
                        }
                    }
                }
            }

            // Convert the field numbers for this class into their metadata for the class
            mmds = new AbstractMemberMetaData[memberNumbersToProcess.length];
            for (int i=0;i<mmds.length;i++)
            {
                mmds[i] = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNumbersToProcess[i]);
            }

            if (op.isEmbedded())
            {
                StringBuffer str = new StringBuffer();
                for (int i=0;i<mmds.length;i++)
                {
                    if (i > 0)
                    {
                        str.append(',');
                    }
                    str.append(mmds[i].getName());
                }
                NucleusLogger.PERSISTENCE.info("Request to load fields \"" + str.toString() +
                    "\" of class " + op.getClassMetaData().getFullClassName() + " but object is embedded, so ignored");
            }
            else
            {
                if (ec.getStatistics() != null)
                {
                    ec.getStatistics().incrementFetchCount();
                }

                DatastoreClass table = storeMgr.getDatastoreClass(op.getClassMetaData().getFullClassName(), clr);
                Request req = getFetchRequest(table, mmds, op.getClassMetaData(), clr);
                req.execute(op);
            }
        }
    }

    /**
     * Returns a request object that will fetch a row from the given table. 
     * The store manager will cache the request object for re-use by subsequent requests to the same table.
     * @param table The table from which to fetch.
     * @param mmds MetaData for the members corresponding to the columns to be fetched.
     * @param cmd ClassMetaData of the object of the request
     * @param clr ClassLoader resolver
     * @return A fetch request object.
     */
    private Request getFetchRequest(DatastoreClass table, AbstractMemberMetaData[] mmds, AbstractClassMetaData cmd, 
            ClassLoaderResolver clr)
    {
        RequestIdentifier reqID = new RequestIdentifier(table, mmds, RequestType.FETCH, 
            cmd.getFullClassName());
        Request req = requestsByID.get(reqID);
        if (req == null)
        {
            req = new FetchRequest(table, mmds, cmd, clr);
            requestsByID.put(reqID, req);
        }
        return req;
    }

    // ------------------------------ Update ----------------------------------

    /**
     * Updates a persistent object in the database.
     * The update can take place in several steps, one update per table that it is stored in (depending on 
     * which fields are updated).
     * e.g When updating an object that uses "new-table" inheritance for each level of the inheritance tree
     * then will get an UPDATE into each table. When updating an object that uses "complete-table"
     * inheritance then will get a single UPDATE into its table.
     * @param op The ObjectProvider of the object to be updated.
     * @param fieldNumbers The numbers of the fields to be updated.
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public void updateObject(ObjectProvider op, int fieldNumbers[])
    {
        // Check if read-only so update not permitted
        storeMgr.assertReadOnlyForUpdateOfObject(op);

        // Check if we need to do any updates to the schema before updating this object
        checkForSchemaUpdatesForFieldsOfObject(op, fieldNumbers);

        AbstractMemberMetaData[] fmds = null;
        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            // Convert the field numbers for this class into their metadata for the table
            ExecutionContext ec = op.getExecutionContext();
            fmds = new AbstractMemberMetaData[fieldNumbers.length];
            for (int i=0;i<fmds.length;i++)
            {
                fmds[i] = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumbers[i]);
            }

            if (ec.getStatistics() != null)
            {
                ec.getStatistics().incrementUpdateCount();
            }

            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            DatastoreClass dc = storeMgr.getDatastoreClass(op.getObject().getClass().getName(), clr);
            updateTable(dc, op, clr, fmds);
        }
    }

    /**
     * Convenience method to handle the update into the various tables that this object is persisted into.
     * @param table The table to process
     * @param op ObjectProvider for the object being updated
     * @param clr ClassLoader resolver
     * @param fieldMetaData MetaData for the fields being updated
     */
    private void updateTable(DatastoreClass table, ObjectProvider op, ClassLoaderResolver clr,
            AbstractMemberMetaData[] fieldMetaData)
    {
        if (table instanceof ClassView)
        {
            throw new NucleusUserException("Cannot perform UpdateRequest on RDBMS view " + table);
        }

        DatastoreClass supertable = table.getSuperDatastoreClass();
        if (supertable != null)
        {
            // Process the superclass table first
            updateTable(supertable, op, clr, fieldMetaData);
        }

        // Do the actual update of this table
        getUpdateRequest(table, fieldMetaData, op.getClassMetaData(), clr).execute(op);

        // Update any secondary tables
        Collection<SecondaryDatastoreClass> secondaryTables = table.getSecondaryDatastoreClasses();
        if (secondaryTables != null)
        {
            Iterator<SecondaryDatastoreClass> tablesIter = secondaryTables.iterator();
            while (tablesIter.hasNext())
            {
                // Process the secondary table
                updateTable(tablesIter.next(), op, clr, fieldMetaData);
            }
        }
    }

    /**
     * Returns a request object that will update a row in the given table. 
     * The store manager will cache the request object for re-use by subsequent requests to the same table.
     * @param table The table in which to update.
     * @param mmds The metadata corresponding to the columns to be updated. 
     *     MetaData whose columns exist in supertables will be ignored.
     * @param cmd ClassMetaData of the object of the request
     * @param clr ClassLoader resolver
     * @return An update request object.
     */
    private Request getUpdateRequest(DatastoreClass table, AbstractMemberMetaData[] mmds, AbstractClassMetaData cmd, 
            ClassLoaderResolver clr)
    {
        RequestIdentifier reqID = new RequestIdentifier(table, mmds, RequestType.UPDATE, cmd.getFullClassName());
        Request req = requestsByID.get(reqID);
        if (req == null)
        {
            req = new UpdateRequest(table, mmds, cmd, clr);
            requestsByID.put(reqID, req);
        }
        return req;
    }

    // ------------------------------ Delete ----------------------------------

    /**
     * Deletes a persistent object from the database.
     * The delete can take place in several steps, one delete per table that it is stored in.
     * e.g When deleting an object that uses "new-table" inheritance for each level of the inheritance tree
     * then will get an DELETE for each table. When deleting an object that uses "complete-table"
     * inheritance then will get a single DELETE for its table.
     * @param op The ObjectProvider of the object to be deleted.
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public void deleteObject(ObjectProvider op)
    {
        // Check if read-only so update not permitted
        storeMgr.assertReadOnlyForUpdateOfObject(op);

        ExecutionContext ec = op.getExecutionContext();
        if (ec.getStatistics() != null)
        {
            ec.getStatistics().incrementDeleteCount();
        }

        ClassLoaderResolver clr = op.getExecutionContext().getClassLoaderResolver();
        DatastoreClass dc = storeMgr.getDatastoreClass(op.getClassMetaData().getFullClassName(), clr);
        deleteTable(dc, op, clr);
    }

    /**
     * Convenience method to handle the delete from the various tables that this object is persisted into.
     * @param table The table to process
     * @param sm StateManager for the object being deleted
     * @param clr ClassLoader resolver
     */
    private void deleteTable(DatastoreClass table, ObjectProvider sm, ClassLoaderResolver clr)
    {
        if (table instanceof ClassView)
        {
            throw new NucleusUserException("Cannot perform DeleteRequest on RDBMS view " + table);
        }

        // Delete any secondary tables
        Collection<SecondaryDatastoreClass> secondaryTables = table.getSecondaryDatastoreClasses();
        if (secondaryTables != null)
        {
            Iterator<SecondaryDatastoreClass> tablesIter = secondaryTables.iterator();
            while (tablesIter.hasNext())
            {
                // Process the secondary table
                deleteTable(tablesIter.next(), sm, clr);
            }
        }

        // Do the actual delete of this table
        getDeleteRequest(table, sm.getClassMetaData(), clr).execute(sm);

        DatastoreClass supertable = table.getSuperDatastoreClass();
        if (supertable != null)
        {
            // Process the superclass table last
            deleteTable(supertable, sm, clr);
        }
    }

    /**
     * Returns a request object that will delete a row from the given table.
     * The store manager will cache the request object for re-use by subsequent requests to the same table.
     * @param table The table from which to delete.
     * @param acmd ClassMetaData of the object of the request
     * @param clr ClassLoader resolver
     * @return A deletion request object.
     */
    private Request getDeleteRequest(DatastoreClass table, AbstractClassMetaData acmd, ClassLoaderResolver clr)
    {
        RequestIdentifier reqID = new RequestIdentifier(table, null, RequestType.DELETE, acmd.getFullClassName());
        Request req = requestsByID.get(reqID);
        if (req == null)
        {
            req = new DeleteRequest(table, acmd, clr);
            requestsByID.put(reqID, req);
        }
        return req;
    }

    // ------------------------------ Locate ----------------------------------

    public void locateObjects(ObjectProvider[] ops)
    {
        if (ops == null || ops.length == 0)
        {
            return;
        }

        ClassLoaderResolver clr = ops[0].getExecutionContext().getClassLoaderResolver();
        Map<DatastoreClass, List<ObjectProvider>> opsByTable = new HashMap<DatastoreClass, List<ObjectProvider>>();
        for (int i=0;i<ops.length;i++)
        {
            AbstractClassMetaData cmd = ops[i].getClassMetaData();
            DatastoreClass table = storeMgr.getDatastoreClass(cmd.getFullClassName(), clr);
            table = table.getBaseDatastoreClass(); // Use root table in hierarchy
            List<ObjectProvider> opList = opsByTable.get(table);
            if (opList == null)
            {
                opList = new ArrayList<ObjectProvider>();
            }
            opList.add(ops[i]);
            opsByTable.put(table, opList);
        }

        Iterator<Map.Entry<DatastoreClass, List<ObjectProvider>>> tableIter = opsByTable.entrySet().iterator();
        while (tableIter.hasNext())
        {
            Map.Entry<DatastoreClass, List<ObjectProvider>> entry = tableIter.next();
            DatastoreClass table = entry.getKey();
            List<ObjectProvider> tableOps = entry.getValue();

            // TODO This just uses the base table. Could change to use the most-derived table
            // which would permit us to join to supertables and load more fields during this process
            LocateBulkRequest req = new LocateBulkRequest(table);
            req.execute(tableOps.toArray(new ObjectProvider[tableOps.size()]));
        }
    }

    /**
     * Locates this object in the datastore.
     * @param op ObjectProvider for the object to be found
     * @throws NucleusObjectNotFoundException if the object doesnt exist
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public void locateObject(ObjectProvider op)
    {
        ClassLoaderResolver clr = op.getExecutionContext().getClassLoaderResolver();
        DatastoreClass table = storeMgr.getDatastoreClass(op.getObject().getClass().getName(), clr);
        getLocateRequest(table, op.getObject().getClass().getName()).execute(op);
    }

    /**
     * Returns a request object that will locate a row from the given table.
     * The store manager will cache the request object for re-use by subsequent requests to the same table.
     * @param table The table from which to locate.
     * @param className the class name of the object of the request
     * @return A locate request object.
     */
    private Request getLocateRequest(DatastoreClass table, String className)
    {
        RequestIdentifier reqID = new RequestIdentifier(table, null, RequestType.LOCATE, className);
        Request req = requestsByID.get(reqID);
        if (req == null)
        {
            req = new LocateRequest(table);
            requestsByID.put(reqID, req);
        }
        return req;
    }

    // ------------------------------ Find ----------------------------------

    /**
     * Method to return a persistable object with the specified id. Optional operation for StoreManagers.
     * Should return a (at least) hollow PersistenceCapable object if the store manager supports the operation.
     * If the StoreManager is managing the in-memory object instantiation (as part of co-managing the object 
     * lifecycle in general), then the StoreManager has to create the object during this call (if it is not 
     * already created). Most relational databases leave the in-memory object instantion to Core, but some 
     * object databases may manage the in-memory object instantion, effectively preventing Core of doing this.
     * <p>
     * StoreManager implementations may simply return null, indicating that they leave the object instantiate to 
     * us. Other implementations may instantiate the object in question (whether the implementation may trust 
     * that the object is not already instantiated has still to be determined). If an implementation believes
     * that an object with the given ID should exist, but in fact does not exist, then the implementation should 
     * throw a RuntimeException. It should not silently return null in this case.
     * </p>
     * @param ec execution context
     * @param id the id of the object in question.
     * @return a persistable object with a valid object state (for example: hollow) or null, 
     *     indicating that the implementation leaves the instantiation work to us.
     */
    public Object findObject(ExecutionContext ec, Object id)
    {
        return null;
    }

    // ------------------------------ Convenience ----------------------------------

    /**
     * Convenience method to remove all requests since the schema has changed.
     */
    public void removeAllRequests()
    {
        synchronized (requestsByID)
        {
            requestsByID.clear();
        }
    }

    /**
     * Convenience method to remove all requests that use a particular table since the structure
     * of the table has changed potentially leading to missing columns in the cached version.
     * @param table The table
     */
    public void removeRequestsForTable(DatastoreClass table)
    {
        synchronized(requestsByID)
        {
            // Synchronise on the "requestsById" set since while it is "synchronised itself, all iterators needs this sync
            Set keySet = new HashSet(requestsByID.keySet());
            Iterator<RequestIdentifier> keyIter = keySet.iterator();
            while (keyIter.hasNext())
            {
                RequestIdentifier reqId = keyIter.next();
                if (reqId.getTable() == table)
                {
                    requestsByID.remove(reqId);
                }
            }
        }
    }

    /**
     * Check if we need to update the schema before performing an insert/update.
     * This is typically of use where the user has an interface field and some new implementation
     * is trying to be persisted to that field, so we need to update the schema.
     * @param sm StateManager for the object
     * @param fieldNumbers The fields to check for required schema updates
     */
    private void checkForSchemaUpdatesForFieldsOfObject(ObjectProvider sm, int[] fieldNumbers)
    {
        if (storeMgr.getBooleanObjectProperty("datanucleus.rdbms.dynamicSchemaUpdates").booleanValue())
        {
            DynamicSchemaFieldManager dynamicSchemaFM = new DynamicSchemaFieldManager((RDBMSStoreManager)storeMgr, sm);
            sm.provideFields(fieldNumbers, dynamicSchemaFM);
            if (dynamicSchemaFM.hasPerformedSchemaUpdates())
            {
                requestsByID.clear();
            }
        }
    }
}