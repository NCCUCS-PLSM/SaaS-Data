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
package org.datanucleus.store.rdbms.sql.expression;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.mapping.CharRDBMSMapping;
import org.datanucleus.store.rdbms.sql.SQLStatement;

/**
 * Representation of temporal literal in a Query.
 * Can be used for anything based on java.util.Date.
 */
public class TemporalLiteral extends TemporalExpression implements SQLLiteral
{
    private final Date value;

    /**
     * Constructor for a temporal literal with a value.
     * @param stmt the SQL statement
     * @param mapping the mapping
     * @param value the value
     * @param parameterName Name of the parameter that this represents if any (as JDBC "?")
     */
    public TemporalLiteral(SQLStatement stmt, JavaTypeMapping mapping, Object value, String parameterName)
    {
        super(stmt, null, mapping);
        this.parameterName = parameterName;

        if (value == null)
        {
            this.value = null;
        }
        else if (value instanceof Date)
        {
            this.value = (Date)value;
        }
        else if (value instanceof Calendar)
        {
            this.value = ((Calendar)value).getTime();
        }
        else
        {
            // Allow for using an input parameter literal
            throw new NucleusException("Cannot create " + this.getClass().getName() + 
                " for value of type " + (value != null ? value.getClass().getName() : null));
        }

        if (parameterName != null)
        {
            st.appendParameter(parameterName, mapping, this.value);
        }
        else
        {
            setStatement();
        }
    }

    public String toString()
    {
        return super.toString() + " = " + value.toString();
    }

    public SQLExpression invoke(String methodName, List args)
    {
        if (parameterName == null)
        {
            if (methodName.equals("getDay"))
            {
                // Date.getDay()
                Calendar cal = Calendar.getInstance();
                cal.setTime(value);
                JavaTypeMapping m = stmt.getRDBMSManager().getMappingManager().getMapping(Integer.class);
                return new IntegerLiteral(stmt, m, Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH)), null);
            }
            else if (methodName.equals("getMonth"))
            {
                // Date.getMonth()
                Calendar cal = Calendar.getInstance();
                cal.setTime(value);
                JavaTypeMapping m = stmt.getRDBMSManager().getMappingManager().getMapping(Integer.class);
                return new IntegerLiteral(stmt, m, Integer.valueOf(cal.get(Calendar.MONTH)), null);
            }
            else if (methodName.equals("getYear"))
            {
                // Date.getMonth()
                Calendar cal = Calendar.getInstance();
                cal.setTime(value);
                JavaTypeMapping m = stmt.getRDBMSManager().getMappingManager().getMapping(Integer.class);
                return new IntegerLiteral(stmt, m, Integer.valueOf(cal.get(Calendar.YEAR)), null);
            }
            else if (methodName.equals("getHour"))
            {
                // Date.getHour()
                Calendar cal = Calendar.getInstance();
                cal.setTime(value);
                JavaTypeMapping m = stmt.getRDBMSManager().getMappingManager().getMapping(Integer.class);
                return new IntegerLiteral(stmt, m, Integer.valueOf(cal.get(Calendar.HOUR_OF_DAY)), null);
            }
            else if (methodName.equals("getMinutes"))
            {
                // Date.getMinutes()
                Calendar cal = Calendar.getInstance();
                cal.setTime(value);
                JavaTypeMapping m = stmt.getRDBMSManager().getMappingManager().getMapping(Integer.class);
                return new IntegerLiteral(stmt, m, Integer.valueOf(cal.get(Calendar.MINUTE)), null);
            }
            else if (methodName.equals("getSeconds"))
            {
                // Date.getMinutes()
                Calendar cal = Calendar.getInstance();
                cal.setTime(value);
                JavaTypeMapping m = stmt.getRDBMSManager().getMappingManager().getMapping(Integer.class);
                return new IntegerLiteral(stmt, m, Integer.valueOf(cal.get(Calendar.SECOND)), null);
            }
        }

        return super.invoke(methodName, args);
    }

    public Object getValue()
    {
        return value;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.rdbms.sql.expression.SQLLiteral#setNotParameter()
     */
    public void setNotParameter()
    {
        if (parameterName == null)
        {
            return;
        }
        parameterName = null;
        st.clearStatement();
        setStatement();
    }

    protected void setStatement()
    {
        String formatted;
        if (value instanceof java.sql.Time || value instanceof java.sql.Date || value instanceof java.sql.Timestamp)
        {
            // Use native format of the type
            formatted = value.toString();
        }
        else if (mapping.getDatastoreMapping(0) instanceof CharRDBMSMapping)
        {
            // Stored as String so use same formatting
            SimpleDateFormat fmt = ((CharRDBMSMapping)mapping.getDatastoreMapping(0)).getJavaUtilDateFormat();
            formatted = fmt.format(value);
        }
        else
        {
            // TODO Include more variations of inputting a Date into JDBC
            // TODO Cater for timezone storage options see TimestampRDBMSMapping
            formatted = new Timestamp(value.getTime()).toString();
        }
        st.append('\'').append(formatted).append('\'');
    }
}