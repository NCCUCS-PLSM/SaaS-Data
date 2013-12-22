/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.ReferenceMapping;
import org.datanucleus.store.mapped.mapping.SerialisedPCMapping;
import org.datanucleus.store.mapped.mapping.SerialisedReferenceMapping;
import org.datanucleus.store.query.ResultObjectFactory;

/**
 * Abstract iterator for presenting the results for an array store.
 */
public abstract class ArrayStoreIterator implements Iterator
{
    private final ExecutionContext ec;

    /** Underlying iterator that we wrap. */
    private final Iterator delegate;

    private Object lastElement = null;

    /**
     * Constructor.
     * @param sm the StateManager
     * @param rs the ResultSet
     * @param rof the Query.ResultObjectFactory
     * @throws MappedDatastoreException
     */
    public ArrayStoreIterator(ObjectProvider sm, Object rs, ResultObjectFactory rof,
            ElementContainerStore backingStore) 
    throws MappedDatastoreException
    {
        this.ec = sm.getExecutionContext();
        ArrayList results = new ArrayList();
        if (rs != null)
        {
            JavaTypeMapping elementMapping = backingStore.getElementMapping();
            while (next(rs))
            {
                Object nextElement;
                if (backingStore.isElementsAreEmbedded() || backingStore.isElementsAreSerialised())
                {
                    int param[] = new int[elementMapping.getNumberOfDatastoreMappings()];
                    for (int i = 0; i < param.length; ++i)
                    {
                        param[i] = i + 1;
                    }

                    if (elementMapping instanceof SerialisedPCMapping || elementMapping instanceof SerialisedReferenceMapping || elementMapping instanceof EmbeddedElementPCMapping)
                    {
                        // Element = Serialised
                        int ownerFieldNumber = -1;
                        if (backingStore.getContainerTable() != null)
                        {
                            ownerFieldNumber = getOwnerFieldMetaData(backingStore.getContainerTable()).getAbsoluteFieldNumber();
                        }
                        nextElement = elementMapping.getObject(ec, rs, param, sm, ownerFieldNumber);
                    }
                    else
                    {
                        // Element = Non-PC
                        nextElement = elementMapping.getObject(ec, rs, param);
                    }
                }
                else if (elementMapping instanceof ReferenceMapping)
                {
                    // Element = Reference (Interface/Object)
                    int param[] = new int[elementMapping.getNumberOfDatastoreMappings()];
                    for (int i = 0; i < param.length; ++i)
                    {
                        param[i] = i + 1;
                    }
                    nextElement = elementMapping.getObject(ec, rs, param);
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

    protected abstract AbstractMemberMetaData getOwnerFieldMetaData(DatastoreContainerObject containerTable);

    protected abstract boolean next(Object rs) throws MappedDatastoreException;

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
        // Do nothing
    }
}