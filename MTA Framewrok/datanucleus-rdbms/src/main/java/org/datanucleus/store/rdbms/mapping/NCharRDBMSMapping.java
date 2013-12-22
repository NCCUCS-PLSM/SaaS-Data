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
package org.datanucleus.store.rdbms.mapping;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.Calendar;

import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.exceptions.NullValueException;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.adapter.RDBMSAdapter;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Mapping of a NCHAR RDBMS type.
 * Copied from CharRDBMSMapping but uses setNString/getNString instead of setString/getString.
 */
public class NCharRDBMSMapping extends CharRDBMSMapping
{
    /**
     * @param storeMgr
     * @param mapping
     */
    public NCharRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }

    /**
     * @param mapping
     * @param storeMgr
     * @param field
     */
    public NCharRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
        super(mapping, storeMgr, field);
    }

    public SQLTypeInfo getTypeInfo()
    {
        if (column != null && column.getColumnMetaData().getSqlType() != null)
        {
            return storeMgr.getSQLTypeInfoForJDBCType(Types.NCHAR, column.getColumnMetaData().getSqlType());
        }
        return storeMgr.getSQLTypeInfoForJDBCType(Types.NCHAR);
    }

    /**
     * Method to set a character at the specified position in the JDBC PreparedStatement.
     * @param ps The PreparedStatement
     * @param param Parameter position
     * @param value The value to set
     */
    public void setChar(Object ps, int param, char value)
    {
        try
        {
            if (value == Character.UNASSIGNED && 
                !getDatabaseAdapter().supportsOption(RDBMSAdapter.PERSIST_OF_UNASSIGNED_CHAR))
            {
                // Some datastores (e.g Postgresql) dont allow persistence of 0x0 ("\0") so use a space
                value = ' ';
                NucleusLogger.DATASTORE.warn(LOCALISER_RDBMS.msg("055008"));
            }
            ((PreparedStatement) ps).setNString(param, Character.valueOf(value).toString());
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001", "char",
                "" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Method to extract a character from the ResultSet at the specified position
     * @param rs The Result Set
     * @param param The parameter position
     * @return the character
     */
    public char getChar(Object rs, int param)
    {
        char value;

        try
        {
            value = ((ResultSet) rs).getNString(param).charAt(0);
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","char","" + param, column, e.getMessage()), e);
        }
        return value;
    }

    /**
     * Method to set a String at the specified position in the JDBC PreparedStatement.
     * @param ps The PreparedStatement
     * @param param Parameter position
     * @param value The value to set
     */
    public void setString(Object ps, int param, String value)
    {
        try
        {
            if (value == null)
            {
                // Null string
                if (column != null && column.isDefaultable() && column.getDefaultValue() != null)
                {
                    ((PreparedStatement) ps).setNString(param,column.getDefaultValue().toString().trim());
                }
                else
                {
                    ((PreparedStatement) ps).setNull(param,getTypeInfo().getDataType());
                }
            }
            else if (value.length() == 0)
            {
                // Empty string
                if (storeMgr.getBooleanProperty("datanucleus.rdbms.persistEmptyStringAsNull"))
                {
                    // Persist as null
                    ((PreparedStatement) ps).setNString(param, null);
                }
                else
                {
                    if (getDatabaseAdapter().supportsOption(RDBMSAdapter.NULL_EQUALS_EMPTY_STRING))
                    {
                        // Datastore doesnt support empty string so use special character
                        value = getDatabaseAdapter().getSurrogateForEmptyStrings();
                    }
                    ((PreparedStatement) ps).setNString(param, value);
                }
            }
            else
            {
                if (column != null) // Column could be null if we have a query of something like an "enumClass.value"
                {
                    Integer colLength = column.getColumnMetaData().getLength();
                    if (colLength != null && colLength.intValue() < value.length())
                    {
                        // Data in field exceeds datastore column, so take required action
                        String action = storeMgr.getStringProperty("datanucleus.rdbms.stringLengthExceededAction");
                        if (action.equals("EXCEPTION"))
                        {
                            throw new NucleusUserException(LOCALISER_RDBMS.msg("055007", 
                                value, column.getIdentifier().toString(), "" + colLength.intValue())).setFatal();
                        }
                        else if (action.equals("TRUNCATE"))
                        {
                            value = value.substring(0, colLength.intValue());
                        }
                    }
                }
                ((PreparedStatement) ps).setNString(param, value);
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","String","" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Method to extract a String from the ResultSet at the specified position
     * @param rs The Result Set
     * @param param The parameter position
     * @return the String
     */
    public String getString(Object rs, int param)
    {
        try
        {
            String value = ((ResultSet) rs).getNString(param);
            if (value == null)
            {
                return value;
            }
            else if (getDatabaseAdapter().supportsOption(RDBMSAdapter.NULL_EQUALS_EMPTY_STRING) &&
                value.equals(getDatabaseAdapter().getSurrogateForEmptyStrings()))
            {
                // Special character symbolizing empty string
                return "";
            }
            else
            {
                if (column.getJdbcType() == Types.CHAR && 
                    getDatabaseAdapter().supportsOption(RDBMSAdapter.CHAR_COLUMNS_PADDED_WITH_SPACES))
                {
                    // String has likely been padded with spaces at the end by the datastore so trim trailing whitespace
                    int numPaddingChars = 0;
                    for (int i=value.length()-1;i>=0;i--)
                    {
                        if (value.charAt(i) == ' ') // Only allow for space currently
                        {
                            numPaddingChars++;
                        }
                        else
                        {
                            break;
                        }
                    }
                    if (numPaddingChars > 0)
                    {
                        value = value.substring(0, value.length()-numPaddingChars);
                    }
                }
                return value;
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","String","" + param, column, e.getMessage()), e);
        }
    }

    /**
     * Method to set a boolean at the specified position in the JDBC PreparedStatement.
     * @param ps The PreparedStatement
     * @param param Parameter position
     * @param value The value to set
     */
    public void setBoolean(Object ps, int param, boolean value)
    {
        try
        {
            ((PreparedStatement) ps).setNString(param, value ? "Y" : "N");
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","boolean","" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Method to extract a boolean from the ResultSet at the specified position
     * @param rs The Result Set
     * @param param The parameter position
     * @return the boolean
     */
    public boolean getBoolean(Object rs, int param)
    {
        boolean value;

        try
        {
            String s = ((ResultSet) rs).getNString(param);
            if (s == null)
            {
                if( column == null || column.getColumnMetaData() == null || !column.getColumnMetaData().isAllowsNull() )
                {
                    if (((ResultSet) rs).wasNull())
                    {
                        throw new NullValueException(LOCALISER_RDBMS.msg("055003",column));
                    }
                }
                return false;
            }

            if (s.equals("Y"))
            {
                value = true;
            }
            else if (s.equals("N"))
            {
                value = false;
            }
            else
            {
                throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055003",column));
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","boolean","" + param, column, e.getMessage()), e);
        }

        return value;
    }

    /**
     * Method to set an object at the specified position in the JDBC PreparedStatement.
     * @param ps The PreparedStatement
     * @param param Parameter position
     * @param value The value to set
     */
    public void setObject(Object ps, int param, Object value)
    {
        try
        {
            if (value == null)
            {
                ((PreparedStatement) ps).setNull(param, getTypeInfo().getDataType());
            }
            else
            {
                if (value instanceof Boolean)
                {
                    ((PreparedStatement) ps).setNString(param, ((Boolean) value).booleanValue() ? "Y" : "N");
                }
                else if (value instanceof java.sql.Time)
                {
                    ((PreparedStatement) ps).setNString(param, ((java.sql.Time) value).toString());
                }
                else if (value instanceof java.sql.Date)
                {
                    ((PreparedStatement) ps).setNString(param, ((java.sql.Date) value).toString());
                }
                else if (value instanceof java.util.Date)
                {
                    ((PreparedStatement) ps).setNString(param, getJavaUtilDateFormat().format((java.util.Date) value));
                }
                else if (value instanceof java.sql.Timestamp)
                {
                    Calendar cal = storeMgr.getCalendarForDateTimezone();

                    // pass the calendar to oracle makes it loses milliseconds
                    if (cal != null)
                    {
                        ((PreparedStatement) ps).setTimestamp(param, (Timestamp) value, cal);
                    }
                    else
                    {
                        ((PreparedStatement) ps).setTimestamp(param, (Timestamp) value);
                    }
                }
                else if (value instanceof String)
                {
                    ((PreparedStatement) ps).setNString(param, ((String) value));
                }
                else
                {
                    // This caters for all non-string types. If any more need specific treatment, split them out above.
                    ((PreparedStatement) ps).setNString(param, value.toString());
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055001","Object","" + value, column, e.getMessage()), e);
        }
    }

    /**
     * Method to extract an object from the ResultSet at the specified position
     * @param rs The Result Set
     * @param param The parameter position
     * @return the object
     */
    public Object getObject(Object rs, int param)
    {
        Object value;

        try
        {
            String s = ((ResultSet) rs).getNString(param);

            if (s == null)
            {
                value = null;
            }
            else
            {
                if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_BOOLEAN))
                {
                    if (s.equals("Y"))
                    {
                        value = Boolean.TRUE;
                    }
                    else if (s.equals("N"))
                    {
                        value = Boolean.FALSE;
                    }
                    else
                    {
                        throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055003",column));
                    }
                }
                else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_LANG_CHARACTER))
                {
                    value = Character.valueOf(s.charAt(0));
                }
                else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_SQL_TIME))
                {
                    value = java.sql.Time.valueOf(s);
                }
                else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_SQL_DATE))
                {
                    value = java.sql.Date.valueOf(s);
                }
                else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_UTIL_DATE))
                {
                    value = getJavaUtilDateFormat().parse(s);
                }
                else if (getJavaTypeMapping().getJavaType().getName().equals(ClassNameConstants.JAVA_SQL_TIMESTAMP))
                {
                    Calendar cal = storeMgr.getCalendarForDateTimezone();
                    value = TypeConversionHelper.stringToTimestamp(s, cal);
                }
                else
                {
                    value = s;
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object","" + param, column, e.getMessage()), e);
        }
        catch (ParseException e)
        {
            throw new NucleusDataStoreException(LOCALISER_RDBMS.msg("055002","Object","" + param, column, e.getMessage()), e);
        }

        return value;
    }
}
