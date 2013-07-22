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
2006 Andy Jefferson - migrated to support serialised and non-serialised BLOBs
2010 Andy Jefferson - add getBlob handler
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.io.IOException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.datatype.BlobImpl;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;

/**
 * Mapping of a BLOB RDBMS type.
 * A BLOB column can be treated in two ways in terms of storage and retrieval.
 * <ul>
 * <li>Serialise the field into the BLOB using ObjectOutputStream, and deserialise
 * it back using ObjectInputStream</li>
 * <li>Store the field using a byte[] stream, and retrieve it in the same way.</li>
 * </ul>
 */
public class BlobRDBMSMapping extends AbstractLargeBinaryRDBMSMapping
{
    /**
     * Constructor.
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected BlobRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public BlobRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(mapping, storeMgr, field);
	}

    /**
     * Accessor for the RDBMS BLOB type being represented.
     * @return TypeInfo for the BLOB type.
     */
    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.BLOB, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.BLOB);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.mapping.AbstractLargeBinaryRDBMSMapping#getObject(java.lang.Object, int)
     */
    @Override
    public Object getObject(Object rs, int param)
    {
        byte[] bytes = null;
        try
        {
            // Retrieve the bytes of the object directly
            bytes = ((ResultSet)rs).getBytes(param);
            if (bytes == null)
            {
                return null;
            }
        }
        catch (SQLException sqle)
        {
            try
            {
                // Retrieve the bytes using the Blob (if getBytes not supported e.g HSQLDB 2.0)
                Blob blob = ((ResultSet)rs).getBlob(param);
                if (blob == null)
                {
                    return null;
                }
                bytes = blob.getBytes(1, (int)blob.length());
                if (bytes == null)
                {
                    return null;
                }
            }
            catch (SQLException sqle2)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object", "" + param, column, sqle2.getMessage()), sqle2);
            }
        }

        return getObjectForBytes(bytes, param);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.mapping.AbstractLargeBinaryRDBMSMapping#setObject(java.lang.Object, int, java.lang.Object)
     */
    @Override
    public void setObject(Object ps, int param, Object value)
    {
        // TODO Auto-generated method stub
        super.setObject(ps, param, value);
    }

    public void setString(Object ps, int param, String value)
    {
        try
        {
            if (getDatabaseAdapter().supportsOption(RDBMSAdapter.BLOB_SET_USING_SETSTRING))
            {
                if (value == null)
                {
                    if (column.isDefaultable() && column.getDefaultValue() != null)
                    {
                        ((PreparedStatement) ps).setString(param,column.getDefaultValue().toString().trim());
                    }
                    else
                    {
                        ((PreparedStatement) ps).setNull(param,getTypeInfo().getDataType());
                    }
                }
                else
                {
                    ((PreparedStatement) ps).setString(param, value);
                }
            }
            else
            {
                if (value == null)
                {
                    if (column != null && column.isDefaultable() && column.getDefaultValue() != null)
                    {
                        ((PreparedStatement) ps).setBlob(param,new BlobImpl(column.getDefaultValue().toString().trim()));
                    }
                    else
                    {
                        ((PreparedStatement) ps).setNull(param,getTypeInfo().getDataType());
                    }
                }
                else
                {
                    ((PreparedStatement) ps).setBlob(param, new BlobImpl(value));
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "String", "" + value, column, e.getMessage()), e);
        }
        catch (IOException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "String", "" + value, column, e.getMessage()), e);
        }
    }

    public String getString(Object rs, int param)
    {
        String value;

        try
        {
            if (getDatabaseAdapter().supportsOption(RDBMSAdapter.BLOB_SET_USING_SETSTRING))
            {
                value = ((ResultSet) rs).getString(param);
            }
            else
            {
                byte[] bytes = ((ResultSet) rs).getBytes(param);
                if (bytes == null)
                {
                    value = null;
                }      
                else
                {
                    BlobImpl blob = new BlobImpl(bytes);
                    value = (String)blob.getObject();
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002", "String", "" + param, column, e.getMessage()), e);
        }

        return value;
    }
}