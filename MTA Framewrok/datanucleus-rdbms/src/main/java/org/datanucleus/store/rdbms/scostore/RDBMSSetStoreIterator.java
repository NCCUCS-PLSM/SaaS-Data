/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.scostore;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.table.JoinTable;

/**
 * RDBMS-specific implementation of {@link SetStoreIterator}.
 */
class RDBMSSetStoreIterator extends SetStoreIterator
{
    RDBMSSetStoreIterator(ObjectProvider sm, Object rs, ResultObjectFactory rof, AbstractSetStore backingStore)
        throws MappedDatastoreException
    {
        super(sm, rs, rof, backingStore);
    }

    protected boolean next(Object rs) throws MappedDatastoreException
    {
        try
        {
            return ((ResultSet) rs).next();
        }
        catch (SQLException e)
        {
            throw new MappedDatastoreException("SQLException", e);
        }
    }

    protected AbstractMemberMetaData getOwnerMemberMetaData(DatastoreContainerObject containerTable)
    {
        return ((JoinTable) containerTable).getOwnerMemberMetaData();
    }
}
