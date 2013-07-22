/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved. 
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
package org.datanucleus.store.rdbms.mapping;

import java.sql.Types;

import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.SingleFieldMapping;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;

/**
 * Mapping of a VARCHAR RDBMS type.
 */
public class VarCharRDBMSMapping extends CharRDBMSMapping
{
    /**
     * Constructor.
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected VarCharRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public VarCharRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(mapping, storeMgr, field);
	}

    /**
     * Method to initialise the column mapping.
     * Provides default length specifications for the VARCHAR column to fit the data being stored.
     */
    protected void initialize()
    {
        if (column != null)
        {
            // Default Length
            if (getJavaTypeMapping() instanceof SingleFieldMapping && column.getColumnMetaData().getLength() == null)
            {
                SingleFieldMapping m = (SingleFieldMapping)getJavaTypeMapping();
                if (m.getDefaultLength(0) > 0)
                {
                    // No column length provided by user and the type has a default length so use it
                    column.getColumnMetaData().setLength(m.getDefaultLength(0));
                }
            }

            column.checkString();

            // Valid Values
            if (getJavaTypeMapping() instanceof SingleFieldMapping)
            {
                Object[] validValues = ((SingleFieldMapping)getJavaTypeMapping()).getValidValues(0);
                if (validValues != null)
                {
                    String constraints = ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getCheckConstraintForValues(column.getIdentifier(), validValues, column.isNullable());
                    column.setConstraints(constraints);
                }
            }
        }
		initTypeInfo();
    }

    /**
     * Accessor for datastore type info for this mapping.
     * @return The datastore type
     */
    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.VARCHAR, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.VARCHAR);
    }
}