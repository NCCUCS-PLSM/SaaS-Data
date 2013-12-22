/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.exceptions.NotYetFlushedException;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.scostore.ArrayStore;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract representation of the backing store for an array.
 */
public abstract class AbstractArrayStore extends ElementContainerStore implements ArrayStore
{
    /**
     * Constructor.
     * @param storeMgr Manager for the store
     * @param clr ClassLoader resolver
     */
    protected AbstractArrayStore(RDBMSStoreManager storeMgr, ClassLoaderResolver clr)
    {
        super(storeMgr, clr);
    }

    /**
     * Accessor for the array from the datastore.
     * @param ownerSM SM for the owner
     * @return The array (as a List of objects)
     */
    public List getArray(ObjectProvider ownerSM)
    {
        Iterator iter = iterator(ownerSM);
        List elements = new ArrayList();
        while (iter.hasNext())
        {
            Object obj = iter.next();
            elements.add(obj);
        }

        return elements;
    }

    /**
     * Clear the association from owner to all elements. Observes the necessary dependent field settings 
     * with respect to whether it should delete the element when doing so.
     * @param ownerSM State Manager for the container.
     */
    public void clear(ObjectProvider ownerSM)
    {
        Collection dependentElements = null;
        if (ownerMemberMetaData.getArray().isDependentElement())
        {
            // Retain the dependent elements that need deleting after clearing
            dependentElements = new HashSet();
            Iterator iter = iterator(ownerSM);
            while (iter.hasNext())
            {
                dependentElements.add(iter.next());
            }
        }
        clearInternal(ownerSM);

        if (dependentElements != null && dependentElements.size() > 0)
        {
            ownerSM.getExecutionContext().deleteObjects(dependentElements.toArray());
        }
    }

    /**
     * Method to set the array for the specified owner to the passed value.
     * @param ownerSM State Manager for the owner
     * @param array the array
     * @return Whether the array was updated successfully
     */
    public boolean set(ObjectProvider ownerSM, Object array)
    {
        if (array == null || Array.getLength(array) == 0)
        {
            return true;
        }

        // Validate all elements for writing
        ExecutionContext ec = ownerSM.getExecutionContext();
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++)
        {
            Object obj = Array.get(array, i);
            validateElementForWriting(ec, obj, null);
        }

        boolean modified = false;
        List exceptions = new ArrayList();
        boolean batched = allowsBatching() && length > 1;

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            try
            {
                processBatchedWrites(mconn);

                // Loop through all elements to be added
                Object element = null;
                for (int i = 0; i < length; i++)
                {
                    element = Array.get(array, i);

                    try
                    {
                        // Add the row to the join table
                        int[] rc = internalAdd(ownerSM, element, mconn, batched, i, (i == length - 1));
                        if (rc != null)
                        {
                            for (int j = 0; j < rc.length; j++)
                            {
                                if (rc[j] > 0)
                                {
                                    // At least one record was inserted
                                    modified = true;
                                }
                            }
                        }
                    }
                    catch (MappedDatastoreException mde)
                    {
                        mde.printStackTrace();
                        exceptions.add(mde);
                        NucleusLogger.DATASTORE.error(mde);
                    }
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (MappedDatastoreException e)
        {
            e.printStackTrace();
            exceptions.add(e);
            NucleusLogger.DATASTORE.error(e);
        }

        if (!exceptions.isEmpty())
        {
            // Throw all exceptions received as the cause of a NucleusDataStoreException so the user can see which
            // record(s) didn't persist
            String msg = LOCALISER.msg("056009", ((Exception) exceptions.get(0)).getMessage());
            NucleusLogger.DATASTORE.error(msg);
            throw new NucleusDataStoreException(msg, 
                (Throwable[]) exceptions.toArray(new Throwable[exceptions.size()]), ownerSM.getObject());
        }

        return modified;
    }

    /**
     * Adds one element to the association owner vs elements
     * @param sm State Manager for the container
     * @param element The element to add
     * @param position The position to add this element at
     * @return Whether it was successful
     */
    public boolean add(ObjectProvider sm, Object element, int position)
    {
        ExecutionContext ec = sm.getExecutionContext();
        validateElementForWriting(ec, element, null);

        boolean modified = false;

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);

            try
            {
                // Add a row to the join table
                int[] returnCode = internalAdd(sm, element, mconn, false, position, true);
                if (returnCode[0] > 0)
                {
                    modified = true;
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (MappedDatastoreException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056009", e.getMessage()), e.getCause());
        }

        return modified;
    }

    /**
     * Accessor for an iterator through the array elements.
     * @param ownerSM State Manager for the container.
     * @return The Iterator
     */
    public abstract Iterator iterator(ObjectProvider ownerSM);

    public void clearInternal(ObjectProvider ownerSM)
    {
        String clearStmt = getClearStmt();
        try
        {
            ExecutionContext ec = ownerSM.getExecutionContext();
            ManagedConnection mconn = getStoreManager().getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, clearStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(ownerSM, ec, ps, jdbcPosition, this);
                    if (getRelationDiscriminatorMapping() != null)
                    {
                        BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                    }

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
            throw new NucleusDataStoreException(LOCALISER.msg("056013", clearStmt), e);
        }
    }

    /**
     * Internal method to add a row to the join table.
     * Used by add() and set() to add a row to the join table.
     * @param sm StateManager for the owner of the collection
     * @param element The element to add the relation to
     * @param conn The connection
     * @param batched Whether we are batching
     * @param orderId The order id to use for this element relation
     * @param executeNow Whether to execute the statement now (and not wait for any batch)
     * @return Whether a row was inserted
     * @throws SQLException Thrown if an error occurs
     */
    public int[] internalAdd(ObjectProvider sm, Object element, ManagedConnection conn, boolean batched, int orderId, 
            boolean executeNow) throws MappedDatastoreException
    {
        ExecutionContext ec = sm.getExecutionContext();
        SQLController sqlControl = storeMgr.getSQLController();
        String addStmt = getAddStmt();
        try
        {
            PreparedStatement ps = sqlControl.getStatementForUpdate(conn, addStmt, false);
            boolean notYetFlushedError = false;
            try
            {
                // Insert the join table row
                int jdbcPosition = 1;
                jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                jdbcPosition = BackingStoreHelper.populateElementInStatement(ec, ps, element, jdbcPosition, getElementMapping());
                jdbcPosition = BackingStoreHelper.populateOrderInStatement(ec, ps, orderId, jdbcPosition, getOrderMapping());
                if (getRelationDiscriminatorMapping() != null)
                {
                    jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                }

                // Execute the statement
                return sqlControl.executeStatementUpdate(ec, conn, addStmt, ps, executeNow);
            }
            catch (NotYetFlushedException nfe)
            {
                notYetFlushedError = true;
                throw nfe;
            }
            finally
            {
                if (notYetFlushedError)
                {
                    sqlControl.abortStatementForConnection(conn, ps);
                }
                else
                {
                    sqlControl.closeStatement(conn, ps);
                }
            }
        }
        catch (SQLException e)
        {
            throw new MappedDatastoreException(addStmt, e);
        }
    }

    public void processBatchedWrites(ManagedConnection mconn) throws MappedDatastoreException
    {
        SQLController sqlControl = storeMgr.getSQLController();
        try
        {
            sqlControl.processStatementsForConnection(mconn); // Process all waiting batched statements before we start our work
        }
        catch (SQLException e)
        {
            throw new MappedDatastoreException("SQLException", e);
        }
    }
}