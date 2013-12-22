/**********************************************************************
Copyright (c) 2009 Anton Troshin. All rights reserved.
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

**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.SingleFieldMapping;
/**
 * Mapping of a VARBINARY RDBMS type for TimesTen database.
 * Provides default length specifications for the VARBINARY column.
 */
public class TimesTenVarBinaryRDBMSMapping extends VarBinaryRDBMSMapping
{

    public TimesTenVarBinaryRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
        super(mapping, storeMgr, field);
    }

    protected TimesTenVarBinaryRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Method to initialise the column mapping.
     * Provides default length specifications for the
     * VARBINARY column to fit the data being stored.
     */
    protected void initialize()
    {
        if (column != null)
        {
            // Default Length
            if (getJavaTypeMapping() instanceof SingleFieldMapping && column.getColumnMetaData().getLength() == null)
            {
                SingleFieldMapping m = (SingleFieldMapping) getJavaTypeMapping();
                if (m.getDefaultLength(0) > 0)
                {
                    // No column length provided by user and the type has a default length so use it
                    column.getColumnMetaData().setLength(m.getDefaultLength(0));
                }
            }

            if (column.getColumnMetaData().getLength() == null)
            {
                // Use the default string length
                // todo set length from persistence configuration
                /* column.getColumnMetaData().setLength(storeMgr.getNucleusContext().getPersistenceConfiguration().
              getIntProperty("datanucleus.rdbms.varBinaryDefaultLength"));  */
                column.getColumnMetaData().setLength(1024);
            }
        }
        super.initialize();
    }
}
