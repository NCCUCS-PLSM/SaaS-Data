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
package org.datanucleus.store.rdbms.sql;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.sql.expression.BooleanExpression;

/**
 * Representation of a join in an SQL statement.
 * The join is of a type (see ANSI SQL), and with inner/left outer/right outer is accompanied by
 * join condition(s), joining from the source table to the target table via columns. Additionally
 * other conditions can be applied to restrict the join (such as discriminator).
 */
public class SQLJoin
{
    public enum JoinType
    {
        NON_ANSI_JOIN,
        INNER_JOIN,
        LEFT_OUTER_JOIN,
        RIGHT_OUTER_JOIN,
        CROSS_JOIN
    }

    /** Type of join to perform. */
    private JoinType type;

    /** Table we are joining to. This is always set irrespective the type of join. */
    private SQLTable table;

    /** The current table that we are joining to to introduce this table. */
    private SQLTable joinedTable;

    /** Optional condition for the join. */
    private BooleanExpression condition;

    /**
     * Constructor for a join.
     * @param type Type of join (one of the defined types in this class).
     * @param tbl Table to join to (required)
     * @param condition Join condition
     */
    public SQLJoin(JoinType type, SQLTable tbl, SQLTable joinedTbl, BooleanExpression condition)
    {
        if (type != JoinType.NON_ANSI_JOIN && 
            type != JoinType.INNER_JOIN && 
            type != JoinType.LEFT_OUTER_JOIN && 
            type != JoinType.RIGHT_OUTER_JOIN && 
            type != JoinType.CROSS_JOIN)
        {
            throw new NucleusException("Unsupported join type specified : " + type);
        }
        else if (tbl == null)
        {
            throw new NucleusException("Specification of join must supply the table reference");
        }

        this.type = type;
        this.table = tbl;
        this.joinedTable = joinedTbl;
        this.condition = condition;
    }

    public JoinType getType()
    {
        return type;
    }

    public void setType(JoinType type)
    {
        this.type = type;
    }

    public SQLTable getTable()
    {
        return table;
    }

    public SQLTable getJoinedTable()
    {
        return joinedTable;
    }

    /**
     * Accessor for the conditions of the join.
     * These conditions can include
     * @return The conditions
     */
    public BooleanExpression getCondition()
    {
        return condition;
    }

    public String toString()
    {
        if (type == JoinType.CROSS_JOIN)
        {
            return "JoinType: CROSSJOIN " + type + " tbl=" + table;
        }
        else if (type == JoinType.INNER_JOIN || type == JoinType.LEFT_OUTER_JOIN)
        {
            return "JoinType: " + (type == JoinType.INNER_JOIN ? "INNERJOIN" : "OUTERJOIN") + 
                " tbl=" + table + " joinedTbl=" + joinedTable;
        }
        return super.toString();
    }

    public String toFromClause(RDBMSAdapter dba, boolean lock)
    {
        if (type != JoinType.NON_ANSI_JOIN)
        {
            StringBuffer result = new StringBuffer();
            if (type == JoinType.INNER_JOIN)
            {
                result.append("INNER JOIN ");
            }
            else if (type == JoinType.LEFT_OUTER_JOIN)
            {
                result.append("LEFT OUTER JOIN ");
            }
            else if (type == JoinType.RIGHT_OUTER_JOIN)
            {
                result.append("RIGHT OUTER JOIN ");
            }
            else if (type == JoinType.CROSS_JOIN)
            {
                result.append("CROSS JOIN ");
            }
            result.append(table);

            if (type == JoinType.INNER_JOIN || type == JoinType.LEFT_OUTER_JOIN || type == JoinType.RIGHT_OUTER_JOIN)
            {
                result.append(" ON ");
                if (condition != null)
                {
                    result.append(condition.toSQLText().toSQL());
                }
            }

            if (lock && dba.supportsOption(RDBMSAdapter.LOCK_OPTION_PLACED_WITHIN_JOIN))
            {
                result.append(" WITH ").append(dba.getSelectWithLockOption());
            }

            return result.toString();
        }
        else
        {
            return "" + table;
        }
    }
}