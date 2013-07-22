/**********************************************************************
Copyright (c) 2002 David Jencks and others. All rights reserved.
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
2003 Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.adapter;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;

/**
 * Provides methods for adapting SQL language elements to the Firebird database.
 * @see DatabaseAdapter
 */
public class FirebirdAdapter extends DatabaseAdapter
{
    /**
     * Constructs a Firebird adapter based on the given JDBC metadata.
     * @param metadata the database metadata.
     */
    public FirebirdAdapter(DatabaseMetaData metadata)
    {
        super(metadata);

        supportedOptions.remove(DEFERRED_CONSTRAINTS);
        supportedOptions.remove(BOOLEAN_COMPARISON);
        supportedOptions.remove(NULLS_IN_CANDIDATE_KEYS);
        supportedOptions.remove(NULLS_KEYWORD_IN_COLUMN_OPTIONS);
        supportedOptions.remove(INCLUDE_ORDERBY_COLS_IN_SELECT);
        supportedOptions.add(ALTER_TABLE_DROP_FOREIGN_KEY_CONSTRAINT);
        supportedOptions.add(CREATE_INDEXES_BEFORE_FOREIGN_KEYS);
        supportedOptions.add(LOCK_WITH_SELECT_FOR_UPDATE);
        supportedOptions.add(SEQUENCES);
    }

    public String getVendorID()
    {
        return "firebird";
    }

    public String getDropTableStatement(DatastoreContainerObject table)
    {
        return "DROP TABLE " + table.toString();
    }

    public SQLTypeInfo newSQLTypeInfo(ResultSet rs)
    {
        return new org.datanucleus.store.rdbms.schema.FirebirdTypeInfo(rs);
    }

    /**
     * Accessor for the sequence create statement for this datastore.
     * TODO Change param types to int.
     * @param sequence_name Name of the sequence 
     * @param min Minimum value for the sequence
     * @param max Maximum value for the sequence
     * @param start Start value for the sequence
     * @param increment Increment value for the sequence
     * @param cache_size Cache size for the sequence
     * @return The statement for getting the next id from the sequence
     */
    public String getSequenceCreateStmt(String sequence_name,
            Integer min,Integer max, Integer start, Integer increment, Integer cache_size)
    {
        if (sequence_name == null)
        {
            throw new NucleusUserException(LOCALISER.msg("051028"));
        }

        StringBuffer stmt = new StringBuffer("CREATE GENERATOR ");
        stmt.append(sequence_name);
        // TODO Can we use the additional parameters ?

        return stmt.toString();
    }

    /**
     * Accessor for the sequence statement to get the next id for this datastore.
     * @param sequence_name Name of the sequence 
     * @return The statement for getting the next id for the sequence
     **/
    public String getSequenceNextStmt(String sequence_name)
    {
        if (sequence_name == null)
        {
            throw new NucleusUserException(LOCALISER.msg("051028"));
        }

        StringBuffer stmt=new StringBuffer("SELECT GEN_ID(");
        stmt.append(sequence_name);
        stmt.append(",1) FROM RDB$DATABASE");

        return stmt.toString();
    }
}