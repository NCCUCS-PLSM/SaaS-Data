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
package org.datanucleus.store.rdbms.fieldmanager;

import java.util.Collection;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.mapping.InterfaceMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.table.CollectionTable;
import org.datanucleus.store.rdbms.table.TableImpl;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Field manager that is used to check the values in fields in order to detect "new" classes
 * that impact on the datastore schema, hence allowing dynamic schema updates.
 */
public class DynamicSchemaFieldManager extends AbstractFieldManager
{
    /** Localiser for internationalisation. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Manager for the RDBMS datastore. */
    RDBMSStoreManager rdbmsMgr;

    /** StateManager of the object being processed. */
    ObjectProvider sm;

    /** Flag for whether we have updated the schema. */
    boolean schemaUpdatesPerformed = false;

    /**
     * Constructor.
     * @param rdbmsMgr RDBMSManager
     * @param sm StateManager for the object being processed
     */
    public DynamicSchemaFieldManager(RDBMSStoreManager rdbmsMgr, ObjectProvider sm)
    {
        this.rdbmsMgr = rdbmsMgr;
        this.sm = sm;
    }

    /**
     * Accessor for whether this field manager has made updates to the schema.
     * @return Whether updates have been made.
     */
    public boolean hasPerformedSchemaUpdates()
    {
        return schemaUpdatesPerformed;
    }

    /**
     * Method to store an object field into the attached instance.
     * @param fieldNumber Number of the field to store
     * @param value the value in the detached instance
     */
    public void storeObjectField(int fieldNumber, Object value)
    {
        if (value == null)
        {
            return; // No value so nothing to do
        }

        ExecutionContext ec = sm.getExecutionContext();
        ClassLoaderResolver clr = ec.getClassLoaderResolver();

        AbstractMemberMetaData mmd = 
            sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        DatastoreClass table = rdbmsMgr.getDatastoreClass(sm.getObject().getClass().getName(), clr);
        JavaTypeMapping fieldMapping = table.getMemberMapping(mmd);
        if (fieldMapping != null)
        {
            if (fieldMapping instanceof InterfaceMapping)
            {
                // 1-1 Interface field
                InterfaceMapping intfMapping = (InterfaceMapping)fieldMapping;
                if (mmd != null)
                {
                    if (mmd.getFieldTypes() != null || mmd.hasExtension("implementation-classes"))
                    {
                        // Field is defined to not accept this type so just return
                        return;
                    }
                }

                processInterfaceMappingForValue(intfMapping, value, mmd, ec);
            }
            else if (mmd.hasCollection())
            {
                boolean hasJoin = false;
                if (mmd.getJoinMetaData() != null)
                {
                    hasJoin = true;
                }
                else
                {
                    AbstractMemberMetaData[] relMmds = mmd.getRelatedMemberMetaData(clr);
                    if (relMmds != null && relMmds[0].getJoinMetaData() != null)
                    {
                        hasJoin = true;
                    }
                }
                if (!hasJoin)
                {
                    // Not join table so no supported schema updates
                    return;
                }

                Collection coll = (Collection)value;
                if (coll == null || coll.isEmpty())
                {
                    return;
                }

                DatastoreContainerObject joinTbl = fieldMapping.getStoreManager().getDatastoreContainerObject(mmd);
                CollectionTable collTbl = (CollectionTable)joinTbl;
                JavaTypeMapping elemMapping = collTbl.getElementMapping();
                if (elemMapping instanceof InterfaceMapping)
                {
                    InterfaceMapping intfMapping = (InterfaceMapping)elemMapping;
                    processInterfaceMappingForValue(intfMapping, coll.iterator().next(), mmd, ec);
                }
            }
            else if (mmd.hasMap())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug("TODO : Support dynamic schema updates for Map field " + mmd.getFullFieldName());
                // TODO Cater for Map<NonPC, Interface>
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeBooleanField(int, boolean)
     */
    public void storeBooleanField(int fieldNumber, boolean value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeByteField(int, byte)
     */
    public void storeByteField(int fieldNumber, byte value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeCharField(int, char)
     */
    public void storeCharField(int fieldNumber, char value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeDoubleField(int, double)
     */
    public void storeDoubleField(int fieldNumber, double value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeFloatField(int, float)
     */
    public void storeFloatField(int fieldNumber, float value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeIntField(int, int)
     */
    public void storeIntField(int fieldNumber, int value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeLongField(int, long)
     */
    public void storeLongField(int fieldNumber, long value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeShortField(int, short)
     */
    public void storeShortField(int fieldNumber, short value)
    {
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeStringField(int, java.lang.String)
     */
    public void storeStringField(int fieldNumber, String value)
    {
    }

    protected void processInterfaceMappingForValue(InterfaceMapping intfMapping, Object value,
            AbstractMemberMetaData mmd, ExecutionContext ec)
    {
        if (intfMapping.getMappingStrategy() == ReferenceMapping.PER_IMPLEMENTATION_MAPPING)
        {
            int intfImplMappingNumber = intfMapping.getMappingNumberForValue(ec, value);
            if (intfImplMappingNumber == -1)
            {
                if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE_SCHEMA.debug("Dynamic schema updates : field=" + mmd.getFullFieldName() + 
                        " has an interface mapping yet " + StringUtils.toJVMIDString(value) + 
                    " is not a known implementation - trying to update the schema ...");
                }

                // Make sure the metadata for this value class is loaded (may be first encounter)
                MetaDataManager mmgr = ec.getNucleusContext().getMetaDataManager();
                ClassLoaderResolver clr = ec.getClassLoaderResolver();
                mmgr.getMetaDataForClass(value.getClass(), clr);

                String[] impls = ec.getMetaDataManager().getClassesImplementingInterface(intfMapping.getType(), clr);
                if (ClassUtils.stringArrayContainsValue(impls, value.getClass().getName()))
                {
                    // Value is a valid implementation yet there is no mapping so re-initialize the mapping
                    try
                    {
                        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
                        {
                            NucleusLogger.DATASTORE_SCHEMA.debug("Dynamic schema updates : field=" + 
                                mmd.getFullFieldName() + " has a new implementation available so reinitialising its mapping");
                        }
                        intfMapping.initialize(mmd, intfMapping.getDatastoreContainer(), clr);
                        ((RDBMSStoreManager)intfMapping.getStoreManager()).validateTable((TableImpl)intfMapping.getDatastoreContainer(), clr);
                    }
                    catch (Exception e)
                    {
                        NucleusLogger.DATASTORE_SCHEMA.debug("Exception thrown trying to create missing columns for implementation", e);
                        throw new NucleusException("Exception thrown performing dynamic update of schema", e);
                    }
                    schemaUpdatesPerformed = true;
                }
            }
        }
    }
}