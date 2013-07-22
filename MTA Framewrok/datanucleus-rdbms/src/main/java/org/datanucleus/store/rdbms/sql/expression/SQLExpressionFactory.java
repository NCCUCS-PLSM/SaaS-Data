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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.method.SQLMethod;
import org.datanucleus.store.rdbms.sql.operation.SQLOperation;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Factory for creating SQL expressions/literals.
 * These are typically called when we are building up an SQL statement and we want to impose conditions
 * using the fields of a class, and values for the field.
 */
public class SQLExpressionFactory
{
    /** Localiser for messages */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.store.rdbms.Localisation", RDBMSStoreManager.class.getClassLoader());

    RDBMSStoreManager storeMgr;

    ClassLoaderResolver clr;

    private final Class[] EXPR_CREATION_ARG_TYPES = 
        new Class[] {SQLStatement.class, SQLTable.class, JavaTypeMapping.class};

    private final Class[] LIT_CREATION_ARG_TYPES = 
        new Class[] {SQLStatement.class, JavaTypeMapping.class, Object.class, String.class};

    /** Cache of expression class, keyed by the mapping class name. */
    Map<String, Class> expressionClassByMappingName = new HashMap();

    /** Cache of literal class, keyed by the mapping class name. */
    Map<String, Class> literalClassByMappingName = new HashMap();

    /** Map of SQLMethod class name, keyed by "datastore#{class}.{method}". */
    Map<String, String> methodClassByDatastoreMethodName = new HashMap<String, String>();

    /** Map of SQLOperation class name, keyed by "datastore#{operation}". */
    Map<String, String> operationClassByDatastoreOperationName = new HashMap<String, String>();

    /** Cache of already created SQLMethod instances, keyed by their class+method name. */
    Map<String, SQLMethod> methodByClassMethodName = new HashMap<String, SQLMethod>();

    Map<String, SQLOperation> operationByOperationName = new HashMap<String, SQLOperation>();

    /** Map of JavaTypeMapping for use in query expressions, keyed by the type being represented. */
    Map<Class, JavaTypeMapping> mappingByClass = new HashMap<Class, JavaTypeMapping>();

    /**
     * Constructor for an SQLExpressionFactory.
     * Also loads up the defined SQL methods
     * [extension-point: "org.datanucleus.store.rdbms.sql_method"] and caches them.
     * Also loads up the defined SQL operations
     * [extension-point: "org.datanucleus.store.rdbms.sql_operation"] and caches them.
     * @param storeMgr RDBMS Manager
     */
    public SQLExpressionFactory(RDBMSStoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
        this.clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);

        PluginManager pluginMgr = storeMgr.getNucleusContext().getPluginManager();
        ConfigurationElement[] methodElems = 
            pluginMgr.getConfigurationElementsForExtension("org.datanucleus.store.rdbms.sql_method", null, null);
        if (methodElems != null)
        {
            for (int i=0;i<methodElems.length;i++)
            {
                String datastoreName = methodElems[i].getAttribute("datastore");
                String className = methodElems[i].getAttribute("class");
                String methodName = methodElems[i].getAttribute("method").trim();
                String sqlMethodName = methodElems[i].getAttribute("evaluator").trim();

                String key = getSQLMethodKey(datastoreName, className, methodName);
                methodClassByDatastoreMethodName.put(key, sqlMethodName);
            }
        }

        ConfigurationElement[] operationElems = 
            pluginMgr.getConfigurationElementsForExtension("org.datanucleus.store.rdbms.sql_operation", null, null);
        if (operationElems != null)
        {
            for (int i=0;i<operationElems.length;i++)
            {
                String datastoreName = operationElems[i].getAttribute("datastore");
                String name = operationElems[i].getAttribute("name").trim();
                String sqlOperationName = operationElems[i].getAttribute("evaluator").trim();

                String key = getSQLOperationKey(datastoreName, name);
                operationClassByDatastoreOperationName.put(key, sqlOperationName);
            }
        }
    }

    /**
     * Factory for an expression representing a mapping on a table.
     * @param stmt The statement
     * @param sqlTbl The table
     * @param mapping The mapping
     * @return The expression
     */
    public SQLExpression newExpression(SQLStatement stmt, SQLTable sqlTbl, JavaTypeMapping mapping)
    {
        return newExpression(stmt, sqlTbl, mapping, null);
    }

    /**
     * Factory for an expression representing a mapping on a table.
     * @param stmt The statement
     * @param sqlTbl The table
     * @param mapping The mapping
     * @param parentMapping Optional parent mapping of this mapping (e.g when handling impl of an interface)
     * @return The expression
     */
    public SQLExpression newExpression(SQLStatement stmt, SQLTable sqlTbl, JavaTypeMapping mapping,
            JavaTypeMapping parentMapping)
    {
        SQLTable exprSqlTbl = SQLStatementHelper.getSQLTableForMappingOfTable(stmt, sqlTbl, 
            parentMapping == null ? mapping : parentMapping);
        Object[] args = new Object[] {stmt, exprSqlTbl, mapping};

        Class expressionClass = expressionClassByMappingName.get(mapping.getClass().getName());
        if (expressionClass != null)
        {
            // Use cached expression class
            return (SQLExpression)ClassUtils.newInstance(expressionClass, EXPR_CREATION_ARG_TYPES, 
                new Object[] {stmt, exprSqlTbl, mapping});
        }

        try
        {
            // Use "new SQLExpression(SQLStatement, SQLTable, JavaTypeMapping)"
            SQLExpression sqlExpr = (SQLExpression)storeMgr.getNucleusContext().getPluginManager().createExecutableExtension(
                "org.datanucleus.store.rdbms.sql_expression", "mapping-class", mapping.getClass().getName(), 
                "expression-class", EXPR_CREATION_ARG_TYPES, args);
            if (sqlExpr == null)
            {
                throw new NucleusException(LOCALISER.msg("060004", mapping.getClass().getName()));
            }

            expressionClassByMappingName.put(mapping.getClass().getName(), sqlExpr.getClass());
            return sqlExpr;
        }
        catch (Exception e)
        {
            String msg = LOCALISER.msg("060005", mapping.getClass().getName());
            NucleusLogger.QUERY.error(msg, e);
            throw new NucleusException(msg, e);
        }
    }

    /**
     * Factory for a literal representing a value.
     * To create a NullLiteral pass in a null mapping.
     * @param stmt The statement
     * @param mapping The mapping
     * @param value The value
     * @return The literal
     */
    public SQLExpression newLiteral(SQLStatement stmt, JavaTypeMapping mapping, Object value)
    {
        Object[] args = new Object[] {stmt, mapping, value, null};

        if (mapping != null)
        {
            Class literalClass = literalClassByMappingName.get(mapping.getClass().getName());
            if (literalClass != null)
            {
                // Use cached literal class
                return (SQLExpression)ClassUtils.newInstance(literalClass, LIT_CREATION_ARG_TYPES, args);
            }
        }

        try
        {
            // Use "new SQLLiteral(SQLStatement, JavaTypeMapping, Object, String)"
            if (mapping == null)
            {
                return (SQLExpression)ClassUtils.newInstance(NullLiteral.class, LIT_CREATION_ARG_TYPES, args);
            }
            else
            {
                SQLExpression sqlExpr = (SQLExpression) storeMgr.getNucleusContext().getPluginManager().createExecutableExtension(
                    "org.datanucleus.store.rdbms.sql_expression", "mapping-class", mapping.getClass().getName(), 
                    "literal-class", LIT_CREATION_ARG_TYPES, args);
                if (sqlExpr == null)
                {
                    throw new NucleusException(LOCALISER.msg("060006", mapping.getClass().getName()));
                }

                literalClassByMappingName.put(mapping.getClass().getName(), sqlExpr.getClass());
                return sqlExpr;
            }
        }
        catch (Exception e)
        {
            NucleusLogger.QUERY.error("Exception creating SQLLiteral for mapping " + mapping.getClass().getName(), e);
            throw new NucleusException(LOCALISER.msg("060007", mapping.getClass().getName()));
        }
    }

    /**
     * Factory for a literal as an input parameter.
     * If the mapping (type of parameter) is not known at this point then put in null and it
     * will return a ParameterLiteral.
     * @param stmt The statement
     * @param mapping The mapping
     * @param value Value of the literal (if known)
     * @param paramName The parameter name
     * @return The literal
     */
    public SQLExpression newLiteralParameter(SQLStatement stmt, JavaTypeMapping mapping, Object value, String paramName)
    {
        try
        {
            // Use "new SQLLiteral(SQLStatement, JavaTypeMapping)"
            Class[] argTypes = new Class[] {SQLStatement.class, JavaTypeMapping.class, Object.class, String.class};
            Object[] args = new Object[] {stmt, mapping, value, paramName};
            if (mapping == null)
            {
                return (SQLExpression)ClassUtils.newInstance(ParameterLiteral.class, argTypes, args);
            }
            else
            {
                SQLExpression sqlExpr = (SQLExpression) storeMgr.getNucleusContext().getPluginManager().createExecutableExtension(
                    "org.datanucleus.store.rdbms.sql_expression", "mapping-class", mapping.getClass().getName(), 
                    "literal-class", argTypes, args);
                if (sqlExpr == null)
                {
                    throw new NucleusException(LOCALISER.msg("060006", mapping.getClass().getName()));
                }
                return sqlExpr;
            }
        }
        catch (Exception e)
        {
            NucleusLogger.QUERY.error("Exception creating SQLLiteral for mapping " + mapping.getClass().getName(), e);
            throw new NucleusException(LOCALISER.msg("060007", mapping.getClass().getName()));
        }
    }

    /**
     * Accessor for the result of a method call on the supplied expression with the supplied args.
     * Throws a NucleusException is the method is not supported.
     * Note that the class name passed in has to be a specific class that has a method defined for it,
     * so it can't be a subclass of a supported class method for example.
     * @param stmt SQLStatement that this relates to
     * @param className Class we are invoking the method on
     * @param methodName Name of the method
     * @param expr The expression we invoke the method on
     * @param args Any arguments to the method call
     * @return The result
     */
    public SQLExpression invokeMethod(SQLStatement stmt, String className, String methodName,
            SQLExpression expr, List args)
    {
        // Try to find an existing instance of the required SQLMethod
        String classMethodKey = (className != null ? (className + "_" + methodName) : methodName);
        SQLMethod method = methodByClassMethodName.get(classMethodKey);
        if (method != null)
        {
            // Reuse method, setting statement for this usage
            synchronized (method) // Only permit sole usage at any time
            {
                method.setStatement(stmt);
                return method.getExpression(expr, args);
            }
        }

        // No existing instance so try a datastore-dependent key
        String datastoreId = storeMgr.getDatastoreAdapter().getVendorID();
        String key = getSQLMethodKey(datastoreId, className, methodName);
        String sqlMethodClassName = methodClassByDatastoreMethodName.get(key);
        if (sqlMethodClassName == null)
        {
            // No datastore-dependent method, so try a datastore-independent key
            key = getSQLMethodKey(null, className, methodName);
            sqlMethodClassName = methodClassByDatastoreMethodName.get(key);
            if (sqlMethodClassName == null)
            {
                if (className != null)
                {
                    throw new NucleusUserException(LOCALISER.msg("060008", methodName, className));
                }
                else
                {
                    throw new NucleusUserException(LOCALISER.msg("060009", methodName));
                }
            }
        }

        // Use SQLMethod().getExpression(SQLExpression, args)
        try
        {
            method = (SQLMethod)stmt.getClassLoaderResolver().classForName(sqlMethodClassName).newInstance();
            synchronized (method) // Only permit sole usage at any time
            {
                method.setStatement(stmt);

                // Cache the method in case its used later
                methodByClassMethodName.put(classMethodKey, method);

                return method.getExpression(expr, args);
            }
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new NucleusUserException(LOCALISER.msg("060010", sqlMethodClassName));
        }
        catch (InstantiationException e)
        {
            throw new NucleusUserException(LOCALISER.msg("060011", sqlMethodClassName), e);
        }
        catch (IllegalAccessException e)
        {
            throw new NucleusUserException(LOCALISER.msg("060011", sqlMethodClassName), e);
        }
    }

    /**
     * Accessor for the result of an operation call on the supplied expression with the supplied args.
     * Throws a NucleusException is the method is not supported.
     * @param name Operation to be invoked
     * @param expr The first expression to perform the operation on
     * @param expr2 The second expression to perform the operation on
     * @return The result
     * @throws UnsupportedOperationException if the operation is not specified
     */
    public SQLExpression invokeOperation(String name, SQLExpression expr, SQLExpression expr2)
    {
        // Try to find an instance of the SQLOperation
        SQLOperation operation = operationByOperationName.get(name);
        if (operation != null)
        {
            return operation.getExpression(expr, expr2);
        }

        // Try a datastore-specific key
        String datastoreId = storeMgr.getDatastoreAdapter().getVendorID();
        String key = getSQLOperationKey(datastoreId, name);
        String sqlOperationClassName = operationClassByDatastoreOperationName.get(key);
        if (sqlOperationClassName == null)
        {
            // Try a datastore-independent key
            key = getSQLOperationKey(null, name);
            sqlOperationClassName = operationClassByDatastoreOperationName.get(key);
            if (sqlOperationClassName == null)
            {
                throw new UnsupportedOperationException();
            }
        }

        // Use SQLOperation().getExpression(SQLExpression, SQLExpression)
        SQLStatement stmt = expr.getSQLStatement();
        try
        {
            operation = (SQLOperation)stmt.getClassLoaderResolver().classForName(sqlOperationClassName).newInstance();
            operation.setExpressionFactory(this);
            operationByOperationName.put(name, operation);

            return operation.getExpression(expr, expr2);
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new NucleusException(LOCALISER.msg("060013", sqlOperationClassName));
        }
        catch (InstantiationException e)
        {
            throw new NucleusException(LOCALISER.msg("060014", sqlOperationClassName), e);
        }
        catch (IllegalAccessException e)
        {
            throw new NucleusException(LOCALISER.msg("060014", sqlOperationClassName), e);
        }
    }

    /**
     * Convenience method to return the key for the SQL method.
     * Returns a string like <pre>{datastore}#{class}.{method}</pre> if the class is defined, and
     * <pre>{datastore}#{method}</pre> if the class is not defined (function).
     * @param datastoreName Vendor id of the RDBMS datastore
     * @param className Name of the class that we are invoking on (null if static).
     * @param methodName Method to be invoked
     * @return Key for the SQLMethod
     */
    private String getSQLMethodKey(String datastoreName, String className, String methodName)
    {
        return (datastoreName != null ? datastoreName.trim() : "ALL") + "#" +
            (className != null ? (className.trim() + ".") : "") + methodName;
    }

    /**
     * Convenience method to return the key for the SQL operation.
     * Returns a string like <pre>{datastore}#{operation}</pre>.
     * @param datastoreName Vendor id of the RDBMS datastore
     * @param name Operation to be invoked
     * @return Key for the SQLOperation
     */
    private String getSQLOperationKey(String datastoreName, String name)
    {
        return (datastoreName != null ? datastoreName.trim() : "ALL") + "#" + name;
    }

    /**
     * Accessor for a mapping to use in a query expression.
     * @param cls The class that the mapping should represent.
     * @param useCached Whether to use any cached mapping (if available)
     * @return The mapping
     */
    public JavaTypeMapping getMappingForType(Class cls, boolean useCached)
    {
        JavaTypeMapping mapping = null;
        if (useCached)
        {
            mapping = mappingByClass.get(cls);
            if (mapping != null)
            {
                return mapping;
            }
        }
        mapping = storeMgr.getMappingManager().getMappingWithDatastoreMapping(cls, false, false, clr);
        mappingByClass.put(cls, mapping);
        return mapping;
    }
}