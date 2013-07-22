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
package org.datanucleus.store.rdbms.table;

import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.exceptions.DatastoreFieldDefinitionException;
import org.datanucleus.store.rdbms.schema.RDBMSColumnInfo;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;

/**
 * Interface for a column in an RDBMS datastore.
 */
public interface Column extends DatastoreField
{
    /** wrapper function select **/
    public static final int WRAPPER_FUNCTION_SELECT = 0;

    /** wrapper function insert **/
    public static final int WRAPPER_FUNCTION_INSERT = 1;

    /** wrapper function update **/
    public static final int WRAPPER_FUNCTION_UPDATE = 2;

    /**
     * Convenience method to check if the length is required to be unlimited (BLOB/CLOB).
     * @return Whether unlimited length required.
     */
    boolean isUnlimitedLength();

    /**
     * Mutator for the type information of the column.
     * @param typeInfo The type info
     * @return The column with the updated info
     */
    Column setTypeInfo(SQLTypeInfo typeInfo);

    /**
     * Accessor for the type info for this column.
     * @return The type info
     */
    SQLTypeInfo getTypeInfo();

    /**
     * Accessor for the JDBC type being used for this Column
     * @return The JDBC data type
     */
    int getJdbcType();

    /**
     * Accessor for the SQL definition of this column.
     * @return The SQL definition of the column
     */
    String getSQLDefinition();

    /**
     * Initialize the default column value and auto increment
     * @param ci The column information
     */
    void initializeColumnInfoFromDatastore(RDBMSColumnInfo ci);

    /**
     * Method to validate the contents of the column. This method can throw
     * IncompatibleDataTypeException, WrongScaleException,
     * WrongPrecisionException, IsNullableException if the data in the column is
     * not compatible with the supplied ColumnInfo.
     * @param ci The column information taken from the database
     */
    void validate(RDBMSColumnInfo ci);

    /**
     * Mutator for the constraints of the column.
     * @param constraints The constraints
     * @return The column with the updated info
     */
    Column setConstraints(String constraints);

    /**
     * @return Returns the constraints.
     */
    String getConstraints();

    /**
     * Checks the column definition as a primitive.
     * @throws DatastoreFieldDefinitionException
     */
    void checkPrimitive() throws DatastoreFieldDefinitionException;

    /**
     * Checks the column definition as an integer.
     * @throws DatastoreFieldDefinitionException
     */
    void checkInteger() throws DatastoreFieldDefinitionException;

    /**
     * Checks the column definition as a decimal.
     * @throws DatastoreFieldDefinitionException
     */
    void checkDecimal() throws DatastoreFieldDefinitionException;

    /**
     * Checks the column definition as a string.
     * @throws DatastoreFieldDefinitionException
     */
    void checkString() throws DatastoreFieldDefinitionException;

    /**
     * Sets a function to wrap the column. 
     * The wrapper function String must use "?" to be replaced later by the column name.
     * For example <pre>SQRT(?) generates: SQRT(COLUMN)</pre>
     * @param wrapperFunction The wrapperFunction to set.
     * @param wrapperMode whether select, insert or update
     */
    void setWrapperFunction(String wrapperFunction, int wrapperMode);

    /**
     * Gets the wrapper for parameters.
     * @param wrapperMode whether select, insert or update
     * @return Returns the wrapperFunction.
     */
    String getWrapperFunction(int wrapperMode);
}