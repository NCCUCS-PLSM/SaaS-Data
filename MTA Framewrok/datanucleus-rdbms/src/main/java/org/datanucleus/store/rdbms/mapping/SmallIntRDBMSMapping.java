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
2005 David Eaves - contributed support for byte/Byte
2006 Andy Jefferson - added support for boolean/Boolean
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

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
 * Mapping of a SMALLINT RDBMS type.
 */
public class SmallIntRDBMSMapping extends RDBMSMapping
{
    /**
     * Constructor.
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected SmallIntRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public SmallIntRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
        super(storeMgr, mapping);
        column = (Column) field;
        initialize();
    }

    /**
     * Initialise the mapping.
     */
    private void initialize()
    {
        if (column != null)
        {
            column.checkPrimitive();

            // Valid Values
            JavaTypeMapping m = getJavaTypeMapping();
            if (m instanceof SingleFieldMapping)
            {
                Object[] validValues = ((SingleFieldMapping)m).getValidValues(0);
                if (validValues != null)
                {
                    String constraints = 
                        ((RDBMSAdapter)storeMgr.getDatastoreAdapter()).getCheckConstraintForValues(
                            column.getIdentifier(), validValues, column.isNullable());
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

    /**
     * Accessor for type info
     * @return The type info for the datastore mapping
     */
    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.SMALLINT, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.SMALLINT);
    }

    /**
     * Setter for when we are storing a boolean field as a SMALLINT.
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
     * Getter for when we are storing a boolean field as a SMALLINT.
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
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055006", "Types.SMALLINT", "" + intValue));
            }

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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Boolean", "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "short", "" + value, column, e.getMessage()), e);
        }
    }

    public short getShort(Object rs, int param)
    {
        short value;

        try
        {
            value = ((ResultSet) rs).getShort(param);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "short", "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "int", "" + value, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "int", "" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "short", 
                "" + value, column, e.getMessage()), e);
        }
    }

    public long getLong(Object rs, int param)
    {
        long value;

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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "short", 
                "" + param, column, e.getMessage()), e);
        }

        return value;
    }

    public void setByte(Object ps, int param, byte value)
    {
        try
        {
            // TODO bug with MSSQL SERVER DRIVER. It doesn't accept Byte -128
            // ps.setByte(param, value);
            ((PreparedStatement) ps).setInt(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "byte", "" + value, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "byte", "" + param, column, e.getMessage()), e);
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
                Class type = value.getClass();
                if (type == Integer.class)
                {
                    ((PreparedStatement) ps).setShort(param, ((Integer)value).shortValue());
                }
                else if (type == Short.class)
                {
                    ((PreparedStatement) ps).setShort(param, ((Short)value).shortValue());
                }
                else if (type == Byte.class)
                {
                    ((PreparedStatement) ps).setShort(param, ((Byte)value).shortValue());
                }
                else if (type == Character.class)
                {
                    ((PreparedStatement) ps).setShort(param, (short)((Character)value).charValue());
                }
                else if (type == Boolean.class)
                {
                    ((PreparedStatement) ps).setShort(param, (short)(((Boolean)value) ? 1 : 0));
                }
                else if (type == BigInteger.class)
                {
                    ((PreparedStatement) ps).setShort(param, ((BigInteger)value).shortValue());
                }
                else if (type == Long.class)
                {
                    ((PreparedStatement) ps).setShort(param, ((Long)value).shortValue());
                }
                else
                {
                    throw new NucleusException("SmallIntRDBMSMapping.setObject called for " + 
                        StringUtils.toJVMIDString(value) + " but not supported");
                }
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
            short d = ((ResultSet) rs).getShort(param);
            Class type = getJavaTypeMapping().getJavaType();
            if (type == Short.class)
            {
                value = ((ResultSet)rs).wasNull() ? null : Short.valueOf(d);
            }
            else if (type == Integer.class)
            {
                value = ((ResultSet)rs).wasNull() ? null : Integer.valueOf(d);
            }
            else if (type == Byte.class)
            {
                value = ((ResultSet)rs).wasNull() ? null : Byte.valueOf((byte)d);
            }
            else if (type == BigInteger.class)
            {
                value = ((ResultSet)rs).wasNull() ? null : BigInteger.valueOf(d);
            }
            else if (type == Boolean.class)
            {
                value = ((ResultSet)rs).wasNull() ? null : (d == 1 ? Boolean.TRUE : Boolean.FALSE);
            }
            else
            {
                value = ((ResultSet) rs).wasNull() ? null : Short.valueOf(d);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "Object", "" + param, column, e.getMessage()), e);
        }

        return value;
    }
}