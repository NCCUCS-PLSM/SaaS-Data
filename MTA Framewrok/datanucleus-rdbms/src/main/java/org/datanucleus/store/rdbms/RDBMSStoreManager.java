/**********************************************************************
Copyright (c) 2003 Mike Martin (TJDO) and others. All rights reserved. 
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
2003 Kelly Grizzle (TJDO)
2003 Erik Bengtson  - removed exist() operation
2003 Erik Bengtson  - refactored the persistent id generator System property
2003 Andy Jefferson - added localiser
2003 Andy Jefferson - updated exception handling with SchemaTable
2003 Andy Jefferson - restructured to remove SchemaTable, and add StoreManagerHelper
2003 Andy Jefferson - updated getSubClassesForClass to recurse
2004 Erik Bengtson  - removed unused method and variables 
2004 Erik Bengtson  - fixed problem with getObjectById for App ID in getClassForOID
2004 Andy Jefferson - re-emergence of SchemaTable. Addition of addClass().
2004 Andy Jefferson - Addition of AutoStartMechanism interface
2004 Andy Jefferson - Update to use Logger
2004 Andy Jefferson - Addition of Catalog name to accompany Schema name
2004 Marco Schulze  - replaced catch(NotPersistenceCapableException ...)
                  by advance-check via TypeManager.isSupportedType(...)
2004 Andy Jefferson - split StoreData into superclass.
2004 Andy Jefferson - added support for other inheritance types
2004 Andy Jefferson - added capability to dynamically add columns
2005 Marco Schulze - prevent closing starter during recursion of ClassAdder.addClassTables(...)
    ...
**********************************************************************/
package org.datanucleus.store.rdbms;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.FetchPlan;
import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.UserTransaction;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.OID;
import org.datanucleus.identity.SCOID;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.ExtensionMetaData;
import org.datanucleus.metadata.IdentityMetaData;
import org.datanucleus.metadata.IdentityStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.InheritanceMetaData;
import org.datanucleus.metadata.InheritanceStrategy;
import org.datanucleus.metadata.MapMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.metadata.TableGeneratorMetaData;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.NucleusConnection;
import org.datanucleus.store.NucleusConnectionImpl;
import org.datanucleus.store.NucleusSequence;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.exceptions.UnsupportedDataTypeException;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.IdentifierType;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.mapping.DatastoreMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapterFactory;
import org.datanucleus.store.rdbms.autostart.SchemaAutoStarter;
import org.datanucleus.store.rdbms.fieldmanager.ParameterSetter;
import org.datanucleus.store.rdbms.fieldmanager.ResultSetGetter;
import org.datanucleus.store.rdbms.mapping.RDBMSMapping;
import org.datanucleus.store.rdbms.query.PersistentClassROF;
import org.datanucleus.store.rdbms.schema.JDBCTypeInfo;
import org.datanucleus.store.rdbms.schema.RDBMSColumnInfo;
import org.datanucleus.store.rdbms.schema.RDBMSSchemaHandler;
import org.datanucleus.store.rdbms.schema.RDBMSSchemaInfo;
import org.datanucleus.store.rdbms.schema.RDBMSTableInfo;
import org.datanucleus.store.rdbms.schema.RDBMSTypesInfo;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.scostore.RDBMSFKArrayStore;
import org.datanucleus.store.rdbms.scostore.RDBMSFKListStore;
import org.datanucleus.store.rdbms.scostore.RDBMSFKMapStore;
import org.datanucleus.store.rdbms.scostore.RDBMSFKSetStore;
import org.datanucleus.store.rdbms.scostore.RDBMSJoinArrayStore;
import org.datanucleus.store.rdbms.scostore.RDBMSJoinListStore;
import org.datanucleus.store.rdbms.scostore.RDBMSJoinMapStore;
import org.datanucleus.store.rdbms.scostore.RDBMSJoinSetStore;
import org.datanucleus.store.rdbms.scostore.RDBMSPersistableRelationStore;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.store.rdbms.table.ArrayTable;
import org.datanucleus.store.rdbms.table.ClassTable;
import org.datanucleus.store.rdbms.table.ClassView;
import org.datanucleus.store.rdbms.table.CollectionTable;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.store.rdbms.table.JoinTable;
import org.datanucleus.store.rdbms.table.MapTable;
import org.datanucleus.store.rdbms.table.PersistableJoinTable;
import org.datanucleus.store.rdbms.table.ProbeTable;
import org.datanucleus.store.rdbms.table.Table;
import org.datanucleus.store.rdbms.table.TableImpl;
import org.datanucleus.store.rdbms.table.ViewImpl;
import org.datanucleus.store.rdbms.valuegenerator.SequenceTable;
import org.datanucleus.store.rdbms.valuegenerator.TableGenerator;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import org.datanucleus.store.scostore.ArrayStore;
import org.datanucleus.store.scostore.ListStore;
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.scostore.PersistableRelationStore;
import org.datanucleus.store.scostore.SetStore;
import org.datanucleus.store.valuegenerator.AbstractDatastoreGenerator;
import org.datanucleus.store.valuegenerator.ValueGenerationConnectionProvider;
import org.datanucleus.store.valuegenerator.ValueGenerator;
import org.datanucleus.transaction.TransactionUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.MacroString;
import org.datanucleus.util.MultiMap;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * StoreManager for RDBMS datastores. 
 * Provided by the "store-manager" extension key "rdbms" and accepts datastore URLs valid for JDBC.
 * <p>
 * The RDBMS manager's responsibilities extend those for StoreManager to add :
 * <ul>
 * <li>creates and controls access to the data sources required for this datastore instance</li>
 * <li>implements insert(), fetch(), update(), delete() in the interface to the StateManager.</li>
 * <li>Providing cached access to JDBC database metadata (in particular column information).</li>
 * <li>Resolving SQL identifier macros to actual SQL identifiers.</li>
 * </ul>
 * TODO Change RDBMSManager to share schema information (DatabaseMetaData) with other RDBMSManager.
 */
public class RDBMSStoreManager extends MappedStoreManager implements SchemaAwareStoreManager
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER_RDBMS = Localiser.getInstance(
        "org.datanucleus.store.rdbms.Localisation", RDBMSStoreManager.class.getClassLoader());

    /** Controller for SQL executed on this store. */
    private SQLController sqlController = null;

    /** Factory for expressions using the generic query SQL mechanism. */
    protected SQLExpressionFactory expressionFactory;

    /**
     * The active class adder transaction, if any. Some RDBMSManager methods are
     * called recursively in the course of adding new classes. This field allows
     * such methods to coordinate with the active ClassAdder transaction.
     * Recursive methods include:
     * <ul>
     * <li>addClasses()</li>
     * <li>addSetTable()</li>
     * <li>addMapTable()</li>
     * <li>addListTable()</li>
     * <li>addArrayTable()</li>
     * </ul>
     * Access is synchronized on the RDBMSManager itself. 
     * Invariant: classAdder == null if RDBMSManager is unlocked.
     */
    private ClassAdder classAdder = null;

    /** 
     * Object to use for locking the ClassAdder process. This could be revised with CORE-3409
     * and the above ClassAdder field to find a better locking strategy.
     */
    private final Object classAdditionLock = new Object();

    /** Writer for use when this RDBMSManager is configured to write DDL. */
    private Writer ddlWriter = null;

    /** 
     * Flag for use when this RDBMSManager is configured to write DDL, whether we should generate 
     * complete DDL or upgrade DDL 
     */
    private boolean completeDDL = false;

    /** 
     * DDL statements already written when in DDL mode. 
     * This is used to eliminate duplicate statements from bidirectional relations.
     */
    private Set<String> writtenDdlStatements = null;

    /** State variable for schema generation of the callback information to be processed. */
    private MultiMap schemaCallbacks = new MultiMap();

    /** Calendar for this datastore. */
    private transient Calendar dateTimezoneCalendar = null;

    /**
     * Constructs a new RDBMSManager. On successful return the new RDBMSManager
     * will have successfully connected to the database with the given
     * credentials and determined the schema name, but will not have inspected
     * the schema contents any further. The contents (tables, views, etc.) will
     * be subsequently created and/or validated on-demand as the application
     * accesses persistent classes.
     * 
     * @param clr the ClassLoaderResolver
     * @param ctx The corresponding Context. This factory's non-tx data source will be 
     *     used to get database connections as needed to perform management functions.
     * @param props Properties for the datastore
     * @exception NucleusDataStoreException If the database could not be accessed or the name of the
     *                schema could not be determined.
     */
    public RDBMSStoreManager(ClassLoaderResolver clr, NucleusContext ctx, Map<String, Object> props)
    {
        super("rdbms", clr, ctx, props);

        persistenceHandler = new RDBMSPersistenceHandler(this);
        schemaHandler = new RDBMSSchemaHandler(this);
        expressionFactory = new SQLExpressionFactory(this);

        // Retrieve the Database Adapter for this datastore
        try
        {
            ManagedConnection mc = getConnection(-1);
            Connection conn = (Connection)mc.getConnection();
            if (conn == null)
            {
                //somehow we haven't got an exception from the JDBC driver
                //to troubleshoot the user should telnet to ip/port of database and check if he can open a connection
                //this may be due to security / firewall things.
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("050007"));
            }

            try
            {
                dba = RDBMSAdapterFactory.getInstance().getDatastoreAdapter(clr, conn, 
                    getStringProperty("datanucleus.rdbms.datastoreAdapterClassName"), 
                    ctx.getPluginManager());
                dba.initialiseTypes(schemaHandler, mc);
                dba.removeUnsupportedMappings(schemaHandler, mc);

                // User specified default catalog/schema name - check for validity, and store
                if (hasPropertyNotNull(PropertyNames.PROPERTY_MAPPING_CATALOG))
                {
                    if (!((RDBMSAdapter)dba).supportsOption(RDBMSAdapter.CATALOGS_IN_TABLE_DEFINITIONS))
                    {
                        NucleusLogger.DATASTORE.warn(LOCALISER_RDBMS.msg("050002",
                            getStringProperty(PropertyNames.PROPERTY_MAPPING_CATALOG)));
                    }
                    else
                    {
                        catalogName = getStringProperty(PropertyNames.PROPERTY_MAPPING_CATALOG);
                    }
                }
                if (hasPropertyNotNull(PropertyNames.PROPERTY_MAPPING_SCHEMA))
                {
                    if (!((RDBMSAdapter)dba).supportsOption(DatastoreAdapter.SCHEMAS_IN_TABLE_DEFINITIONS))
                    {
                        NucleusLogger.DATASTORE.warn(LOCALISER_RDBMS.msg("050003",
                            getStringProperty(PropertyNames.PROPERTY_MAPPING_SCHEMA)));
                    }
                    else
                    {
                        schemaName = getStringProperty(PropertyNames.PROPERTY_MAPPING_SCHEMA);
                    }
                }

                // Create an identifier factory - needs the database adapter to exist first
                initialiseIdentifierFactory(ctx);

                // Now that we have the identifier factory, make sure any user-provided names were valid!
                if (schemaName != null)
                {
                    String validSchemaName = identifierFactory.getIdentifierInAdapterCase(schemaName);
                    if (!validSchemaName.equals(schemaName))
                    {
                        NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER_RDBMS.msg("020192", "schema", schemaName, validSchemaName));
                        schemaName = validSchemaName;
                    }
                }
                if (catalogName != null)
                {
                    String validCatalogName = identifierFactory.getIdentifierInAdapterCase(catalogName);
                    if (!validCatalogName.equals(catalogName))
                    {
                        NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER_RDBMS.msg("020192", "catalog", catalogName, validCatalogName));
                        catalogName = validCatalogName;
                    }
                }

                // Create the SQL controller
                sqlController = new SQLController(((RDBMSAdapter)dba).supportsOption(RDBMSAdapter.STATEMENT_BATCHING), 
                    getIntProperty("datanucleus.rdbms.statementBatchLimit"),
                    getIntProperty(PropertyNames.PROPERTY_DATASTORE_READ_TIMEOUT),
                    getBooleanProperty("datanucleus.rdbms.sqlParamValuesInBrackets"));

                // TODO These ought to be stored with the StoreManager, not the NucleusContext
                // Initialise any properties controlling the adapter
                // Just use properties matching the pattern "datanucleus.rdbms.adapter.*"
                Map<String, Object> dbaProps = new HashMap();
                Map<String, Object> omfProps = ctx.getPersistenceConfiguration().getPersistenceProperties();
                Iterator<Map.Entry<String, Object>> propIter = omfProps.entrySet().iterator();
                while (propIter.hasNext())
                {
                    Map.Entry<String, Object> entry = propIter.next();
                    String prop = entry.getKey();
                    if (prop.startsWith("datanucleus.rdbms.adapter."))
                    {
                        dbaProps.put(prop, entry.getValue());
                    }
                }
                if (dbaProps.size() > 0)
                {
                    dba.setProperties(dbaProps);
                }

                // Initialise the Schema
                initialiseSchema(conn, clr);

                // Log the configuration of the RDBMS
                logConfiguration();
            }
            finally
            {
                mc.close();
            }
        }
        catch (NucleusException ne)
        {
            NucleusLogger.DATASTORE_SCHEMA.error(LOCALISER_RDBMS.msg("050004"), ne);
            throw ne.setFatal();
        }
        catch (Exception e1)
        {
            // Unknown type of exception so wrap it in a NucleusUserException for later handling
            String msg = LOCALISER_RDBMS.msg("050004") + ' ' + 
                LOCALISER_RDBMS.msg("050006") + ' ' + LOCALISER_RDBMS.msg("048000",e1);
            NucleusLogger.DATASTORE_SCHEMA.error(msg, e1);
            throw new NucleusUserException(msg, e1).setFatal();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getQueryCacheKey()
     */
    public String getQueryCacheKey()
    {
        // Return "rdbms-hsqldb", etc
        return getStoreManagerKey() + "-" + getDatastoreAdapter().getVendorID();
    }

    public MultiMap getSchemaCallbacks()
    {
        return schemaCallbacks;
    }

    public void addSchemaCallback(String className, AbstractMemberMetaData mmd)
    {
        Collection coll = (Collection)schemaCallbacks.get(className);
        if (coll == null || !coll.contains(mmd))
        {
            schemaCallbacks.put(className, mmd);
        }
    }

    /**
     * Convenience method to log the configuration of this store manager.
     */
    protected void logConfiguration()
    {
        super.logConfiguration();

        if (NucleusLogger.DATASTORE.isDebugEnabled())
        {
            RDBMSAdapter rdba = (RDBMSAdapter) dba;

            NucleusLogger.DATASTORE.debug("Datastore Adapter : " + dba.getClass().getName());
            NucleusLogger.DATASTORE.debug("Datastore : name=\"" + rdba.getDatastoreProductName() + "\"" +
                " version=\"" + rdba.getDatastoreProductVersion() + "\"");
            NucleusLogger.DATASTORE.debug("Datastore Driver : name=\"" + rdba.getDatastoreDriverName() + "\"" +
                " version=\"" + rdba.getDatastoreDriverVersion() + "\"");

            // Connection Pooling
            ConnectionFactoryImpl txCF = (ConnectionFactoryImpl)connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
            String poolingType = txCF.getPoolingType();
            NucleusLogger.DATASTORE.debug("Transactional     Connections : Pooling = " + 
                (poolingType == null || poolingType.equals("default") ? "None" : poolingType));
            ConnectionFactoryImpl nontxCF = (ConnectionFactoryImpl)connectionMgr.lookupConnectionFactory(nontxConnectionFactoryName);
            poolingType = nontxCF.getPoolingType();
            NucleusLogger.DATASTORE.debug("Non-Transactional Connections : Pooling = " + 
                (poolingType == null || poolingType.equals("default") ? "None" : poolingType));

            if (identifierFactory != null)
            {
                NucleusLogger.DATASTORE.debug("Datastore Identifiers :" +
                    " factory=\"" + getStringProperty(PropertyNames.PROPERTY_IDENTIFIER_FACTORY) + "\"" +
                    " case=" + identifierFactory.getIdentifierCase().toString() +
                    (catalogName != null ? (" catalog=" + catalogName) : "") +
                    (schemaName != null ? (" schema=" + schemaName) : ""));
                NucleusLogger.DATASTORE.debug("Supported Identifier Cases : " +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_LOWERCASE) ? "lowercase " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_LOWERCASE_QUOTED) ? "\"lowercase\" " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_MIXEDCASE) ? "MixedCase " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_MIXEDCASE_QUOTED) ? "\"MixedCase\" " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_UPPERCASE) ? "UPPERCASE " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_UPPERCASE_QUOTED) ? "\"UPPERCASE\" " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_MIXEDCASE_SENSITIVE) ? "MixedCase-Sensitive " : "") +
                    (rdba.supportsOption(RDBMSAdapter.IDENTIFIERS_MIXEDCASE_QUOTED_SENSITIVE) ? "\"MixedCase-Sensitive\" " : ""));
                NucleusLogger.DATASTORE.debug("Supported Identifier Lengths (max) :" +
                    " Table=" + rdba.getDatastoreIdentifierMaxLength(IdentifierType.TABLE) +
                    " Column=" + rdba.getDatastoreIdentifierMaxLength(IdentifierType.COLUMN) +
                    " Constraint=" + rdba.getDatastoreIdentifierMaxLength(IdentifierType.CANDIDATE_KEY) +
                    " Index=" + rdba.getDatastoreIdentifierMaxLength(IdentifierType.INDEX) +
                    " Delimiter=" + rdba.getIdentifierQuoteString());
                NucleusLogger.DATASTORE.debug("Support for Identifiers in DDL :" +
                    " catalog=" + rdba.supportsOption(RDBMSAdapter.CATALOGS_IN_TABLE_DEFINITIONS) +
                    " schema=" + rdba.supportsOption(RDBMSAdapter.SCHEMAS_IN_TABLE_DEFINITIONS));
            }
            NucleusLogger.DATASTORE.debug("Datastore : " +
                (getBooleanProperty("datanucleus.rdbms.checkExistTablesOrViews") ? "checkTableViewExistence" : "") +
                ", rdbmsConstraintCreateMode=" + getStringProperty("datanucleus.rdbms.constraintCreateMode") +
                ", initialiseColumnInfo=" + getStringProperty("datanucleus.rdbms.initializeColumnInfo"));

            int batchLimit = getIntProperty("datanucleus.rdbms.statementBatchLimit");
            boolean supportBatching = rdba.supportsOption(RDBMSAdapter.STATEMENT_BATCHING);
            if (supportBatching)
            {
                NucleusLogger.DATASTORE.debug("Support Statement Batching : yes (max-batch-size=" + 
                    (batchLimit == -1 ? "UNLIMITED" : "" + batchLimit) + ")");
            }
            else
            {
                NucleusLogger.DATASTORE.debug("Support Statement Batching : no");
            }

            NucleusLogger.DATASTORE.debug("Queries : Results " +
                "direction=" + getStringProperty("datanucleus.rdbms.query.fetchDirection") + 
                ", type=" + getStringProperty("datanucleus.rdbms.query.resultSetType") +
                ", concurrency=" + getStringProperty("datanucleus.rdbms.query.resultSetConcurrency"));

            // JDBC Types
            NucleusLogger.DATASTORE.debug("Java-Types : string-default-length=" + getIntProperty("datanucleus.rdbms.stringDefaultLength"));
            RDBMSTypesInfo typesInfo = (RDBMSTypesInfo)schemaHandler.getSchemaData(null, "types", null);
            if (typesInfo != null && typesInfo.getNumberOfChildren() > 0)
            {
                StringBuffer typeStr = new StringBuffer();
                Iterator jdbcTypesIter = typesInfo.getChildren().keySet().iterator();
                while (jdbcTypesIter.hasNext())
                {
                    String jdbcTypeStr = (String)jdbcTypesIter.next();
                    int jdbcTypeNumber = 0;
                    try
                    {
                        jdbcTypeNumber = Short.valueOf(jdbcTypeStr).shortValue();
                    }
                    catch (NumberFormatException nfe) { };

                    String typeName = JDBCUtils.getNameForJDBCType(jdbcTypeNumber);
                    if (typeName == null)
                    {
                        typeName = "[id=" + jdbcTypeNumber + "]";
                    }
                    typeStr.append(typeName);
                    if (jdbcTypesIter.hasNext())
                    {
                        typeStr.append(", ");
                    }
                }
                NucleusLogger.DATASTORE.debug("JDBC-Types : " + typeStr);
            }

            NucleusLogger.DATASTORE.debug("===========================================================");
        }
    }

    /**
     * Release of resources
     */
    public void close()
    {
        super.close();
        classAdder = null;
    }

    /**
     * Method to return a datastore sequence for this datastore matching the passed sequence MetaData.
     * @param ec execution context
     * @param seqmd SequenceMetaData
     * @return The Sequence
     */
    public NucleusSequence getNucleusSequence(ExecutionContext ec, SequenceMetaData seqmd)
    {
        return new NucleusSequenceImpl(ec, this, seqmd);
    }

    /**
     * Method to return a NucleusConnection for the ObjectManager.
     * @param ec execution context
     * @return The NucleusConnection
     */
    public NucleusConnection getNucleusConnection(final ExecutionContext ec)
    {
        final ManagedConnection mc;
        final boolean enlisted;
        if (!ec.getTransaction().isActive())
        {
            // no active transaction so dont enlist
            enlisted = false;
        }
        else
        {
            enlisted = true;
        }
        ConnectionFactory cf = null;
        if (enlisted)
        {
            cf = connectionMgr.lookupConnectionFactory(txConnectionFactoryName);
        }
        else
        {
            cf = connectionMgr.lookupConnectionFactory(nontxConnectionFactoryName);
        }
        mc = cf.getConnection(enlisted ? ec : null, ec.getTransaction(), null); // Will throw exception if already locked

        // Lock the connection now that it is in use by the user
        mc.lock();

        Runnable closeRunnable = new Runnable()
        {
            public void run()
            {
                // Unlock the connection now that the user has finished with it
                mc.unlock();
                if (!enlisted)
                {
                    // Close the (unenlisted) connection (committing its statements)
                    try
                    {
                        ((Connection)mc.getConnection()).close();
                    }
                    catch (SQLException sqle)
                    {
                        throw new NucleusDataStoreException(sqle.getMessage());
                    }
                }
            }
        };
        NucleusConnection nc = new NucleusConnectionImpl(mc.getConnection(), closeRunnable);

        // Return own implementation of JDOConnection since we need to implement JDBC Connection too as per the spec
        return new JDOConnectionImpl(nc);
    }

    /**
     * Accessor for the SQL controller.
     * @return The SQL controller
     */
    public SQLController getSQLController()
    {
        return sqlController;
    }

    /**
     * Accessor for the SQL expression factory to use when generating SQL statements.
     * @return SQL expression factory
     */
    public SQLExpressionFactory getSQLExpressionFactory()
    {
        return expressionFactory;
    }

    /**
     * Initialises the schema name for the datastore, and (optionally) the schema table 
     * (and associated initial schema data).
     * @param conn A connection to the database
     * @param clr ClassLoader resolver
     */
    private void initialiseSchema(Connection conn, ClassLoaderResolver clr)
    throws Exception
    {
        // Initialise the Catalog/Schema names
        RDBMSAdapter rdba = (RDBMSAdapter)dba;
        if (schemaName == null && catalogName == null)
        {
            // User didn't provide catalog/schema so determine the defaults from the datastore
            // TODO Should we bother with this if the RDBMS doesn't support catalog/schema in the table identifiers ?
            try
            {
                try
                {
                    catalogName = rdba.getCatalogName(conn);
                    schemaName = rdba.getSchemaName(conn);
                }
                catch (UnsupportedOperationException e)
                {
                    if (!readOnlyDatastore && !fixedDatastore)
                    {
                        // If we aren't a read-only datastore, try to create a table and then 
                        // retrieve its details, so as to obtain the catalog, schema. 
                        ProbeTable pt = new ProbeTable(this);
                        pt.initialize(clr);
                        pt.create(conn);
                        try
                        {
                            String[] schema_details = pt.findSchemaDetails(conn);
                            if (schema_details != null)
                            {
                                catalogName = schema_details[0];
                                schemaName = schema_details[1];
                            }
                        }
                        finally
                        {
                            pt.drop(conn);
                        }
                    }
                }
            }
            catch (SQLException e)
            {
                String msg = LOCALISER_RDBMS.msg("050005", e.getMessage()) + ' ' + 
                    LOCALISER_RDBMS.msg("050006");
                NucleusLogger.DATASTORE_SCHEMA.warn(msg);
                // This is only logged as a warning since if the JDBC driver has some issue creating the ProbeTable we would be stuck
                // We need to allow SchemaTool "dbinfo" mode to work in all circumstances.
            }
        }
        // TODO If catalogName/schemaName are set convert them to the adapter case

        if (!readOnlyDatastore)
        {
            // Provide any add-ons for the datastore that may be needed later
            dba.initialiseDatastore(conn);
        }

        // AutoStarter - Load up any startup class names
        if ((readOnlyDatastore || fixedDatastore) && autoStartMechanism != null && autoStartMechanism.equals("SchemaTable"))
        {
            // Schema fixed and user requires an auto-starter needing schema content so turn it off
            autoStartMechanism = "None";
        }
        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER_RDBMS.msg("050008", catalogName, schemaName, autoStartMechanism));
        }
        initialiseAutoStart(clr);

        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            if (readOnlyDatastore)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER_RDBMS.msg("050010",
                    catalogName, schemaName, "" + storeDataMgr.size()));
            }
            else if (fixedDatastore)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER_RDBMS.msg("050011",
                        catalogName, schemaName, "" + storeDataMgr.size()));
            }
            else
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER_RDBMS.msg("050009",
                        catalogName, schemaName, "" + storeDataMgr.size()));
            }
        }
    }

    /**
     * Clears all knowledge of tables, cached requests, metadata, etc and resets
     * the store manager to its initial state.
     */
    private void clearSchemaData()
    {
        deregisterAllStoreData();

        // Clear and reinitialise the schemaHandler
        schemaHandler.clear();
        ManagedConnection mc = getConnection(-1);
        try
        {
            dba.initialiseTypes(schemaHandler, mc);
        }
        finally
        {
            mc.close();
        }

        ((RDBMSPersistenceHandler)persistenceHandler).removeAllRequests();
    }

    /**
     * Accessor for the (default) RDBMS catalog name.
     * 
     * @return The catalog name.
     */
    public String getCatalogName()
    {
        return catalogName;
    }

    /**
     * Accessor for the (default) RDBMS schema name.
     * 
     * @return The schema name.
     */
    public String getSchemaName()
    {
        return schemaName;
    }

    /**
     * Get the date/time of the datastore.
     * @return Date/time of the datastore
     */
    public Date getDatastoreDate()
    {
        Date serverDate = null;

        String dateStmt = ((RDBMSAdapter)dba).getDatastoreDateStatement();
        ManagedConnection mconn = null;
        try
        {
            mconn = getConnection(UserTransaction.TRANSACTION_NONE);

            PreparedStatement ps = null;
            ResultSet rs = null;
            try
            {
                ps = getSQLController().getStatementForQuery(mconn, dateStmt);
                rs = getSQLController().executeStatementQuery(null, mconn, dateStmt, ps);
                if (rs.next())
                {
                    // Retrieve the timestamp for the server date/time using the server TimeZone from OMF
                    // Assume that the dateStmt returns 1 column and is Timestamp
                    Timestamp time = rs.getTimestamp(1, getCalendarForDateTimezone());
                    serverDate = new Date(time.getTime());
                }
                else
                {
                    return null;
                }
            }
            catch (SQLException sqle)
            {
                String msg = LOCALISER_RDBMS.msg("050052", sqle.getMessage());
                NucleusLogger.DATASTORE.warn(msg, sqle);
                throw new NucleusUserException(msg, sqle).setFatal();
            }
            finally
            {
                if (rs != null)
                {
                    rs.close();
                }
                if (ps != null)
                {
                    getSQLController().closeStatement(mconn, ps);
                }
            }
        }
        catch (SQLException sqle)
        {
            String msg = LOCALISER_RDBMS.msg("050052", sqle.getMessage());
            NucleusLogger.DATASTORE.warn(msg, sqle);
            throw new NucleusException(msg, sqle).setFatal();
        }
        finally
        {
            mconn.close();
        }

        return serverDate;
    }

    // ----------------------------- Class Management -------------------------------

    /**
     * Method to add several persistable classes to the store manager's set of supported classes.
     * This will create any necessary database objects (tables, views, constraints, indexes etc). 
     * This will also cause the addition of any related classes.
     * @param classNames Name of the class(es) to be added.
     * @param clr The ClassLoaderResolver
     */
    public void addClasses(String[] classNames, ClassLoaderResolver clr)
    {
        synchronized (classAdditionLock)
        {
            if (classAdder != null)
            {
                // addClasses() has been recursively re-entered: just add table
                // objects for the requested classes and return.
                classAdder.addClasses(classNames, clr);
                return;
            }
            if (classNames != null && classNames.length > 0)
            {
                new ClassAdder(classNames, null).execute(clr);
            }
        }
    }

    /**
     * Utility to remove all classes that we are managing.
     * @param clr The ClassLoaderResolver
     */
    public void removeAllClasses(ClassLoaderResolver clr)
    {
        DeleteTablesSchemaTransaction deleteTablesTxn = new DeleteTablesSchemaTransaction(this,
            Connection.TRANSACTION_READ_COMMITTED, storeDataMgr);
        boolean success = true;
        try
        {
            deleteTablesTxn.execute(clr);
        }
        catch (NucleusException ne)
        {
            success = false;
            throw ne;
        }
        finally
        {
            if (success)
            {
                clearSchemaData();
            }
        }
    }

    /**
     * Accessor for the writer for DDL (if set).
     * @return DDL writer
     */
    public Writer getDdlWriter()
    {
        return ddlWriter;
    }

    /**
     * Accessor for whether we should generate complete DDL when in that mode.
     * Otherwise will generate "upgrade DDL".
     * @return Generate complete DDL ?
     */
    public boolean getCompleteDDL()
    {
        return completeDDL;
    }

    /**
     * When we are in SchemaTool DDL mode, return if the supplied statement is already present.
     * This is used to eliminate duplicate statements from a bidirectional relation.
     * @param stmt The statement
     * @return Whether we have that statement already
     */
    public boolean hasWrittenDdlStatement(String stmt)
    {
        return (writtenDdlStatements != null && writtenDdlStatements.contains(stmt));
    }

    /**
     * When we are in SchemaTool DDL mode, add a new DDL statement.
     * @param stmt The statement
     */
    public void addWrittenDdlStatement(String stmt)
    {
        if (writtenDdlStatements != null)
        {
            writtenDdlStatements.add(stmt);
        }
    }

    /**
     * Utility to validate the specified table.
     * This is useful where we have made an update to the columns in a table and want to
     * apply the updates to the datastore.
     * @param clr The ClassLoaderResolver
     */
    public void validateTable(final TableImpl table, ClassLoaderResolver clr)
    {
        ValidateTableSchemaTransaction validateTblTxn = new ValidateTableSchemaTransaction(this, 
            Connection.TRANSACTION_READ_COMMITTED, table);
        validateTblTxn.execute(clr);
    }

    // ---------------------------------------------------------------------------------------

    /**
     * Returns the class corresponding to the given object identity.
     * If the identity is an instanceof OID then returns the associated persistable class name.
     * If the identity is a SCOID, return the SCO class.
     * If the identity is a SingleFieldIdentity then returns the associated persistable class name.
     * If the object is an AppID PK, returns the associated PC class (as far as determinable).
     * If the object id is an application id and the user supplies the "ec" argument then
     * a check can be performed in the datastore where necessary.
     * @param id The identity of some object.
     * @param clr ClassLoader resolver
     * @param ec execution context (optional - to allow check inheritance level in datastore)
     * @return For datastore identity, return the class of the corresponding object.
     *      For application identity, return the class of the corresponding object.
     *      Otherwise returns null if unable to tie as the identity of a particular class.
     */
    public String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ExecutionContext ec)
    {
        // Object is a SCOID
        if (id instanceof SCOID)
        {
            return ((SCOID)id).getSCOClass();
        }

        // Generate a list of metadata for the roots of inheritance tree(s) that this identity can represent
        // Really ought to be for a single inheritance tree (hence one element in the List) but we allow for
        // a user reusing their PK class in multiple trees
        ApiAdapter api = getApiAdapter();
        List<AbstractClassMetaData> rootCmds = new ArrayList<AbstractClassMetaData>();
        if (id instanceof OID)
        {
            // Datastore Identity, so identity is an OID, and the object is of the target class or a subclass
            OID oid = (OID) id;
            AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(oid.getPcClass(), clr);
            rootCmds.add(cmd);
            if (cmd.getIdentityType() != IdentityType.DATASTORE)
            {
                throw new NucleusUserException(LOCALISER_RDBMS.msg("050022", id, cmd.getFullClassName()));
            }
        }
        else if (api.isSingleFieldIdentity(id))
        {
            // Using SingleFieldIdentity so can assume that object is of the target class or a subclass
            String className = api.getTargetClassNameForSingleFieldIdentity(id);
            AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
            rootCmds.add(cmd);
            if (cmd.getIdentityType() != IdentityType.APPLICATION || !cmd.getObjectidClass().equals(id.getClass().getName()))
            {
                throw new NucleusUserException(LOCALISER_RDBMS.msg("050022", id, cmd.getFullClassName()));
            }
        }
        else
        {
            // Find all of the class with a PK class of this type
            Collection<AbstractClassMetaData> pkCmds =
                getMetaDataManager().getClassMetaDataWithApplicationId(id.getClass().getName());
            if (pkCmds != null && pkCmds.size() > 0)
            {
                Iterator<AbstractClassMetaData> iter = pkCmds.iterator();
                while (iter.hasNext())
                {
                    AbstractClassMetaData pkCmd = iter.next();

                    AbstractClassMetaData cmdToSwap = null;
                    boolean toAdd = true;

                    Iterator<AbstractClassMetaData> rootCmdIterator = rootCmds.iterator();
                    while (rootCmdIterator.hasNext())
                    {
                        AbstractClassMetaData rootCmd = rootCmdIterator.next();
                        if (rootCmd.isDescendantOf(pkCmd))
                        {
                            // This cmd is a parent of an existing, so swap them
                            cmdToSwap = rootCmd;
                            toAdd = false;
                            break;
                        }
                        else if (pkCmd.isDescendantOf(rootCmd))
                        {
                            toAdd = false;
                        }
                    }

                    if (cmdToSwap != null)
                    {
                        rootCmds.remove(cmdToSwap);
                        rootCmds.add(pkCmd);
                    }
                    else if (toAdd)
                    {
                        rootCmds.add(pkCmd);
                    }
                }
            }

            if (rootCmds.size() == 0)
            {
                return null;
            }
        }

        AbstractClassMetaData rootCmd = rootCmds.get(0);
        if (ec != null)
        {
            // Perform a check on the exact object inheritance level with this key (uses SQL query)
            if (rootCmds.size() == 1)
            {
                if (!rootCmd.isImplementationOfPersistentDefinition())
                {
                    // Not persistent interface implementation so check if only class using this table
                    HashSet subclasses = getSubClassesForClass(rootCmd.getFullClassName(), true, clr);
                    if (subclasses == null || subclasses.isEmpty())
                    {
                        DatastoreClass primaryTable = getDatastoreClass(rootCmd.getFullClassName(), clr);
                        String[] managedClassesInTable = primaryTable.getManagedClasses();
                        if (managedClassesInTable.length == 1 && managedClassesInTable[0].equals(rootCmd.getFullClassName()))
                        {
                            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                            {
                                NucleusLogger.PERSISTENCE.debug("Sole candidate for id is " +
                                    rootCmd.getFullClassName() + " and has no subclasses, so returning without checking datastore");
                            }
                            return rootCmd.getFullClassName();
                        }
                    }
                }

                // Simple candidate query of this class and subclasses
                if (rootCmd.hasDiscriminatorStrategy())
                {
                    // Query using discriminator
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug("Performing query using discriminator on " +
                            rootCmd.getFullClassName() + " and its subclasses to find the class of " + id);
                    }
                    return RDBMSStoreHelper.getClassNameForIdUsingDiscriminator(this, ec, id, rootCmd);
                }
                else
                {
                    // Query using UNION
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug("Performing query using UNION on " +
                            rootCmd.getFullClassName() + " and its subclasses to find the class of " + id);
                    }
                    return RDBMSStoreHelper.getClassNameForIdUsingUnion(this, ec, id, rootCmds);
                }
            }
            else
            {
                // Multiple possible roots so use UNION statement
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    StringBuffer str = new StringBuffer();
                    Iterator<AbstractClassMetaData> rootCmdIter = rootCmds.iterator();
                    while (rootCmdIter.hasNext())
                    {
                        AbstractClassMetaData cmd = rootCmdIter.next();
                        str.append(cmd.getFullClassName());
                        if (rootCmdIter.hasNext())
                        {
                            str.append(",");
                        }
                    }
                    NucleusLogger.PERSISTENCE.debug("Performing query using UNION on " +
                        str.toString() + " and their subclasses to find the class of " + id);
                }
                return RDBMSStoreHelper.getClassNameForIdUsingUnion(this, ec, id, rootCmds);
            }
        }
        else
        {
            // Check not possible so just return the first root
            if (rootCmds.size() > 1)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug("Id \""+id+"\" has been determined to be the id of class "+
                        rootCmd.getFullClassName() + " : this is the first of " + rootCmds.size() + " possible" +
                        ", but unable to determine further");
                }
                return rootCmd.getFullClassName();
            }
            else
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug("Id \""+id+"\" has been determined to be the id of class "+
                        rootCmd.getFullClassName() + " : unable to determine if actually of a subclass");
                }
                return rootCmd.getFullClassName();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.mapped.MappedStoreManager#getFieldManagerForResultProcessing(org.datanucleus.store.ObjectProvider, java.lang.Object, org.datanucleus.store.mapped.StatementClassMapping)
     */
    @Override
    public FieldManager getFieldManagerForResultProcessing(ObjectProvider sm, Object resultSet, StatementClassMapping resultMappings)
    {
        return new ResultSetGetter(this, sm, resultSet, resultMappings);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.mapped.MappedStoreManager#getFieldManagerForResultProcessing(org.datanucleus.store.ExecutionContext, java.lang.Object, org.datanucleus.store.mapped.StatementClassMapping, org.datanucleus.metadata.AbstractClassMetaData)
     */
    @Override
    public FieldManager getFieldManagerForResultProcessing(ExecutionContext ec, Object resultSet, StatementClassMapping resultMappings,
            AbstractClassMetaData cmd)
    {
        return new ResultSetGetter(this, ec, resultSet, resultMappings, cmd);
    }

    /**
     * Method to return a FieldManager for populating information in statements.
     * @param sm The state manager for the object.
     * @param stmt The Prepared Statement to set values on.
     * @param stmtMappings the index of parameters/mappings
     * @param checkNonNullable Whether to check for nullability
     * @return The FieldManager to use
     */
    public FieldManager getFieldManagerForStatementGeneration(ObjectProvider sm, Object stmt,
        StatementClassMapping stmtMappings, boolean checkNonNullable)
    {
        return new ParameterSetter(sm, stmt, stmtMappings, true);
    }

    /**
     * Method to return the value from the results for the mapping at the specified position.
     * @param resultSet The results
     * @param mapping The mapping
     * @param position The position in the results
     * @return The value at that position
     * @throws NucleusDataStoreException if an error occurs accessing the results
     */
    public Object getResultValueAtPosition(Object resultSet, JavaTypeMapping mapping, int position)
    {
        try
        {
            return ((ResultSet)resultSet).getObject(position);
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(sqle.getMessage(), sqle);
        }
    }

    /**
     * Accessor for the next value from the specified generator.
     * This implementation caters for datastore-specific generators and provides synchronisation
     * on the connection to the datastore.
     * @param generator The generator
     * @param ec execution context
     * @return The next value.
     */
    protected Object getStrategyValueForGenerator(ValueGenerator generator, final ExecutionContext ec)
    {
        Object oid = null;
        synchronized (generator)
        {
            // Get the next value for this generator for this ObjectManager
            // Note : this is synchronised since we dont want to risk handing out this generator
            // while its connectionProvider is set to that of a different ObjectManager
            // It maybe would be good to change ValueGenerator to have a next taking the connectionProvider
            if (generator instanceof AbstractDatastoreGenerator)
            {
                // RDBMS-based generator so set the connection provider
                final RDBMSStoreManager thisStoreMgr = this;
                ValueGenerationConnectionProvider connProvider = new ValueGenerationConnectionProvider()
                {
                    ManagedConnection mconn;
                    public ManagedConnection retrieveConnection()
                    {
                        if (getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE).equalsIgnoreCase("UsePM"))
                        {
                            this.mconn = thisStoreMgr.getConnection(ec);
                        }
                        else
                        {
                            int isolationLevel = TransactionUtils.getTransactionIsolationLevelForName(
                                getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION));
                            this.mconn = thisStoreMgr.getConnection(isolationLevel);
                        }
                        return mconn;
                    }

                    public void releaseConnection()
                    {
                        try
                        {
                            if (getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE).equalsIgnoreCase("UsePM"))
                            {
                                mconn.release();
                            }
                            else
                            {
                                mconn.close();
                            }
                            mconn = null;
                        }
                        catch (NucleusException e)
                        {
                            String msg = LOCALISER_RDBMS.msg("050025", e);
                            NucleusLogger.VALUEGENERATION.error(msg);
                            throw new NucleusDataStoreException(msg, e);
                        }
                    }
                };
                ((AbstractDatastoreGenerator)generator).setConnectionProvider(connProvider);
            }
            oid = generator.next();
        }
        return oid;
    }

    /**
     * Method to return the properties to pass to the generator for the specified field.
     * @param cmd MetaData for the class
     * @param absoluteFieldNumber Number of the field (-1 = datastore identity)
     * @param ec execution context
     * @param seqmd Any sequence metadata
     * @param tablegenmd Any table generator metadata
     * @return The properties to use for this field
     */
    protected Properties getPropertiesForGenerator(AbstractClassMetaData cmd, int absoluteFieldNumber,
            ExecutionContext ec, SequenceMetaData seqmd, TableGeneratorMetaData tablegenmd)
    {
        AbstractMemberMetaData mmd = null;
        IdentityStrategy strategy = null;
        String sequence = null;
        ExtensionMetaData[] extensions = null;
        if (absoluteFieldNumber >= 0)
        {
            // real field
            mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absoluteFieldNumber);
            strategy = mmd.getValueStrategy();
            sequence = mmd.getSequence();
            extensions = mmd.getExtensions();
        }
        else
        {
            // datastore-identity surrogate field
            // always use the root IdentityMetaData since the root class defines the identity
            IdentityMetaData idmd = cmd.getBaseIdentityMetaData();
            strategy = idmd.getValueStrategy();
            sequence = idmd.getSequence();
            extensions = idmd.getExtensions();
        }

        // Get base table with the required field
        DatastoreClass tbl = getDatastoreClass(cmd.getBaseAbstractClassMetaData().getFullClassName(),
            ec.getClassLoaderResolver());
        if (tbl == null)
        {
            tbl = getTableForStrategy(cmd,absoluteFieldNumber,ec.getClassLoaderResolver());
        }
        JavaTypeMapping m = null;
        if (mmd != null)
        {
            m = tbl.getMemberMapping(mmd);
            if (m == null)
            {
                // Field not mapped in root table so use passed-in table
                tbl = getTableForStrategy(cmd,absoluteFieldNumber,ec.getClassLoaderResolver());
                m = tbl.getMemberMapping(mmd);
            }
        }
        else
        {
            m = tbl.getIdMapping();
        }
        StringBuffer columnsName = new StringBuffer();
        for (int i = 0; i < m.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                columnsName.append(",");
            }
            columnsName.append(m.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
        }

        Properties properties = new Properties();
        properties.setProperty("class-name", cmd.getFullClassName());
        properties.put("root-class-name", cmd.getBaseAbstractClassMetaData().getFullClassName());
        if (mmd != null)
        {
            properties.setProperty("field-name", mmd.getFullFieldName());
        }
        if (cmd.getCatalog() != null)
        {
            properties.setProperty("catalog-name", cmd.getCatalog());
        }
        if (cmd.getSchema() != null)
        {
            properties.setProperty("schema-name", cmd.getSchema());
        }
        properties.setProperty("table-name", tbl.getIdentifier().toString());
        properties.setProperty("column-name", columnsName.toString());

        if (sequence != null)
        {
            properties.setProperty("sequence-name", sequence);
        }

        // Add any extension properties
        if (extensions != null)
        {
            for (int i=0;i<extensions.length;i++)
            {
                properties.put(extensions[i].getKey(), extensions[i].getValue());
            }
        }

        if (strategy == IdentityStrategy.INCREMENT && tablegenmd != null)
        {
            // User has specified a TableGenerator (JPA)
            properties.put("key-initial-value", "" + tablegenmd.getInitialValue());
            properties.put("key-cache-size", "" + tablegenmd.getAllocationSize());
            if (tablegenmd.getTableName() != null)
            {
                properties.put("sequence-table-name", tablegenmd.getTableName());
            }
            if (tablegenmd.getCatalogName() != null)
            {
                properties.put("sequence-catalog-name", tablegenmd.getCatalogName());
            }
            if (tablegenmd.getSchemaName() != null)
            {
                properties.put("sequence-schema-name", tablegenmd.getSchemaName());
            }
            if (tablegenmd.getPKColumnName() != null)
            {
                properties.put("sequence-name-column-name", tablegenmd.getPKColumnName());
            }
            if (tablegenmd.getPKColumnName() != null)
            {
                properties.put("sequence-nextval-column-name", tablegenmd.getValueColumnName());
            }
            if (tablegenmd.getPKColumnValue() != null)
            {
                properties.put("sequence-name", tablegenmd.getPKColumnValue());
            }

            // Using JPA generator so don't enable initial value detection
            properties.remove("table-name");
            properties.remove("column-name");
        }
        else if (strategy == IdentityStrategy.INCREMENT && tablegenmd == null)
        {
            if (!properties.containsKey("key-cache-size"))
            {
                // Use default allocation size
                int allocSize = getIntProperty(PropertyNames.PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE);
                properties.put("key-cache-size", "" + allocSize);
            }
        }
        else if (strategy == IdentityStrategy.SEQUENCE && seqmd != null)
        {
            // User has specified a SequenceGenerator (JDO/JPA)
            if (StringUtils.isWhitespace(sequence))
            {
                // Apply default to sequence name, as name of sequence metadata
                if (seqmd != null && seqmd.getName() != null)
                {
                    properties.put("sequence-name", seqmd.getName());
                }
            }
            if (seqmd.getDatastoreSequence() != null)
            {
                if (seqmd.getInitialValue() >= 0)
                {
                    properties.put("key-initial-value", "" + seqmd.getInitialValue());
                }
                if (seqmd.getAllocationSize() > 0)
                {
                    properties.put("key-cache-size", "" + seqmd.getAllocationSize());
                }
                else
                {
                    // Use default allocation size
                    int allocSize = getIntProperty(PropertyNames.PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE);
                    properties.put("key-cache-size", "" + allocSize);
                }
                properties.put("sequence-name", "" + seqmd.getDatastoreSequence());

                // Add on any extensions specified on the sequence
                ExtensionMetaData[] seqExtensions = seqmd.getExtensions();
                if (seqExtensions != null)
                {
                    for (int i=0;i<seqExtensions.length;i++)
                    {
                        properties.put(seqExtensions[i].getKey(), seqExtensions[i].getValue());
                    }
                }
            }
            else
            {
                // JDO Factory-based sequence generation
                // TODO Support this
            }
        }
        return properties;
    }

    private DatastoreClass getTableForStrategy(AbstractClassMetaData cmd, int fieldNumber, ClassLoaderResolver clr)
    {
        DatastoreClass t = getDatastoreClass(cmd.getFullClassName(), clr);
        if (t == null && cmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
        {
            throw new NucleusUserException(LOCALISER.msg("032013", cmd.getFullClassName()));
        }

        if (fieldNumber>=0)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            t = t.getBaseDatastoreClassWithMember(mmd);
        }
        else if (t!=null)
        {
            // Go up to overall superclass to find id for that class.
            boolean has_superclass = true;
            while (has_superclass)
            {
                DatastoreClass supert = t.getSuperDatastoreClass();
                if (supert != null)
                {
                    t = supert;
                }
                else
                {
                    has_superclass = false;
                }
            }
        }
        return t;
    }

    /**
     * Method defining which value-strategy to use when the user specifies "native".
     * @return Should be overridden by all store managers that have other behaviour.
     */
    protected String getStrategyForNative(AbstractClassMetaData cmd, int absFieldNumber)
    {
        if (getBooleanProperty("datanucleus.rdbms.useLegacyNativeValueStrategy"))
        {
            // Use legacy process for deciding which strategy to use
            String sequence = null;
            if (absFieldNumber >= 0)
            {
                // real field
                sequence = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absFieldNumber).getSequence();
            }
            else
            {
                // datastore-identity surrogate field
                sequence = cmd.getIdentityMetaData().getSequence();
            }

            if (dba.supportsOption(DatastoreAdapter.SEQUENCES) && sequence != null)
            {
                return "sequence";
            }
            else
            {
                return "table-sequence"; // Maybe ought to use "increment"
            }
        }
        else
        {
            return super.getStrategyForNative(cmd, absFieldNumber);
        }
    }

    /**
     * Accessor for the (default) SQL type info for the specified JDBC type
     * @param jdbcType JDBC type
     * @return (default) SQL type
     * @throws UnsupportedDataTypeException If the JDBC type is not found
     */
    public SQLTypeInfo getSQLTypeInfoForJDBCType(int jdbcType)
    throws UnsupportedDataTypeException
    {
        return this.getSQLTypeInfoForJDBCType(jdbcType, "DEFAULT");
    }

    /**
     * Accessor for the (default) SQL type info for the specified JDBC type
     * @param jdbcType JDBC type
     * @param sqlType The SQL type (if known, otherwise uses the default for this JDBC type).
     * @return SQL type
     * @throws UnsupportedDataTypeException If the JDBC type is not found
     */
    public SQLTypeInfo getSQLTypeInfoForJDBCType(int jdbcType, String sqlType)
    throws UnsupportedDataTypeException
    {
        // NB The connection first arg is not required since will be cached from initialisation stage
        RDBMSTypesInfo typesInfo = (RDBMSTypesInfo)schemaHandler.getSchemaData(null, "types", null);
        JDBCTypeInfo jdbcTypeInfo = (JDBCTypeInfo)typesInfo.getChild("" + jdbcType);
        if (jdbcTypeInfo.getNumberOfChildren() == 0)
        {
            throw new UnsupportedDataTypeException(LOCALISER.msg("051005",
                JDBCUtils.getNameForJDBCType(jdbcType)));
        }

        return (SQLTypeInfo)jdbcTypeInfo.getChild(sqlType != null ? sqlType : "DEFAULT");
    }

    /**
     * Returns the column info for a column name. This should be used instead
     * of making direct calls to DatabaseMetaData.getColumns().
     * <p>
     * Where possible, this method loads and caches column info for more than
     * just the table being requested, improving performance by reducing the
     * overall number of calls made to DatabaseMetaData.getColumns() (each of
     * which usually results in one or more database queries).
     * </p>
     * @param table The table/view
     * @param conn JDBC connection to the database.
     * @param column the column
     * @return The ColumnInfo objects describing the column.
     * @throws SQLException
     */
    public RDBMSColumnInfo getColumnInfoForColumnName(Table table, Connection conn, DatastoreIdentifier column)
    throws SQLException
    {
        RDBMSColumnInfo colInfo = (RDBMSColumnInfo)schemaHandler.getSchemaData(
            conn, "column", new Object[] {table, column.getIdentifierName()});
        return colInfo;
    }

    /**
     * Returns the column info for a database table. This should be used instead
     * of making direct calls to DatabaseMetaData.getColumns().
     * <p>
     * Where possible, this method loads and caches column info for more than
     * just the table being requested, improving performance by reducing the
     * overall number of calls made to DatabaseMetaData.getColumns() (each of
     * which usually results in one or more database queries).
     * </p>
     * @param table The table/view
     * @param conn JDBC connection to the database.
     * @return A list of ColumnInfo objects describing the columns of the table.
     * The list is in the same order as was supplied by getColumns(). If no
     * column info is found for the given table, an empty list is returned.
     * @throws SQLException
     */
    public List getColumnInfoForTable(Table table, Connection conn)
    throws SQLException
    {
        RDBMSTableInfo tableInfo = (RDBMSTableInfo)schemaHandler.getSchemaData(conn, "columns",
            new Object[] {table});
        if (tableInfo == null)
        {
            return Collections.EMPTY_LIST;
        }

        List cols = new ArrayList(tableInfo.getNumberOfChildren());
        cols.addAll(tableInfo.getChildren());
        return cols;
    }

    /**
     * Method to invalidate the cached column info for a table.
     * This is called when we have just added columns to the table in the schema
     * has the effect of a reload of the tables information the next time it is needed.
     * @param table The table
     */
    public void invalidateColumnInfoForTable(Table table)
    {
        RDBMSSchemaInfo schemaInfo = (RDBMSSchemaInfo)schemaHandler.getSchemaData(null, "tables", null);
        if (schemaInfo != null && schemaInfo.getNumberOfChildren() > 0)
        {
            schemaInfo.getChildren().remove(table.getIdentifier().getFullyQualifiedName(true));
        }
    }

    /**
     * Convenience accessor of the Table objects managed in this datastore at this point.
     * @param catalog Name of the catalog to restrict the collection by
     * @param schema Name of the schema to restrict the collection by
     * @return Collection<Table>
     */
    public Collection getManagedTables(String catalog, String schema)
    {
        if (storeDataMgr == null)
        {
            return Collections.EMPTY_SET;
        }

        Collection tables = new HashSet();
        for (Iterator<StoreData> i = storeDataMgr.getManagedStoreData().iterator(); i.hasNext();)
        {
            RDBMSStoreData sd = (RDBMSStoreData) i.next();
            if (sd.getDatastoreContainerObject() != null)
            {
                // Catalog/Schema match if either managed table not set, or input requirements not set
                DatastoreIdentifier identifier = sd.getDatastoreContainerObject().getIdentifier();
                boolean catalogMatches = true;
                boolean schemaMatches = true;
                if (catalog != null && identifier.getCatalogName() != null &&
                    !catalog.equals(identifier.getCatalogName()))
                {
                    catalogMatches = false;
                }
                if (schema != null && identifier.getSchemaName() != null &&
                    !schema.equals(identifier.getSchemaName()))
                {
                    schemaMatches = false;
                }
                if (catalogMatches && schemaMatches)
                {
                    tables.add(sd.getDatastoreContainerObject());
                }
            }
        }
        return tables;
    }

    /**
     * Resolves an identifier macro. The public fields <var>className</var>, <var>fieldName </var>,
     * and <var>subfieldName </var> of the given macro are taken as inputs, and the public
     * <var>value </var> field is set to the SQL identifier of the corresponding database table or column.
     * @param im The macro to resolve.
     * @param clr The ClassLoaderResolver
     */
    public void resolveIdentifierMacro(MacroString.IdentifierMacro im, ClassLoaderResolver clr)
    {
        DatastoreClass ct = getDatastoreClass(im.className, clr);
        if (im.fieldName == null)
        {
            im.value = ct.getIdentifier().toString();
            return;
        }

        JavaTypeMapping m;
        if (im.fieldName.equals("this")) // TODO This should be candidate alias or something, not hardcoded "this"
        {
            if (!(ct instanceof ClassTable))
            {
                throw new NucleusUserException(LOCALISER_RDBMS.msg("050034", im.className));
            }

            if (im.subfieldName != null)
            {
                throw new NucleusUserException(LOCALISER_RDBMS.msg("050035", im.className, im.fieldName, im.subfieldName));
            }
            m = ((DatastoreContainerObject) ct).getIdMapping();
        }
        else
        {
            AbstractMemberMetaData mmd = getMetaDataManager().getMetaDataForMember(im.className, im.fieldName, clr);
            m = ct.getMemberMapping(mmd);
            DatastoreContainerObject t = getDatastoreContainerObject(mmd);
            if (im.subfieldName == null)
            {
                if (t != null)
                {
                    im.value = t.getIdentifier().toString();
                    return;
                }
            }
            else
            {
                if (t instanceof CollectionTable)
                {
                    CollectionTable collTable = (CollectionTable) t;
                    if (im.subfieldName.equals("owner"))
                    {
                        m = collTable.getOwnerMapping();
                    }
                    else if (im.subfieldName.equals("element"))
                    {
                        m = collTable.getElementMapping();
                    }
                    else if (im.subfieldName.equals("index"))
                    {
                        m = collTable.getOrderMapping();
                    }
                    else
                    {
                        throw new NucleusUserException(LOCALISER_RDBMS.msg(
                            "050036", im.subfieldName, im));
                    }
                }
                else if (t instanceof MapTable)
                {
                    MapTable mt = (MapTable) t;
                    if (im.subfieldName.equals("owner"))
                    {
                        m = mt.getOwnerMapping();
                    }
                    else if (im.subfieldName.equals("key"))
                    {
                        m = mt.getKeyMapping();
                    }
                    else if (im.subfieldName.equals("value"))
                    {
                        m = mt.getValueMapping();
                    }
                    else
                    {
                        throw new NucleusUserException(LOCALISER_RDBMS.msg(
                                "050037",
                                im.subfieldName, im));
                    }
                }
                else
                {
                    throw new NucleusUserException(LOCALISER_RDBMS.msg(
                            "050035", im.className,
                            im.fieldName, im.subfieldName));
                }
            }
        }
        im.value = ((Column)m.getDatastoreMapping(0).getDatastoreField()).getIdentifier().toString();
    }

    /**
     * Method to output particular information owned by this datastore.
     * Supports "DATASTORE" and "SCHEMA" categories.
     * @param category Category of information
     * @param ps PrintStream
     * @throws Exception Thrown if an error occurs in the output process
     */
    public void printInformation(String category, PrintStream ps)
    throws Exception
    {
        RDBMSAdapter dba = (RDBMSAdapter) getDatastoreAdapter();

        super.printInformation(category, ps);

        if (category.equalsIgnoreCase("DATASTORE"))
        {
            ps.println(dba.toString());
            ps.println();
            ps.println("Database TypeInfo");

            RDBMSTypesInfo typesInfo = (RDBMSTypesInfo)schemaHandler.getSchemaData(null, "types", null);
            if (typesInfo != null)
            {
                Iterator iter = typesInfo.getChildren().keySet().iterator();
                while (iter.hasNext())
                {
                    String jdbcTypeStr = (String)iter.next();
                    short jdbcTypeNumber = 0;
                    try
                    {
                        jdbcTypeNumber = Short.valueOf(jdbcTypeStr).shortValue();
                    }
                    catch (NumberFormatException nfe) { }
                    JDBCTypeInfo jdbcType = (JDBCTypeInfo)typesInfo.getChild(jdbcTypeStr);
                    Collection sqlTypeNames = jdbcType.getChildren().keySet();

                    // SQL type names for JDBC type
                    String typeStr = "JDBC Type=" + JDBCUtils.getNameForJDBCType(jdbcTypeNumber) +
                        " sqlTypes=" + StringUtils.collectionToString(sqlTypeNames);
                    ps.println(typeStr);

                    // Default SQL type details
                    SQLTypeInfo sqlType = (SQLTypeInfo)jdbcType.getChild("DEFAULT");
                    ps.println(sqlType);
                }
            }
            ps.println("");

            // Print out the keywords info
            ps.println("Database Keywords");

            Iterator reservedWordsIter = dba.iteratorReservedWords();
            while (reservedWordsIter.hasNext())
            {
                Object words = reservedWordsIter.next();
                ps.println(words);
            }
            ps.println("");
        }
        else if (category.equalsIgnoreCase("SCHEMA"))
        {
            ps.println(dba.toString());
            ps.println();
            ps.println("TABLES");

            ManagedConnection mc = getConnection(-1);
            try
            {
                Connection conn = (Connection)mc.getConnection();
                RDBMSSchemaInfo schemaInfo = (RDBMSSchemaInfo)schemaHandler.getSchemaData(
                    conn, "tables", new Object[] {this.catalogName, this.schemaName});
                if (schemaInfo != null)
                {
                    Iterator tableIter = schemaInfo.getChildren().values().iterator();
                    while (tableIter.hasNext())
                    {
                        // Print out the table information
                        RDBMSTableInfo tableInfo = (RDBMSTableInfo)tableIter.next();
                        ps.println(tableInfo);

                        Iterator columnIter = tableInfo.getChildren().iterator();
                        while (columnIter.hasNext())
                        {
                            // Print out the column information
                            RDBMSColumnInfo colInfo = (RDBMSColumnInfo)columnIter.next();
                            ps.println(colInfo);
                        }
                    }
                }
            }
            finally
            {
                if (mc != null)
                {
                    mc.close();
                }
            }
            ps.println("");
        }
    }

    /**
     * Called by (container) Mapping objects to request the creation of a DatastoreObject (table).
     * If the specified field doesn't require a join table then this returns null.
     * If the join table already exists, then this returns it.
     * @param mmd The metadata describing the field/property.
     * @param clr The ClassLoaderResolver
     * @return The container object (SetTable/ListTable/MapTable/ArrayTable)
     */
    public synchronized DatastoreContainerObject newJoinDatastoreContainerObject(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        if (mmd.getJoinMetaData() == null)
        {
            AbstractMemberMetaData[] relatedMmds = mmd.getRelatedMemberMetaData(clr);
            if (relatedMmds != null && relatedMmds[0].getJoinMetaData() != null)
            {
                // Join specified at other end of a bidirectional relation so create a join table
            }
            else
            {
                Class element_class;
                if (mmd.hasCollection())
                {
                    element_class = clr.classForName(mmd.getCollection().getElementType());
                }
                else if (mmd.hasMap())
                {
                    MapMetaData mapmd = (MapMetaData)mmd.getContainer();
                    if (mmd.getValueMetaData() != null && mmd.getValueMetaData().getMappedBy() != null)
                    {
                        // value stored in the key table
                        element_class = clr.classForName(mapmd.getKeyType());
                    }
                    else if (mmd.getKeyMetaData() != null && mmd.getKeyMetaData().getMappedBy() != null)
                    {
                        // key stored in the value table
                        element_class = clr.classForName(mapmd.getValueType());
                    }
                    else
                    {
                        // No information given for what is stored in what, so throw it back to the user to fix the input :-)
                        throw new NucleusUserException(LOCALISER_RDBMS.msg("050050", mmd.getFullFieldName()));
                    }
                }
                else if (mmd.hasArray())
                {
                    element_class = clr.classForName(mmd.getTypeName()).getComponentType();
                }
                else
                {
                    // N-1 using join table ?
                    // what is this? should not happen
                    return null;
                }

                // Check that the element class has MetaData
                if (getMetaDataManager().getMetaDataForClass(element_class, clr) != null)
                {
                    // FK relationship, so no join table
                    return null;
                }
                else if (ClassUtils.isReferenceType(element_class))
                {
                    // reference type using FK relationship so no join table
                    return null;
                }

                // Trap all non-PC elements that haven't had a join table specified but need one
                throw new NucleusUserException(LOCALISER_RDBMS.msg("050049",
                    mmd.getFullFieldName(), mmd.toString()));
            }
        }

        // Check if the join table already exists
        DatastoreContainerObject joinTable = getDatastoreContainerObject(mmd);
        if (joinTable != null)
        {
            return joinTable;
        }

        // Create a new join table for the container
        if (classAdder == null)
        {
            throw new IllegalStateException(LOCALISER_RDBMS.msg("050016"));
        }

        if (mmd.getType().isArray())
        {
            // Use Array table for array types
            return classAdder.addJoinTableForContainer(mmd, clr, ClassAdder.JOIN_TABLE_ARRAY);
        }
        else if (Map.class.isAssignableFrom(mmd.getType()))
        {
            // Use Map join table for supported map types
            return classAdder.addJoinTableForContainer(mmd, clr, ClassAdder.JOIN_TABLE_MAP);
        }
        else if (Collection.class.isAssignableFrom(mmd.getType()))
        {
            // Use Collection join table for collection/set types
            return classAdder.addJoinTableForContainer(mmd, clr, ClassAdder.JOIN_TABLE_COLLECTION);
        }
        else
        {
            // N-1 uni join
            return classAdder.addJoinTableForContainer(mmd, clr, ClassAdder.JOIN_TABLE_PERSISTABLE);
        }
    }

    /**
     * A schema transaction that adds a set of classes to the RDBMSManager,
     * making them usable for persistence.
     * <p>
     * This class embodies the work necessary to activate a persistent class and
     * ready it for storage management. It is the primary mutator of a RDBMSManager.
     * </p>
     * <p>
     * Adding classes is an involved process that includes the creation and/or
     * validation in the database of tables, views, and table constraints, and
     * their corresponding Java objects maintained by the RDBMSManager. Since
     * it's a management transaction, the entire process is subject to retry on
     * SQLExceptions. It is responsible for ensuring that the procedure either
     * adds <i>all </i> of the requested classes successfully, or adds none of
     * them and preserves the previous state of the RDBMSManager exactly as it was.
     * </p>
     */
    private class ClassAdder extends AbstractSchemaTransaction
    {
        /** join table for Collection. **/
        public static final int JOIN_TABLE_COLLECTION = 1;
        /** join table for Map. **/
        public static final int JOIN_TABLE_MAP = 2;
        /** join table for Array. **/
        public static final int JOIN_TABLE_ARRAY = 3;
        /** join table for persistable. **/
        public static final int JOIN_TABLE_PERSISTABLE = 4;

        /** Optional writer to dump the DDL for any classes being added. */
        private Writer ddlWriter = null;

        /** Whether to check if table/view exists */
        protected final boolean checkExistTablesOrViews;

        /** tracks the SchemaData currrently being added - used to rollback the AutoStart added classes **/
        private HashSet<RDBMSStoreData> schemaDataAdded = new HashSet();

        private final String[] classNames;

        /**
         * Constructs a new class adder transaction that will add the given classes to the RDBMSManager.
         * @param classNames Names of the (initial) class(es) to be added.
         * @param writer Optional writer for DDL when we want the DDL outputting to file instead of creating the tables
         */
        private ClassAdder(String[] classNames, Writer writer)
        {
            super(RDBMSStoreManager.this, ((RDBMSAdapter)dba).getTransactionIsolationForSchemaCreation());
            this.ddlWriter = writer;
            this.classNames = getNucleusContext().getTypeManager().filterOutSupportedSecondClassNames(classNames);

            checkExistTablesOrViews = RDBMSStoreManager.this.getBooleanProperty("datanucleus.rdbms.checkExistTablesOrViews");
        }

        /**
         * Method to give a string version of this object.
         * @return The String version of this object.
         */
        public String toString()
        {
            return LOCALISER_RDBMS.msg("050038", catalogName, schemaName);
        }

        /**
         * Method to perform the action using the specified connection to the datastore.
         * @param clr the ClassLoaderResolver
         * @throws SQLException Thrown if an error occurs in execution.
         */
        protected void run(ClassLoaderResolver clr)
        throws SQLException
        {
            if (classNames.length == 0)
            {
                return;
            }

            synchronized (classAdditionLock)
            {
                classAdder = this;
                try
                {
                    addClassTablesAndValidate(classNames, clr);
                }
                finally
                {
                    classAdder = null;
                }
            }
        }

        /**
         * Called by RDBMSManager.addClasses() when it has been recursively
         * re-entered. This just adds table objects for the requested additional 
         * classes and returns.
         * @param classNames Names of the (additional) class(es) to be added.
         * @param clr the ClassLoaderResolver
         */
        private void addClasses(String[] classNames, ClassLoaderResolver clr)
        {
            // Filter out any supported classes
            classNames = getNucleusContext().getTypeManager().filterOutSupportedSecondClassNames(classNames);
            if (classNames.length == 0)
            {
                return;
            }

            try
            {
                if (getCurrentConnection() == null)
                {
                    throw new IllegalStateException(LOCALISER_RDBMS.msg("050039"));
                }
            }
            catch(SQLException e)
            {
                throw new NucleusDataStoreException("SQL exception: " + this, e);
            }

            // Add the tables for these additional classes
            addClassTables(classNames, clr);
        }
        
        private int addClassTablesRecursionCounter = 0;

        /**
         * Adds a new table object (ie ClassTable or ClassView) for every
         * class in the given list. These classes
         * <ol>
         * <li>require a table</li>
         * <li>do not yet have a table initialized in the store manager.</li>
         * </ol>
         * <p>
         * This doesn't initialize or validate the tables, it just adds the
         * table objects to the RDBMSManager's internal data structures.
         * </p>
         *
         * @param classNames Names of class(es) whose tables are to be added.
         * @param clr the ClassLoaderResolver
         */
        private void addClassTables(String[] classNames, ClassLoaderResolver clr)
        {
            addClassTablesRecursionCounter += 1;
            try
            {
                Iterator iter = getMetaDataManager().getReferencedClasses(classNames, clr).iterator();
                try
                {
                    if (starter != null && starterInitialised && !starter.isOpen())
                    {
                        starter.open();
                    }

                    // Pass through the classes and create necessary tables
                    while (iter.hasNext())
                    {
                        ClassMetaData cmd = (ClassMetaData) iter.next();
                        addClassTable(cmd, clr);
                    }

                    // For data where the table wasn't defined, make a second pass.
                    // This is necessary where a subclass uses "superclass-table" and the superclass' table
                    // hadn't been defined at the point of adding this class
                    Iterator<RDBMSStoreData> addedIter = new HashSet(this.schemaDataAdded).iterator();
                    while (addedIter.hasNext())
                    {
                        RDBMSStoreData data = addedIter.next();
                        if (data.getDatastoreContainerObject() == null && data.isFCO())
                        {
                            AbstractClassMetaData cmd = (AbstractClassMetaData) data.getMetaData();
                            InheritanceMetaData imd = cmd.getInheritanceMetaData();
                            if (imd.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
                            {
                                AbstractClassMetaData[] managingCmds = getClassesManagingTableForClass(cmd, clr);
                                DatastoreClass superTable = null;
                                if (managingCmds != null && managingCmds.length == 1)
                                {
                                    RDBMSStoreData superData =
                                        (RDBMSStoreData) storeDataMgr.get(managingCmds[0].getFullClassName());

                                    // Assert that managing class is in the set of storeDataByClass
                                    if (superData == null)
                                    {
                                        this.addClassTables(new String[]{managingCmds[0].getFullClassName()}, clr);
                                        superData = (RDBMSStoreData) storeDataMgr.get(managingCmds[0].getFullClassName());
                                    }
                                    if (superData == null)
                                    {
                                        String msg = LOCALISER_RDBMS.msg("050013",
                                            cmd.getFullClassName());
                                        NucleusLogger.PERSISTENCE.error(msg);
                                        throw new NucleusUserException(msg);
                                    }
                                    superTable = (DatastoreClass) superData.getDatastoreContainerObject();
                                    data.setDatastoreContainerObject(superTable);
                                }
                            }
                        }
                    }
                }
                finally
                {
                    if (starter != null && starterInitialised && starter.isOpen() && addClassTablesRecursionCounter <= 1)
                    {
                        starter.close();
                    }
                }
            }
            finally
            {
                addClassTablesRecursionCounter -= 1;
            }
        }

        /**
         * Method to add a new table object (ie ClassTable or ClassView).
         * Doesn't initialize or validate the tables, just adding the table objects to the internal data structures.
         * @param cmd the ClassMetaData
         * @param clr the ClassLoaderResolver
         */
        private void addClassTable(ClassMetaData cmd, ClassLoaderResolver clr)
        {
            // Only add tables for "PERSISTENCE_CAPABLE" classes
            if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                return;
            }
            if (cmd.getIdentityType() == IdentityType.NONDURABLE)
            {
                if (cmd.hasExtension("requires-table") && cmd.getValueForExtension("requires-table") != null && 
                    cmd.getValueForExtension("requires-table").equalsIgnoreCase("false"))
                {
                    return;
                }
            }
            RDBMSStoreData sd = (RDBMSStoreData) storeDataMgr.get(cmd.getFullClassName());
            if (sd == null)
            {
                // For application-identity classes with user-defined identities we check for use of the 
                // objectid-class in different inheritance trees. We prevent this to avoid problems later on.
                // The builtin objectid-classes are allowed to be duplicated.
                if (cmd.getIdentityType() == IdentityType.APPLICATION)
                {
                    if (!cmd.usesSingleFieldIdentityClass())
                    {
                        // Check whether this class has the same base persistable class as the others using the PK. 
                        // If not, then throw an error
                        String baseClassWithMetaData = cmd.getBaseAbstractClassMetaData().getFullClassName();
                        Collection<AbstractClassMetaData> pkCmds =
                            getMetaDataManager().getClassMetaDataWithApplicationId(cmd.getObjectidClass());
                        if (pkCmds != null && pkCmds.size() > 0)
                        {
                            // We already have at least 1 class using the same app id PK class
                            // so check if it is has the same persistable root class.
                            boolean in_same_tree = false;
                            String sample_class_in_other_tree = null;

                            Iterator<AbstractClassMetaData> iter = pkCmds.iterator();
                            while (iter.hasNext())
                            {
                                AbstractClassMetaData pkCmd = iter.next();
                                String otherClassBaseClass = 
                                    pkCmd.getBaseAbstractClassMetaData().getFullClassName();
                                if (otherClassBaseClass.equals(baseClassWithMetaData))
                                {
                                    in_same_tree = true;
                                    break;
                                }
                                sample_class_in_other_tree = pkCmd.getFullClassName();
                            }

                            if (!in_same_tree)
                            {
                                String error_msg = LOCALISER_RDBMS.msg("050021", cmd.getFullClassName(), 
                                    cmd.getObjectidClass(), sample_class_in_other_tree);
                                NucleusLogger.DATASTORE.error(error_msg);
                                throw new NucleusUserException(error_msg);
                            }
                        }
                    }
                }

                if (cmd.isEmbeddedOnly())
                {
                    // Nothing to do. Only persisted as SCO.
                    NucleusLogger.DATASTORE.info(LOCALISER.msg("032012", cmd.getFullClassName()));
                }
                else
                {
                    InheritanceMetaData imd = cmd.getInheritanceMetaData();
                    RDBMSStoreData sdNew = null;
                    if (imd.getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                    {
                        // Table mapped into the table(s) of subclass(es)
                        // Just add the SchemaData entry with no table - managed by subclass
                        sdNew = new RDBMSStoreData(cmd, null, false);
                        registerStoreData(sdNew);
                    }
                    else if (imd.getStrategy() == InheritanceStrategy.COMPLETE_TABLE && 
                            cmd.isAbstract())
                    {
                        // Abstract class with "complete-table" so gets no table
                        sdNew = new RDBMSStoreData(cmd, null, false);
                        registerStoreData(sdNew);
                    }
                    else if (imd.getStrategy() == InheritanceStrategy.NEW_TABLE ||
                             imd.getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
                    {
                        // Table managed by this class
                        // Generate an identifier for the table required
                        DatastoreIdentifier tableName = null;
                        RDBMSStoreData tmpData = (RDBMSStoreData) storeDataMgr.get(cmd.getFullClassName());
                        if (tmpData !=null && tmpData.getDatastoreIdentifier() != null)
                        {
                            tableName = tmpData.getDatastoreIdentifier();
                        }
                        else
                        {
                            tableName = identifierFactory.newDatastoreContainerIdentifier(cmd);
                        }

                        // Check that the required table isn't already in use
                        StoreData[] existingStoreData = getStoreDataForDatastoreContainerObject(tableName);
                        if (existingStoreData != null)
                        {
                            String existingClass = null;
                            for (int j=0;j<existingStoreData.length;j++)
                            {
                                if (!existingStoreData[j].getName().equals(cmd.getFullClassName()))
                                {
                                    existingClass = existingStoreData[j].getName();
                                    break;
                                }
                            }
                            // Give a warning and then create a new instance of the table (mapped to the same datastore object)
                            if (existingClass != null)
                            {
                                String msg = LOCALISER_RDBMS.msg("050015", cmd.getFullClassName(), 
                                    tableName.getIdentifierName(), existingClass);
                                NucleusLogger.DATASTORE.warn(msg);
                            }
                        }

                        // Create the table to use for this class
                        DatastoreClass t = null;
                        boolean hasViewDef = false;
                        if (dba.getVendorID() != null)
                        {
                            hasViewDef = cmd.hasExtension("view-definition" + '-' + dba.getVendorID());
                        }
                        if (!hasViewDef)
                        {
                            hasViewDef = cmd.hasExtension("view-definition");
                        }
                        if (hasViewDef)
                        {
                            t = new ClassView(tableName, RDBMSStoreManager.this, cmd);
                        }
                        else
                        {
                            t = new ClassTable(tableName, RDBMSStoreManager.this, cmd);
                        }

                        sdNew = new RDBMSStoreData(cmd, t, true);
                        registerStoreData(sdNew);

                        // must be initialized after registering, to avoid StackOverflowError
                        ((Table) t).preInitialize(clr);
                    }
                    else if (imd.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
                    {
                        // Table mapped into table of superclass
                        // Find the superclass - should have been created first
                        AbstractClassMetaData[] managingCmds = getClassesManagingTableForClass(cmd, clr);
                        DatastoreContainerObject superTable = null;
                        if (managingCmds != null && managingCmds.length == 1)
                        {
                            RDBMSStoreData superData = (RDBMSStoreData) storeDataMgr.get(managingCmds[0].getFullClassName());
                            if (superData != null)
                            {
                                // Specify the table if it already exists
                                superTable = superData.getDatastoreContainerObject();
                            }
                            sdNew = new RDBMSStoreData(cmd, superTable, false);
                            registerStoreData(sdNew);
                        }
                        else
                        {
                            String msg = LOCALISER_RDBMS.msg("050013", cmd.getFullClassName());
                            NucleusLogger.PERSISTENCE.error(msg);
                            throw new NucleusUserException(msg);
                        }
                    }
                    schemaDataAdded.add(sdNew);
                }
            }
        }

        /**
         * Adds a new table object (ie ClassTable or ClassView) for every class
         * in the given list that 1) requires an extent and 2) does not yet have
         * an extent (ie table) initialized in the store manager.
         * <p>
         * After all of the table objects, including any other tables they might
         * reference, have been added, each table is initialized and validated
         * in the database.
         * </p>
         * <p>
         * If any error occurs along the way, any table(s) that were created are
         * dropped and the state of the RDBMSManager is rolled back to the point
         * at which this method was called.
         * </p>
         * @param classNames The class(es) whose tables are to be added.
         * @param clr the ClassLoaderResolver
         */
        private void addClassTablesAndValidate(String[] classNames, ClassLoaderResolver clr)
        {
            synchronized (storeDataMgr)
            {
                storeDataMgr.begin();
                boolean completed = false;

                List tablesCreated = null;
                List tableConstraintsCreated = null;
                List viewsCreated = null;

                try
                {
                    List autoCreateErrors = new ArrayList();

                    // Add SchemaData entries and Table's for the requested classes
                    addClassTables(classNames, clr);

                    // Initialise all tables/views for the classes
                    List<DatastoreContainerObject>[] toValidate = initializeClassTables(classNames, clr);

                    if (toValidate[0] != null && toValidate[0].size() > 0)
                    {
                        // Validate the tables
                        List[] result = performTablesValidation(toValidate[0], clr);
                        tablesCreated = result[0];
                        tableConstraintsCreated = result[1];
                        autoCreateErrors = result[2];
                    }

                    if (toValidate[1] != null && toValidate[1].size() > 0)
                    {
                        // Validate the views
                        List[] result = performViewsValidation(toValidate[1]);
                        viewsCreated = result[0];
                        autoCreateErrors.addAll(result[1]);
                    }

                    // Process all errors from the above
                    verifyErrors(autoCreateErrors);

                    completed = true;
                }
                catch (SQLException sqle)
                {
                    String msg = LOCALISER_RDBMS.msg("050044", sqle);
                    NucleusLogger.DATASTORE_SCHEMA.error(msg);
                    throw new NucleusDataStoreException(msg, sqle);
                }
                catch (Exception e)
                {
                    if (NucleusException.class.isAssignableFrom(e.getClass()))
                    {
                        throw (NucleusException)e;
                    }
                    else
                    {
                        NucleusLogger.DATASTORE_SCHEMA.error(LOCALISER_RDBMS.msg("050044", e));
                    }
                    throw new NucleusException(e.toString(), e).setFatal();
                }
                finally
                {
                    // If something went wrong, roll things back to the way they were before we started.
                    // This may not restore the database 100% of the time (if DDL statements are not transactional) 
                    // but it will always put the RDBMSManager's internal structures back the way they were.
                    if (!completed)
                    {
                        storeDataMgr.rollback();
                        rollbackSchemaCreation(viewsCreated,tableConstraintsCreated,tablesCreated);
                    }
                    else
                    {
                        storeDataMgr.commit();
                    }
                    schemaDataAdded.clear();
                }
            }
        }

        /**
         * Initialisation of tables. Updates the internal representation of the table to match what is 
         * required for the class(es). Each time a table object is initialized, it may cause other associated 
         * table objects to be added (via callbacks to addClasses()) so the loop is repeated until no more 
         * initialisation is needed.
         * @param classNames String array of class names
         * @param clr the ClassLoaderResolver
         * @return an array of List where index == 0 is list of the tables created, index == 1 is list of the views created
         */
        private List<DatastoreContainerObject>[] initializeClassTables(String[] classNames, ClassLoaderResolver clr)
        {
            List<DatastoreContainerObject> tablesToValidate = new ArrayList();
            List<DatastoreContainerObject> viewsToValidate = new ArrayList();
            boolean someNeededInitialisation;
            List<DatastoreContainerObject> recentlyInitiliased = new ArrayList();
            do
            {
                someNeededInitialisation = false;
                RDBMSStoreData[] rdbmsStoreData =
                    storeDataMgr.getManagedStoreData().toArray(new RDBMSStoreData[storeDataMgr.size()]);
                for (int i=0; i<rdbmsStoreData.length; i++)
                {
                    if (rdbmsStoreData[i].hasTable())
                    {
                        Table t = (Table)rdbmsStoreData[i].getDatastoreContainerObject();
                        if (t instanceof DatastoreClass)
                        {
                            ((RDBMSPersistenceHandler)persistenceHandler).removeRequestsForTable((DatastoreClass)t);
                        }

                        // Any classes managed by their own table needing initialising
                        if (!t.isInitialized())
                        {
                            t.initialize(clr);
                            recentlyInitiliased.add(t);
                            if (t instanceof ViewImpl)
                            {
                                viewsToValidate.add(t);
                            }
                            else
                            {
                                tablesToValidate.add(t);
                            }
                            someNeededInitialisation = true;
                        }

                        // Any classes that are managed by other tables needing initialising
                        if (!rdbmsStoreData[i].isTableOwner() && !((ClassTable)t).managesClass(rdbmsStoreData[i].getName()))
                        {
                            ((ClassTable)t).manageClass((ClassMetaData)rdbmsStoreData[i].getMetaData(), clr);
                            if (!tablesToValidate.contains(t))
                            {
                                tablesToValidate.add(t);
                            }
                            someNeededInitialisation = true;
                        }
                    }
                    else
                    {
                        // Nothing to do for cases without their own table ("subclass-table" strategy) since
                        // the table is initialised to contain those fields by the subclass.
                    }
                }
            }
            while (someNeededInitialisation);

            // Post initialisation of tables
            for (int j=0; j<recentlyInitiliased.size(); j++)
            {
                ((Table)recentlyInitiliased.get(j)).postInitialize(clr);
            }

            return new List[] { tablesToValidate, viewsToValidate };
        }

        /**
         * Validate tables.
         * @param tablesToValidate list of TableImpl to validate
         * @param clr the ClassLoaderResolver
         * @return an array of List where index == 0 has a list of the tables created
         *                                index == 1 has a list of the contraints created
         *                                index == 2 has a list of the auto creation errors 
         * @throws SQLException
         */
        private List[] performTablesValidation(List<DatastoreContainerObject> tablesToValidate, ClassLoaderResolver clr) 
        throws SQLException
        {
            List autoCreateErrors = new ArrayList();
            List<DatastoreContainerObject> tableConstraintsCreated = new ArrayList();
            List<DatastoreContainerObject> tablesCreated = new ArrayList();

            if (ddlWriter != null)
            {
                // Remove any existence of the same actual table more than once so we dont duplicate its
                // DDL for creation. Note that this will allow more than once instance of tables with the same
                // name (identifier) since when you have multiple inheritance trees each inheritance tree
                // will have its own ClassTable, and you want both of these to pass through to schema generation.
                tablesToValidate = removeDuplicateTablesFromList(tablesToValidate);
            }

            // Table existence and validation.
            // a). Check for existence of the table
            // b). If autocreate, create the table if necessary
            // c). If validate, validate the table
            Iterator i = tablesToValidate.iterator();
            while (i.hasNext())
            {
                TableImpl t = (TableImpl) i.next();

                boolean columnsValidated = false;
                if (checkExistTablesOrViews)
                {
                    if (ddlWriter != null)
                    {
                        try
                        {
                            if (t instanceof ClassTable)
                            {
                                ddlWriter.write("-- Table " + t.toString() + 
                                    " for classes " + StringUtils.objectArrayToString(((ClassTable)t).getManagedClasses()) + "\n");
                            }
                            else if (t instanceof JoinTable)
                            {
                                ddlWriter.write("-- Table " + t.toString() + " for join relationship\n");
                            }
                        }
                        catch (IOException ioe)
                        {
                            NucleusLogger.DATASTORE_SCHEMA.error("error writing DDL into file for table " + t, ioe);
                        }
                    }

                    if (!tablesCreated.contains(t) && t.exists(getCurrentConnection(), autoCreateTables))
                    {
                        // Table has been created so add to our list so we dont process it multiple times
                        // Any subsequent instance of this table in the list will have the columns checked only
                        tablesCreated.add(t);
                        columnsValidated = true;
                    }
                    else
                    {
                        // Table wasn't just created, so do any autocreate of columns necessary
                        if (t.isInitializedModified() || autoCreateColumns)
                        {
                            // Check for existence of the required columns and add where required
                            t.validateColumns(getCurrentConnection(), false, autoCreateColumns, autoCreateErrors);
                            columnsValidated = true;
                        }
                    }
                }

                if (validateTables && !columnsValidated) // Table not just created and validation requested
                {
                    // Check down to the column structure where required
                    t.validate(getCurrentConnection(), validateColumns, false, autoCreateErrors);
                }
                else if (!columnsValidated)
                {
                    // Validation not requested but allow initialisation of the column information
                    String initInfo = getStringProperty("datanucleus.rdbms.initializeColumnInfo");
                    if (initInfo.equalsIgnoreCase("PK"))
                    {
                        // Initialise the PK columns only
                        t.initializeColumnInfoForPrimaryKeyColumns(getCurrentConnection());
                    }
                    else if (initInfo.equalsIgnoreCase("ALL"))
                    {
                        // Initialise all columns
                        t.initializeColumnInfoFromDatastore(getCurrentConnection());
                    }
                }

                // Discard any cached column info used to validate the table
                invalidateColumnInfoForTable(t);
            }

            // Table constraint existence and validation
            // a). Check for existence of the constraint
            // b). If autocreate, create the constraint if necessary
            // c). If validate, validate the constraint
            // Constraint processing is done as a separate step from table processing
            // since the constraints are dependent on tables being available
            i = tablesToValidate.iterator();
            while (i.hasNext())
            {
                TableImpl t = (TableImpl) i.next();
                if (validateConstraints || autoCreateConstraints)
                {
                    if (ddlWriter != null)
                    {
                        try
                        {
                            if (t instanceof ClassTable)
                            {
                                ddlWriter.write("-- Constraints for table " + t.toString() + 
                                    " for class(es) " + StringUtils.objectArrayToString(((ClassTable)t).getManagedClasses()) + "\n");
                            }
                            else
                            {
                                ddlWriter.write("-- Constraints for table " + t.toString() + "\n");
                            }
                        }
                        catch (IOException ioe)
                        {
                            NucleusLogger.DATASTORE_SCHEMA.error("error writing DDL into file for table " + t, ioe);
                        }
                    }
                    // TODO : split this method into checkExistsConstraints and validateConstraints
                    // TODO : if duplicated entries on the list, we need to validate before.
                    if (tablesCreated.contains(t) && !hasDuplicateTablesFromList(tablesToValidate))
                    {
                        if (t.createConstraints(getCurrentConnection(), autoCreateErrors, clr))
                        {
                            tableConstraintsCreated.add(t);
                        }
                    }
                    else if (t.validateConstraints(getCurrentConnection(), autoCreateConstraints, autoCreateErrors, clr))
                    {
                        tableConstraintsCreated.add(t);
                    }
                    if (ddlWriter != null)
                    {
                        try
                        {
                            ddlWriter.write("\n");
                        }
                        catch (IOException ioe)
                        {
                            NucleusLogger.DATASTORE_SCHEMA.error("error writing DDL into file for table " + t, ioe);
                        }
                    }
                }
            }
            return new List[] { tablesCreated, tableConstraintsCreated, autoCreateErrors };
        }

        /**
         * Remove duplicated tables from the list.
         * Tables are only removed if they are the same table object. That is we don't remove if they have
         * the same table identifier.
         * @param newTables the list of DatastoreContainerObject
         * @return a distinct list with DatastoreContainerObject  
         */
        private List<DatastoreContainerObject> removeDuplicateTablesFromList(List<DatastoreContainerObject> newTables)
        {
            List<DatastoreContainerObject> result = new ArrayList();
            for (DatastoreContainerObject tbl : newTables)
            {
                if (!result.contains(tbl)) // TODO This just checks identifier name. The check commented out below
                    // was the original check so may need re-instating depending on some example that requires it
                {
                    result.add(tbl);
                }
/*              String tblRef1 = StringUtils.toJVMIDString(tbl.getIdentifier());
                boolean exists = false;
                for (DatastoreContainerObject resultTbl : result)
                {
                    // Compare identifier reference since the same identifier name does not imply the same
                    // table identifier
                    String tblRef2 = StringUtils.toJVMIDString(resultTbl.getIdentifier());
                    if (tblRef1.equals(tblRef2))
                    {
                        exists = true;
                    }
                }
                if (!exists)
                {
                    result.add(tbl);
                }*/
            }
            return result;
        }

        /**
         * Check if duplicated tables are in the list.
         * @param newTables the list of DatastoreContainerObject
         * @return true if duplicated tables are in the list
         */
        private boolean hasDuplicateTablesFromList(List<DatastoreContainerObject> newTables)
        {
            Map map = new HashMap();
            for (int i=0; i<newTables.size(); i++)
            {
                DatastoreContainerObject t1 = newTables.get(i);
                if (map.containsKey(t1.getIdentifier().getIdentifierName()))
                {
                    return true;
                }
                map.put(t1.getIdentifier().getIdentifierName(), t1);
            }
            return false;
        }

        /**
         * Validate the supplied views.
         * @param viewsToValidate list of ViewImpl to validate
         * @return an array of List where index == 0 has a list of the views created
         *                                index == 1 has a list of the auto creation errors 
         * @throws SQLException
         */
        private List[] performViewsValidation(List<DatastoreContainerObject> viewsToValidate) throws SQLException
        {
            List<DatastoreContainerObject> viewsCreated = new ArrayList();
            List autoCreateErrors = new ArrayList();
            // View existence and validation.
            // a). Check for existence of the view
            // b). If autocreate, create the view if necessary
            // c). If validate, validate the view
            Iterator i = viewsToValidate.iterator();
            while (i.hasNext())
            {
                ViewImpl v = (ViewImpl) i.next();
                if (checkExistTablesOrViews)
                {
                    if (v.exists(getCurrentConnection(), autoCreateTables))
                    {
                        viewsCreated.add(v);
                    }
                }
                if (validateTables)
                {
                    v.validate(getCurrentConnection(), true, false, autoCreateErrors);
                }

                // Discard any cached column info used to validate the view
                invalidateColumnInfoForTable(v);
            }
            return new List[] { viewsCreated, autoCreateErrors };
        }

        /**
         * Verify the list of errors, log the errors and raise NucleusDataStoreException when fail on error is enabled.
         * @param autoCreateErrors the list of Throwables
         */
        private void verifyErrors(List autoCreateErrors)
        {
            if (autoCreateErrors.size() > 0)
            {
                // Print out all errors found during auto-creation/validation
                Iterator errorsIter = autoCreateErrors.iterator();
                while (errorsIter.hasNext())
                {
                    Throwable exc = (Throwable)errorsIter.next();
                    if (autoCreateWarnOnError)
                    {
                        NucleusLogger.DATASTORE.warn(LOCALISER_RDBMS.msg("050044", exc));
                    }
                    else
                    {
                        NucleusLogger.DATASTORE.error(LOCALISER_RDBMS.msg("050044", exc));
                    }
                }
                if (!autoCreateWarnOnError)
                {
                    throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("050043"), 
                        (Throwable[])autoCreateErrors.toArray(new Throwable[autoCreateErrors.size()]));
                }
            }
        }

        /**
         * Rollback / Compensate schema creation by dropping tables, views, constraints and
         * deleting entries in the auto start mechanism.
         * @param viewsCreated the views created that must be dropped
         * @param tableConstraintsCreated the constraints created that must be dropped
         * @param tablesCreated the tables created that must be dropped
         */
        private void rollbackSchemaCreation(List<DatastoreContainerObject> viewsCreated,
                List<DatastoreContainerObject> tableConstraintsCreated, List<DatastoreContainerObject> tablesCreated)
        {
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER_RDBMS.msg("050040"));
            }

            // Tables, table constraints, and views get removed in the reverse order from which they were created.
            try
            {
                if (viewsCreated != null)
                {
                    ListIterator li = viewsCreated.listIterator(viewsCreated.size());
                    while (li.hasPrevious())
                    {
                        ((ViewImpl) li.previous()).drop(getCurrentConnection());
                    }
                }
                if( tableConstraintsCreated != null)
                {
                    ListIterator li = tableConstraintsCreated.listIterator(tableConstraintsCreated.size());
                    while (li.hasPrevious())
                    {
                        ((TableImpl) li.previous()).dropConstraints(getCurrentConnection());
                    }
                }
                if (tablesCreated != null)
                {
                    ListIterator li = tablesCreated.listIterator(tablesCreated.size());
                    while (li.hasPrevious())
                    {
                        ((TableImpl) li.previous()).drop(getCurrentConnection());
                    }
                }
            }
            catch (Exception e)
            {
                NucleusLogger.DATASTORE_SCHEMA.warn(LOCALISER_RDBMS.msg("050041", e));
            }

            // AutoStarter - Remove all classes from the supported list that were added in this pass.
            if (starter != null && starterInitialised)
            {
                try
                {
                    if (!starter.isOpen())
                    {
                        starter.open();
                    }
                    Iterator<RDBMSStoreData> schema_added_iter = schemaDataAdded.iterator();
                    while (schema_added_iter.hasNext())
                    {
                        RDBMSStoreData sd = schema_added_iter.next();
                        starter.deleteClass(sd.getName());
                    }                            
                }
                finally
                {
                    if (starter.isOpen())
                    {
                        starter.close();
                    }
                }
            }
        }

        /**
         * Called by Mapping objects in the midst of RDBMSManager.addClasses()
         * to request the creation of a join table to hold a containers' contents.
         * @param mmd The member metadata for this field/property.
         * @param type The type of the join table
         */
        private DatastoreContainerObject addJoinTableForContainer(AbstractMemberMetaData mmd, ClassLoaderResolver clr, int type)
        {
            DatastoreIdentifier tableName = null;
            RDBMSStoreData sd = (RDBMSStoreData) storeDataMgr.get(mmd);
            if (sd != null && sd.getDatastoreIdentifier() != null)
            {
                tableName = sd.getDatastoreIdentifier();
            }
            else
            {
                tableName = identifierFactory.newDatastoreContainerIdentifier(mmd);
            }

            DatastoreContainerObject join = null;
            if (type == JOIN_TABLE_COLLECTION)
            {
                join = new CollectionTable(tableName, mmd, RDBMSStoreManager.this);
            }
            else if (type == JOIN_TABLE_MAP)
            {
                join = new MapTable(tableName, mmd, RDBMSStoreManager.this);
            }
            else if (type == JOIN_TABLE_ARRAY)
            {
                join = new ArrayTable(tableName, mmd, RDBMSStoreManager.this);
            }
            else if (type == JOIN_TABLE_PERSISTABLE)
            {
                join = new PersistableJoinTable(tableName, mmd, RDBMSStoreManager.this);
            }

            RDBMSStoreData data;
            try
            {
                if (starter != null && starterInitialised && !starter.isOpen())
                {
                    starter.open();
                }

                data = new RDBMSStoreData(mmd, join);
                registerStoreData(data);
            }
            finally
            {
                if (starter != null && starterInitialised && starter.isOpen())
                {
                    starter.close();
                }
            }

            schemaDataAdded.add(data);
            return join;
        }
    }

    /**
     * Accessor for the supported options in string form
     */
    public Collection getSupportedOptions()
    {
        Set set = new HashSet();
        set.add("ORM");
        set.add("NonDurableIdentity");
        set.add("DatastoreIdentity");
        set.add("ApplicationIdentity");

        // Add isolation levels for this database adapter
        if (dba.supportsOption(RDBMSAdapter.TX_ISOLATION_READ_COMMITTED))
        {
            set.add("TransactionIsolationLevel.read-committed");
        }
        if (dba.supportsOption(RDBMSAdapter.TX_ISOLATION_READ_UNCOMMITTED))
        {
            set.add("TransactionIsolationLevel.read-uncommitted");
        }
        if (dba.supportsOption(RDBMSAdapter.TX_ISOLATION_REPEATABLE_READ))
        {
            set.add("TransactionIsolationLevel.repeatable-read");
        }
        if (dba.supportsOption(RDBMSAdapter.TX_ISOLATION_SERIALIZABLE))
        {
            set.add("TransactionIsolationLevel.serializable");
        }

        // Query Cancel and Datastore Timeout is supported on JDOQL for RDBMS (unless turned off by user)
        set.add("Query.Cancel");
        set.add("Datastore.Timeout");

        return set;
    }

    /**
     * Accessor for whether this mapping requires values inserting on an INSERT.
     * @param datastoreMapping The datastore mapping
     * @return Whether values are to be inserted into this mapping on an INSERT
     */
    public boolean insertValuesOnInsert(DatastoreMapping datastoreMapping)
    {
        return ((RDBMSMapping)datastoreMapping).insertValuesOnInsert();
    }

    /**
     * Convenience method to return if the datastore supports batching and the user wants batching.
     * @return If batching of statements is permissible
     */
    public boolean allowsBatching()
    {
        if (dba.supportsOption(RDBMSAdapter.STATEMENT_BATCHING) &&
            getIntProperty("datanucleus.rdbms.statementBatchLimit") != 0)
        {
            return true;
        }
        return false;
    }

    public ResultObjectFactory newResultObjectFactory(AbstractClassMetaData acmd, 
        StatementClassMapping mappingDefinition, boolean ignoreCache, FetchPlan fetchPlan, 
        Class persistentClass)
    {
        return new PersistentClassROF(this, acmd, mappingDefinition, ignoreCache, fetchPlan, persistentClass);
    }

    protected ArrayStore newFKArrayStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        return new RDBMSFKArrayStore(mmd, this, clr);
    }

    protected ListStore newFKListStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        return new RDBMSFKListStore(mmd, this, clr);
    }

    protected SetStore newFKSetStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        return new RDBMSFKSetStore(mmd, this, clr);
    }

    protected MapStore newFKMapStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        return new RDBMSFKMapStore(mmd, this, clr);
    }

    protected ArrayStore newJoinArrayStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr, 
            DatastoreContainerObject table)
    {
        return new RDBMSJoinArrayStore(mmd, (ArrayTable)table, clr);
    }

    protected MapStore newJoinMapStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr,
            DatastoreContainerObject table)
    {
        return new RDBMSJoinMapStore((MapTable)table, clr);
    }

    protected ListStore newJoinListStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr,
            DatastoreContainerObject table)
    {
        return new RDBMSJoinListStore(mmd, (CollectionTable)table, clr);
    }

    protected SetStore newJoinSetStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr,
            DatastoreContainerObject table)
    {
        return new RDBMSJoinSetStore(mmd, (CollectionTable)table, clr);
    }

    protected PersistableRelationStore newPersistableRelationStore(AbstractMemberMetaData mmd, ClassLoaderResolver clr,
            DatastoreContainerObject table)
    {
        return new RDBMSPersistableRelationStore(mmd, (PersistableJoinTable) table, clr);
    }

    public boolean usesBackedSCOWrappers()
    {
        return true;
    }

    /**
     * Accessor for the Calendar to be used in handling all timezone issues with the datastore.
     * Utilises the "serverTimeZoneID" in providing this Calendar used in time/date conversions.
     * @return The calendar to use for dateTimezone issues.
     */
    public Calendar getCalendarForDateTimezone()
    {
        if (dateTimezoneCalendar == null)
        {
            TimeZone tz;
            String serverTimeZoneID = getStringProperty(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID);
            if (serverTimeZoneID != null)
            {
                tz = TimeZone.getTimeZone(serverTimeZoneID);
            }
            else
            {
                tz = TimeZone.getDefault();
            }
            dateTimezoneCalendar = new GregorianCalendar(tz);
        }
        // This returns a clone because Oracle JDBC driver was taking the Calendar and modifying it
        // in calls. Hence passing a clone gets around that. May be best to just return it direct here
        // and then in Oracle usage we pass in a clone to its JDBC driver
        return (Calendar) dateTimezoneCalendar.clone();
    }

    // ---------------------------------------SchemaTool------------------------------------------------

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.SchemaAwareStoreManager#createSchema(java.util.Set, java.util.Properties)
     */
    public void createSchema(Set<String> inputClassNames, Properties props)
    {
        Set<String> classNames = cleanInputClassNames(nucleusContext, inputClassNames);

        String ddlFilename = props != null ? props.getProperty("ddlFilename") : null;
        String completeDdlProp = props != null ? props.getProperty("completeDdl") : null;
        boolean completeDdl = (completeDdlProp != null && completeDdlProp.equalsIgnoreCase("true"));
        String autoStartProp = props != null ? props.getProperty("autoStartTable") : null;
        boolean autoStart = (autoStartProp != null && autoStartProp.equalsIgnoreCase("true"));

        if (classNames.size() > 0)
        {
            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(null);
            FileWriter ddlFileWriter = null;
            try
            {
                if (ddlFilename != null)
                {
                    // Open the DDL file for writing
                    File ddlFile = StringUtils.getFileForFilename(ddlFilename);
                    if (ddlFile.exists())
                    {
                        // Delete existing file
                        ddlFile.delete();
                    }
                    if (ddlFile.getParentFile() != null && !ddlFile.getParentFile().exists())
                    {
                        // Make sure the directory exists
                        ddlFile.getParentFile().mkdirs();
                    }
                    ddlFile.createNewFile();
                    ddlFileWriter = new FileWriter(ddlFile);

                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    ddlFileWriter.write("------------------------------------------------------------------\n");
                    ddlFileWriter.write("-- DataNucleus SchemaTool " + 
                        "(ran at " + fmt.format(new java.util.Date()) + ")\n");
                    ddlFileWriter.write("------------------------------------------------------------------\n");
                    if (completeDdl)
                    {
                        ddlFileWriter.write("-- Complete schema required for the following classes:-\n");
                    }
                    else  
                    {
                        ddlFileWriter.write("-- Schema diff for " + getConnectionURL() + " and the following classes:-\n");
                    }
                    Iterator classNameIter = classNames.iterator();
                    while (classNameIter.hasNext())
                    {
                        ddlFileWriter.write("--     " + classNameIter.next() + "\n");
                    }
                    ddlFileWriter.write("--\n");
                }

                try
                {
                    if (ddlFileWriter != null)
                    {
                        this.ddlWriter = ddlFileWriter;
                        this.completeDDL = completeDdl;
                        this.writtenDdlStatements = new HashSet();
                    }

                    // Generate the tables/constraints
                    new ClassAdder(classNames.toArray(new String[classNames.size()]), ddlFileWriter).execute(clr);

                    if (autoStart)
                    {
                        // Generate the SchemaTable auto-starter table
                        if (ddlFileWriter != null)
                        {
                            try
                            {
                                ddlFileWriter.write("\n");
                                ddlFileWriter.write("------------------------------------------------------------------\n");
                                ddlFileWriter.write("-- Table for SchemaTable auto-starter\n");
                            }
                            catch (IOException ioe)
                            {
                            }
                        }
                        new SchemaAutoStarter(this, clr);
                    }

                    if (ddlFileWriter != null)
                    {
                        this.ddlWriter = null;
                        this.completeDDL = false;
                        this.writtenDdlStatements.clear();
                        this.writtenDdlStatements = null;
                    }

                    if (ddlFileWriter != null)
                    {
                        ddlFileWriter.write("\n");
                        ddlFileWriter.write("------------------------------------------------------------------\n");
                        ddlFileWriter.write("-- Sequences and SequenceTables\n");
                    }
                    createSchemaSequences(classNames, clr, ddlFileWriter);
                }
                finally
                {
                    if (ddlFileWriter != null)
                    {
                        ddlFileWriter.close();
                    }
                }
            }
            catch (IOException ioe)
            {
                // Error in writing DDL file
                // TODO Handle this
            }
        }
        else
        {
            String msg = LOCALISER.msg(false, "014039");
            NucleusLogger.DATASTORE_SCHEMA.error(msg);
            System.out.println(msg);

            throw new NucleusException(msg);
        }
    }

    protected void createSchemaSequences(Set<String> classNames, ClassLoaderResolver clr, FileWriter ddlWriter)
    {
        // Check for datastore-based value-generator usage
        if (classNames != null && classNames.size() > 0)
        {
            Set<String> seqTablesGenerated = new HashSet();
            Set<String> sequencesGenerated = new HashSet();
            Iterator<String> classNameIter = classNames.iterator();
            while (classNameIter.hasNext())
            {
                String className = classNameIter.next();
                AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
                if (cmd.getIdentityMetaData() != null && cmd.getIdentityMetaData().getValueStrategy() != null)
                {
                    if (cmd.getIdentityMetaData().getValueStrategy() == IdentityStrategy.INCREMENT)
                    {
                        addSequenceTableForMetaData(cmd.getIdentityMetaData(), clr, seqTablesGenerated);
                    }
                    else if (cmd.getIdentityMetaData().getValueStrategy() == IdentityStrategy.SEQUENCE)
                    {
                        String seqName = cmd.getIdentityMetaData().getSequence();
                        if (StringUtils.isWhitespace(seqName))
                        {
                            seqName = cmd.getIdentityMetaData().getValueGeneratorName();
                        }
                        if (!StringUtils.isWhitespace(seqName))
                        {
                            addSequenceForMetaData(cmd.getIdentityMetaData(), seqName, clr, sequencesGenerated, 
                                ddlWriter);
                        }
                    }
                }

                AbstractMemberMetaData[] mmds = cmd.getManagedMembers();
                for (int j=0;j<mmds.length;j++)
                {
                    IdentityStrategy str = mmds[j].getValueStrategy();
                    if (str == IdentityStrategy.INCREMENT)
                    {
                        addSequenceTableForMetaData(mmds[j], clr, seqTablesGenerated);
                    }
                    else if (str == IdentityStrategy.SEQUENCE)
                    {
                        String seqName = mmds[j].getSequence();
                        if (StringUtils.isWhitespace(seqName))
                        {
                            seqName = mmds[j].getValueGeneratorName();
                        }
                        if (!StringUtils.isWhitespace(seqName))
                        {
                            addSequenceForMetaData(mmds[j], seqName, clr, sequencesGenerated, ddlWriter);
                        }
                    }
                }
            }
        }
    }

    protected void addSequenceTableForMetaData(MetaData md, ClassLoaderResolver clr, Set<String> seqTablesGenerated)
    {
        String catName = null;
        String schName = null;
        String tableName = TableGenerator.DEFAULT_TABLE_NAME;
        String seqColName = TableGenerator.DEFAULT_SEQUENCE_COLUMN_NAME;
        String nextValColName = TableGenerator.DEFAULT_NEXTVALUE_COLUMN_NAME;
        if (md.hasExtension("sequence-catalog-name"))
        {
            catName = md.getValueForExtension("sequence-catalog-name");
        }
        if (md.hasExtension("sequence-schema-name"))
        {
            schName = md.getValueForExtension("sequence-schema-name");
        }
        if (md.hasExtension("sequence-table-name"))
        {
            tableName = md.getValueForExtension("sequence-table-name");
        }
        if (md.hasExtension("sequence-name-column-name"))
        {
            seqColName = md.getValueForExtension("sequence-name-column-name");
        }
        if (md.hasExtension("sequence-nextval-column-name"))
        {
            nextValColName = md.getValueForExtension("sequence-nextval-column-name");
        }

        if (!seqTablesGenerated.contains(tableName))
        {
            ManagedConnection mconn = getConnection(UserTransaction.TRANSACTION_NONE);
            Connection conn = (Connection) mconn.getConnection();
            try
            {
                DatastoreIdentifier tableIdentifier = identifierFactory.newDatastoreContainerIdentifier(tableName);
                if (catName != null)
                {
                    tableIdentifier.setCatalogName(catName);
                }
                if (schName != null)
                {
                    tableIdentifier.setSchemaName(schName);
                }
                SequenceTable seqTable = new SequenceTable(tableIdentifier, this, seqColName, nextValColName);
                seqTable.initialize(clr);
                seqTable.exists(conn, true);
            }
            catch (Exception e)
            {
            }
            finally
            {
                mconn.close();
            }
            seqTablesGenerated.add(tableName);
        }
    }

    protected void addSequenceForMetaData(MetaData md, String seq,  ClassLoaderResolver clr,
            Set<String> sequencesGenerated, FileWriter ddlWriter)
    {
        String seqName = seq;
        Integer min = null;
        Integer max = null;
        Integer start = null;
        Integer increment = null;
        Integer cacheSize = null;

        SequenceMetaData seqmd = getMetaDataManager().getMetaDataForSequence(clr, seq);
        if (seqmd != null)
        {
            seqName = seqmd.getDatastoreSequence();
            if (seqmd.getAllocationSize() > 0)
            {
                increment = Integer.valueOf(seqmd.getAllocationSize());
            }
            if (seqmd.getInitialValue() >= 0)
            {
                start = Integer.valueOf(seqmd.getInitialValue());
            }
            md = seqmd;
        }
        if (md.hasExtension("key-min-value"))
        {
            min = Integer.valueOf(md.getValueForExtension("key-min-value"));
        }
        if (md.hasExtension("key-max-value"))
        {
            max = Integer.valueOf(md.getValueForExtension("key-max-value"));
        }
        if (md.hasExtension("key-cache-size"))
        {
            increment = Integer.valueOf(md.getValueForExtension("key-cache-size"));
        }
        if (md.hasExtension("key-initial-value"))
        {
            start = Integer.valueOf(md.getValueForExtension("key-initial-value"));
        }
        if (md.hasExtension("key-database-cache-size"))
        {
            cacheSize = Integer.valueOf(md.getValueForExtension("key-database-cache-size"));
        }
        if (!sequencesGenerated.contains(seqName))
        {
            String stmt = ((RDBMSAdapter)getDatastoreAdapter()).getSequenceCreateStmt(
                seqName, min, max, start, increment, cacheSize);
            if (ddlWriter != null)
            {
                try
                {
                    ddlWriter.write(stmt + ";\n");
                }
                catch (IOException ioe)
                {
                }
            }
            else
            {
                PreparedStatement ps = null;
                ManagedConnection mconn = getConnection(UserTransaction.TRANSACTION_NONE);
                try
                {
                    ps = sqlController.getStatementForUpdate(mconn, stmt, false);
                    sqlController.executeStatementUpdate(null, mconn, stmt, ps, true);
                }
                catch (SQLException e)
                {
                }
                finally
                {
                    try
                    {
                        if (ps != null)
                        {
                            sqlController.closeStatement(mconn, ps);
                        }
                    }
                    catch (SQLException e)
                    {
                    }
                    mconn.close();
                }
            }
            sequencesGenerated.add(seqName);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.SchemaAwareStoreManager#deleteSchema(java.util.Set)
     */
    public void deleteSchema(Set<String> inputClassNames, Properties props)
    {
        Set<String> classNames = cleanInputClassNames(nucleusContext, inputClassNames);

        if (classNames.size() > 0)
        {
            // Delete the tables
            String ddlFilename = props != null ? props.getProperty("ddlFilename") : null;
            String completeDdlProp = props != null ? props.getProperty("completeDdl") : null;
            boolean completeDdl = (completeDdlProp != null && completeDdlProp.equalsIgnoreCase("true"));
            String autoStartProp = props != null ? props.getProperty("autoStartTable") : null;
            boolean autoStart = (autoStartProp != null && autoStartProp.equalsIgnoreCase("true"));

            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(null);
            FileWriter ddlFileWriter = null;
            try
            {
                if (ddlFilename != null)
                {
                    // Open the DDL file for writing
                    File ddlFile = StringUtils.getFileForFilename(ddlFilename);
                    if (ddlFile.exists())
                    {
                        // Delete existing file
                        ddlFile.delete();
                    }
                    if (ddlFile.getParentFile() != null && !ddlFile.getParentFile().exists())
                    {
                        // Make sure the directory exists
                        ddlFile.getParentFile().mkdirs();
                    }
                    ddlFile.createNewFile();
                    ddlFileWriter = new FileWriter(ddlFile);

                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    ddlFileWriter.write("------------------------------------------------------------------\n");
                    ddlFileWriter.write("-- DataNucleus SchemaTool " + 
                        "(ran at " + fmt.format(new java.util.Date()) + ")\n");
                    ddlFileWriter.write("------------------------------------------------------------------\n");
                    ddlFileWriter.write("-- Delete schema required for the following classes:-\n");
                    Iterator classNameIter = classNames.iterator();
                    while (classNameIter.hasNext())
                    {
                        ddlFileWriter.write("--     " + classNameIter.next() + "\n");
                    }
                    ddlFileWriter.write("--\n");
                }

                try
                {
                    if (ddlFileWriter != null)
                    {
                        this.ddlWriter = ddlFileWriter;
                        this.completeDDL = completeDdl;
                        this.writtenDdlStatements = new HashSet();
                    }

                    // Generate the tables/constraints for these classes (so we know the tables to delete)
                    String[] classNameArray = classNames.toArray(new String[classNames.size()]);
                    addClasses(classNameArray, clr); // Add them to mgr first

                    // Delete the tables of the required classes
                    DeleteTablesSchemaTransaction deleteTablesTxn = new DeleteTablesSchemaTransaction(this,
                        Connection.TRANSACTION_READ_COMMITTED, storeDataMgr);
                    deleteTablesTxn.setWriter(ddlWriter);
                    boolean success = true;
                    try
                    {
                        deleteTablesTxn.execute(clr);
                    }
                    catch (NucleusException ne)
                    {
                        success = false;
                        throw ne;
                    }
                    finally
                    {
                        if (success)
                        {
                            clearSchemaData();
                        }
                    }

                    if (autoStart)
                    {
                        // TODO Delete the SchemaTable auto-starter table
                    }

                    // TODO Delete sequences and sequenceTables
                }
                finally
                {
                    if (ddlFileWriter != null)
                    {
                        this.ddlWriter = null;
                        this.completeDDL = false;
                        this.writtenDdlStatements.clear();
                        this.writtenDdlStatements = null;

                        ddlFileWriter.close();
                    }
                }
            }
            catch (IOException ioe)
            {
                // Error in writing DDL file
                // TODO Handle this
            }
        }
        else
        {
            String msg = LOCALISER.msg(false, "014039");
            NucleusLogger.DATASTORE_SCHEMA.error(msg);
            System.out.println(msg);

            throw new NucleusException(msg);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.SchemaAwareStoreManager#validateSchema(java.util.Set)
     */
    public void validateSchema(Set<String> inputClassNames, Properties props)
    {
        Set<String> classNames = cleanInputClassNames(nucleusContext, inputClassNames);
        if (classNames != null && classNames.size() > 0)
        {
            // Validate the tables/constraints
            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(null);

            String[] classNameArray = classNames.toArray(new String[classNames.size()]);
            addClasses(classNameArray, clr); // Validates since we have the flags set
        }
        else
        {
            String msg = LOCALISER.msg(false, "014039");
            NucleusLogger.DATASTORE_SCHEMA.error(msg);
            System.out.println(msg);

            throw new NucleusException(msg);
        }
    }

    /**
     * Method to generate a set of class names using the input list.
     * If no input class names are provided then uses the list of classes known to have metadata.
     * @param ctx NucleusContext
     * @param inputClassNames Class names to start from
     * @return The set of class names
     */
    protected static Set<String> cleanInputClassNames(NucleusContext ctx, Set<String> inputClassNames) 
    {
        Set<String> classNames = new TreeSet<String>();
        if (inputClassNames == null || inputClassNames.size() == 0)
        {
            // Use all "known" persistable classes
            Collection classesWithMetadata = ctx.getMetaDataManager().getClassesWithMetaData();
            classNames.addAll(classesWithMetadata);
        }
        else
        {
            // Use all input classes
            classNames.addAll(inputClassNames);
        }
        return classNames;
    }
}