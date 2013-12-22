/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
import org.datanucleus.util.ClassUtils;

/**
 * Plugin for the creation of a Proxool connection pool.
 * Note that all Proxool classes are named explicitly in the code to avoid loading
 * them at class initialisation. (see http://proxool.sourceforge.net/)
 */
public class ProxoolDataSourceFactory extends AbstractDataSourceFactory implements DataNucleusDataSourceFactory
{
    /** Number of the pool being created (using in the Proxool alias). */
    private static int poolNumber = 0;

    /**
     * Method to make a Proxool DataSource for use internally.
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

        // Check the presence of commons-logging
        ClassUtils.assertClassForJarExistsInClasspath(clr, 
            "org.apache.commons.logging.Log", "commons-logging.jar");
        ClassUtils.assertClassForJarExistsInClasspath(clr, 
            "org.logicalcobwebs.proxool.ProxoolDriver", "proxool.jar");

        // Create a Proxool pool with alias "datanucleus{poolNumber}"
        String alias = "datanucleus" + poolNumber;
        try
        {
            // Apply any properties
            Properties dbProps = getPropertiesForDriver(storeMgr);

            if (storeMgr.hasProperty("datanucleus.connectionPool.maxConnections"))
            {
                int value = storeMgr.getIntProperty("datanucleus.connectionPool.maxConnections");
                if (value > 0)
                {
                    dbProps.put("proxool.maximum-connection-count", "" + value);
                }
                else
                {
                    dbProps.put("proxool.maximum-connection-count", "10");
                }
            }
            else
            {
                dbProps.put("proxool.maximum-connection-count", "10");
            }
            if (storeMgr.hasProperty("datanucleus.connectionPool.testSQL"))
            {
                String value = storeMgr.getStringProperty("datanucleus.connectionPool.testSQL");
                dbProps.put("proxool.house-keeping-test-sql", value);
            }
            else
            {
                dbProps.put("proxool.house-keeping-test-sql", "SELECT 1");
            }

            String url = "proxool." + alias + ":" + dbDriver + ":" + dbURL;
            poolNumber++;
            org.logicalcobwebs.proxool.ProxoolFacade.registerConnectionPool(url, dbProps);
        }
        catch (org.logicalcobwebs.proxool.ProxoolException pe)
        {
            pe.printStackTrace();
            throw new DatastorePoolException("Proxool", dbDriver, dbURL, pe);
        }

        DataSource ds = new org.logicalcobwebs.proxool.ProxoolDataSource(alias);

        return ds;
    }
}