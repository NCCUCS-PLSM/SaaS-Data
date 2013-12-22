/**********************************************************************
 Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.scostore;

import java.util.ArrayList;
import java.util.Iterator;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.exceptions.MappedDatastoreException;
import org.datanucleus.store.mapped.mapping.EmbeddedElementPCMapping;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.mapped.mapping.SerialisedPCMapping;
import org.datanucleus.store.mapped.mapping.SerialisedReferenceMapping;
import org.datanucleus.store.query.ResultObjectFactory;

/**
 * Class representing an iterator of the Set.
 **/
public abstract class SetStoreIterator implements Iterator
{
    private final AbstractSetStore abstractSetStore;
    private final ObjectProvider sm;
    private final ExecutionContext ec;
    private final Iterator delegate;
    private Object lastElement = null;

    /**
     * Constructor
     * @param sm the StateManager
     * @param rs the ResultSet
     * @param rof the Query.ResultObjectFactory
     * @param abstractSetStore the abstract set store
     * @throws MappedDatastoreException
     */
    public SetStoreIterator(ObjectProvider sm, Object rs, ResultObjectFactory rof, AbstractSetStore abstractSetStore)
    throws MappedDatastoreException
    {
        this.sm = sm;
        this.ec = sm.getExecutionContext();
        this.abstractSetStore = abstractSetStore;
        ArrayList results = new ArrayList();
        if (rs != null)
        {
            while (next(rs))
            {
                Object nextElement;
                if (abstractSetStore.elementsAreEmbedded || abstractSetStore.elementsAreSerialised)
                {
                    int param[] = new int[abstractSetStore.elementMapping.getNumberOfDatastoreMappings()];
                    for (int i = 0; i < param.length; ++i)
                    {
                        param[i] = i + 1;
                    }

                    if (abstractSetStore.elementMapping instanceof SerialisedPCMapping ||
                        abstractSetStore.elementMapping instanceof SerialisedReferenceMapping ||
                        abstractSetStore.elementMapping instanceof EmbeddedElementPCMapping)
                    {
                        // Element = Serialised
                        int ownerFieldNumber = -1;
                        if (abstractSetStore.containerTable != null)
                        {
                            ownerFieldNumber = getOwnerMemberMetaData(abstractSetStore.containerTable).getAbsoluteFieldNumber();
                        }
                        nextElement = abstractSetStore.elementMapping.getObject(ec, rs, param, sm, ownerFieldNumber);
                    }
                    else
                    {
                        // Element = Non-PC
                        nextElement = abstractSetStore.elementMapping.getObject(ec, rs, param);
                    }
                }
                else if (abstractSetStore.elementMapping instanceof ReferenceMapping)
                {
                    // Element = Reference (Interface/Object)
                    int param[] = new int[abstractSetStore.elementMapping.getNumberOfDatastoreMappings()];
                    for (int i = 0; i < param.length; ++i)
                    {
                        param[i] = i + 1;
                    }
                    nextElement = abstractSetStore.elementMapping.getObject(ec, rs, param);
                }
                else
                {
                    // Element = PC
                    nextElement = rof.getObject(ec, rs);
                }

                results.add(nextElement);
            }
        }
        delegate = results.iterator();
    }

    public boolean hasNext()
    {
        return delegate.hasNext();
    }

    public Object next()
    {
        lastElement = delegate.next();

        return lastElement;
    }

    public synchronized void remove()
    {
        if (lastElement == null)
        {
            throw new IllegalStateException("No entry to remove");
        }

        abstractSetStore.remove(sm, lastElement, -1, true);
        delegate.remove();

        lastElement = null;
    }

    protected abstract boolean next(Object rs) throws MappedDatastoreException;

    protected abstract AbstractMemberMetaData getOwnerMemberMetaData(DatastoreContainerObject containerTable);
}