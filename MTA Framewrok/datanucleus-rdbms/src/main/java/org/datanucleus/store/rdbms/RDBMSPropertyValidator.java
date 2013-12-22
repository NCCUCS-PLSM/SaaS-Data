/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.properties.PersistencePropertyValidator;

/**
 * Validator for persistence properties used by RDBMS.
 */
public class RDBMSPropertyValidator implements PersistencePropertyValidator
{
    /**
     * Validate the specified property.
     * @param name Name of the property
     * @param value Value
     * @return Whether it is valid
     */
    public boolean validate(String name, Object value)
    {
        if (name == null)
        {
            return false;
        }
        else if (name.equalsIgnoreCase("datanucleus.rdbms.query.fetchDirection"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("forward") ||
                    strVal.equalsIgnoreCase("reverse") ||
                    strVal.equalsIgnoreCase("unknown"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase("datanucleus.rdbms.query.resultSetType"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("forward-only") ||
                    strVal.equalsIgnoreCase("scroll-sensitive") ||
                    strVal.equalsIgnoreCase("scroll-insensitive"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase("datanucleus.rdbms.query.resultSetConcurrency"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("read-only") ||
                    strVal.equalsIgnoreCase("updateable"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase("datanucleus.rdbms.constraintCreateMode"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("DataNucleus") ||
                    strVal.equalsIgnoreCase("JDO2"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase("datanucleus.rdbms.stringLengthExceededAction"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("EXCEPTION") ||
                    strVal.equalsIgnoreCase("TRUNCATE"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase("datanucleus.rdbms.initializeColumnInfo"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("ALL") ||
                    strVal.equalsIgnoreCase("PK") ||
                    strVal.equalsIgnoreCase("NONE"))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
