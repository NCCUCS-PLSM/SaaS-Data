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
package org.datanucleus.store.rdbms.sql.method;

import java.util.List;
import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.MapMetaData.MapType;
import org.datanucleus.query.compiler.CompilationComponent;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.SQLJoin.JoinType;
import org.datanucleus.store.rdbms.sql.expression.BooleanExpression;
import org.datanucleus.store.rdbms.sql.expression.BooleanLiteral;
import org.datanucleus.store.rdbms.sql.expression.BooleanSubqueryExpression;
import org.datanucleus.store.rdbms.sql.expression.MapExpression;
import org.datanucleus.store.rdbms.sql.expression.MapLiteral;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.UnboundExpression;
import org.datanucleus.store.rdbms.sql.expression.MapLiteral.MapKeyLiteral;
import org.datanucleus.store.rdbms.table.JoinTable;
import org.datanucleus.store.rdbms.table.MapTable;
import org.datanucleus.util.NucleusLogger;

/**
 * Method for evaluating {mapExpr}.containsKey(keyExpr).
 * Returns a BooleanExpression.
 */
public class MapContainsKeyMethod extends AbstractSQLMethod
{
    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.sql.method.SQLMethod#getExpression(org.datanucleus.store.rdbms.sql.expression.SQLExpression, java.util.List)
     */
    public SQLExpression getExpression(SQLExpression expr, List args)
    {
        if (args == null || args.size() == 0 || args.size() > 1)
        {
            throw new NucleusException(LOCALISER.msg("060016", "containsKey", "MapExpression", 1));
        }

        MapExpression mapExpr = (MapExpression)expr;
        SQLExpression keyExpr = (SQLExpression)args.get(0);

        if (keyExpr.isParameter())
        {
            // Key is a parameter so make sure its type is set
            AbstractMemberMetaData mmd = mapExpr.getJavaTypeMapping().getMemberMetaData();
            if (mmd != null && mmd.getMap() != null)
            {
                Class keyCls = stmt.getQueryGenerator().getClassLoaderResolver().classForName(mmd.getMap().getKeyType());
                stmt.getQueryGenerator().bindParameter(keyExpr.getParameterName(), keyCls);
            }
        }

        if (expr instanceof MapLiteral)
        {
            MapLiteral lit = (MapLiteral)expr;
            Map map = (Map)lit.getValue();
            if (map == null || map.size() == 0)
            {
                JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
                return new BooleanLiteral(stmt, m, Boolean.FALSE);
            }

            // TODO If keyExpr is a parameter and mapExpr is derived from a parameter ?
            MapKeyLiteral mapKeyLiteral = lit.getKeyLiteral();
            BooleanExpression bExpr = null;
            List<SQLExpression> elementExprs = mapKeyLiteral.getKeyExpressions();
            for (int i=0; i<elementExprs.size(); i++)
            {
                if (bExpr == null)
                {
                    bExpr = (elementExprs.get(i)).eq(keyExpr); 
                }
                else
                {
                    bExpr = bExpr.ior((elementExprs.get(i)).eq(keyExpr)); 
                }
            }
            bExpr.encloseInParentheses();
            return bExpr;
        }
        else
        {
            if (stmt.getQueryGenerator().getCompilationComponent() == CompilationComponent.FILTER)
            {
                boolean needsSubquery = getNeedsSubquery();

                // TODO Check if *this* "containsKey" is negated, not any of them (and remove above check)
                if (needsSubquery)
                {
                    NucleusLogger.QUERY.debug("MapContainsKey on " + mapExpr + "(" + keyExpr + ") using SUBQUERY");
                    return containsAsSubquery(mapExpr, keyExpr);
                }
                else
                {
                    NucleusLogger.QUERY.debug("MapContainsKey on " + mapExpr + "(" + keyExpr + ") using INNERJOIN");
                    return containsAsInnerJoin(mapExpr, keyExpr);
                }
            }
            else
            {
                return containsAsSubquery(mapExpr, keyExpr);
            }
        }
    }

    /**
     * Convenience method to decide if we handle the contains() by using a subquery, or otherwise
     * via an inner join. If there is an OR or a NOT in the query then uses a subquery.
     * @return Whether to use a subquery
     */
    protected boolean getNeedsSubquery()
    {
        // TODO Check if *this* "contains" is negated, not just any of them (and remove above check)
        boolean needsSubquery = false;
        Boolean hasOR = (Boolean)stmt.getQueryGenerator().getProperty("Filter.OR");
        if (hasOR != null && hasOR.booleanValue())
        {
            needsSubquery = true;
        }
        Boolean hasNOT = (Boolean)stmt.getQueryGenerator().getProperty("Filter.NOT");
        if (hasNOT != null && hasNOT.booleanValue())
        {
            needsSubquery = true;
        }
        return needsSubquery;
    }

    /**
     * Method to return an expression for Map.containsKey using INNER JOIN to the element.
     * This is only for use when there are no "!containsKey" and no "OR" operations.
     * Creates SQL by adding INNER JOIN to the join table (where it exists), and also to the key table
     * adding an AND condition on the element (with value of the keyExpr).
     * Returns a BooleanExpression "TRUE" (since the INNER JOIN will guarantee if the key is
     * contained of not).
     * @param mapExpr Map expression
     * @param keyExpr Expression for the key
     * @return Contains expression
     */
    protected SQLExpression containsAsInnerJoin(MapExpression mapExpr, SQLExpression keyExpr)
    {
        boolean keyIsUnbound = (keyExpr instanceof UnboundExpression);
        String varName = null;
        String keyAlias = null;
        if (keyIsUnbound)
        {
            varName = ((UnboundExpression)keyExpr).getVariableName();
            NucleusLogger.QUERY.debug(">> map.containsKey(" + keyExpr + ") binding unbound variable " + varName +
                " using INNER JOIN");
            // TODO What if the variable is declared as a subtype, handle this see CollectionContainsMethod
        }
        else if (!stmt.getQueryGenerator().hasExplicitJoins())
        {
            JoinType joinType = stmt.getJoinTypeForTable(keyExpr.getSQLTable());
            if (joinType == JoinType.CROSS_JOIN)
            {
                keyAlias = stmt.removeCrossJoin(keyExpr.getSQLTable());
                keyIsUnbound = true;
                NucleusLogger.QUERY.debug(">> map.containsKey(" + keyExpr +
                    ") was previously bound as CROSS JOIN but changing to INNER JOIN");
            }
        }

        RDBMSStoreManager storeMgr = stmt.getRDBMSManager();
        MetaDataManager mmgr = storeMgr.getMetaDataManager();
        AbstractMemberMetaData mmd = mapExpr.getJavaTypeMapping().getMemberMetaData();
        AbstractClassMetaData keyCmd = mmd.getMap().getKeyClassMetaData(clr, mmgr);
        if (mmd.getMap().getMapType() == MapType.MAP_TYPE_JOIN)
        {
            // Map formed in join table - add join to join table, then to key table (if present)
            MapTable mapTbl = (MapTable)storeMgr.getDatastoreContainerObject(mmd);
            SQLTable joinSqlTbl = stmt.innerJoin(mapExpr.getSQLTable(), mapExpr.getSQLTable().getTable().getIdMapping(),
                mapTbl, null, mapTbl.getOwnerMapping(), null, null);
            if (keyCmd != null)
            {
                if (keyIsUnbound)
                {
                    DatastoreClass keyTbl = storeMgr.getDatastoreClass(keyCmd.getFullClassName(), clr);
                    SQLTable keySqlTbl = stmt.innerJoin(joinSqlTbl, mapTbl.getKeyMapping(), 
                        keyTbl, keyAlias, keyTbl.getIdMapping(), null, null);

                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(stmt, keySqlTbl, keyTbl.getIdMapping());
                    stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(), 
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression keyIdExpr = exprFactory.newExpression(stmt, joinSqlTbl, mapTbl.getKeyMapping());
                    stmt.whereAnd(keyIdExpr.eq(keyExpr), true);
                }
            }
            else
            {
                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(stmt, joinSqlTbl, mapTbl.getKeyMapping());
                    stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(), 
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression keyIdExpr = exprFactory.newExpression(stmt, joinSqlTbl, mapTbl.getKeyMapping());
                    stmt.whereAnd(keyIdExpr.eq(keyExpr), true);
                }
            }
        }
        else if (mmd.getMap().getMapType() == MapType.MAP_TYPE_KEY_IN_VALUE)
        {
            // Map formed in value table - add join to value table, then to key table
            AbstractClassMetaData valCmd = mmd.getMap().getValueClassMetaData(clr, mmgr);
            DatastoreClass valTbl = storeMgr.getDatastoreClass(valCmd.getFullClassName(), clr);
            AbstractMemberMetaData valKeyMmd =
                valCmd.getMetaDataForMember(mmd.getKeyMetaData().getMappedBy());
            JavaTypeMapping ownerMapping = null;
            if (mmd.getMappedBy() != null)
            {
                ownerMapping = valTbl.getMemberMapping(valCmd.getMetaDataForMember(mmd.getMappedBy()));
            }
            else
            {
                ownerMapping = valTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
            }

            SQLTable valSqlTbl = stmt.innerJoin(mapExpr.getSQLTable(), mapExpr.getSQLTable().getTable().getIdMapping(),
                valTbl, null, ownerMapping, null, null);

            if (keyCmd != null)
            {
                DatastoreClass keyTbl = storeMgr.getDatastoreClass(keyCmd.getFullClassName(), clr);
                SQLTable keySqlTbl = stmt.innerJoin(valSqlTbl, valTbl.getMemberMapping(valKeyMmd),
                    keyTbl, keyAlias, keyTbl.getIdMapping(), null, null);

                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(stmt, keySqlTbl, keyTbl.getIdMapping());
                    stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(), 
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression keyIdExpr = exprFactory.newExpression(stmt, keySqlTbl, keyTbl.getIdMapping());
                    stmt.whereAnd(keyIdExpr.eq(keyExpr), true);
                }
            }
            else
            {
                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(stmt, valSqlTbl, valTbl.getMemberMapping(valKeyMmd));
                    stmt.getQueryGenerator().bindVariable(varName, null, keyExpr.getSQLTable(), 
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression keyIdExpr = exprFactory.newExpression(stmt, valSqlTbl, valTbl.getMemberMapping(valKeyMmd));
                    stmt.whereAnd(keyIdExpr.eq(keyExpr), true);
                }
            }
        }
        else if (mmd.getMap().getMapType() == MapType.MAP_TYPE_VALUE_IN_KEY)
        {
            // Map formed in key table - add join to key table
            DatastoreClass keyTbl = storeMgr.getDatastoreClass(keyCmd.getFullClassName(), clr);
            JavaTypeMapping ownerMapping = null;
            if (mmd.getMappedBy() != null)
            {
                ownerMapping = keyTbl.getMemberMapping(keyCmd.getMetaDataForMember(mmd.getMappedBy()));
            }
            else
            {
                ownerMapping = keyTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
            }
            SQLTable keySqlTbl = stmt.innerJoin(mapExpr.getSQLTable(), mapExpr.getSQLTable().getTable().getIdMapping(),
                keyTbl, keyAlias, ownerMapping, null, null);

            if (keyIsUnbound)
            {
                // Bind the variable in the QueryGenerator
                keyExpr = exprFactory.newExpression(stmt, keySqlTbl, keyTbl.getIdMapping());
                stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(), 
                    keyExpr.getJavaTypeMapping());
            }
            else
            {
                // Add restrict to key
                SQLExpression keyIdExpr = exprFactory.newExpression(stmt, keySqlTbl, keyTbl.getIdMapping());
                stmt.whereAnd(keyIdExpr.eq(keyExpr), true);
            }
        }

        JavaTypeMapping m = exprFactory.getMappingForType(boolean.class, true);
        return exprFactory.newLiteral(stmt, m, true).eq(exprFactory.newLiteral(stmt, m, true));
    }

    /**
     * Method to return an expression for Map.containsKey using a subquery "EXISTS".
     * This is for use when there are "!contains" or "OR" operations in the filter.
     * Creates the following SQL,
     * <ul>
     * <li><b>Map using join table</b>
     * <pre>
     * SELECT 1 FROM JOIN_TBL A0_SUB 
     * WHERE A0_SUB.JOIN_OWN_ID = A0.ID AND A0_SUB.JOIN_KEY_ID = {keyExpr}
     * </pre>
     * </li>
     * <li><b>Map with key stored in value</b>
     * <pre>
     * SELECT 1 FROM KEY_TABLE A0_SUB INNER JOIN VALUE_TBL B0 ON ... 
     * WHERE B0.JOIN_OWN_ID = A0.ID AND A0_SUB.ID = {keyExpr}
     * </pre>
     * </li>
     * <li><b>Map of value stored in key</b>
     * <pre>
     * SELECT 1 FROM KEY_TABLE A0_SUB
     * WHERE A0_SUB.OWN_ID = A0.ID AND A0_SUB.ID = {keyExpr}
     * </pre>
     * </li>
     * </ul>
     * and returns a BooleanSubqueryExpression ("EXISTS (subquery)")
     * @param mapExpr Map expression
     * @param keyExpr Expression for the key
     * @return Contains expression
     */
    protected SQLExpression containsAsSubquery(MapExpression mapExpr, SQLExpression keyExpr)
    {
        boolean keyIsUnbound = (keyExpr instanceof UnboundExpression);
        String varName = null;
        if (keyIsUnbound)
        {
            varName = ((UnboundExpression)keyExpr).getVariableName();
            NucleusLogger.QUERY.debug(">> Map.containsKey binding unbound variable " + varName +
                " using SUBQUERY");
            // TODO What if the variable is declared as a subtype, handle this see CollectionContainsMethod
        }

        RDBMSStoreManager storeMgr = stmt.getRDBMSManager();
        MetaDataManager mmgr = storeMgr.getMetaDataManager();
        AbstractMemberMetaData mmd = mapExpr.getJavaTypeMapping().getMemberMetaData();
        AbstractClassMetaData keyCmd = mmd.getMap().getKeyClassMetaData(clr, mmgr);
        MapTable joinTbl = (MapTable)storeMgr.getDatastoreContainerObject(mmd);
        SQLStatement subStmt = null;
        if (mmd.getMap().getMapType() == MapType.MAP_TYPE_JOIN)
        {
            // JoinTable Map
            if (keyCmd == null)
            {
                // Map<Non-PC, ?>
                subStmt = new SQLStatement(stmt, storeMgr, joinTbl, null, null);
                subStmt.setClassLoaderResolver(clr);
                JavaTypeMapping oneMapping = storeMgr.getMappingManager().getMapping(Integer.class);
                subStmt.select(exprFactory.newLiteral(subStmt, oneMapping, 1), null);

                // Restrict to map owner
                JavaTypeMapping ownerMapping = ((JoinTable)joinTbl).getOwnerMapping();
                SQLExpression ownerExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), ownerMapping);
                SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, mapExpr.getSQLTable(),
                    mapExpr.getSQLTable().getTable().getIdMapping());
                subStmt.whereAnd(ownerExpr.eq(ownerIdExpr), true);

                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), joinTbl.getKeyMapping());
                    stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(), 
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression elemIdExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(),
                        joinTbl.getKeyMapping());
                    subStmt.whereAnd(elemIdExpr.eq(keyExpr), true);
                }
            }
            else
            {
                // Map<PC, ?>
                DatastoreClass keyTbl = storeMgr.getDatastoreClass(mmd.getMap().getKeyType(), clr);
                subStmt = new SQLStatement(stmt, storeMgr, keyTbl, null, null);
                subStmt.setClassLoaderResolver(clr);
                JavaTypeMapping oneMapping = storeMgr.getMappingManager().getMapping(Integer.class);
                subStmt.select(exprFactory.newLiteral(subStmt, oneMapping, 1), null);

                // Join to join table
                SQLTable joinSqlTbl = subStmt.innerJoin(subStmt.getPrimaryTable(), keyTbl.getIdMapping(),
                    joinTbl, null, joinTbl.getKeyMapping(), null, null);

                // Restrict to map owner
                JavaTypeMapping ownerMapping = joinTbl.getOwnerMapping();
                SQLExpression ownerExpr = exprFactory.newExpression(subStmt, joinSqlTbl, ownerMapping);
                SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, mapExpr.getSQLTable(),
                    mapExpr.getSQLTable().getTable().getIdMapping());
                subStmt.whereAnd(ownerExpr.eq(ownerIdExpr), true);

                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), keyTbl.getIdMapping());
                    stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(),
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression keyIdExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(),
                        keyTbl.getIdMapping());
                    subStmt.whereAnd(keyIdExpr.eq(keyExpr), true);
                }
            }
        }
        else if (mmd.getMap().getMapType() == MapType.MAP_TYPE_KEY_IN_VALUE)
        {
            // Key stored in value table
            AbstractClassMetaData valCmd = mmd.getMap().getValueClassMetaData(clr, mmgr);
            DatastoreClass valTbl = storeMgr.getDatastoreClass(mmd.getMap().getValueType(), clr);
            JavaTypeMapping ownerMapping = null;
            if (mmd.getMappedBy() != null)
            {
                ownerMapping = valTbl.getMemberMapping(valCmd.getMetaDataForMember(mmd.getMappedBy()));
            }
            else
            {
                ownerMapping = valTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
            }
            AbstractMemberMetaData valKeyMmd =
                valCmd.getMetaDataForMember(mmd.getKeyMetaData().getMappedBy());
            if (keyCmd == null)
            {
                subStmt = new SQLStatement(stmt, storeMgr, valTbl, null, null);
                subStmt.setClassLoaderResolver(clr);
                JavaTypeMapping oneMapping = storeMgr.getMappingManager().getMapping(Integer.class);
                subStmt.select(exprFactory.newLiteral(subStmt, oneMapping, 1), null);

                // Restrict to map owner (on value table)
                SQLExpression ownerExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), ownerMapping);
                SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, mapExpr.getSQLTable(),
                    mapExpr.getSQLTable().getTable().getIdMapping());
                subStmt.whereAnd(ownerExpr.eq(ownerIdExpr), true);

                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), valTbl.getMemberMapping(valKeyMmd));
                    stmt.getQueryGenerator().bindVariable(varName, null, keyExpr.getSQLTable(),
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    JavaTypeMapping keyMapping = valTbl.getMemberMapping(valKeyMmd);
                    SQLExpression elemIdExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), keyMapping);
                    subStmt.whereAnd(elemIdExpr.eq(keyExpr), true);
                }
            }
            else
            {
                DatastoreClass keyTbl = storeMgr.getDatastoreClass(mmd.getMap().getKeyType(), clr);
                subStmt = new SQLStatement(stmt, storeMgr, keyTbl, null, null);
                subStmt.setClassLoaderResolver(clr);
                JavaTypeMapping oneMapping = storeMgr.getMappingManager().getMapping(Integer.class);
                subStmt.select(exprFactory.newLiteral(subStmt, oneMapping, 1), null);

                // Join to value table
                SQLTable valSqlTbl = subStmt.innerJoin(subStmt.getPrimaryTable(), keyTbl.getIdMapping(),
                    valTbl, null, valTbl.getMemberMapping(valKeyMmd), null, null);

                // Restrict to map owner (on value table)
                SQLExpression ownerExpr = exprFactory.newExpression(subStmt, valSqlTbl, ownerMapping);
                SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, mapExpr.getSQLTable(),
                    mapExpr.getSQLTable().getTable().getIdMapping());
                subStmt.whereAnd(ownerExpr.eq(ownerIdExpr), true);

                if (keyIsUnbound)
                {
                    // Bind the variable in the QueryGenerator
                    keyExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), keyTbl.getIdMapping());
                    stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(),
                        keyExpr.getJavaTypeMapping());
                }
                else
                {
                    // Add restrict to key
                    SQLExpression keyIdExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(),
                        keyTbl.getIdMapping());
                    subStmt.whereAnd(keyIdExpr.eq(keyExpr), true);
                }
            }
        }
        else if (mmd.getMap().getMapType() == MapType.MAP_TYPE_VALUE_IN_KEY)
        {
            DatastoreClass keyTbl = storeMgr.getDatastoreClass(mmd.getMap().getKeyType(), clr);
            JavaTypeMapping ownerMapping = null;
            if (mmd.getMappedBy() != null)
            {
                ownerMapping = keyTbl.getMemberMapping(keyCmd.getMetaDataForMember(mmd.getMappedBy()));
            }
            else
            {
                ownerMapping = keyTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
            }

            subStmt = new SQLStatement(stmt, storeMgr, keyTbl, null, null);
            subStmt.setClassLoaderResolver(clr);
            JavaTypeMapping oneMapping = storeMgr.getMappingManager().getMapping(Integer.class);
            subStmt.select(exprFactory.newLiteral(subStmt, oneMapping, 1), null);

            // Restrict to map owner (on key table)
            SQLExpression ownerExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), ownerMapping);
            SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, mapExpr.getSQLTable(),
                mapExpr.getSQLTable().getTable().getIdMapping());
            subStmt.whereAnd(ownerExpr.eq(ownerIdExpr), true);

            if (keyIsUnbound)
            {
                // Bind the variable in the QueryGenerator
                keyExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), keyTbl.getIdMapping());
                stmt.getQueryGenerator().bindVariable(varName, keyCmd, keyExpr.getSQLTable(),
                    keyExpr.getJavaTypeMapping());
            }
            else
            {
                // Add restrict to key
                JavaTypeMapping keyMapping = keyTbl.getIdMapping();
                SQLExpression keyIdExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), keyMapping);
                subStmt.whereAnd(keyIdExpr.eq(keyExpr), true);
            }
        }

        return new BooleanSubqueryExpression(stmt, "EXISTS", subStmt);
    }
}