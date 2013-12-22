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

import org.datanucleus.store.exceptions.DatastoreValidationException;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.util.Localiser;

/**
 * A <tt>UnexpectedColumnException</tt> is thrown if an unexpected column is
 * encountered in the database during schema validation.
 * @version $Revision: 1.3 $ 
 */
public class UnexpectedColumnException extends DatastoreValidationException
{
    private static final Localiser LOCALISER_RDBMS=Localiser.getInstance("org.datanucleus.store.rdbms.Localisation",
        RDBMSStoreManager.class.getClassLoader());

    /**
     * Constructs a unexpected column exception.
     * @param table_name The table in which the column was found.
     * @param column_name The name of the unexpected column.
     * @param schema_name The schema name
     * @param catalog_name The catalog name
     */
    public UnexpectedColumnException(String table_name, String column_name, String schema_name, String catalog_name)
    {
        super(LOCALISER_RDBMS.msg("020024",column_name,table_name,schema_name, catalog_name));
    }
}