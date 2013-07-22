/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.rdbms.mapping;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.datatype.BlobImpl;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Mapping of a large binary (e.g BLOB, LONGVARBINARY) RDBMS type.
 * A large binary column can be treated in two ways in terms of storage and retrieval.
 * <ul>
 * <li>Serialise the field into the large binary using ObjectOutputStream, and deserialise
 * it back using ObjectInputStream - i.e Java serialisation</li>
 * <li>Store the field using a byte[] stream, and retrieve it in the same way.</li>
 * </ul>
 */
public abstract class AbstractLargeBinaryRDBMSMapping extends RDBMSMapping
{
    /**
     * Constructor.
     * @param storeMgr Store Manager
     * @param mapping Java type mapping
     */
    protected AbstractLargeBinaryRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * Constructor.
     * @param mapping Java type mapping
     * @param storeMgr Store Manager
     * @param field Field to be mapped
     */
    public AbstractLargeBinaryRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(storeMgr, mapping);
		column = (Column) field;
		initialize();
	}

    /**
     * Initialisation method, initialising the type info.
     */
    protected void initialize()
    {
		initTypeInfo();
    }

    /**
     * Method to store a field into a "large varbinary" column.
     * @param ps The PreparedStatement
     * @param param Parameter position in the statement
     * @param value The value to store
     */
    public void setObject(Object ps, int param, Object value)
    {
        if (value == null)
        {
            try
            {
                if (column != null && column.isDefaultable() && column.getDefaultValue() != null)
                {
                    ((PreparedStatement)ps).setBytes(param, column.getDefaultValue().toString().trim().getBytes());
                }
                else
                {
                    ((PreparedStatement)ps).setNull(param, getTypeInfo().getDataType());
                }
            }
            catch (SQLException sqle)
            {
				throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object", "" + param, column, sqle.getMessage()),sqle);
            }
        }
        else
        {
            try
            {
                // Use Java serialisation, else byte-streaming, and if not determined then Java serialisation
                if (getJavaTypeMapping().isSerialised())
                {
                    // Serialised field so just perform basic Java serialisation for retrieval
                    if (!(value instanceof Serializable))
                    {
                        throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055005", value.getClass().getName()));
                    }
                    BlobImpl b = new BlobImpl(value);
                    ((PreparedStatement) ps).setBytes(param, b.getBytes(0, (int) b.length()));
                }
                else if (value instanceof boolean[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromBooleanArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof char[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromCharArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof double[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromDoubleArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof float[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromFloatArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof int[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromIntArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof long[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromLongArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof short[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromShortArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Boolean[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromBooleanObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Byte[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromByteObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Character[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromCharObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Double[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromDoubleObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Float[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromFloatObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Integer[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromIntObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Long[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromLongObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof Short[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromShortObjectArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof BigDecimal[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromBigDecimalArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof BigInteger[])
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromBigIntegerArray(value);
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof byte[])
                {
                    ((PreparedStatement) ps).setBytes(param, (byte[]) value);
                }
                else if (value instanceof java.util.BitSet)
                {
                    byte[] data = TypeConversionHelper.getByteArrayFromBooleanArray(TypeConversionHelper.getBooleanArrayFromBitSet((java.util.BitSet) value));
                    ((PreparedStatement) ps).setBytes(param, data);
                }
                else if (value instanceof java.awt.image.BufferedImage)
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                    ImageIO.write((BufferedImage) value, "jpg", baos);
                    byte[] buffer = baos.toByteArray();
                    baos.close();
                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                    ((PreparedStatement) ps).setBytes(param, buffer);
                    bais.close();
                }
                else
                {
                    // Fall back to just perform Java serialisation for storage
                    if (!(value instanceof Serializable))
                    {
                        throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055005", value.getClass().getName()));
                    }
                    BlobImpl b = new BlobImpl(value);
                    ((PreparedStatement) ps).setBytes(param, b.getBytes(0, (int) b.length()));
                }
            }
            catch (Exception e)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object","" + value, column, e.getMessage()),e);
            }
        }
    }

    protected Object getObjectForBytes(byte[] bytes, int param)
    {
        // Use Java serialisation, else byte-streaming, and if not determined then Java serialisation
        if (getJavaTypeMapping().isSerialised())
        {
            // Serialised field so just perform basic Java deserialisation for retrieval
            try
            {
                BlobImpl blob;
                blob = new BlobImpl(bytes);
                return blob.getObject();
            }
            catch (SQLException sqle)
            {
                // Impossible (JDK 1.6 use of free())
                return null;
            }
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.BOOLEAN_ARRAY))
        {
            return TypeConversionHelper.getBooleanArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.BYTE_ARRAY))
        {
            return bytes;
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.CHAR_ARRAY))
        {
            return TypeConversionHelper.getCharArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.DOUBLE_ARRAY))
        {
            return TypeConversionHelper.getDoubleArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.FLOAT_ARRAY))
        {
            return TypeConversionHelper.getFloatArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.INT_ARRAY))
        {
            return TypeConversionHelper.getIntArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.LONG_ARRAY))
        {
            return TypeConversionHelper.getLongArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.SHORT_ARRAY))
        {
            return TypeConversionHelper.getShortArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_BOOLEAN_ARRAY))
        {
            return TypeConversionHelper.getBooleanObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_BYTE_ARRAY))
        {
            return TypeConversionHelper.getByteObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_CHARACTER_ARRAY))
        {
            return TypeConversionHelper.getCharObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_DOUBLE_ARRAY))
        {
            return TypeConversionHelper.getDoubleObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_FLOAT_ARRAY))
        {
            return TypeConversionHelper.getFloatObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_INTEGER_ARRAY))
        {
            return TypeConversionHelper.getIntObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_LONG_ARRAY))
        {
            return TypeConversionHelper.getLongObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(ClassNameConstants.JAVA_LANG_SHORT_ARRAY))
        {
            return TypeConversionHelper.getShortObjectArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(BigDecimal[].class.getName()))
        {
            return TypeConversionHelper.getBigDecimalArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getType().equals(BigInteger[].class.getName()))
        {
            return TypeConversionHelper.getBigIntegerArrayFromByteArray(bytes);
        }
        else if (getJavaTypeMapping().getJavaType() != null &&
            getJavaTypeMapping().getJavaType().getName().equals(java.util.BitSet.class.getName()))
        {
            return TypeConversionHelper.getBitSetFromBooleanArray((boolean[])TypeConversionHelper.getBooleanArrayFromByteArray(bytes));
        }
        else if (getJavaTypeMapping().getJavaType() != null &&
                getJavaTypeMapping().getJavaType().getName().equals(java.awt.image.BufferedImage.class.getName()))
        {
            try
            {
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
            catch (IOException e)
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object", "" + param, column, e.getMessage()),e);
            }
        }
        else
        {
            // Fallback to just perform basic Java deserialisation for retrieval
            try
            {
                BlobImpl blob;
                blob = new BlobImpl(bytes);
                return blob.getObject();
            }
            catch (SQLException sqle)
            {
                // Impossible (JDK 1.6 use of free())
                return null;
            }
        }
    }

    /**
     * Method to retrieve the object from the large binary column.
     * @param rs The ResultSet
     * @param param The parameter position
     * @return The object
     */
    public Object getObject(Object rs, int param)
    {
        byte[] bytes = null;
        try
        {
            // Retrieve the bytes of the object
            bytes = ((ResultSet) rs).getBytes(param);
        }
        catch (SQLException sqle)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object", "" + param, column, sqle.getMessage()),sqle);
        }
        if (bytes == null)
        {
            return null;
        }

        return getObjectForBytes(bytes, param);
    }

    /**
     * Cater for serialisation of Strings.
     * @param preparedStatement PreparedStatement
     * @param exprIndex param indexes
     * @param value The value of the String
     */
    public void setString(Object preparedStatement, int exprIndex, String value)
    {
        // Delegate to the setObject method
        setObject(preparedStatement, exprIndex, value);
    }

    /**
     * Accessor for String value when serialised.
     * @param resultSet ResultSet
     * @param exprIndex param indexes
     * @return The String value
     */
    public String getString(Object resultSet, int exprIndex)
    {
        Object obj = getObject(resultSet, exprIndex);
        return (String)obj;
    }
}