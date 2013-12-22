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
package org.datanucleus.store.rdbms.datasource;

import java.util.Properties;

import javax.sql.DataSource;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.rdbms.datasource.DatastorePoolException;
import org.datanucleus.store.rdbms.datasource.DataNucleusDataSourceFactory;

/**
 * Plugin for the creation of a DBCP connection pool.
 * Note that all Apache DBCP classes are named explicitly in the code to avoid loading
 * them at class initialisation.
 * (see http://jakarta.apache.org/commons/dbcp/)
 * Also see 
 * http://jakarta.apache.org/commons/dbcp/apidocs/org/apache/commons/dbcp/package-summary.html#package_description
 * for javadocs that give pretty much the only useful description of DBCP.
 */
public class DBCPBuiltinDataSourceFactory extends AbstractDataSourceFactory implements DataNucleusDataSourceFactory
{
    /**
     * Method to make a DBCP DataSource for use internally in DataNucleus.
     * @param storeMgr Context
     * @return The DataSource
     * @throws Exception Thrown if an error occurs during creation
     */
    public DataSource makePooledDataSource(StoreManager storeMgr)
    {
        String dbDriver = storeMgr.getConnectionDriverName();
        String dbURL = storeMgr.getConnectionURL();

        // Load the database driver
        ClassLoaderResolver clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);
        loadDriver(dbDriver, clr);

        // Create the actual pool of connections
        org.datanucleus.store.rdbms.datasource.dbcp.pool.ObjectPool connectionPool =
            new org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool(null);

        // Apply any properties
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxIdle"))
        {
            int value = storeMgr.getIntProperty("datanucleus.connectionPool.maxIdle");
            if (value > 0)
            {
                ((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setMaxIdle(value);
            }
        }
        if (storeMgr.hasProperty("datanucleus.connectionPool.minIdle"))
        {
            int value = storeMgr.getIntProperty("datanucleus.connectionPool.minIdle");
            if (value > 0)
            {
                ((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setMinIdle(value);
            }
        }
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxActive"))
        {
            int value = storeMgr.getIntProperty("datanucleus.connectionPool.maxActive");
            if (value > 0)
            {
                ((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setMaxActive(value);
            }
        }
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxWait"))
        {
            int value = storeMgr.getIntProperty("datanucleus.connectionPool.maxWait");
            if (value > 0)
            {
                ((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setMaxWait(value);
            }
        }
        // how often should the evictor run (if ever, default is -1 = off)
        if (storeMgr.hasProperty("datanucleus.connectionPool.timeBetweenEvictionRunsMillis"))
        {
        	int value = storeMgr.getIntProperty("datanucleus.connectionPool.timeBetweenEvictionRunsMillis");
        	if (value > 0)
        	{
        		((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setTimeBetweenEvictionRunsMillis(value);
        		
        		// in each eviction run, ecict at least a fourth of "maxIdle" connections
        		int maxIdle = ((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).getMaxIdle();
        		int numTestsPerEvictionRun = (int) Math.ceil(((double) maxIdle / 4));
        		((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        	}
        }
        // how long may a connection sit idle in the pool before it may be evicted
        if (storeMgr.hasProperty("datanucleus.connectionPool.minEvictableIdleTimeMillis"))
        {
        	int value = storeMgr.getIntProperty("datanucleus.connectionPool.minEvictableIdleTimeMillis");
        	if (value > 0)
        	{
        		((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setMinEvictableIdleTimeMillis(value);
        	}
        }

        // Create a factory to be used by the pool to create the connections
        Properties dbProps = getPropertiesForDriver(storeMgr);
        org.datanucleus.store.rdbms.datasource.dbcp.ConnectionFactory connectionFactory = 
            new org.datanucleus.store.rdbms.datasource.dbcp.DriverManagerConnectionFactory(dbURL, dbProps);

        // Create a factory for caching the PreparedStatements
        org.datanucleus.store.rdbms.datasource.dbcp.pool.KeyedObjectPoolFactory kpf = null;
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxStatements"))
        {
            int value = storeMgr.getIntProperty("datanucleus.connectionPool.maxStatements");
            if (value > 0)
            {
                kpf = new org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.StackKeyedObjectPoolFactory(null, value);
            }
        }

        // Wrap the connections and statements with pooled variants
        try
        {
            String testSQL = null;
            if (storeMgr.hasProperty("datanucleus.connectionPool.testSQL"))
            {
                testSQL = storeMgr.getStringProperty("datanucleus.connectionPool.testSQL");
            }
            new org.datanucleus.store.rdbms.datasource.dbcp.PoolableConnectionFactory(connectionFactory, connectionPool, kpf, 
                testSQL, false, false);
            if (testSQL != null)
            {
                ((org.datanucleus.store.rdbms.datasource.dbcp.pool.impl.GenericObjectPool)connectionPool).setTestOnBorrow(true);
            }
        }
        catch (Exception e)
        {
            throw new DatastorePoolException("DBCP", dbDriver, dbURL, e);
        }

        // Create the datasource
        DataSource ds = new org.datanucleus.store.rdbms.datasource.dbcp.PoolingDataSource(connectionPool);

        return ds;
    }
}