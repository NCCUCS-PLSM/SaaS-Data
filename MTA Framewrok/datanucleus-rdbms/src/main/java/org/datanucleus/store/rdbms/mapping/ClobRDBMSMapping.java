/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved. 
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
2005 David Eaves - contributed ClobRDBMSMapping
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.datatype.ClobImpl;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;

/**
 * Mapping of a Clob RDBMS type.
 */
public class ClobRDBMSMapping extends LongVarcharRDBMSMapping
{
    /**
     * Constructor.
     * @param storeMgr Manager for the store
     * @param mapping The java type mapping for the field
     */
    public ClobRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping The java type mapping for the field.
     * @param storeMgr Manager for the store
     * @param field The field
     */
    public ClobRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
        super(mapping, storeMgr, field);
    }
    
    /**
     * Accessor for the type info for this datastore field
     * @return Type info for the datastore field
     */
    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.CLOB, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.CLOB);
    }

    public void setString(Object ps, int param, String value)
    {
        if (getDatabaseAdapter().supportsOption(RDBMSAdapter.CLOB_SET_USING_SETSTRING))
        {
            super.setString(ps, param ,value);
        }
        else
        {
            setObject(ps, param, value);
        }
    }
    
    public void setObject(Object ps, int param, Object value)
    {
        if (getDatabaseAdapter().supportsOption(RDBMSAdapter.CLOB_SET_USING_SETSTRING))
        {
            super.setObject(ps, param, value);
        }
        else
        {
            try
            {
                if (value == null)
                {
                    ((PreparedStatement) ps).setNull(param, getTypeInfo().getDataType());
                }
                else
                {
                    ((PreparedStatement) ps).setClob(param, new ClobImpl((String)value));
                }
            }
            catch (SQLException e)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object", "" + value, column, e.getMessage()), e);
            }
            catch (IOException e)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object", "" + value, column, e.getMessage()), e);
            }
        }
    }

    public String getString(Object rs, int param)
    {
        if (getDatabaseAdapter().supportsOption(RDBMSAdapter.CLOB_SET_USING_SETSTRING))
        {
            return super.getString(rs, param);
        }
        else
        {
            return (String) getObject(rs, param);
        }
    }
    
    public Object getObject(Object rs, int param)
    {
        if (getDatabaseAdapter().supportsOption(RDBMSAdapter.CLOB_SET_USING_SETSTRING))
        {
            return super.getObject(rs, param);
        }
        else
        {
            Object value;
            
            try
            {
                Clob clob = ((ResultSet) rs).getClob(param);
                if (!((ResultSet) rs).wasNull())
                {
                    BufferedReader br = new BufferedReader(clob.getCharacterStream());
                    try
                    {
                        int c;
                        StringBuffer sb = new StringBuffer();
                        while ((c = br.read()) != -1)
                        {
                            sb.append((char)c);
                        }
                        value = sb.toString(); 
                    }
                    finally
                    {
                        br.close();
                    }
                }
                else
                {
                    value = null;
                }
            }
            catch (SQLException e)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object", "" + param, column, e.getMessage()), e);
            }
            catch (IOException e)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object", "" + param, column, e.getMessage()), e);
            }
            
            return value;
        }
    }    
}