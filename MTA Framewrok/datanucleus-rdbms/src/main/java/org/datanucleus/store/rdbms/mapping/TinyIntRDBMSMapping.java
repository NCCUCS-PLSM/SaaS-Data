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
2006 Andy Jefferson - added support for boolean/Boolean
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.exceptions.NullValueException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.SingleFieldMapping;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.util.StringUtils;

/**
 * Mapping of a TINYINT RDBMS type.
 */
public class TinyIntRDBMSMapping extends RDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected TinyIntRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public TinyIntRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
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

            if (getJavaTypeMapping().getJavaType() == Boolean.class)
            {
                // With a Boolean we'll store it as 1, 0 (see setBoolean/getBoolean methods)
                StringBuffer constraints = new StringBuffer("CHECK (" + column.getIdentifier() + " IN (0,1)");
                if (column.isNullable())
                {
                    constraints.append(" OR " + column.getIdentifier() + " IS NULL");
                }
                constraints.append(')');
                column.setConstraints(constraints.toString());
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
            return storeMgr.getSQLTypeInfoForJDBCType(Types.TINYINT, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.TINYINT);
    }

    /**
     * Setter for when we are storing a boolean field as a TINYINT.
     * @param ps Prepared Statement
     * @param param Number of the parameter in the statement
     * @param value The boolean value
     */
    public void setBoolean(Object ps, int param, boolean value)
    {
        try
        {
            ((PreparedStatement) ps).setInt(param, value ? 1 : 0);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "boolean", "" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Getter for when we are storing a boolean field as a TINYINT.
     * @param rs Result Set from which to get the boolean
     * @param param Number of the parameter in the statement
     * @return The boolean value
     */
    public boolean getBoolean(Object rs, int param)
    {
        boolean value;

        try
        {
            int intValue = ((ResultSet) rs).getInt(param);
            if (intValue == 0)
            {
                value = false;
            }
            else if (intValue == 1)
            {
                value = true;
            }
            else
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055006", "Types.TINYINT", "" + intValue));
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Boolean", "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","int", "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","int", 
                "" + value, column, e.getMessage()), e);
        }
    }

    public long getLong(Object rs, int param)
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","int", 
                "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","byte", "" + param, column, e.getMessage()), e);
        }

        return value;
    }

    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                if (column != null && column.isDefaultable() && column.getDefaultValue() != null &&
                    !StringUtils.isWhitespace(column.getDefaultValue().toString()))
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
                if (value instanceof Byte)
                {
                    ((PreparedStatement) ps).setInt(param, ((Byte)value).shortValue());
                }
                else if (value instanceof BigInteger)
                {
                    ((PreparedStatement) ps).setInt(param, ((BigInteger)value).shortValue());
                }
                else if (value instanceof Boolean)
                {
                    ((PreparedStatement) ps).setInt(param, ((Boolean)value) ? 1 : 0);
                }
                else
                {
                    throw new NucleusException("TinyIntRDBMSMapping.setObject called for " + 
                        StringUtils.toJVMIDString(value) + " but not supported");
                }
                //TODO bug with SQL SERVER DRIVER. It doesn't accept Byte -128
                //ps.setByte(param, ((Byte)value).byteValue());
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Byte", "" + value, column, e.getMessage()), e);
        }
    }

    public Object getObject(Object rs, int param)
    {
        Object value;
        try
        {
            int d = ((ResultSet)rs).getInt(param);
            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_MATH_BIGINTEGER))
            {
                value = ((ResultSet)rs).wasNull() ? null : BigInteger.valueOf(d);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_BOOLEAN))
            {
                value = ((ResultSet)rs).wasNull() ? null : (d == 1 ? Boolean.TRUE : Boolean.FALSE);
            }
            else
            {
                value = ((ResultSet)rs).wasNull() ? null : Byte.valueOf(((ResultSet)rs).getByte(param));
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Byte", "" + param, column, e.getMessage()), e);
        }

        return value;
    }
}