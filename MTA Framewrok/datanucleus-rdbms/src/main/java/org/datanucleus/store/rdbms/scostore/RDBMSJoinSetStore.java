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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.Relation;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.exceptions.NotYetFlushedException;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.JDBCUtils;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.fieldmanager.DynamicSchemaFieldManager;
import org.datanucleus.store.rdbms.query.StatementParameterMapping;
import org.datanucleus.store.rdbms.sql.DiscriminatorStatementGenerator;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.StatementGenerator;
import org.datanucleus.store.rdbms.sql.UnionStatementGenerator;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.store.rdbms.table.CollectionTable;
import org.datanucleus.store.scostore.SetStore;
import org.datanucleus.store.types.sco.SCOMtoN;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * RDBMS-specific implementation of a {@link SetStore} using join table.
 */
public class RDBMSJoinSetStore extends AbstractSetStore
{
    /** Statement to check the existence of an owner-element relation. */
    protected String locateStmt;

    /** Statement to get the maximum order column id so we can set the next insert value. */
    protected String maxOrderColumnIdStmt;

    /** JDBC statement to use for retrieving keys of the map (locking). */
    private String iteratorStmtLocked = null;

    /** JDBC statement to use for retrieving keys of the map (not locking). */
    private String iteratorStmtUnlocked = null;

    private StatementClassMapping iteratorMappingClass = null;
    private StatementParameterMapping iteratorMappingParams = null;

    /**
     * Constructor for a join set store for RDBMS.
     * @param mmd owner member metadata
     * @param joinTable The join table
     * @param clr The ClassLoaderResolver
     */
    public RDBMSJoinSetStore(AbstractMemberMetaData mmd, CollectionTable joinTable, ClassLoaderResolver clr)
    {
        super((RDBMSStoreManager) joinTable.getStoreManager(), clr);

        // A Set really needs a SetTable, but we need to cope with the situation
        // where a user declares a field as Collection but is instantiated as a List or a Set
        // so we just accept CollectionTable and rely on it being adequate
        this.containerTable = joinTable;
        setOwner(mmd, clr);

        this.ownerMapping = joinTable.getOwnerMapping();
        this.elementMapping = joinTable.getElementMapping();
        this.orderMapping = joinTable.getOrderMapping();
        this.relationDiscriminatorMapping = joinTable.getRelationDiscriminatorMapping();
        this.relationDiscriminatorValue = joinTable.getRelationDiscriminatorValue();

        this.elementType = mmd.getCollection().getElementType();
        this.elementsAreEmbedded = joinTable.isEmbeddedElement();
        this.elementsAreSerialised = joinTable.isSerialisedElement();

        if (elementsAreSerialised)
        {
            elementInfo = null;
        }
        else
        {
            Class element_class = clr.classForName(elementType);
            if (ClassUtils.isReferenceType(element_class))
            {
                // Collection of reference types (interfaces/Objects)
                String[] implNames = MetaDataUtils.getInstance().getImplementationNamesForReferenceField(
                    ownerMemberMetaData, FieldRole.ROLE_COLLECTION_ELEMENT, clr, storeMgr.getMetaDataManager());
                elementInfo = new ElementInfo[implNames.length];
                for (int i = 0; i < implNames.length; i++)
                {
                    DatastoreClass table = storeMgr.getDatastoreClass(implNames[i], clr);
                    AbstractClassMetaData cmd =
                        storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(implNames[i], clr);
                    elementInfo[i] = new ElementInfo(cmd, table);
                }
            }
            else
            {
                // Set<PC>, Set<Non-PC>
                // Generate the information for the possible elements
                emd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(element_class, clr);
                if (emd != null && !elementsAreEmbedded)
                {
                    elementInfo = getElementInformationForClass();
                }
                else
                {
                    elementInfo = null;
                }
            }
        }
    }

    /**
     * Method to update the collection to be the supplied collection of elements.
     * @param sm StateManager of the object
     * @param coll The collection to use
     */
    public void update(ObjectProvider sm, Collection coll)
    {
        if (coll == null || coll.isEmpty())
        {
            clear(sm);
            return;
        }

        if (ownerMemberMetaData.getCollection().isSerializedElement() || ownerMemberMetaData.getCollection().isEmbeddedElement())
        {
            // Serialized/Embedded elements so just clear and add again
            clear(sm);
            addAll(sm, coll, 0);
            return;
        }

        // Find existing elements, and remove any that are no longer present
        Iterator elemIter = iterator(sm);
        Collection existing = new HashSet();
        while (elemIter.hasNext())
        {
            Object elem = elemIter.next();
            if (!coll.contains(elem))
            {
                remove(sm, elem, -1, true);
            }
            else
            {
                existing.add(elem);
            }
        }

        if (existing.size() != coll.size())
        {
            // Add any elements that aren't already present
            Iterator iter = coll.iterator();
            while (iter.hasNext())
            {
                Object elem = iter.next();
                if (!existing.contains(elem))
                {
                    add(sm, elem, 0);
                }
            }
        }
    }

    /**
     * Remove all elements from a collection from the association owner vs elements.
     * @param sm State Manager for the container
     * @param elements Collection of elements to remove
     * @return Whether the database was updated
     */
    public boolean removeAll(ObjectProvider sm, Collection elements, int size)
    {
        if (elements == null || elements.size() == 0)
        {
            return false;
        }

        boolean modified = removeAllInternal(sm, elements, size);
        boolean dependent = ownerMemberMetaData.getCollection().isDependentElement();
        if (ownerMemberMetaData.isCascadeRemoveOrphans())
        {
            dependent = true;
        }
        if (dependent)
        {
            // "delete-dependent" : delete elements if the collection is marked as dependent
            // TODO What if the collection contains elements that are not in the Set ? should not delete them
            sm.getExecutionContext().deleteObjects(elements.toArray());
        }

        return modified;
    }

    /**
     * Convenience method to check if an element already refers to the owner in an M-N relation.
     * @param ownerSM State Manager of the owner
     * @param element The element
     * @return Whether the element contains the owner
     */
    private boolean elementAlreadyContainsOwnerInMtoN(ObjectProvider ownerSM, Object element)
    {
        ExecutionContext ec = ownerSM.getExecutionContext();
        ObjectProvider elementSM = ec.findObjectProvider(element);
        AbstractMemberMetaData[] relatedMmds = ownerMemberMetaData.getRelatedMemberMetaData(ec.getClassLoaderResolver());
        Object elementSCO = elementSM.provideField(relatedMmds[0].getAbsoluteFieldNumber());
        if (elementSCO instanceof SCOMtoN)
        {
            // The field is already a SCO wrapper so just query it
            if (contains(ownerSM, element))
            {
                NucleusLogger.DATASTORE.info(LOCALISER.msg("056040", ownerMemberMetaData.getFullFieldName(), element));
                return true;
            }
        }
        else
        {
            // The element is not a SCO wrapper so query the datastore directly
            // TODO Fix this. It is inefficient to go off to the datastore to check whether a record exists.
            // This should be changed in line with TCK test AllRelationships since that tries to load up
            // many relationships, and CollectionMapping.postInsert calls addAll with these hence we don't
            // have SCO's to use contains() on
            if (locate(ownerSM, element))
            {
                NucleusLogger.DATASTORE.info(LOCALISER.msg("056040", ownerMemberMetaData.getFullFieldName(), element));
                return true;
            }
        }
        return false;
    }

    /**
     * Adds one element to the association owner vs elements.
     * @param sm State Manager for the container.
     * @param element Element to add
     * @return Whether it was successful
     */
    public boolean add(ObjectProvider sm, Object element, int size)
    {
        // Check that the object is valid for writing
        ExecutionContext ec = sm.getExecutionContext();
        validateElementForWriting(ec, element, null);

        if (relationType == Relation.ONE_TO_MANY_BI)
        {
            // TODO This is ManagedRelations - move into RelationshipManager
            // Managed Relations : make sure we have consistency of relation
            ObjectProvider elementSM = ec.findObjectProvider(element);
            if (elementSM != null)
            {
                AbstractMemberMetaData[] relatedMmds = ownerMemberMetaData.getRelatedMemberMetaData(clr);
                Object elementOwner = elementSM.provideField(relatedMmds[0].getAbsoluteFieldNumber());
                if (elementOwner == null)
                {
                    // No owner, so correct it
                    NucleusLogger.PERSISTENCE.info(LOCALISER.msg("056037", sm.toPrintableID(), ownerMemberMetaData
                            .getFullFieldName(), StringUtils.toJVMIDString(elementSM.getObject())));
                    elementSM.replaceField(relatedMmds[0].getAbsoluteFieldNumber(), sm.getObject());
                }
                else if (elementOwner != sm.getObject() && sm.getReferencedPC() == null)
                {
                    // Owner of the element is neither this container nor being attached
                    // Inconsistent owner, so throw exception
                    throw new NucleusUserException(LOCALISER.msg("056038", sm.toPrintableID(), ownerMemberMetaData
                            .getFullFieldName(), StringUtils.toJVMIDString(elementSM.getObject()), StringUtils.toJVMIDString(elementOwner)));
                }
            }
        }

        boolean modified = false;

        boolean toBeInserted = true;
        if (relationType == Relation.MANY_TO_MANY_BI)
        {
            // This is an M-N relation so we need to check if the element already has us in its collection
            // to avoid duplicate join table entries
            toBeInserted = !elementAlreadyContainsOwnerInMtoN(sm, element);
        }

        if (toBeInserted)
        {
            try
            {
                ManagedConnection mconn = storeMgr.getConnection(ec);
                try
                {
                    // Add a row to the join table
                    int orderID = -1;
                    if (orderMapping != null)
                    {
                        orderID = getNextIDForOrderColumn(sm);
                    }
                    int[] returnCode = internalAdd(sm, element, mconn, false, orderID, true);
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
                NucleusLogger.DATASTORE.error(e);
                String msg = LOCALISER.msg("056009", e.getMessage());
                NucleusLogger.DATASTORE.error(msg);
                throw new NucleusDataStoreException(msg, e);
            }
        }

        return modified;
    }

    /**
     * Adds all elements from a collection to the association container.
     * @param sm State Manager for the container.
     * @param elements Collection of elements to add
     * @return Whether it was successful
     */
    public boolean addAll(ObjectProvider sm, Collection elements, int size)
    {
        if (elements == null || elements.size() == 0)
        {
            return false;
        }

        boolean modified = false;
        List exceptions = new ArrayList();
        boolean batched = (elements.size() > 1);

        // Validate all elements for writing
        ExecutionContext ec = sm.getExecutionContext();
        Iterator iter = elements.iterator();
        while (iter.hasNext())
        {
            Object element = iter.next();
            validateElementForWriting(ec, element, null);

            if (relationType == Relation.ONE_TO_MANY_BI)
            {
                // TODO This is ManagedRelations - move into RelationshipManager
                // Managed Relations : make sure we have consistency of relation
                ObjectProvider elementSM = sm.getExecutionContext().findObjectProvider(element);
                if (elementSM != null)
                {
                    AbstractMemberMetaData[] relatedMmds = ownerMemberMetaData.getRelatedMemberMetaData(clr);
                    Object elementOwner = elementSM.provideField(relatedMmds[0].getAbsoluteFieldNumber());
                    if (elementOwner == null)
                    {
                        // No owner, so correct it
                        NucleusLogger.PERSISTENCE.info(LOCALISER.msg("056037", sm.toPrintableID(), ownerMemberMetaData
                                .getFullFieldName(), StringUtils.toJVMIDString(elementSM.getObject())));
                        elementSM.replaceField(relatedMmds[0].getAbsoluteFieldNumber(), sm.getObject());
                    }
                    else if (elementOwner != sm.getObject() && sm.getReferencedPC() == null)
                    {
                        // Owner of the element is neither this container nor its referenced object
                        // Inconsistent owner, so throw exception
                        throw new NucleusUserException(LOCALISER.msg("056038", sm.toPrintableID(),
                            ownerMemberMetaData.getFullFieldName(), StringUtils.toJVMIDString(elementSM.getObject()), StringUtils
                                    .toJVMIDString(elementOwner)));
                    }
                }
            }
        }

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            try
            {
                preGetNextIDForOrderColumn(mconn);

                int nextOrderID = 0;
                if (orderMapping != null)
                {
                    // Get the order id for the first item
                    nextOrderID = getNextIDForOrderColumn(sm);
                }

                // Loop through all elements to be added
                iter = elements.iterator();
                Object element = null;
                while (iter.hasNext())
                {
                    element = iter.next();

                    try
                    {
                        // Add the row to the join table
                        int[] rc = internalAdd(sm, element, mconn, batched, nextOrderID, !batched || (batched && !iter.hasNext()));
                        if (rc != null)
                        {
                            for (int i = 0; i < rc.length; i++)
                            {
                                if (rc[i] > 0)
                                {
                                    // At least one record was inserted
                                    modified = true;
                                }
                            }
                        }
                        nextOrderID++;
                    }
                    catch (MappedDatastoreException mde)
                    {
                        exceptions.add(mde);
                        NucleusLogger.DATASTORE.error("Exception thrown", mde);
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
            exceptions.add(e);
            NucleusLogger.DATASTORE.error("Exception thrown", e);
        }

        if (!exceptions.isEmpty())
        {
            // Throw all exceptions received as the cause of a NucleusDataStoreException so the user can see which
            // record(s) didn't persist
            String msg = LOCALISER.msg("056009", ((Exception) exceptions.get(0)).getMessage());
            NucleusLogger.DATASTORE.error(msg);
            throw new NucleusDataStoreException(msg, (Throwable[]) exceptions.toArray(new Throwable[exceptions.size()]), sm.getObject());
        }

        return modified;
    }

    /**
     * Method to add a row to the join table. Used by add() and addAll() to add a row to the join table.
     * @param sm StateManager for the owner of the collection
     * @param element The element to add the relation to
     * @param conn Connection to use
     * @param batched Whether we are batching
     * @param orderId The order id to use for this element relation (if ordering is used)
     * @param executeNow Whether to execute the statement now (or leave til later)
     * @return The return code(s) for any records added. There may be multiple if using batched
     * @throws MappedDatastoreException Thrown if an error occurs
     */
    private int[] internalAdd(ObjectProvider sm, Object element, ManagedConnection conn, boolean batched, int orderId, boolean executeNow)
        throws MappedDatastoreException
    {
        boolean toBeInserted = true;
        if (relationType == Relation.MANY_TO_MANY_BI)
        {
            // This is an M-N relation so we need to check if the element already has us
            // in its collection to avoid duplicate join table entries
            // TODO Find a better way of doing this
            toBeInserted = !elementAlreadyContainsOwnerInMtoN(sm, element);
        }

        if (toBeInserted)
        {
            return doInternalAdd(sm, element, conn, batched, orderId, executeNow);
        }
        return null;
    }

    protected boolean removeAllInternal(ObjectProvider sm, Collection elements, int size)
    {
        boolean modified = false;

        String removeAllStmt = getRemoveAllStmt(sm, elements);
        try
        {
            ExecutionContext ec = sm.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, removeAllStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    Iterator iter = elements.iterator();
                    while (iter.hasNext())
                    {
                        Object element = iter.next();
                        jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                        jdbcPosition = BackingStoreHelper.populateElementForWhereClauseInStatement(ec, ps, element,
                            jdbcPosition, elementMapping);
                        if (relationDiscriminatorMapping != null)
                        {
                            jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                        }
                    }

                    int[] number = sqlControl.executeStatementUpdate(ec, mconn, removeAllStmt, ps, true);
                    if (number[0] > 0)
                    {
                        modified = true;
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
            NucleusLogger.DATASTORE.error(e);
            throw new NucleusDataStoreException(LOCALISER.msg("056012", removeAllStmt), e);
        }
        return modified;
    }

    /**
     * Generate statement for deleting items from the Set.
     * The EMBEDDEDFIELDX is only present when the elements are PC(embedded).
     * <PRE>
     * DELETE FROM SETTABLE
     * WHERE OWNERCOL=?
     * AND ELEMENTCOL = ?
     * [AND EMBEDDEDFIELD1 = ? AND EMBEDDEDFIELD2 = ? AND EMBEDDEDFIELD3 = ?]
     * [AND RELATION_DISCRIM = ?]
     * </PRE>
     * @return Statement for deleting items from the Set.
     */
    protected String getRemoveStmt(Object element)
    {
        StringBuffer stmt = new StringBuffer("DELETE FROM ");
        stmt.append(containerTable.toString());
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
        BackingStoreHelper.appendWhereClauseForElement(stmt, elementMapping, element, elementsAreSerialised,
            null, false);
        if (relationDiscriminatorMapping != null)
        {
            BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
        }

        return stmt.toString();
    }

    /**
     * Generate statement for removing a collection of items from the Set.
     * <PRE>
     * DELETE FROM SETTABLE
     * WHERE (OWNERCOL=? AND ELEMENTCOL=?) OR
     *       (OWNERCOL=? AND ELEMENTCOL=?) OR
     *       (OWNERCOL=? AND ELEMENTCOL=?)
     * </PRE>
     * @param op ObjectProvider for the owner
     * @param elements Collection of elements to remove
     * @return Statement for deleting items from the Set.
     */
    protected String getRemoveAllStmt(ObjectProvider op, Collection elements)
    {
        if (elements == null || elements.size() == 0)
        {
            return null;
        }

        StringBuffer stmt = new StringBuffer("DELETE FROM ");
        stmt.append(containerTable.toString());
        stmt.append(" WHERE ");

        Iterator elementsIter = elements.iterator();
        boolean first = true;
        while (elementsIter.hasNext())
        {
            Object element = elementsIter.next();
            if (first)
            {
                stmt.append("(");
            }
            else
            {
                stmt.append(" OR (");
            }

            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            BackingStoreHelper.appendWhereClauseForElement(stmt, elementMapping, element, elementsAreSerialised,
                null, false);
            if (relationDiscriminatorMapping != null)
            {
                BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
            }

            stmt.append(")");
            first = false;
        }

        return stmt.toString();
    }

    public boolean locate(ObjectProvider sm, Object element)
    {
        boolean exists = true;
        String stmt = getLocateStmt(element);
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
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    jdbcPosition = BackingStoreHelper.populateElementForWhereClauseInStatement(ec, ps, element, 
                        jdbcPosition, elementMapping);
                    if (relationDiscriminatorMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                    }

                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, stmt, ps);
                    try
                    {
                        if (!rs.next())
                        {
                            exists = false;
                        }
                    }
                    catch (SQLException sqle)
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
            NucleusLogger.DATASTORE.error(e);
            throw new NucleusDataStoreException(LOCALISER.msg("RDBMS.SCO.LocateRequestFailed", stmt), e);
        }
        return exists;
    }

    protected int[] doInternalAdd(ObjectProvider sm, Object element, ManagedConnection conn, boolean batched,
            int orderId, boolean executeNow)
    throws MappedDatastoreException
    {
        // Check for dynamic schema updates prior to addition
        if (storeMgr.getBooleanObjectProperty("datanucleus.rdbms.dynamicSchemaUpdates").booleanValue())
        {
            DynamicSchemaFieldManager dynamicSchemaFM = new DynamicSchemaFieldManager(storeMgr, sm);
            Collection coll = new HashSet();
            coll.add(element);
            dynamicSchemaFM.storeObjectField(ownerMemberMetaData.getAbsoluteFieldNumber(), coll);
            if (dynamicSchemaFM.hasPerformedSchemaUpdates())
            {
                invalidateAddStmt();
            }
        }

        String addStmt = getAddStmt();
        boolean notYetFlushedError = false;
        ExecutionContext ec = sm.getExecutionContext();
        SQLController sqlControl = storeMgr.getSQLController();
        try
        {
            PreparedStatement ps = sqlControl.getStatementForUpdate(conn, addStmt, batched);
            try
            {
                // Insert the join table row
                int jdbcPosition = 1;
                jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                jdbcPosition = BackingStoreHelper.populateElementInStatement(ec, ps, element, jdbcPosition, elementMapping);
                if (orderMapping != null)
                {
                    jdbcPosition = BackingStoreHelper.populateOrderInStatement(ec, ps, orderId, jdbcPosition, orderMapping);
                }
                if (relationDiscriminatorMapping != null)
                {
                    jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                }

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
    /**
     * Generate statement for checking the existence of an owner-element relation (used for M-N).
     * <PRE>
     * SELECT 1 FROM SETTABLE WHERE OWNERCOL = ? AND ELEMENTCOL = ?
     * </PRE>
     * @return Statement for locating an owner-element relation
     */
    private String getLocateStmt(Object element)
    {
        if (locateStmt == null)
        {
            StringBuffer stmt = new StringBuffer("SELECT 1 FROM ");
            stmt.append(containerTable.toString());
            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            BackingStoreHelper.appendWhereClauseForElement(stmt, elementMapping, element, elementsAreSerialised,
                null, false);
            if (relationDiscriminatorMapping != null)
            {
                BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
            }

            if (elementMapping instanceof ReferenceMapping && elementMapping.getNumberOfDatastoreMappings() > 1)
            {
                // Don't cache since depends on the element
                return stmt.toString();
            }
            locateStmt = stmt.toString();
        }

        return locateStmt;
    }

    protected void preGetNextIDForOrderColumn(ManagedConnection mconn) throws MappedDatastoreException
    {
        SQLController sqlControl = storeMgr.getSQLController();
        try
        {
            // Process all waiting batched statements before we start our work
            sqlControl.processStatementsForConnection(mconn);
        }
        catch (SQLException e)
        {
            throw new MappedDatastoreException("SQLException", e);
        }
    }

    /**
     * Generate statement for obtaining the maximum id for the order column.
     * <PRE>
     * SELECT MAX(SCOID) FROM SETTABLE
     * WHERE OWNERCOL=?
     * [AND RELATION_DISCRIM=?]
     * </PRE>
     * @return The Statement returning the higher id
     */
    private String getMaxOrderColumnIdStmt()
    {
        if (maxOrderColumnIdStmt == null)
        {
            StringBuffer stmt = new StringBuffer("SELECT MAX(" + 
                orderMapping.getDatastoreMapping(0).getDatastoreField().getIdentifier().toString() + ")");
            stmt.append(" FROM ");
            stmt.append(containerTable.toString());
            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            if (relationDiscriminatorMapping != null)
            {
                BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
            }

            maxOrderColumnIdStmt = stmt.toString();
        }

        return maxOrderColumnIdStmt;
    }

    protected int getNextIDForOrderColumn(ObjectProvider sm)
    {
        int nextID;
        ExecutionContext ec = sm.getExecutionContext();
        String stmt = getMaxOrderColumnIdStmt();
        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();

            try
            {
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, stmt);
                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    if (relationDiscriminatorMapping != null)
                    {
                        BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                    }

                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, stmt, ps);
                    try
                    {
                        if (!rs.next())
                        {
                            nextID = 1;
                        }
                        else
                        {
                            nextID = rs.getInt(1) + 1;
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
            throw new NucleusDataStoreException(LOCALISER.msg("056020", stmt), e);
        }

        return nextID;
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
                        if (elementsAreEmbedded || elementsAreSerialised)
                        {
                            // No ResultObjectFactory needed - handled by SetStoreIterator
                            return new RDBMSSetStoreIterator(ownerSM, rs, null, this);
                        }
                        else if (elementMapping instanceof ReferenceMapping)
                        {
                            // No ResultObjectFactory needed - handled by SetStoreIterator
                            return new RDBMSSetStoreIterator(ownerSM, rs, null, this);
                        }
                        else
                        {
                            ResultObjectFactory rof = storeMgr.newResultObjectFactory(emd, 
                                iteratorMappingClass, false, null, clr.classForName(elementType));
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