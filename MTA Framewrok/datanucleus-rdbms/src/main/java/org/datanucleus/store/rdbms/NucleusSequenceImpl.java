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
package org.datanucleus.store.rdbms;

import java.util.Properties;

import org.datanucleus.PersistenceConfiguration;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.ExtensionMetaData;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.valuegenerator.ValueGenerationConnectionProvider;
import org.datanucleus.store.valuegenerator.ValueGenerationManager;
import org.datanucleus.transaction.TransactionUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * Basic implementation of a DataNucleus datastore sequence for RDBMS.
 * Utilises the <B>org.datanucleus.store.valuegenerator</B> classes to generate sequence values.
 */
public class NucleusSequenceImpl extends org.datanucleus.store.NucleusSequenceImpl
{
    /**
     * Constructor.
     * @param objectMgr The Object Manager managing the sequence
     * @param storeMgr Manager of the store where we obtain the sequence
     * @param seqmd MetaData defining the sequence
     */
    public NucleusSequenceImpl(ExecutionContext objectMgr, MappedStoreManager storeMgr, SequenceMetaData seqmd)
    {
        super(objectMgr, storeMgr, seqmd);
    }

    /**
     * Method to set the value generator.
     * Uses "sequence" if the datastore supports it, otherwise "increment".
     */
    public void setGenerator()
    {
        // Allocate the ValueGenerationManager for this sequence
        String valueGeneratorName = null;
        if (((RDBMSStoreManager)storeManager).getDatastoreAdapter().supportsOption(DatastoreAdapter.SEQUENCES))
        {
            valueGeneratorName = "sequence";
        }
        else
        {
            valueGeneratorName = "table-sequence";
        }

        // Create the controlling properties for this sequence
        Properties props = new Properties();
        ExtensionMetaData[] seqExtensions = seqMetaData.getExtensions();
        if (seqExtensions != null && seqExtensions.length > 0)
        {
            // Add all MetaData extension properties provided
            for (int i=0;i<seqExtensions.length;i++)
            {
                props.put(seqExtensions[i].getKey(), seqExtensions[i].getValue());
            }
        }
        props.put("sequence-name", seqMetaData.getDatastoreSequence());

        // Get a ValueGenerationManager to create the generator
        ValueGenerationManager mgr = storeManager.getValueGenerationManager();
        ValueGenerationConnectionProvider connProvider = new ValueGenerationConnectionProvider()
            {
                ManagedConnection mconn;

                public ManagedConnection retrieveConnection()
                {
                    // Obtain a new connection
                    // Note : it may be worthwhile to use the PM's connection here however where a Sequence doesnt yet
                    // exist the connection would then be effectively dead until the end of the tx
                    // The way around this would be to find a way of checking for existence of the sequence
                    PersistenceConfiguration conf = ec.getNucleusContext().getPersistenceConfiguration();
                    int isolationLevel = TransactionUtils.getTransactionIsolationLevelForName(
                        conf.getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION));
                    this.mconn = ((RDBMSStoreManager)storeManager).getConnection(isolationLevel);
                    return mconn;
                }

                public void releaseConnection()
                {
                    try
                    {
                        // Release the connection
                        mconn.close();
                    }
                    catch (NucleusException e)
                    {
                        NucleusLogger.PERSISTENCE.error(LOCALISER.msg("017007", e));
                        throw e;
                    }
                }
            };
        Class cls = null;
        ConfigurationElement elem =
            ec.getNucleusContext().getPluginManager().getConfigurationElementForExtension(
                "org.datanucleus.store_valuegenerator", 
                new String[]{"name", "datastore"}, 
                new String[] {valueGeneratorName, storeManager.getStoreManagerKey()});
        if (elem != null)
        {
            cls = ec.getNucleusContext().getPluginManager().loadClass(
                elem.getExtension().getPlugin().getSymbolicName(), elem.getAttribute("class-name"));
        }
        if (cls == null)
        {
            throw new NucleusException("Cannot create ValueGenerator for strategy "+valueGeneratorName);
        }
        generator = mgr.createValueGenerator(seqMetaData.getName(), cls, props, storeManager, connProvider);

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("017003", seqMetaData.getName(), valueGeneratorName));
        }
    }
}