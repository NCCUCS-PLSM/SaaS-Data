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

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.rdbms.datasource.DatastorePoolException;
import org.datanucleus.store.rdbms.datasource.DataNucleusDataSourceFactory;
import org.datanucleus.util.ClassUtils;

import com.mchange.v2.c3p0.DataSources;

/**
 * Plugin for the creation of a C3P0 connection pool.
 * Note that all C3P0 classes are named explicitly in the code to avoid loading
 * them at class initialisation.
 * See http://www.mchange.com/projects/c3p0/index.html
 * See http://www.sf.net/projects/c3p0
 */
public class C3P0DataSourceFactory extends AbstractDataSourceFactory implements DataNucleusDataSourceFactory
{
    /**
     * Method to make a C3P0 DataSource for use internally in DataNucleus.
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

        // Check the existence of the necessary pooling classes
        ClassUtils.assertClassForJarExistsInClasspath(clr, "com.mchange.v2.c3p0.DataSources", "c3p0.jar");

        try
        {
            Properties dbProps = getPropertiesForDriver(storeMgr);
            DataSource unpooled = DataSources.unpooledDataSource(dbURL, dbProps);

            // Apply any properties and make it a pooled DataSource
            // Note that C3P0 will always look for "c3p0.properties" at the root of the CLASSPATH
            Properties c3p0Props = new Properties();
            if (storeMgr.hasProperty("datanucleus.connectionPool.maxStatements"))
            {
                int size = storeMgr.getIntProperty("datanucleus.connectionPool.maxStatements");
                if (size >= 0)
                {
                    c3p0Props.setProperty("maxStatements", "" + size);
                }
            }
            if (storeMgr.hasProperty("datanucleus.connectionPool.maxPoolSize"))
            {
                int size = storeMgr.getIntProperty("datanucleus.connectionPool.maxPoolSize");
                if (size >= 0)
                {
                    c3p0Props.setProperty("maxPoolSize", "" + size);
                }
            }
            if (storeMgr.hasProperty("datanucleus.connectionPool.minPoolSize"))
            {
                int size = storeMgr.getIntProperty("datanucleus.connectionPool.minPoolSize");
                if (size >= 0)
                {
                    c3p0Props.setProperty("minPoolSize", "" + size);
                }
            }
            if (storeMgr.hasProperty("datanucleus.connectionPool.initialPoolSize"))
            {
                int size = storeMgr.getIntProperty("datanucleus.connectionPool.initialPoolSize");
                if (size >= 0)
                {
                    c3p0Props.setProperty("initialPoolSize", "" + size);
                }
            }

            return DataSources.pooledDataSource(unpooled, c3p0Props);
        }
        catch (SQLException sqle)
        {
            throw new DatastorePoolException("c3p0", dbDriver, dbURL, sqle);
        }
    }
}