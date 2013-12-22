package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import rewriting.AlterRewriting;
import rewriting.CreateRewriting;
import rewriting.CreateTenantRewriting;
import rewriting.DeleteRewriting;
import rewriting.InsertRewriting;
import rewriting.SelectRewriting;
import rewriting.UpdateRewriting;

import com.mysql.jdbc.Statement.CancelTask;
import com.mysql.jdbc.exceptions.MySQLTimeoutException;

public class Multitenantstatement extends Statement {

	public Multitenantstatement(Connection c, String catalog) throws SQLException {
		super(c, catalog);
		// TODO Auto-generated constructor stub
	}

	public String tenantId = "";
	//public String tenantId = "";
	public int Count = 0;


	public void settenantId (String tenantId) {
		//System.out.println("-------"+tenantId);
		this.tenantId = tenantId;

	}

	public String gettenantId () {
		return this.tenantId;

	}

	@Override
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}



	protected int executeUpdate(String sql, boolean isBatch)
			throws SQLException {
        int count = 0;
        List sqlList = new ArrayList();
		if(sql.toUpperCase().startsWith("ALTER"))
		{
			//System.out.println("2313");

			AlterRewriting Alterstm;
			Alterstm = new AlterRewriting(sql,this.tenantId);
			count = Alterstm.gettransactionCount();
			sqlList = Alterstm.getrewritesql();
		}
		else if(sql.toUpperCase().startsWith("INSERT"))
		{
			InsertRewriting Insertstm;
			try {
				Insertstm = new InsertRewriting(sql,this.tenantId);
				count = Insertstm.gettransactionCount();
				sqlList = Insertstm.getrewritesql();
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if(sql.toUpperCase().startsWith("UPDATE"))
		{
			UpdateRewriting Updatestm;
			try {
				Updatestm = new UpdateRewriting(sql,this.tenantId);
				count = Updatestm.gettransactionCount();
				sqlList = Updatestm.getrewritesql();
				//System.out.println("count"+count);
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if(sql.toUpperCase().startsWith("DELETE"))
		{
			DeleteRewriting Deletestm;
			try {
				Deletestm = new DeleteRewriting(sql,this.tenantId);
				count = Deletestm.gettransactionCount();
				sqlList = Deletestm.getrewritesql();
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if(sql.toUpperCase().startsWith("CREATE TABLE"))
		{
			CreateRewriting Createstm;
			try {
				Createstm = new CreateRewriting(sql);
				count = Createstm.gettransactionCount();
				sqlList = Createstm.getrewritesql();
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		else if(sql.toUpperCase().startsWith("CREATE TENANT"))
		{
			CreateTenantRewriting CreateTenantstm = new CreateTenantRewriting(sql,this.tenantId);
			count = CreateTenantstm.gettransactionCount();
			sqlList = CreateTenantstm.getrewritesql();
		}


		//System.out.println("sql : "+sql+" , count : "+count);
		int AlltruncatedUpdateCount = 0;

			checkClosed();

			Connection locallyScopedConn = this.connection;

			char firstStatementChar = StringUtils.firstNonWsCharUc(sql,
					findStartOfStatement(sql));

		try{
				locallyScopedConn.setAutoCommit(false);

				for(int i = 1;i <= count;i++)
				{
					sql = (String) sqlList.get(i-1);

				ResultSet rs = null;

				synchronized (locallyScopedConn.getMutex()) {
					synchronized (this.cancelTimeoutMutex) {
						this.wasCancelled = false;
					}

					checkNullOrEmptyQuery(sql);

					if (this.doEscapeProcessing) {
						Object escapedSqlResult = EscapeProcessor.escapeSQL(sql,
								this.connection.serverSupportsConvertFn(), this.connection);

						if (escapedSqlResult instanceof String) {
							sql = (String) escapedSqlResult;
						} else {
							sql = ((EscapeProcessorResult) escapedSqlResult).escapedSql;
						}
					}
					//System.out.println("1"+sql);
					if (locallyScopedConn.isReadOnly()) {
						throw SQLError.createSQLException(Messages
								.getString("Statement.42") //$NON-NLS-1$
								+ Messages.getString("Statement.43"), //$NON-NLS-1$
								SQLError.SQL_STATE_ILLEGAL_ARGUMENT); //$NON-NLS-1$
					}
					//System.out.println("2"+sql);
					if (StringUtils.startsWithIgnoreCaseAndWs(sql, "select")) { //$NON-NLS-1$
						throw SQLError.createSQLException(Messages
								.getString("Statement.46"), //$NON-NLS-1$
								"01S03"); //$NON-NLS-1$
					}
					//System.out.println("3"+sql);
					if (this.results != null) {
						if (!locallyScopedConn.getHoldResultsOpenOverStatementClose()) {
							this.results.realClose(false);
						}
					}

					// The checking and changing of catalogs
					// must happen in sequence, so synchronize
					// on the same mutex that _conn is using

					CancelTask timeoutTask = null;

					String oldCatalog = null;

					try {
						if (locallyScopedConn.getEnableQueryTimeouts() &&
								this.timeoutInMillis != 0
								&& locallyScopedConn.versionMeetsMinimum(5, 0, 0)) {
							timeoutTask = new CancelTask();
							Connection.getCancelTimer().schedule(timeoutTask,
									this.timeoutInMillis);
						}

						if (!locallyScopedConn.getCatalog().equals(this.currentCatalog)) {
							oldCatalog = locallyScopedConn.getCatalog();
							locallyScopedConn.setCatalog(this.currentCatalog);
						}

						//
						// Only apply max_rows to selects
						//
						if (locallyScopedConn.useMaxRows()) {
							locallyScopedConn.execSQL(
									this,
									"SET OPTION SQL_SELECT_LIMIT=DEFAULT", //$NON-NLS-1$
									-1, null, java.sql.ResultSet.TYPE_FORWARD_ONLY,
									java.sql.ResultSet.CONCUR_READ_ONLY, false,
									this.currentCatalog, true);
						}

						rs = locallyScopedConn.execSQL(this, sql, -1, null,
								java.sql.ResultSet.TYPE_FORWARD_ONLY,
								java.sql.ResultSet.CONCUR_READ_ONLY, false,
								this.currentCatalog,
								true /* force read of field info on DML */,
								isBatch);

						if (timeoutTask != null) {
							if (timeoutTask.caughtWhileCancelling != null) {
								throw timeoutTask.caughtWhileCancelling;
							}

							timeoutTask.cancel();
							timeoutTask = null;
						}

						synchronized (this.cancelTimeoutMutex) {
							if (this.wasCancelled) {
								this.wasCancelled = false;
								throw new MySQLTimeoutException();
							}
						}
					} finally {
						if (timeoutTask != null) {
							timeoutTask.cancel();
						}

						if (oldCatalog != null) {
							locallyScopedConn.setCatalog(oldCatalog);
						}
					}
				}

				this.results = rs;

				rs.setFirstCharOfQuery(firstStatementChar);

				this.updateCount = rs.getUpdateCount();

				int truncatedUpdateCount = 0;

				if (this.updateCount > Integer.MAX_VALUE) {
					truncatedUpdateCount = Integer.MAX_VALUE;
				} else {
					truncatedUpdateCount = (int) this.updateCount;
				}

				this.lastInsertId = rs.getUpdateID();
				AlltruncatedUpdateCount = AlltruncatedUpdateCount+truncatedUpdateCount;
			}
			locallyScopedConn.commit();

		}catch(SQLException se){

	        if (locallyScopedConn != null) {
	            try {
	                System.err.print("Transaction is being rolled back");
	                locallyScopedConn.rollback();
	            } catch(SQLException excep) {
	                System.out.println(excep);
	            }
	        }

		}finally{
			locallyScopedConn.setAutoCommit(true);
		}



		return AlltruncatedUpdateCount;
	}

	public java.sql.ResultSet executeQuery(String sql)
			throws SQLException {
		//System.out.println("1111111111111111111111111118");
		Count++;
		//System.out.println("2131314698"+sql+Count);

		//String rewritesql = "";
		if(sql.toUpperCase().startsWith("SELECT"))
		{
			//System.out.println("------");
				//System.out.println("---"+this.tenantId);
				SelectRewriting Selectstm;
				try {
					Selectstm = new SelectRewriting(sql,this.tenantId);
					sql = Selectstm.rewritesql;
					//System.out.println(sql);
				} catch (JSQLParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


		}




		checkClosed();

		Connection locallyScopedConn = this.connection;


		synchronized (locallyScopedConn.getMutex()) {
			synchronized (this.cancelTimeoutMutex) {
				this.wasCancelled = false;
			}

			checkNullOrEmptyQuery(sql);

			if (this.doEscapeProcessing) {
				Object escapedSqlResult = EscapeProcessor.escapeSQL(sql,
						locallyScopedConn.serverSupportsConvertFn(), this.connection);

				if (escapedSqlResult instanceof String) {
					sql = (String) escapedSqlResult;
				} else {
					sql = ((EscapeProcessorResult) escapedSqlResult).escapedSql;
				}
			}

			char firstStatementChar = StringUtils.firstNonWsCharUc(sql,
					findStartOfStatement(sql));

			if (sql.charAt(0) == '/') {
				if (sql.startsWith(PING_MARKER)) {
					doPingInstead();

					return this.results;
				}
			}

			checkForDml(sql, firstStatementChar);

			if (this.results != null) {
				if (!locallyScopedConn.getHoldResultsOpenOverStatementClose()) {
					this.results.realClose(false);
				}
			}

			CachedResultSetMetaData cachedMetaData = null;

			// If there isn't a limit clause in the SQL
			// then limit the number of rows to return in
			// an efficient manner. Only do this if
			// setMaxRows() hasn't been used on any Statements
			// generated from the current Connection (saves
			// a query, and network traffic).

			if (useServerFetch()) {
				this.results = createResultSetUsingServerFetch(sql);
				//System.out.println(this.results.getFetchSize());
				return this.results;

			}

			CancelTask timeoutTask = null;

			String oldCatalog = null;

			try {
				if (locallyScopedConn.getEnableQueryTimeouts() &&
						this.timeoutInMillis != 0
						&& locallyScopedConn.versionMeetsMinimum(5, 0, 0)) {
					timeoutTask = new CancelTask();
					Connection.getCancelTimer().schedule(timeoutTask,
							this.timeoutInMillis);
				}

				if (!locallyScopedConn.getCatalog().equals(this.currentCatalog)) {
					oldCatalog = locallyScopedConn.getCatalog();
					locallyScopedConn.setCatalog(this.currentCatalog);
				}

				//
				// Check if we have cached metadata for this query...
				//

				if (locallyScopedConn.getCacheResultSetMetadata()) {
					cachedMetaData = locallyScopedConn.getCachedMetaData(sql);
				}

				if (locallyScopedConn.useMaxRows()) {
					// We need to execute this all together
					// So synchronize on the Connection's mutex (because
					// even queries going through there synchronize
					// on the connection
					if (StringUtils.indexOfIgnoreCase(sql, "LIMIT") != -1) { //$NON-NLS-1$
						this.results = locallyScopedConn.execSQL(this, sql,
								this.maxRows, null, this.resultSetType,
								this.resultSetConcurrency,
								createStreamingResultSet(),
								this.currentCatalog, (cachedMetaData == null));
					} else {
						if (this.maxRows <= 0) {
							locallyScopedConn
									.execSQL(
											this,
											"SET OPTION SQL_SELECT_LIMIT=DEFAULT", -1, null, //$NON-NLS-1$
											java.sql.ResultSet.TYPE_FORWARD_ONLY,
											java.sql.ResultSet.CONCUR_READ_ONLY,
											false, this.currentCatalog,
											true); //$NON-NLS-1$
						} else {
							locallyScopedConn
									.execSQL(
											this,
											"SET OPTION SQL_SELECT_LIMIT=" + this.maxRows, -1, //$NON-NLS-1$
											null,
											java.sql.ResultSet.TYPE_FORWARD_ONLY,
											java.sql.ResultSet.CONCUR_READ_ONLY,
											false, this.currentCatalog,
											true); //$NON-NLS-1$
						}

						this.results = locallyScopedConn.execSQL(this, sql, -1,
								null, this.resultSetType,
								this.resultSetConcurrency,
								createStreamingResultSet(),
								this.currentCatalog, (cachedMetaData == null));

						if (oldCatalog != null) {
							locallyScopedConn.setCatalog(oldCatalog);
						}
					}
				} else {
					this.results = locallyScopedConn.execSQL(this, sql, -1, null,
							this.resultSetType, this.resultSetConcurrency,
							createStreamingResultSet(),
							this.currentCatalog, (cachedMetaData == null));
				}

				if (timeoutTask != null) {
					if (timeoutTask.caughtWhileCancelling != null) {
						throw timeoutTask.caughtWhileCancelling;
					}

					timeoutTask.cancel();
					timeoutTask = null;
				}

				synchronized (this.cancelTimeoutMutex) {
					if (this.wasCancelled) {
						this.wasCancelled = false;
						throw new MySQLTimeoutException();
					}
				}
			} finally {
				if (timeoutTask != null) {
					timeoutTask.cancel();
				}

				if (oldCatalog != null) {
					locallyScopedConn.setCatalog(oldCatalog);
				}
			}

			this.lastInsertId = this.results.getUpdateID();

			if (cachedMetaData != null) {
				locallyScopedConn.initializeResultsMetadataFromCache(sql, cachedMetaData,
						this.results);
			} else {
				if (this.connection.getCacheResultSetMetadata()) {
					locallyScopedConn.initializeResultsMetadataFromCache(sql,
							null /* will be created */, this.results);
				}
			}

			return this.results;
		}
	}


}
