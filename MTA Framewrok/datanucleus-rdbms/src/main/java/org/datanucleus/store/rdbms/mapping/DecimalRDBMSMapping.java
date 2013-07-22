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
 * Mapping of a Decimal RDBMS type.
 */
public class DecimalRDBMSMapping extends RDBMSMapping
{
    private static final int INT_MAX_DECIMAL_DIGITS = 10;
    private static final int LONG_MAX_DECIMAL_DIGITS = 19;

    /**
     * @param storeMgr Store Manager
     * @param mapping The java type mapping
     */
    protected DecimalRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public DecimalRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(storeMgr, mapping);
		column = (Column) field;
		initialize();
	}

    /**
     * Initialise the mapping, setting any default precision.
     */
    private void initialize()
    {
        // If the column has no precision specified, set its size.
        // If the user has already set their precision we do nothing here since they
        // want to control it.
        if (column != null && column.getColumnMetaData().getLength() == null)
        {
            // In case the default DECIMAL precision is less than the number of
            // digits we need, set it manually
            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_INTEGER))
            {
                column.getColumnMetaData().setLength(INT_MAX_DECIMAL_DIGITS);
                column.checkDecimal();
            }
            else
            {
                column.getColumnMetaData().setLength(Math.min(getTypeInfo().getPrecision(), LONG_MAX_DECIMAL_DIGITS));
                column.checkDecimal();
            }
        }
        initTypeInfo();
    }

    /**
     * Accessor for whether the mapping is decimal-based.
     * @return Whether the mapping is decimal based
     */
    public boolean isDecimalBased()
    {
        return true;
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.DECIMAL, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.DECIMAL);
    }

    public void setDouble(Object ps, int param, double value)
    {
        try
        {
            ((PreparedStatement) ps).setDouble(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","double","" + value, column, e.getMessage()), e);
        }
    }
    
    public void setFloat(Object ps, int param, float value)
    {
        try
        {
            ((PreparedStatement) ps).setDouble(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","float","" + value, column, e.getMessage()), e);
        }
    }    
    
    public void setInt(Object ps, int param, int value)
    {
        try
        {
            ((PreparedStatement) ps).setInt(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","int","" + value, column, e.getMessage()), e);
        }
    }

    public double getDouble(Object rs, int param)
    {
        double value;

        try
        {
            value = ((ResultSet) rs).getDouble(param);

            if (column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
            {
                if (((ResultSet) rs).wasNull())
                {
                    throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","double","" + param, column, e.getMessage()), e);
        }

        return value;
    }
    
    public float getFloat(Object rs, int param)
    {
        float value;

        try
        {
            value = (float) ((ResultSet) rs).getDouble(param);

            if( column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
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
    
    public int getInt(Object rs, int param)
    {
        int value;

        try
        {
            value = ((ResultSet) rs).getInt(param);

            if( column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
            {
                if (((ResultSet) rs).wasNull())
                {
                    throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","int","" + param, column, e.getMessage()), e);
        }

        return value;
    }

    public void setLong(Object ps, int param, long value)
    {
        try
        {
            ((PreparedStatement) ps).setLong(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","long","" + value, column,e.getMessage()), e);
        }
    }

    public long getLong(Object rs, int param)
    {
        long value;

        try
        {
            value = ((ResultSet) rs).getLong(param);

            if( column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
            {
                if (((ResultSet) rs).wasNull())
                {
                    throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","long","" + param, column, e.getMessage()), e);
        }

        return value;
    }
    
    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                if (column!= null && column.isDefaultable() && column.getDefaultValue() != null)
                {
                    ((PreparedStatement) ps).setInt(param, Integer.valueOf(column.getDefaultValue().toString()).intValue());
                }
                else
                {
                    ((PreparedStatement) ps).setNull(param, getTypeInfo().getDataType());
                }
            }
            else
            {
                if( value instanceof Integer) 
                {
                    ((PreparedStatement) ps).setBigDecimal(param, new BigDecimal(((Integer)value).intValue()));
                }
                else if( value instanceof Long) 
                {
                    ((PreparedStatement) ps).setBigDecimal(param, new BigDecimal(((Long)value).longValue()));
                }
                else if( value instanceof BigDecimal) 
                {
                    ((PreparedStatement) ps).setBigDecimal(param, (BigDecimal)value);
                }
                else if( value instanceof Float )
                {
                    ((PreparedStatement) ps).setDouble(param, ((Float)value).doubleValue());
                }            
                else if( value instanceof Double )
                {
                    ((PreparedStatement) ps).setDouble(param, ((Double)value).doubleValue());
                }                
                else if( value instanceof BigInteger) 
                {
                    ((PreparedStatement) ps).setBigDecimal(param, new BigDecimal((BigInteger)value));
                }                
                else
                {
                    ((PreparedStatement) ps).setInt(param, ((Integer)value).intValue());
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object","" + value, column, e.getMessage()), e);
        }
    }

    public Object getObject(Object rs, int param)
    {
        Object value;

        try
        {
            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_INTEGER))
            {
                value = ((ResultSet) rs).getBigDecimal(param);                
	            value = value == null ? null : Integer.valueOf(((BigDecimal)value).toBigInteger().intValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_LONG))
            {
                value = ((ResultSet) rs).getBigDecimal(param);                
	            value = value == null ? null : Long.valueOf(((BigDecimal)value).toBigInteger().longValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_MATH_BIGINTEGER))
            {
                value = ((ResultSet) rs).getBigDecimal(param);                
	            value = value == null ? null : ((BigDecimal)value).toBigInteger();
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_MATH_BIGDECIMAL))
            {
                value = ((ResultSet) rs).getBigDecimal(param);                
            }            
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_FLOAT))
            {
                double d = ((ResultSet) rs).getDouble(param);                
                value = ((ResultSet) rs).wasNull() ? null : new Float(d);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_DOUBLE))
            {
                double d = ((ResultSet) rs).getDouble(param);                
                value = ((ResultSet) rs).wasNull() ? null : new Double(d);
            }            
            else
            {
	            int i = ((ResultSet) rs).getInt(param);
	            value = ((ResultSet) rs).wasNull() ? null : Integer.valueOf(i);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object","" + param, column, e.getMessage()), e);
        }

        return value;
    }    
}