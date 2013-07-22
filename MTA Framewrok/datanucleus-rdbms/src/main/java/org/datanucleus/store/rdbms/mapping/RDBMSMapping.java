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
2004 Andy Jefferson - added getTypeInfo method
2005 Andy Jefferson - changed to use StoreManager instead of DatabaseAdapter
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import org.datanucleus.store.exceptions.UnsupportedDataTypeException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.AbstractDatastoreMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.util.Localiser;

/**
 * Implementation of the mapping of an RDBMS type.
 */
public abstract class RDBMSMapping extends AbstractDatastoreMapping
{
    protected static final Localiser LOCALISER_RDBMS = Localiser.getInstance(
        "org.datanucleus.store.rdbms.Localisation", RDBMSStoreManager.class.getClassLoader());

    /** Store Manager to use for mapping. */
    protected final RDBMSStoreManager storeMgr;

    /** The RDBMS Column being persisted to. */
    protected Column column;

    /**
     * Create a new Mapping with the given DatabaseAdapter for the given type.
     * @param storeMgr The Store Manager that this Mapping should use.
     * @param mapping Mapping for the underlying java type. This can be null on an "unmapped column".
     */
    protected RDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(mapping);

        this.storeMgr = (RDBMSStoreManager) storeMgr;
    }

    /**
     * Convenience to access the Datastore adapter as a DatabaseAdapter.
     * @return The adapter in use
     */
    protected RDBMSAdapter getDatabaseAdapter()
    {
        return (RDBMSAdapter)storeMgr.getDatastoreAdapter();
    }

    /**
     * Accessor for the (SQL) type info for this datastore type.
     * @return The type info
     */
    public abstract SQLTypeInfo getTypeInfo();

    /**
     * Accessor for whether the mapping is nullable.
     * @return Whether it is nullable
     */
    public boolean isNullable()
    {
        if (column != null)
        {
            return column.isNullable();
        }
        return true;
    }

    /**
     * Whether this mapping is included in the fetch statement.
     * @return Whether to include in fetch statement
     */
    public boolean includeInFetchStatement()
    {
        return true;
    }

    /**
     * Accessor for whether this mapping requires values inserting on an INSERT.
     * @return Whether values are to be inserted into this mapping on an INSERT
     */
    public boolean insertValuesOnInsert()
    {
        return getInsertionInputParameter().indexOf('?') > -1;
    }

    /**
     * Accessor for the string to put in any retrieval datastore statement for this field.
     * In RDBMS, this is typically a ? to be used in JDBC statements.
     * @return The input parameter
     */
    public String getInsertionInputParameter()
    {
        return column.getWrapperFunction(Column.WRAPPER_FUNCTION_INSERT);
    }

    /**
     * Accessor for the string to put in any update datastore statements for this field.
     * In RDBMS, this is typically a ? to be used in JDBC statements.
     * @return The input parameter.
     */
    public String getUpdateInputParameter()
    {
        return column.getWrapperFunction(Column.WRAPPER_FUNCTION_UPDATE);
    }

    /**
     * Accessor for the datastore field
     * @return The column
     */
    public DatastoreField getDatastoreField()
    {
        return column;
    }

    /**
     * Sets the TypeInfo for the columns of the Mapping.
     * Mappings using two or more columns using different TypeInfo(s) should
     * overwrite this method to appropriate set the TypeInfo (SQL type) for
     * all the columns
     */
    protected void initTypeInfo()
    {
        SQLTypeInfo typeInfo = getTypeInfo();
        if (typeInfo == null)
        {
            throw new UnsupportedDataTypeException(LOCALISER_RDBMS.msg("055000",column));
        }
    
        if (column != null)
        {
            column.setTypeInfo(typeInfo);
        }
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
    
        if (!(obj instanceof RDBMSMapping))
        {
            return false;
        }
    
        RDBMSMapping cm = (RDBMSMapping)obj;
    
        return getClass().equals(cm.getClass()) &&
            storeMgr.equals(cm.storeMgr) &&
            (column == null ? cm.column == null : column.equals(cm.column));
    }

    public int hashCode()
    {
        return storeMgr.hashCode() ^ (column == null ? 0 : column.hashCode());
    }

    /**
     * Utility to output any error message.
     * @param method The method that failed.
     * @param position The position of the column
     * @param e The exception
     * @return The localised failure message
     */
    protected String failureMessage(String method, int position, Exception e)
    {
        return LOCALISER.msg("041050", getClass().getName() + "." + method,
            position, column, e.getMessage());
    }

    /**
     * Utility to output any error message.
     * @param method The method that failed.
     * @param value Value at the position
     * @param e The exception
     * @return The localised failure message
     */
    protected String failureMessage(String method, Object value, Exception e)
    {
        return LOCALISER.msg("041050", getClass().getName() + "." + method,
            value, column, e.getMessage());
    }
}