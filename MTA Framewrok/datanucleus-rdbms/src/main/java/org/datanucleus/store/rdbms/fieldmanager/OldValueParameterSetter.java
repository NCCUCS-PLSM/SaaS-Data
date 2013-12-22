/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.rdbms.fieldmanager;

import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.mapped.StatementClassMapping;

/**
 * Parameter setter that uses old values when available.
 * Used as part of the nondurable update process.
 * Assumes that the old value for fields are stored by the StateManager under name "FIELD_VALUE.ORIGINAL.{fieldNum}".
 */
public class OldValueParameterSetter extends ParameterSetter
{
    /**
     * Constructor.
     * @param sm The state manager for the object.
     * @param stmt The Statement to set values on.
     * @param stmtMappings mappings for parameters in the statement.
     * @param checkNonNullable Whether to check for nullability
     */
    public OldValueParameterSetter(ObjectProvider sm, Object stmt, StatementClassMapping stmtMappings,
            boolean checkNonNullable)
    {
        super(sm, stmt, stmtMappings, checkNonNullable);
    }

    public void storeBooleanField(int fieldNumber, boolean value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeBooleanField(fieldNumber, (Boolean) oldValue);
        }
        else
        {
            super.storeBooleanField(fieldNumber, value);
        }
    }

    public void storeCharField(int fieldNumber, char value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeCharField(fieldNumber, (Character) oldValue);
        }
        else
        {
            super.storeCharField(fieldNumber, value);
        }
    }

    public void storeByteField(int fieldNumber, byte value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeByteField(fieldNumber, (Byte) oldValue);
        }
        else
        {
            super.storeByteField(fieldNumber, value);
        }
    }

    public void storeShortField(int fieldNumber, short value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeShortField(fieldNumber, (Short) oldValue);
        }
        else
        {
            super.storeShortField(fieldNumber, value);
        }
    }

    public void storeIntField(int fieldNumber, int value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeIntField(fieldNumber, (Integer) oldValue);
        }
        else
        {
            super.storeIntField(fieldNumber, value);
        }
    }

    public void storeLongField(int fieldNumber, long value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeLongField(fieldNumber, (Long) oldValue);
        }
        else
        {
            super.storeLongField(fieldNumber, value);
        }
    }

    public void storeFloatField(int fieldNumber, float value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeFloatField(fieldNumber, (Float) oldValue);
        }
        else
        {
            super.storeFloatField(fieldNumber, value);
        }
    }

    public void storeDoubleField(int fieldNumber, double value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeDoubleField(fieldNumber, (Double) oldValue);
        }
        else
        {
            super.storeDoubleField(fieldNumber, value);
        }
    }

    public void storeStringField(int fieldNumber, String value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeStringField(fieldNumber, (String) oldValue);
        }
        else
        {
            super.storeStringField(fieldNumber, value);
        }
    }

    public void storeObjectField(int fieldNumber, Object value)
    {
        Object oldValue = sm.getAssociatedValue("FIELD_VALUE.ORIGINAL." + fieldNumber);
        if (oldValue != null)
        {
            super.storeObjectField(fieldNumber, oldValue);
        }
        else
        {
            super.storeObjectField(fieldNumber, value);
        }
    }
}