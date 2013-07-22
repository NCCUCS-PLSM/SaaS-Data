/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.MappingConsumer;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.expression.NumericExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.table.ClassTable;
import org.datanucleus.store.rdbms.table.CollectionTable;

/**
 * Expression handler for JPQL "INDEX" expression to return the index of an element.
 * Returns a NumericExpression.
 */
public class IndexFunction extends AbstractSQLMethod
{
    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.sql.method.SQLMethod#getExpression(org.datanucleus.store.rdbms.sql.expression.SQLExpression, java.util.List)
     */
    public SQLExpression getExpression(SQLExpression ignore, List args)
    {
        if (ignore == null)
        {
            if (args == null || args.size() != 2)
            {
                throw new NucleusException("INDEX can only be used with 2 arguments - the element expression, and the collection expression");
            }

            SQLExpression elemSqlExpr = (SQLExpression) args.get(0);
            SQLExpression collSqlExpr = (SQLExpression) args.get(1);

            AbstractMemberMetaData mmd = collSqlExpr.getJavaTypeMapping().getMemberMetaData();
            if (!mmd.hasCollection())
            {
                throw new NucleusException("INDEX expression for field " + mmd.getFullFieldName() +
                    " does not represent a collection!");
            }
            else if (!mmd.getOrderMetaData().isIndexedList())
            {
                throw new NucleusException("INDEX expression for field " + mmd.getFullFieldName() + 
                    " does not represent an indexed list!");
            }

            JavaTypeMapping orderMapping = null;
            SQLTable orderTable = null;
            DatastoreContainerObject joinTbl = stmt.getRDBMSManager().getDatastoreContainerObject(mmd);
            if (joinTbl != null)
            {
                // 1-N via join table
                CollectionTable collTable = (CollectionTable)joinTbl;
                orderTable = stmt.getTableForDatastoreContainer(collTable);
                // TODO If the join table is not yet referenced, or referenced multiple times then fix this
                orderMapping = collTable.getOrderMapping();
            }
            else
            {
                // 1-N via FK
                orderTable = elemSqlExpr.getSQLTable();
                orderMapping = ((ClassTable)elemSqlExpr.getSQLTable().getTable()).getExternalMapping(mmd, 
                    MappingConsumer.MAPPING_TYPE_EXTERNAL_INDEX);
            }

            return new NumericExpression(stmt, orderTable, orderMapping);
        }
        else
        {
            throw new NucleusException(LOCALISER.msg("060002", "INDEX", ignore));
        }
    }
}