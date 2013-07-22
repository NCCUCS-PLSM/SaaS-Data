/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved. 
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
2004 Andy Jefferson - removed MetaData requirement
2007 Andy Jefferson - revised to fit in with JPA requirements for table sequences
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.valuegenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.valuegenerator.ValueGenerationBlock;
import org.datanucleus.store.valuegenerator.ValueGenerationException;
import org.datanucleus.util.NucleusLogger;

/**
 * Identity generator for RDBMS databases that generates ids using a table in the database.
 * This generator is coupled to core and can't be used in standalone mode.
 * <P>
 * The following properties define the name of the sequence being generated. If "sequence-name" is specified then it is
 * used. Otherwise the name of the sequence will either be based on the table name or the class name (for what we are
 * generating the ids).
 * <UL>
 * <LI><U>sequence-name</U> - Name for the sequence</LI>
 * <LI><U>sequence-table-basis</U> - Basic for the sequence name (if "sequence-name" not provided). This can be
 * "table" or "class".</LI>
 * </UL>
 * <p>
 * The following properties define the table where the identities are generated.
 * </p>
 * <UL>
 * <LI><U>sequence-catalog-name</U> - the catalog name for the table (defaults to the default catalog)</LI>
 * <LI><U>sequence-schema-name</U> - the schema name for the table (defaults to the default schema)</LI>
 * <LI><U>sequence-table-name</U> - the table name for the table (defaults to SEQUENCE_TABLE)</LI>
 * <LI><U>sequence-name-column-name</U> - the name for the column that represent sequence names</LI>
 * <LI><U>sequence-nextval-column-name</U> - the name for the column that represent incrementing sequence values</LI>
 * <UL>
 * <p>
 * The following properties control the initial value, and the number of ids that are cached (generated) in each call.
 * </p>
 * <UL>
 * <LI><U>key-initial-value</U> - start value (if we have no current value). If not specified and we have
 * no current value then we do a "SELECT max(column-name) FROM table-name" for the column being incremented</LI>
 * <LI><U>key-cache-size</U> - number of unique identifiers to cache (defaults to 5)</LI>
 * </UL>
 * <p>
 * The following properties are used when finding the starting point for the identities generated.
 * </p>
 * <UL>
 * <li><U>table-name</U> - name of the table whose column we are generating the value for</li>
 * <li><U>column-name</U> - name of the column that we are generating the value for</li>
 * </UL>
 * </P>
 */
public final class TableGenerator extends AbstractRDBMSGenerator
{
    /** Table where we store the identities for each table. */
    private SequenceTable sequenceTable = null;

    /** Name of the sequence that we are storing values under in the SequenceTable. */
    private final String sequenceName;

    /** Default name for the datastore table storing the sequence values. Defaults to SEQUENCE_TABLE */
    public static final String DEFAULT_TABLE_NAME = "SEQUENCE_TABLE";

    /** Default name for the column storing the name of the sequence. */
    public static final String DEFAULT_SEQUENCE_COLUMN_NAME = "SEQUENCE_NAME";

    /** Default name for the column storing the next value of the sequence. */
    public static final String DEFAULT_NEXTVALUE_COLUMN_NAME = "NEXT_VAL";

    /**
     * Constructor.
     * @param name Symbolic name for this generator
     * @param props Properties defining the behaviour of this generator
     */
    public TableGenerator(String name, Properties props)
    {
        super(name, props);
        allocationSize = 5;
        initialValue = -1; // So we know if being set
        if (properties != null)
        {
            if (properties.get("key-cache-size") != null)
            {
                try
                {
                    allocationSize = Integer.parseInt(properties.getProperty("key-cache-size"));
                }
                catch (Exception e)
                {
                    throw new ValueGenerationException(LOCALISER.msg("Sequence040006",properties.get("key-cache-size")));
                }
            }
            if (properties.get("key-initial-value") != null)
            {
                try
                {
                    initialValue = Integer.parseInt(properties.getProperty("key-initial-value"));
                }
                catch (NumberFormatException nfe)
                {
                    // Not an integer so ignore it
                }
            }

            if (properties.getProperty("sequence-name") != null)
            {
                // Specified sequence-name so use that
                sequenceName = properties.getProperty("sequence-name");
            }
            else if (properties.getProperty("sequence-table-basis") != null && 
                properties.getProperty("sequence-table-basis").equalsIgnoreCase("table"))
            {
                // Use table name in the sequence table as the sequence name
                sequenceName = properties.getProperty("table-name");
            }
            else
            {
                // Use root class name (for this inheritance tree) in the sequence table as the sequence name
                sequenceName = properties.getProperty("root-class-name");
            }
        }
        else
        {
            // User hasn't provided any sequence name!!!
            sequenceName = "SEQUENCENAME";
        }
    }

    /**
     * Convenience accessor for the table being used.
     * @return The table
     */
    public SequenceTable getTable()
    {
        return sequenceTable;
    }

    /**
     * Method to reserve a block of "size" identities.
     * @param size Block size
     * @return The reserved block
     */
    public ValueGenerationBlock reserveBlock(long size)
    {
        if (size < 1)
        {
            return null;
        }

        // search for an ID in the database
        List oid = new ArrayList();
        try
        {
            if (sequenceTable == null)
            {
                initialiseSequenceTable();
            }

            DatastoreIdentifier sourceTableIdentifier = null;
            if (properties.getProperty("table-name") != null)
            {
                sourceTableIdentifier = ((MappedStoreManager)storeMgr).getIdentifierFactory().newDatastoreContainerIdentifier(properties.getProperty("table-name"));
                // TODO Apply passed in catalog/schema to this identifier rather than the default for the factory
            }
            Long nextId = sequenceTable.getNextVal(sequenceName, connection, (int)size,
                sourceTableIdentifier, properties.getProperty("column-name"), initialValue);
            for (int i=0; i<size; i++)
            {
                oid.add(nextId);
                nextId = Long.valueOf(nextId.longValue()+1);
            }
            if (NucleusLogger.VALUEGENERATION.isDebugEnabled())
            {
                NucleusLogger.VALUEGENERATION.debug(LOCALISER.msg("040004", "" + size));
            }
            return new ValueGenerationBlock(oid);
        }
        catch (SQLException e)
        {
            throw new ValueGenerationException(LOCALISER_RDBMS.msg("061001",e.getMessage()));
        }
    }

    /**
     * Indicator for whether the generator requires its own repository.
     * This class needs a repository so returns true.
     * @return Whether a repository is required.
     */
    protected boolean requiresRepository()
    {
        return true;
    }

    /**
     * Method to return if the repository already exists.
     * @return Whether the repository exists
     */
    protected boolean repositoryExists()
    {
        if (repositoryExists)
        {
            return repositoryExists;
        }
        else if (storeMgr.getBooleanProperty("datanucleus.rdbms.omitDatabaseMetaDataGetColumns"))
        {
            // Assumed to exist if ignoring DMD.getColumns()
            repositoryExists = true;
            return repositoryExists;
        }

        try
        {
            if (sequenceTable == null)
            {
                initialiseSequenceTable();
            }
            sequenceTable.exists((Connection)connection.getConnection(), true);
            repositoryExists = true;
            return true;
        }
        catch (SQLException sqle)
        {
            throw new ValueGenerationException("Exception thrown calling table.exists() for " + sequenceTable, sqle);
        }
    }

	/**
	 * Method to create the repository for ids to be stored.
	 * @return Whether it was created successfully.
	 */
	protected boolean createRepository()
	{
        RDBMSStoreManager srm = (RDBMSStoreManager)storeMgr;
        if (!srm.isAutoCreateTables())
        {
            throw new NucleusUserException(LOCALISER.msg("040011", sequenceTable));
        }

        try
        {
            if (sequenceTable == null)
            {
                initialiseSequenceTable();
            }
            sequenceTable.exists((Connection)connection.getConnection(), true);
            repositoryExists = true;
            return true;
        }
        catch (SQLException sqle)
        {
            throw new ValueGenerationException("Exception thrown calling table.exists() for " + sequenceTable, sqle);
        }
	}

    /**
     * Method to initialise the sequence table used for storing the sequence values.
     */
    protected void initialiseSequenceTable()
    {
        // Set catalog/schema name (using properties, and if not specified using the values for the table)
        String catalogName = properties.getProperty("sequence-catalog-name");
        if (catalogName == null)
        {
            catalogName = properties.getProperty("catalog-name");
        }
        String schemaName = properties.getProperty("sequence-schema-name");
        if (schemaName == null)
        {
            schemaName = properties.getProperty("schema-name");
        }
        String tableName = (properties.getProperty("sequence-table-name") == null ? 
                DEFAULT_TABLE_NAME : properties.getProperty("sequence-table-name"));

        MappedStoreManager storeMgr = (MappedStoreManager)this.storeMgr;
        DatastoreAdapter dba = storeMgr.getDatastoreAdapter();
        DatastoreIdentifier identifier = storeMgr.getIdentifierFactory().newDatastoreContainerIdentifier(tableName);
        if (dba.supportsOption(DatastoreAdapter.CATALOGS_IN_TABLE_DEFINITIONS) && catalogName != null)
        {
            identifier.setCatalogName(catalogName);
        }
        if (dba.supportsOption(DatastoreAdapter.SCHEMAS_IN_TABLE_DEFINITIONS) && schemaName != null)
        {
            identifier.setSchemaName(schemaName);
        }

        DatastoreClass table = storeMgr.getDatastoreClass(identifier);
        if (table != null)
        {
            sequenceTable = (SequenceTable)table;
        }
        else
        {
            String sequenceNameColumnName = DEFAULT_SEQUENCE_COLUMN_NAME;
            String nextValColumnName = DEFAULT_NEXTVALUE_COLUMN_NAME;
            if (properties.getProperty("sequence-name-column-name") != null)
            {
                sequenceNameColumnName = properties.getProperty("sequence-name-column-name");
            }
            if (properties.getProperty("sequence-nextval-column-name") != null)
            {
                nextValColumnName = properties.getProperty("sequence-nextval-column-name");
            }
            sequenceTable = new SequenceTable(identifier, (RDBMSStoreManager)storeMgr, sequenceNameColumnName, nextValColumnName);
            sequenceTable.initialize(storeMgr.getNucleusContext().getClassLoaderResolver(null));
            storeMgr.addDatastoreContainer(sequenceTable);
        }
    }
}