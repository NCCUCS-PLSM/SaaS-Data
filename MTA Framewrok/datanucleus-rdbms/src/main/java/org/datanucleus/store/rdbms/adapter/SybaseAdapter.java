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
2006 Thomas Corte - updates for Sybase 15
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.adapter;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;

import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.rdbms.schema.RDBMSColumnInfo;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Table;

/**
 * Provides methods for adapting SQL language elements to the Sybase database.
 */
public class SybaseAdapter extends DatabaseAdapter
{
    /**
     * Constructor.
     * @param metadata MetaData for the DB
     */
    public SybaseAdapter(DatabaseMetaData metadata)
    {
        super(metadata);

        supportedOptions.add(IDENTITY_COLUMNS);
        supportedOptions.add(STORED_PROCEDURES);
        supportedOptions.remove(DEFERRED_CONSTRAINTS);
        supportedOptions.remove(BOOLEAN_COMPARISON);
        supportedOptions.remove(LOCK_WITH_SELECT_FOR_UPDATE);
        supportedOptions.remove(AUTO_INCREMENT_KEYS_NULL_SPECIFICATION);
    }

    public String getVendorID()
    {
        return "sybase";
    }

    /**
     * Accessor for the DROP TABLE statement for Sybase.
     * Sybase doesnt support CASCADE CONSTRAINTS so we just return a simple
     * DROP TABLE table-name
     * @param table The table to drop.
     * @return The DROP TABLE statement
     **/
	public String getDropTableStatement(DatastoreContainerObject table)
	{
		return "DROP TABLE " + table.toString();
	}

    public SQLTypeInfo newSQLTypeInfo(ResultSet rs)
    {
        SQLTypeInfo info = new SQLTypeInfo(rs);

        // Discard the tinyint type because it doesn't support negative values.
        if (info.getTypeName().toLowerCase().startsWith("tinyint"))
        {
            return null;
        }
        // Discard the longsysname type because it doesn't allow specification of length
        if (info.getTypeName().toLowerCase().startsWith("longsysname"))
        {
            return null;
        }
        return info;
    }

    /**
     * Method to create a column info for the current row.
     * Overrides the dataType/columnSize/decimalDigits to cater for Sybase particularities.
     * @param rs ResultSet from DatabaseMetaData.getColumns()
     * @return column info
     */
    public RDBMSColumnInfo newRDBMSColumnInfo(ResultSet rs)
    {
        RDBMSColumnInfo info = new RDBMSColumnInfo(rs);

        short dataType = info.getDataType();
        switch (dataType)
        {
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                // Values > 0 inexplicably get returned here.
                info.setDecimalDigits(0);
                break;
            default:
                break;
        }

        return info;
    }

    /**
     * Accessor for the auto-increment sql statement for this datastore.
     * @param table Name of the table that the autoincrement is for
     * @param columnName Name of the column that the autoincrement is for
     * @return The statement for getting the latest auto-increment key
     */
    public String getAutoIncrementStmt(Table table, String columnName)
    {
        return "SELECT @@IDENTITY";
    }

    /**
     * Accessor for the auto-increment keyword for generating DDLs (CREATE TABLEs...).
     * @return The keyword for a column using auto-increment
     */
    public String getAutoIncrementKeyword()
    {
        return "IDENTITY";
    }
}