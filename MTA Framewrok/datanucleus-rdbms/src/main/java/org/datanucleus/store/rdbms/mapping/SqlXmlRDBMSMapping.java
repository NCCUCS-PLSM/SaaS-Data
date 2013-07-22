/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;

/**
 * Mapping of an SQLXML RDBMS type.
 */
public class SqlXmlRDBMSMapping extends LongVarcharRDBMSMapping
{
    /**
     * Constructor for an RDBMS mapping of an SQLXML type.
     * @param storeMgr Store Manager
     * @param mapping The mapping
     */
    public SqlXmlRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor for an RDBMS mapping of an SQLXML type, for a column.
     * @param mapping The mapping
     * @param storeMgr Store Manager
     * @param col The column
     */
    public SqlXmlRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField col)
    {
        super(mapping, storeMgr, col);
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.SQLXML, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.SQLXML);
    }
}