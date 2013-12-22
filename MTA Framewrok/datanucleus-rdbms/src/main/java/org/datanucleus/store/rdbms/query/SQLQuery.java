/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved. 
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
2004 Andy Jefferson - commented and localised
2004 Andy Jefferson - changed to allow for null candidate class
2005 Andy Jefferson - changed to use JDBC parameters
2005 Andy Jefferson - added timeout support
2005 Andy Jefferson - changed to use PersistenceCapable candidate class
2005 Andy Jefferson - added checks on missing columns to compile step
2006 Andy Jefferson - implemented executeWithMap taking Integer args
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotPersistableException;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.connection.ManagedConnectionResourceListener;
import org.datanucleus.store.mapped.DatastoreAdapter;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.mapping.DatastoreMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.mapped.mapping.PersistableMapping;
import org.datanucleus.store.query.AbstractSQLQuery;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.QueryInterruptedException;
import org.datanucleus.store.query.QueryResult;
import org.datanucleus.store.query.ResultObjectFactory;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * A Query using SQL, and keeping to the JDO2 definition of a SQL query.
 * The majority of this has to be specified in the query filter itself.
 * There are no variables/imports. Ordering/grouping is explicit in the query.
 * <h3>Results</h3>
 * Results from this type of query will be :-
 * <ul>
 * <li><b>resultClass</b> - each row of the ResultSet is converted into an instance of the result class</li>
 * <li><b>candidateClass</b> - each row of the ResultSet is converted into an instance of the candidate</li>
 * <li><b>Object[]</b> - when no candidate or result class specified</li>
 * <li><b>Long</b> - when the query is an INSERT/UPDATE/DELETE/MERGE</li>
 * </ul>
 * <h3>Parameters</h3>
 * Parameters to this type of query can be :-
* <ul>
 * <li><b>Positional</b> : The SQL statement includes "?" and the parameters are positional starting at 1
 * (just like in JDBC).</li>
 * <li><b>Numbered</b> : The SQL statement includes "?1", "?2" etc (with numbers starting at 1) and the users input
 * of parameters at execute is of the numbered parameters.</li>
 * <li><b>Named</b> : The SQL statement includes ":myParam1", ":myOtherParam" etc and the users input of parameters
 * via "executeWithMap" includes values for all specified names.</li>
 * </ul>
 */
public final class SQLQuery extends AbstractSQLQuery
{
    /** Localiser of messages. */
    protected static final Localiser LOCALISER_RDBMS = Localiser.getInstance(
        "org.datanucleus.store.rdbms.Localisation", RDBMSStoreManager.class.getClassLoader());

    /** State variable for the compilation state */
    protected transient boolean isCompiled = false;

    /** Mappings for the result of the query. */
    protected transient StatementMappingIndex[] stmtMappings;

    /**
     * Constructor for a new query using the existing query.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     * @param query The existing query
     */
    public SQLQuery(StoreManager storeMgr, ExecutionContext ec, SQLQuery query)
    {
        super(storeMgr, ec, query);
    }

    /**
     * Constructs a new query instance having the same criteria as the given query.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     */
    public SQLQuery(StoreManager storeMgr, ExecutionContext ec)
    {
        super(storeMgr, ec, (String)null);
    }

    /**
     * Constructs a new query instance for the provided single-string SQL query.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     * @param queryString The SQL query string
     */
    public SQLQuery(StoreManager storeMgr, ExecutionContext ec, String queryString)
    {
        super(storeMgr, ec, queryString);
    }

    /**
     * Equality operator.
     * @param obj The object to compare against
     * @return Whether they are equal
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof SQLQuery) || !super.equals(obj))
        {
            return false;
        }

        return inputSQL.equals(((SQLQuery)obj).inputSQL);
    }

    /**
     * Utility to remove any previous compilation of this Query.
     */
    protected void discardCompiled()
    {
        isCompiled = false;
        stmtMappings = null;
        super.discardCompiled();
    }

    /**
     * Method to return if the query is compiled.
     * @return Whether it is compiled
     */
    protected boolean isCompiled()
    {
        return isCompiled;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.Query#processesRangeInDatastoreQuery()
     */
    @Override
    public boolean processesRangeInDatastoreQuery()
    {
        return true;
    }

    /**
     * Verify the elements of the query and provide a hint to the query to prepare and optimize an 
     * execution plan.
     */
    public void compileInternal(Map parameterValues)
    {
        if (isCompiled)
        {
            return;
        }

        compiledSQL = generateQueryStatement();
        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            NucleusLogger.QUERY.debug(LOCALISER.msg("059012", compiledSQL));
        }

        isCompiled = true;
    }

    /**
     * Execute the query and return the result.
     * For a SELECT query this will be the QueryResult. 
     * For an UPDATE/DELETE it will be the row count for the update statement.
     * @param parameters the Map containing all of the parameters (positional parameters) (not null)
     * @return the result of the query
     */
    protected Object performExecute(Map parameters)
    {
        if (parameters.size() != (parameterNames != null ? parameterNames.length : 0))
        {
            throw new NucleusUserException(LOCALISER_RDBMS.msg("059019", 
                "" + parameterNames.length,"" + parameters.size()));
        }

        if (type == BULK_DELETE || type == BULK_UPDATE)
        {
            // Update/Delete statement (INSERT/UPDATE/DELETE/MERGE)
            try
            {
                RDBMSStoreManager storeMgr = (RDBMSStoreManager)getStoreManager();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                SQLController sqlControl = storeMgr.getSQLController();

                try
                {
                    PreparedStatement ps = sqlControl.getStatementForUpdate(mconn, compiledSQL, false);
                    try
                    {
                        // Set the values of any parameters
                        for (int i=0;i<parameters.size();i++)
                        {
                            Object obj = parameters.get(Integer.valueOf(i+1));
                            ps.setObject((i+1), obj);
                        }

                        // Execute the update statement
                        int[] rcs = sqlControl.executeStatementUpdate(ec, mconn, compiledSQL, ps, true);
                        return Long.valueOf(rcs[0]); // Return a single Long with the number of records updated
                    }
                    finally
                    {
                        sqlControl.closeStatement(mconn, ps);
                    }
                }
                finally
                {
                    mconn.release();
                }
            }
            catch (SQLException e)
            {
                throw new NucleusDataStoreException(LOCALISER.msg("059025", compiledSQL), e);
            }
        }
        else
        {
            // Query statement (SELECT, stored-procedure)
            QueryResult qr = null;
            try
            {
                RDBMSStoreManager storeMgr = (RDBMSStoreManager)getStoreManager();
                ManagedConnection mconn = storeMgr.getConnection(ec);
                SQLController sqlControl = storeMgr.getSQLController();

                try
                {
                    PreparedStatement ps = RDBMSQueryUtils.getPreparedStatementForQuery(mconn, compiledSQL, this);
                    try
                    {
                        // Set the values of any parameters
                        for (int i=0;i<parameters.size();i++)
                        {
                            Object obj = parameters.get(Integer.valueOf(i+1));
                            ps.setObject((i+1), obj);
                        }

                        // Apply any user-specified constraints over timeouts and ResultSet
                        RDBMSQueryUtils.prepareStatementForExecution(ps, this, false);

                        ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, compiledSQL, ps);
                        try
                        {
                            // Generate a ResultObjectFactory
                            ResultObjectFactory rof = null;
                            if (resultMetaData != null)
                            {
                                // Each row of the ResultSet is defined by MetaData
                                rof = new ResultMetaDataROF(storeMgr, resultMetaData);
                            }
                            else if (resultClass != null || candidateClass == null)
                            {
                                // Each row of the ResultSet is either an instance of resultClass, or Object[]
                                rof = RDBMSQueryUtils.getResultObjectFactoryForNoCandidateClass(storeMgr, rs, resultClass);
                            }
                            else
                            {
                                // Each row of the ResultSet is an instance of the candidate class
                                rof = getResultObjectFactoryForCandidateClass(rs);
                            }

                            // Return the associated type of results depending on whether scrollable or not
                            String resultSetType = RDBMSQueryUtils.getResultSetTypeForQuery(this);
                            if (resultSetType.equals("scroll-insensitive") ||
                                resultSetType.equals("scroll-sensitive"))
                            {
                                qr = new ScrollableQueryResult(this, rof, rs, null);
                            }
                            else
                            {
                                qr = new ForwardQueryResult(this, rof, rs, null);
                            }

                            final QueryResult qr1 = qr;
                            final ManagedConnection mconn1 = mconn;
                            mconn.addListener(new ManagedConnectionResourceListener()
                            {
                                public void transactionFlushed(){}
                                public void transactionPreClose(){}
                                {
                                    // Disconnect the query from this ManagedConnection (read in unread rows etc)
                                    qr1.disconnect();                        
                                }
                                public void managedConnectionPreClose()
                                {
                                    if (!ec.getTransaction().isActive())
                                    {
                                        // Disconnect the query from this ManagedConnection (read in unread rows etc)
                                        qr1.disconnect();
                                    }
                                }
                                public void managedConnectionPostClose(){}
                                public void resourcePostClose()
                                {
                                    mconn1.removeListener(this);
                                }
                            });                            
                        }
                        finally
                        {
                            if (qr == null)
                            {
                                rs.close();
                            }
                        }
                    }
                    catch (QueryInterruptedException qie)
                    {
                        // Execution was cancelled so cancel the PreparedStatement
                        ps.cancel();
                        throw qie;
                    }
                    finally
                    {
                        if (qr == null)
                        {
                            sqlControl.closeStatement(mconn, ps);
                        }
                    }
                }
                finally
                {
                    mconn.release();
                }
            }
            catch (SQLException e)
            {
                throw new NucleusDataStoreException(LOCALISER.msg("059025", compiledSQL), e);
            }
            return qr;
        }
    }

    /**
     * Method that will throw an {@link UnsupportedOperationException} if the query implementation doesn't
     * support cancelling queries.
     */
    protected void assertSupportsCancel()
    {
        // We support cancel via JDBC PreparedStatement.cancel();
    }

    /**
     * Method to generate a ResultObjectFactory for converting rows of the provided ResultSet into
     * instances of the candidate class. Populates stmtMappings.
     * @param rs The ResultSet
     * @return The ResultObjectFactory
     * @throws SQLException Thrown if an error occurs processing the ResultSet
     */
    protected ResultObjectFactory getResultObjectFactoryForCandidateClass(ResultSet rs)
    throws SQLException
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        MappedStoreManager storeMgr = (MappedStoreManager)getStoreManager();
        DatastoreAdapter dba = storeMgr.getDatastoreAdapter();

        // Create an index listing for ALL (fetchable) fields in the result class.
        final AbstractClassMetaData candidateCmd = ec.getMetaDataManager().getMetaDataForClass(candidateClass, clr);
        int fieldCount = candidateCmd.getNoOfManagedMembers() + candidateCmd.getNoOfInheritedManagedMembers();
        Map columnFieldNumberMap = new HashMap(); // Map of field numbers keyed by the column name
        stmtMappings = new StatementMappingIndex[fieldCount];
        DatastoreClass tbl = storeMgr.getDatastoreClass(candidateClass.getName(), clr);
        for (int fieldNumber = 0; fieldNumber < fieldCount; ++fieldNumber)
        {
            AbstractMemberMetaData fmd = candidateCmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            String fieldName = fmd.getName();
            Class fieldType = fmd.getType();

            JavaTypeMapping m = null;
            if (fmd.getPersistenceModifier() != FieldPersistenceModifier.NONE)
            {
                if (tbl != null)
                {
                    // Get the field mapping from the candidate table
                    m = tbl.getMemberMapping(fmd);
                }
                else
                {
                    // Fall back to generating a mapping for this type - does this ever happen?
                    m = storeMgr.getMappingManager().getMappingWithDatastoreMapping(
                        fieldType, false, false, clr);
                }
                if (m.includeInFetchStatement())
                {
                    // Set mapping for this field since it can potentially be returned from a fetch
                    String columnName = null;
                    if (fmd.getColumnMetaData() != null && fmd.getColumnMetaData().length > 0)
                    {
                        for (int colNum = 0;colNum<fmd.getColumnMetaData().length;colNum++)
                        {
                            columnName = fmd.getColumnMetaData()[colNum].getName();
                            columnFieldNumberMap.put(columnName, Integer.valueOf(fieldNumber));
                        }
                    }
                    else
                    {
                        columnName = storeMgr.getIdentifierFactory().newDatastoreFieldIdentifier(
                            fieldName, ec.getNucleusContext().getTypeManager().isDefaultEmbeddedType(fieldType), 
                            FieldRole.ROLE_NONE).getIdentifierName();
                        columnFieldNumberMap.put(columnName, Integer.valueOf(fieldNumber));
                    }
                }
                else
                {
                    // Don't put anything in this position (field has no column in the result set)
                }
            }
            else
            {
                // Don't put anything in this position (field has no column in the result set)
            }
            stmtMappings[fieldNumber] = new StatementMappingIndex(m);
        }
        if (columnFieldNumberMap.size() == 0)
        {
            // None of the fields in the class have columns in the datastore table!
            throw new NucleusUserException(LOCALISER.msg("059030", candidateClass.getName())).setFatal();
        }

        // Generate id column field information for later checking the id is present
        DatastoreClass table = storeMgr.getDatastoreClass(candidateClass.getName(), clr);
        PersistableMapping idMapping = (PersistableMapping)table.getIdMapping();
        String[] idColNames = new String[idMapping.getNumberOfDatastoreMappings()];
        for (int i=0;i<idMapping.getNumberOfDatastoreMappings();i++)
        {
            DatastoreMapping m = idMapping.getDatastoreMapping(i);
            idColNames[i] = m.getDatastoreField().getIdentifier().toString();
        }

        // Generate discriminator/version information for later checking they are present
        String versionColName = table.getVersionMapping(false) != null ?
                table.getVersionMapping(false).getDatastoreMapping(0).getDatastoreField().getIdentifier().toString() : null;

        // Go through the fields of the ResultSet and map to the required fields in the candidate
        // Note that we check the existence of the columns again here even though they were checked at compilation
        // TODO This could be removed from here since its now done at compile time
        ResultSetMetaData rsmd = rs.getMetaData();
        HashSet remainingColumnNames = new HashSet(columnFieldNumberMap.size());
        int colCount = rsmd.getColumnCount();
        int[] datastoreIndex = null;
        int[] versionIndex = null;

        int[] matchedFieldNumbers = new int[colCount];
        int fieldNumberPosition = 0;
        for (int colNum=1; colNum<=colCount; ++colNum)
        {
            String colName = rsmd.getColumnName(colNum);

            // Find the field for this column
            int fieldNumber = -1;
            Integer fieldNum = (Integer)columnFieldNumberMap.get(colName);
            if (fieldNum == null)
            {
                // Try column name in lowercase
                fieldNum = (Integer)columnFieldNumberMap.get(colName.toLowerCase());
                if (fieldNum == null)
                {
                    // Try column name in UPPERCASE
                    fieldNum = (Integer)columnFieldNumberMap.get(colName.toUpperCase());
                }
            }

            if (fieldNum != null)
            {
                fieldNumber = fieldNum.intValue();
            }
            if (fieldNumber >= 0)
            {
                int[] exprIndices = null;
                if (stmtMappings[fieldNumber].getColumnPositions() != null)
                {
                    exprIndices = new int[stmtMappings[fieldNumber].getColumnPositions().length+1];
                    for (int i=0;i<stmtMappings[fieldNumber].getColumnPositions().length;i++)
                    {
                        exprIndices[i] = stmtMappings[fieldNumber].getColumnPositions()[i];
                    }
                    exprIndices[exprIndices.length-1] = colNum;
                }
                else
                {
                    exprIndices = new int[] {colNum};
                }
                stmtMappings[fieldNumber].setColumnPositions(exprIndices);
                remainingColumnNames.remove(colName);
                matchedFieldNumbers[fieldNumberPosition++] = fieldNumber;
            }

            if (versionColName != null && colName.equals(versionColName))
            {
                // Identify the location of the version column
                versionIndex = new int[1];
                versionIndex[0] = colNum;
            }

            if (candidateCmd.getIdentityType() == IdentityType.DATASTORE)
            {
                // Check for existence of id column, allowing for any RDBMS using quoted identifiers
                if (columnNamesAreTheSame(dba, idColNames[0], colName))
                {
                    datastoreIndex = new int[1];
                    datastoreIndex[0] = colNum;
                }
            }
        }

        // Set the field numbers found to match what we really have
        int[] fieldNumbers = new int[fieldNumberPosition];
        for (int i=0;i<fieldNumberPosition;i++)
        {
            fieldNumbers[i] = matchedFieldNumbers[i];
        }

        StatementClassMapping mappingDefinition = new StatementClassMapping();
        for (int i=0;i<fieldNumbers.length;i++)
        {
            mappingDefinition.addMappingForMember(fieldNumbers[i], stmtMappings[fieldNumbers[i]]);
        }
        if (datastoreIndex != null)
        {
            StatementMappingIndex datastoreMappingIdx = new StatementMappingIndex(table.getDatastoreObjectIdMapping());
            datastoreMappingIdx.setColumnPositions(datastoreIndex);
            mappingDefinition.addMappingForMember(StatementClassMapping.MEMBER_DATASTORE_ID, datastoreMappingIdx);
        }
        if (versionIndex != null)
        {
            StatementMappingIndex versionMappingIdx = new StatementMappingIndex(table.getVersionMapping(true));
            versionMappingIdx.setColumnPositions(versionIndex);
            mappingDefinition.addMappingForMember(StatementClassMapping.MEMBER_VERSION, versionMappingIdx);
        }

        return storeMgr.newResultObjectFactory(candidateCmd, mappingDefinition,
            ignoreCache, getFetchPlan(), getCandidateClass());
    }

    /**
     * Convenience method to compare two column names.
     * Allows for case sensitivity issues, and for database added quoting.
     * @param dba Datastore adapter
     * @param name1 The first name (from the datastore)
     * @param name2 The second name (from the user SQL statement)
     * @return Whether they are the same
     */
    public static boolean columnNamesAreTheSame(DatastoreAdapter dba, String name1, String name2)
    {
        if (name1.equalsIgnoreCase(name2) ||
            name1.equalsIgnoreCase(dba.getIdentifierQuoteString() + name2 + dba.getIdentifierQuoteString()))
        {
            return true;
        }
        return false;
    }


    /**
     * Method to perform any necessary pre-processing on the users query statement
     * before we execute it. SQL queries are not modified in any way, as per JDO2 spec [14.7].
     * @return The compiled SQL statement
     */
    protected String generateQueryStatement()
    {
        // We're returning the users SQL direct with no substitution of params etc
        String compiledSQL = getInputSQL();

        if (candidateClass != null && getType() == Query.SELECT)
        {
            // Perform any sanity checking of input for SELECT queries
            MappedStoreManager storeMgr = (MappedStoreManager)getStoreManager();
            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(candidateClass, clr);
            if (cmd == null)
            {
                throw new ClassNotPersistableException(candidateClass.getName());
            }
            if (cmd.getPersistenceCapableSuperclass() != null)
            {
               // throw new PersistentSuperclassNotAllowedException(candidateClass.getName());
            }

            if (getResultClass() == null)
            {
                // Check the presence of the required columns (id, version, discriminator) in the candidate class
                String selections = stripComments(compiledSQL.trim()).substring(7); // Skip "SELECT "
                int fromStart = selections.indexOf("FROM");
                if (fromStart == -1)
                {
                    fromStart = selections.indexOf("from");
                }
                selections = selections.substring(0, fromStart).trim();
                String[] selectedColumns = StringUtils.split(selections, ",");

                if (selectedColumns == null || selectedColumns.length == 0)
                {
                    throw new NucleusUserException(LOCALISER_RDBMS.msg("059003", compiledSQL));
                }
                if (selectedColumns.length == 1 && selectedColumns[0].trim().equals("*"))
                {
                    // SQL Query using * so just return since all possible columns will be selected
                    return compiledSQL;
                }

                // Generate id column field information for later checking the id is present
                DatastoreClass table = storeMgr.getDatastoreClass(candidateClass.getName(), clr);
                PersistableMapping idMapping = (PersistableMapping)table.getIdMapping();
                String[] idColNames = new String[idMapping.getNumberOfDatastoreMappings()];
                boolean[] idColMissing = new boolean[idMapping.getNumberOfDatastoreMappings()];
                for (int i=0;i<idMapping.getNumberOfDatastoreMappings();i++)
                {
                    DatastoreMapping m = idMapping.getDatastoreMapping(i);
                    idColNames[i] = m.getDatastoreField().getIdentifier().toString();
                    idColMissing[i] = true;
                }

                // Generate discriminator/version information for later checking they are present
                String discriminatorColName = table.getDiscriminatorMapping(false) != null ? 
                        table.getDiscriminatorMapping(false).getDatastoreMapping(0).getDatastoreField().getIdentifier().toString() : null;
                String versionColName = table.getVersionMapping(false) != null ? 
                        table.getVersionMapping(false).getDatastoreMapping(0).getDatastoreField().getIdentifier().toString() : null;
                boolean discrimMissing = (discriminatorColName != null);
                boolean versionMissing = true;
                if (versionColName == null)
                {
                    versionMissing = false;
                }

                // Go through the selected fields and check the existence of id, version, discriminator cols
                DatastoreAdapter dba = storeMgr.getDatastoreAdapter();
                final AbstractClassMetaData candidateCmd = ec.getMetaDataManager().getMetaDataForClass(candidateClass, clr);
                for (int i = 0; i < selectedColumns.length; i++)
                {
                    String colName = selectedColumns[i].trim();
                    if (colName.indexOf(" AS ") > 0)
                    {
                        // Allow for user specification of "XX.YY AS ZZ"
                        colName = colName.substring(colName.indexOf(" AS ")+4).trim();
                    }
                    else if (colName.indexOf(" as ") > 0)
                    {
                        // Allow for user specification of "XX.YY as ZZ"
                        colName = colName.substring(colName.indexOf(" as ")+4).trim();
                    }

                    if (candidateCmd.getIdentityType() == IdentityType.DATASTORE)
                    {
                        // Check for existence of id column, allowing for any RDBMS using quoted identifiers
                        if (SQLQuery.columnNamesAreTheSame(dba, idColNames[0], colName))
                        {
                            idColMissing[0] = false;
                        }
                    }
                    else if (candidateCmd.getIdentityType() == IdentityType.APPLICATION)
                    {
                        for (int j=0; j<idColNames.length; j++)
                        {
                            // Check for existence of id column, allowing for any RDBMS using quoted identifiers
                            if (SQLQuery.columnNamesAreTheSame(dba, idColNames[j], colName))
                            {
                                idColMissing[j] = false;
                            }
                        }
                    }

                    if (discrimMissing && SQLQuery.columnNamesAreTheSame(dba, discriminatorColName, colName))
                    {
                        discrimMissing = false;
                    }
                    else if (versionMissing && SQLQuery.columnNamesAreTheSame(dba, versionColName, colName))
                    {
                        versionMissing = false;
                    }
                }

                if (discrimMissing)
                {
                    throw new NucleusUserException(LOCALISER_RDBMS.msg("059014", 
                        compiledSQL, candidateClass.getName(), discriminatorColName));
                }
                if (versionMissing)
                {
                    throw new NucleusUserException(LOCALISER_RDBMS.msg("059015", 
                        compiledSQL, candidateClass.getName(), versionColName));
                }
                for (int i = 0; i < idColMissing.length; i++)
                {
                    if (idColMissing[i])
                    {
                        throw new NucleusUserException(LOCALISER_RDBMS.msg("059013", 
                            compiledSQL, candidateClass.getName(), idColNames[i]));
                    }
                }
            }
        }
        return compiledSQL;
    }
    
    /**
     * Strips any slash-star comments from the given sql string.
     * @param sql sql string
     * @return sql without comments
     */
    private String stripComments(String sql)
    {
      return sql.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
    }
}