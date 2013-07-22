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
 ...
 **********************************************************************/
package org.datanucleus.store.rdbms.mapping.oracle;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.mapped.mapping.ArrayMapping;
import org.datanucleus.store.rdbms.datatype.BlobImpl;
import org.datanucleus.store.types.sco.SCOUtils;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Mapping for arrays for Oracle.
 */
public class OracleArrayMapping extends ArrayMapping
{
    /**
     * Method to be called after the insert of the owner class element.
     * @param ownerOP ObjectProvider of the owner
     */
    public void postInsert(ObjectProvider ownerOP)
    {
        if (containerIsStoredInSingleColumn())
        {
            Object value = ownerOP.provideField(mmd.getAbsoluteFieldNumber());
            if (value == null)
            {
                return;
            }
            ExecutionContext ec = ownerOP.getExecutionContext();
            SCOUtils.validateObjectsForWriting(ec, value);

            // Generate the contents for the BLOB
            byte[] bytes = new byte[0];
            try
            {
                if (mmd.isSerialized())
                {
                    // Serialised field so just perform basic Java serialisation for retrieval
                    if (!(value instanceof Serializable))
                    {
                        throw new NucleusDataStoreException(LOCALISER.msg("055005", value.getClass().getName()));
                    }
                    BlobImpl b = new BlobImpl(value);
                    bytes = b.getBytes(0, (int) b.length());
                }
                else if (value instanceof boolean[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromBooleanArray(value);
                }
                else if (value instanceof char[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromCharArray(value);
                }
                else if (value instanceof double[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromDoubleArray(value);
                }
                else if (value instanceof float[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromFloatArray(value);
                }
                else if (value instanceof int[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromIntArray(value);
                }
                else if (value instanceof long[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromLongArray(value);
                }
                else if (value instanceof short[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromShortArray(value);
                }
                else if (value instanceof Boolean[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromBooleanObjectArray(value);
                }
                else if (value instanceof Byte[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromByteObjectArray(value);
                }
                else if (value instanceof Character[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromCharObjectArray(value);
                }
                else if (value instanceof Double[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromDoubleObjectArray(value);
                }
                else if (value instanceof Float[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromFloatObjectArray(value);
                }
                else if (value instanceof Integer[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromIntObjectArray(value);
                }
                else if (value instanceof Long[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromLongObjectArray(value);
                }
                else if (value instanceof Short[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromShortObjectArray(value);
                }
                else if (value instanceof BigDecimal[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromBigDecimalArray(value);
                }
                else if (value instanceof BigInteger[])
                {
                    bytes = TypeConversionHelper.getByteArrayFromBigIntegerArray(value);
                }
                else if (value instanceof byte[])
                {
                    bytes = (byte[]) value;
                }
                else if (value instanceof java.util.BitSet)
                {
                    bytes = TypeConversionHelper.getByteArrayFromBooleanArray(TypeConversionHelper.getBooleanArrayFromBitSet((java.util.BitSet) value));
                }
                else
                {
                    // Fall back to just perform Java serialisation for storage
                    if (!(value instanceof Serializable))
                    {
                        throw new NucleusDataStoreException(LOCALISER.msg("055005", value.getClass().getName()));
                    }
                    BlobImpl b = new BlobImpl(value);
                    bytes = b.getBytes(0, (int) b.length());
                }
            }
            catch (SQLException e)
            {
                throw new NucleusDataStoreException(LOCALISER.msg("055001", "Object", "" + value, mmd, e.getMessage()), e);
            }
            catch (IOException e1)
            {
                // Do nothing
            }

            // Update the BLOB
            OracleBlobRDBMSMapping.updateBlobColumn(ownerOP, getDatastoreContainer(), getDatastoreMapping(0), bytes);
        }
        else
        {
            super.postInsert(ownerOP);
        }
    }

    /**
     * Method to be called after any update of the owner class element.
     * @param sm StateManager of the owner
     */
    public void postUpdate(ObjectProvider sm)
    {
        if (containerIsStoredInSingleColumn())
        {
            postInsert(sm);
        }
        else
        {
            super.postUpdate(sm);
        }
    }
}