/**********************************************************************
Copyright (c) 2003 Andy Jefferson and others. All rights reserved.
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
	TJDO - original version
	Andy Jefferson - equality operator
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.key;

import org.datanucleus.store.mapped.DatastoreContainerObject;

/**
 * Representation of the primary key of a table.
 */
public class PrimaryKey extends CandidateKey
{
    /**
     * Creates a primary key. A default name of the primary key is created by the
     * constructor. This name can be overwritten.
     * @param table Table that this is the PK for
     */
    public PrimaryKey(DatastoreContainerObject table)
    {
        super(table);
        name = table.getStoreManager().getIdentifierFactory().newPrimaryKeyIdentifier(table).getIdentifierName();
    }

    /**
     * Equality operator
     * @param obj The object to compare against
     * @return Whether they are equal
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof PrimaryKey))
        {
            return false;
        }

        // Check for same no of columns
        PrimaryKey pk=(PrimaryKey)obj;
        if (pk.columns.size() != columns.size())
        {
            return false;
        }

        // Defer to superclass
        return super.equals(obj);
    }

    /**
     * Stringifier method.
     * Generates a form of the PK ready to be used in a DDL statement.
     * e.g PRIMARY KEY (col1,col2)
     * @return The string form of this object. Ready to be used in a DDL statement.
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer("PRIMARY KEY ").append(getColumnList(columns));

        return s.toString();
    }
}