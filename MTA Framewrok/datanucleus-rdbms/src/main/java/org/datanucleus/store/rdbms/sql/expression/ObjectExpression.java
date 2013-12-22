/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.sql.expression;

import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.OID;
import org.datanucleus.identity.OIDFactory;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.InheritanceStrategy;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.mapping.BigDecimalMapping;
import org.datanucleus.store.mapped.mapping.BigIntegerMapping;
import org.datanucleus.store.mapped.mapping.BooleanMapping;
import org.datanucleus.store.mapped.mapping.ByteMapping;
import org.datanucleus.store.mapped.mapping.CharacterMapping;
import org.datanucleus.store.mapped.mapping.DateMapping;
import org.datanucleus.store.mapped.mapping.DiscriminatorMapping;
import org.datanucleus.store.mapped.mapping.DoubleMapping;
import org.datanucleus.store.mapped.mapping.EmbeddedMapping;
import org.datanucleus.store.mapped.mapping.FloatMapping;
import org.datanucleus.store.mapped.mapping.IntegerMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.LongMapping;
import org.datanucleus.store.mapped.mapping.PersistableIdMapping;
import org.datanucleus.store.mapped.mapping.PersistableMapping;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.mapped.mapping.ShortMapping;
import org.datanucleus.store.mapped.mapping.SqlDateMapping;
import org.datanucleus.store.mapped.mapping.SqlTimeMapping;
import org.datanucleus.store.mapped.mapping.SqlTimestampMapping;
import org.datanucleus.store.mapped.mapping.StringMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.table.Column;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Representation of an Object expression in a Query. Typically represents a persistable object,
 * and so its identity, though could be used to represent any Object.
 * <p>
 * Let's take an example. We have classes A and B, and A contains a reference to B "b".
 * If we do a JDOQL query for class A of "b == value" then "b" is interpreted first 
 * and an ObjectExpression is created to represent that object (of type B).
 * </p>
 */
public class ObjectExpression extends SQLExpression
{
    /** Localiser for messages */
    protected static final Localiser LOCALISER_CORE = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructor for an SQL expression for a (field) mapping in a specified table.
     * @param stmt The statement
     * @param table The table in the statement
     * @param mapping The mapping for the field
     */
    public ObjectExpression(SQLStatement stmt, SQLTable table, JavaTypeMapping mapping)
    {
        super(stmt, table, mapping);
    }

    /**
     * Method to change the expression to use only the first column.
     * This is used where we want to use the expression with COUNT(...) and that only allows 1 column.
     */
    public void useFirstColumnOnly()
    {
        if (mapping.getNumberOfDatastoreMappings() <= 1)
        {
            // Do nothing
            return;
        }

        // Replace the expressionList and SQL as if starting from scratch
        subExprs = new ColumnExpressionList();
        ColumnExpression colExpr =
            new ColumnExpression(stmt, table, (Column)mapping.getDatastoreMapping(0).getDatastoreField());
        subExprs.addExpression(colExpr);
        st.clearStatement();
        st.append(subExprs.toString());
    }

    /**
     * Equals operator. Called when the query contains "obj == value" where "obj" is this object.
     * @param expr The expression we compare with (the right-hand-side in the query)
     * @return Boolean expression representing the comparison.
     */
    public BooleanExpression eq(SQLExpression expr)
    {
        addSubexpressionsToRelatedExpression(expr);

        // Check suitability for comparison
        // TODO Implement checks
        if (mapping instanceof PersistableIdMapping)
        {
            // Special Case : OID comparison ("id == val")
            if (expr instanceof StringLiteral)
            {
                String oidString = (String)((StringLiteral)expr).getValue();
                if (oidString != null)
                {
                    AbstractClassMetaData cmd =
                        stmt.getRDBMSManager().getMetaDataManager().getMetaDataForClass(mapping.getType(),
                            stmt.getQueryGenerator().getClassLoaderResolver());
                    if (cmd.getIdentityType() == IdentityType.DATASTORE)
                    {
                        try
                        {
                            OID oid = OIDFactory.getInstance(stmt.getRDBMSManager().getNucleusContext(), oidString);
                            if (oid == null)
                            {
                                // TODO Implement this comparison with the key value
                            }
                        }
                        catch (IllegalArgumentException iae)
                        {
                            NucleusLogger.QUERY.info("Attempted comparison of " + this + " and " + expr +
                                " where the former is a datastore-identity and the latter is of incorrect form (" +
                                oidString + ")");
                            SQLExpressionFactory exprFactory = stmt.getSQLExpressionFactory();
                            JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
                            return exprFactory.newLiteral(stmt, m, false).eq(exprFactory.newLiteral(stmt, m, true));
                        }
                    }
                    else if (cmd.getIdentityType() == IdentityType.APPLICATION)
                    {
                        // TODO Implement comparison with PK field(s)
                    }
                }
            }
        }

        if (mapping instanceof ReferenceMapping && expr.mapping instanceof PersistableMapping)
        {
            return processComparisonOfImplementationWithReference(this, expr, false);
        }
        else if (mapping instanceof PersistableMapping && expr.mapping instanceof ReferenceMapping)
        {
            return processComparisonOfImplementationWithReference(expr, this, false);
        }

        BooleanExpression bExpr = null;
        if (isParameter() || expr.isParameter())
        {
            if (this.subExprs.size() > 1)
            {
                for (int i=0;i<subExprs.size();i++)
                {
                    BooleanExpression subexpr = subExprs.getExpression(i).eq(((ObjectExpression)expr).subExprs.getExpression(i));
                    bExpr = (bExpr == null ? subexpr : bExpr.and(subexpr));
                }
                return bExpr;
            }
            else
            {
                // Comparison with parameter, so just give boolean compare
                return new BooleanExpression(this, Expression.OP_EQ, expr);
            }
        }
        else if (expr instanceof NullLiteral)
        {
            for (int i=0;i<subExprs.size();i++)
            {
                BooleanExpression subexpr = expr.eq(subExprs.getExpression(i));
                bExpr = (bExpr == null ? subexpr : bExpr.and(subexpr));
            }
            return bExpr;
        }
        else if (literalIsValidForSimpleComparison(expr))
        {
            if (subExprs.size() > 1)
            {
                // More than 1 value to compare with a simple literal!
                return super.eq(expr);
            }
            else
            {
                // Just do a direct comparison with the basic literals
                return new BooleanExpression(this, Expression.OP_EQ, expr);
            }
        }
        else if (expr instanceof ObjectExpression)
        {
            return ExpressionUtils.getEqualityExpressionForObjectExpressions(this, (ObjectExpression)expr, true);
        }
        else
        {
            return super.eq(expr);
        }
    }

    protected BooleanExpression processComparisonOfImplementationWithReference(SQLExpression refExpr, SQLExpression implExpr,
            boolean negate)
    {
        ReferenceMapping refMapping = (ReferenceMapping)refExpr.mapping;
        JavaTypeMapping[] implMappings = refMapping.getJavaTypeMapping();
        int subExprStart = 0;
        int subExprEnd = 0;
        for (int i=0;i<implMappings.length;i++)
        {
            // TODO Handle case where we have a subclass of the implementation here
            if (implMappings[i].getType().equals(implExpr.mapping.getType()))
            {
                subExprEnd = subExprStart + implMappings[i].getNumberOfDatastoreMappings();
                break;
            }
            else
            {
                subExprStart += implMappings[i].getNumberOfDatastoreMappings();
            }
        }

        BooleanExpression bExpr = null;
        int implMappingNum = 0;
        for (int i=subExprStart;i<subExprEnd;i++)
        {
            BooleanExpression subexpr = refExpr.subExprs.getExpression(i).eq(implExpr.subExprs.getExpression(implMappingNum++));
            bExpr = (bExpr == null ? subexpr : bExpr.and(subexpr));
        }

        if (bExpr == null)
        {
            // Implementation not found explicitly, so just treat as if ObjectExpression.eq(ObjectExpression)
            // See e.g JDO2 TCK "companyPMInterface" test
            return ExpressionUtils.getEqualityExpressionForObjectExpressions((ObjectExpression) refExpr, (ObjectExpression)implExpr, true);
        }

        return (negate ? new BooleanExpression(Expression.OP_NOT, bExpr.encloseInParentheses()) : bExpr);
    }

    /**
     * Not equals operator. Called when the query contains "obj != value" where "obj" is this object.
     * @param expr The expression we compare with (the right-hand-side in the query)
     * @return Boolean expression representing the comparison.
     */
    public BooleanExpression ne(SQLExpression expr)
    {
        addSubexpressionsToRelatedExpression(expr);

        if (mapping instanceof ReferenceMapping && expr.mapping instanceof PersistableMapping)
        {
            return processComparisonOfImplementationWithReference(this, expr, true);
        }
        else if (mapping instanceof PersistableMapping && expr.mapping instanceof ReferenceMapping)
        {
            return processComparisonOfImplementationWithReference(expr, this, true);
        }

        BooleanExpression bExpr = null;
        if (isParameter() || expr.isParameter())
        {
            if (this.subExprs.size() > 1)
            {
                for (int i=0;i<subExprs.size();i++)
                {
                    BooleanExpression subexpr =
                        subExprs.getExpression(i).eq(((ObjectExpression)expr).subExprs.getExpression(i));
                    bExpr = (bExpr == null ? subexpr : bExpr.and(subexpr));
                }
                return new BooleanExpression(Expression.OP_NOT, bExpr.encloseInParentheses());
            }
            else
            {
                // Comparison with parameter, so just give boolean compare
                return new BooleanExpression(this, Expression.OP_NOTEQ, expr);
            }
        }
        else if (expr instanceof NullLiteral)
        {
            for (int i=0;i<subExprs.size();i++)
            {
                BooleanExpression subexpr = expr.eq(subExprs.getExpression(i));
                bExpr = (bExpr == null ? subexpr : bExpr.and(subexpr));
            }
            return new BooleanExpression(Expression.OP_NOT, bExpr.encloseInParentheses());
        }
        else if (literalIsValidForSimpleComparison(expr))
        {
            if (subExprs.size() > 1)
            {
                // More than 1 value to compare with a literal!
                return super.ne(expr);
            }
            else
            {
                // Just do a direct comparison with the basic literals
                return new BooleanExpression(this, Expression.OP_NOTEQ, expr);
            }
        }
        else if (expr instanceof ObjectExpression)
        {
            return ExpressionUtils.getEqualityExpressionForObjectExpressions(this, (ObjectExpression)expr, false);
        }
        else
        {
            return super.ne(expr);
        }
    }

    /**
     * Updates the supplied expression with sub-expressions of consistent types to this expression.
     * This is called when we have some comparison expression (e.g this == expr) and where the
     * other expression has no sub-expressions currently.
     * @param expr The expression
     */
    protected void addSubexpressionsToRelatedExpression(SQLExpression expr)
    {
        if (expr.subExprs == null)
        {
            // operand has no sub-expressions yet (object input parameter) so add some
            expr.subExprs = new ColumnExpressionList();
            for (int i=0;i<subExprs.size();i++)
            {
                // TODO Put value of subMapping in
                expr.subExprs.addExpression(new ColumnExpression(stmt, 
                    expr.parameterName, expr.mapping, null, i));
            }
        }
    }

    /**
     * Convenience method to return if this object is valid for simple comparison
     * with the passed expression. Performs a type comparison of the object and the expression
     * for compatibility. The expression must be a literal of a suitable type for simple
     * comparison (e.g where this object is a String, and the literal is a StringLiteral).
     * @param expr The expression
     * @return Whether a simple comparison is valid
     */
    private boolean literalIsValidForSimpleComparison(SQLExpression expr)
    {
        // Our mapping is a single field type and is of the same basic type as the expression
        if ((expr instanceof BooleanLiteral && (mapping instanceof BooleanMapping)) ||
            (expr instanceof ByteLiteral && (mapping instanceof ByteMapping)) ||
            (expr instanceof CharacterLiteral && (mapping instanceof CharacterMapping)) ||
            (expr instanceof FloatingPointLiteral && 
             (mapping instanceof FloatMapping || mapping instanceof DoubleMapping ||
              mapping instanceof BigDecimalMapping)) ||
            (expr instanceof IntegerLiteral &&
             (mapping instanceof IntegerMapping || mapping instanceof LongMapping ||
              mapping instanceof BigIntegerMapping) || mapping instanceof ShortMapping) ||
            (expr instanceof TemporalLiteral &&
             (mapping instanceof DateMapping || mapping instanceof SqlDateMapping || 
              mapping instanceof SqlTimeMapping || mapping instanceof SqlTimestampMapping)) ||
            (expr instanceof StringLiteral &&
             (mapping instanceof StringMapping || mapping instanceof CharacterMapping)))
        {
            return true;
        }

        return false;
    }

    public BooleanExpression in(SQLExpression expr, boolean not) 
    {
        return new BooleanExpression(this, not ? Expression.OP_NOTIN : Expression.OP_IN, expr);
    }

    /**
     * Cast operator. Called when the query contains "(type)obj" where "obj" is this object.
     * @param expr Expression representing the type to cast to
     * @return Scalar expression representing the cast object.
     */
    public SQLExpression cast(SQLExpression expr)
    {
        RDBMSStoreManager storeMgr = stmt.getRDBMSManager();
        ClassLoaderResolver clr = stmt.getClassLoaderResolver();

        // Extract cast type
        String castClassName = (String)((StringLiteral)expr).getValue();
        Class type = null;
        try
        {
            type = stmt.getQueryGenerator().resolveClass(castClassName);
        }
        catch (ClassNotResolvedException cnre)
        {
            type = null;
        }
        if (type == null)
        {
            throw new NucleusUserException(LOCALISER_CORE.msg("037017", castClassName));
        }

        // Extract type of this object and check obvious conditions
        SQLExpressionFactory exprFactory = stmt.getSQLExpressionFactory();
        Class memberType = clr.classForName(mapping.getType());
        if (!memberType.isAssignableFrom(type) && !type.isAssignableFrom(memberType))
        {
            // object type and cast type are totally incompatible, so just return false
            JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
            return exprFactory.newLiteral(stmt, m, false).eq(exprFactory.newLiteral(stmt, m, true));
        }
        else if (memberType == type)
        {
            // Just return this expression since it is already castable
            return this;
        }

        if (mapping instanceof EmbeddedMapping)
        {
            // Don't support embedded casts
            JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
            return exprFactory.newLiteral(stmt, m, false).eq(exprFactory.newLiteral(stmt, m, true));
        }
        else if (mapping instanceof ReferenceMapping)
        {
            // This expression will be for the table containing the reference so need to join now
            ReferenceMapping refMapping = (ReferenceMapping)mapping;
            if (refMapping.getMappingStrategy() != ReferenceMapping.PER_IMPLEMENTATION_MAPPING)
            {
                throw new NucleusUserException("Impossible to do cast of interface to " + type.getName() +
                    " since interface is persisted as embedded String." +
                    " Use per-implementation mapping to allow this query");
            }
            JavaTypeMapping[] implMappings = refMapping.getJavaTypeMapping();
            for (int i=0;i<implMappings.length;i++)
            {
                Class implType = clr.classForName(implMappings[i].getType());
                if (type.isAssignableFrom(implType))
                {
                    DatastoreClass castTable = storeMgr.getDatastoreClass(type.getName(), clr);
                    SQLTable castSqlTbl = stmt.leftOuterJoin(table, implMappings[i], refMapping,
                        castTable, null, castTable.getIdMapping(), null, null, null);
                    return exprFactory.newExpression(stmt, castSqlTbl, castTable.getIdMapping());
                }
            }

            // No implementation matching this cast type, so return false
            NucleusLogger.QUERY.warn("Unable to process cast of interface field to " + type.getName() +
                " since it has no implementations that match that type");
            JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
            return exprFactory.newLiteral(stmt, m, false).eq(exprFactory.newLiteral(stmt, m, true));
        }
        else if (mapping instanceof PersistableMapping)
        {
            // Check if there is already the cast table in the current table group
            DatastoreClass castTable = storeMgr.getDatastoreClass(type.getName(), clr);
            SQLTable castSqlTbl = stmt.getTable(castTable, table.getGroupName());
            if (castSqlTbl == null)
            {
                // Join not present, so join to the cast table
                castSqlTbl = stmt.leftOuterJoin(table, table.getTable().getIdMapping(),
                    castTable, null, castTable.getIdMapping(), null, table.getGroupName());
            }

            // Return an expression based on the cast table
            return exprFactory.newExpression(stmt, castSqlTbl, castTable.getIdMapping());
        }
        else
        {
            // TODO Handle other casts
        }

        // TODO Implement cast (left outer join to table of type, then return new ObjectExpression)
        throw new NucleusUserException("Dont currently support ObjectExpression.cast(" + type + ")");
    }

    /**
     * An "is" (instanceOf) expression, providing a BooleanExpression whether this expression
     * is an instanceof the provided type.
     * @param expr The expression representing the type
     * @param not Whether the operator is "!instanceof"
     * @return Whether this expression is an instance of the provided type
     */
    public BooleanExpression is(SQLExpression expr, boolean not)
    {
        RDBMSStoreManager storeMgr = stmt.getRDBMSManager();
        ClassLoaderResolver clr = stmt.getClassLoaderResolver();

        // Extract instanceOf type
        String instanceofClassName = (String)((StringLiteral)expr).getValue();
        Class type = null;
        try
        {
            type = stmt.getQueryGenerator().resolveClass(instanceofClassName);
        }
        catch (ClassNotResolvedException cnre)
        {
            type = null;
        }
        if (type == null)
        {
            throw new NucleusUserException(LOCALISER_CORE.msg("037016", instanceofClassName));
        }

        // Extract type of member and check obvious conditions
        SQLExpressionFactory exprFactory = stmt.getSQLExpressionFactory();
        Class memberType = clr.classForName(mapping.getType());
        if (!memberType.isAssignableFrom(type) && !type.isAssignableFrom(memberType))
        {
            // Member type and instanceof type are totally incompatible, so just return false
            JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
            return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, not));
        }
        else if (memberType == type)
        {
            // instanceof type is the same as the member type therefore must comply (can't store supertypes)
            JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
            return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, !not));
        }

        if (mapping instanceof EmbeddedMapping)
        {
            // Don't support embedded instanceof expressions
            AbstractClassMetaData fieldCmd = storeMgr.getMetaDataManager().getMetaDataForClass(
                mapping.getType(), clr);
            if (fieldCmd.hasDiscriminatorStrategy())
            {
                // Embedded field with inheritance so add discriminator restriction
                JavaTypeMapping discMapping = ((EmbeddedMapping)mapping).getDiscriminatorMapping();
                DiscriminatorMetaData dismd = fieldCmd.getDiscriminatorMetaDataRoot();
                AbstractClassMetaData typeCmd = storeMgr.getMetaDataManager().getMetaDataForClass(type, clr);
                SQLExpression discExpr = stmt.getSQLExpressionFactory().newExpression(stmt, table, discMapping);
                SQLExpression discVal = null;
                if (dismd.getStrategy() == DiscriminatorStrategy.CLASS_NAME)
                {
                    discVal = stmt.getSQLExpressionFactory().newLiteral(stmt, discMapping, typeCmd.getFullClassName());
                }
                else
                {
                    discVal = stmt.getSQLExpressionFactory().newLiteral(stmt, discMapping,
                        typeCmd.getDiscriminatorMetaData().getValue());
                }
                BooleanExpression typeExpr = (not ? discExpr.ne(discVal) : discExpr.eq(discVal));

                Iterator<String> subclassIter = storeMgr.getSubClassesForClass(type.getName(), true, clr).iterator();
                while (subclassIter.hasNext())
                {
                    String subclassName = subclassIter.next();
                    AbstractClassMetaData subtypeCmd =
                        storeMgr.getMetaDataManager().getMetaDataForClass(subclassName, clr);
                    if (dismd.getStrategy() == DiscriminatorStrategy.CLASS_NAME)
                    {
                        discVal = stmt.getSQLExpressionFactory().newLiteral(stmt, discMapping,
                            subtypeCmd.getFullClassName());
                    }
                    else
                    {
                        discVal = stmt.getSQLExpressionFactory().newLiteral(stmt, discMapping,
                            subtypeCmd.getDiscriminatorMetaData().getValue());
                    }
                    BooleanExpression subtypeExpr = (not ? discExpr.ne(discVal) : discExpr.eq(discVal));

                    if (not)
                    {
                        typeExpr = typeExpr.and(subtypeExpr);
                    }
                    else
                    {
                        typeExpr = typeExpr.ior(subtypeExpr);
                    }
                }

                return typeExpr;
            }
            else
            {
                JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
                return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, not));
            }
        }
        else if (mapping instanceof PersistableMapping || mapping instanceof ReferenceMapping)
        {
            // Field has its own table, so join to it
            AbstractClassMetaData memberCmd = storeMgr.getMetaDataManager().getMetaDataForClass(mapping.getType(), clr);
            DatastoreClass memberTable = null;
            if (memberCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
            {
                // Field is a PC class that uses "subclass-table" inheritance strategy (and so has multiple possible tables to join to)
                AbstractClassMetaData[] cmds = storeMgr.getClassesManagingTableForClass(memberCmd, clr);
                if (cmds != null)
                {
                    // Join to the first table
                    // TODO Allow for all possible tables. Can we do an OR of the tables ? How ?
                    if (cmds.length > 1)
                    {
                        NucleusLogger.QUERY.warn(LOCALISER_CORE.msg("037006",
                            mapping.getMemberMetaData().getFullFieldName(), cmds[0].getFullClassName()));
                    }
                    memberTable = storeMgr.getDatastoreClass(cmds[0].getFullClassName(), clr);
                }
                else
                {
                    // No subclasses with tables to join to, so throw a user error
                    throw new NucleusUserException(LOCALISER_CORE.msg("037005", 
                        mapping.getMemberMetaData().getFullFieldName()));
                }
            }
            else
            {
                // Class of the field will have its own table
                memberTable = storeMgr.getDatastoreClass(mapping.getType(), clr);
            }

            DiscriminatorMetaData dismd = memberTable.getDiscriminatorMetaData();
            DiscriminatorMapping discMapping = (DiscriminatorMapping)memberTable.getDiscriminatorMapping(false);
            if (discMapping != null)
            {
                SQLTable targetSqlTbl = null;
                if (mapping.getDatastoreContainer() != memberTable)
                {
                    // FK is on source table so inner join to target table (holding the discriminator)
                    targetSqlTbl = stmt.getTable(memberTable, null);
                    if (targetSqlTbl == null)
                    {
                        targetSqlTbl = stmt.innerJoin(getSQLTable(), mapping, memberTable, null, memberTable.getIdMapping(),
                            null, null);
                    }
                }
                else
                {
                    // FK is on target side and already joined
                    targetSqlTbl = SQLStatementHelper.getSQLTableForMappingOfTable(stmt, getSQLTable(), discMapping);
                }

                // Add restrict to discriminator for the instanceOf type and subclasses
                SQLTable discSqlTbl = targetSqlTbl;
                BooleanExpression discExpr =
                    SQLStatementHelper.getExpressionForDiscriminatorForClass(stmt, type.getName(),
                        dismd, discMapping, discSqlTbl, clr);

                Iterator subclassIter = storeMgr.getSubClassesForClass(type.getName(), true, clr).iterator();
                boolean hasSubclass = false;
                while (subclassIter.hasNext())
                {
                    String subclassName = (String)subclassIter.next();
                    BooleanExpression discExprSub =
                        SQLStatementHelper.getExpressionForDiscriminatorForClass(stmt, subclassName,
                            dismd, discMapping, discSqlTbl, clr);
                    discExpr = discExpr.ior(discExprSub);
                    hasSubclass = true;
                }
                if (hasSubclass)
                {
                    discExpr.encloseInParentheses();
                }
                return (not ? discExpr.not() : discExpr);
            }
            else
            {
                // Join to member table
                DatastoreClass table = null;
                if (memberCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                {
                    // Field is a PC class that uses "subclass-table" inheritance strategy (and so has multiple possible tables to join to)
                    AbstractClassMetaData[] cmds = storeMgr.getClassesManagingTableForClass(memberCmd, clr);
                    if (cmds != null)
                    {
                        // Join to the first table
                        // TODO Allow for all possible tables. Can we do an OR of the tables ? How ?
                        if (cmds.length > 1)
                        {
                            NucleusLogger.QUERY.warn(LOCALISER_CORE.msg("037006",
                                mapping.getMemberMetaData().getFullFieldName(), cmds[0].getFullClassName()));
                        }
                        table = storeMgr.getDatastoreClass(cmds[0].getFullClassName(), clr);
                    }
                    else
                    {
                        // No subclasses with tables to join to, so throw a user error
                        throw new NucleusUserException(LOCALISER_CORE.msg("037005",
                            mapping.getMemberMetaData().getFullFieldName()));
                    }
                }
                else
                {
                    // Class of the field will have its own table
                    table = storeMgr.getDatastoreClass(mapping.getType(), clr);
                }

                if (table.managesClass(type.getName()))
                {
                    // This type is managed in this table so must be an instance TODO Is this correct, what if using discrim?
                    JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
                    return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, !not));
                }
                else
                {
                    if (table == stmt.getPrimaryTable().getTable())
                    {
                        // This is member table, so just need to restrict to the instanceof type now
                        JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
                        if (stmt.getNumberOfUnions() > 0)
                        {
                            // a). we have unions for the member, so restrict to just the applicable unions
                            // Note that this is only really valid is wanting "a instanceof SUB1".
                            // It fails when we want to do "a instanceof SUB1 || a instanceof SUB2"
                            // TODO How do we handle those cases?
                            Class mainCandidateCls = clr.classForName(stmt.getCandidateClassName());
                            if (type.isAssignableFrom(mainCandidateCls) == not)
                            {
                                SQLExpression unionClauseExpr = exprFactory.newLiteral(stmt, m, true).eq(
                                    exprFactory.newLiteral(stmt, m, false));
                                stmt.whereAnd((BooleanExpression)unionClauseExpr, false);
                            }

                            List<SQLStatement> unionStmts = stmt.getUnions();
                            Iterator<SQLStatement> iter = unionStmts.iterator();
                            while (iter.hasNext())
                            {
                                SQLStatement unionStmt = iter.next();
                                Class unionCandidateCls = clr.classForName(unionStmt.getCandidateClassName());
                                if (type.isAssignableFrom(unionCandidateCls) == not)
                                {
                                    SQLExpression unionClauseExpr = exprFactory.newLiteral(unionStmt, m, true).eq(
                                        exprFactory.newLiteral(unionStmt, m, false));
                                    unionStmt.whereAnd((BooleanExpression)unionClauseExpr, false);
                                }
                            }

                            // Just return true since we applied the condition direct to the unions
                            SQLExpression returnExpr = exprFactory.newLiteral(stmt, m, true).eq(
                                exprFactory.newLiteral(stmt, m, true));
                            return (BooleanExpression)returnExpr;
                        }
                        else
                        {
                            // b). The member table doesn't manage the instanceof type, so do inner join to 
                            // the table of the instanceof to impose the instanceof condition
                            DatastoreClass instanceofTable = storeMgr.getDatastoreClass(type.getName(), clr);
                            stmt.innerJoin(this.table, this.table.getTable().getIdMapping(),
                                instanceofTable, null, instanceofTable.getIdMapping(), null, this.table.getGroupName());
                            return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, !not));
                        }
                    }
                    else
                    {
                        // Do inner join to this table to impose the instanceOf
                        DatastoreClass instanceofTable = storeMgr.getDatastoreClass(type.getName(), clr);
                        if (stmt.getNumberOfUnions() > 0)
                        {
                            // Inner join will likely not give the right result
                            NucleusLogger.QUERY.debug("InstanceOf for " + table +
                                " but no discriminator so adding inner join to " + instanceofTable +
                            " : in some cases with UNIONs this may fail");
                        }
                        stmt.innerJoin(this.table, this.table.getTable().getIdMapping(),
                            instanceofTable, null, instanceofTable.getIdMapping(), null, this.table.getGroupName());
                        JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
                        return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, !not));
                    }
                }
            }
        }
        else
        {
            // TODO Implement instanceof for other types
            throw new NucleusException("Dont currently support " + this + " instanceof " + type.getName());
        }
    }

    public SQLExpression invoke(String methodName, List args)
    {
        return stmt.getRDBMSManager().getSQLExpressionFactory().invokeMethod(stmt, Object.class.getName(), 
            methodName, this, args);
    }
}