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
import org.datanucleus.store.exceptions.NullValueException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping of a REAL RDBMS type.
 */
public class RealRDBMSMapping extends RDBMSMapping
{
    /**
     * @param storeMgr Store Manager
     * @param mapping java type mapping
     */
    protected RealRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public RealRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
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
            return storeMgr.getSQLTypeInfoForJDBCType(Types.REAL, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.REAL);
    }

    public void setFloat(Object ps, int param, float value)
    {
        try
        {
            ((PreparedStatement) ps).setFloat(param, value);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","float","" + value, column, e.getMessage()), e);
        }
    }

    public float getFloat(Object rs, int param)
    {
        float value;

        try
        {
            value = ((ResultSet) rs).getFloat(param);
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
            try
            {
                //when value is real in database, cause cause a parse error when calling getFloat
                //JDBC error:Value can not be converted to requested type.

                value = Float.parseFloat(((ResultSet) rs).getString(param));
                if( column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
                {
                    if (((ResultSet) rs).wasNull())
                    {
                        throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                    }
                }
            }
            catch (SQLException e1)
            {
                try
                {
                    throw new NucleusDataStoreException("Can't get float result: param = " + param + " - " +((ResultSet) rs).getString(param), e);
                }
                catch (SQLException e2)
                {
                    throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","float","" + param, column, e.getMessage()), e);
                }
            }
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
                ((PreparedStatement) ps).setFloat(param, ((Float)value).floatValue());
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
            float f = ((ResultSet) rs).getFloat(param);
            value = ((ResultSet) rs).wasNull() ? null : new Float(f);
        }
		catch (SQLException e)
		{
            try
            {
                // when value is real in database, cause cause a parse error
                // when calling getFloat
                // JDBC error:Value can not be converted to requested type.
                value = new Float(Float.parseFloat(((ResultSet) rs).getString(param)));
                if (column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
                {
                    if (((ResultSet) rs).wasNull())
                    {
                        throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                    }
                }
            }
            catch (SQLException e1)
            {
                try
                {
                    throw new NucleusDataStoreException("Can't get float result: param = " + param + " - " + ((ResultSet) rs).getString(param), e);
                }
                catch (SQLException e2)
                {
                    throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "Object", "" + param, column, e.getMessage()), e);
                }
            }
        }

        return value;
    }
}