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
import org.datanucleus.util.StringUtils;

/**
 * Mapping of a INTEGER RDBMS type.
 */
public class IntegerRDBMSMapping extends RDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping The java type mapping
     */
    protected IntegerRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public IntegerRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
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
            return storeMgr.getSQLTypeInfoForJDBCType(Types.INTEGER, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.INTEGER);
    }

    public void setChar(Object ps, int param, char value)
    {
        try
        {
            ((PreparedStatement) ps).setInt(param,value);                 
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","char","" + value, column,e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","char","" + param, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","int","" + value, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","long","" + value, column, e.getMessage()), e);
        }
    }

    public long getLong(Object rs, int param)
    {
        long value;

        try
        {
            value = ((ResultSet) rs).getLong(param);

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
                if (value instanceof Character)
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
                    ((PreparedStatement) ps).setLong(param,((Long)value).longValue());                  
                }
                else
                {
                    ((PreparedStatement) ps).setLong(param, ((Number)value).longValue());
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
            long i = ((ResultSet) rs).getLong(param);
            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_CHARACTER))
            {
            	value = ((ResultSet) rs).wasNull() ? null : Character.valueOf((char)i);
            }
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_STRING))
            {
            	value = ((ResultSet) rs).wasNull() ? null : Character.valueOf((char)i).toString();
            }            
            else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_LONG))
            {
                value = ((ResultSet) rs).wasNull() ? null : Long.valueOf(i);
            }            
			else
            {
                value = ((ResultSet) rs).wasNull() ? null : Integer.valueOf((int)i);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object","" + param, column, e.getMessage()), e);
        }

        return value;
    }    
}