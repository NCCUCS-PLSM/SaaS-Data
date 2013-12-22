/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved. 
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
2003 Erik Bengtson - removed unused import
2003 Andy Jefferson - coding standards
2003 Andy Jefferson - addition of getGetStatement for inherited values
2004 Andy Jefferson - addition of query methods
2004 Andy Jefferson - moved statements from subclasses
2005 Andy Jefferson - allow for embedded keys/values
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.scostore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.mapping.EmbeddedKeyPCMapping;
import org.datanucleus.store.mapped.mapping.EmbeddedValuePCMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingHelper;
import org.datanucleus.store.rdbms.JDBCUtils;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.mapping.RDBMSMapping;
import org.datanucleus.store.rdbms.table.JoinTable;
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.types.sco.SCOUtils;

/**
 * Abstract representation of the backing store for a Map.
 */
public abstract class AbstractMapStore extends BaseContainerStore implements MapStore
{
    /** Flag to set whether the iterator statement will use a discriminator or not. */
    protected boolean iterateUsingDiscriminator = false;

    /** Table storing the map relation. May be join table, or key table, or value table. */
    protected DatastoreContainerObject mapTable;

    /** Table storing the values. */
    protected DatastoreClass valueTable;

    /** Metadata for the keys (if persistable). */
    protected AbstractClassMetaData kmd;

    /** Metadata for the values (if persistable). */
    protected AbstractClassMetaData vmd;

    /** Mapping to the key from the mapTable. */
    protected JavaTypeMapping keyMapping;

    /** Mapping to the value from the mapTable. */
    protected JavaTypeMapping valueMapping;

    /** Type of the key. */
    protected String keyType;

    /** Type of the value. */
    protected String valueType;

    /** Whether the keys are embedded. */
    protected boolean keysAreEmbedded;

    /** Whether the keys are serialised. */
    protected boolean keysAreSerialised;

    /** Whether the values are embedded. */
    protected boolean valuesAreEmbedded;

    /** Whether the values are serialised. */
    protected boolean valuesAreSerialised;

    private String containsValueStmt;

    /**
     * Constructor.
     * @param storeMgr Manager for the store
     */
    public AbstractMapStore(RDBMSStoreManager storeMgr)
    {
        super(storeMgr);
    }

    /**
     * Method to initialise the statements being used.
     * Subclasses should override the getXXXStmt() if they want to use an alternative statement.
     */
    protected void initialise()
    {
        containsValueStmt = getContainsValueStmt(getOwnerMapping(), getValueMapping(), getMapTable());
    }

    /**
     * Accessor for whether the keys are embedded or not.
     * If they are PC instances then returns false;
     * @return Whether the keys are embedded
     */
    public boolean keysAreEmbedded()
    {
        return keysAreEmbedded;
    }

    /**
     * Accessor for whether the keys are serialised or not.
     * If they are PC instances then returns false;
     * @return Whether the keys are serialised
     */
    public boolean keysAreSerialised()
    {
        return keysAreSerialised;
    }

    /**
     * Accessor for whether the values are embedded or not.
     * If they are PC instances then returns false;
     * @return Whether the values are embedded
     */
    public boolean valuesAreEmbedded()
    {
        return valuesAreEmbedded;
    }

    /**
     * Accessor for whether the values are serialised or not.
     * If they are PC instances then returns false;
     * @return Whether the values are serialised
     */
    public boolean valuesAreSerialised()
    {
        return valuesAreSerialised;
    }

    // ------------------------- Public Methods --------------------------------
 
    /**
     * Method to check if a key exists in the Map.
     * @param sm State Manager for the map
     * @param key The key to check for.
     * @return Whether the key exists in the Map.
     */
    public boolean containsKey(ObjectProvider sm, Object key)
    {
        if( key == null )
        {
            //nulls not allowed
            return false;
        }
        try
        {
            getValue(sm, key);
            return true;
        }
        catch (NoSuchElementException e)
        {
            return false;
        }
    }

    /**
     * Method to check if a value exists in the Map.
     * @param sm State Manager for the map
     * @param value The value to check for.
     * @return Whether the value exists in the Map.
     */
    public boolean containsValue(ObjectProvider sm, Object value)
    {
        if (value == null)
        {
            //nulls not allowed
            return false;
        }
        if (!validateValueForReading(sm, value))
        {
            return false;
        }

        boolean exists = false;
        try
        {
            ExecutionContext ec = sm.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();
            try
            {
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, containsValueStmt);
                try
                {
                    int jdbcPosition = 1;
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    BackingStoreHelper.populateValueInStatement(ec, ps, value, jdbcPosition, getValueMapping());

                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, containsValueStmt, ps);
                    try
                    {
                        if (rs.next())
                        {
                            exists = true;
                        }

                        JDBCUtils.logWarnings(rs);
                    }
                    finally
                    {
                        rs.close();
                    }
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("056019",containsValueStmt),e);
        }

        return exists;
    }

    /**
     * Method to return the value for a key.
     * @param sm State Manager for the Map.
     * @param key The key of the object to retrieve.
     * @return The value for this key.
     */
    public Object get(ObjectProvider sm, Object key)
    {
        try
        {
            return getValue(sm, key);
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    /**
     * Method to put all elements from a Map into our Map.
     * @param sm State Manager for the Map
     * @param m The Map to add
     */
    public void putAll(ObjectProvider sm, Map m)
    {
        Iterator i = m.entrySet().iterator();

        while (i.hasNext())
        {
            Map.Entry e = (Map.Entry)i.next();
            put(sm, e.getKey(), e.getValue());
        }
    }

    // --------------------------- Utility Methods -----------------------------
 
    /**
     * Utility to validate the type of a key for storing in the Map.
     * @param clr The ClassLoaderResolver
     * @param key The key to check.
     */
    protected void validateKeyType(ClassLoaderResolver clr, Object key)
    {
        if (key == null && !allowNulls)
        {
            // Nulls not allowed and key is null
            throw new NullPointerException(LOCALISER.msg("056062"));
        }

        if (key != null && !clr.isAssignableFrom(keyType, key.getClass()))
        {
            throw new ClassCastException(LOCALISER.msg("056064", key.getClass().getName(), keyType));
        }
    }

    /**
     * Utility to validate the type of a value for storing in the Map.
     * @param clr The ClassLoaderResolver
     * @param value The value to check.
     */
    protected void validateValueType(ClassLoaderResolver clr, Object value)
    {
        if (value == null && !allowNulls)
        {
            // Nulls not allowed and value is null
            throw new NullPointerException(LOCALISER.msg("056063"));
        }
        
        if (value != null && !clr.isAssignableFrom(valueType, value.getClass()))
        {
            throw new ClassCastException(LOCALISER.msg("056065", value.getClass().getName(), valueType));
        }
    }

    /**
     * Utility to validate a key is ok for reading.
     * @param sm State Manager for the map.
     * @param key The key to check.
     * @return Whether it is validated. 
     */
    protected boolean validateKeyForReading(ObjectProvider sm, Object key)
    {
        validateKeyType(sm.getExecutionContext().getClassLoaderResolver(), key);

        if (!keysAreEmbedded && !keysAreSerialised)
        {
            ExecutionContext ec = sm.getExecutionContext();
            if (key!=null && (!ec.getApiAdapter().isPersistent(key) ||
                ec != ec.getApiAdapter().getExecutionContext(key)) && !ec.getApiAdapter().isDetached(key))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Utility to validate a value is ok for reading.
     * @param sm State Manager for the map.
     * @param value The value to check.
     * @return Whether it is validated.
     */
    protected boolean validateValueForReading(ObjectProvider sm, Object value)
    {
        validateValueType(sm.getExecutionContext().getClassLoaderResolver(), value);

        if (!valuesAreEmbedded && !valuesAreSerialised)
        {
            ExecutionContext ec = sm.getExecutionContext();
            if (value != null && (!ec.getApiAdapter().isPersistent(value) ||
                ec != ec.getApiAdapter().getExecutionContext(value)) && !ec.getApiAdapter().isDetached(value))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Utility to validate a key is ok for writing (present in the datastore).
     * @param ownerOP ObjectProvider for the owner of the map
     * @param key The key to check.
     */
    protected void validateKeyForWriting(ObjectProvider ownerOP, Object key)
    {
        ExecutionContext ec = ownerOP.getExecutionContext();
        validateKeyType(ec.getClassLoaderResolver(), key);
        if (!keysAreEmbedded && !keysAreSerialised)
        {
            SCOUtils.validateObjectForWriting(ec, key, null);
        }
    }

    /**
     * Utility to validate a value is ok for writing (present in the datastore).
     * @param ownerOP ObjectProvider for the owner of the map
     * @param value The value to check.
     */
    protected void validateValueForWriting(ObjectProvider ownerOP, Object value)
    {
        ExecutionContext ec = ownerOP.getExecutionContext();
        validateValueType(ec.getClassLoaderResolver(), value);
        if (!valuesAreEmbedded && !valuesAreSerialised)
        {
            SCOUtils.validateObjectForWriting(ec, value, null);
        }
    }

    /**
     * Method to retrieve a value from the Map given the key.
     * @param sm State Manager for the map.
     * @param key The key to retrieve the value for.
     * @return The value for this key
     * @throws NoSuchElementException if the value for the key was not found
     */
    protected abstract Object getValue(ObjectProvider sm, Object key)
    throws NoSuchElementException;

    /**
     * Method to update a field of an embedded key.
     * @param sm State Manager of the owner
     * @param key The key to update
     * @param fieldNumber The number of the field to update
     * @param newValue The new value
     */
    public boolean updateEmbeddedKey(ObjectProvider sm, Object key, int fieldNumber, Object newValue)
    {
        boolean modified = false;
        if (keyMapping != null && keyMapping instanceof EmbeddedKeyPCMapping)
        {
            String fieldName = vmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getName();
            if (fieldName == null)
            {
                // We have no mapping for this field so presumably is the owner field or a PK field
                return false;
            }
            JavaTypeMapping fieldMapping = ((EmbeddedKeyPCMapping)keyMapping).getJavaTypeMapping(fieldName);
            if (fieldMapping == null)
            {
                // We have no mapping for this field so presumably is the owner field or a PK field
                return false;
            }
            modified = updatedEmbeddedKey(sm, key, fieldNumber, newValue, fieldMapping);
        }

        return modified;
    }

    /**
     * Method to update a field of an embedded key.
     * @param sm State Manager of the owner
     * @param value The value to update
     * @param fieldNumber The number of the field to update
     * @param newValue The new value
     */
    public boolean updateEmbeddedValue(ObjectProvider sm, Object value, int fieldNumber, Object newValue)
    {
        boolean modified = false;
        if (valueMapping != null && valueMapping instanceof EmbeddedValuePCMapping)
        {
            String fieldName = vmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getName();
            if (fieldName == null)
            {
                // We have no mapping for this field so presumably is the owner field or a PK field
                return false;
            }
            JavaTypeMapping fieldMapping = ((EmbeddedValuePCMapping)valueMapping).getJavaTypeMapping(fieldName);
            if (fieldMapping == null)
            {
                // We have no mapping for this field so presumably is the owner field or a PK field
                return false;
            }
            modified = updateEmbeddedValue(sm, value, fieldNumber, newValue, fieldMapping);
        }

        return modified;
    }

    public JavaTypeMapping getValueMapping()
    {
        return valueMapping;
    }

    public JavaTypeMapping getKeyMapping()
    {
        return keyMapping;
    }

    public boolean isValuesAreEmbedded()
    {
        return valuesAreEmbedded;
    }

    public boolean isValuesAreSerialised()
    {
        return valuesAreSerialised;
    }

    public DatastoreContainerObject getMapTable()
    {
        return mapTable;
    }

    public AbstractClassMetaData getKmd()
    {
        return kmd;
    }

    public AbstractClassMetaData getVmd()
    {
        return vmd;
    }

    /**
     * Generate statement to check if a value is contained in the Map.
     * <PRE>
     * SELECT OWNERCOL
     * FROM MAPTABLE
     * WHERE OWNERCOL=? AND VALUECOL = ?
     * </PRE>
     * @param ownerMapping the owner mapping
     * @param valueMapping the value mapping
     * @param mapTable the map table
     * @return Statement to check if a value is contained in the Map.
     */
    private String getContainsValueStmt(JavaTypeMapping ownerMapping, JavaTypeMapping valueMapping, 
            DatastoreContainerObject mapTable)
    {
        StringBuffer stmt = new StringBuffer("SELECT ");
        for (int i=0; i<ownerMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(ownerMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
        }
        stmt.append(" FROM ");
        stmt.append(mapTable.toString());
        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);
        BackingStoreHelper.appendWhereClauseForMapping(stmt, valueMapping, null, false);

        return stmt.toString();
    }

    public boolean updateEmbeddedValue(ObjectProvider sm, Object value, int fieldNumber, Object newValue,
            JavaTypeMapping fieldMapping)
    {
        boolean modified;
        String stmt = getUpdateEmbeddedValueStmt(fieldMapping, getOwnerMapping(), getValueMapping(), getMapTable());
        try
        {
            ExecutionContext ec = sm.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();

            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, stmt, false);
                try
                {
                    int jdbcPosition = 1;
                    fieldMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, fieldMapping), newValue);
                    jdbcPosition += fieldMapping.getNumberOfDatastoreMappings();
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    jdbcPosition = BackingStoreHelper.populateEmbeddedValueFieldsInStatement(
                        sm, value, ps, jdbcPosition, (JoinTable)getMapTable(), this);
                    sqlControl.executeStatementUpdate(ec, mconn, stmt, ps, true);
                    modified = true;
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new NucleusDataStoreException(LOCALISER.msg("056011", stmt), e);
        }
        return modified;
    }

    /**
     * Generate statement for update the field of an embedded key.
     * <PRE>
     * UPDATE MAPTABLE
     * SET EMBEDDEDKEYCOL1 = ?
     * WHERE OWNERCOL=?
     * AND EMBEDDEDKEYCOL1 = ?
     * AND EMBEDDEDKEYCOL2 = ? ...
     * </PRE>
     * @param fieldMapping The mapping for the field to be updated
     * @param ownerMapping The owner mapping
     * @param keyMapping The key mapping
     * @param mapTable The map table
     * @return Statement for updating an embedded key in the Set
     */
    protected String getUpdateEmbeddedKeyStmt(JavaTypeMapping fieldMapping, JavaTypeMapping ownerMapping,
            JavaTypeMapping keyMapping, DatastoreContainerObject mapTable)
    {
        StringBuffer stmt = new StringBuffer("UPDATE ");
        stmt.append(mapTable.toString());
        stmt.append(" SET ");
        for (int i=0; i<fieldMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(fieldMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
            stmt.append(" = ");
            stmt.append(((RDBMSMapping)fieldMapping.getDatastoreMapping(i)).getUpdateInputParameter());
        }

        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);

        EmbeddedKeyPCMapping embeddedMapping = (EmbeddedKeyPCMapping)keyMapping;
        for (int i=0;i<embeddedMapping.getNumberOfJavaTypeMappings();i++)
        {
            JavaTypeMapping m = embeddedMapping.getJavaTypeMapping(i);
            if (m != null)
            {
                for (int j=0;j<m.getNumberOfDatastoreMappings();j++)
                {
                    stmt.append(" AND ");
                    stmt.append(m.getDatastoreMapping(j).getDatastoreField().getIdentifier().toString());
                    stmt.append(" = ");
                    stmt.append(((RDBMSMapping)m.getDatastoreMapping(j)).getUpdateInputParameter());
                }
            }
        }
        return stmt.toString();
    }

    /**
     * Generate statement for update the field of an embedded value.
     * <PRE>
     * UPDATE MAPTABLE
     * SET EMBEDDEDVALUECOL1 = ?
     * WHERE OWNERCOL=?
     * AND EMBEDDEDVALUECOL1 = ?
     * AND EMBEDDEDVALUECOL2 = ? ...
     * </PRE>
     * @param fieldMapping The mapping for the field to be updated
     * @param ownerMapping The owner mapping
     * @param mapTable The map table
     * @return Statement for updating an embedded value in the Set
     */
    protected String getUpdateEmbeddedValueStmt(JavaTypeMapping fieldMapping, JavaTypeMapping ownerMapping,
           JavaTypeMapping valueMapping, DatastoreContainerObject mapTable)
    {
        StringBuffer stmt = new StringBuffer("UPDATE ");
        stmt.append(mapTable.toString());
        stmt.append(" SET ");
        for (int i=0; i<fieldMapping.getNumberOfDatastoreMappings(); i++)
        {
            if (i > 0)
            {
                stmt.append(",");
            }
            stmt.append(fieldMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
            stmt.append(" = ");
            stmt.append(((RDBMSMapping)fieldMapping.getDatastoreMapping(i)).getUpdateInputParameter());
        }

        stmt.append(" WHERE ");
        BackingStoreHelper.appendWhereClauseForMapping(stmt, ownerMapping, null, true);

        EmbeddedValuePCMapping embeddedMapping = (EmbeddedValuePCMapping)valueMapping;
        for (int i=0;i<embeddedMapping.getNumberOfJavaTypeMappings();i++)
        {
            JavaTypeMapping m = embeddedMapping.getJavaTypeMapping(i);
            if (m != null)
            {
                for (int j=0;j<m.getNumberOfDatastoreMappings();j++)
                {
                    stmt.append(" AND ");
                    stmt.append(m.getDatastoreMapping(j).getDatastoreField().getIdentifier().toString());
                    stmt.append(" = ");
                    stmt.append(((RDBMSMapping)m.getDatastoreMapping(j)).getUpdateInputParameter());
                }
            }
        }
        return stmt.toString();
    }

    public boolean updatedEmbeddedKey(ObjectProvider sm, Object key, int fieldNumber, Object newValue, 
            JavaTypeMapping fieldMapping)
    {
        boolean modified;
        String stmt = getUpdateEmbeddedKeyStmt(fieldMapping, getOwnerMapping(), getKeyMapping(), getMapTable());
        try
        {
            ExecutionContext ec = sm.getExecutionContext();
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();

            try
            {
                PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, stmt, false);
                try
                {
                    int jdbcPosition = 1;
                    fieldMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, fieldMapping), key);
                    jdbcPosition += fieldMapping.getNumberOfDatastoreMappings();
                    jdbcPosition = BackingStoreHelper.populateOwnerInStatement(sm, ec, ps, jdbcPosition, this);
                    jdbcPosition = BackingStoreHelper.populateEmbeddedKeyFieldsInStatement(sm, key, ps, jdbcPosition,
                        (JoinTable)getMapTable(), this);

                    sqlControl.executeStatementUpdate(ec, mconn, stmt, ps, true);
                    modified = true;
                }
                finally
                {
                    sqlControl.closeStatement(mconn, ps);
                }
            }
            finally
            {
                mconn.release();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            throw new NucleusDataStoreException(LOCALISER.msg("056010", stmt), e);
        }
        return modified;
    }
}