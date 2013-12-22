/**********************************************************************
Copyright (c) 2002 Mike Martin and others. All rights reserved.
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
    Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.exceptions;

import java.util.Collection;
import java.util.Iterator;

import org.datanucleus.store.exceptions.DatastoreValidationException;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.util.Localiser;

/**
 * A <tt>MissingColumnException</tt> is thrown if an expected column is
 * not found in the database during schema validation.
 * @see org.datanucleus.store.rdbms.table.Column
 */
public class MissingColumnException extends DatastoreValidationException
{
    private static final Localiser LOCALISER_RDBMS=Localiser.getInstance("org.datanucleus.store.rdbms.Localisation",
        RDBMSStoreManager.class.getClassLoader());

    /**
     * Constructs a missing column exception.
     * @param table The table in which column(s) were missing.
     * @param columns The collection of Column(s) that were missing.
     */
    public MissingColumnException(DatastoreContainerObject table, Collection columns)
    {
        super(LOCALISER_RDBMS.msg("020010",table.toString(),getColumnNameList(columns)));
    }

    private static String getColumnNameList(Collection columns)
    {
        StringBuffer list = new StringBuffer();
        Iterator i = columns.iterator();

        while (i.hasNext())
        {
            if (list.length() > 0)
            {
                list.append(", ");
            }
            list.append(((DatastoreField)i.next()).getIdentifier());
        }

        return list.toString();
    }
}