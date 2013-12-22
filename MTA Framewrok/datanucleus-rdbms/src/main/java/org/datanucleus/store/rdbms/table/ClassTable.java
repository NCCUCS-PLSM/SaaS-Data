/**********************************************************************
Copyright (c) 2002 Kelly Grizzle (TJDO) and others. All rights reserved. 
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
2003 Andy Jefferson - added localiser
2003 Andy Jefferson - replaced TableMetadata with identifier.
2003 Erik Bengtson - refactored the datastore identity together with the SQLIdentifier class
2003 Erik Bengtson - added OptimisticMapping
2003 Andy Jefferson - coding standards
2004 Andy Jefferson - merged with JDOBaseTable
2004 Andy Jefferson - changed to store map of fieldMappings keyed by absolute field num. Added start point for inheritance strategy handling
2004 Andy Jefferson - changed consumer to set highest field number based on actual highest (to allow for inheritance strategies)
2004 Andy Jefferson - added DiscriminatorMapping
2004 Andy Jefferson - enabled use of Long/String datastore identity column
2004 Andy Jefferson - added capability to handle 1-N inverse unidirectional FKs
2004 Andy Jefferson - removed the majority of the value-strategy code - done elsewhere
    ...
 **********************************************************************/
package org.datanucleus.store.rdbms.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.ColumnMetaDataContainer;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.ExtensionMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.ForeignKeyAction;
import org.datanucleus.metadata.ForeignKeyMetaData;
import org.datanucleus.metadata.IdentityStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.IndexMetaData;
import org.datanucleus.metadata.InheritanceStrategy;
import org.datanucleus.metadata.InterfaceMetaData;
import org.datanucleus.metadata.JoinMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.OrderMetaData;
import org.datanucleus.metadata.PrimaryKeyMetaData;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.metadata.Relation;
import org.datanucleus.metadata.UniqueMetaData;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.exceptions.ClassDefinitionException;
import org.datanucleus.store.exceptions.NoSuchPersistentFieldException;
import org.datanucleus.store.exceptions.NoTableManagedException;
import org.datanucleus.store.mapped.ColumnCreator;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.IdentifierFactory;
import org.datanucleus.store.mapped.IdentifierType;
import org.datanucleus.store.mapped.exceptions.DuplicateDatastoreFieldException;
import org.datanucleus.store.mapped.mapping.CorrespondentColumnsMapper;
import org.datanucleus.store.mapped.mapping.DatastoreMapping;
import org.datanucleus.store.mapped.mapping.DiscriminatorMapping;
import org.datanucleus.store.mapped.mapping.EmbeddedPCMapping;
import org.datanucleus.store.mapped.mapping.IndexMapping;
import org.datanucleus.store.mapped.mapping.IntegerMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.LongMapping;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.mapped.mapping.MappingManager;
import org.datanucleus.store.mapped.mapping.PersistableMapping;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.mapped.mapping.SerialisedMapping;
import org.datanucleus.store.mapped.mapping.VersionLongMapping;
import org.datanucleus.store.mapped.mapping.VersionTimestampMapping;
import org.datanucleus.store.rdbms.JDBCUtils;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.key.CandidateKey;
import org.datanucleus.store.rdbms.key.ForeignKey;
import org.datanucleus.store.rdbms.key.Index;
import org.datanucleus.store.rdbms.key.PrimaryKey;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.types.sco.SCOUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.MacroString;
import org.datanucleus.util.NucleusLogger;

/**
 * Table representing a Java class (or classes) as a first class object (FCO).
 * Uses the inheritance strategy to control whether this represents multiple classes
 * or just the one class.
 * <H3>Mappings</H3>
 * This class adds some additional mappings over what the superclass provides. Here we add
 * <UL>
 * <LI><B>externalFkMappings</B> - any mappings for Collections that have no associated
 * field in this class, providing the foreign key column(s)</LI>
 * <LI><B>externalOrderMappings</B> - mappings for any ordering column used by Lists for ordering
 * elements of this class</LI>
 * <LI><B>externalFkDiscriminatorMappings</B> - mappings for any discriminator column used when sharing
 * external foreign keys to distinguish the element owner field</LI>
 * </UL>
 * <H3>Classes</H3>
 * A table can represent multiple classes. It has a nominal owner which is the class
 * that has an inheritance strategy of "new-table". All classes that utilise this table
 * have their MetaData stored in this object.
 * <H3>Secondary Tables</H3>
 * This class represents a "primary" table. That is, the main table where objects of a
 * class are persisted. It can have several "secondary" tables where some of the classes
 * fields are stored at persistence.
 */
public class ClassTable extends AbstractClassTable implements DatastoreClass 
{
    /**
     * MetaData for the principal class being stored here.
     * In inheritance situations where multiple classes share the same table, this will
     * be the class which uses "new-table" strategy.
     */
    private final ClassMetaData cmd;

    /** MetaData for all classes being managed here. */
    private final Collection<AbstractClassMetaData> managedClassMetaData = new HashSet();

    /** Callbacks that have been applied keyed by the managed class. */
    private final Map<String, Collection<AbstractMemberMetaData>> callbacksAppliedForManagedClass = new HashMap<String, Collection<AbstractMemberMetaData>>();

    /** Table above this table storing superclass information (if any). */
    private ClassTable supertable;

    /** Secondary tables for this table (if any). */
    private Map<String, SecondaryTable> secondaryTables;

    /**
     * Mappings for FK Collections/Lists not managed by this class (1-N unidirectional).
     * Keyed by the metadata of the owner field/property.
     */
    private HashMap<AbstractMemberMetaData, JavaTypeMapping> externalFkMappings;

    /**
     * Mappings for FK Collections/Lists relation discriminators not managed by this class (1-N unidirectional).
     * Keyed by the metadata of the owner field/property.
     */
    private HashMap<AbstractMemberMetaData, JavaTypeMapping> externalFkDiscriminatorMappings;

    /**
     * Mappings for FK Lists order columns not managed by this class (1-N unidirectional).
     * Keyed by the metadata of the owner field/property.
     */
    private HashMap<AbstractMemberMetaData, JavaTypeMapping> externalOrderMappings;

    /** User defined table schema **/
    private MacroString tableDef;

    /** DDL statement for creating the table, if using user defined table schema **/
    private String createStatementDDL;

    HashMap<AbstractMemberMetaData, CandidateKey> candidateKeysByMapField = new HashMap();

    /** Set of unmapped "Column" objects that have no associated field (and hence RDBMSMapping). */
    HashSet<Column> unmappedColumns = null;

    /**
     * Constructor.
     * @param tableName Table name SQL identifier
     * @param storeMgr Store Manager to manage this table
     * @param cmd MetaData for the class.
     */
    public ClassTable(DatastoreIdentifier tableName, RDBMSStoreManager storeMgr, ClassMetaData cmd)
    {
        super(tableName, storeMgr);

        this.cmd = cmd;

        // Check if this is a valid class to map to its own table
        if (cmd.getInheritanceMetaData().getStrategy() != InheritanceStrategy.NEW_TABLE &&
            cmd.getInheritanceMetaData().getStrategy() != InheritanceStrategy.COMPLETE_TABLE)
        {
            throw new NucleusUserException(LOCALISER.msg("057003", cmd.getFullClassName(),
                cmd.getInheritanceMetaData().getStrategy().toString())).setFatal();
        }

        highestMemberNumber = cmd.getNoOfManagedMembers() + cmd.getNoOfInheritedManagedMembers();
        
        // Extract the table definition from MetaData, if exists
        String tableImpStr = cmd.getValueForExtension("ddl-imports");
        String tableDefStr = null;
        if (dba.getVendorID() != null)
        {
            tableDefStr = cmd.getValueForExtension("ddl-definition" + '-' + dba.getVendorID());
        }
        if (tableDefStr == null)
        {
            tableDefStr = cmd.getValueForExtension("ddl-definition");
        }
        if (tableDefStr != null)
        {
            tableDef = new MacroString(cmd.getFullClassName(), tableImpStr, tableDefStr);
        }
    }

    /**
     * Pre-initialize.
     * We require any supertable, and the PK to be ready before we start initialisation.
     * @param clr the ClassLoaderResolver
     */
    public void preInitialize(final ClassLoaderResolver clr)
    {
        assertIsPKUninitialized();

        if (cmd.getInheritanceMetaData().getStrategy() != InheritanceStrategy.COMPLETE_TABLE)
        {
            // Inheritance strategy may imply having a supertable, so identify it
            supertable = getSupertable(cmd, clr);

            if (supertable != null && !supertable.isInitialized() && !supertable.isPKInitialized())
            {
                // Make sure that the supertable is preinitialised before we think about initialising here
                supertable.preInitialize(clr);
            }
        }

        // Initialise the PK field(s)
        if (!isPKInitialized())
        {
            initializePK(clr);
        }
    }

    /**
     * Method to initialise the table. 
     * This adds the columns based on the MetaData representation for the class being represented by this table.
     * @param clr The ClassLoaderResolver
     */
    public void initialize(ClassLoaderResolver clr)
    {
        assertIsUninitialized();

        // Add the fields for this class (and any other superclasses that we need to manage the
        // fields for (inheritance-strategy="subclass-table" in the superclass)
        initializeForClass(cmd, clr);

        MappingManager mapMgr = storeMgr.getMappingManager();
        // Add Version where specified in MetaData
        // TODO If there is a superclass table that has a version we should omit from here even if in MetaData
        // See "getTableWithDiscriminator()" for the logic
        versionMetaData = cmd.getVersionMetaDataForTable();
        if (versionMetaData != null && versionMetaData.getFieldName() == null)
        {
            if (versionMetaData.getVersionStrategy() == VersionStrategy.NONE)
            {
                // No optimistic locking but the idiot wants a column for that :-)
                versionMapping = new VersionLongMapping(dba, this, mapMgr.getMapping(Long.class));
            }
            else if (versionMetaData.getVersionStrategy() == VersionStrategy.VERSION_NUMBER)
            {
                versionMapping = new VersionLongMapping(dba, this, mapMgr.getMapping(Long.class));
            }
            else if (versionMetaData.getVersionStrategy() == VersionStrategy.DATE_TIME)
            {
                if (!dba.supportsOption(RDBMSAdapter.DATETIME_STORES_MILLISECS))
                {
                    // TODO Localise this
                    throw new NucleusException("Class " + cmd.getFullClassName() + " is defined " +
                        "to use date-time versioning, yet this datastore doesnt support storing " +
                        "milliseconds in DATETIME/TIMESTAMP columns. Use version-number");
                }
                versionMapping = new VersionTimestampMapping(dba, this, mapMgr.getMapping(Timestamp.class));
            }
        }

        // Add Discriminator where specified in MetaData
        DiscriminatorMetaData dismd = cmd.getDiscriminatorMetaDataForTable();
        if (dismd != null)
        {
            discriminatorMetaData = dismd;
            if (storeMgr.getBooleanProperty("datanucleus.rdbms.discriminatorPerSubclassTable"))
            {
                // Backwards compatibility only. Creates discriminator in all subclass tables even though not needed
                // TODO Remove this in the future
                discriminatorMapping = DiscriminatorMapping.createDiscriminatorMapping(this, dismd);
            }
            else
            {
                // Create discriminator column only in top most table that needs it
                ClassTable tableWithDiscrim = getTableWithDiscriminator();
                if (tableWithDiscrim == this)
                {
                    // No superclass with a discriminator so add it in this table
                    discriminatorMapping = DiscriminatorMapping.createDiscriminatorMapping(this, dismd);
                }
            }
        }

        // Add Multi-tenancy discriminator if applicable
        // TODO Only put on root table (i.e "if (supertable != null)" then omit)
        if (storeMgr.getStringProperty(PropertyNames.PROPERTY_TENANT_ID) != null)
        {
            if ("true".equalsIgnoreCase(cmd.getValueForExtension("multitenancy-disable")))
            {
                // Don't bother with multitenancy for this class
            }
            else
            {
                ColumnMetaData colmd = new ColumnMetaData();
                if (cmd.hasExtension("multitenancy-column-name"))
                {
                    colmd.setName(cmd.getValueForExtension("multitenancy-column-name"));
                }
                if (cmd.hasExtension("multitenancy-jdbc-type"))
                {
                    colmd.setJdbcType(cmd.getValueForExtension("multitenancy-jdbc-type"));
                }
                if (cmd.hasExtension("multitenancy-column-length"))
                {
                    colmd.setLength(cmd.getValueForExtension("multitenancy-column-length"));
                }
                addMultitenancyMapping(colmd);
            }
        }

        // Initialise any SecondaryTables
        if (secondaryTables != null)
        {
            Set secondaryTableNames = secondaryTables.keySet();
            Iterator secondaryTableNamesIter = secondaryTableNames.iterator();
            while (secondaryTableNamesIter.hasNext())
            {
                String secondaryTableName = (String)secondaryTableNamesIter.next();
                SecondaryTable second = secondaryTables.get(secondaryTableName);
                if (!second.isInitialized())
                {
                    second.initialize(clr);
                }
            }
        }

        state = TABLE_STATE_INITIALIZED;
    }
    
    /**
     * Post initilize. For things that must be set after all classes have been initialized before 
     * @param clr the ClassLoaderResolver
     */
    public void postInitialize(final ClassLoaderResolver clr)
    {
        assertIsInitialized();
        runCallBacks(clr);

        if (tableDef != null)
        {
            createStatementDDL = tableDef.substituteMacros(new MacroString.MacroHandler()
                {
                    public void onIdentifierMacro(MacroString.IdentifierMacro im)
                    {
                        storeMgr.resolveIdentifierMacro(im, clr);
                    }
    
                    public void onParameterMacro(MacroString.ParameterMacro pm)
                    {
                        throw new NucleusUserException(LOCALISER.msg("057033", cmd.getFullClassName(), pm));
                    }
                }, clr
            );
        }
    }

    /** Name of class currently being processed in manageClass (if any). */
    protected transient String managingClassCurrent = null;

    /** Flag to run the callbacks after the current class is managed fully. */
    protected boolean runCallbacksAfterManageClass = false;

    /**
     * Method that adds the specified class to be managed by this table.
     * Will provide mapping of all persistent fields to their underlying columns, map all necessary
     * identity fields, and manage all "unmapped" columns that have no associated field.
     * where the columns are defined for each mapping.
     * @param theCmd ClassMetaData for the class to be managed
     * @param clr The ClassLoaderResolver
     */
    public void manageClass(AbstractClassMetaData theCmd, ClassLoaderResolver clr)
    {
        if (NucleusLogger.DATASTORE.isDebugEnabled())
        {
            NucleusLogger.DATASTORE.debug(LOCALISER.msg("057024", toString(), theCmd.getFullClassName(), 
                theCmd.getInheritanceMetaData().getStrategy().toString()));
        }

        managingClassCurrent = theCmd.getFullClassName();
        managedClassMetaData.add(theCmd);

        // Manage all fields of this class and all fields of superclasses that this is overriding
        manageMembers(theCmd, clr, theCmd.getManagedMembers());
        manageMembers(theCmd, clr, theCmd.getOverriddenMembers());

        // Manage all "unmapped" columns (that have no field)
        manageUnmappedColumns(theCmd, clr);

        managingClassCurrent = null;
        if (runCallbacksAfterManageClass)
        {
            // We need to run the callbacks now that this class is fully managed
            runCallBacks(clr);
            runCallbacksAfterManageClass = false;
        }
    }

    /**
     * Accessor for the names of all classes managed by this table.
     * @return Names of the classes managed (stored) here
     */
    public String[] getManagedClasses()
    {
        String[] classNames = new String[managedClassMetaData.size()];
        Iterator<AbstractClassMetaData> iter = managedClassMetaData.iterator();
        int i = 0;
        while (iter.hasNext())
        {
            classNames[i++] = (iter.next()).getFullClassName();
        }
        return classNames;
    }

    /**
     * Goes through all specified members for the specified class and adds a mapping for each.
     * Ignores primary-key fields which are added elsewhere.
     * @param theCmd ClassMetaData for the class to be managed
     * @param clr The ClassLoaderResolver
     * @param mmds the fields/properties to manage
     */
    private void manageMembers(AbstractClassMetaData theCmd, ClassLoaderResolver clr, AbstractMemberMetaData[] mmds)
    {
        // Go through the fields for this class and add columns for them
        for (int fieldNumber=0; fieldNumber<mmds.length; fieldNumber++)
        {
            // Primary key fields are added by the initialisePK method
            AbstractMemberMetaData mmd = mmds[fieldNumber];
            if (!mmd.isPrimaryKey())
            {
                if (managesMember(mmd.getFullFieldName()))
                {
                    if (!mmd.getClassName(true).equals(theCmd.getFullClassName()))
                    {
                        // Field already managed by this table so maybe we are overriding a superclass
                        JavaTypeMapping fieldMapping = getMappingForMemberName(mmd.getFullFieldName());
                        ColumnMetaData[] colmds = mmd.getColumnMetaData();
                        if (colmds != null && colmds.length > 0)
                        {
                            // Apply this set of ColumnMetaData to the existing mapping
                            int colnum = 0;
                            IdentifierFactory idFactory = getStoreManager().getIdentifierFactory();
                            for (int i=0;i<fieldMapping.getNumberOfDatastoreMappings();i++)
                            {
                                Column col = (Column)fieldMapping.getDatastoreMapping(i).getDatastoreField();
                                col.setIdentifier(idFactory.newDatastoreFieldIdentifier(colmds[colnum].getName()));
                                col.setColumnMetaData(colmds[colnum]);

                                colnum++;
                                if (colnum == colmds.length)
                                {
                                    // Reached end of specified metadata
                                    break;
                                }
                            }
                            if (NucleusLogger.DATASTORE.isDebugEnabled())
                            {
                                // TODO Change this to reflect that we have updated the previous mapping
                                // Provide field->column mapping debug message
                                StringBuffer columnsStr = new StringBuffer();
                                for (int i=0;i<fieldMapping.getNumberOfDatastoreMappings();i++)
                                {
                                    if (i > 0)
                                    {
                                        columnsStr.append(",");
                                    }
                                    columnsStr.append(fieldMapping.getDatastoreMapping(i).getDatastoreField());
                                }
                                if (fieldMapping.getNumberOfDatastoreMappings() == 0)
                                {
                                    columnsStr.append("[none]");
                                }
                                StringBuffer datastoreMappingTypes = new StringBuffer();
                                for (int i=0;i<fieldMapping.getNumberOfDatastoreMappings();i++)
                                {
                                    if (i > 0)
                                    {
                                        datastoreMappingTypes.append(',');
                                    }
                                    datastoreMappingTypes.append(fieldMapping.getDatastoreMapping(i).getClass().getName());
                                }
                                NucleusLogger.DATASTORE.debug(LOCALISER.msg("057010",
                                    mmd.getFullFieldName(), columnsStr.toString(), fieldMapping.getClass().getName(), datastoreMappingTypes.toString()));
                            }
                        }
                    }
                }
                else
                {
                    // Manage the field if not already managed (may already exist if overriding a superclass field)
                    if (mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
                    {
                        boolean isPrimary = true;
                        if (mmd.getTable() != null && mmd.getJoinMetaData() == null)
                        {
                            // Field has a table specified and is not a 1-N with join table
                            // so is mapped to a secondary table
                            isPrimary = false;
                        }

                        if (isPrimary)
                        {
                            // Add the field to this table
                            JavaTypeMapping fieldMapping = storeMgr.getMappingManager().getMapping(this, mmd, clr, FieldRole.ROLE_FIELD);
                            if (theCmd != cmd && 
                                theCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE &&
                                fieldMapping.getNumberOfDatastoreMappings() > 0)
                            {
                                // Field is for a subclass and so column(s) has to either allow nulls, or have default
                                int numCols = fieldMapping.getNumberOfDatastoreMappings();
                                for (int colNum = 0;colNum < numCols; colNum++)
                                {
                                    Column col = (Column) fieldMapping.getDatastoreMapping(colNum).getDatastoreField();
                                    if (col.getDefaultValue() == null && !col.isNullable())
                                    {
                                        // Column needs to be nullable
                                        NucleusLogger.DATASTORE_SCHEMA.debug("Member " + mmd.getFullFieldName() +
                                            " uses superclass-table yet the field is not marked as nullable " +
                                            " nor does it have a default value, so setting the column as nullable");
                                        col.setNullable();
                                    }
                                }
                            }

                            addMemberMapping(fieldMapping);
                        }
                        else
                        {
                            // Add the field to the appropriate secondary table
                            if (secondaryTables == null)
                            {
                                secondaryTables = new HashMap();
                            }
                            SecondaryTable secTable = secondaryTables.get(mmd.getTable());
                            if (secTable == null)
                            {
                                // Secondary table doesnt exist yet so create it to users specifications.
                                JoinMetaData[] joinmds = theCmd.getJoinMetaData();
                                JoinMetaData joinmd = null;
                                if (joinmds != null)
                                {
                                    for (int j=0;j<joinmds.length;j++)
                                    {
                                        if (joinmds[j].getTable().equalsIgnoreCase(mmd.getTable()) &&
                                            (joinmds[j].getCatalog() == null || (joinmds[j].getCatalog() != null && joinmds[j].getCatalog().equalsIgnoreCase(mmd.getCatalog()))) &&
                                            (joinmds[j].getSchema() == null || (joinmds[j].getSchema() != null && joinmds[j].getSchema().equalsIgnoreCase(mmd.getSchema()))))
                                        {
                                            joinmd = joinmds[j];
                                            break;
                                        }
                                    }
                                }

                                DatastoreIdentifier secTableIdentifier = 
                                    storeMgr.getIdentifierFactory().newDatastoreContainerIdentifier(mmd.getTable());
                                // Use specified catalog, else take catalog of the owning table
                                String catalogName = mmd.getCatalog();
                                if (catalogName == null)
                                {
                                    catalogName = getCatalogName();
                                }
                                // Use specified schema, else take schema of the owning table
                                String schemaName = mmd.getSchema();
                                if (schemaName == null)
                                {
                                    schemaName = getSchemaName();
                                }
                                secTableIdentifier.setCatalogName(catalogName);
                                secTableIdentifier.setSchemaName(schemaName);

                                secTable = new SecondaryTable(secTableIdentifier, storeMgr, this, joinmd, clr);
                                secTable.preInitialize(clr);
                                secTable.initialize(clr);
                                secTable.postInitialize(clr);
                                secondaryTables.put(mmd.getTable(), secTable);
                            }
                            secTable.addMemberMapping(storeMgr.getMappingManager().getMapping(secTable, mmd, 
                                clr, FieldRole.ROLE_FIELD));
                        }
                    }
                    else if (mmd.getPersistenceModifier() != FieldPersistenceModifier.TRANSACTIONAL)
                    {
                        throw new NucleusException(LOCALISER.msg("057006",mmd.getName())).setFatal();
                    }

                    // Calculate if we need a FK adding due to a 1-N (FK) relationship
                    boolean needsFKToContainerOwner = false;
                    int relationType = mmd.getRelationType(clr);
                    if (relationType == Relation.ONE_TO_MANY_BI)
                    {
                        AbstractMemberMetaData[] relatedMmds = mmd.getRelatedMemberMetaData(clr);
                        if (mmd.getJoinMetaData() == null && relatedMmds[0].getJoinMetaData() == null)
                        {
                            needsFKToContainerOwner = true;
                        }
                    }
                    else if (relationType == Relation.ONE_TO_MANY_UNI)
                    {
                        if (mmd.getJoinMetaData() == null)
                        {
                            needsFKToContainerOwner = true;
                        }
                    }

                    if (needsFKToContainerOwner)
                    {
                        // 1-N uni/bidirectional using FK, so update the element side with a FK
                        if ((mmd.getCollection() != null && !SCOUtils.collectionHasSerialisedElements(mmd)) ||
                            (mmd.getArray() != null && !SCOUtils.arrayIsStoredInSingleColumn(mmd, storeMgr.getMetaDataManager())))
                        {
                            // 1-N ForeignKey collection/array, so add FK to element table
                            AbstractClassMetaData elementCmd = null;
                            if (mmd.hasCollection())
                            {
                                // Collection
                                elementCmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(mmd.getCollection().getElementType(), clr); 
                            }
                            else
                            {
                                // Array
                                elementCmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(mmd.getType().getComponentType(), clr);
                            }
                            if (elementCmd == null)
                            {
                                // Elements that are reference types or non-PC will come through here
                                if (mmd.hasCollection())
                                {
                                    NucleusLogger.METADATA.warn(LOCALISER.msg("057016", 
                                        theCmd.getFullClassName(), mmd.getCollection().getElementType()));
                                }
                                else
                                {
                                    NucleusLogger.METADATA.warn(LOCALISER.msg("057014", 
                                        theCmd.getFullClassName(), mmd.getType().getComponentType().getName()));
                                }
                            }
                            else
                            {
                                AbstractClassMetaData[] elementCmds = null;
                                // TODO : Cater for interface elements, and get the metadata for the implementation classes here
                                if (elementCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                                {
                                    elementCmds = storeMgr.getClassesManagingTableForClass(elementCmd, clr);
                                }
                                else
                                {
                                    elementCmds = new ClassMetaData[1];
                                    elementCmds[0] = elementCmd;
                                }

                                // Run callbacks for each of the element classes.
                                for (int i=0;i<elementCmds.length;i++)
                                {
                                    storeMgr.addSchemaCallback(elementCmds[i].getFullClassName(), mmd);
                                    DatastoreClass dc = storeMgr.getDatastoreClass(elementCmds[i].getFullClassName(), clr);
                                    if (dc == null)
                                    {
                                        throw new NucleusException("Unable to add foreign-key to " + 
                                            elementCmds[i].getFullClassName() + " to " + this + " since element has no table!");
                                    }
                                    ClassTable ct = (ClassTable) dc;
                                    if (ct.isInitialized())
                                    {
                                        // if the target table is already initialized, run the callbacks
                                        ct.runCallBacks(clr);
                                    }
                                }
                            }
                        }
                        else if (mmd.getMap() != null && !SCOUtils.mapHasSerialisedKeysAndValues(mmd))
                        {
                            // 1-N ForeignKey map, so add FK to value table
                            if (mmd.getKeyMetaData() != null && mmd.getKeyMetaData().getMappedBy() != null)
                            {
                                // Key is stored in the value table so add the FK to the value table
                                AbstractClassMetaData valueCmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(mmd.getMap().getValueType(), clr);
                                if (valueCmd == null)
                                {
                                    // Interface elements will come through here and java.lang.String and others as well
                                    NucleusLogger.METADATA.warn(LOCALISER.msg("057018", 
                                        theCmd.getFullClassName(), mmd.getMap().getValueType()));
                                }
                                else
                                {
                                    AbstractClassMetaData[] valueCmds = null;
                                    // TODO : Cater for interface values, and get the metadata for the implementation classes here
                                    if (valueCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                                    {
                                        valueCmds = storeMgr.getClassesManagingTableForClass(valueCmd, clr);
                                    }
                                    else
                                    {
                                        valueCmds = new ClassMetaData[1];
                                        valueCmds[0] = valueCmd;
                                    }
                                    
                                    // Run callbacks for each of the value classes.
                                    for (int i=0;i<valueCmds.length;i++)
                                    {
                                        storeMgr.addSchemaCallback(valueCmds[i].getFullClassName(), mmd);
                                        DatastoreClass dc = storeMgr.getDatastoreClass(valueCmds[i].getFullClassName(), clr);
                                        ClassTable ct = (ClassTable) dc;
                                        if (ct.isInitialized())
                                        {
                                            // if the target table is already initialized, run the callbacks
                                            ct.runCallBacks(clr);
                                        }
                                    }
                                }
                            }
                            else if (mmd.getValueMetaData() != null && mmd.getValueMetaData().getMappedBy() != null)
                            {
                                // Value is stored in the key table so add the FK to the key table
                                AbstractClassMetaData keyCmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(mmd.getMap().getKeyType(), clr);
                                if (keyCmd == null)
                                {
                                    // Interface elements will come through here and java.lang.String and others as well
                                    NucleusLogger.METADATA.warn(LOCALISER.msg("057019", 
                                        theCmd.getFullClassName(), mmd.getMap().getKeyType()));
                                }
                                else
                                {
                                    AbstractClassMetaData[] keyCmds = null;
                                    // TODO : Cater for interface keys, and get the metadata for the implementation classes here
                                    if (keyCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                                    {
                                        keyCmds = storeMgr.getClassesManagingTableForClass(keyCmd, clr);
                                    }
                                    else
                                    {
                                        keyCmds = new ClassMetaData[1];
                                        keyCmds[0] = keyCmd;
                                    }

                                    // Run callbacks for each of the key classes.
                                    for (int i=0;i<keyCmds.length;i++)
                                    {
                                        storeMgr.addSchemaCallback(keyCmds[i].getFullClassName(), mmd);
                                        DatastoreClass dc = storeMgr.getDatastoreClass(keyCmds[i].getFullClassName(), clr);
                                        ClassTable ct = (ClassTable) dc;
                                        if (ct.isInitialized())
                                        {
                                            // if the target table is already initialized, run the callbacks
                                            ct.runCallBacks(clr);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds on management of the columns in the defined MetaData that are "unmapped" (have no field associated).
     * @param theCmd ClassMetaData for the class to be managed
     * @param clr The ClassLoaderResolver
     */
    private void manageUnmappedColumns(AbstractClassMetaData theCmd, ClassLoaderResolver clr)
    {
        List cols = theCmd.getUnmappedColumns();
        if (cols != null && cols.size() > 0)
        {
            Iterator colsIter = cols.iterator();
            while (colsIter.hasNext())
            {
                ColumnMetaData colmd = (ColumnMetaData)colsIter.next();

                // Create a column with the specified name and jdbc-type
                if (colmd.getJdbcType().equals("VARCHAR") && colmd.getLength() == null)
                {
                    colmd.setLength(storeMgr.getIntProperty("datanucleus.rdbms.stringDefaultLength"));
                }
                IdentifierFactory idFactory = getStoreManager().getIdentifierFactory();
                DatastoreIdentifier colIdentifier = idFactory.newIdentifier(IdentifierType.COLUMN, colmd.getName());
                Column col = (Column)addDatastoreField(null, colIdentifier, null, colmd);
                SQLTypeInfo sqlTypeInfo = storeMgr.getSQLTypeInfoForJDBCType(
                    JDBCUtils.getJDBCTypeForName(colmd.getJdbcType()));
                col.setTypeInfo(sqlTypeInfo);

                if (unmappedColumns == null)
                {
                    unmappedColumns = new HashSet();
                }

                if (NucleusLogger.DATASTORE.isDebugEnabled())
                {
                    NucleusLogger.DATASTORE.debug(LOCALISER.msg("057011",
                        col.toString(), colmd.getJdbcType()));
                }
                unmappedColumns.add(col);
            }
        }
    }

    /**
     * Accessor for whether this table manages the specified class
     * @param className Name of the class
     * @return Whether it is managed by this table
     */
    public boolean managesClass(String className)
    {
        if (className == null)
        {
            return false;
        }

        Iterator<AbstractClassMetaData> iter = managedClassMetaData.iterator();
        while (iter.hasNext())
        {
            AbstractClassMetaData managedCmd = iter.next();
            if (managedCmd.getFullClassName().equals(className))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Method to initialise the table primary key field(s).
     * @param clr The ClassLoaderResolver
     */
    protected void initializePK(ClassLoaderResolver clr)
    {
        assertIsPKUninitialized();
        AbstractMemberMetaData[] membersToAdd = new AbstractMemberMetaData[cmd.getNoOfPrimaryKeyMembers()];

        // Initialise Primary Key mappings for application id with PK fields in this class
        int pkFieldNum=0;
        int fieldCount = cmd.getNoOfManagedMembers();
        boolean hasPrimaryKeyInThisClass = false;
        if (cmd.getNoOfPrimaryKeyMembers() > 0)
        {
            pkMappings = new JavaTypeMapping[cmd.getNoOfPrimaryKeyMembers()];
            if (cmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
            {
                // COMPLETE-TABLE so use root class metadata and add PK members
                // TODO Does this allow for overridden PK field info ?
                AbstractClassMetaData baseCmd = cmd.getBaseAbstractClassMetaData();
                fieldCount = baseCmd.getNoOfManagedMembers();
                for (int relFieldNum = 0; relFieldNum < fieldCount; ++relFieldNum)
                {
                    AbstractMemberMetaData mmd = baseCmd.getMetaDataForManagedMemberAtPosition(relFieldNum);
                    if (mmd.isPrimaryKey())
                    {
                        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
                        {
                            membersToAdd[pkFieldNum++] = mmd;
                            hasPrimaryKeyInThisClass = true;
                        }
                        else if (mmd.getPersistenceModifier() != FieldPersistenceModifier.TRANSACTIONAL)
                        {
                            throw new NucleusException(LOCALISER.msg("057006", mmd.getName())).setFatal();
                        }

                        // Check if auto-increment and that it is supported by this RDBMS
                        if ((mmd.getValueStrategy() == IdentityStrategy.IDENTITY) && 
                            !dba.supportsOption(DatastoreAdapter.IDENTITY_COLUMNS))
                        {
                            throw new NucleusException(LOCALISER.msg("057020",
                                cmd.getFullClassName(), mmd.getName())).setFatal();
                        }
                    }
                }
            }
            else
            {
                for (int relFieldNum=0; relFieldNum<fieldCount; ++relFieldNum)
                {
                    AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtPosition(relFieldNum);
                    if (fmd.isPrimaryKey())
                    {
                        if (fmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
                        {
                            membersToAdd[pkFieldNum++] = fmd;
                            hasPrimaryKeyInThisClass = true;
                        }
                        else if (fmd.getPersistenceModifier() != FieldPersistenceModifier.TRANSACTIONAL)
                        {
                            throw new NucleusException(LOCALISER.msg("057006",fmd.getName())).setFatal();
                        }

                        // Check if auto-increment and that it is supported by this RDBMS
                        if ((fmd.getValueStrategy() == IdentityStrategy.IDENTITY) && 
                            !dba.supportsOption(DatastoreAdapter.IDENTITY_COLUMNS))
                        {
                            throw new NucleusException(LOCALISER.msg("057020",
                                cmd.getFullClassName(), fmd.getName())).setFatal();
                        }
                    }
                }
            }
        }

        // No Primary Key defined, so search for superclass or handle datastore id
        if (!hasPrimaryKeyInThisClass)
        {
            if (cmd.getIdentityType() == IdentityType.APPLICATION)
            {
                // application-identity
                DatastoreClass elementCT = storeMgr.getDatastoreClass(cmd.getPersistenceCapableSuperclass(), clr);
                if (elementCT != null)
                {
                    // Superclass has a table so copy its PK mappings
                    ColumnMetaDataContainer colContainer = null;
                    if (cmd.getInheritanceMetaData() != null)
                    {
                        // Try via <inheritance><join>...</join></inheritance>
                        colContainer = cmd.getInheritanceMetaData().getJoinMetaData();
                    }
                    if (colContainer == null)
                    {
                        // Try via <primary-key>...</primary-key>
                        colContainer = cmd.getPrimaryKeyMetaData();
                    }

                    addApplicationIdUsingClassTableId(colContainer, elementCT, clr, cmd);
                }
                else
                {
                    // Superclass has no table so create new mappings and columns
                    AbstractClassMetaData pkCmd = storeMgr.getClassWithPrimaryKeyForClass(cmd.getSuperAbstractClassMetaData(), clr);
                    if (pkCmd != null)
                    {
                        pkMappings = new JavaTypeMapping[pkCmd.getNoOfPrimaryKeyMembers()];
                        pkFieldNum = 0;
                        fieldCount = pkCmd.getNoOfInheritedManagedMembers() + pkCmd.getNoOfManagedMembers();
                        for (int absFieldNum = 0; absFieldNum < fieldCount; ++absFieldNum)
                        {
                            AbstractMemberMetaData fmd = pkCmd.getMetaDataForManagedMemberAtAbsolutePosition(absFieldNum);
                            if (fmd.isPrimaryKey())
                            {
                                AbstractMemberMetaData overriddenFmd = cmd.getOverriddenMember(fmd.getName());
                                if (overriddenFmd != null)
                                {
                                    // PK field is overridden so use the overriding definition
                                    fmd = overriddenFmd;
                                }

                                if (fmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
                                {
                                    membersToAdd[pkFieldNum++] = fmd;
                                }
                                else if (fmd.getPersistenceModifier() != FieldPersistenceModifier.TRANSACTIONAL)
                                {
                                    throw new NucleusException(LOCALISER.msg("057006",fmd.getName())).setFatal();
                                }
                            }
                        }
                    }
                }
            }
            else if (cmd.getIdentityType() == IdentityType.DATASTORE)
            {
                // datastore-identity
                ColumnMetaData colmd = null;
                if (cmd.getIdentityMetaData() != null && cmd.getIdentityMetaData().getColumnMetaData() != null)
                {
                    // Try via <datastore-identity>...</datastore-identity>
                    colmd = cmd.getIdentityMetaData().getColumnMetaData();
                }
                if (colmd == null)
                {
                    // Try via <primary-key>...</primary-key>
                    if (cmd.getPrimaryKeyMetaData() != null && cmd.getPrimaryKeyMetaData().getColumnMetaData() != null &&
                        cmd.getPrimaryKeyMetaData().getColumnMetaData().length > 0)
                    {
                        colmd = cmd.getPrimaryKeyMetaData().getColumnMetaData()[0];
                    }
                }
                addDatastoreId(colmd, null, cmd);
            }
            else if (cmd.getIdentityType() == IdentityType.NONDURABLE)
            {
                // Do nothing since no identity!
            }
        }

        //add field mappings in the end, so we compute all columns after the post initialize
        for (int i=0; i<membersToAdd.length; i++)
        {
            if (membersToAdd[i] != null)
            {
                try
                {
                    DatastoreClass datastoreClass = getStoreManager().getDatastoreClass(membersToAdd[i].getType().getName(),clr);
                    if (datastoreClass.getIdMapping() == null)
                    {
                        throw new NucleusException("Unsupported relationship with field "+membersToAdd[i].getFullFieldName()).setFatal();
                    }
                }
                catch (NoTableManagedException ex)
                {
                    //do nothing
                }
                JavaTypeMapping fieldMapping = storeMgr.getMappingManager().getMapping(this, membersToAdd[i], clr, FieldRole.ROLE_FIELD);
                addMemberMapping(fieldMapping);
                pkMappings[i] = fieldMapping;
            }
        }
        initializeIDMapping();
        
        state = TABLE_STATE_PK_INITIALIZED;
    }

    /**
     * Method to initialise this table to include all fields in the specified class.
     * This is used to recurse up the hierarchy so that we include all immediate superclasses
     * that have "subclass-table" specified as their inheritance strategy. If we encounter the parent of
     * this class with other than "subclass-table" we stop the process.
     * @param theCmd The ClassMetaData for the class
     */
    private void initializeForClass(AbstractClassMetaData theCmd, ClassLoaderResolver clr)
    {
        String columnOrdering = storeMgr.getStringProperty("datanucleus.rdbms.tableColumnOrder");
        if (columnOrdering.equalsIgnoreCase("superclass-first"))
        {
            // Superclasses persisted into this table
            AbstractClassMetaData parentCmd = theCmd.getSuperAbstractClassMetaData();
            if (parentCmd != null)
            {
                if (cmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
                {
                    // Managed class requires all superclasses managed here too
                    initializeForClass(parentCmd, clr);
                }
                else if (parentCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                {
                    // Superclass uses "subclass-table" so needs managing here
                    initializeForClass(parentCmd, clr);
                }
            }

            // Owning class
            manageClass(theCmd, clr);
        }
        else
        {
            // Owning class
            manageClass(theCmd, clr);

            // Superclasses persisted into this table
            AbstractClassMetaData parentCmd = theCmd.getSuperAbstractClassMetaData();
            if (parentCmd != null)
            {
                if (cmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
                {
                    // Managed class requires all superclasses managed here too
                    initializeForClass(parentCmd, clr);
                }
                else if (parentCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                {
                    // Superclass uses "subclass-table" so needs managing here
                    initializeForClass(parentCmd, clr);
                }
            }
        }
    }

    /**
     * Execute the callbacks for the classes that this table maps to.
     * @param clr ClassLoader resolver
     */
    private void runCallBacks(ClassLoaderResolver clr)
    {
        // Run callbacks for all classes managed by this table
        Iterator<AbstractClassMetaData> cmdIter = managedClassMetaData.iterator();
        while (cmdIter.hasNext())
        {
            AbstractClassMetaData managedCmd = cmdIter.next();
            if (managingClassCurrent != null && managingClassCurrent.equals(managedCmd.getFullClassName()))
            {
                // We can't run callbacks for this class since it is still being initialised. Mark callbacks to run after it completes
                runCallbacksAfterManageClass = true;
                break;
            }

            Collection processedCallbacks = callbacksAppliedForManagedClass.get(managedCmd.getFullClassName());
            Collection c = (Collection)storeMgr.getSchemaCallbacks().get(managedCmd.getFullClassName());
            if (c != null)
            {
                if (processedCallbacks == null)
                {
                    processedCallbacks = new HashSet();
                    callbacksAppliedForManagedClass.put(managedCmd.getFullClassName(), processedCallbacks);
                }
                for (Iterator it = c.iterator(); it.hasNext();)
                {
                    AbstractMemberMetaData callbackMmd = (AbstractMemberMetaData) it.next();
                    if (processedCallbacks.contains(callbackMmd))
                    {
                        continue;
                    }
                    else
                    {
                        processedCallbacks.add(callbackMmd);
                    }

                    if (callbackMmd.getJoinMetaData() == null)
                    {
                        // 1-N FK relationship
                        AbstractMemberMetaData ownerFmd = callbackMmd;
                        if (ownerFmd.getMappedBy() != null)
                        {
                            // Bidirectional (element has a PC mapping to the owner)
                            // Check that the "mapped-by" field in the other class actually exists
                            AbstractMemberMetaData fmd = managedCmd.getMetaDataForMember(ownerFmd.getMappedBy());
                            if (fmd == null)
                            {
                                throw new NucleusUserException(LOCALISER.msg("057036",
                                    ownerFmd.getMappedBy(), managedCmd.getFullClassName(), ownerFmd.getFullFieldName()));
                            }

                            if (ownerFmd.getMap() != null && storeMgr.getBooleanProperty("datanucleus.rdbms.uniqueConstraints.mapInverse"))
                            {
                                initializeFKMapUniqueConstraints(ownerFmd);
                            }

                            boolean duplicate = false;
                            JavaTypeMapping fkDiscrimMapping = null;
                            JavaTypeMapping orderMapping = null;
                            if (ownerFmd.hasExtension("relation-discriminator-column"))
                            {
                                // Collection has a relation discriminator so we need to share the FK. Check for the required discriminator
                                String colName = ownerFmd.getValueForExtension("relation-discriminator-column");
                                if (colName == null)
                                {
                                    // No column defined so use a fallback name
                                    colName = "RELATION_DISCRIM";
                                }
                                Set fkDiscrimEntries = getExternalFkDiscriminatorMappings().entrySet();
                                Iterator discrimMappingIter = fkDiscrimEntries.iterator();
                                while (discrimMappingIter.hasNext())
                                {
                                    Map.Entry entry = (Map.Entry)discrimMappingIter.next();
                                    JavaTypeMapping discrimMapping = (JavaTypeMapping)entry.getValue();
                                    String discrimColName = (discrimMapping.getDatastoreMapping(0).getDatastoreField().getColumnMetaData()).getName();
                                    if (discrimColName.equalsIgnoreCase(colName))
                                    {
                                        duplicate = true;
                                        fkDiscrimMapping = discrimMapping;
                                        orderMapping = getExternalOrderMappings().get(entry.getKey());
                                        break;
                                    }
                                }

                                if (!duplicate)
                                {
                                    // Create the relation discriminator column since we dont have this discriminator
                                    ColumnMetaData colmd = new ColumnMetaData();
                                    colmd.setName(colName);
                                    colmd.setAllowsNull(Boolean.TRUE); // Allow for elements not in any discriminated collection
                                    fkDiscrimMapping = storeMgr.getMappingManager().getMapping(String.class); // Only support String discriminators currently
                                    fkDiscrimMapping.setDatastoreContainer(this);
                                    ColumnCreator.createIndexColumn(fkDiscrimMapping, storeMgr, clr, this, colmd, false);
                                }

                                if (fkDiscrimMapping != null)
                                {
                                    getExternalFkDiscriminatorMappings().put(ownerFmd, fkDiscrimMapping);
                                }
                            }

                            // Add the order mapping as necessary
                            addOrderMapping(ownerFmd, orderMapping, clr);
                        }
                        else
                        {
                            // Unidirectional (element knows nothing about the owner)
                            String ownerClassName = ownerFmd.getAbstractClassMetaData().getFullClassName();
                            JavaTypeMapping fkMapping = new PersistableMapping();
                            fkMapping.setDatastoreContainer(this);
                            fkMapping.initialize(storeMgr, ownerClassName);
                            JavaTypeMapping fkDiscrimMapping = null;
                            JavaTypeMapping orderMapping = null;
                            boolean duplicate = false;

                            try
                            {
                                // Get the owner id mapping of the "1" end
                                DatastoreClass ownerTbl = storeMgr.getDatastoreClass(ownerClassName, clr);
                                if (ownerTbl == null)
                                {
                                    // Class doesn't have its own table (subclass-table) so find where it persists
                                    AbstractClassMetaData[] ownerParentCmds = 
                                        storeMgr.getClassesManagingTableForClass(ownerFmd.getAbstractClassMetaData(), clr);
                                    if (ownerParentCmds.length > 1)
                                    {
                                        throw new NucleusUserException("Relation (" + ownerFmd.getFullFieldName() +
                                        ") with multiple related tables (using subclass-table). Not supported");
                                    }
                                    ownerClassName = ownerParentCmds[0].getFullClassName();
                                    ownerTbl = storeMgr.getDatastoreClass(ownerClassName, clr);
                                }

                                JavaTypeMapping ownerIdMapping = ownerTbl.getIdMapping();
                                ColumnMetaDataContainer colmdContainer = null;
                                if (ownerFmd.hasCollection() || ownerFmd.hasArray())
                                {
                                    // 1-N Collection/array
                                    colmdContainer = ownerFmd.getElementMetaData();
                                }
                                else if (ownerFmd.hasMap() && ownerFmd.getKeyMetaData() != null && ownerFmd.getKeyMetaData().getMappedBy() != null)
                                {
                                    // 1-N Map with key stored in the value
                                    colmdContainer = ownerFmd.getValueMetaData();
                                }
                                else if (ownerFmd.hasMap() && ownerFmd.getValueMetaData() != null && ownerFmd.getValueMetaData().getMappedBy() != null)
                                {
                                    // 1-N Map with value stored in the key
                                    colmdContainer = ownerFmd.getKeyMetaData();
                                }
                                CorrespondentColumnsMapper correspondentColumnsMapping = 
                                    new CorrespondentColumnsMapper(colmdContainer, ownerIdMapping, true);
                                int countIdFields = ownerIdMapping.getNumberOfDatastoreMappings();
                                for (int i=0; i<countIdFields; i++)
                                {
                                    DatastoreMapping refDatastoreMapping = ownerIdMapping.getDatastoreMapping(i);
                                    JavaTypeMapping mapping = storeMgr.getMappingManager().getMapping(refDatastoreMapping.getJavaTypeMapping().getJavaType());
                                    ColumnMetaData colmd = correspondentColumnsMapping.getColumnMetaDataByIdentifier(((Column)refDatastoreMapping.getDatastoreField()).getIdentifier());
                                    if (colmd == null)
                                    {
                                        throw new NucleusUserException(LOCALISER.msg("057035",
                                            ((Column)refDatastoreMapping.getDatastoreField()).getIdentifier(), toString())).setFatal();
                                    }

                                    DatastoreIdentifier identifier = null;
                                    IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
                                    if (colmd.getName() == null || colmd.getName().length() < 1)
                                    {
                                        // No user provided name so generate one
                                        identifier = idFactory.newForeignKeyFieldIdentifier(ownerFmd, 
                                            null, refDatastoreMapping.getDatastoreField().getIdentifier(), 
                                            storeMgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(mapping.getJavaType()),
                                            FieldRole.ROLE_OWNER);
                                    }
                                    else
                                    {
                                        // User-defined name
                                        identifier = idFactory.newDatastoreFieldIdentifier(colmd.getName());
                                    }
                                    DatastoreField refColumn = addDatastoreField(mapping.getJavaType().getName(), identifier, mapping, colmd);
                                    ((Column)refDatastoreMapping.getDatastoreField()).copyConfigurationTo(refColumn);

                                    if (colmd == null || (colmd != null && colmd.getAllowsNull() == null) ||
                                            (colmd != null && colmd.getAllowsNull() != null && colmd.isAllowsNull()))
                                    {
                                        // User either wants it nullable, or haven't specified anything, so make it nullable
                                        refColumn.setNullable();
                                    }

                                    fkMapping.addDatastoreMapping(getStoreManager().getMappingManager().createDatastoreMapping(mapping, refColumn, refDatastoreMapping.getJavaTypeMapping().getJavaType().getName()));
                                    ((PersistableMapping)fkMapping).addJavaTypeMapping(mapping);
                                }
                            }
                            catch (DuplicateDatastoreFieldException dce)
                            {
                                // If the user hasnt specified "relation-discriminator-column" here we dont allow the sharing of columns
                                if (!ownerFmd.hasExtension("relation-discriminator-column"))
                                {
                                    throw dce;
                                }

                                // Find the FK using this column and use it instead of creating a new one since we're sharing
                                Iterator fkIter = getExternalFkMappings().entrySet().iterator();
                                fkMapping = null;
                                while (fkIter.hasNext())
                                {
                                    Map.Entry entry = (Map.Entry)fkIter.next();
                                    JavaTypeMapping existingFkMapping = (JavaTypeMapping)entry.getValue();
                                    for (int j=0;j<existingFkMapping.getNumberOfDatastoreMappings();j++)
                                    {
                                        if (existingFkMapping.getDatastoreMapping(j).getDatastoreField().getIdentifier().toString().equals(dce.getConflictingColumn().getIdentifier().toString()))
                                        {
                                            // The FK is shared (and so if it is a List we also share the index)
                                            fkMapping = existingFkMapping;
                                            fkDiscrimMapping = externalFkDiscriminatorMappings.get(entry.getKey());
                                            orderMapping = getExternalOrderMappings().get(entry.getKey());
                                            break;
                                        }
                                    }
                                }
                                if (fkMapping == null)
                                {
                                    // Should never happen since we know there is a col duplicating ours
                                    throw dce;
                                }
                                duplicate = true;
                            }

                            if (!duplicate && ownerFmd.hasExtension("relation-discriminator-column"))
                            {
                                // Create the relation discriminator column
                                String colName = ownerFmd.getValueForExtension("relation-discriminator-column");
                                if (colName == null)
                                {
                                    // No column defined so use a fallback name
                                    colName = "RELATION_DISCRIM";
                                }
                                ColumnMetaData colmd = new ColumnMetaData();
                                colmd.setName(colName);
                                colmd.setAllowsNull(Boolean.TRUE); // Allow for elements not in any discriminated collection
                                fkDiscrimMapping = storeMgr.getMappingManager().getMapping(String.class); // Only support String discriminators currently
                                fkDiscrimMapping.setDatastoreContainer(this);
                                ColumnCreator.createIndexColumn(fkDiscrimMapping, storeMgr, clr, this, colmd, false);
                            }

                            // Save the external FK
                            getExternalFkMappings().put(ownerFmd, fkMapping);
                            if (fkDiscrimMapping != null)
                            {
                                getExternalFkDiscriminatorMappings().put(ownerFmd, fkDiscrimMapping);
                            }

                            // Add the order mapping as necessary
                            addOrderMapping(ownerFmd, orderMapping, clr);
                        }
                    }
                }
            }
        }
    }

    /**
     * Convenience method to add an order mapping to the table. Used with 1-N FK "indexed List"/array relations.
     * @param mmd Owner field MetaData
     * @param orderMapping The order mapping (maybe already set and just needs adding)
     * @param clr ClassLoader resolver
     * @return The order mapping (if updated)
     */
    private JavaTypeMapping addOrderMapping(AbstractMemberMetaData fmd, JavaTypeMapping orderMapping, ClassLoaderResolver clr)
    {
        boolean needsOrderMapping = false;
        OrderMetaData omd = fmd.getOrderMetaData();
        if (fmd.hasArray())
        {
            // Array field always has the index mapping
            needsOrderMapping = true;
        }
        else if (List.class.isAssignableFrom(fmd.getType()))
        {
            // List field
            needsOrderMapping = true;
            if (omd != null && !omd.isIndexedList())
            {
                // "ordered List" so no order mapping is needed
                needsOrderMapping = false;
            }
        }
        else if (java.util.Collection.class.isAssignableFrom(fmd.getType()) && 
            omd != null && omd.isIndexedList())
        {
            // Collection field with <order> and is indexed list so needs order mapping
            needsOrderMapping = true;
            if (omd.getMappedBy() != null)
            {
                // Try to find the mapping if already created
                orderMapping = getMemberMapping(omd.getMappedBy());
            }
        }

        if (needsOrderMapping)
        {
            // if the field is list or array type, add index column
            state = TABLE_STATE_NEW;
            if (orderMapping == null)
            {
                // Create new order mapping since we need one and we aren't using a shared FK
                orderMapping = this.addOrderColumn(fmd, clr);
            }
            getExternalOrderMappings().put(fmd, orderMapping);
            state = TABLE_STATE_INITIALIZED;
        }

        return orderMapping;
    }

    /**
     * Accessor for the main class represented.
     * @return The name of the class
     **/
    public String getType()
    {
        return cmd.getFullClassName();
    }

    /**
     * Accessor for the identity-type. 
     * @return identity-type tag value
     */    
    public IdentityType getIdentityType()
    {
        return cmd.getIdentityType();
    }

    /**
     * Accessor for versionMetaData
     * @return Returns the versionMetaData.
     */
    public final VersionMetaData getVersionMetaData()
    {
        return versionMetaData;
    }

    /**
     * Accessor for Discriminator MetaData
     * @return Returns the Discriminator MetaData.
     */
    public final DiscriminatorMetaData getDiscriminatorMetaData()
    {
        return discriminatorMetaData;
    }

    /**
     * Convenience method to return the root table with a discriminator in this inheritance tree.
     * @return The root table which has the discriminator in this inheritance tree
     */
    public final ClassTable getTableWithDiscriminator()
    {
        if (supertable != null)
        {
            ClassTable tbl = supertable.getTableWithDiscriminator();
            if (tbl != null)
            {
                return tbl;
            }
        }

        if (discriminatorMetaData != null)
        {
            // Initialised and discriminator metadata set so return this
            return this;
        }
        else if (cmd.getInheritanceMetaData() != null && cmd.getInheritanceMetaData().getDiscriminatorMetaData() != null)
        {
            // Not initialised but has discriminator MetaData so return this
            return this;
        }

        return null;
    }

    /**
     * Whether this table or super table has id (primary key) attributed by the datastore
     * @return true if the id attributed by the datastore
     */
    public boolean isObjectIdDatastoreAttributed()
    {
        boolean attributed = storeMgr.isStrategyDatastoreAttributed(cmd, -1);
        if (attributed)
        {
            return true;
        }
        for (int i=0; i<columns.size(); i++)
        {
            Column col = columns.get(i);
            if (col.isPrimaryKey() && col.isIdentity())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this table is the base table in the inheritance hierarchy.
     * @return true if this table is a root table
     */
    public boolean isBaseDatastoreClass()
    {
        return supertable == null ? true : false;
    }

    public DatastoreClass getBaseDatastoreClass()
    {
        if (supertable != null)
        {
            return supertable.getBaseDatastoreClass();
        }
        return this;
    }

    /**
     * Accessor for the supertable for this table.
     * @return The supertable
     **/
    public DatastoreClass getSuperDatastoreClass()
    {
        assertIsInitialized();

        return supertable;
    }

    /**
     * Accessor whether the supplied DatastoreClass is a supertable of this table.
     * @param table The DatastoreClass to check
     * @return Whether it is a supertable (somewhere up the inheritance tree)
     */
    public boolean isSuperDatastoreClass(DatastoreClass table)
    {
        if (this == table)
        {
            return true;
        }
        else if (supertable != null)
        {
            if (table == supertable)
            {
                return true;
            }
            else
            {
                return supertable.isSuperDatastoreClass(table);
            }
        }
        return false;
    }

    /**
     * Accessor for any secondary tables for this table.
     * @return Secondary tables (if any)
     */
    public Collection getSecondaryDatastoreClasses()
    {
        return (secondaryTables != null ? secondaryTables.values() : null);
    }

    /**
     * Accessor for the version mapping specified .
     * @param allowSuperclasses Whether we should return just the mapping from this table
     *     or whether we should return it when this table has none and the supertable has
     * @return The version mapping.
     */
    public JavaTypeMapping getVersionMapping(boolean allowSuperclasses)
    {
        if (versionMapping != null)
        {
            // We have the mapping so return it
            return versionMapping;
        }
        if (allowSuperclasses && supertable != null)
        {
            // Return what the supertable has if it has the mapping
            return supertable.getVersionMapping(allowSuperclasses);
        }
        return null;
    }

    /**
     * Accessor for the discriminator mapping specified .
     * @param allowSuperclasses Whether we should return just the mapping from this table
     *     or whether we should return it when this table has none and the supertable has
     * @return The discriminator mapping.
     */
    public JavaTypeMapping getDiscriminatorMapping(boolean allowSuperclasses)
    {
        if (discriminatorMapping != null)
        {
            // We have the mapping so return it
            return discriminatorMapping;
        }
        if (allowSuperclasses && supertable != null)
        {
            // Return what the supertable has if it has the mapping
            return supertable.getDiscriminatorMapping(allowSuperclasses);
        }
        return null;
    }

    public ClassTable getTableManagingMapping(JavaTypeMapping mapping)
    {
        if (managesMapping(mapping))
        {
            return this;
        }
        else if (supertable != null)
        {
            return supertable.getTableManagingMapping(mapping);
        }
        return null;
    }

    /**
     * Utility to find the table above this one. Will recurse to cater for inheritance
     * strategies where fields are handed up to the super class, or down to this class.
     * @param theCmd ClassMetaData of the class to find the supertable for.
     * @return The table above this one in any inheritance hierarchy
     */
    private ClassTable getSupertable(AbstractClassMetaData theCmd, ClassLoaderResolver clr)
    {
        if (cmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
        {
            // "complete-table" has no super table. All is persisted into this table
            return null;
        }

        AbstractClassMetaData superCmd = theCmd.getSuperAbstractClassMetaData();
        if (superCmd != null)
        {
            if (superCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.NEW_TABLE)
            {
                // This class has its own table, so return it.
                return (ClassTable) storeMgr.getDatastoreClass(superCmd.getFullClassName(), clr);
            }
            else if (superCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
            {
                // This class is mapped to the same table, so go up another level.
                return getSupertable(superCmd, clr);
            }
            else
            {
                // This class is mapped to its superclass table, so go up to that and try again.
                return getSupertable(superCmd, clr);
            }
        }
        return null;
    }

    /**
     * Convenience accessor for the base table for this table which has the specified field.
     * @param mmd Field MetaData for this field
     * @return The base table which has the field specified
     */
    public DatastoreClass getBaseDatastoreClassWithMember(AbstractMemberMetaData mmd)
    {
        if (mmd == null)
        {
            return null;
        }
        if (mmd.isPrimaryKey())
        {
            // If this is a PK field then it will be in all tables up to the base so we need to continue navigating up
            if (getSuperDatastoreClass() != null)
            {
                return getSuperDatastoreClass().getBaseDatastoreClassWithMember(mmd);
            }
        }
        if (memberMappingsMap.get(mmd) != null)
        {
            // We have this field so return this table
            return this;
        }
        else if (externalFkMappings != null && externalFkMappings.get(mmd) != null)
        {
            return this;
        }
        else if (externalFkDiscriminatorMappings != null && externalFkDiscriminatorMappings.get(mmd) != null)
        {
            return this;
        }
        else if (externalOrderMappings != null && externalOrderMappings.get(mmd) != null)
        {
            return this;
        }
        else if (getSuperDatastoreClass() == null)
        {
            // We don't have the field, but have no superclass, so return null
            return this;
        }
        else
        {
            // Return the superclass sicne we don't have it
            return getSuperDatastoreClass().getBaseDatastoreClassWithMember(mmd);
        }
    }

    /**
     * Accessor for the (primary) class MetaData.
     * Package-level access to restrict to other table types only.
     * @return The (primary) class MetaData
     **/
    ClassMetaData getClassMetaData()
    {
        return cmd;
    }

    /**
     * Accessor for the indices for this table. This includes both the
     * user-defined indices (via MetaData), and the ones required by foreign
     * keys (required by relationships).
     * @param clr The ClassLoaderResolver
     * @return The indices
     */
    protected Set getExpectedIndices(ClassLoaderResolver clr)
    {
        // Auto mode allows us to decide which indices are needed as well as using what is in the users MetaData
        boolean autoMode = false;
        if (storeMgr.getStringProperty("datanucleus.rdbms.constraintCreateMode").equals("DataNucleus"))
        {
            autoMode = true;
        }

        Set indices = new HashSet();

        // Add on any user-required indices for the fields/properties
        Set memberNumbersSet = memberMappingsMap.keySet();
        Iterator iter = memberNumbersSet.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData fmd = (AbstractMemberMetaData) iter.next();
            JavaTypeMapping fieldMapping = memberMappingsMap.get(fmd);
            if (fieldMapping instanceof EmbeddedPCMapping)
            {
                // Add indexes for fields of this embedded PC object
                EmbeddedPCMapping embMapping = (EmbeddedPCMapping)fieldMapping;
                for (int i=0;i<embMapping.getNumberOfJavaTypeMappings();i++)
                {
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
            else if (fieldMapping instanceof SerialisedMapping)
            {
                // Don't index these
            }
            else
            {
                // Add any required index for this field
                IndexMetaData imd = fmd.getIndexMetaData();
                if (imd != null)
                {
                    // Index defined so add it
                    Index index = TableUtils.getIndexForField(this, imd, fieldMapping);
                    if (index != null)
                    {
                        indices.add(index);
                    }
                }
                else if (autoMode)
                {
                    if (fmd.getIndexed() == null)
                    {
                        // Indexing not set, so add where we think it is appropriate
                        if (!fmd.isPrimaryKey()) // Ignore PKs since they will be indexed anyway
                        {
                            int relationType = fmd.getRelationType(clr);
                            if (relationType == Relation.ONE_TO_ONE_UNI)
                            {
                                // 1-1 with FK at this side so index the FK
                                if (fieldMapping instanceof ReferenceMapping)
                                {
                                    ReferenceMapping refMapping = (ReferenceMapping)fieldMapping;
                                    if (refMapping.getMappingStrategy() == ReferenceMapping.PER_IMPLEMENTATION_MAPPING)
                                    {
                                        // Cols per implementation : index each of implementations
                                        if (refMapping.getJavaTypeMapping() != null)
                                        {
                                            int colNum = 0;
                                            JavaTypeMapping[] implMappings = refMapping.getJavaTypeMapping();
                                            for (int i=0;i<implMappings.length;i++)
                                            {
                                                int numColsInImpl = implMappings[i].getNumberOfDatastoreMappings();
                                                Index index = new Index(this, false, null);
                                                for (int j=0;j<numColsInImpl;j++)
                                                {
                                                    index.setColumn(j, 
                                                        (Column)fieldMapping.getDatastoreMapping(colNum++).getDatastoreField());
                                                }
                                                indices.add(index);
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    Index index = new Index(this, false, null);
                                    for (int i=0;i<fieldMapping.getNumberOfDatastoreMappings();i++)
                                    {
                                        index.setColumn(i, (Column)fieldMapping.getDatastoreMapping(i).getDatastoreField());
                                    }
                                    indices.add(index);
                                }
                            }
                            else if (relationType == Relation.ONE_TO_ONE_BI && fmd.getMappedBy() == null)
                            {
                                // 1-1 with FK at this side so index the FK
                                Index index = new Index(this, false, null);
                                for (int i=0;i<fieldMapping.getNumberOfDatastoreMappings();i++)
                                {
                                    index.setColumn(i, (Column)fieldMapping.getDatastoreMapping(i).getDatastoreField());
                                }
                                indices.add(index);
                            }
                            else if (relationType == Relation.MANY_TO_ONE_BI)
                            {
                                // N-1 with FK at this side so index the FK
                                AbstractMemberMetaData relMmd = fmd.getRelatedMemberMetaData(clr)[0];
                                if (relMmd.getJoinMetaData() == null && fmd.getJoinMetaData() == null)
                                {
                                    Index index = new Index(this, false, null);
                                    for (int i=0;i<fieldMapping.getNumberOfDatastoreMappings();i++)
                                    {
                                        index.setColumn(i, (Column)fieldMapping.getDatastoreMapping(i).getDatastoreField());
                                    }
                                    indices.add(index);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Check if any version column needs indexing
        if (versionMapping != null)
        {
            IndexMetaData idxmd = getVersionMetaData().getIndexMetaData();
            if (idxmd != null)
            {
                Index index = new Index(this, idxmd.isUnique(),
                    idxmd.getValueForExtension("extended-setting"));
                if (idxmd.getName() != null)
                {
                    index.setName(idxmd.getName());
                }
                int countVersionFields = versionMapping.getNumberOfDatastoreMappings();
                for (int i=0; i<countVersionFields; i++)
                {
                    index.addDatastoreField(versionMapping.getDatastoreMapping(i).getDatastoreField());
                }
                indices.add(index);
            }
        }

        // Check if any discriminator column needs indexing
        if (discriminatorMapping != null)
        {
            DiscriminatorMetaData dismd = getDiscriminatorMetaData();
            IndexMetaData idxmd = dismd.getIndexMetaData();
            if (idxmd != null)
            {
                Index index = new Index(this, idxmd.isUnique(), 
                    idxmd.getValueForExtension("extended-setting"));
                if (idxmd.getName() != null)
                {
                    index.setName(idxmd.getName());
                }
                int countDiscrimFields = discriminatorMapping.getNumberOfDatastoreMappings();
                for (int i=0; i<countDiscrimFields; i++)
                {
                    index.addDatastoreField(discriminatorMapping.getDatastoreMapping(i).getDatastoreField());
                }
                indices.add(index);
            }
        }

        // Add on any order fields (for lists, arrays, collections) that need indexing
        Set orderMappingsEntries = getExternalOrderMappings().entrySet();
        Iterator orderMappingsEntriesIter = orderMappingsEntries.iterator();
        while (orderMappingsEntriesIter.hasNext())
        {
            Map.Entry entry = (Map.Entry)orderMappingsEntriesIter.next();
            AbstractMemberMetaData fmd = (AbstractMemberMetaData)entry.getKey();
            JavaTypeMapping mapping = (JavaTypeMapping)entry.getValue();
            OrderMetaData omd = fmd.getOrderMetaData();
            if (omd != null && omd.getIndexMetaData() != null)
            {
                Index index = getIndexForIndexMetaDataAndMapping(omd.getIndexMetaData(), mapping);
                if (index != null)
                {
                    indices.add(index);
                }
            }
        }

        // Add on any user-required indices for the class(es) as a whole (subelement of <class>)
        Iterator<AbstractClassMetaData> cmdIter = managedClassMetaData.iterator();
        while (cmdIter.hasNext())
        {
            AbstractClassMetaData thisCmd = cmdIter.next();
            IndexMetaData[] classIndices = thisCmd.getIndexMetaData();
            if (classIndices != null)
            {
                for (int i=0;i<classIndices.length;i++)
                {
                    Index index = getIndexForIndexMetaData(classIndices[i]);
                    if (index != null)
                    {
                        indices.add(index);
                    }
                }
            }
        }

        return indices;
    }

    /**
     * Convenience method to convert an IndexMetaData and a mapping into an Index.
     * @param imd The Index MetaData
     * @param mapping The mapping
     * @return The Index
     */
    private Index getIndexForIndexMetaDataAndMapping(IndexMetaData imd, JavaTypeMapping mapping)
    {
        // Verify if a unique index is needed
        boolean unique = imd.isUnique();

        Index index = new Index(this, unique, imd.getValueForExtension("extended-setting"));

        // Set the index name if required
        if (imd.getName() != null)
        {
            index.setName(imd.getName());
        }

        int numCols = mapping.getNumberOfDatastoreMappings();
        for (int i=0;i<numCols;i++)
        {
            index.addDatastoreField(mapping.getDatastoreMapping(i).getDatastoreField());
        }

        return index;
    }

    /**
     * Convenience method to convert an IndexMetaData into an Index.
     * @param imd The Index MetaData
     * @return The Index
     */
    private Index getIndexForIndexMetaData(IndexMetaData imd)
    {
        // Verify if a unique index is needed
        boolean unique = imd.isUnique();

        Index index = new Index(this, unique, imd.getValueForExtension("extended-setting"));

        // Set the index name if required
        if (imd.getName() != null)
        {
            index.setName(imd.getName());
        }

        // Set the column(s) to index
        // Class-level index so use its column definition
        ColumnMetaData[] colmds = imd.getColumnMetaData();
        AbstractMemberMetaData[] mmds = imd.getMemberMetaData();
        // a). Columns specified directly
        if (colmds != null && colmds.length > 0)
        {
            for (int i=0;i<colmds.length;i++)
            {
                DatastoreIdentifier colName = storeMgr.getIdentifierFactory().newDatastoreFieldIdentifier(colmds[i].getName());
                Column col = columnsByName.get(colName);
                if (col == null)
                {
                    NucleusLogger.DATASTORE.warn(LOCALISER.msg("058001", toString(), index.getName(),
                        colmds[i].getName()));
                    break;
                }
                else
                {
                    index.addDatastoreField(col);
                }
            }
        }
        // b). Columns specified using fields
        else if (mmds != null && mmds.length > 0)
        {
            for (int i=0;i<mmds.length;i++)
            {
                // Find the metadata for the actual field with the same name as this "index" field
                AbstractMemberMetaData realFmd = getMetaDataForMember(mmds[i].getName());
                JavaTypeMapping fieldMapping = memberMappingsMap.get(realFmd);
                int countFields = fieldMapping.getNumberOfDatastoreMappings();
                for (int j=0; j<countFields; j++)
                {
                    index.addDatastoreField(fieldMapping.getDatastoreMapping(j).getDatastoreField());
                }
            }
        }
        else
        {
            // We can't have an index of no columns
            NucleusLogger.DATASTORE.warn(LOCALISER.msg("058002", toString(), index.getName()));
            return null;
        }

        return index;
    }

    /**
     * Accessor for the expected foreign keys for this table.
     * @param clr The ClassLoaderResolver
     * @return The expected foreign keys.
     */
    public List getExpectedForeignKeys(ClassLoaderResolver clr)
    {
        assertIsInitialized();

        // Auto mode allows us to decide which FKs are needed as well as using what is in the users MetaData.
        boolean autoMode = false;
        if (storeMgr.getStringProperty("datanucleus.rdbms.constraintCreateMode").equals("DataNucleus"))
        {
            autoMode = true;
        }

        ArrayList foreignKeys = new ArrayList();

        // Check each field for FK requirements (user-defined, or required)
        // <field><foreign-key>...</foreign-key></field>
        Set memberNumbersSet = memberMappingsMap.keySet();
        Iterator iter = memberNumbersSet.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData mmd = (AbstractMemberMetaData) iter.next();
            JavaTypeMapping memberMapping = memberMappingsMap.get(mmd);

            if (mmd.getEmbeddedMetaData() != null && memberMapping instanceof EmbeddedPCMapping)
            {
                EmbeddedPCMapping embMapping = (EmbeddedPCMapping)memberMapping;
                addExpectedForeignKeysForEmbeddedPCField(foreignKeys, autoMode, clr, embMapping);
            }
            else
            {
                if (ClassUtils.isReferenceType(mmd.getType()) && memberMapping instanceof ReferenceMapping)
                {
                    // Field is a reference type, so add a FK to the table of the PC for each PC implementation
                    Collection fks = TableUtils.getForeignKeysForReferenceField(memberMapping, mmd, autoMode, storeMgr, clr);
                    foreignKeys.addAll(fks);
                }
                else if (storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(mmd.getType(), clr) != null &&
                        memberMapping.getNumberOfDatastoreMappings() > 0 &&
                        memberMapping instanceof PersistableMapping)
                {
                    // Field is for a PC class with the FK at this side, so add a FK to the table of this PC
                    ForeignKey fk = TableUtils.getForeignKeyForPCField(memberMapping, mmd, autoMode, storeMgr, clr);
                    if (fk != null)
                    {
                        foreignKeys.add(fk);
                    }
                }
            }
        }

        // FK from id column(s) to id column(s) of superclass, as specified by
        // <inheritance><join><foreign-key ...></join></inheritance>
        ForeignKeyMetaData idFkmd = (cmd.getInheritanceMetaData().getJoinMetaData() != null) ? 
                cmd.getInheritanceMetaData().getJoinMetaData().getForeignKeyMetaData() : null;
        if (supertable != null && (autoMode || (idFkmd != null && idFkmd.getDeleteAction() != ForeignKeyAction.NONE)))
        {
            ForeignKey fk = new ForeignKey(getIdMapping(), dba, supertable, false);
            if (idFkmd != null && idFkmd.getName() != null)
            {
                fk.setName(idFkmd.getName());
            }
            foreignKeys.add(0, fk);
        }

        // Add any user-required FKs for the class as a whole
        // <class><foreign-key>...</foreign-key></field>
        Iterator<AbstractClassMetaData> cmdIter = managedClassMetaData.iterator();
        while (cmdIter.hasNext())
        {
            AbstractClassMetaData thisCmd = cmdIter.next();
            ForeignKeyMetaData[] fkmds = thisCmd.getForeignKeyMetaData();
            if (fkmds != null)
            {
                for (int i=0;i<fkmds.length;i++)
                {
                    ForeignKey fk = getForeignKeyForForeignKeyMetaData(fkmds[i]);
                    if (fk != null)
                    {
                        foreignKeys.add(fk);
                    }
                }
            }
        }

        HashMap externalFks = getExternalFkMappings();
        if (!externalFks.isEmpty())
        {
            // 1-N FK relationships - FK to id column(s) of owner table where this is the element table and we have a FK
            Collection externalFkKeys = externalFks.entrySet();
            Iterator<Map.Entry<AbstractMemberMetaData, JavaTypeMapping>> externalFkKeysIter = externalFkKeys.iterator();
            while (externalFkKeysIter.hasNext())
            {
                Map.Entry<AbstractMemberMetaData, JavaTypeMapping> entry = externalFkKeysIter.next();
                AbstractMemberMetaData fmd = entry.getKey();
                DatastoreClass referencedTable = storeMgr.getDatastoreClass(fmd.getAbstractClassMetaData().getFullClassName(), clr);
                if (referencedTable != null)
                {
                    // Take <foreign-key> from either <field> or <element>
                    ForeignKeyMetaData fkmd = fmd.getForeignKeyMetaData();
                    if (fkmd == null && fmd.getElementMetaData() != null)
                    {
                        fkmd = fmd.getElementMetaData().getForeignKeyMetaData();
                    }
                    if ((fkmd != null && fkmd.getDeleteAction() != ForeignKeyAction.NONE) || autoMode)
                    {
                        // Either has been specified by user, or using autoMode, so add FK
                        JavaTypeMapping fkMapping = entry.getValue();
                        ForeignKey fk = new ForeignKey(fkMapping, dba, referencedTable, true);
                        fk.setForMetaData(fkmd); // Does nothing when no FK MetaData
                        if (!foreignKeys.contains(fk))
                        {
                            // Only add when not already present (in the case of shared FKs there can be dups here)
                            foreignKeys.add(fk);
                        }
                    }
                }
            }
        }

        return foreignKeys;
    }

    /**
     * Convenience method to add the expected FKs for an embedded PC field.
     * @param foreignKeys The list of FKs to add the FKs to
     * @param autoMode Whether operating in "auto-mode" where JPOX can create its own FKs
     * @param clr ClassLoader resolver
     * @param embeddedMapping The embedded PC mapping
     */
    private void addExpectedForeignKeysForEmbeddedPCField(List foreignKeys, boolean autoMode, ClassLoaderResolver clr, 
            EmbeddedPCMapping embeddedMapping)
    {
        for (int i=0;i<embeddedMapping.getNumberOfJavaTypeMappings();i++)
        {
            JavaTypeMapping embFieldMapping = embeddedMapping.getJavaTypeMapping(i);
            if (embFieldMapping instanceof EmbeddedPCMapping)
            {
                // Nested embedded PC so add the FKs for that
                addExpectedForeignKeysForEmbeddedPCField(foreignKeys, autoMode, clr, (EmbeddedPCMapping)embFieldMapping);
            }
            else
            {
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
    }

    /**
     * Convenience method to create a FK for the specified ForeignKeyMetaData.
     * Used for foreign-keys specified at &lt;class&gt; level.
     * @param fkmd ForeignKey MetaData
     * @return The ForeignKey
     */
    private ForeignKey getForeignKeyForForeignKeyMetaData(ForeignKeyMetaData fkmd)
    {
        if (fkmd == null)
        {
            return null;
        }

        // Create the ForeignKey base details
        ForeignKey fk = new ForeignKey(fkmd.isDeferred());
        fk.setForMetaData(fkmd);

        // Find the target of the foreign-key
        AbstractClassMetaData acmd = cmd;
        if (fkmd.getTable() == null)
        {
            // Can't create a FK if we don't know where it goes to
            NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058105", acmd.getFullClassName()));
            return null;
        }
        DatastoreIdentifier tableId = storeMgr.getIdentifierFactory().newDatastoreContainerIdentifier(fkmd.getTable());
        ClassTable refTable = (ClassTable)storeMgr.getDatastoreClass(tableId);
        if (refTable == null)
        {
            // TODO Go to the datastore and query for this table to get the columns of the PK
            NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058106", acmd.getFullClassName(),
                fkmd.getTable()));
            return null;
        }
        PrimaryKey pk = refTable.getPrimaryKey();
        List targetCols = pk.getColumns();

        // Generate the columns for the source of the foreign-key
        List sourceCols = new ArrayList();
        ColumnMetaData[] colmds = fkmd.getColumnMetaData();
        AbstractMemberMetaData[] fmds = fkmd.getMemberMetaData();
        if (colmds != null && colmds.length > 0)
        {
            // FK specified via <column>
            for (int i=0;i<colmds.length;i++)
            {
                // Find the column and add to the source columns for the FK
                DatastoreIdentifier colId = storeMgr.getIdentifierFactory().newDatastoreFieldIdentifier(colmds[i].getName());
                Column sourceCol = columnsByName.get(colId);
                if (sourceCol == null)
                {
                    NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058107",
                        acmd.getFullClassName(), fkmd.getTable(), colmds[i].getName(), toString()));
                    return null;
                }
                sourceCols.add(sourceCol);
            }
        }
        else if (fmds != null && fmds.length > 0)
        {
            // FK specified via <field>
            for (int i=0;i<fmds.length;i++)
            {
                // Find the metadata for the actual field with the same name as this "foreign-key" field
                // and add all columns to the source columns for the FK
                AbstractMemberMetaData realFmd = getMetaDataForMember(fmds[i].getName());
                JavaTypeMapping fieldMapping = memberMappingsMap.get(realFmd);
                int countDatastoreFields = fieldMapping.getNumberOfDatastoreMappings();
                for (int j=0; j<countDatastoreFields; j++)
                {
                    // Add each column of this field to the FK definition
                    sourceCols.add(fieldMapping.getDatastoreMapping(j).getDatastoreField());
                }
            }
        }

        if (sourceCols.size() != targetCols.size())
        {
            // Different number of cols in this table and target table
            NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER.msg("058108",
                acmd.getFullClassName(), fkmd.getTable(), "" + sourceCols.size(), "" + targetCols.size()));
        }

        // Add all column mappings to the ForeignKey
        if (sourceCols.size() > 0)
        {
            for (int i=0;i<sourceCols.size();i++)
            {
                Column source = (Column)sourceCols.get(i);
                String targetColName = colmds[i].getTarget();
                Column target = (Column)targetCols.get(i); // Default to matching via the natural order
                if (targetColName != null)
                {
                    // User has specified the target column for this col so try it in our target list
                    for (int j=0;j<targetCols.size();j++)
                    {
                        Column targetCol = (Column)targetCols.get(j);
                        if (targetCol.getIdentifier().getIdentifierName().equalsIgnoreCase(targetColName))
                        {
                            // Found the required column
                            target = targetCol;
                            break;
                        }
                    }
                }
                fk.addDatastoreField(source, target);
            }
        }

        return fk;
    }

    /**
     * Accessor for the expected candidate keys for this table.
     * @return The expected candidate keys.
     */
    protected List getExpectedCandidateKeys()
    {
        assertIsInitialized();

        // The candidate keys required by the basic table
        List candidateKeys = super.getExpectedCandidateKeys();

        // Add on any user-required candidate keys for the fields
        Set fieldNumbersSet = memberMappingsMap.keySet();
        Iterator iter = fieldNumbersSet.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData fmd=(AbstractMemberMetaData) iter.next();
            JavaTypeMapping fieldMapping = memberMappingsMap.get(fmd);
            if (fieldMapping instanceof EmbeddedPCMapping)
            {
                // Add indexes for fields of this embedded PC object
                EmbeddedPCMapping embMapping = (EmbeddedPCMapping)fieldMapping;
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
            else
            {
                // Add any required candidate key for this field
                UniqueMetaData umd = fmd.getUniqueMetaData();
                if (umd != null)
                {
                    CandidateKey ck = TableUtils.getCandidateKeyForField(this, umd, fieldMapping);
                    if (ck != null)
                    {
                        candidateKeys.add(ck);
                    }
                }
            }
        }

        // Add on any user-required candidate keys for the class(es) as a whole (composite keys)
        Iterator<AbstractClassMetaData> cmdIter = managedClassMetaData.iterator();
        while (cmdIter.hasNext())
        {
            AbstractClassMetaData thisCmd = cmdIter.next();
            UniqueMetaData[] classCKs = thisCmd.getUniqueMetaData();
            if (classCKs != null)
            {
                for (int i=0;i<classCKs.length;i++)
                {
                    CandidateKey ck = getCandidateKeyForUniqueMetaData(classCKs[i]);
                    if (ck != null)
                    {
                        candidateKeys.add(ck);
                    }
                }
            }
        }

        return candidateKeys;
    }

    /**
     * Convenience method to convert a UniqueMetaData into a CandidateKey.
     * @param umd The Unique MetaData
     * @return The Candidate Key
     */
    private CandidateKey getCandidateKeyForUniqueMetaData(UniqueMetaData umd)
    {
        CandidateKey ck = new CandidateKey(this);

        // Set the key name if required
        if (umd.getName() != null)
        {
            ck.setName(umd.getName());
        }

        // Class-level index so use its column definition
        ColumnMetaData[] colmds = umd.getColumnMetaData();
        AbstractMemberMetaData[] mmds = umd.getMemberMetaData();
        // a). Columns specified directly
        if (colmds != null && colmds.length > 0)
        {
            for (int i=0;i<colmds.length;i++)
            {
                DatastoreIdentifier colName = storeMgr.getIdentifierFactory().newDatastoreFieldIdentifier(colmds[i].getName());
                Column col = columnsByName.get(colName);
                if (col == null)
                {
                    NucleusLogger.DATASTORE.warn(LOCALISER.msg("058202", 
                        toString(), ck.getName(), colmds[i].getName()));
                    break;
                }
                else
                {
                    ck.addDatastoreField(col);
                }
            }
        }
        // b). Columns specified using fields
        else if (mmds != null && mmds.length > 0)
        {
            for (int i=0;i<mmds.length;i++)
            {
                // Find the metadata for the actual field with the same name as this "unique" field
                AbstractMemberMetaData realMmd = getMetaDataForMember(mmds[i].getName());
                if (realMmd == null)
                {
                    // User is an idiot and has specified some field that doesnt exist
                    NucleusLogger.DATASTORE.warn("Unique metadata defined to use field " + mmds[i].getName() + " which doesn't exist in this class");
                    return null;
                }
                JavaTypeMapping memberMapping = memberMappingsMap.get(realMmd);
                int countFields = memberMapping.getNumberOfDatastoreMappings();
                for (int j=0; j<countFields; j++)
                {
                    ck.addDatastoreField(memberMapping.getDatastoreMapping(j).getDatastoreField());
                }
            }
        }
        else
        {
            // We can't have an index of no columns
            NucleusLogger.DATASTORE.warn(LOCALISER.msg("058203", 
                toString(), ck.getName()));
            return null;
        }

        return ck;
    }

    /**
     * Accessor for the primary key for this table. Overrides the method in TableImpl
     * to add on any specification of PK name in the metadata.
     * @return The primary key.
     */
    public PrimaryKey getPrimaryKey()
    {
        PrimaryKey pk = super.getPrimaryKey();
        PrimaryKeyMetaData pkmd = cmd.getPrimaryKeyMetaData();
        if (pkmd != null && pkmd.getName() != null)
        {
            pk.setName(pkmd.getName());
        }

        return pk;
    }

    /**
     * Accessor for the CREATE statements for this table.
     * Creates this table followed by all secondary tables (if any).
     * @param props Properties for creating the table
     * @return the SQL statements to be executed for creation
     */
    protected List getSQLCreateStatements(Properties props)
    {
        List stmts;

        // Create statements for this table
        Properties tableProps = null;
        if (createStatementDDL != null)
        {
            // User has specified the DDL
            stmts = new ArrayList();
            StringTokenizer tokens = new StringTokenizer(createStatementDDL, ";");
            while (tokens.hasMoreTokens())
            {
                stmts.add(tokens.nextToken());
            }
        }
        else
        {
            // JPOX creates the DDL
            if (cmd.getExtensions() != null)
            {
                tableProps = new Properties();
                ExtensionMetaData[] emds = cmd.getExtensions();
                for (int i=0;i<emds.length;i++)
                {
                    if (emds[i].getVendorName().equalsIgnoreCase(MetaData.VENDOR_NAME))
                    {
                        tableProps.put(emds[i].getKey(), emds[i].getValue());
                    }
                }
            }
            stmts = super.getSQLCreateStatements(tableProps);
        }

        // Create statements for any secondary tables
        // Since the secondary tables are managed by us, we need to create their table
        if (secondaryTables != null)
        {
            Set secondaryTableNames = secondaryTables.keySet();
            Iterator iter = secondaryTableNames.iterator();
            while (iter.hasNext())
            {
                SecondaryTable secTable = secondaryTables.get(iter.next());
                stmts.addAll(secTable.getSQLCreateStatements(tableProps)); // Use same tableProps as the primary table
            }
        }

        // Unique constraints creation
        stmts.addAll(getSQLAddUniqueConstraintsStatements());

        return stmts;
    }

    /**
     * Accessor for the DROP statements for this table.
     * Drops all secondary tables (if any) followed by the table itself.
     * @return List of statements
     */
    protected List getSQLDropStatements()
    {
        assertIsInitialized();
        ArrayList stmts = new ArrayList();

        // Drop any secondary tables
        if (secondaryTables != null)
        {
            Set secondaryTableNames = secondaryTables.keySet();
            Iterator iter = secondaryTableNames.iterator();
            while (iter.hasNext())
            {
                SecondaryTable secTable = secondaryTables.get(iter.next());
                stmts.addAll(secTable.getSQLDropStatements());
            }
        }

        // Drop this table
        stmts.add(dba.getDropTableStatement(this));

        return stmts;
    }

    private List getSQLAddUniqueConstraintsStatements()
    {
        ArrayList stmts = new ArrayList();
        int ckNum = 0;
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();

        Iterator<CandidateKey> cks = candidateKeysByMapField.values().iterator();
        while (cks.hasNext())
        {
            DatastoreIdentifier ckName = idFactory.newCandidateKeyIdentifier(this, ++ckNum);
            CandidateKey ck = cks.next();
            ck.setName(ckName.getIdentifierName());

            String ckSql = dba.getAddCandidateKeyStatement(ck, idFactory);
            if (ckSql != null)
            {
                stmts.add(ckSql);
            }
        }

        return stmts;
    }

    /**
     * Method to initialise unique constraints for 1-N Map using FK.
     * @param ownerMmd metadata for the field/property with the map in the owner class
     */
    private void initializeFKMapUniqueConstraints(AbstractMemberMetaData ownerMmd)
    {
        // Load "mapped-by"
        AbstractMemberMetaData mfmd = null;
        String map_field_name = ownerMmd.getMappedBy(); // Field in our class that maps back to the owner class
        if (map_field_name != null)
        {
            // Bidirectional
            mfmd = cmd.getMetaDataForMember(map_field_name);
            if (mfmd == null)
            {
                // Field not in primary class so may be in subclass so check all managed classes
                Iterator<AbstractClassMetaData> cmdIter = managedClassMetaData.iterator();
                while (cmdIter.hasNext())
                {
                    AbstractClassMetaData managedCmd = cmdIter.next();
                    mfmd = managedCmd.getMetaDataForMember(map_field_name);
                    if (mfmd != null)
                    {
                        break;
                    }
                }
            }
            if (mfmd == null)
            {
                // "mapped-by" refers to a field in our class that doesnt exist!
                throw new NucleusUserException(LOCALISER.msg("057036",
                    map_field_name, cmd.getFullClassName(), ownerMmd.getFullFieldName()));                       
            }

            if (mfmd != null)
            {
                if (ownerMmd.getJoinMetaData() == null)
                {
                    // Load field of key in value
                    if (ownerMmd.getKeyMetaData() != null && ownerMmd.getKeyMetaData().getMappedBy() != null)
                    {
                        // Key field is stored in the value table
                        AbstractMemberMetaData kmd = null;
                        String key_field_name = ownerMmd.getKeyMetaData().getMappedBy();
                        if (key_field_name != null)
                        {
                            kmd = cmd.getMetaDataForMember(key_field_name);
                        }
                        if (kmd == null)
                        {
                            // Field not in primary class so may be in subclass so check all managed classes
                            Iterator<AbstractClassMetaData> cmdIter = managedClassMetaData.iterator();
                            while (cmdIter.hasNext())
                            {
                                AbstractClassMetaData managedCmd = cmdIter.next();
                                kmd = managedCmd.getMetaDataForMember(key_field_name);
                                if (kmd != null)
                                {
                                    break;
                                }
                            }
                        }
                        if (kmd == null)
                        {
                            throw new ClassDefinitionException(LOCALISER.msg("057007", 
                                mfmd.getFullFieldName(), key_field_name));
                        }

                        JavaTypeMapping ownerMapping = getMemberMapping(map_field_name);
                        JavaTypeMapping keyMapping = getMemberMapping(kmd.getName());

                        if (dba.supportsOption(RDBMSAdapter.NULLS_IN_CANDIDATE_KEYS) || 
                            (!ownerMapping.isNullable() && !keyMapping.isNullable()))
                        {
                            // If the owner and key fields are represented in this table then we can impose
                            // a unique constraint on them. If the key field is in a superclass then we
                            // cannot do this so just omit it.
                            if (keyMapping.getDatastoreContainer() == this &&
                                ownerMapping.getDatastoreContainer() == this)
                            {
                                CandidateKey ck = new CandidateKey(this);

                                // This HashSet is to avoid duplicate adding of columns.
                                HashSet addedColumns = new HashSet();

                                // Add columns for the owner field
                                int countOwnerFields = ownerMapping.getNumberOfDatastoreMappings();
                                for (int i = 0; i < countOwnerFields; i++)
                                {
                                    Column col = (Column) ownerMapping.getDatastoreMapping(i).getDatastoreField();
                                    addedColumns.add(col);
                                    ck.addDatastoreField(col);
                                }

                                // Add columns for the key field
                                int countKeyFields = keyMapping.getNumberOfDatastoreMappings();
                                for (int i = 0; i < countKeyFields; i++)
                                {
                                    Column col = (Column) keyMapping.getDatastoreMapping(i).getDatastoreField();
                                    if (!addedColumns.contains(col))
                                    {
                                        addedColumns.add(col);
                                        ck.addDatastoreField(col);
                                    }
                                    else
                                    {
                                        NucleusLogger.DATASTORE.warn(LOCALISER.msg("057041", 
                                            ownerMmd.getName()));
                                    }
                                }

                                if (candidateKeysByMapField.put(mfmd, ck) != null)
                                {
                                    // We have multiple "mapped-by" coming to this field so give a warning that this may potentially
                                    // cause problems. For example if they have the key field defined here for 2 different relations
                                    // so you may get keys/values appearing in the other relation that shouldn't be.
                                    // Logged as a WARNING for now. 
                                    // If there is a situation where this should throw an exception, please update this AND COMMENT WHY.
                                    NucleusLogger.DATASTORE.warn(LOCALISER.msg("057012", 
                                        mfmd.getFullFieldName(), ownerMmd.getFullFieldName()));
                                }
                            }
                        }
                    }
                    else if (ownerMmd.getValueMetaData() != null && ownerMmd.getValueMetaData().getMappedBy() != null)
                    {
                        // Value field is stored in the key table
                        AbstractMemberMetaData vmd = null;
                        String value_field_name = ownerMmd.getValueMetaData().getMappedBy();
                        if (value_field_name != null)
                        {
                            vmd = cmd.getMetaDataForMember(value_field_name);
                        }
                        if (vmd == null)
                        {
                            throw new ClassDefinitionException(LOCALISER.msg("057008", mfmd));
                        }

                        JavaTypeMapping ownerMapping = getMemberMapping(map_field_name);
                        JavaTypeMapping valueMapping = getMemberMapping(vmd.getName());

                        if (dba.supportsOption(RDBMSAdapter.NULLS_IN_CANDIDATE_KEYS) || 
                            (!ownerMapping.isNullable() && !valueMapping.isNullable()))
                        {
                            // If the owner and value fields are represented in this table then we can impose
                            // a unique constraint on them. If the value field is in a superclass then we
                            // cannot do this so just omit it.
                            if (valueMapping.getDatastoreContainer() == this &&
                                ownerMapping.getDatastoreContainer() == this)
                            {
                                CandidateKey ck = new CandidateKey(this);

                                // This HashSet is to avoid duplicate adding of columns.
                                HashSet addedColumns = new HashSet();

                                // Add columns for the owner field
                                int countOwnerFields = ownerMapping.getNumberOfDatastoreMappings();
                                for (int i = 0; i < countOwnerFields; i++)
                                {
                                    Column col = (Column) ownerMapping.getDatastoreMapping(i).getDatastoreField();
                                    addedColumns.add(col);
                                    ck.addDatastoreField(col);
                                }

                                // Add columns for the value field
                                int countValueFields = valueMapping.getNumberOfDatastoreMappings();
                                for (int i = 0; i < countValueFields; i++)
                                {
                                    Column col = (Column) valueMapping.getDatastoreMapping(i).getDatastoreField();
                                    if (!addedColumns.contains(col))
                                    {
                                        addedColumns.add(col);
                                        ck.addDatastoreField(col);
                                    }
                                    else
                                    {
                                        NucleusLogger.DATASTORE.warn(LOCALISER.msg("057042", 
                                            ownerMmd.getName()));
                                    }
                                }

                                if (candidateKeysByMapField.put(mfmd, ck) != null)
                                {
                                    // We have multiple "mapped-by" coming to this field so give a warning that this may potentially
                                    // cause problems. For example if they have the key field defined here for 2 different relations
                                    // so you may get keys/values appearing in the other relation that shouldn't be.
                                    // Logged as a WARNING for now. 
                                    // If there is a situation where this should throw an exception, please update this AND COMMENT WHY.
                                    NucleusLogger.DATASTORE.warn(LOCALISER.msg("057012",
                                        mfmd.getFullFieldName(), ownerMmd.getFullFieldName()));
                                }
                            }
                        }
                    }
                    else
                    {
                        // We can only have either the key stored in the value or the value stored in the key but nothing else!
                        throw new ClassDefinitionException(LOCALISER.msg("057009", ownerMmd.getFullFieldName()));
                    }
                }
            }
        }
    }

    /**
     * Initialize the ID Mapping.
     * For datastore identity this will be a PCMapping that contains the OIDMapping.
     * For application identity this will be a PCMapping that contains the PK mapping(s).
     */
    private void initializeIDMapping()
    {
        if (idMapping != null)
        {
            return;
        }

        final PersistableMapping mapping = new PersistableMapping();
        mapping.setDatastoreContainer(this);
        mapping.initialize(getStoreManager(), cmd.getFullClassName());

        if (getIdentityType() == IdentityType.DATASTORE)
        {
            mapping.addJavaTypeMapping(datastoreIDMapping);
        }
        else if (getIdentityType() == IdentityType.APPLICATION)
        {
            for (int i = 0; i < pkMappings.length; i++)
            {
                mapping.addJavaTypeMapping(pkMappings[i]);
            }
        }
        else
        {
            // Nothing to do for nondurable since no identity
        }

        idMapping = mapping;        
    }
    
    /**
     * Accessor for a mapping for the ID (PersistenceCapable) for this table.
     * @return The (PersistenceCapable) ID mapping.
     */
    public JavaTypeMapping getIdMapping()
    {
        return idMapping;
    }

    /**
     * Accessor for all of the order mappings (used by FK Lists, Collections, Arrays)
     * @return The mappings for the order columns.
     **/
    private HashMap<AbstractMemberMetaData, JavaTypeMapping> getExternalOrderMappings()
    {
        if (externalOrderMappings == null)
        {       
            externalOrderMappings = new HashMap();
        }
        return externalOrderMappings;
    }

    /**
     * Accessor for all of the external FK mappings.
     * @return The mappings for external FKs
     **/
    private HashMap<AbstractMemberMetaData, JavaTypeMapping> getExternalFkMappings()
    {
        if (externalFkMappings == null)
        {       
            externalFkMappings = new HashMap();
        }
        return externalFkMappings;
    }

    /**
     * Accessor for an external mapping for the specified field of the required type.
     * @param mmd MetaData for the field/property
     * @param mappingType Type of mapping
     * @return The (external) mapping
     */
    public JavaTypeMapping getExternalMapping(AbstractMemberMetaData mmd, int mappingType)
    {
        if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_FK)
        {
            return getExternalFkMappings().get(mmd);
        }
        else if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_FK_DISCRIM)
        {
            return getExternalFkDiscriminatorMappings().get(mmd);
        }
        else if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX)
        {
            return getExternalOrderMappings().get(mmd);
        }
        else
        {
            return null;
        }
    }

    /**
     * Accessor for the MetaData for the (owner) field that an external mapping corresponds to.
     * @param mapping The mapping
     * @param mappingType The mapping type
     * @return metadata for the external mapping
     */
    public AbstractMemberMetaData getMetaDataForExternalMapping(JavaTypeMapping mapping, int mappingType)
    {
        if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_FK)
        {
            Set entries = getExternalFkMappings().entrySet();
            Iterator iter = entries.iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                if (entry.getValue() == mapping)
                {
                    return (AbstractMemberMetaData)entry.getKey();
                }
            }
        }
        else if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_FK_DISCRIM)
        {
            Set entries = getExternalFkDiscriminatorMappings().entrySet();
            Iterator iter = entries.iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                if (entry.getValue() == mapping)
                {
                    return (AbstractMemberMetaData)entry.getKey();
                }
            }
        }
        else if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX)
        {
            Set entries = getExternalOrderMappings().entrySet();
            Iterator iter = entries.iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                if (entry.getValue() == mapping)
                {
                    return (AbstractMemberMetaData)entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Accessor for all of the external FK discriminator mappings.
     * @return The mappings for external FKs
     **/
    private HashMap<AbstractMemberMetaData, JavaTypeMapping> getExternalFkDiscriminatorMappings()
    {
        if (externalFkDiscriminatorMappings == null)
        {       
            externalFkDiscriminatorMappings = new HashMap();
        }
        return externalFkDiscriminatorMappings;
    }

    /**
     * Accessor for the field mapping for the specified field.
     * The field can be managed by a supertable of this table.
     * @param mmd MetaData for this field/property
     * @return the Mapping for the field/property
     */
    public JavaTypeMapping getMemberMapping(AbstractMemberMetaData mmd)
    {
        if (mmd == null)
        {
            return null;
        }

        if (mmd instanceof PropertyMetaData && mmd.getAbstractClassMetaData() instanceof InterfaceMetaData)
        {
            // "Persistent Interfaces" don't do lookups correctly in here so just use the field name for now
            // TODO Fix Persistent Interfaces lookup
            return getMemberMapping(mmd.getName());
        }

        if (mmd.isPrimaryKey())
        {
            assertIsPKInitialized();
        }
        else
        {
            assertIsInitialized();
        }

        // Check if we manage this field
        JavaTypeMapping m = memberMappingsMap.get(mmd);
        if (m != null)
        {
            return m;
        }

        // Check supertable
        int ifc = cmd.getNoOfInheritedManagedMembers();
        if (mmd.getAbsoluteFieldNumber() < ifc)
        {
            if (supertable != null)
            {
                m = supertable.getMemberMapping(mmd);
                if (m != null)
                {
                    return m;
                }
            }
        }

        // Check secondary tables
        if (secondaryTables != null)
        {
            Collection secTables = secondaryTables.values();
            Iterator iter = secTables.iterator();
            while (iter.hasNext())
            {
                SecondaryTable secTable = (SecondaryTable)iter.next();
                m = secTable.getMemberMapping(mmd);
                if (m != null)
                {
                    return m;
                }
            }
        }

        return null;
    }

    /**
     * Accessor for the mapping for the specified field only in this datastore class.
     * @param mmd Metadata of the field/property
     * @return The Mapping for the field/property (or null if not present here)
     */
    public JavaTypeMapping getMemberMappingInDatastoreClass(AbstractMemberMetaData mmd)
    {
        if (mmd == null)
        {
            return null;
        }

        if (mmd instanceof PropertyMetaData && mmd.getAbstractClassMetaData() instanceof InterfaceMetaData)
        {
            // "Persistent Interfaces" don't do lookups correctly in here so just use the field name for now
            // TODO Fix Persistent Interfaces lookup
            return getMemberMapping(mmd.getName());
        }

        if (mmd.isPrimaryKey())
        {
            assertIsPKInitialized();
        }
        else
        {
            assertIsInitialized();
        }

        // Check if we manage this field
        JavaTypeMapping m = memberMappingsMap.get(mmd);
        if (m != null)
        {
            return m;
        }

        // Check if it is a PK field
        if (pkMappings != null)
        {
            for (int i=0;i<pkMappings.length;i++)
            {
                JavaTypeMapping pkMapping = pkMappings[i];
                if (pkMapping.getMemberMetaData() == mmd)
                {
                    return pkMapping;
                }
            }
        }

        return null;
    }

    /**
     * Accessor for the field mapping for the named field.
     * The field may exist in a parent table or a secondary table.
     * Throws a NoSuchPersistentFieldException if the field name is not found.
     * TODO Use of this is discouraged since the fieldName is not fully qualified
     * and if a superclass-table inheritance is used we could have 2 fields of that name here.
     * @param memberName Name of field/property
     * @return The mapping.
     * @throws NoSuchPersistentFieldException Thrown when the field/property is not found
     */
    public JavaTypeMapping getMemberMapping(String memberName)
    {
        assertIsInitialized();
        AbstractMemberMetaData mmd = getMetaDataForMember(memberName);
        JavaTypeMapping m = getMemberMapping(mmd);
        if (m == null)
        {
            throw new NoSuchPersistentFieldException(cmd.getFullClassName(), memberName);
        }
        return m;
    }

    /**
     * Acessor for the FieldMetaData for the field with the specified name.
     * Searches the MetaData of all classes managed by this table.
     * Doesn't allow for cases where the table manages subclasses with the same field name.
     * In that case you should use the equivalent method passing FieldMetaData.
     * TODO Support subclasses with fields of the same name
     * TODO Use of this is discouraged since the fieldName is not fully qualified
     * and if a superclass-table inheritance is used we could have 2 fields of that name here.
     * @param memberName the field/property name
     * @return metadata for the member
     */
    AbstractMemberMetaData getMetaDataForMember(String memberName)
    {
        // TODO Dont search "cmd" since it is included in "managedClassMetaData"
        AbstractMemberMetaData mmd = cmd.getMetaDataForMember(memberName);
        if (mmd == null)
        {
            Iterator<AbstractClassMetaData> iter = managedClassMetaData.iterator();
            while (iter.hasNext())
            {
                AbstractClassMetaData theCmd = iter.next();
                final AbstractMemberMetaData foundMmd = theCmd.getMetaDataForMember(memberName);
                if (foundMmd != null)
                {
                    if (mmd != null && (!mmd.toString().equalsIgnoreCase(foundMmd.toString()) || mmd.getType() != foundMmd.getType()))
                    {
                        final String errMsg = "Table " + getIdentifier() + 
                        " manages at least 2 subclasses that both define a field \"" + memberName + "\", " + 
                        "and the fields' metadata is different or they have different type! That means you can get e.g. wrong fetch results.";
                        NucleusLogger.DATASTORE.error(errMsg);
                        throw new NucleusException(errMsg).setFatal();
                    }
                    mmd = foundMmd;
                }
            }
        }
        return mmd;
    }

    /**
     * Utility to throw an exception if the object is not PersistenceCapable.
     * @param sm The StateManager for the object
     */ 
    void assertPCClass(ObjectProvider sm)
    {
        Class c = sm.getObject().getClass();

        if (!sm.getExecutionContext().getClassLoaderResolver().isAssignableFrom(cmd.getFullClassName(),c))
        {
            throw new NucleusException(LOCALISER.msg("057013",cmd.getFullClassName(),c)).setFatal();
        }
    }

    /**
     * Adds an ordering column to the element table (this) in FK list relationships.
     * Used to store the position of the element in the List.
     * If the &lt;order&gt; provides a mapped-by, this will return the existing column mapping.
     * @param mmd The MetaData of the field/property with the list for the column to map to
     * @return The Mapping for the order column
     */
    private JavaTypeMapping addOrderColumn(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        Class indexType = Integer.class;
        JavaTypeMapping indexMapping = new IndexMapping();
        indexMapping.initialize(storeMgr, indexType.getName());
        indexMapping.setMemberMetaData(mmd);
        indexMapping.setDatastoreContainer(this);
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        DatastoreIdentifier indexColumnName = null;
        ColumnMetaData colmd = null;

        // Allow for any user definition in OrderMetaData
        OrderMetaData omd = mmd.getOrderMetaData();
        if (omd != null)
        {
            colmd = (omd.getColumnMetaData() != null && omd.getColumnMetaData().length > 0 ? omd.getColumnMetaData()[0] : null);
            if (omd.getMappedBy() != null)
            {
                // User has defined ordering using the column(s) of an existing field.
                state = TABLE_STATE_INITIALIZED; // Not adding anything so just set table back to "initialised"
                JavaTypeMapping orderMapping = getMemberMapping(omd.getMappedBy());
                if (orderMapping == null)
                {
                    throw new NucleusUserException(LOCALISER.msg("057021", 
                        mmd.getFullFieldName(), omd.getMappedBy()));
                }
                if (!(orderMapping instanceof IntegerMapping) && !(orderMapping instanceof LongMapping))
                {
                    throw new NucleusUserException(LOCALISER.msg("057022", 
                        mmd.getFullFieldName(), omd.getMappedBy()));
                }
                return orderMapping;
            }

            String colName = null;
            if (omd.getColumnMetaData() != null && omd.getColumnMetaData().length > 0 && omd.getColumnMetaData()[0].getName() != null)
            {
                // User-defined name so create an identifier using it
                colName = omd.getColumnMetaData()[0].getName();
                indexColumnName = idFactory.newDatastoreFieldIdentifier(colName);
            }
        }
        if (indexColumnName == null)
        {
            // No name defined so generate one
            indexColumnName = idFactory.newForeignKeyFieldIdentifier(mmd, null, null, 
                storeMgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(indexType), FieldRole.ROLE_INDEX);
        }

        DatastoreField column = addDatastoreField(indexType.getName(), indexColumnName, indexMapping, colmd);
        if (colmd == null || (colmd != null && colmd.getAllowsNull() == null) ||
            (colmd != null && colmd.getAllowsNull() != null && colmd.isAllowsNull()))
        {
            // User either wants it nullable, or havent specified anything, so make it nullable
            column.setNullable();
        }

        storeMgr.getMappingManager().createDatastoreMapping(indexMapping, column, indexType.getName());

        return indexMapping;
    }

    /**
     * Provide the mappings to the consumer for all primary-key fields mapped to
     * this table.
     * @param consumer Consumer for the mappings
     */
    public void providePrimaryKeyMappings(MappingConsumer consumer)
    {
        consumer.preConsumeMapping(highestMemberNumber + 1);

        if (pkMappings != null)
        {
            // Application identity
            int[] primaryKeyFieldNumbers = cmd.getPKMemberPositions();
            for (int i=0;i<pkMappings.length;i++)
            {
                // Make the assumption that the pkMappings are in the same order as the absolute field numbers
                AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(primaryKeyFieldNumbers[i]);
                consumer.consumeMapping(pkMappings[i], fmd);
            }
        }
        else
        {
            // Datastore identity
            int[] primaryKeyFieldNumbers = cmd.getPKMemberPositions();
            int countPkFields = cmd.getNoOfPrimaryKeyMembers();
            for (int i = 0; i < countPkFields; i++)
            {
                AbstractMemberMetaData pkfmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(primaryKeyFieldNumbers[i]);
                consumer.consumeMapping(getMemberMapping(pkfmd), pkfmd);
            }
        }
    }

    /**
     * Provide the mappings to the consumer for all external fields mapped to this table
     * of the specified type
     * @param consumer Consumer for the mappings
     * @param mappingType Type of external mapping
     */
    final public void provideExternalMappings(MappingConsumer consumer, int mappingType)
    {
        if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_FK && externalFkMappings != null)
        {
            consumer.preConsumeMapping(highestMemberNumber + 1);

            Iterator<AbstractMemberMetaData> iter = externalFkMappings.keySet().iterator();
            while (iter.hasNext())
            {
                AbstractMemberMetaData fmd = iter.next();
                JavaTypeMapping fieldMapping = externalFkMappings.get(fmd);
                if (fieldMapping != null)
                {
                    consumer.consumeMapping(fieldMapping, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
                }
            }
        }
        else if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_FK_DISCRIM && externalFkDiscriminatorMappings != null)
        {
            consumer.preConsumeMapping(highestMemberNumber + 1);

            Iterator<AbstractMemberMetaData> iter = externalFkDiscriminatorMappings.keySet().iterator();
            while (iter.hasNext())
            {
                AbstractMemberMetaData fmd = iter.next();
                JavaTypeMapping fieldMapping = externalFkDiscriminatorMappings.get(fmd);
                if (fieldMapping != null)
                {
                    consumer.consumeMapping(fieldMapping, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK_DISCRIM);
                }
            }
        }
        else if (mappingType == MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX && externalOrderMappings != null)
        {
            consumer.preConsumeMapping(highestMemberNumber + 1);

            Iterator<AbstractMemberMetaData> iter = externalOrderMappings.keySet().iterator();
            while (iter.hasNext())
            {
                AbstractMemberMetaData fmd = iter.next();
                JavaTypeMapping fieldMapping = externalOrderMappings.get(fmd);
                if (fieldMapping != null)
                {
                    consumer.consumeMapping(fieldMapping, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
                }
            }
        }
    }

    /**
     * Provide the mappings to the consumer for all absolute field Numbers in this table
     * that are container in the fieldNumbers parameter.
     * @param consumer Consumer for the mappings
     * @param fieldMetaData MetaData for the fields to provide mappings for
     * @param includeSecondaryTables Whether to provide fields in secondary tables
     */
    public void provideMappingsForMembers(MappingConsumer consumer, AbstractMemberMetaData[] fieldMetaData, boolean includeSecondaryTables)
    {
        super.provideMappingsForMembers(consumer, fieldMetaData, true);
        if (includeSecondaryTables && secondaryTables != null)
        {
            Collection secTables = secondaryTables.values();
            Iterator iter = secTables.iterator();
            while (iter.hasNext())
            {
                SecondaryTable secTable = (SecondaryTable)iter.next();
                secTable.provideMappingsForMembers(consumer, fieldMetaData, false);
            }
        }
    }

    /**
     * Method to provide all unmapped datastore fields (columns) to the consumer.
     * @param consumer Consumer of information
     */
    public void provideUnmappedDatastoreFields(MappingConsumer consumer)
    {
        if (unmappedColumns != null)
        {
            Iterator<Column> iter = unmappedColumns.iterator();
            while (iter.hasNext())
            {
                consumer.consumeUnmappedDatastoreField(iter.next());
            }
        }
    }

    /**
     * Method to validate the constraints of this table.
     * @param conn Connection to use in validation
     * @param autoCreate Whether to auto create the constraints
     * @param autoCreateErrors Whether to log a warning only on errors during "auto create"
     * @param clr The ClassLoaderResolver
     * @return Whether the DB was modified
     * @throws SQLException Thrown when an error occurs in validation
     */
    public boolean validateConstraints(Connection conn, boolean autoCreate, Collection autoCreateErrors, ClassLoaderResolver clr)
    throws SQLException
    {
        boolean modified = false;
        if (super.validateConstraints(conn, autoCreate, autoCreateErrors, clr))
        {
            modified = true;
        }

        // Validate our secondary tables since we manage them
        if (secondaryTables != null)
        {
            Collection secTables = secondaryTables.values();
            Iterator iter = secTables.iterator();
            while (iter.hasNext())
            {
                SecondaryTable secTable = (SecondaryTable)iter.next();
                if (secTable.validateConstraints(conn, autoCreate, autoCreateErrors, clr))
                {
                    modified = true;
                }
            }
        }

        return modified;
    }
}