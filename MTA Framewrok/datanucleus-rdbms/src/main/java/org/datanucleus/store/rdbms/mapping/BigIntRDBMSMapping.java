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
2004 Andy Jefferson - fixed getObject() to cater for decimal values
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
 * Mapping of a Big Integer RDBMS type.
 */
public class BigIntRDBMSMapping extends RDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping The java mapping
     */
    protected BigIntRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */    
    public BigIntRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
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
        }
		initTypeInfo();
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.BIGINT, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.BIGINT);
    }

    public void setInt(Object ps, int param, int value)
    {
        try
        {
            ((PreparedStatement) ps).setLong(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "int", "" + value), e);
        }
    }

    public int getInt(Object rs, int param)
    {
        int value;

        try
        {
            value = (int)((ResultSet) rs).getLong(param);

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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "long", "" + value, column, e.getMessage()), e);
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
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "long", "" + param, column, e.getMessage()), e);
        }

        return value;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.mapped.mapping.AbstractDatastoreMapping#setString(java.lang.Object, int, java.lang.String)
     */
    @Override
    public void setString(Object preparedStatement, int exprIndex, String value)
    {
        super.setLong(preparedStatement, exprIndex, Long.parseLong(value));
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.mapped.mapping.AbstractDatastoreMapping#getString(java.lang.Object, int)
     */
    @Override
    public String getString(Object resultSet, int exprIndex)
    {
        return Long.toString(super.getLong(resultSet, exprIndex));
    }

    /**
     * Setter for a parameter in a PreparedStatement
     * @param ps The PreparedStatement
     * @param param The parameter number to set
     * @param value The value to set it to.
     */
    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                if (column != null && column.isDefaultable() && column.getDefaultValue() != null &&
                    !StringUtils.isWhitespace(column.getDefaultValue().toString()))
                {
                    ((PreparedStatement) ps).setLong(param, Long.valueOf(column.getDefaultValue().toString().trim()).longValue());
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
                else if (value instanceof java.util.Date)
                {
                    ((PreparedStatement) ps).setLong(param,((java.util.Date)value).getTime());                  
                }                
                else
                {
                    ((PreparedStatement) ps).setLong(param, ((Number)value).longValue());
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "Long", "" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Method to retrieve a Big int from a ResultSet.
     * @param rs ResultSet
     * @param param The Parameter number in the result set
     * @return The BIGINT object
     */
    public Object getObject(Object rs, int param)
    {
        Object value;
        try
        {
            // Read the object as a String since that is the most DB independent
            // type we can use and should always get us something.
            String str = ((ResultSet) rs).getString(param);
            if (((ResultSet)rs).wasNull())
            {
                value = null;
            }
            else
            {

				// Some RDBMS (e.g PostgreSQL) can return a long as a double
                // so cater for this, and generate a Long :-)
                try
                {
                    // Try it as a long
                    value = Long.valueOf(str);
                }
                catch (NumberFormatException nfe)
                {
                    // Must be a double precision, so cast it
                    value = Long.valueOf((new Double(str)).longValue());
                }
	            if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_UTIL_DATE))
	            {
	                value = new java.util.Date(((Long)value).longValue());
	            }            
            }
        }
        catch (SQLException e)
        {
            String msg = LOCALISER_RDBMS.msg("055002", "Long", "" + param, column, e.getMessage());
            throw new NucleusDataStoreException(msg, e);
        }

        return value;
    }
}