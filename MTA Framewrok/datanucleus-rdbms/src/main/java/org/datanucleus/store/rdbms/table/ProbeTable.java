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
2003 Andy Jefferson - added catalog name
2003 Andy Jefferson - added default catalog/schema
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.IdentifierCase;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.util.NucleusLogger;

/**
 * Utility class used for detecting database schema existence and provides
 * means of determining the schema name.
 */ 
public class ProbeTable extends TableImpl
{
    /**
     * Constructor
     * @param storeMgr The RDBMSManager for this datastore
     **/
    public ProbeTable(RDBMSStoreManager storeMgr)
    {
        super(storeMgr.getIdentifierFactory().newDatastoreContainerIdentifier("DELETEME" + System.currentTimeMillis()),storeMgr);
    }

    /**
     * Method to initialise the table.
     * @param clr The ClassLoaderResolver
     **/
    public void initialize(ClassLoaderResolver clr)
    {
        assertIsUninitialized();

		JavaTypeMapping mapping = storeMgr.getMappingManager().getMapping(int.class);
		DatastoreField column = addDatastoreField(int.class.getName(), 
            storeMgr.getIdentifierFactory().newDatastoreFieldIdentifier("UNUSED"), mapping, null);
		getStoreManager().getMappingManager().createDatastoreMapping(mapping, column, int.class.getName());

        state = TABLE_STATE_INITIALIZED;
    }

    /**
     * Accessor for a mapping for the ID (PersistenceCapable) for this table.
     * @return The (PersistenceCapable) ID mapping.
     **/
    public JavaTypeMapping getIdMapping()
    {
        throw new NucleusException("Attempt to get ID mapping of ProbeTable!").setFatal();
    }

    /**
     * Accessor for the Schema details. This will return a String array with 2
     * elements. The first is the Catalog name, and the second the Schema name.
     * @param conn Connection for this datastore.
     * @return The Schema details
     * @throws SQLException Thrown when an error occurs in the process.
     **/
    public String[] findSchemaDetails(Connection conn)
    throws SQLException
    {
        String[] schemaDetails = new String[2];

        DatabaseMetaData dmd = conn.getMetaData();

        // Make sure the table name is in the correct case.
        // This is required by RDBMS such as PostgreSQL which allow creation in one format yet
        // actually store it in another.
        String table_name = identifier.getIdentifierName();
        if (storeMgr.getIdentifierFactory().getIdentifierCase() == IdentifierCase.LOWER_CASE ||
            storeMgr.getIdentifierFactory().getIdentifierCase() == IdentifierCase.LOWER_CASE_QUOTED)
        {
            table_name = table_name.toLowerCase();
        }
        else if (storeMgr.getIdentifierFactory().getIdentifierCase() == IdentifierCase.UPPER_CASE ||
            storeMgr.getIdentifierFactory().getIdentifierCase() == IdentifierCase.UPPER_CASE_QUOTED)
        {
            table_name = table_name.toUpperCase();
        }

        // Utilise default catalog/schema if available and applicable
        String catalog_name = storeMgr.getStringProperty(PropertyNames.PROPERTY_MAPPING_CATALOG);
        String schema_name = storeMgr.getStringProperty(PropertyNames.PROPERTY_MAPPING_SCHEMA);
        if (!dba.supportsOption(DatastoreAdapter.CATALOGS_IN_TABLE_DEFINITIONS))
        {
            catalog_name = null;
        }
        if (!dba.supportsOption(DatastoreAdapter.SCHEMAS_IN_TABLE_DEFINITIONS))
        {
            schema_name = null;
        }

        // Find the schema details
        ResultSet rs = dmd.getTables(catalog_name,schema_name,table_name,null);
        try
        {
            if (!rs.next())
            {
                throw new NucleusDataStoreException(LOCALISER.msg("057027",identifier));
            }

            schemaDetails[0] = rs.getString(1);
            schemaDetails[1] = rs.getString(2);
        }
        finally
        {
            rs.close();
        }

        // Log any failures in this process due to the database adapter
        if (schemaDetails[0] == null)
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("057026"));
        }
        if (schemaDetails[1] == null)
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("057025"));
        }

        return schemaDetails;
    }

    /**
     * Override to always really create ProbeTables in the DB. Needed to
     * determine schema name.
     * @see org.datanucleus.store.rdbms.table.AbstractTable#allowDDLOutput()
     */
    protected boolean allowDDLOutput()
    {
        return false;
    }

    /**
     * Accessor the for the mapping for a field/property stored in this table.
     * @param mmd MetaData for the field whose mapping we want
     * @return The mapping
     */
    public JavaTypeMapping getMemberMapping(AbstractMemberMetaData mmd)
    {
        return null;
    }
}