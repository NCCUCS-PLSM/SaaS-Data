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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping of a DATE RDBMS type.
 */
public class DateRDBMSMapping extends RDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected DateRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field The field to be mapped
     */
    public DateRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(storeMgr, mapping);
		column = (Column) field;
		initialize();
	}

    private void initialize()
    {
        if (column != null)
        {
            column.checkPrimitive();
        }
		initTypeInfo();
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.DATE, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.DATE);
    }

    /**
     * Mutator for the object.
     * @param ps The JDBC Statement
     * @param param The Parameter position
     * @param value The value to set
     **/ 
    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                ((PreparedStatement) ps).setNull(param, getTypeInfo().getDataType());
            }
            else if (value instanceof java.util.Calendar)
            {
                ((PreparedStatement) ps).setDate(param, new Date(((java.util.Calendar)value).getTime().getTime()));
            }
            else if (value instanceof java.util.Date)
            {
                ((PreparedStatement) ps).setDate(param, new Date(((java.util.Date)value).getTime()));
            }
            else
            {
                // If the "value" was a java.sql.Date it would have been handled above, so this will give ClassCastException!
                ((PreparedStatement) ps).setDate(param, (Date)value);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","java.sql.Date","" + value), e);
        }
    }

    protected Date getDate(Object rs, int param)
    {
        Date value;

        try
        {
            value = ((ResultSet) rs).getDate(param);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","java.sql.Date","" + param), e);
        }

        return value;
    }

    /**
     * Accessor for the object.
     * @param rs The ResultSet to extract the value from
     * @param param The parameter position
     * @return The object value
     **/
    public Object getObject(Object rs, int param)
    {
        Date value = getDate(rs, param);

        if (value == null)
        {
            return null;
        }
        else
        {
            if( getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_UTIL_DATE))
            {
                return new java.util.Date(value.getTime());
            }
            else
            {
                return value;
            }
        }
    }
}