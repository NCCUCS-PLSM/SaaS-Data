/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved.
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

import org.datanucleus.store.StoreManager;

/**
 * Default DataSource implementation.
 */
public class DefaultDataSourceFactory implements DataNucleusDataSourceFactory
{
    /**
     * Method to make a DataSource for use within DataNucleus.
     * @param storeMgr The Context
     * @return The DataSource
     * @throws Exception Thrown if an error occurs during creation
     */
    public DataSource makePooledDataSource(StoreManager storeMgr)
    {
        Properties props = AbstractDataSourceFactory.getPropertiesForDriver(storeMgr);
        if (props.size() == 2)
        {
            props = null;
        }
        return new DriverManagerDataSource(
            storeMgr.getConnectionDriverName(),
            storeMgr.getConnectionURL(),
            storeMgr.getConnectionUserName(),
            storeMgr.getConnectionPassword(),
            storeMgr.getNucleusContext().getClassLoaderResolver(null), props);
    }
}