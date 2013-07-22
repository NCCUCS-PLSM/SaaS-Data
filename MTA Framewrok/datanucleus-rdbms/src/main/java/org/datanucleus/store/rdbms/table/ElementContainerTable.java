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
package org.datanucleus.store.rdbms.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.ElementMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.ForeignKeyMetaData;
import org.datanucleus.metadata.IndexMetaData;
import org.datanucleus.metadata.PrimaryKeyMetaData;
import org.datanucleus.metadata.UniqueMetaData;
import org.datanucleus.store.exceptions.NoTableManagedException;
import org.datanucleus.store.mapped.ColumnCreator;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.IdentifierFactory;
import org.datanucleus.store.mapped.mapping.EmbeddedElementPCMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.PersistableMapping;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.mapped.mapping.SerialisedPCMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.key.CandidateKey;
import org.datanucleus.store.rdbms.key.ForeignKey;
import org.datanucleus.store.rdbms.key.Index;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * Representation of a join table for a container of elements.
 * Can be used for collections, lists, sets and arrays.
 * There can be multiple JoinTable objects referring to the same underlying datastore
 * object. If the JoinTable is shared by multiple fields for example then there will
 * be one for each relation.
 */
public abstract class ElementContainerTable extends JoinTable
{
    /**
     * Mapping of an element. This is either a PersistenceCapableMapping to the element table,
     * or an EmbeddedElementPCMapping (when PC elements are embedded), or a simple mapping (when
     * using non-PC elements), or a SerialisedPCMapping, or a SerialisedReferenceMapping.
     * It will be specified in the MetaData using the &lt;element&gt; tag.
     */
    protected JavaTypeMapping elementMapping;

    /**
     * Order mapping, to provide part of the primary key.
     * In the case of a List this represents the ordering index. In the case of a Set
     * this represents an index for allowing duplicates, or where the element is embedded and
     * is of a type that can't be part of the PK. 
     * It will be specified in the MetaData using the &lt;order&gt; tag.
     */
    protected JavaTypeMapping orderMapping;

    /**
     * Optional mapping for a column used to discriminate between elements of one collection from another.
     * Used where the join table is being shared by more than 1 relation.
     * Specified using the metadata extension "relation-discriminator-column" in the "field" element.
     */
    protected JavaTypeMapping relationDiscriminatorMapping;

    /**
     * Value to use with any relation discriminator column for objects of this field placed in the join table.
     * Specified using the metadata extension "relation-discriminator-value" in the "field" element.
     */
    protected String relationDiscriminatorValue;

    /**
     * Constructor.
     * @param tableName Identifier name of the table
     * @param mmd MetaData for the field/property owning this join table
     * @param storeMgr The Store Manager managing these tables.
     */
    public ElementContainerTable(DatastoreIdentifier tableName, AbstractMemberMetaData mmd, RDBMSStoreManager storeMgr)
    {
        super(tableName, mmd, storeMgr);
    }

    /**
     * Method to initialise the table definition. Adds the owner mapping.
     * @param clr The ClassLoaderResolver
     */
    public void initialize(ClassLoaderResolver clr)
    {
        assertIsUninitialized();

        boolean pkRequired = requiresPrimaryKey();

        // Add owner mapping
        AbstractMemberMetaData[] relatedMmds = mmd.getRelatedMemberMetaData(clr);
        ColumnMetaData[] columnMetaData = null;
        if (mmd.getJoinMetaData() != null &&
            mmd.getJoinMetaData().getColumnMetaData() != null &&
            mmd.getJoinMetaData().getColumnMetaData().length > 0)
        {
            // Column mappings defined at this side (1-N, M-N)
            // When specified at this side they use the <join> tag
            columnMetaData = mmd.getJoinMetaData().getColumnMetaData();
        }
        else if (relatedMmds != null && relatedMmds[0].getElementMetaData() != null &&
            relatedMmds[0].getElementMetaData().getColumnMetaData() != null &&
            relatedMmds[0].getElementMetaData().getColumnMetaData().length > 0)
        {
            // Column mappings defined at other side (M-N)
            // When specified at other side they use the <element> tag
            // ** This is really only for Collections/Sets since M-N doesnt make sense for Lists/arrays **
            columnMetaData = relatedMmds[0].getElementMetaData().getColumnMetaData();
        }
        ownerMapping = ColumnCreator.createColumnsForJoinTables(clr.classForName(ownerType), mmd, columnMetaData, 
            storeMgr, this, pkRequired, false, FieldRole.ROLE_OWNER, clr);
        if (NucleusLogger.DATASTORE.isDebugEnabled())
        {
            debugMapping(ownerMapping);
        }

        // Add any distinguisher column
        if (mmd.hasExtension("relation-discriminator-column") || mmd.hasExtension("relation-discriminator-value"))
        {
            // Generate some columnMetaData for our new column
            String colName = mmd.getValueForExtension("relation-discriminator-column");
            if (colName == null)
            {
                // No column defined so use a fallback name
                colName = "RELATION_DISCRIM";
            }
            ColumnMetaData colmd = new ColumnMetaData();
            colmd.setName(colName);

            boolean relationDiscriminatorPk = false;
            if (mmd.hasExtension("relation-discriminator-pk") &&
                mmd.getValueForExtension("relation-discriminator-pk").equalsIgnoreCase("true"))
            {
                // Default this to not be part of the PK of the join table, but allow the user to override it
                relationDiscriminatorPk = true;
            }
            if (!relationDiscriminatorPk)
            {
                colmd.setAllowsNull(Boolean.TRUE); // Allow for elements not in any discriminated collection (when not PK)
            }

            // Create the mapping and its datastore column (only support String relation discriminators here)
            relationDiscriminatorMapping = storeMgr.getMappingManager().getMapping(String.class);
            ColumnCreator.createIndexColumn(relationDiscriminatorMapping, storeMgr, clr, this, colmd, relationDiscriminatorPk);

            relationDiscriminatorValue = mmd.getValueForExtension("relation-discriminator-value");
            if (relationDiscriminatorValue == null)
            {
                // No value defined so just use the field name
                relationDiscriminatorValue = mmd.getFullFieldName();
            }
        }
    }

    /**
     * Access the element type class name
     * @return the element type class name
     */
    public abstract String getElementType();

    /**
     * Convenience method to apply the user specification of <primary-key> columns
     * @param pkmd MetaData for the primary key
     */
    protected void applyUserPrimaryKeySpecification(PrimaryKeyMetaData pkmd)
    {
        ColumnMetaData[] pkCols = pkmd.getColumnMetaData();
        for (int i=0;i<pkCols.length;i++)
        {
            String colName = pkCols[i].getName();
            boolean found = false;
            for (int j=0;j<ownerMapping.getNumberOfDatastoreMappings();j++)
            {
                if (ownerMapping.getDatastoreMapping(j).getDatastoreField().getIdentifier().getIdentifierName().equals(colName))
                {
                    ownerMapping.getDatastoreMapping(j).getDatastoreField().setAsPrimaryKey();
                    found = true;
                }
            }

            if (!found)
            {
                for (int j=0;j<elementMapping.getNumberOfDatastoreMappings();j++)
                {
                    if (elementMapping.getDatastoreMapping(j).getDatastoreField().getIdentifier().getIdentifierName().equals(colName))
                    {
                        elementMapping.getDatastoreMapping(j).getDatastoreField().setAsPrimaryKey();
                        found = true;
                    }
                }
            }

            if (!found)
            {
                throw new NucleusUserException(LOCALISER.msg("057040", toString(), colName));
            }
        }
    }

    /**
     * Accessor not used by this table.
     * @param mmd MetaData for the field whose mapping we want
     * @return The mapping
     */
    public JavaTypeMapping getMemberMapping(AbstractMemberMetaData mmd)
    {
        return null;
    }

    /**
     * Accessor for the "element" mapping end of the relationship. 
     * This is used where the element is PersistenceCapable and has its own table (not embedded),
     * or where the element is a simple type.
     * @return The column mapping for the element.
     */
    public JavaTypeMapping getElementMapping()
    {
        assertIsInitialized();
        return elementMapping;
    }

    /**
     * Accessor for the order mapping.
     * The columns in this mapping are part of the primary key.
     * @return Order mapping (where required)
     */
    public JavaTypeMapping getOrderMapping()
    {
        assertIsInitialized();
        return orderMapping;
    }

    /**
     * Accessor for the element discriminator mapping.
     * @return Element discriminator mapping (where required)
     */
    public JavaTypeMapping getRelationDiscriminatorMapping()
    {
        assertIsInitialized();
        return relationDiscriminatorMapping;
    }

    /**
     * Accessor for the element discriminator value.
     * @return Element discriminator value (where required)
     */
    public String getRelationDiscriminatorValue()
    {
        assertIsInitialized();
        return relationDiscriminatorValue;
    }

    /**
     * Convenience method to generate a ForeignKey from this join table to an owner table.
     * @param ownerTable The owner table
     * @param autoMode Whether we are in auto mode (where we generate the keys regardless of what the metadata says)
     * @return The ForeignKey
     */
    protected ForeignKey getForeignKeyToOwner(DatastoreClass ownerTable, boolean autoMode)
    {
        ForeignKey fk = null;
        if (ownerTable != null)
        {
            // Take <foreign-key> from <join>
            ForeignKeyMetaData fkmd = null;
            if (mmd.getJoinMetaData() != null)
            {
                fkmd = mmd.getJoinMetaData().getForeignKeyMetaData();
            }
            // TODO If in autoMode and there are multiple possible owner tables then don't create a FK
            if (fkmd != null || autoMode)
            {
                fk = new ForeignKey(ownerMapping, dba, ownerTable, true);
                fk.setForMetaData(fkmd);
            }
        }
        return fk;
    }

    /**
     * Convenience method to generate a ForeignKey from this join table to an element table
     * using the specified mapping.
     * @param elementTable The element table
     * @param autoMode Whether we are in auto mode (where we generate the keys regardless of what the metadata says)
     * @param m The mapping to the element table
     * @return The ForeignKey
     */
    protected ForeignKey getForeignKeyToElement(DatastoreClass elementTable, boolean autoMode, JavaTypeMapping m)
    {
        ForeignKey fk = null;
        if (elementTable != null)
        {
            // Take <foreign-key> from either <field> or <element>
            ForeignKeyMetaData fkmd = mmd.getForeignKeyMetaData();
            if (fkmd == null && mmd.getElementMetaData() != null)
            {
                fkmd = mmd.getElementMetaData().getForeignKeyMetaData();
            }
            // TODO If in autoMode and there are multiple possible element tables then don't create a FK
            if (fkmd != null || autoMode)
            {
                fk = new ForeignKey(m, dba, elementTable, true);
                fk.setForMetaData(fkmd);
            }
        }
        return fk;
    }


    /**
     * Accessor for the expected foreign keys for this table.
     * @param clr The ClassLoaderResolver
     * @return The expected foreign keys.
     */
    public List getExpectedForeignKeys(ClassLoaderResolver clr)
    {
        assertIsInitialized();

        // Find the mode that we're operating in for FK addition
        boolean autoMode = false;
        if (storeMgr.getStringProperty("datanucleus.rdbms.constraintCreateMode").equals("DataNucleus"))
        {
            autoMode = true;
        }

        ArrayList foreignKeys = new ArrayList();
        try
        {
            // FK from join table to owner table
            DatastoreClass referencedTable = storeMgr.getDatastoreClass(ownerType, clr);
            if (referencedTable != null)
            {
                // Single owner table, so add a single FK to the owner as appropriate
                ForeignKey fk = getForeignKeyToOwner(referencedTable, autoMode);
                if (fk != null)
                {
                    foreignKeys.add(fk);
                }
            }
            else
            {
                // No single owner so we don't bother with the FK since referential integrity by FK cannot work
                // if we don't have a single owner at the other end of the FK(s).
            }

            // FK from join table to element table(s)
            if (elementMapping instanceof SerialisedPCMapping)
            {
                // Do nothing since no element table
            }
            else if (elementMapping instanceof EmbeddedElementPCMapping)
            {
                // Add any FKs for the fields of the (embedded) element
                EmbeddedElementPCMapping embMapping = (EmbeddedElementPCMapping)elementMapping;
                for (int i=0;i<embMapping.getNumberOfJavaTypeMappings();i++)
                {
                    JavaTypeMapping embFieldMapping = embMapping.getJavaTypeMapping(i);
                    AbstractMemberMetaData embFmd = embFieldMapping.getMemberMetaData();
                    if (ClassUtils.isReferenceType(embFmd.getType()) && 
                        embFieldMapping instanceof ReferenceMapping)
                    {
                        // Field is a reference type, so add a FK to the table of the PC for each PC implementation
                        Collection fks = TableUtils.getForeignKeysForReferenceField(embFieldMapping, embFmd, autoMode, storeMgr, clr);
                        foreignKeys.addAll(fks);
                    }
                    else if (storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(embFmd.getType(), clr) != null &&
                            embFieldMapping.getNumberOfDatastoreMappings() > 0 &&
                            embFieldMapping instanceof PersistableMapping)
                    {
                        // Field is for a PC class with the FK at this side, so add a FK to the table of this PC
                        ForeignKey fk = TableUtils.getForeignKeyForPCField(embFieldMapping, embFmd, autoMode, storeMgr, clr);
                        if (fk != null)
                        {
                            foreignKeys.add(fk);
                        }
                    }
                }
            }
            else if (elementMapping instanceof ReferenceMapping)
            {
                JavaTypeMapping[] implJavaTypeMappings = ((ReferenceMapping)elementMapping).getJavaTypeMapping();
                for (int i=0;i<implJavaTypeMappings.length;i++)
                {
                    JavaTypeMapping implMapping = implJavaTypeMappings[i];
                    if (storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(implMapping.getType(), clr) != null &&
                        implMapping.getNumberOfDatastoreMappings() > 0)
                    {
                        referencedTable = storeMgr.getDatastoreClass(implMapping.getType(), clr);
                        if (referencedTable != null)
                        {
                            ForeignKey fk = getForeignKeyToElement(referencedTable, autoMode, implMapping);
                            if (fk != null)
                            {
                                foreignKeys.add(fk);
                            }
                        }
                    }
                }
            }
            else
            {
                referencedTable = storeMgr.getDatastoreClass(getElementType(), clr);
                if (referencedTable != null)
                {
                    ForeignKey fk = getForeignKeyToElement(referencedTable, autoMode, elementMapping);
                    if (fk != null)
                    {
                        foreignKeys.add(fk);
                    }
                }
                else
                {
                    // Either no element table or multiple (where the user has element with "subclass-table" strategy)
                    // so do nothing since referential integrity will not allow multiple FKs.
                }
            }
        }
        catch (NoTableManagedException e)
        {
            // expected when no table exists
        }
        return foreignKeys;
    }

    /**
     * Accessor for the indices for this table. 
     * This includes both the user-defined indices (via MetaData), and the ones required by 
     * foreign keys (required by relationships).
     * @param clr The ClassLoaderResolver
     * @return The indices
     */
    protected Set getExpectedIndices(ClassLoaderResolver clr)
    {
        // The indices required by foreign keys (BaseTable)
        Set indices = super.getExpectedIndices(clr);

        // Check owner for specified indices
        if (mmd.getIndexMetaData() != null)
        {
            Index index = TableUtils.getIndexForField(this, mmd.getIndexMetaData(), ownerMapping);
            if (index != null)
            {
                indices.add(index);
            }
        }
        else if (mmd.getJoinMetaData() != null && mmd.getJoinMetaData().getIndexMetaData() != null)
        {
            Index index = TableUtils.getIndexForField(this, mmd.getJoinMetaData().getIndexMetaData(), ownerMapping);
            if (index != null)
            {
                indices.add(index);
            }
        }

        // Check element for specified indices
        if (elementMapping instanceof EmbeddedElementPCMapping)
        {
            // Add all indices required by fields of the embedded element
            EmbeddedElementPCMapping embMapping = (EmbeddedElementPCMapping)elementMapping;
            for (int i=0;i<embMapping.getNumberOfJavaTypeMappings();i++)
            {
                // Add indexes for fields of this embedded PC object
                JavaTypeMapping embFieldMapping = embMapping.getJavaTypeMapping(i);
                IndexMetaData imd = embFieldMapping.getMemberMetaData().getIndexMetaData();
                if (imd != null)
                {
                    Index index = TableUtils.getIndexForField(this, imd, embFieldMapping);
                    if (index != null)
                    {
                        indices.add(index);
                    }
                }
            }
        }
        else
        {
            ElementMetaData elemmd = mmd.getElementMetaData();
            if (elemmd != null && elemmd.getIndexMetaData() != null)
            {
                Index index = TableUtils.getIndexForField(this, elemmd.getIndexMetaData(), elementMapping);
                if (index != null)
                {
                    indices.add(index);
                }
            }
        }

        // Check order for specified indices
        if (orderMapping != null)
        {
            if (mmd.getOrderMetaData() != null && mmd.getOrderMetaData().getIndexMetaData() != null)
            {
                Index index = TableUtils.getIndexForField(this, mmd.getOrderMetaData().getIndexMetaData(), orderMapping);
                if (index != null)
                {
                    indices.add(index);
                }
            }
        }

        return indices;
    }

    /**
     * Accessor for the candidate keys for this table.
     * @return The indices
     */
    protected List getExpectedCandidateKeys()
    {
        // The indices required by foreign keys (BaseTable)
        List candidateKeys = super.getExpectedCandidateKeys();

        if (elementMapping instanceof EmbeddedElementPCMapping)
        {
            // Add all candidate keys required by fields of the embedded element
            EmbeddedElementPCMapping embMapping = (EmbeddedElementPCMapping)elementMapping;
            for (int i=0;i<embMapping.getNumberOfJavaTypeMappings();i++)
            {
                JavaTypeMapping embFieldMapping = embMapping.getJavaTypeMapping(i);
                UniqueMetaData umd = embFieldMapping.getMemberMetaData().getUniqueMetaData();
                if (umd != null)
                {
                    CandidateKey ck = TableUtils.getCandidateKeyForField(this, umd, embFieldMapping);
                    if (ck != null)
                    {
                        candidateKeys.add(ck);
                    }
                }
            }
        }

        if (mmd.getJoinMetaData() != null && mmd.getJoinMetaData().getUniqueMetaData() != null)
        {
            // User has defined a unique key on the join table
            UniqueMetaData unimd = mmd.getJoinMetaData().getUniqueMetaData();
            ColumnMetaData[] colmds = unimd.getColumnMetaData();
            if (colmds != null)
            {
                CandidateKey uniKey = new CandidateKey(this);
                IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
                for (int i=0;i<colmds.length;i++)
                {
                    DatastoreField col = getDatastoreField(idFactory.newDatastoreFieldIdentifier(colmds[i].getName()));
                    if (col != null)
                    {
                        uniKey.addDatastoreField(col);
                    }
                    else
                    {
                        throw new NucleusUserException("Unique key on join-table " + this + " has column " +
                            colmds[i].getName() + " that is not found");
                    }
                }
                candidateKeys.add(uniKey);
            }
        }

        return candidateKeys;
    }
}