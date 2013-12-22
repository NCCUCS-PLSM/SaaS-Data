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
2004 Andy Jefferson - localised messages
2005 Andrew Hoffman - changed mapping parent class
2006 Andy Jefferson - add convenience method for updating CLOB
    ...
**********************************************************************/
package org.datanucleus.store.rdbms.mapping.oracle;

import java.io.IOException;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.mapped.DatastoreClass;
import org.datanucleus.store.mapped.DatastoreContainerObject;
import org.datanucleus.store.mapped.DatastoreField;
import org.datanucleus.store.mapped.MappedStoreManager;
import org.datanucleus.store.mapped.StatementClassMapping;
import org.datanucleus.store.mapped.StatementMappingIndex;
import org.datanucleus.store.mapped.exceptions.DatastoreFieldDefinitionException;
import org.datanucleus.store.mapped.mapping.DatastoreMapping;
import org.datanucleus.store.mapped.mapping.JavaTypeMapping;
import org.datanucleus.store.rdbms.RDBMSStoreManager;
import org.datanucleus.store.rdbms.SQLController;
import org.datanucleus.store.rdbms.adapter.DatabaseAdapter;
import org.datanucleus.store.rdbms.adapter.OracleAdapter;
import org.datanucleus.store.rdbms.mapping.ClobRDBMSMapping;
import org.datanucleus.store.rdbms.schema.SQLTypeInfo;
import org.datanucleus.store.rdbms.sql.SQLStatement;
import org.datanucleus.store.rdbms.sql.SQLStatementHelper;
import org.datanucleus.store.rdbms.sql.SQLTable;
import org.datanucleus.store.rdbms.sql.expression.SQLExpression;
import org.datanucleus.store.rdbms.sql.expression.SQLExpressionFactory;
import org.datanucleus.store.rdbms.table.Column;

/**
 * Mapping for Oracle CLOB type.
 */
public class OracleClobRDBMSMapping extends ClobRDBMSMapping
{
    /**
     * Constructor
     * @param storeMgr Store Manager
     * @param mapping The Java type mapping
     */
    protected OracleClobRDBMSMapping(MappedStoreManager storeMgr, JavaTypeMapping mapping)
    {
        super(storeMgr, mapping);
    }
    
    /**
     * Constructor.
     * @param mapping The java type mapping for the field.
     * @param storeMgr Manager for the store
     * @param field The field
     */
    public OracleClobRDBMSMapping(JavaTypeMapping mapping, MappedStoreManager storeMgr, DatastoreField field)
    {
		super(storeMgr, mapping);
		column = (Column) field;
		initialize();
	}

    private void initialize()
    {
		initTypeInfo();
        if (column != null && !column.isUnlimitedLength())
        {
            throw new DatastoreFieldDefinitionException("Invalid length specified for CLOB column " + column + ", must be 'unlimited'");
        }
    }

    public SQLTypeInfo getTypeInfo()
    {
        return storeMgr.getSQLTypeInfoForJDBCType(Types.CLOB);
    }

    public String getInsertionInputParameter()
    {
        return "EMPTY_CLOB()";
    }
    
    public boolean includeInFetchStatement()
    {
        return true;
    }

    public String getUpdateInputParameter()
    {
        return "EMPTY_CLOB()";
    }

    /**
     * Accessor for whether this mapping requires values inserting on an INSERT.
     * @return Whether values are to be inserted into this mapping on an INSERT
     */
    public boolean insertValuesOnInsert()
    {
        // We will just insert "EMPTY_CLOB()" above so dont put value in
        return false;
    }

    public String getString(Object rs, int param)
    {
        String value = null;

        try
        {
            char[] cbuf = null;
            java.sql.Clob clob = ((ResultSet) rs).getClob(param);

            if (clob != null)
            {
                // Note: Using clob.stringValue() results in StoreManagerTest
                // exception: "java.sql.SQLException: Conversion to String failed"

                StringBuffer sbuf = new StringBuffer();
                Reader reader = clob.getCharacterStream();
                try
                {
                    final int BUFF_SIZE = 4096;
                    cbuf = new char[BUFF_SIZE];
                    int charsRead = reader.read(cbuf);

                    while (-1 != charsRead)
                    {
                        sbuf.append(cbuf, 0, charsRead);

                        java.util.Arrays.fill(cbuf, (char)0);
                        charsRead = reader.read(cbuf);
                    }
                }
                catch (IOException e)
                {
                    throw new NucleusDataStoreException("Error reading Oracle CLOB object: param = " + param, e);
                }
                finally
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        throw new NucleusDataStoreException("Error reading Oracle CLOB object: param = " + param, e);
                    }
                }

                value = sbuf.toString();

                if (value.length() == 0)
                {
                    value = null;
                }
                else if (value.equals(getDatabaseAdapter().getSurrogateForEmptyStrings()))
                {
                    value = "";
                }
            }
        }
        catch (SQLException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("055001","String", "" + param), e);
        }

        return value;
    }

    public Object getObject(Object rs, int param)
    {
        return getString(rs, param);
    }
    /**
     * Convenience method to update the contents of a CLOB column.
     * Oracle requires that a CLOB is initialised with EMPTY_CLOB() and then you retrieve
     * the column and update its CLOB value. Performs a statement
     * <pre>
     * SELECT {clobColumn} FROM TABLE WHERE ID=? FOR UPDATE
     * </pre>
     * and then updates the Clob value returned.
     * @param sm StateManager of the object
     * @param datastoreContainer Table storing the CLOB column
     * @param mapping Datastore mapping for the CLOB column
     * @param value The value to store in the CLOB
     * @throws NucleusObjectNotFoundException
     * @throws NucleusDataStoreException
     */
    @SuppressWarnings("deprecation")
    public static void updateClobColumn(ObjectProvider sm, DatastoreContainerObject datastoreContainer,
            DatastoreMapping mapping, String value)
    {
        ExecutionContext ec = sm.getExecutionContext();
        RDBMSStoreManager storeMgr = (RDBMSStoreManager)datastoreContainer.getStoreManager();
        DatastoreClass classTable = (DatastoreClass)datastoreContainer; // Don't support join tables yet
        SQLExpressionFactory exprFactory = storeMgr.getSQLExpressionFactory();

        // Generate "SELECT {clobColumn} FROM TABLE WHERE ID=? FOR UPDATE" statement
        SQLStatement sqlStmt = new SQLStatement(storeMgr, datastoreContainer, null, null);
        sqlStmt.setClassLoaderResolver(ec.getClassLoaderResolver());
        sqlStmt.addExtension("lock-for-update", true);
        SQLTable blobSqlTbl = SQLStatementHelper.getSQLTableForMappingOfTable(sqlStmt, sqlStmt.getPrimaryTable(), mapping.getJavaTypeMapping());
        sqlStmt.select(blobSqlTbl, mapping.getDatastoreField(), null);
        StatementClassMapping mappingDefinition = new StatementClassMapping();
        AbstractClassMetaData cmd = sm.getClassMetaData();
        int inputParamNum = 1;
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            // Datastore identity value for input
            JavaTypeMapping datastoreIdMapping = classTable.getDatastoreObjectIdMapping();
            SQLExpression expr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(), 
                datastoreIdMapping);
            SQLExpression val = exprFactory.newLiteralParameter(sqlStmt, datastoreIdMapping, null, "ID");
            sqlStmt.whereAnd(expr.eq(val), true);

            StatementMappingIndex datastoreIdx = mappingDefinition.getMappingForMemberPosition(StatementClassMapping.MEMBER_DATASTORE_ID);
            if (datastoreIdx == null)
            {
                datastoreIdx = new StatementMappingIndex(datastoreIdMapping);
                mappingDefinition.addMappingForMember(StatementClassMapping.MEMBER_DATASTORE_ID, datastoreIdx);
            }
            datastoreIdx.addParameterOccurrence(new int[] {inputParamNum});
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // Application identity value(s) for input
            int[] pkNums = cmd.getPKMemberPositions();
            for (int i=0;i<pkNums.length;i++)
            {
                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkNums[i]);
                JavaTypeMapping pkMapping = classTable.getMemberMapping(mmd);
                SQLExpression expr = exprFactory.newExpression(sqlStmt, sqlStmt.getPrimaryTable(),
                    pkMapping);
                SQLExpression val = exprFactory.newLiteralParameter(sqlStmt, pkMapping, null, "PK" + i);
                sqlStmt.whereAnd(expr.eq(val), true);

                StatementMappingIndex pkIdx = mappingDefinition.getMappingForMemberPosition(pkNums[i]);
                if (pkIdx == null)
                {
                    pkIdx = new StatementMappingIndex(pkMapping);
                    mappingDefinition.addMappingForMember(pkNums[i], pkIdx);
                }
                int[] inputParams = new int[pkMapping.getNumberOfDatastoreMappings()];
                for (int j=0;j<pkMapping.getNumberOfDatastoreMappings();j++)
                {
                    inputParams[j] = inputParamNum++;
                }
                pkIdx.addParameterOccurrence(inputParams);
            }
        }

        String textStmt = sqlStmt.getSelectStatement().toSQL();

        if (sm.isEmbedded())
        {
            // This mapping is embedded, so navigate back to the real owner since that is the "id" in the table
            ObjectProvider[] embeddedOwners = sm.getEmbeddedOwners();
            if (embeddedOwners != null)
            {
                // Just use the first owner
                // TODO Should check if the owner is stored in this table
                sm = embeddedOwners[0];
            }
        }

        try
        {
            ManagedConnection mconn = storeMgr.getConnection(ec);
            SQLController sqlControl = storeMgr.getSQLController();

            try
            {
                PreparedStatement ps = sqlControl.getStatementForQuery(mconn, textStmt);

                try
                {
                    // Provide the primary key field(s) to the JDBC statement
                    if (cmd.getIdentityType() == IdentityType.DATASTORE)
                    {
                        StatementMappingIndex datastoreIdx = mappingDefinition.getMappingForMemberPosition(
                            StatementClassMapping.MEMBER_DATASTORE_ID);
                        for (int i=0;i<datastoreIdx.getNumberOfParameterOccurrences();i++)
                        {
                            classTable.getDatastoreObjectIdMapping().setObject(ec, ps,
                                datastoreIdx.getParameterPositionsForOccurrence(i), sm.getInternalObjectId());
                        }
                    }
                    else if (cmd.getIdentityType() == IdentityType.APPLICATION)
                    {
                        sm.provideFields(cmd.getPKMemberPositions(),
                            storeMgr.getFieldManagerForStatementGeneration(sm, ps, mappingDefinition, false));
                    }

                    ResultSet rs = sqlControl.executeStatementQuery(ec, mconn, textStmt, ps);
                    try
                    {
                        if (!rs.next())
                        {
                            throw new NucleusObjectNotFoundException("No such database row", sm.getInternalObjectId());
                        }

                        DatabaseAdapter dba = (DatabaseAdapter)storeMgr.getDatastoreAdapter();
                        int jdbcMajorVersion = dba.getDriverMajorVersion();
                        if (dba.getDatastoreDriverName().equalsIgnoreCase(OracleAdapter.OJDBC_DRIVER_NAME) && jdbcMajorVersion < 10)
                        {
                            // Oracle JDBC drivers version 9 and below use some sh*tty Oracle-specific CLOB type
                            // we have to cast to that, face west, pray whilst saying ommmmmmmmmmm
                        
                            oracle.sql.CLOB clob = (oracle.sql.CLOB)rs.getClob(1);
                            if (clob != null)
                            {
                                clob.putString(1, value); // Deprecated but what can you do
                            }
                        }
                        else
                        {
                            // Oracle JDBC drivers 10 and above supposedly use the JDBC standard class for Clobs
                            java.sql.Clob clob = rs.getClob(1);
                            if (clob != null)
                            {
                                clob.setString(1, value);
                            }
                        }
                    }
                    finally
                    {
                        rs.close();
                    }
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
            throw new NucleusDataStoreException("Update of CLOB value failed: " + textStmt, e);
        }
    }
}