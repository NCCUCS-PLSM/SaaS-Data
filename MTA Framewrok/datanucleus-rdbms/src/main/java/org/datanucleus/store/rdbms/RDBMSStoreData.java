/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.rdbms;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.MappedStoreData;
import org.datanucleus.store.rdbms.table.ViewImpl;

/**
 * Representation of a class (FCO) / field (SCO) that is persisted to an RDBMS table.
 * Extends the basic data to allow determination of whether it is a table or a view being represented.
 */
public class RDBMSStoreData extends MappedStoreData
{
    /**
     * Constructor for FCO data.
     * @param cmd MetaData for the class.
     * @param table Table where the class is stored.
     * @param tableOwner Whether the class is the owner of the table.
     */
    public RDBMSStoreData(ClassMetaData cmd, DatastoreContainerObject table, boolean tableOwner)
    {
        super(cmd, table, tableOwner);
    }

    /**
     * Constructor, taking the meta data for the field, and the table it is mapped to.
     * @param mmd MetaData for the field.
     * @param table Table definition
     */
    public RDBMSStoreData(AbstractMemberMetaData mmd, DatastoreContainerObject table)
    {
        super(mmd, table);
    }

    /**
     * Utility to return whether this table is a view.
     * @return Whether it is for a view.
     */
    public boolean mapsToView()
    {
        DatastoreContainerObject table = getDatastoreContainerObject();
        if (table == null)
        {
            return false;
        }
        return (table instanceof ViewImpl);
    }
}