/******************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved. 
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
2004 Erik Bengtson - addition of JDBC, SQL type maps
2004 Andy Jefferson - fixed use of SQL/JDBC to find the one applicable to
                      the specified field and type.
    ...
*****************************************************************/
package org.datanucleus.store.rdbms.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.ColumnMetaDataContainer;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.NullValue;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.DatastoreIdentifier;
import org.datanucleus.store.mapped.IdentifierFactory;
import org.datanucleus.store.mapped.IdentifierType;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.AbstractMappingManager;
import org.datanucleus.store.mapped.mapping.DatastoreMapping;
import org.datanucleus.store.mapped.mapping.DatastoreMappingFactory;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.JavaUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.MultiMap;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Mapping manager for RDBMS datastores.
 * Provides mappings from standard Java types defined in org.datanucleus.store.mapped.mapping to
 * datastore mappings for JDBC types.
 */
public class RDBMSMappingManager extends AbstractMappingManager
{
    protected static final Localiser LOCALISER_RDBMS = Localiser.getInstance("org.datanucleus.store.rdbms.Localisation",
        RDBMSStoreManager.class.getClassLoader());

    final ClassLoaderResolver clr;

    MultiMap datastoreMappingsByJavaType;
    MultiMap datastoreMappingsByJDBCType;
    MultiMap datastoreMappingsBySQLType;

    /**
     * Constructor for a mapping manager for a mapped ORM datastore.
     * @param storeMgr The StoreManager
     */
    public RDBMSMappingManager(MappedStoreManager storeMgr)
    {
        super(storeMgr);
        this.clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);
    }

    /**
     * Load all datastore mappings defined in the associated plugins.
     * We handle RDBMS datastore mappings so refer to rdbms-mapping-class, jdbc-type, sql-type in particular.
     * @param mgr the PluginManager
     * @param clr the ClassLoaderResolver
     * @param vendorId the datastore vendor id
     */
    public void loadDatastoreMapping(PluginManager mgr, ClassLoaderResolver clr, String vendorId)
    {
        if (datastoreMappingsByJavaType != null)
        {
            // Already loaded
            return;
        }

        datastoreMappingsByJDBCType = new MultiMap();
        datastoreMappingsBySQLType = new MultiMap();
        datastoreMappingsByJavaType = new MultiMap();
        ConfigurationElement[] elems =
            mgr.getConfigurationElementsForExtension("org.datanucleus.store.rdbms.rdbms_mapping", null, null);
        if (elems != null)
        {
            for (int i=0;i<elems.length;i++)
            {
                String javaName = elems[i].getAttribute("java-type").trim();
                String rdbmsMappingClassName = elems[i].getAttribute("rdbms-mapping-class");
                String jdbcType = elems[i].getAttribute("jdbc-type");
                String sqlType = elems[i].getAttribute("sql-type");
                String defaultJava = elems[i].getAttribute("default");
                String javaVersion = elems[i].getAttribute("java-version");
                String javaVersionRestricted = elems[i].getAttribute("java-version-restricted");

                boolean defaultForJavaType = false;
                if (defaultJava != null)
                {
                    if (defaultJava.equalsIgnoreCase("true"))
                    {
                        defaultForJavaType = Boolean.TRUE.booleanValue();
                    }
                }

                boolean javaRestricted = false;
                if (javaVersionRestricted != null)
                {
                    if (javaVersionRestricted.equalsIgnoreCase("true"))
                    {
                        javaRestricted = Boolean.TRUE.booleanValue();
                    }
                }

                if (javaVersion == null || javaVersion.length() < 1)
                {
                    // Default to use for all JDKs 1.3+
                    javaVersion = "1.3";
                }

                if ((JavaUtils.isGreaterEqualsThan(javaVersion) && !javaRestricted) ||
                        (JavaUtils.isEqualsThan(javaVersion) && javaRestricted))
                {
                    Class mappingType = null;
                    if (!StringUtils.isWhitespace(rdbmsMappingClassName))
                    {
                        try
                        {
                            mappingType = mgr.loadClass(elems[i].getExtension().getPlugin().getSymbolicName(), 
                                rdbmsMappingClassName);
                        }
                        catch (NucleusException jpe)
                        {
                            NucleusLogger.DATASTORE.error(LOCALISER.msg("041013", rdbmsMappingClassName));
                        }
                        Set includes = new HashSet();
                        Set excludes = new HashSet();
                        ConfigurationElement[] childElm = elems[i].getChildren();
                        for (int j=0; j<childElm.length; j++)
                        {
                            if (childElm[j].getName().equals("includes"))
                            {
                                includes.add(childElm[j].getAttribute("vendor-id"));
                            }
                            else if (childElm[j].getName().equals("excludes"))
                            {
                                excludes.add(childElm[j].getAttribute("vendor-id"));
                            }
                        }

                        if (!excludes.contains(vendorId))
                        {
                            if (includes.isEmpty() || includes.contains(vendorId))
                            {
                                registerDatastoreMapping(javaName, mappingType, jdbcType, sqlType, 
                                    defaultForJavaType);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility to register a datastore mapping for a java type, and the SQL/JDBC types it can be mapped to.
     * This can also be called to change the default setting of a mapping - just supply the same
     * values of java/JDBC/SQL types and a different default value
     * @param javaTypeName Name of the java type
     * @param datastoreMappingType The datastore mapping
     * @param jdbcType The JDBC type that can be used
     * @param sqlType The SQL type that can be used
     * @param dflt Whether this type should be used as the default mapping for this Java type
     */
    public void registerDatastoreMapping(String javaTypeName, Class datastoreMappingType, String jdbcType, 
            String sqlType, boolean dflt)
    {
        boolean mappingRequired = true;
        Collection coll = (Collection)datastoreMappingsByJavaType.get(javaTypeName);
        if (coll != null && coll.size() > 0)
        {
            // Check existing mappings to see if we already have a mapping for this jdbc/sql-type combo
            Iterator collIter = coll.iterator();
            while (collIter.hasNext())
            {
                RDBMSTypeMapping typeMapping = (RDBMSTypeMapping)collIter.next();
                if (typeMapping.jdbcType.equals(jdbcType) && typeMapping.sqlType.equals(sqlType))
                {
                    // Mapping for same java/jdbc/sql types
                    mappingRequired = false;
                    if (typeMapping.isDefault() != dflt)
                    {
                        typeMapping.setDefault(dflt);
                    }
                }
                else if (dflt)
                {
                    typeMapping.setDefault(false);
                }
            }
        }

        // Add the mapping if not already existing ("priority" attribute can be used to order the plugin entries)
        if (mappingRequired)
        {
            RDBMSTypeMapping mapping = new RDBMSTypeMapping(datastoreMappingType, dflt, javaTypeName, jdbcType,
                sqlType);
            datastoreMappingsByJDBCType.put(jdbcType,mapping);
            datastoreMappingsBySQLType.put(sqlType,mapping);
            datastoreMappingsByJavaType.put(javaTypeName,mapping);
            if (NucleusLogger.DATASTORE.isDebugEnabled())
            {
                NucleusLogger.DATASTORE.debug(LOCALISER_RDBMS.msg("054009", javaTypeName, 
                    jdbcType, sqlType, datastoreMappingType.getName(), "" + dflt));
            }
            
        }
    }

    /**
     * Utility to deregister all mappings for a JDBC type.
     * @param jdbcTypeName The JDBC type name
     */
    public void deregisterDatastoreMappingsForJDBCType(String jdbcTypeName)
    {
        // Find the mappings that we have for this JDBC type
        Collection coll = (Collection)datastoreMappingsByJDBCType.get(jdbcTypeName);
        if (coll == null || coll.size() == 0)
        {
            return;
        }

        // Take a copy so we have no ConcurrentModificationException issues
        Collection mappings = new HashSet(coll);

        // Delete the mapping from the 3 maps
        Iterator iter = mappings.iterator();
        while (iter.hasNext())
        {
            RDBMSTypeMapping mapping = (RDBMSTypeMapping)iter.next();
            datastoreMappingsByJavaType.remove(mapping.javaType, mapping);
            datastoreMappingsBySQLType.remove(mapping.sqlType, mapping);
            datastoreMappingsByJDBCType.remove(mapping.jdbcType, mapping);
            if (NucleusLogger.DATASTORE.isDebugEnabled())
            {
                NucleusLogger.DATASTORE.debug(LOCALISER_RDBMS.msg("054010", 
                    mapping.javaType, mapping.jdbcType, mapping.sqlType));
            }
        }
    }

    /**
     * Accessor for a datastore mapping class for the specified java type (and optional jdbc type or sql type).
     * @param fieldName Name of the field (if known)
     * @param javaType The java type
     * @param jdbcType The JDBC type
     * @param sqlType The SQL Type
     * @param clr ClassLoader resolver to use
     * @return The datastore mapping class
     */
    protected Class getDatastoreMappingClass(String fieldName, String javaType, String jdbcType, String sqlType, 
            ClassLoaderResolver clr)
    {
        RDBMSTypeMapping datastoreMapping = null;

        if (javaType == null)
        {
            return null;
        }
        // Make sure we don't have a primitive in here
       javaType = ClassUtils.getWrapperTypeNameForPrimitiveTypeName(javaType);

        if (sqlType != null)
        {
            // First look for "sql-type"
            if (datastoreMappingsBySQLType.get(sqlType.toUpperCase()) == null)
            {
                if (fieldName != null)
                {
                    throw new NucleusException(LOCALISER_RDBMS.msg("054001", 
                        javaType, sqlType, fieldName)).setFatal();
                }
                else
                {
                    throw new NucleusException(LOCALISER_RDBMS.msg("054000", 
                        javaType, sqlType)).setFatal();
                }
            }

            // Find if this sql-type has been defined for this java-type
            Iterator sqlTypeIter = ((Collection) datastoreMappingsBySQLType.get(sqlType.toUpperCase())).iterator();
            while (sqlTypeIter.hasNext())
            {
                RDBMSTypeMapping sqlTypeMapping = (RDBMSTypeMapping)sqlTypeIter.next();
                if (sqlTypeMapping.javaType.equals(javaType))
                {
                    datastoreMapping = sqlTypeMapping;
                    break;
                }
            }
        }
        else if (jdbcType != null)
        {
            // Then look for "jdbc-type"
            if (datastoreMappingsByJDBCType.get(jdbcType.toUpperCase()) == null)
            {
                if (fieldName != null)
                {
                    throw new NucleusException(LOCALISER_RDBMS.msg("054003", 
                        javaType, jdbcType, fieldName)).setFatal();
                }
                else
                {
                    throw new NucleusException(LOCALISER_RDBMS.msg("054002", 
                        javaType, jdbcType)).setFatal();
                }
            }

            // Find if this jdbc-type has been defined for this java-type
            Iterator jdbcTypeIter = ((Collection) datastoreMappingsByJDBCType.get(jdbcType.toUpperCase())).iterator();
            while (jdbcTypeIter.hasNext())
            {
                RDBMSTypeMapping jdbcTypeMapping = (RDBMSTypeMapping)jdbcTypeIter.next();
                if (jdbcTypeMapping.javaType.equals(javaType))
                {
                    datastoreMapping = jdbcTypeMapping;
                    break;
                }
            }
            if (datastoreMapping == null)
            {
                // This JDBC type is supported but not for persisting this java type
                if (fieldName != null)
                {
                    throw new NucleusException(LOCALISER_RDBMS.msg("054003", 
                        javaType, jdbcType, fieldName)).setFatal();
                }
                else
                {
                    throw new NucleusException(LOCALISER_RDBMS.msg("054002", 
                        javaType, jdbcType)).setFatal();
                }
            }
        }

        if (datastoreMapping == null)
        {
            // No specified type so get the best for this java-type (if primitve then use the wrapper java-type)
            // Use wrapper type instead of primitive type since primitives have no java-type entries for datastore mappings
            String type = ClassUtils.getWrapperTypeNameForPrimitiveTypeName(javaType);
            Collection mappings = (Collection)datastoreMappingsByJavaType.get(type);
            if (mappings == null)
            {
                // This java-type isnt specifically supported so maybe its superclass is
                Class javaTypeClass = clr.classForName(type);
                Class superClass = javaTypeClass.getSuperclass();
                while (superClass != null && !superClass.getName().equals(ClassNameConstants.Object) && mappings == null)
                {
                    mappings = (Collection) datastoreMappingsByJavaType.get(superClass.getName());
                    superClass = superClass.getSuperclass();
                }
            }
            if (mappings != null)
            {
                if (mappings.size() == 1)
                {
                    datastoreMapping = (RDBMSTypeMapping) mappings.iterator().next();
                }
                else
                {
                    // More than 1 so take the default
                    Iterator mappingsIter = mappings.iterator();
                    while (mappingsIter.hasNext())
                    {
                        RDBMSTypeMapping rdbmsMapping = (RDBMSTypeMapping)mappingsIter.next();
                        if (rdbmsMapping.isDefault())
                        {
                            // default matched so take it
                            datastoreMapping = rdbmsMapping;
                            break;
                        }
                    }

                    // No default set, so use the first one
                    if (datastoreMapping == null && mappings.size() > 0)
                    {
                        datastoreMapping = (RDBMSTypeMapping) mappings.iterator().next();
                    }
                }
            }
        }

        if (datastoreMapping == null)
        {
            if (fieldName != null)
            {
                throw new NucleusException(LOCALISER_RDBMS.msg("054005",
                    javaType, jdbcType, sqlType, fieldName)).setFatal();
            }
            else
            {
                throw new NucleusException(LOCALISER_RDBMS.msg("054004",
                    javaType, jdbcType, sqlType)).setFatal();
            }
        }
        return datastoreMapping.getMappingType();
    }

    protected class RDBMSTypeMapping extends TypeMapping
    {
        private String javaType;
        private String jdbcType;
        private String sqlType;

        public RDBMSTypeMapping(Class mappingType, boolean isDefault, String javaType, String jdbcType, 
                String sqlType)
        {
            super(mappingType, isDefault);
            this.javaType = javaType;
            this.jdbcType = jdbcType;
            this.sqlType = sqlType;
        }
    }

    /**
     * Method to create the datastore mapping for a java type mapping at a particular index.
     * @param mapping The java mapping
     * @param mmd MetaData for the field/property
     * @param index Index of the column
     * @param column The column
     * @return The datastore mapping
     */
    public DatastoreMapping createDatastoreMapping(JavaTypeMapping mapping, AbstractMemberMetaData mmd, int index, 
            DatastoreField column)
    {
        Class datastoreMappingClass = null;

        if (mmd.getColumnMetaData().length > 0)
        {
            // Use "datastore-mapping-class" extension if provided
            if (mmd.getColumnMetaData()[index].hasExtension("datastore-mapping-class"))
            {
                datastoreMappingClass = clr.classForName(
                    mmd.getColumnMetaData()[index].getValueForExtension("datastore-mapping-class"));
            }
        }
        if (datastoreMappingClass == null)
        {
            String javaType = mapping.getJavaTypeForDatastoreMapping(index);
            String jdbcType = null;
            String sqlType = null;
            if (mapping.getRoleForMember() == FieldRole.ROLE_ARRAY_ELEMENT ||
                mapping.getRoleForMember() == FieldRole.ROLE_COLLECTION_ELEMENT)
            {
                // Element of a collection/array
                ColumnMetaData[] colmds = (mmd.getElementMetaData() != null ? mmd.getElementMetaData().getColumnMetaData() : null);
                if (colmds != null && colmds.length > 0)
                {
                    jdbcType = colmds[index].getJdbcType();
                    sqlType = colmds[index].getSqlType();
                }
                if (mmd.getCollection() != null && mmd.getCollection().isSerializedElement())
                {
                    javaType = ClassNameConstants.JAVA_IO_SERIALIZABLE;
                }
                if (mmd.getArray() != null && mmd.getArray().isSerializedElement())
                {
                    javaType = ClassNameConstants.JAVA_IO_SERIALIZABLE;
                }
            }
            else if (mapping.getRoleForMember() == FieldRole.ROLE_MAP_KEY)
            {
                // Key of a map
                ColumnMetaData[] colmds = (mmd.getKeyMetaData() != null ? mmd.getKeyMetaData().getColumnMetaData() : null);
                if (colmds != null && colmds.length > 0)
                {
                    jdbcType = colmds[index].getJdbcType();
                    sqlType = colmds[index].getSqlType();
                }
                if (mmd.getMap().isSerializedKey())
                {
                    javaType = ClassNameConstants.JAVA_IO_SERIALIZABLE;
                }
            }
            else if (mapping.getRoleForMember() == FieldRole.ROLE_MAP_VALUE)
            {
                // Value of a map
                ColumnMetaData[] colmds = (mmd.getValueMetaData() != null ? mmd.getValueMetaData().getColumnMetaData() : null);
                if (colmds != null && colmds.length > 0)
                {
                    jdbcType = colmds[index].getJdbcType();
                    sqlType = colmds[index].getSqlType();
                }
                if (mmd.getMap().isSerializedValue())
                {
                    javaType = ClassNameConstants.JAVA_IO_SERIALIZABLE;
                }
            }
            else
            {
                // Normal field
                if (mmd.getColumnMetaData().length > 0)
                {
                    // Utilise the jdbc and sql types if specified
                    jdbcType = mmd.getColumnMetaData()[index].getJdbcType();
                    sqlType = mmd.getColumnMetaData()[index].getSqlType();
                }
                if (mmd.isSerialized())
                {
                    javaType = ClassNameConstants.JAVA_IO_SERIALIZABLE;
                }
            }
            datastoreMappingClass = getDatastoreMappingClass(mmd.getFullFieldName(), javaType, jdbcType, sqlType, clr);
        }

        DatastoreMapping datastoreMapping = DatastoreMappingFactory.createMapping(datastoreMappingClass, mapping, storeMgr, column);
        if (column != null)
        {
            column.setDatastoreMapping(datastoreMapping);
        }
        return datastoreMapping;
    }

    /**
     * Method to create the datastore mapping for a particular column and java type.
     * If the column is specified it is linked to the created datastore mapping.
     * @param mapping The java mapping
     * @param column The column (can be null)
     * @param javaType The java type
     * @return The datastore mapping
     */
    public DatastoreMapping createDatastoreMapping(JavaTypeMapping mapping, DatastoreField column, 
            String javaType)
    {
        Column col = (Column)column;
        String jdbcType = null;
        String sqlType = null;
        if (col != null && col.getColumnMetaData() != null)
        {
            // Utilise the jdbc and sql types if specified
            jdbcType = col.getColumnMetaData().getJdbcType();
            sqlType = col.getColumnMetaData().getSqlType();
        }
        Class datastoreMappingClass = getDatastoreMappingClass(null, javaType, jdbcType, sqlType, clr);

        DatastoreMapping datastoreMapping = DatastoreMappingFactory.createMapping(datastoreMappingClass, mapping, storeMgr, column);
        if (column != null)
        {
            column.setDatastoreMapping(datastoreMapping);
        }
        return datastoreMapping;
    }

    /**
     * Method to create a datastore field for a Java type mapping.
     * This is NOT used for PersistenceCapable mappings - see method below.
     * @param mapping Java type mapping for the field
     * @param javaType The type of field being stored in this column
     * @param datastoreFieldIndex Index of the datastore field to use
     * @return The datastore field
     */
    public DatastoreField createDatastoreField(JavaTypeMapping mapping, String javaType, int datastoreFieldIndex)
    {
        AbstractMemberMetaData fmd = mapping.getMemberMetaData();
        int roleForField = mapping.getRoleForMember();
        DatastoreContainerObject datastoreContainer = mapping.getDatastoreContainer();

        // Take the column MetaData from the component that this mappings role relates to
        ColumnMetaData colmd = null;
        ColumnMetaDataContainer columnContainer = fmd;
        if (roleForField == FieldRole.ROLE_COLLECTION_ELEMENT ||
            roleForField == FieldRole.ROLE_ARRAY_ELEMENT)
        {
            columnContainer = fmd.getElementMetaData();
        }
        else if (roleForField == FieldRole.ROLE_MAP_KEY)
        {
            columnContainer = fmd.getKeyMetaData();
        }
        else if (roleForField == FieldRole.ROLE_MAP_VALUE)
        {
            columnContainer= fmd.getValueMetaData();
        }

        Column col;
        ColumnMetaData[] colmds;
        if (columnContainer != null && columnContainer.getColumnMetaData().length > datastoreFieldIndex)
        {
            colmd = columnContainer.getColumnMetaData()[datastoreFieldIndex];
            colmds = columnContainer.getColumnMetaData();
        }
        else
        {
            // If column specified add one (use any column name specified on field element)
            colmd = new ColumnMetaData();
            colmd.setName(fmd.getColumn()); // TODO Avoid use of getColumn() - try getColumnMetaData() but test with spatial too
            if (columnContainer != null)
            {
                columnContainer.addColumn(colmd);
                colmds = columnContainer.getColumnMetaData();
            }
            else
            {
                colmds = new ColumnMetaData[1];
                colmds[0] = colmd;
            }
        }

        // Generate the column identifier
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        DatastoreIdentifier identifier = null;
        if (colmd.getName() == null)
        {
            // No name specified, so generate the identifier from the field name
            if (roleForField == FieldRole.ROLE_COLLECTION_ELEMENT)
            {
                // Join table collection element
                identifier = idFactory.newJoinTableFieldIdentifier(fmd, null, null, 
                    true, FieldRole.ROLE_COLLECTION_ELEMENT);
            }
            else if (roleForField == FieldRole.ROLE_ARRAY_ELEMENT)
            {
                // Join table array element
                identifier = idFactory.newJoinTableFieldIdentifier(fmd, null, null,
                    true, FieldRole.ROLE_ARRAY_ELEMENT);
            }
            else if (roleForField == FieldRole.ROLE_MAP_KEY)
            {
                // Join table map key
                identifier = idFactory.newJoinTableFieldIdentifier(fmd, null, null,
                    true, FieldRole.ROLE_MAP_KEY);
            }
            else if (roleForField == FieldRole.ROLE_MAP_VALUE)
            {
                // Join table map value
                identifier = idFactory.newJoinTableFieldIdentifier(fmd, null, null,
                    true, FieldRole.ROLE_MAP_VALUE);
            }
            else
            {
                identifier = idFactory.newIdentifier(IdentifierType.COLUMN, fmd.getName());
                int i=0;
                while (datastoreContainer.hasDatastoreField(identifier))
                {
                    identifier = idFactory.newIdentifier(IdentifierType.COLUMN, fmd.getName() + "_" + i);
                    i++;
                }
            }

            colmd.setName(identifier.getIdentifierName());
        }
        else
        {
            // User has specified a name, so try to keep this unmodified
            identifier = idFactory.newDatastoreFieldIdentifier(colmds[datastoreFieldIndex].getName(), 
                storeMgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(fmd.getType()), 
                FieldRole.ROLE_CUSTOM);
        }

        // Create the column
        col = (Column) datastoreContainer.addDatastoreField(javaType, identifier, mapping, colmd);

        if (fmd.isPrimaryKey())
        {
            col.setAsPrimaryKey();
        }

        if (!(fmd.getParent() instanceof AbstractClassMetaData))
        {
            // Embedded so cant be datastore-attributed
        }
        else
        {
            if (storeMgr.isStrategyDatastoreAttributed(fmd.getAbstractClassMetaData(), fmd.getAbsoluteFieldNumber()))
            {
                if (datastoreContainer instanceof DatastoreClass)
                {
                    if ((fmd.isPrimaryKey() && ((DatastoreClass)datastoreContainer).isBaseDatastoreClass()) || 
                            !fmd.isPrimaryKey())
                    {
                        // Increment any PK field if we are in base class, and increment any other field
                        col.setIdentity(true);
                    }
                }
            }
        }

        if (fmd.getValueForExtension("select-function") != null)
        {
            col.setWrapperFunction(fmd.getValueForExtension("select-function"),Column.WRAPPER_FUNCTION_SELECT);
        }
        if (fmd.getValueForExtension("insert-function") != null)
        {
            col.setWrapperFunction(fmd.getValueForExtension("insert-function"),Column.WRAPPER_FUNCTION_INSERT);
        }
        if (fmd.getValueForExtension("update-function") != null)
        {
            col.setWrapperFunction(fmd.getValueForExtension("update-function"),Column.WRAPPER_FUNCTION_UPDATE);
        }

        setDatastoreFieldNullability(fmd, colmd, col);
        if (fmd.getNullValue() == NullValue.DEFAULT)
        {
            // Users default should be applied if a null is to be inserted
            col.setDefaultable();
            if (colmd.getDefaultValue() != null)
            {
                col.setDefaultValue(colmd.getDefaultValue());
            }
        }

        return col;
    }

    /**
     * Method to create a datastore field for a Java type mapping.
     * This is used for serialised PC elements/keys/values in a join table.
     * TODO Merge this with the method above.
     * @param mapping Java type mapping for the field
     * @param javaType The type of field being stored in this column
     * @param colmd MetaData for the column
     * @return The datastore field
     */
    public DatastoreField createDatastoreField(JavaTypeMapping mapping, String javaType, ColumnMetaData colmd)
    {
        AbstractMemberMetaData fmd = mapping.getMemberMetaData();
        DatastoreContainerObject datastoreContainer = mapping.getDatastoreContainer();

        Column col;
        if (colmd == null)
        {
            // If column specified add one (use any column name specified on field element)
            colmd = new ColumnMetaData();
            colmd.setName(fmd.getColumn()); // TODO Avoid use of getColumn() - try getColumnMetaData() but test with spatial too
            fmd.addColumn(colmd);
        }

        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        if (colmd.getName() == null)
        {
            // No name specified, so generate the identifier from the field name
            DatastoreIdentifier identifier = idFactory.newIdentifier(IdentifierType.COLUMN, fmd.getName());
            int i=0;
            while (datastoreContainer.hasDatastoreField(identifier))
            {
                identifier = idFactory.newIdentifier(IdentifierType.COLUMN, fmd.getName() + "_" + i);
                i++;
            }

            colmd.setName(identifier.getIdentifierName());
            col = (Column) datastoreContainer.addDatastoreField(javaType, identifier, mapping, colmd);
        }
        else
        {
            // User has specified a name, so try to keep this unmodified
            col = (Column) datastoreContainer.addDatastoreField(javaType, 
                idFactory.newDatastoreFieldIdentifier(colmd.getName(), 
                    storeMgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(fmd.getType()), 
                    FieldRole.ROLE_CUSTOM), 
                mapping, colmd);
        }

        setDatastoreFieldNullability(fmd, colmd, col);
        if (fmd.getNullValue() == NullValue.DEFAULT)
        {
            // Users default should be applied if a null is to be inserted
            col.setDefaultable();
            if (colmd.getDefaultValue() != null)
            {
                col.setDefaultValue(colmd.getDefaultValue());
            }
        }

        return col;
    }

    /**
     * Method to create a datastore field for a PersistenceCapable mapping.
     * @param mmd MetaData for the field whose mapping it is
     * @param datastoreContainer Datastore class where we create the datastore field
     * @param mapping The Java type for this field
     * @param colmd The columnMetaData for this datastore field
     * @param reference The datastore field we are referencing
     * @param clr ClassLoader resolver
     * @return The DatastoreField
     */
    public DatastoreField createDatastoreField(AbstractMemberMetaData mmd,
                                               DatastoreContainerObject datastoreContainer,
                                               JavaTypeMapping mapping,
                                               ColumnMetaData colmd,
                                               DatastoreField reference,
                                               ClassLoaderResolver clr)
    {
        IdentifierFactory idFactory = storeMgr.getIdentifierFactory();
        DatastoreIdentifier identifier = null;
        if (colmd.getName() == null)
        {
            // No name specified, so generate the identifier from the field name
            AbstractMemberMetaData[] relatedMmds = mmd.getRelatedMemberMetaData(clr);
            identifier = idFactory.newForeignKeyFieldIdentifier(
                relatedMmds != null ? relatedMmds[0] : null, 
                mmd, reference.getIdentifier(), 
                storeMgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(mmd.getType()), 
                FieldRole.ROLE_OWNER);
            colmd.setName(identifier.getIdentifierName());
        }
        else
        {
            // User has specified a name, so try to keep this unmodified
            identifier = idFactory.newDatastoreFieldIdentifier(colmd.getName(), false, FieldRole.ROLE_CUSTOM);
        }
        Column col = (Column)datastoreContainer.addDatastoreField(mmd.getType().getName(), identifier, mapping, colmd);

        // Copy the characteristics of the reference column to this one
        reference.copyConfigurationTo(col);

        if (mmd.isPrimaryKey())
        {
            col.setAsPrimaryKey();
        }

        if (!(mmd.getParent() instanceof AbstractClassMetaData))
        {
            // Embedded so can't be datastore-attributed
        }
        else
        {
            if (storeMgr.isStrategyDatastoreAttributed(mmd.getAbstractClassMetaData(), mmd.getAbsoluteFieldNumber()))
            {
                if ((mmd.isPrimaryKey() && ((DatastoreClass)datastoreContainer).isBaseDatastoreClass()) || !mmd.isPrimaryKey())
                {
                    // Increment any PK field if we are in base class, and increment any other field
                    col.setIdentity(true);
                }
            }
        }

        if (mmd.getValueForExtension("select-function") != null)
        {
            col.setWrapperFunction(mmd.getValueForExtension("select-function"),Column.WRAPPER_FUNCTION_SELECT);
        }
        if (mmd.getValueForExtension("insert-function") != null)
        {
            col.setWrapperFunction(mmd.getValueForExtension("insert-function"),Column.WRAPPER_FUNCTION_INSERT);
        }
        if (mmd.getValueForExtension("update-function") != null)
        {
            col.setWrapperFunction(mmd.getValueForExtension("update-function"),Column.WRAPPER_FUNCTION_UPDATE);
        }

        setDatastoreFieldNullability(mmd, colmd, col);
        if (mmd.getNullValue() == NullValue.DEFAULT)
        {
            // Users default should be applied if a null is to be inserted
            col.setDefaultable();
            if (colmd.getDefaultValue() != null)
            {
                col.setDefaultValue(colmd.getDefaultValue());
            }
        }

        return col;        
    }

    /**
     * Sets the DatastoreField nullability based on metadata configuration.
     * <P>
     * Configuration is taken in this order:
     * <UL>
     * <LI>ColumnMetaData (allows-null)</LI>
     * <LI>AbstractMemberMetaData (null-value)</LI>
     * <LI>Field type (primitive does not allows null)</LI>
     * </UL>
     * @param mmd Metadata for the field/property
     * @param colmd the ColumnMetaData
     * @param field the DatastoreField
     */
    private void setDatastoreFieldNullability(AbstractMemberMetaData mmd, ColumnMetaData colmd, 
            DatastoreField field)
    {
        // Provide defaults for "allows-null" in ColumnMetaData
        if (colmd != null && colmd.getAllowsNull() == null)
        {
            if (mmd.isPrimaryKey())
            {
                colmd.setAllowsNull(Boolean.valueOf(false));
            }
            else if (!mmd.getType().isPrimitive() && mmd.getNullValue() != NullValue.EXCEPTION)
            {
                colmd.setAllowsNull(Boolean.valueOf(true));
            }
            else
            {
                colmd.setAllowsNull(Boolean.valueOf(false));
            }
            if (colmd.isAllowsNull())
            {
                field.setNullable();
            }
        }
        // Set the nullability of the column in the datastore
        else if (colmd != null && colmd.getAllowsNull() != null)
        {
            if (colmd.isAllowsNull())
            {
                field.setNullable();
            }
        }
        else if (mmd.isPrimaryKey())
        {
            // field is never nullable
        }
        else if (!mmd.getType().isPrimitive() && mmd.getNullValue() != NullValue.EXCEPTION)
        {
            field.setNullable();
        }
    }
}