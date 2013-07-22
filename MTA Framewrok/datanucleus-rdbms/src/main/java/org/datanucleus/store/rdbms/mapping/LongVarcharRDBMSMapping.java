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
2004 Andy Jefferson - localised messages
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping of a Long VARCHAR RDBMS type.
 */
public class LongVarcharRDBMSMapping extends RDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected LongVarcharRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public LongVarcharRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(storeMgr, mapping);
		column = (Column) field;
		initialize();
	}

    /**
     * Accessor for whether the mapping is string-based.
     * @return Whether the mapping is string based
     */
    public boolean isStringBased()
    {
        return true;
    }

    private void initialize()
    {
		initTypeInfo();
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.LONGVARCHAR, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.LONGVARCHAR);
    }

    public void setString(Object ps, int param, String value)
    {
        try
        {
            if (value == null)
            {
                if (column != null && column.isDefaultable() && column.getDefaultValue() != null)
                {
                    ((PreparedStatement) ps).setString(param,column.getDefaultValue().toString().trim());
                }
                else
                {
                    ((PreparedStatement) ps).setNull(param,getTypeInfo().getDataType());
                }
            }
            else
            {
                ((PreparedStatement) ps).setString(param, value);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","String", "" + value, column, e.getMessage()), e);
        }
    }

    public String getString(Object rs, int param)
    {
        String value;

        try
        {
            value = ((ResultSet) rs).getString(param);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","String", "" + param, column, e.getMessage()), e);
        }

        return value;
    }

    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                ((PreparedStatement) ps).setNull(param, getTypeInfo().getDataType());
            }
            else
            {
                ((PreparedStatement) ps).setString(param, ((String)value));
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object", "" + value, column, e.getMessage()), e);
        }
    }

    public Object getObject(Object rs, int param)
    {
        Object value;

        try
        {
            String s = ((ResultSet) rs).getString(param);
            value = ((ResultSet) rs).wasNull() ? null : s;
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object", "" + param, column, e.getMessage()), e);
        }

        return value;
    }    
}