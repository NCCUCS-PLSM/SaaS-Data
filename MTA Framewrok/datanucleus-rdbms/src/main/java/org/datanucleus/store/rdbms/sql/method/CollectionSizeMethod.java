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

import java.util.Collection;
import java.util.List;

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.expression.CollectionLiteral;
import org.datanucleus.store.rdbms.sql.expression.NumericSubqueryExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.StringLiteral;
import org.datanucleus.store.rdbms.table.JoinTable;

/**
 * Method for evaluating {collExpr1}.size().
 * Returns a NumericExpression. Equates to a sub-query on the table for the collection
 * returning the COUNT(*) of the number of elements. Something like
 * <PRE>
 * SELECT COUNT(*) FROM COLLTABLE A0_SUB WHERE A0_SUB.OWNER_ID_OID = A0.OWNER_ID
 * </PRE>
 * where A0 is the candidate table in the outer query, and COLLTABLE is the join table (if using 
 * join collection) or the element table (if using FK collection).
 */
public class CollectionSizeMethod extends AbstractSQLMethod
{
    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.sql.method.SQLMethod#getExpression(org.datanucleus.store.rdbms.sql.expression.SQLExpression, java.util.List)
     */
    public SQLExpression getExpression(SQLExpression expr, List args)
    {
        if (args != null && args.size() > 0)
        {
            throw new NucleusException(LOCALISER.msg("060015", "size", "CollectionExpression"));
        }

        if (expr instanceof CollectionLiteral)
        {
            // Just return the collection size since we have the value
            Collection coll = (Collection)((CollectionLiteral)expr).getValue();
            return exprFactory.newLiteral(stmt, exprFactory.getMappingForType(int.class, false),
                Integer.valueOf(coll.size()));
        }
        else
        {
            AbstractMemberMetaData mmd = expr.getJavaTypeMapping().getMemberMetaData();
            if (mmd.isSerialized())
            {
                throw new NucleusUserException("Cannot perform Collection.size when the collection is being serialised");
            }
            else
            {
                ApiAdapter api = stmt.getRDBMSManager().getApiAdapter();
                Class elementType = clr.classForName(mmd.getCollection().getElementType());
                if (!api.isPersistable(elementType) && mmd.getJoinMetaData() == null)
                {
                    throw new NucleusUserException(
                        "Cannot perform Collection.size when the collection<Non-Persistable> is not in a join table");
                }
            }

            String elementType = mmd.getCollection().getElementType();
            RDBMSStoreManager storeMgr = stmt.getRDBMSManager();

            // TODO Allow for interface elements, etc
            JavaTypeMapping ownerMapping = null;
            DatastoreContainerObject collectionTbl = null;
            if (mmd.getMappedBy() != null)
            {
                // Bidirectional
                AbstractMemberMetaData elementMmd = mmd.getRelatedMemberMetaData(clr)[0];
                if (mmd.getJoinMetaData() != null || elementMmd.getJoinMetaData() != null)
                {
                    // JoinTable
                    collectionTbl = storeMgr.getDatastoreContainerObject(mmd);
                    ownerMapping = ((JoinTable)collectionTbl).getOwnerMapping();
                }
                else
                {
                    // ForeignKey
                    collectionTbl = storeMgr.getDatastoreClass(elementType, clr);
                    ownerMapping = collectionTbl.getMemberMapping(elementMmd);
                }
            }
            else
            {
                // Unidirectional
                if (mmd.getJoinMetaData() != null)
                {
                    // JoinTable
                    collectionTbl = storeMgr.getDatastoreContainerObject(mmd);
                    ownerMapping = ((JoinTable)collectionTbl).getOwnerMapping();
                }
                else
                {
                    // ForeignKey
                    collectionTbl = storeMgr.getDatastoreClass(elementType, clr);
                    ownerMapping = ((DatastoreClass)collectionTbl).getExternalMapping(mmd, 
                        MappingConsumer.MAPPING_TYPE_EXTERNAL_FK);
                }
            }

            SQLStatement subStmt = new SQLStatement(stmt, storeMgr, collectionTbl, null, null);
            subStmt.setClassLoaderResolver(clr);
            JavaTypeMapping mapping =
                storeMgr.getMappingManager().getMappingWithDatastoreMapping(String.class, false, false, clr);
            SQLExpression countExpr = exprFactory.newLiteral(subStmt, mapping, "COUNT(*)");
            ((StringLiteral)countExpr).generateStatementWithoutQuotes();
            subStmt.select(countExpr, null);

            SQLExpression elementOwnerExpr = exprFactory.newExpression(subStmt,
                subStmt.getPrimaryTable(), ownerMapping);
            SQLExpression ownerIdExpr = exprFactory.newExpression(stmt, expr.getSQLTable(),
                expr.getSQLTable().getTable().getIdMapping());
            subStmt.whereAnd(elementOwnerExpr.eq(ownerIdExpr), true);

            JavaTypeMapping subqMapping = exprFactory.getMappingForType(Integer.class, false);
            SQLExpression subqExpr = new NumericSubqueryExpression(stmt, subStmt);
            subqExpr.setJavaTypeMapping(subqMapping);
            return subqExpr;
        }
    }
}