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
package org.datanucleus.store.rdbms.sql.method;

import java.util.List;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.query.compiler.CompilationComponent;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.expression.CollectionExpression;
import org.datanucleus.store.rdbms.sql.expression.CollectionLiteral;
import org.datanucleus.store.rdbms.sql.expression.NullLiteral;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLLiteral;
import org.datanucleus.store.rdbms.sql.expression.SubqueryExpression;
import org.datanucleus.store.rdbms.table.CollectionTable;

/**
 * Method for evaluating {listExpr}.get(idxExpr).
 * Returns an ObjectExpression representing the element
 */
public class ListGetMethod extends AbstractSQLMethod
{
    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.sql.method.SQLMethod#getExpression(org.datanucleus.store.rdbms.sql.expression.SQLExpression, java.util.List)
     */
    public SQLExpression getExpression(SQLExpression expr, List args)
    {
        if (args == null || args.size() == 0 || args.size() > 1)
        {
            throw new NucleusException(LOCALISER.msg("060016", "get", "CollectionExpression", 1));
        }

        CollectionExpression listExpr = (CollectionExpression)expr;
        AbstractMemberMetaData mmd = listExpr.getJavaTypeMapping().getMemberMetaData();
        if (!List.class.isAssignableFrom(mmd.getType()))
        {
            throw new UnsupportedOperationException("Query contains " + expr + ".get(int) yet the field is not a List!");
        }
        else if (mmd.getOrderMetaData() != null && !mmd.getOrderMetaData().isIndexedList())
        {
            throw new UnsupportedOperationException("Query contains " + expr + ".get(int) yet the field is not an 'indexed' List!");
        }

        SQLExpression idxExpr = (SQLExpression)args.get(0);
        if (idxExpr instanceof SQLLiteral)
        {
            if (!(((SQLLiteral)idxExpr).getValue() instanceof Number))
            {
                throw new UnsupportedOperationException("Query contains " + expr + ".get(int) yet the index is not a numeric literal so not yet supported");
            }

        }
        else
        {
            throw new UnsupportedOperationException("Query contains " + expr + ".get(int) yet the index is not a numeric literal so not yet supported");
        }

        if (listExpr instanceof CollectionLiteral && idxExpr instanceof SQLLiteral)
        {
            CollectionLiteral lit = (CollectionLiteral)expr;
            if (lit.getValue() == null)
            {
                return new NullLiteral(stmt, null, null, null);
            }

            return lit.invoke("get", args);
        }
        else
        {
            if (stmt.getQueryGenerator().getCompilationComponent() == CompilationComponent.FILTER)
            {
                return getAsInnerJoin(listExpr, idxExpr);
            }
            else if (stmt.getQueryGenerator().getCompilationComponent() == CompilationComponent.ORDERING ||
                    stmt.getQueryGenerator().getCompilationComponent() == CompilationComponent.RESULT)
            {
                return getAsSubquery(listExpr, idxExpr);
            }

            throw new NucleusException("List.get() is not supported for " + listExpr +
                " with argument " + idxExpr + " for query component " + stmt.getQueryGenerator().getCompilationComponent());
        }
    }

    /**
     * Implementation of Collection.get() using a subquery on the table representing the collection,
     * adding a condition on the index and returning the element.
     * @param listExpr The list expression
     * @param idxExpr The index expression
     * @return The element expression
     */
    protected SQLExpression getAsSubquery(CollectionExpression listExpr, SQLExpression idxExpr)
    {
        AbstractMemberMetaData mmd = listExpr.getJavaTypeMapping().getMemberMetaData();
        RDBMSStoreManager storeMgr = stmt.getRDBMSManager();

        JavaTypeMapping ownerMapping = null;
        JavaTypeMapping indexMapping = null;
        JavaTypeMapping elemMapping = null;
        DatastoreContainerObject listTbl = null;
        if (mmd != null)
        {
            AbstractMemberMetaData[] relatedMmds = mmd.getRelatedMemberMetaData(clr);
            if (mmd.getJoinMetaData() != null || relatedMmds != null && relatedMmds[0].getJoinMetaData() != null)
            {
                // Join table List
                listTbl = storeMgr.getDatastoreContainerObject(mmd);
                ownerMapping = ((CollectionTable)listTbl).getOwnerMapping();
                indexMapping = ((CollectionTable)listTbl).getOrderMapping();
                elemMapping = ((CollectionTable)listTbl).getElementMapping();
            }
            else
            {
                // FK List
                DatastoreClass elemTbl = storeMgr.getDatastoreClass(mmd.getCollection().getElementType(), clr);
                listTbl = elemTbl;

                if (relatedMmds != null)
                {
                    ownerMapping = elemTbl.getMemberMapping(relatedMmds[0]);
                    indexMapping = elemTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
                    elemMapping = elemTbl.getIdMapping();
                }
                else
                {
                    ownerMapping = elemTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
                    indexMapping = elemTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
                    elemMapping = elemTbl.getIdMapping();
                }
            }
        }

        SQLStatement subStmt = new SQLStatement(stmt, storeMgr, listTbl, null, null);
        subStmt.setClassLoaderResolver(clr);
        SQLExpression valExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), elemMapping);
        subStmt.select(valExpr, null);

        // Link to primary statement
        SQLExpression elementOwnerExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(),
            ownerMapping);
        SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, listExpr.getSQLTable(),
            listExpr.getSQLTable().getTable().getIdMapping());
        subStmt.whereAnd(elementOwnerExpr.eq(ownerIdExpr), true);

        // Condition on key
        SQLExpression keyExpr = exprFactory.newExpression(subStmt, subStmt.getPrimaryTable(), indexMapping);
        subStmt.whereAnd(keyExpr.eq(idxExpr), true);

        // Create subquery, with mapping being of the element type (since "get" returns an element)
        SubqueryExpression subExpr = new SubqueryExpression(stmt, subStmt);
        subExpr.setJavaTypeMapping(elemMapping);
        return subExpr;
    }

    /**
     * Implementation of List.get() using an inner join to the table representing the list,
     * adding a condition on the index and returning the element.
     * @param listExpr The list expression
     * @param idxExpr The index expression
     * @return The element expression
     */
    protected SQLExpression getAsInnerJoin(CollectionExpression listExpr, SQLExpression idxExpr)
    {
        JavaTypeMapping m = listExpr.getJavaTypeMapping();
        RDBMSStoreManager storeMgr = stmt.getRDBMSManager();
        AbstractMemberMetaData mmd = m.getMemberMetaData();

        AbstractMemberMetaData[] relatedMmds = mmd.getRelatedMemberMetaData(clr);
        if (mmd.getJoinMetaData() != null || relatedMmds != null && relatedMmds[0].getJoinMetaData() != null)
        {
            // Join table List
            CollectionTable joinTbl = (CollectionTable)storeMgr.getDatastoreContainerObject(mmd);

            // Add join to join table
            SQLTable joinSqlTbl = stmt.innerJoin(listExpr.getSQLTable(), listExpr.getSQLTable().getTable().getIdMapping(),
                joinTbl, null, joinTbl.getOwnerMapping(), null, null);

            // Add condition on index
            SQLExpression idxSqlExpr = exprFactory.newExpression(stmt, joinSqlTbl, joinTbl.getOrderMapping());
            stmt.whereAnd(idxSqlExpr.eq(idxExpr), true);

            // Return element expression
            SQLExpression valueExpr = exprFactory.newExpression(stmt, joinSqlTbl, joinTbl.getElementMapping());
            return valueExpr;
        }
        else
        {
            // FK List
            DatastoreClass elementTbl = storeMgr.getDatastoreClass(mmd.getCollection().getElementType(), clr);

            // Add join to element table
            JavaTypeMapping targetMapping = null;
            JavaTypeMapping orderMapping = null;
            if (relatedMmds != null)
            {
                targetMapping = elementTbl.getMemberMapping(relatedMmds[0]);
                orderMapping = elementTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
            }
            else
            {
                targetMapping = elementTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
                orderMapping = elementTbl.getExternalMapping(mmd, MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
            }
            SQLTable elemSqlTbl = stmt.innerJoin(listExpr.getSQLTable(), listExpr.getSQLTable().getTable().getIdMapping(),
                elementTbl, null, targetMapping, null, null);

            // Add condition on index
            SQLExpression idxSqlExpr = exprFactory.newExpression(stmt, elemSqlTbl, orderMapping);
            stmt.whereAnd(idxSqlExpr.eq(idxExpr), true);

            // Return element expression
            return exprFactory.newExpression(stmt, elemSqlTbl, elementTbl.getIdMapping());
        }
    }
}