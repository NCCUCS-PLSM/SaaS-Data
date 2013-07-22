/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.rdbms.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.datanucleus.store.query.AbstractQueryResult;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of a QueryResult for RDBMS queries.
 * Based on the assumption that we have a JDBC ResultSet, and we are extracting the results using
 * a ResultObjectFactory.
 */
public abstract class AbstractRDBMSQueryResult extends AbstractQueryResult
{
    /** The ResultSet containing the results. */
    protected ResultSet rs;

    /** ResultObjectFactory for converting the result set into objects. */
    protected ResultObjectFactory rof;

    /**
     * Constructor of the result from a Query.
     * @param query The Query
     * @param rof The factory to retrieve results from
     * @param rs The ResultSet from the Query Statement
     */
    public AbstractRDBMSQueryResult(Query query, ResultObjectFactory rof, ResultSet rs)
    {
        super(query);
        this.rof = rof;
        this.rs = rs;
    }

    /**
     * Method to disconnect the results from the ObjectManager, meaning that thereafter it just behaves
     * like a List. All remaining results are read in at this point (unless selected not to be).
     */
    public void disconnect()
    {
        if (query == null)
        {
            // Already disconnected
            return;
        }

        super.disconnect();
        rof = null;
        rs = null;
    }

    /**
     * Method to close the results, meaning that they are inaccessible after this point.
     */
    public synchronized void close()
    {
        super.close();
        rof = null;
        rs = null;
    }

    /**
     * Internal method to close the ResultSet.
     */
    protected void closeResults()
    {
        if (rs != null)
        {
            try
            {
                Statement stmt = null;
                try
                {
                    stmt = rs.getStatement();

                    // Close the result set
                    rs.close();
                }
                catch (SQLException e)
                {
                    NucleusLogger.DATASTORE.error(LOCALISER.msg("052605",e));
                }
                finally
                {
                    try
                    {
                        if (stmt != null)
                        {
                            // Close the original statement
                            stmt.close();
                        }
                    }
                    catch (SQLException e)
                    {
                        // Do nothing
                    }
                }
            }
            finally
            {
                rs = null;
            }
        }
    }

    /**
     * Override for compatibility with equals().
     * @return The hashcode
     */
    public int hashCode()
    {
        if (rs != null)
        {
            return rs.hashCode();
        }
        else if (query != null)
        {
            return query.hashCode();
        }
        return StringUtils.toJVMIDString(this).hashCode();
    }
}