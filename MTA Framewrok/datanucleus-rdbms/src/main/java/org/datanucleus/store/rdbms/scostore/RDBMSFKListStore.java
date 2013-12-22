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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.FetchPlan;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.CollectionMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.Relation;
import org.datanucleus.metadata.OrderMetaData.FieldOrder;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.FieldValues;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.mapped.mapping.MappingHelper;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.art.utb.UTbServiceHelper;
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
import org.datanucleus.store.scostore.ListStore;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * RDBMS-specific implementation of an {@link ListStore} using foreign keys.
 */
public class RDBMSFKListStore extends AbstractListStore
{
    /** Field number of owner link in element class. */
    private final int ownerFieldNumber;

    /** JDBC statement to use for iterating the whole list (locking). */
    private String iteratorStmtLocked = null;

    /** JDBC statement to use for iterating the whole list (not locking). */
    private String iteratorStmtUnlocked = null;

    /** result definition when iterating the whole list. */
    private StatementClassMapping iteratorMappingDef = null;

    /** parameter definition when iterating the whole list. */
    private StatementParameterMapping iteratorMappingParams = null;

    /** Statement for updating a foreign key in a 1-N unidirectional */
    private String updateFkStmt;

    private String clearNullifyStmt;

    private String removeAtNullifyStmt;

    private String setStmt;
    private String unsetStmt;

    /**
     * @param mmd Metadata for owning field/property
     * @param storeMgr Manager for the datastore
     * @param clr ClassLoader resolver
     */
    public RDBMSFKListStore(AbstractMemberMetaData mmd, RDBMSStoreManager storeMgr, ClassLoaderResolver clr)
    {
        super(storeMgr, clr);

        setOwner(mmd, clr);
        CollectionMetaData colmd = mmd.getCollection();
        if (colmd == null)
        {
            throw new NucleusUserException(LOCALISER.msg("056001", mmd.getFullFieldName()));
        }

        // Load the element class
        elementType = colmd.getElementType();
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
            throw new NucleusUserException(LOCALISER.msg("056031", ownerMemberMetaData.getFullFieldName()));
        }
        else if (elementInfo == null || elementInfo.length == 0)
        {
            throw new NucleusUserException(LOCALISER.msg("056075", ownerMemberMetaData.getFullFieldName(), elementType));
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
            AbstractMemberMetaData eofmd = emd.getMetaDataForMember(mappedByFieldName);
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
            ownerFieldNumber = emd.getAbsolutePositionOfMember(ownerFieldName);
            ownerMapping = elementInfo[0].getDatastoreClass().getMemberMapping(eofmd);
            if (ownerMapping == null)
            {
                throw new NucleusUserException(LOCALISER.msg("056029",
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
            // The element class knows nothing about the owner (but its table has external mappings)
            ownerFieldNumber = -1;
            ownerMapping = elementInfo[0].getDatastoreClass().getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
            if (ownerMapping == null)
            {
                throw new NucleusUserException(LOCALISER.msg("056030", 
                    mmd.getAbstractClassMetaData().getFullClassName(), mmd.getName(), elementType));
            }
        }

        orderMapping = elementInfo[0].getDatastoreClass().getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
        if (mmd.getOrderMetaData() != null && !mmd.getOrderMetaData().isIndexedList())
        {
            indexedList = false;
        }
        if (orderMapping == null && indexedList)
        {
            // "Indexed List" but no order mapping present!
            throw new NucleusUserException(LOCALISER.msg("056041", 
                mmd.getAbstractClassMetaData().getFullClassName(), mmd.getName(), elementType));
        }

        relationDiscriminatorMapping =
            elementInfo[0].getDatastoreClass().getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK_DISCRIM);
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
     * Method to set an object in the List at a position.
     * @param op ObjectProvider for the owner
     * @param index The item index
     * @param element What to set it to.
     * @param allowDependentField Whether to enable dependent-field deletes during the set
     * @return The value before setting.
     */
    public Object set(ObjectProvider op, int index, Object element, boolean allowDependentField)
    {
        validateElementForWriting(op, element, -1); // Last argument means don't set the position on any INSERT
        Object oldElement = get(op, index);
//        return set(op, index, element, allowDependentField, oldElement);

        try
        {
            ExecutionContext ec = op.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                // Unset the existing object from this position
                String unsetStmt = getUnsetStmt();
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, unsetStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(op, ec, ps, jdbcPosition, this);
                    if (orderMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateOrderInStatement(ec, ps, index, jdbcPosition, orderMapping);
                    }
                    if (relationDiscriminatorMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                    }

                    sqlControl.executeStatementUpdate(ec, mconn, unsetStmt, ps, true);
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }

                // Set the new object at this position
                String setStmt = getSetStmt(element);
                PreparedStatement ps2 = sqlControl.getStatementForUpdate(mconn, setStmt, false);
                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(op, ec, ps2, jdbcPosition, this);
                    if (orderMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateOrderInStatement(ec, ps2, index, jdbcPosition, orderMapping);
                    }
                    if (relationDiscriminatorMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps2, jdbcPosition, this);
                    }
                    jdbcPosition = BackingStoreHelper.populateElementForWhereClauseInStatement(ec, ps2, element, jdbcPosition, elementMapping);

                    sqlControl.executeStatementUpdate(ec, mconn, setStmt, ps2, true);
                }
                finally
                {
                    ps2.close();
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056015", setStmt), e);
        }

        // Dependent field
        boolean dependent = getOwnerMemberMetaData().getCollection().isDependentElement();
        if (getOwnerMemberMetaData().isCascadeRemoveOrphans())
        {
            dependent = true;
        }
        if (dependent && allowDependentField)
        {
            if (oldElement != null)
            {
                // Delete the element if it is dependent and doesnt have a duplicate entry in the list
                op.getExecutionContext().deleteObjectInternal(oldElement);
            }
        }

        return oldElement;
    }

    /**
     * Utility to update a foreign-key in the element in the case of a unidirectional 1-N relationship.
     * @param op ObjectProvider for the owner
     * @param element The element to update
     * @param owner The owner object to set in the FK
     * @param index The index position (or -1 if not known)
     * @return Whether it was performed successfully
     */
    private boolean updateElementFk(ObjectProvider op, Object element, Object owner, int index)
    {
        if (element == null)
        {
            return false;
        }

        ExecutionContext ec = op.getExecutionContext();
        JavaTypeMapping ownerMapping = getOwnerMapping();
        JavaTypeMapping orderMapping = getOrderMapping();
        JavaTypeMapping elementMapping = getElementMapping();
        ElementContainerStore.ElementInfo[] elementInfo = getElementInfo();
        JavaTypeMapping relationDiscriminatorMapping = getRelationDiscriminatorMapping();
        AbstractMemberMetaData ownerMemberMetaData = getOwnerMemberMetaData();

        String updateFkStmt = getUpdateFkStmt(element);
        boolean retval;
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
                        storeMgr.getDatastoreClass(element.getClass().getName(), clr);
                    }
                    if (owner == null)
                    {
                        if (ownerMemberMetaData != null)
                        {
                            ownerMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, ownerMapping), null, op,
                                ownerMemberMetaData.getAbsoluteFieldNumber());
                        }
                        else
                        {
                            ownerMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, ownerMapping), null);
                        }
                        jdbcPosition += ownerMapping.getNumberOfDatastoreMappings();
                    }
                    else
                    {
                        jdbcPosition = BackingStoreHelper.populateOwnerInStatement(op, ec, ps, jdbcPosition, this);
                    }
                    if (orderMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateOrderInStatement(ec, ps, index, jdbcPosition, orderMapping);
                    }
                    if (relationDiscriminatorMapping != null)
                    {
                        jdbcPosition = BackingStoreHelper.populateRelationDiscriminatorInStatement(ec, ps, jdbcPosition, this);
                    }

                    jdbcPosition = BackingStoreHelper.populateElementForWhereClauseInStatement(ec, ps, element, jdbcPosition, elementMapping);

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
     * Method to update the collection to be the supplied collection of elements.
     * @param op ObjectProvider for the owner
     * @param coll The collection to use
     */
    public void update(ObjectProvider op, Collection coll)
    {
        if (coll == null || coll.isEmpty())
        {
            clear(op);
            return;
        }

        // Find existing elements, and remove any that are no longer present
        Collection existing = new ArrayList();
        Iterator elemIter = iterator(op);
        while (elemIter.hasNext())
        {
            Object elem = elemIter.next();
            if (!coll.contains(elem))
            {
                remove(op, elem, -1, true);
            }
            else
            {
                existing.add(elem);
            }
        }

        if (existing.equals(coll))
        {
            // Existing (after any removals) is same as the specified so job done
            return;
        }

        // TODO Improve this - need to allow for list element position changes etc
        clear(op);
        addAll(op, coll, 0);
    }

    /**
     * Internal method for adding an item to the List.
     * @param op ObjectProvider for the owner
     * @param startAt The start position
     * @param atEnd Whether to add at the end
     * @param c The Collection of elements to add.
     * @param size Current size of list (if known). -1 if not known
     * @return Whether it was successful
     */
    protected boolean internalAdd(ObjectProvider op, int startAt, boolean atEnd, Collection c, int size)
    {
        if (c == null || c.size() == 0)
        {
            return true;
        }

        // Check what we have persistent already
        int currentListSize = 0;
        if (size < 0)
        {
            // Get the current size from the datastore
            currentListSize = size(op);
        }
        else
        {
            currentListSize = size;
        }

        boolean shiftingElements = true;
        if (atEnd || startAt == currentListSize)
        {
            shiftingElements = false;
            startAt = currentListSize; // Not shifting so we insert from the end
        }

        boolean elementsNeedPositioning = false;
        int position = startAt;
        Iterator elementIter = c.iterator();
        while (elementIter.hasNext())
        {
            // Persist any non-persistent objects optionally at their final list position (persistence-by-reachability)
            if (shiftingElements)
            {
                // We have to shift things so dont bother with positioning
                position = -1;
            }

            boolean inserted = validateElementForWriting(op, elementIter.next(), position);
            if (!inserted || shiftingElements)
            {
                // This element wasnt positioned in the validate so we need to set the positions later
                elementsNeedPositioning = true;
            }
            if (!shiftingElements)
            {
                position++;
            }
        }

        if (shiftingElements)
        {
            // We need to shift existing elements before positioning the new ones
            try
            {
                // Calculate the amount we need to shift any existing elements by
                // This is used where inserting between existing elements and have to shift down all elements after the start point
                int shift = c.size();

                ExecutionContext ec = op.getExecutionContext();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                try
                {
                    // shift up existing elements after start position by "shift"
                    for (int i=currentListSize-1; i>=startAt; i--)
                    {
                        // Shift the index of this row by "shift"
                        internalShift(op, mconn, false, i, shift, true);
                    }
                }
                finally
                {
                    mconn.release();
                }
            }
            catch (MappedDatastoreException e)
            {
                // An error was encountered during the shift process so abort here
                throw new NucleusDataStoreException(LOCALISER.msg("056009", e.getMessage()), e.getCause());
            }
        }

        if (shiftingElements || elementsNeedPositioning)
        {
            // Some elements have been shifted so the new elements need positioning now, or we already had some
            // of the new elements persistent and so they need their positions setting now
            elementIter = c.iterator();
            while (elementIter.hasNext())
            {
                Object element = elementIter.next();
                updateElementFk(op, element, op.getObject(), startAt);
                startAt++;
            }
        }

        return true;
    }


    /**
     * Convenience method to remove the specified element from the List.
     * @param op ObjectProvider for the owner
     * @param element The element
     * @return Whether the List was modified
     */
    protected boolean internalRemove(ObjectProvider op, Object element, int size)
    {
        if (indexedList)
        {
            // Indexed List
            // The element can be at one position only (no duplicates allowed in FK list)
            int index = indexOf(op, element);
            if (index == -1)
            {
                return false;
            }
            internalRemoveAt(op, index, size);
        }
        else
        {
            // Ordered List - no index so null the FK (if nullable) or delete the element
            if (ownerMapping.isNullable())
            {
                // Nullify the FK
                ExecutionContext ec = op.getExecutionContext();
                ObjectProvider elementSM = ec.findObjectProvider(element);
                if (relationType == Relation.ONE_TO_MANY_BI)
                {
                    // Set field in element to null (so it nulls the FK)
                    // TODO This is ManagedRelations - move into RelationshipManager
                    elementSM.replaceFieldMakeDirty(ownerMemberMetaData.getRelatedMemberMetaData(clr)[0].getAbsoluteFieldNumber(), 
                        null);
                    if (op.getExecutionContext().isFlushing())
                    {
                        elementSM.flush();
                    }
                }
                else
                {
                    // Null the (external) FK in the element
                    updateElementFk(op, element, null, -1);
                }
            }
            else
            {
                // Delete the element
                op.getExecutionContext().deleteObjectInternal(element);
            }
        }

        return true;
    }

    /**
     * Convenience method to manage the removal of an element from the collection, performing
     * any necessary "managed relationship" updates when the field is bidirectional.
     * @param ownerSM StateManager for the collection owner
     * @param element The element
     */
    protected void manageRemovalOfElement(ObjectProvider ownerSM, Object element)
    {
        // TODO Complete this
        /*ObjectManager om = ownerSM.getExecutionContext();
        if (relationType == Relation.ONE_TO_MANY_BI && om.getManageRelations())
        {
            // Managed Relations : 1-N bidirectional so null the owner on the elements
            if (!om.getApiAdapter().isDeleted(element))
            {
                StateManager elementSM = om.findStateManager(element);
                if (elementSM != null)
                {
                    // Null the owner of the element
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug(
                            LOCALISER.msg("055010",
                                ownerSM.toPrintableID(),
                                ownerMemberMetaData.getFullFieldName(),
                                StringUtils.toJVMIDString(element)));
                    }

                    elementSM.replaceField(getFieldNumberInElementForBidirectional(elementSM), null, true);
                    if (om.isFlushing())
                    {
                        // Make sure this change gets flushed
                        elementSM.flush();
                    }
                }
            }
        }*/
    }

    /**
     * Internal method to remove an object at a location in the List.
     * Differs from the JoinTable List in that it nulls out the owner FK.
     * @param op ObjectProvider for the owner
     * @param index The location
     * @param size Current size of list (if known). -1 if not known
     */
    protected void internalRemoveAt(ObjectProvider op, int index, int size)
    {
        if (!indexedList)
        {
            throw new NucleusUserException("Cannot remove an element from a particular position with an ordered list since no indexes exist");
        }

        boolean nullify = false;
        if (ownerMapping.isNullable() && orderMapping != null && orderMapping.isNullable())
        {
            NucleusLogger.DATASTORE.debug(LOCALISER.msg("056043"));
            nullify = true;
        }
        else
        {
            NucleusLogger.DATASTORE.debug(LOCALISER.msg("056042"));
        }

        String stmt;
        if (nullify)
        {
            // TODO When using this statement we need to plug in the element table name, and do it for
            // all possible element tables
            stmt = getRemoveAtNullifyStmt();
        }
        else
        {
            // TODO Really ought to make this delete via ExecutionContext
            stmt = getRemoveAtStmt();
        }
        internalRemoveAt(op, index, stmt, size);
    }

    /**
     * Method to clear the List.
     * This is called by the List.clear() method, or when the container object is being deleted
     * and the elements are to be removed (maybe for dependent field), or also when updating a Collection
     * and removing all existing prior to adding all new.
     * @param op ObjectProvider for the owner
     */
    public void clear(ObjectProvider op)
    {
        boolean deleteElements = false;
        ExecutionContext ec = op.getExecutionContext();
        boolean dependent = ownerMemberMetaData.getCollection().isDependentElement();
        if (ownerMemberMetaData.isCascadeRemoveOrphans())
        {
            dependent = true;
        }
        if (dependent)
        {
            // Elements are dependent and can't exist on their own, so delete them all
            NucleusLogger.DATASTORE.debug(LOCALISER.msg("056034"));
            deleteElements = true;
        }
        else
        {
            if (ownerMapping.isNullable() && orderMapping == null)
            {
                // Field is not dependent, and nullable so we null the FK
                NucleusLogger.DATASTORE.debug(LOCALISER.msg("056036"));
                deleteElements = false;
            }
            else if (ownerMapping.isNullable() && orderMapping != null && orderMapping.isNullable())
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
            // Find elements present in the datastore and delete them one-by-one
            Iterator elementsIter = iterator(op);
            if (elementsIter != null)
            {
                while (elementsIter.hasNext())
                {
                    Object element = elementsIter.next();
                    if (ec.getApiAdapter().isPersistable(element) && ec.getApiAdapter().isDeleted(element))
                    {
                        // Element is waiting to be deleted so flush it (it has the FK)
                        ObjectProvider objSM = ec.findObjectProvider(element);
                        objSM.flush();
                    }
                    else
                    {
                        // Element not yet marked for deletion so go through the normal process
                        ec.deleteObjectInternal(element);
                    }
                }
            }
        }
        else
        {
            // Clear without delete
            // TODO If the relation is bidirectional we need to clear the owner in the element
            String clearNullifyStmt = getClearNullifyStmt();
            try
            {
                ManagedConnection mconn = storeMgr.getConnection(ec);
                SQLController sqlControl = storeMgr.getSQLController();
                try
                {
                    PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, clearNullifyStmt, false);
                    try
                    {
                        int jdbcPosition = 1;
                        jdbcPosition = BackingStoreHelper.populateOwnerInStatement(op, ec, ps, jdbcPosition, this);
                        if (getRelationDiscriminatorMapping() != null)
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
     * Method to validate that an element is valid for writing to the datastore.
     * TODO Minimise differences to super.validateElementForWriting()
     * @param sm StateManager for the List
     * @param element The element to validate
     * @param index The position that the element is being stored at in the list
     * @return Whether the element was inserted
     */
    protected boolean validateElementForWriting(final ObjectProvider sm, Object element, final int index)
    {
        final Object newOwner = sm.getObject();

        // Check if element is ok for use in the datastore, specifying any external mappings that may be required
        boolean inserted = super.validateElementForWriting(sm.getExecutionContext(), element, new FieldValues()
        {
            public void fetchFields(ObjectProvider esm)
            {
                // Find the (element) table storing the FK back to the owner
                boolean isPersistentInterface = storeMgr.getNucleusContext().getMetaDataManager().isPersistentInterface(elementType);
                DatastoreClass elementTable = null;
                if (isPersistentInterface)
                {
                    elementTable = storeMgr.getDatastoreClass(
                        storeMgr.getNucleusContext().getMetaDataManager().getImplementationNameForPersistentInterface(elementType), clr);
                }
                else
                {
                    elementTable = storeMgr.getDatastoreClass(elementType, clr);
                }
                if (elementTable == null)
                {
                    // "subclass-table", persisted into table of other class
                    AbstractClassMetaData[] managingCmds = storeMgr.getClassesManagingTableForClass(emd, clr);
                    if (managingCmds != null && managingCmds.length > 0)
                    {
                        // Find which of these subclasses is appropriate for this element
                        for (int i=0;i<managingCmds.length;i++)
                        {
                            Class tblCls = clr.classForName(managingCmds[i].getFullClassName());
                            if (tblCls.isAssignableFrom(esm.getObject().getClass()))
                            {
                                elementTable = storeMgr.getDatastoreClass(managingCmds[i].getFullClassName(), clr);
                                break;
                            }
                        }
                    }
                }

                if (elementTable != null)
                {
                    JavaTypeMapping externalFKMapping = elementTable.getExternalMapping(ownerMemberMetaData, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
                    if (externalFKMapping != null)
                    {
                        // The element has an external FK mapping so set the value it needs to use in the INSERT
                        esm.setAssociatedValue(externalFKMapping, sm.getObject());
                    }
                    if (relationDiscriminatorMapping != null)
                    {
                        esm.setAssociatedValue(relationDiscriminatorMapping, relationDiscriminatorValue);
                    }
                    if (orderMapping != null && index >= 0)
                    {
                        if (ownerMemberMetaData.getOrderMetaData() != null && ownerMemberMetaData.getOrderMetaData().getMappedBy() != null)
                        {
                            // Order is stored in a field in the element so update it
                            // We support mapped-by fields of types int/long/Integer/Long currently
                            Object indexValue = null;
                            if (orderMapping.getMemberMetaData().getTypeName().equals(ClassNameConstants.JAVA_LANG_LONG) ||
                                    orderMapping.getMemberMetaData().getTypeName().equals(ClassNameConstants.LONG))
                            {
                                indexValue = Long.valueOf(index);
                            }
                            else
                            {
                                indexValue = Integer.valueOf(index);
                            }
                            esm.replaceFieldMakeDirty(orderMapping.getMemberMetaData().getAbsoluteFieldNumber(), indexValue);
                        }
                        else
                        {
                            // Order is stored in a surrogate column so save its vaue for the element to use later
                            esm.setAssociatedValue(orderMapping, Integer.valueOf(index));
                        }
                    }
                }
                if (ownerFieldNumber >= 0)
                {
                    // TODO This is ManagedRelations - move into RelationshipManager
                    // Managed Relations : 1-N bidir, so make sure owner is correct at persist
                    Object currentOwner = esm.provideField(ownerFieldNumber);
                    if (currentOwner == null)
                    {
                        // No owner, so correct it
                        NucleusLogger.PERSISTENCE.info(LOCALISER.msg("056037",
                            sm.toPrintableID(), ownerMemberMetaData.getFullFieldName(), 
                            StringUtils.toJVMIDString(esm.getObject())));
                        esm.replaceFieldMakeDirty(ownerFieldNumber, newOwner);
                    }
                    else if (currentOwner != newOwner && sm.getReferencedPC() == null)
                    {
                        // Owner of the element is neither this container nor is it being attached
                        // Inconsistent owner, so throw exception
                        throw new NucleusUserException(LOCALISER.msg("056038",
                            sm.toPrintableID(), ownerMemberMetaData.getFullFieldName(), 
                            StringUtils.toJVMIDString(esm.getObject()),
                            StringUtils.toJVMIDString(currentOwner)));
                    }
                }
            }
            public void fetchNonLoadedFields(ObjectProvider sm)
            {
            }
            public FetchPlan getFetchPlanForLoading()
            {
                return null;
            }
        });

        return inserted;
    }

    /**
     * Accessor for an iterator through the list elements.
     * @param ownerSM State Manager for the container.
     * @param startIdx The start index in the list (only for indexed lists)
     * @param endIdx The end index in the list (only for indexed lists)
     * @return The List Iterator
     */
    protected ListIterator listIterator(ObjectProvider ownerSM, int startIdx, int endIdx)
    {
        ExecutionContext ec = ownerSM.getExecutionContext();
        Transaction tx = ec.getTransaction();
        boolean useUpdateLock = tx.lockReadObjects();

        StatementClassMapping resultDefinition = null;
        StatementParameterMapping paramDefinition = null;
        String stmt = null;

        if (startIdx < 0 && endIdx < 0)
        {
            // Iteration of all elements - cached
            if (iteratorStmtLocked == null)
            {
                synchronized (this)
                {
                    // Generate the statement, and statement mapping/parameter information
                    iteratorMappingDef = new StatementClassMapping();
                    iteratorMappingParams = new StatementParameterMapping();
                    SQLStatement sqlStmt = getSQLStatementForIterator(ownerSM, startIdx, endIdx,
                        iteratorMappingDef, iteratorMappingParams);
                    iteratorStmtUnlocked = sqlStmt.getSelectStatement().toSQL();
                    sqlStmt.addExtension("lock-for-update", true);
                    iteratorStmtLocked = sqlStmt.getSelectStatement().toSQL();
                    
                    //todo arthur modify
                    stmt = UTbServiceHelper.generateUTbSql(sqlStmt , sqlStmt.getCandidateClassName() );
                }
            }
            resultDefinition = iteratorMappingDef;
            paramDefinition = iteratorMappingParams;
          //todo arthur need check this
            //stmt = (useUpdateLock ? iteratorStmtLocked : iteratorStmtUnlocked);
        }
        else
        {
            // Iteration over a range so generate statement on the fly (uncached)
            resultDefinition = new StatementClassMapping();
            paramDefinition = new StatementParameterMapping();
            SQLStatement sqlStmt = getSQLStatementForIterator(ownerSM, startIdx, endIdx, resultDefinition,
                paramDefinition);
            sqlStmt.addExtension("lock-for-update", useUpdateLock);
            //todo arthur modify
            stmt = UTbServiceHelper.generateUTbSql(sqlStmt , sqlStmt.getCandidateClassName() );
            //stmt = sqlStmt.getSelectStatement().toSQL();
        }

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                // Create the statement and set the owner
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, stmt);
                /* todo arthur marked
                StatementMappingIndex ownerIdx = paramDefinition.getMappingForParameter("owner");
                int numParams = ownerIdx.getNumberOfParameterOccurrences();
                for (int paramInstance=0;paramInstance<numParams;paramInstance++)
                {
                    ownerIdx.getMapping().setObject(ec, ps,
                        ownerIdx.getParameterPositionsForOccurrence(paramInstance), ownerSM.getObject());
                }
                 */
                try
                {
                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, stmt, ps);
                    try
                    {
                        ResultObjectFactory rof = null;
                        if (elementsAreEmbedded || elementsAreSerialised)
                        {
                            throw new NucleusException("Cannot have FK set with non-persistent objects");
                        }
                        else
                        {
                            rof = storeMgr.newResultObjectFactory(emd, resultDefinition, false, null,
                                clr.classForName(elementType));
                        }

                        return new RDBMSListStoreIterator(ownerSM, rs, rof, this);
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
     * Method to generate an SQLStatement for iterating through elements of the list.
     * Selects the element table.
     * Populates the resultMapping and paramMapping argument objects.
     * @param ownerSM StateManager for the owner object
     * @param startIdx start index to be retrieved (inclusive). Only for indexed list
     * @param endIdx end index to be retrieved (exclusive). Only for indexed list
     * @param resultMapping Mapping for the candidate result columns
     * @param paramMapping Mapping for the input parameters
     * @return The SQLStatement
     */
    protected SQLStatement getSQLStatementForIterator(ObjectProvider ownerSM, int startIdx, int endIdx,
            StatementClassMapping resultMapping, StatementParameterMapping paramMapping)
    {
        if (elementInfo == null || elementInfo.length == 0)
        {
            return null;
        }

        SQLStatement sqlStmt = null;

        final ClassLoaderResolver clr = ownerSM.getExecutionContext().getClassLoaderResolver();
        if (elementInfo.length == 1 &&
            elementInfo[0].getDatastoreClass().getDiscriminatorMetaData() != null &&
            elementInfo[0].getDatastoreClass().getDiscriminatorMetaData().getStrategy() != DiscriminatorStrategy.NONE)
        {
            String elementType = ownerMemberMetaData.getCollection().getElementType();
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
            SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(sqlStmt, resultMapping,
                ownerSM.getExecutionContext().getFetchPlan(), sqlStmt.getPrimaryTable(), emd, 0);
        }
        else
        {
            for (int i=0;i<elementInfo.length;i++)
            {
                final Class elementCls = clr.classForName(this.elementInfo[i].getClassName());
                UnionStatementGenerator stmtGen = new UnionStatementGenerator(storeMgr, clr, elementCls, true, null, null);
                stmtGen.setOption(StatementGenerator.OPTION_SELECT_NUCLEUS_TYPE);
                resultMapping.setNucleusTypeColumnName(UnionStatementGenerator.NUC_TYPE_COLUMN);
                SQLStatement subStmt = stmtGen.getStatement();

                // Select the required fields (of the element class)
                if (sqlStmt == null)
                {
                    SQLStatementHelper.selectFetchPlanOfSourceClassInStatement(subStmt, resultMapping,
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
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();
        SQLTable ownerSqlTbl =
            SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), ownerMapping);
        
        SQLExpression ownerExpr = exprFactory.newExpression(sqlStmt, ownerSqlTbl, ownerMapping);
        //todo arthur objectprodiver myid
       // SQLExpression ownerVal = exprFactory.newLiteralParameter(sqlStmt, ownerMapping, null, "OWNER");
        SQLExpression ownerVal = exprFactory.newLiteralParameter(sqlStmt, ownerMapping, ownerSM.getObjectId() , "OWNER");
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

        if (indexedList)
        {
            // "Indexed List" so allow restriction on returned indexes
            boolean needsOrdering = true;
            if (startIdx == -1 && endIdx == -1)
            {
                // Just restrict to >= 0 so we don't get any disassociated elements
                SQLExpression indexExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
                SQLExpression indexVal = exprFactory.newLiteral(sqlStmt, orderMapping, 0);
                sqlStmt.whereAnd(indexExpr.ge(indexVal), true);
            }
            else if (startIdx >= 0 && endIdx == startIdx)
            {
                // Particular index required so add restriction
                needsOrdering = false;
                SQLExpression indexExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
                SQLExpression indexVal = exprFactory.newLiteral(sqlStmt, orderMapping, startIdx);
                sqlStmt.whereAnd(indexExpr.eq(indexVal), true);
            }
            else
            {
                // Add restrictions on start/end indices as required
                if (startIdx >= 0)
                {
                    SQLExpression indexExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
                    SQLExpression indexVal = exprFactory.newLiteral(sqlStmt, orderMapping, startIdx);
                    sqlStmt.whereAnd(indexExpr.ge(indexVal), true);
                }
                else
                {
                    // Just restrict to >= 0 so we don't get any disassociated elements
                    SQLExpression indexExpr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
                    SQLExpression indexVal = exprFactory.newLiteral(sqlStmt, orderMapping, 0);
                    sqlStmt.whereAnd(indexExpr.ge(indexVal), true);
                }

                if (endIdx >= 0)
                {
                    SQLExpression indexExpr2 = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
                    SQLExpression indexVal2 = exprFactory.newLiteral(sqlStmt, orderMapping, endIdx);
                    sqlStmt.whereAnd(indexExpr2.lt(indexVal2), true);
                }
            }

            if (needsOrdering)
            {
                // Order by the ordering column
                SQLTable orderSqlTbl =
                    SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), orderMapping);
                SQLExpression[] orderExprs = new SQLExpression[orderMapping.getNumberOfDatastoreMappings()];
                boolean descendingOrder[] = new boolean[orderMapping.getNumberOfDatastoreMappings()];
                orderExprs[0] = exprFactory.newExpression(sqlStmt, orderSqlTbl, orderMapping);
                sqlStmt.setOrdering(orderExprs, descendingOrder);
            }
        }
        else
        {
            // Apply ordering defined by <order-by>
            DatastoreClass elementTbl = elementInfo[0].getDatastoreClass();
            FieldOrder[] orderComponents = ownerMemberMetaData.getOrderMetaData().getFieldOrders();
            SQLExpression[] orderExprs = new SQLExpression[orderComponents.length];
            boolean[] orderDirs = new boolean[orderComponents.length];

            for (int i=0;i<orderComponents.length;i++)
            {
                String fieldName = orderComponents[i].getFieldName();
                JavaTypeMapping fieldMapping = elementTbl.getMemberMapping(elementInfo[0].getAbstractClassMetaData().getMetaDataForMember(fieldName));
                orderDirs[i] = !orderComponents[i].isForward();
                SQLTable fieldSqlTbl = SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), fieldMapping);
                orderExprs[i] = exprFactory.newExpression(sqlStmt, fieldSqlTbl, fieldMapping);
            }

            sqlStmt.setOrdering(orderExprs, orderDirs);
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
        paramMapping.addMappingForParameter("owner", ownerIdx);

        return sqlStmt;
    }

    /**
     * Generate statement for updating the owner, index columns in an inverse 1-N. 
     * Will result in the statement
     * <PRE>
     * UPDATE ELEMENTTABLE SET FK_COL_1 = ?, FK_COL_2 = ?, FK_IDX = ? [,DISTINGUISHER=?]
     * WHERE ELEMENT_ID = ?
     * </PRE>
     * when we have a single element table, and
     * <PRE>
     * UPDATE ? SET FK_COL_1=?, FK_COL_2=?, FK_IDX=? [,DISTINGUISHER=?]
     * WHERE ELEMENT_ID=?
     * </PRE>
     * when we have multiple element tables possible
     * @return Statement for updating the owner/index of an element in an inverse 1-N
     */
    private String getUpdateFkStmt(Object element)
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
                // Could use elementInfo[0].getDatastoreClass but need to allow for relation in superclass table
                stmt.append(containerTable.toString());
            }
            stmt.append(" SET ");
            for (int i = 0; i < ownerMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(",");
                }
                stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                stmt.append("=");
                stmt.append(((RDBMSMapping) ownerMapping.getDatastoreMapping(i)).getUpdateInputParameter());
            }
            if (orderMapping != null)
            {
                for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append("=");
                    stmt.append(((RDBMSMapping) orderMapping.getDatastoreMapping(i)).getUpdateInputParameter());
                }
            }
            if (relationDiscriminatorMapping != null)
            {
                for (int i = 0; i < relationDiscriminatorMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(relationDiscriminatorMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append("=");
                    stmt.append(((RDBMSMapping) relationDiscriminatorMapping.getDatastoreMapping(i)).getUpdateInputParameter());
                }
            }

            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForElement(stmt, elementMapping, element, elementsAreSerialised,
                null, true);

            if (elementMapping instanceof ReferenceMapping && elementMapping.getNumberOfDatastoreMappings() > 1)
            {
                // Don't cache since depends on the element
                return stmt.toString();
            }
            updateFkStmt = stmt.toString();
        }

        return updateFkStmt;
    }

    /**
     * Generates the statement for clearing items by nulling the owner link out. The statement will be
     * <PRE>
     * UPDATE LISTTABLE SET OWNERCOL=NULL, INDEXCOL=-1 [,DISTINGUISHER=NULL]
     * WHERE OWNERCOL=? [AND DISTINGUISHER=?]
     * </PRE>
     * when there is only one element table, and will be
     * <PRE>
     * UPDATE ? SET OWNERCOL=NULL, INDEXCOL=-1 [,DISTINGUISHER=NULL]
     * WHERE OWNERCOL=? [AND DISTINGUISHER=?]
     * </PRE>
     * when there is more than 1 element table.
     * @return The Statement for clearing items for the owner.
     */
    private String getClearNullifyStmt()
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
                // Could use elementInfo[0].getDatastoreClass but need to allow for relation in superclass table
                stmt.append(containerTable.toString());
            }
            stmt.append(" SET ");
            for (int i = 0; i < ownerMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(", ");
                }
                stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString() + "=NULL");
            }
            if (orderMapping != null)
            {
                for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(", ");
                    stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString() + "=-1");
                }
            }
            if (relationDiscriminatorMapping != null)
            {
                for (int i = 0; i < relationDiscriminatorMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(", ");
                    stmt.append(relationDiscriminatorMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append("=NULL");
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
     * Generates the statement for setting an item to be at a position.
     * <PRE>
     * UPDATE LISTTABLE SET OWNERCOL=?, INDEXCOL = ? [,DISTINGUISHER=?]
     * WHERE ELEMENTCOL = ?
     * </PRE>
     * @param element The element to set
     * @return The Statement for setting an item
     */
    private String getSetStmt(Object element)
    {
        if (setStmt == null)
        {
            StringBuffer stmt = new StringBuffer("UPDATE ");
            stmt.append(containerTable.toString()); // TODO Allow for multiple element tables
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

            if (orderMapping != null)
            {
                for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append(" = ");
                    stmt.append(((RDBMSMapping) orderMapping.getDatastoreMapping(i)).getUpdateInputParameter());
                }
            }
            if (relationDiscriminatorMapping != null)
            {
                for (int i = 0; i < relationDiscriminatorMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(relationDiscriminatorMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append(" = ");
                    stmt.append(((RDBMSMapping) relationDiscriminatorMapping.getDatastoreMapping(i)).getUpdateInputParameter());
                }
            }

            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForElement(stmt, elementMapping, element, isElementsAreSerialised(), null, true);

            if (elementMapping instanceof ReferenceMapping && elementMapping.getNumberOfDatastoreMappings() > 1)
            {
                // Don't cache since depends on the element
                return stmt.toString();
            }
            setStmt = stmt.toString();
        }

        return setStmt;
    }

    /**
     * Generates the statement for unsetting an item from a list position.
     * <PRE>
     * UPDATE LISTTABLE SET OWNERCOL=NULL, INDEXCOL=-1 [, DISTINGUISHER = NULL]
     * WHERE OWNERCOL = ? AND INDEXCOL = ? [AND DISTINGUISHER = ?]
     * </PRE>
     * @return The Statement for unsetting an item
     */
    private String getUnsetStmt()
    {
        if (unsetStmt == null)
        {
            StringBuffer stmt = new StringBuffer("UPDATE ");
            // TODO Allow for multiple element tables
            stmt.append(containerTable.toString());
            stmt.append(" SET ");
            for (int i = 0; i < ownerMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(",");
                }
                stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                stmt.append("=NULL");
            }

            if (orderMapping != null)
            {
                for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append("=-1");
                }
            }
            if (relationDiscriminatorMapping != null)
            {
                for (int i = 0; i < relationDiscriminatorMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(",");
                    stmt.append(relationDiscriminatorMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append(" = NULL");
                }
            }

            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            BackingStoreHelper.appendWhereClauseForMapping(stmt, orderMapping, null, false);
            if (relationDiscriminatorMapping != null)
            {
                BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
            }

            unsetStmt = stmt.toString();
        }
        return unsetStmt;
    }

    /**
     * Generates the statement for removing an item by nulling it out.
     * When there is only a single element table the statement will be
     * <PRE>
     * UPDATE LISTTABLE SET OWNERCOL=NULL, INDEXCOL=-1
     * WHERE OWNERCOL = ?
     * AND INDEXCOL = ?
     * [AND DISTINGUISHER = ?]
     * </PRE>
     * and when there are multiple element tables the statement will be
     * <PRE>
     * UPDATE ? SET OWNERCOL=NULL, INDEXCOL=-1
     * WHERE OWNERCOL=?
     * AND INDEXCOL=?
     * [AND DISTINGUISHER = ?]
     * </PRE>
     * @return The Statement for removing an item from a position
     */
    private String getRemoveAtNullifyStmt()
    {
        if (removeAtNullifyStmt == null)
        {
            StringBuffer stmt = new StringBuffer("UPDATE ");
            if (elementInfo.length > 1)
            {
                stmt.append("?");
            }
            else
            {
                // Could use elementInfo[0].getDatastoreClass but need to allow for relation in superclass table
                stmt.append(containerTable.toString());
            }
            stmt.append(" SET ");
            for (int i = 0; i < ownerMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(", ");
                }
                stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                stmt.append("=NULL");
            }
            if (orderMapping != null)
            {
                for (int i = 0; i < orderMapping.getNumberOfDatastoreMappings(); i++)
                {
                    stmt.append(", ");
                    stmt.append(orderMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                    stmt.append("=-1");
                }
            }

            stmt.append(" WHERE ");
            BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
            BackingStoreHelper.appendWhereClauseForMapping(stmt, orderMapping, null, false);
            if (relationDiscriminatorMapping != null)
            {
                BackingStoreHelper.appendWhereClauseForMapping(stmt, relationDiscriminatorMapping, null, false);
            }

            removeAtNullifyStmt = stmt.toString();
        }
        return removeAtNullifyStmt;
    }
}