/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.scostore;

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.mapping.EmbeddedElementPCMapping;
import org.datanucleus.store.mapped.mapping.EmbeddedKeyPCMapping;
import org.datanucleus.store.mapped.mapping.EmbeddedValuePCMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingHelper;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.rdbms.mapping.RDBMSMapping;
import org.datanucleus.store.rdbms.table.JoinTable;

/**
 * Series of helper methods for use with RDBMS backing stores.
 */
public class BackingStoreHelper
{
    /**
     * Convenience method to populate the passed PreparedStatement with the value from the owner.
     * @param sm State Manager
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param jdbcPosition Position in JDBC statement to populate
     * @param bcs Base container backing store
     * @return The next position in the JDBC statement
     */
    public static int populateOwnerInStatement(ObjectProvider sm, ExecutionContext ec, Object ps, 
            int jdbcPosition, BaseContainerStore bcs)
    {
        if (!bcs.getStoreManager().insertValuesOnInsert(bcs.getOwnerMapping().getDatastoreMapping(0)))
        {
            // Don't try to insert any mappings with insert parameter that isnt ? (e.g Oracle)
            return jdbcPosition;
        }

        if (bcs.getOwnerMemberMetaData() != null)
        {
            bcs.getOwnerMapping().setObject(ec, ps,
                MappingHelper.getMappingIndices(jdbcPosition, bcs.getOwnerMapping()),
                sm.getObject(), sm, bcs.getOwnerMemberMetaData().getAbsoluteFieldNumber());
        }
        else
        {
            bcs.getOwnerMapping().setObject(ec, ps,
                MappingHelper.getMappingIndices(jdbcPosition, bcs.getOwnerMapping()),
                sm.getObject());
        }
        return jdbcPosition + bcs.getOwnerMapping().getNumberOfDatastoreMappings();
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value for the distinguisher
     * value.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param jdbcPosition Position in JDBC statement to populate
     * @return The next position in the JDBC statement
     */
    public static int populateRelationDiscriminatorInStatement(ExecutionContext ec, PreparedStatement ps, 
            int jdbcPosition, ElementContainerStore ecs)
    {
        ecs.getRelationDiscriminatorMapping().setObject(ec, ps, 
            MappingHelper.getMappingIndices(jdbcPosition, ecs.getRelationDiscriminatorMapping()),
            ecs.getRelationDiscriminatorValue());
        return jdbcPosition + ecs.getRelationDiscriminatorMapping().getNumberOfDatastoreMappings();
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value for the order index.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param idx The order value
     * @param jdbcPosition Position in JDBC statement to populate
     * @param orderMapping The order mapping
     * @return The next position in the JDBC statement
     */
    public static int populateOrderInStatement(ExecutionContext ec, Object ps, int idx, int jdbcPosition,
            JavaTypeMapping orderMapping)
    {
        orderMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, orderMapping), Integer.valueOf(idx));
        return jdbcPosition + orderMapping.getNumberOfDatastoreMappings();
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value for the element.
     * Not used with embedded PC elements.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param element The element
     * @param jdbcPosition Position in JDBC statement to populate
     * @return The next position in the JDBC statement
     */
    public static int populateElementInStatement(ExecutionContext ec, Object ps, Object element, 
            int jdbcPosition, JavaTypeMapping elementMapping)
    {
        if (!elementMapping.getStoreManager().insertValuesOnInsert(elementMapping.getDatastoreMapping(0)))
        {
            // Don't try to insert any mappings with insert parameter that isn't ? (e.g Oracle)
            return jdbcPosition;
        }
        elementMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, elementMapping), element);
        return jdbcPosition + elementMapping.getNumberOfDatastoreMappings();
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value for the element in a WHERE clause.
     * Like the above method except handles reference mappings where you want have some implementations as "IS NULL"
     * in the SQL, and just want to set the actual implementation FK for the element.
     * Not used with embedded PC elements.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param element The element
     * @param jdbcPosition Position in JDBC statement to populate
     * @return The next position in the JDBC statement
     */
    public static int populateElementForWhereClauseInStatement(ExecutionContext ec, Object ps, Object element,
            int jdbcPosition, JavaTypeMapping elementMapping)
    {
        if (elementMapping.getStoreManager().insertValuesOnInsert(elementMapping.getDatastoreMapping(0)))
        {
            if (elementMapping instanceof ReferenceMapping && elementMapping.getNumberOfDatastoreMappings() > 1)
            {
                ReferenceMapping elemRefMapping = (ReferenceMapping)elementMapping;
                JavaTypeMapping[] elemFkMappings = elemRefMapping.getJavaTypeMapping();
                int[] positions = null;
                for (int i=0;i<elemFkMappings.length;i++)
                {
                    if (elemFkMappings[i].getType().equals(element.getClass().getName()))
                    {
                        // The FK for the element in question, so populate this
                        positions = new int[elemFkMappings[i].getNumberOfDatastoreMappings()];
                        for (int j=0;j<positions.length;j++)
                        {
                            positions[j] = jdbcPosition++;
                        }
                    }
                }
                elementMapping.setObject(ec, ps, positions, element);
                jdbcPosition = jdbcPosition + positions.length;
            }
            else
            {
                elementMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, elementMapping), element);
                jdbcPosition = jdbcPosition + elementMapping.getNumberOfDatastoreMappings();
            }
        }
        return jdbcPosition;
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value for the map key.
     * Not used with embedded PC keys.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param key The key
     * @param jdbcPosition Position in JDBC statement to populate
     * @param keyMapping The key mapping
     * @return The next position in the JDBC statement
     */
    public static int populateKeyInStatement(ExecutionContext ec, PreparedStatement ps, Object key,
            int jdbcPosition, JavaTypeMapping keyMapping)
    {
        if (!((RDBMSMapping)keyMapping.getDatastoreMapping(0)).insertValuesOnInsert())
        {
            // Dont try to insert any mappings with insert parameter that isnt ? (e.g Oracle)
            return jdbcPosition;
        }
        keyMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, keyMapping), key);
        return jdbcPosition + keyMapping.getNumberOfDatastoreMappings();
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value for the map value.
     * Not used with embedded PC values.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param value The value
     * @param jdbcPosition Position in JDBC statement to populate
     * @param valueMapping The value mapping
     * @return The next position in the JDBC statement
     */
    public static int populateValueInStatement(ExecutionContext ec, PreparedStatement ps, Object value,
            int jdbcPosition, JavaTypeMapping valueMapping)
    {
        if (!((RDBMSMapping)valueMapping.getDatastoreMapping(0)).insertValuesOnInsert())
        {
            // Don't try to insert any mappings with insert parameter that isn't ? (e.g Oracle)
            return jdbcPosition;
        }
        valueMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition, valueMapping), value);
        return jdbcPosition + valueMapping.getNumberOfDatastoreMappings();
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the value from the element
     * discriminator, optionally including all subclasses of the element type.
     * @param ec execution context
     * @param ps The PreparedStatement
     * @param jdbcPosition Position in JDBC statement to populate
     * @param includeSubclasses Whether to include subclasses
     * @param info The element information
     * @param clr ClassLoader resolver
     * @return The next position in the JDBC statement
     */
    public static int populateElementDiscriminatorInStatement(ExecutionContext ec, PreparedStatement ps,
            int jdbcPosition, boolean includeSubclasses, ElementContainerStore.ElementInfo info,
            ClassLoaderResolver clr)
    {
        DiscriminatorStrategy strategy = info.getDiscriminatorStrategy();
        JavaTypeMapping discrimMapping = info.getDiscriminatorMapping();

        // Include element type
        if (strategy == DiscriminatorStrategy.CLASS_NAME)
        {
            discrimMapping.setObject(ec, ps, 
                MappingHelper.getMappingIndices(jdbcPosition, discrimMapping), info.getClassName());
            jdbcPosition += discrimMapping.getNumberOfDatastoreMappings();
        }
        else
        {
            if (strategy == DiscriminatorStrategy.VALUE_MAP)
            {
                discrimMapping.setObject(ec, ps, 
                    MappingHelper.getMappingIndices(jdbcPosition, discrimMapping),
                    info.getAbstractClassMetaData().getInheritanceMetaData().getDiscriminatorMetaData().getValue());
                jdbcPosition += discrimMapping.getNumberOfDatastoreMappings();
            }
        }

        // Include all subclasses
        if (includeSubclasses)
        {
            MappedStoreManager storeMgr = discrimMapping.getStoreManager();
            HashSet<String> subclasses = storeMgr.getSubClassesForClass(info.getClassName(), true, clr);
            if (subclasses != null && subclasses.size() > 0)
            {
                Iterator<String> iter = subclasses.iterator();
                while (iter.hasNext())
                {
                    String subclass = iter.next();
                    if (strategy == DiscriminatorStrategy.CLASS_NAME)
                    {
                        discrimMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition,
                            discrimMapping), subclass);
                        jdbcPosition += discrimMapping.getNumberOfDatastoreMappings();
                    }
                    else
                    {
                        if (strategy == DiscriminatorStrategy.VALUE_MAP)
                        {
                            AbstractClassMetaData subclassCmd = storeMgr.getNucleusContext().getMetaDataManager()
                                .getMetaDataForClass(subclass, clr);
                            discrimMapping.setObject(ec, ps, MappingHelper.getMappingIndices(jdbcPosition,
                                discrimMapping),
                                subclassCmd.getInheritanceMetaData().getDiscriminatorMetaData().getValue());
                            jdbcPosition += discrimMapping.getNumberOfDatastoreMappings();
                        }
                    }
                }
            }
        }
        return jdbcPosition;
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the field values from
     * the embedded element starting at the specified jdbc position.
     * @param sm State Manager of the owning container
     * @param element The embedded element
     * @param ps The PreparedStatement
     * @param jdbcPosition JDBC position in the statement to start at
     * @param ownerFieldMetaData The meta data for the owner field
     * @param bcs Container store
     * @return The next JDBC position
     */
    public static int populateEmbeddedElementFieldsInStatement(ObjectProvider sm, Object element, Object ps,
            int jdbcPosition, AbstractMemberMetaData ownerFieldMetaData, JavaTypeMapping elementMapping,
            AbstractClassMetaData emd, BaseContainerStore bcs)
    {
        EmbeddedElementPCMapping embeddedMapping = (EmbeddedElementPCMapping) elementMapping;
        StatementClassMapping mappingDefinition = new StatementClassMapping();
        int[] elementFieldNumbers = new int[embeddedMapping.getNumberOfJavaTypeMappings()];
        for (int i = 0; i < embeddedMapping.getNumberOfJavaTypeMappings(); i++)
        {
            JavaTypeMapping fieldMapping = embeddedMapping.getJavaTypeMapping(i);
            int absFieldNum = emd.getAbsolutePositionOfMember(fieldMapping.getMemberMetaData().getName());
            elementFieldNumbers[i] = absFieldNum;
            StatementMappingIndex stmtMapping = new StatementMappingIndex(fieldMapping);
            int[] jdbcParamPositions = new int[fieldMapping.getNumberOfDatastoreMappings()];
            for (int j = 0; j < fieldMapping.getNumberOfDatastoreMappings(); j++)
            {
                jdbcParamPositions[j] = jdbcPosition++;
            }
            stmtMapping.addParameterOccurrence(jdbcParamPositions);
            mappingDefinition.addMappingForMember(absFieldNum, stmtMapping);
        }

        ObjectProvider elementSM = bcs.getStateManagerForEmbeddedPCObject(sm, element, ownerFieldMetaData,
            ObjectProvider.EMBEDDED_COLLECTION_ELEMENT_PC);
        elementSM.provideFields(elementFieldNumbers, 
            elementMapping.getStoreManager().getFieldManagerForStatementGeneration(elementSM, ps, mappingDefinition, true));

        return jdbcPosition;
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the field values from
     * the embedded map key starting at the specified jdbc position.
     * @param sm State Manager of the owning container
     * @param key The embedded key
     * @param ps The PreparedStatement
     * @param jdbcPosition JDBC position in the statement to start at
     * @param joinTable The Join table where the values are embedded
     * @param mapStore the map store
     * @return The next JDBC position
     */
    public static int populateEmbeddedKeyFieldsInStatement(ObjectProvider sm, Object key,
            PreparedStatement ps, int jdbcPosition, JoinTable joinTable, AbstractMapStore mapStore)
    {
        AbstractClassMetaData kmd = mapStore.getKmd();
        EmbeddedKeyPCMapping embeddedMapping = (EmbeddedKeyPCMapping)mapStore.getKeyMapping();
        StatementClassMapping mappingDefinition = new StatementClassMapping();  
        int[] elementFieldNumbers = new int[embeddedMapping.getNumberOfJavaTypeMappings()];
        for (int i=0;i<embeddedMapping.getNumberOfJavaTypeMappings();i++)
        {
            JavaTypeMapping fieldMapping = embeddedMapping.getJavaTypeMapping(i);
            int absFieldNum = kmd.getAbsolutePositionOfMember(fieldMapping.getMemberMetaData().getName());
            elementFieldNumbers[i] = absFieldNum;
            StatementMappingIndex stmtMapping = new StatementMappingIndex(fieldMapping);
            int[] jdbcParamPositions = new int[fieldMapping.getNumberOfDatastoreMappings()];
            for (int j=0;j<fieldMapping.getNumberOfDatastoreMappings();j++)
            {
                jdbcParamPositions[j] = jdbcPosition++;
            }
            stmtMapping.addParameterOccurrence(jdbcParamPositions);
            mappingDefinition.addMappingForMember(absFieldNum, stmtMapping);
        }

        ObjectProvider elementSM = mapStore.getStateManagerForEmbeddedPCObject(sm, key, 
            joinTable.getOwnerMemberMetaData(), ObjectProvider.EMBEDDED_MAP_KEY_PC);
        elementSM.provideFields(elementFieldNumbers,
            embeddedMapping.getStoreManager().getFieldManagerForStatementGeneration(elementSM, ps, mappingDefinition, true));

        return jdbcPosition;
    }

    /**
     * Convenience method to populate the passed PreparedStatement with the field values from
     * the embedded map value starting at the specified jdbc position.
     * @param sm State Manager of the owning container
     * @param value The embedded value
     * @param ps The PreparedStatement
     * @param jdbcPosition JDBC position in the statement to start at
     * @param joinTable The Join table where the values are embedded
     * @param mapStore The map store
     * @return The next JDBC position
     */
    public static int populateEmbeddedValueFieldsInStatement(ObjectProvider sm, Object value,
            PreparedStatement ps, int jdbcPosition, JoinTable joinTable, AbstractMapStore mapStore)
    {
        AbstractClassMetaData vmd = mapStore.getVmd();
        EmbeddedValuePCMapping embeddedMapping = (EmbeddedValuePCMapping)mapStore.getValueMapping();
        StatementClassMapping mappingDefinition = new StatementClassMapping();
        int[] elementFieldNumbers = new int[embeddedMapping.getNumberOfJavaTypeMappings()];
        for (int i=0;i<embeddedMapping.getNumberOfJavaTypeMappings();i++)
        {
            JavaTypeMapping fieldMapping = embeddedMapping.getJavaTypeMapping(i);
            int absFieldNum = vmd.getAbsolutePositionOfMember(fieldMapping.getMemberMetaData().getName());
            elementFieldNumbers[i] = absFieldNum;
            StatementMappingIndex stmtMapping = new StatementMappingIndex(fieldMapping);
            int[] jdbcParamPositions = new int[fieldMapping.getNumberOfDatastoreMappings()];
            for (int j=0;j<fieldMapping.getNumberOfDatastoreMappings();j++)
            {
                jdbcParamPositions[j] = jdbcPosition++;
            }
            stmtMapping.addParameterOccurrence(jdbcParamPositions);
            mappingDefinition.addMappingForMember(absFieldNum, stmtMapping);
        }

        ObjectProvider elementSM = mapStore.getStateManagerForEmbeddedPCObject(sm, value, 
            joinTable.getOwnerMemberMetaData(), ObjectProvider.EMBEDDED_MAP_VALUE_PC);
        elementSM.provideFields(elementFieldNumbers,
            embeddedMapping.getStoreManager().getFieldManagerForStatementGeneration(elementSM, ps, mappingDefinition, true));

        return jdbcPosition;
    }

    /**
     * Convenience method to add a WHERE clause to match an element.
     * For a non-serialised PC/Non-PC element appends "AND xxx = ?".
     * For a serialised PC/Non-PC element appends "AND xxx LIKE ?".
     * For a reference field (interface/Object) appends "AND xxx1 = ? AND xxx2 IS NULL ...".
     * @param stmt The statement so far that we append to
     * @param elementMapping Mapping for the element
     * @param element The element to match
     * @param elementsSerialised Whether the elements are stored serialised
     * @param containerAlias Any alias for the container of this mapping
     * @param firstWhereClause Whether this is the first WHERE clause (i.e omit the first "AND")
     */
    public static void appendWhereClauseForElement(StringBuffer stmt, JavaTypeMapping elementMapping, Object element,
            boolean elementsSerialised, String containerAlias, boolean firstWhereClause)
    {
        if (!firstWhereClause)
        {
            stmt.append(" AND ");
        }
        if (elementMapping instanceof ReferenceMapping && elementMapping.getNumberOfDatastoreMappings() > 1)
        {
            // Mapping with multiple FK, with element only matching one
            for (int i = 0; i < elementMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(" AND ");
                }

                if (containerAlias != null)
                {
                    stmt.append(containerAlias).append(".");
                }
                stmt.append(elementMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                if (((ReferenceMapping)elementMapping).getJavaTypeMapping()[i].getType().equals(element.getClass().getName()))
                {
                    if (elementsSerialised)
                    {
                        stmt.append(" LIKE ");
                    }
                    else
                    {
                        stmt.append("=");
                    }
                    stmt.append(((RDBMSMapping) elementMapping.getDatastoreMapping(i)).getUpdateInputParameter());
                }
                else
                {
                    stmt.append(" IS NULL");
                }
            }
        }
        else
        {
            for (int i = 0; i < elementMapping.getNumberOfDatastoreMappings(); i++)
            {
                if (i > 0)
                {
                    stmt.append(" AND ");
                }

                if (containerAlias != null)
                {
                    stmt.append(containerAlias).append(".");
                }
                stmt.append(elementMapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
                if (elementsSerialised)
                {
                    stmt.append(" LIKE ");
                }
                else
                {
                    stmt.append("=");
                }
                stmt.append(((RDBMSMapping) elementMapping.getDatastoreMapping(i)).getUpdateInputParameter());
            }
        }
    }

    /**
     * Convenience method to add a WHERE clause restricting the specified mapping.
     * Appends something like <pre>"[AND] MYFIELD1 = ? [AND MYFIELD2 = ?]"</pre>
     * @param stmt The statement to append onto
     * @param mapping The mapping to restrict
     * @param containerAlias Any alias for the container of this mapping
     * @param firstWhereClause Whether this is the first WHERE clause (i.e omit the first "AND")
     */
    public static void appendWhereClauseForMapping(StringBuffer stmt, JavaTypeMapping mapping, String containerAlias,
            boolean firstWhereClause)
    {
        for (int i = 0; i < mapping.getNumberOfDatastoreMappings(); i++)
        {
            if (!firstWhereClause || (firstWhereClause && i > 0))
            {
                stmt.append(" AND ");
            }
            if (containerAlias != null)
            {
                stmt.append(containerAlias).append(".");
            }
            stmt.append(mapping.getDatastoreMapping(i).getDatastoreField().getIdentifier().toString());
            stmt.append("=");
            stmt.append(((RDBMSMapping) mapping.getDatastoreMapping(i)).getInsertionInputParameter());
        }
    }
}