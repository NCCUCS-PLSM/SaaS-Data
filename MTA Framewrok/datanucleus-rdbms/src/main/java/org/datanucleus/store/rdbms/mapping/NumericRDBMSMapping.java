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
2007 Andy Jefferson - Added handling for Number
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
import org.datanucleus.store.mapped.mapping.SingleFieldMapping;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping of a numeric RDBMS type.
 */
public class NumericRDBMSMapping extends RDBMSMapping
{
    private static final int INT_MAX_DECIMAL_DIGITS = 10;

    /**
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected NumericRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public NumericRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
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
        if (column != null)
        {
            // Valid Values
            if (getJavaTypeMapping() instanceof SingleFieldMapping)
            {
                Object[] validValues = ((SingleFieldMapping)getJavaTypeMapping()).getValidValues(0);
                if (validValues != null)
                {
                    String constraints = ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getCheckConstraintForValues(column.getIdentifier(), validValues, column.isNullable());
                    column.setConstraints(constraints);
                }
            }

            /*
             * In case the default DECIMAL precision is less than the number of
             * digits we need, set it manually.
             */
            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_INTEGER))
            {
                // If the user hasn't set the precision, set it to the size necessary for
                // the Java type
                if (column.getColumnMetaData().getLength() == null)
                {
                    column.getColumnMetaData().setLength(INT_MAX_DECIMAL_DIGITS);
                }
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_BOOLEAN))
            {
                column.getColumnMetaData().setLength(1);

                StringBuffer constraints = new StringBuffer("CHECK (" + column.getIdentifier() + " IN (1,0)");
                if (column.isNullable())
                {
                    constraints.append(" OR " + column.getIdentifier() + " IS NULL");
                }
                constraints.append(')');
                column.setConstraints(constraints.toString());
                column.checkDecimal();
            }
        }
        initTypeInfo();
    }

    /**
     * Accessor for whether the mapping is integer-based.
     * @return Whether the mapping is integer based
     */
    public boolean isIntegerBased()
    {
        return true;
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.NUMERIC, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.NUMERIC);
    }

    public void setChar(Object ps, int param, char value)
    {
        try
        {
            ((PreparedStatement) ps).setInt(param,value);                 
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","char", "" + value, column, e.getMessage()), e);
        }
    }

    public char getChar(Object rs, int param)
    {
        char value;

        try
        {
            value = (char)((ResultSet) rs).getInt(param);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","char", "" + param, column, e.getMessage()), e);
        }
        return value;
    }
    
    public void setInt(Object ps, int param, int value)
    {
        try
        {
            ((PreparedStatement) ps).setInt(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","int", "" + value, column, e.getMessage()), e);
        }
    }

    public int getInt(Object rs, int param)
    {
        int value;

        try
        {
            value = ((ResultSet) rs).getInt(param);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","int", "" + param, column, e.getMessage()), e);
        }

        return value;
    }

    public void setByte(Object ps, int param, byte value)
    {
        try
        {
            //TODO bug with SQL SERVER DRIVER. It doesnt accept Byte -128
//          ps.setByte(param, value);
            ((PreparedStatement) ps).setInt(param, value);
            
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","byte", "" + value, column, e.getMessage()), e);
        }
    }

    public byte getByte(Object rs, int param)
    {
        byte value;

        try
        {
            value = ((ResultSet) rs).getByte(param);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002",
                "byte", "" + param, column, e.getMessage()), e);
        }

        return value;
    }
    
    public void setBoolean(Object ps, int param, boolean value)
    {
        try
        {
            ((PreparedStatement) ps).setBoolean(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001",
                "boolean", "" + value, column, e.getMessage()), e);
        }
    }

    public boolean getBoolean(Object rs, int param)
    {
        boolean value;

        try
        {
            value = ((ResultSet) rs).getBoolean(param);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002",
                "boolean", "" + param, column, e.getMessage()), e);
        }

        return value;
    }

    public void setDouble(Object ps, int param, double value)
    {
        try
        {
            ((PreparedStatement) ps).setDouble(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001",
                "double","" + value, column, e.getMessage()), e);
        }
    }

    public double getDouble(Object rs, int param)
    {
        double value;

        try
        {
            value = ((ResultSet) rs).getDouble(param);
            if (column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull())
            {
                if (((ResultSet) rs).wasNull())
                {
                    throw new NullValueException(LOCALISER_RDBMS.msg("055003", column));
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","double","" + param, column, e.getMessage()), e);
        }

        return value;
    }
    
    public void setFloat(Object ps, int param, float value)
    {
        try
        {
            ((PreparedStatement) ps).setDouble(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002",
                "float","" + value, column, e.getMessage()), e);
        }
    }    

    public float getFloat(Object rs, int param)
    {
        float value;

        try
        {
            value = (float) ((ResultSet) rs).getDouble(param);
            if (column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull())
            {
                if (((ResultSet)rs).wasNull())
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

    public void setShort(Object ps, int param, short value)
    {
        try
        {
            ((PreparedStatement) ps).setShort(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001",
                "short", "" + value,column, e.getMessage()), e);
        }
    }

    public short getShort(Object rs, int param)
    {
        short value;

        try
        {
            value = ((ResultSet) rs).getShort(param);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","short", "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","long", "" + value, column, e.getMessage()), e);
        }
    }

    public long getLong(Object rs, int param)
    {
        long value;

        try
        {
            value = ((ResultSet) rs).getLong(param);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","long", "" + param, column, e.getMessage()), e);
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
            else if (value instanceof Byte)
            {
                ((PreparedStatement) ps).setInt(param, ((Byte)value).byteValue());
            }
            else if (value instanceof Integer)
            {
                ((PreparedStatement) ps).setInt(param, ((Integer)value).intValue());
            }
            else if (value instanceof Character)
            {
                String s = value.toString();
                ((PreparedStatement) ps).setInt(param,s.charAt(0));                  
            }
            else if (value instanceof String)
            {
                String s = (String)value;
                ((PreparedStatement) ps).setInt(param,s.charAt(0));                  
            }             
            else if (value instanceof Long)
            {
                ((PreparedStatement) ps).setLong(param, ((Long)value).longValue());
            }            
            else if (value instanceof Float)
            {
                ((PreparedStatement) ps).setFloat(param, ((Float)value).floatValue());
            }           
            else if (value instanceof Double)
            {
                ((PreparedStatement) ps).setDouble(param, ((Double)value).doubleValue());
            }
            else if (value instanceof Short)
            {
                ((PreparedStatement) ps).setShort(param, ((Short)value).shortValue());
            }
            else if (value instanceof BigDecimal)
            {
                ((PreparedStatement) ps).setBigDecimal(param, (BigDecimal)value);
            }            
            else if (value instanceof Boolean)
            {
                ((PreparedStatement) ps).setBoolean(param, ((Boolean)value).booleanValue());
            }
            else
            {
                ((PreparedStatement) ps).setBigDecimal(param, new BigDecimal((BigInteger)value));
            }            
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Numeric", "" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Accessor for the value from a result set.
     * @param rs Result Set
     * @param param Position in result set
     * @return The value of the object
     */
    public Object getObject(Object rs, int param)
    {
        try
        {
            BigDecimal value = ((ResultSet) rs).getBigDecimal(param);
            if (value == null)
            {
                return null;
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(Number.class.getName()))
            {
                // "Number" means we accept any numeric type from the datastore
                // TODO Try to split out into Long, Float, Integer, etc somehow
                return value;
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_MATH_BIGINTEGER))
            {
                return value.toBigInteger();
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_INTEGER))
            {
                return Integer.valueOf(value.intValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_LONG))
            {
                return Long.valueOf(value.longValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_BOOLEAN))
            {
                return Boolean.valueOf(value.intValue() == 1 ? true : false);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_BYTE))
            {
                return Byte.valueOf(value.byteValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_SHORT))
            {
                return Short.valueOf(value.shortValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_FLOAT))
            {
                return Float.valueOf(value.floatValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_DOUBLE))
            {
                return Double.valueOf(value.doubleValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_CHARACTER))
            {
                return Character.valueOf((char) value.intValue());
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_STRING))
            {
                return Character.valueOf((char) value.intValue()).toString();
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_MATH_BIGDECIMAL))
            {
                return value;
            }
            else
            {
                return Long.valueOf(value.longValue());
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Numeric", "" + param, column, e.getMessage()),e);
        }
    }
}