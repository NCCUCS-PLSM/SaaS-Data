/**********************************************************************
Copyright (c) 2002 Kelly Grizzle and others. All rights reserved.
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
2002 Mike Martin - unknown changes
2003 Andy Jefferson - added localiser
2003 Andy Jefferson - replaced TableMetadata with SQLIdentifier and java name
2004 Andy Jefferson - changed to extend TableImpl
2005 Andy Jefferson - moved discrim/version, owner, PK methods from subclasses
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.table;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.PrimaryKeyMetaData;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.key.PrimaryKey;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract class representing a field that maps to a table in the datastore.
 * This will be something like an SCO, such as Set, Map, List where a link table is used.
 * It could potentially be used where the user wants to map some field into its own SCO table.
 */
public abstract class JoinTable extends TableImpl implements DatastoreContainerObject
{
    /** MetaData for the field/property in the owner class. */
    protected final AbstractMemberMetaData mmd;

    /** Mapping of owner column(s) back to the owner table PK. */
    protected JavaTypeMapping ownerMapping;

    /** Object type of the owner. */
    protected final String ownerType;

    /**
     * Constructor.
     * @param tableName The Table SQL identifier
     * @param mmd Field meta data for the owner field/property
     * @param storeMgr Manager for the datastore.
     **/
    protected JoinTable(DatastoreIdentifier tableName, AbstractMemberMetaData mmd, RDBMSStoreManager storeMgr)
    {
        super(tableName, storeMgr);

        this.mmd = mmd;
        this.ownerType = mmd.getClassName(true);

        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.NONE)
        {
            throw new NucleusException(LOCALISER.msg("057006", mmd.getName())).setFatal();
        }
    }

    /**
     * Accessor for the primary key for this table. Overrides the method in TableImpl
     * to add on any specification of PK name in the &lt;join&gt; metadata.
     * @return The primary key.
     */
    public PrimaryKey getPrimaryKey()
    {
        PrimaryKey pk = super.getPrimaryKey();
        if (mmd.getJoinMetaData() != null)
        {
            PrimaryKeyMetaData pkmd = mmd.getJoinMetaData().getPrimaryKeyMetaData();
            if (pkmd != null && pkmd.getName() != null)
            {
                pk.setName(pkmd.getName());
            }
        }

        return pk;
    }

    /**
     * Convenience method for whether a PK is required for the join table.
     * Makes use of the extension "primary-key" (within &lt;join&gt;) to allow turning off PK generation.
     * @return Whether a PK is required
     */
    protected boolean requiresPrimaryKey()
    {
        boolean pkRequired = true;
        if (mmd.getJoinMetaData() != null && mmd.getJoinMetaData().hasExtension("primary-key") &&
            mmd.getJoinMetaData().getValueForExtension("primary-key").equalsIgnoreCase("false"))
        {
            pkRequired = false;
        }
        return pkRequired;
    }

    /**
     * Accessor for the "owner" mapping end of the relationship. This will be
     * the primary key of the owner table.
     * @return The column mapping for the owner.
     */
    public JavaTypeMapping getOwnerMapping()
    {
        assertIsInitialized();
        return ownerMapping;
    }

    /**
     * Accessor for the MetaData for the owner field/property for this container.
     * @return metadata for the owning field/property
     */
    public AbstractMemberMetaData getOwnerMemberMetaData()
    {
        return mmd;
    }

    /**
     * Accessor for a mapping for the ID (PersistenceCapable) for this table.
     * This is not supported by join tables since they don't represent FCOs.
     * @return The (PersistenceCapable) ID mapping.
     */
    public JavaTypeMapping getIdMapping()
    {
        throw new NucleusException("Unsupported ID mapping in join table").setFatal();
    }

    /**
     * Convenience logging method to output the mapping information for an element, key, value field
     * @param mapping The mapping
     */
    protected void debugMapping(JavaTypeMapping mapping)
    {
        if (NucleusLogger.DATASTORE.isDebugEnabled())
        {
            // Provide field->column mapping debug message
            StringBuffer columnsStr = new StringBuffer();
            for (int i=0;i<mapping.getNumberOfDatastoreMappings();i++)
            {
                if (i > 0)
                {
                    columnsStr.append(",");
                }
                columnsStr.append(mapping.getDatastoreMapping(i).getDatastoreField());
            }
            if (mapping.getNumberOfDatastoreMappings() == 0)
            {
                columnsStr.append("[none]");
            }
            StringBuffer datastoreMappingTypes = new StringBuffer();
            for (int i=0;i<mapping.getNumberOfDatastoreMappings();i++)
            {
                if (i > 0)
                {
                    datastoreMappingTypes.append(',');
                }
                datastoreMappingTypes.append(mapping.getDatastoreMapping(i).getClass().getName());
            }
            NucleusLogger.DATASTORE.debug(LOCALISER.msg("057010",
                mmd.getFullFieldName(), columnsStr.toString(), mapping.getClass().getName(), datastoreMappingTypes.toString()));
        }
    }
}