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
package org.datanucleus.store.rdbms.datasource;

import java.util.Properties;

import javax.sql.DataSource;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.rdbms.datasource.DataNucleusDataSourceFactory;
import org.datanucleus.util.ClassUtils;

/**
 * Plugin for the creation of a BoneCP connection pool.
 * Note that all BoneCP classes are named explicitly in the code to avoid loading
 * them at class initialisation.
 * http://jolbox.com/
 */
public class BoneCPDataSourceFactory extends AbstractDataSourceFactory implements DataNucleusDataSourceFactory
{
    /**
     * Method to make a BoneCP DataSource for use internally in DataNucleus.
     * @param storeMgr Context
     * @return The DataSource
     * @throws Exception Thrown if an error occurs during creation
     */
    public DataSource makePooledDataSource(StoreManager storeMgr)
    {
        String dbDriver = storeMgr.getConnectionDriverName();
        String dbURL = storeMgr.getConnectionURL();
        String dbUser = storeMgr.getConnectionUserName();
        if (dbUser == null)
        {
            dbUser = ""; // Some RDBMS (e.g Postgresql) don't like null usernames
        }
        String dbPassword = storeMgr.getConnectionPassword();
        if (dbPassword == null)
        {
            dbPassword = ""; // Some RDBMS (e.g Postgresql) don't like null passwords
        }

        // Load the database driver
        ClassLoaderResolver clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);
        loadDriver(dbDriver, clr);

        // Check the existence of the necessary pooling classes
        ClassUtils.assertClassForJarExistsInClasspath(clr, "com.jolbox.bonecp.BoneCPDataSource", "bonecp.jar");

        com.jolbox.bonecp.BoneCPConfig config = new com.jolbox.bonecp.BoneCPConfig();
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        Properties dbProps = getPropertiesForDriver(storeMgr);
        config.setDriverProperties(dbProps);

        // Create the actual pool of connections
        com.jolbox.bonecp.BoneCPDataSource ds = new com.jolbox.bonecp.BoneCPDataSource(config);

        // Apply any BoneCP properties
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxStatements"))
        {
            int size = storeMgr.getIntProperty("datanucleus.connectionPool.maxStatements");
            if (size >= 0)
            {
                ds.setStatementsCacheSize(size);
            }
        }
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxPoolSize"))
        {
            int size = storeMgr.getIntProperty("datanucleus.connectionPool.maxPoolSize");
            if (size >= 0)
            {
                ds.setMaxConnectionsPerPartition(size);
            }
        }
        if (storeMgr.hasProperty("datanucleus.connectionPool.minPoolSize"))
        {
            int size = storeMgr.getIntProperty("datanucleus.connectionPool.minPoolSize");
            if (size >= 0)
            {
                ds.setMinConnectionsPerPartition(size);
            }
        }
        if (storeMgr.hasProperty("datanucleus.connectionPool.maxIdle"))
        {
            int value = storeMgr.getIntProperty("datanucleus.connectionPool.maxIdle");
            if (value > 0)
            {
                ds.setIdleMaxAgeInMinutes(value);
            }
        }

        ds.setJdbcUrl(dbURL);
        ds.setUsername(dbUser);
        ds.setPassword(dbPassword);

        return ds;
    }
}