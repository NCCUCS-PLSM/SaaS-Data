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

import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.schema.DB2TypeInfo;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping of a Datalink RDBMS type (used by DB2).
 */
public class DatalinkRDBMSMapping extends CharRDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected DatalinkRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public DatalinkRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(mapping, storeMgr, field);
	}

    protected void initialize()
    {
        if (column != null)
        {
            if (mapping.getMemberMetaData().getValueForExtension("select-function") == null)
            {
                column.setWrapperFunction("DLURLCOMPLETEONLY(?)", Column.WRAPPER_FUNCTION_SELECT);
            }
        }
		initTypeInfo();
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(DB2TypeInfo.DATALINK, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(DB2TypeInfo.DATALINK);
    }

    public String getInsertionInputParameter()
    {
        //instead of (?) we use (? || '') as workaround
        //could be replaced with something like ? CAST ( ? AS varchar(255) ) 
        return "DLVALUE(? || '')";
    }
    
    public boolean includeInFetchStatement()
    {
        return true;
    }

    public String getUpdateInputParameter()
    {
        //instead of (?) we use (? || '') as workaround
        //could be replaced with something like ? CAST ( ? AS varchar(255) ) 
        return "DLVALUE(? || '')";
    }	
}