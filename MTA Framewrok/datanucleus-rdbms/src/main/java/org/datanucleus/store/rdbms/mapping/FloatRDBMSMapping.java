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
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.exceptions.NullValueException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping of a Float RDBMS type.
 */
public class FloatRDBMSMapping extends DoubleRDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping THe java type mapping
     */
    protected FloatRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping The java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public FloatRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(storeMgr, mapping);
		column = (Column) field;
		initialize();
	}

    private void initialize()
    {
		initTypeInfo();
    }
    
    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.FLOAT, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.FLOAT);
    }

    public float getFloat(Object rs, int param)
    {
        float value;

        try
        {
            value = ((ResultSet) rs).getFloat(param);
            if (column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull())
            {
                if (((ResultSet) rs).wasNull())
                {
                    throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","float","" + param, column, e.getMessage()), e);
        }

        return value;
    }
    
    public void setFloat(Object ps, int param, float value)
    {
        try
        {
            ((PreparedStatement) ps).setFloat(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","float","" + value, column, e.getMessage()), e);
        }
    }
    
    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                ((PreparedStatement) ps).setNull(param, getTypeInfo().getDataType());
            }
            else if (value instanceof Integer)
            {
                ((PreparedStatement) ps).setFloat(param, ((Integer) value).floatValue());
            }
            else if (value instanceof Long)
            {
                ((PreparedStatement) ps).setFloat(param, ((Long) value).floatValue());
            }
            else if (value instanceof Short)
            {
                ((PreparedStatement) ps).setFloat(param, ((Short) value).floatValue());
            }
            else if (value instanceof BigInteger)
            {
                ((PreparedStatement) ps).setFloat(param, ((BigInteger) value).floatValue());
            }
            else if (value instanceof BigDecimal)
            {
                ((PreparedStatement) ps).setFloat(param, ((BigDecimal) value).floatValue());
            }
            else if (value instanceof Character)
            {
                String s = value.toString();
                ((PreparedStatement) ps).setFloat(param, s.charAt(0));                  
            }
            else if (value instanceof Float)
            {
                ((PreparedStatement) ps).setFloat(param, ((Float) value).floatValue());
            }
            else if (value instanceof Double)
            {
                ((PreparedStatement) ps).setDouble(param, ((Double) value).doubleValue());
            }
            else
            {
                ((PreparedStatement) ps).setFloat(param, ((Double) value).floatValue());
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "Object", "" + value, column, e.getMessage()), e);
        }
    }

    public Object getObject(Object rs, int param)
    {
        Object value;

        try
        {
            float d = ((ResultSet) rs).getFloat(param);
            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_INTEGER))
            {
                value = ((ResultSet) rs).wasNull() ? null : Integer.valueOf((int) d);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_LONG))
            {
                value = ((ResultSet) rs).wasNull() ? null : Long.valueOf((long) d);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_FLOAT))
            {
                value = ((ResultSet) rs).wasNull() ? null : new Float(d);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_DOUBLE))
            {
                double dbl = ((ResultSet)rs).getDouble(param);
                value = ((ResultSet) rs).wasNull() ? null : Double.valueOf(dbl);
            }
            else
            {
                value = ((ResultSet) rs).wasNull() ? null : Double.valueOf(d);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object","" + param, column, e.getMessage()), e);
        }

        return value;
    }    
}