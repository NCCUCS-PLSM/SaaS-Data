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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.ObjectProviderFactory;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.mapping.InterfaceMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.OIDMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.util.Localiser;

/**
 * Base class for all mapped container stores (collections, maps, arrays).
 * Provides a series of helper methods for handling the store process.
 */
public abstract class BaseContainerStore
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Manager for the store. */
    protected RDBMSStoreManager storeMgr;

    /** Datastore adapter in use by this store. */
    protected DatastoreAdapter dba;

    /** Mapping to the owner of the container. */
    protected JavaTypeMapping ownerMapping;

    /** MetaData for the field/property in the owner with this container. */
    protected AbstractMemberMetaData ownerMemberMetaData;

    /** Type of relation (1-N uni, 1-N bi, M-N). */
    protected int relationType;

    /** Whether the container allows null elements/values. */
    protected boolean allowNulls = false;

    /**
     * Constructor.
     * @param storeMgr Manager for the datastore being used
     */
    protected BaseContainerStore(RDBMSStoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
        this.dba = this.storeMgr.getDatastoreAdapter();
    }

    /**
     * Method to set the owner for this backing store.
     * Sets the metadata of the owner field/property, as well as whether nulls are allowed, and
     * the relation type.
     * @param mmd MetaData for the member owning this backing store.
     * @param clr ClassLoader resolver
     */
    protected void setOwner(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        this.ownerMemberMetaData = mmd;
        if (Boolean.TRUE.equals(ownerMemberMetaData.getContainer().allowNulls()))
        {
            // User has requested allowing nulls in this field/property
            allowNulls = true;
        }
        this.relationType = ownerMemberMetaData.getRelationType(clr);
    }

    /**
     * Accessor for the StoreManager.
     * @return The StoreManager.
     */
    public RDBMSStoreManager getStoreManager()
    {
        return storeMgr;
    }

    /**
     * Accessor for the owner mapping.
     * @return Owner mapping.
     */
    public JavaTypeMapping getOwnerMapping()
    {
        return ownerMapping;
    }

    /**
     * Check if the mapping correspond to a non pc object or embedded field
     * @param mapping the mapping
     * @return true if the field is embedded into one column
     */
    protected boolean isEmbeddedMapping(JavaTypeMapping mapping)
    {
        return !InterfaceMapping.class.isAssignableFrom(mapping.getClass()) &&
               !OIDMapping.class.isAssignableFrom(mapping.getClass());
    }

    /**
     * Method to return the StateManager for an embedded PC object (element, key, value).
     * It creates one if the element is not currently managed.
     * @param sm State Manager of the owner
     * @param obj The embedded PC object
     * @param ownerMmd The meta data for the owner field
     * @param pcType Object type for the embedded object (see ObjectProvider EMBEDDED_PC etc)
     * @return The state manager
     */
    public ObjectProvider getStateManagerForEmbeddedPCObject(ObjectProvider sm, Object obj, 
            AbstractMemberMetaData ownerMmd, short pcType)
    {
        ExecutionContext ec = sm.getExecutionContext();
        ObjectProvider objSM = ec.findObjectProvider(obj);
        if (objSM == null)
        {
            objSM = ObjectProviderFactory.newForEmbedded(ec, obj, false, sm, ownerMmd.getAbsoluteFieldNumber());
        }
        objSM.setPcObjectType(pcType);
        return objSM;
    }

    /**
     * Convenience method to return if the datastore supports batching and the user wants batching.
     * @return If batching of statements is permissible
     */
    protected boolean allowsBatching()
    {
        return storeMgr.allowsBatching();
    }

    public int getRelationType()
    {
        return relationType;
    }

    public AbstractMemberMetaData getOwnerMemberMetaData()
    {
        return ownerMemberMetaData;
    }

    public DatastoreAdapter getDatastoreAdapter()
    {
        return dba;
    }
}