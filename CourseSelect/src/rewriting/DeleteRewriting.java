package rewriting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;


public class DeleteRewriting {

	private String sql;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	private Delete delete;
	private String table;
	private String where;
	private String tenantId;
	public List rewritesql;
	public int transactionCount;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";

	public DeleteRewriting(String sql, String tenantId) throws JSQLParserException, SQLException {
		// TODO Auto-generated constructor stub
		this.sql = sql;
		this.tenantId = tenantId;
		transactionCount = 0;
		rewritesql = new ArrayList();
		delete = (Delete) pm.parse(new StringReader(sql));
		table = delete.getTable().toString();
		if(delete.getWhere()!=null)
		{
			where = delete.getWhere().toString();
		}

		ArrayList lines = loadFile(filePath);
		List RowResult = getRowNum(table,tenantId);
		Iterator Rowitr = RowResult.iterator();


		while(Rowitr.hasNext())
		{
			int RowNum = (int) Rowitr.next();
		    String CommonDelete = "DELETE FROM "+table+"CommonFields WHERE TenantId = '"+tenantId+"' AND Row = "+RowNum;
			String PrivateDelete = "DELETE FROM "+tenantId+table+" WHERE TenantId = '"+tenantId+"' AND Row = "+RowNum;
			rewritesql.add(CommonDelete);
			transactionCount++;
			rewritesql.add(PrivateDelete);
			transactionCount++;

			//System.out.println(CommonDelete);
			//System.out.println(PrivateDelete);
		}

	}
	public List getRowNum(String table, String tenantid){
		String mysql = "";
		if(delete.getWhere()!=null)
		{
			mysql="SELECT Row FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'"+ " AND ( "+where+" )";
			//mysql =" SYSTEM SELECT Row FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'"+ " AND "+where;
		}
		else
		{
			mysql="SELECT Row FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'";
			//mysql=" SYSTEM SELECT Row FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'";
		}

		//System.out.println(mysql);
		SQLconn sqlConn = new SQLconn();

		return sqlConn.SelectRow(mysql);
	}

	private ArrayList loadFile(String fileName){
        if ((fileName == null) || (fileName == ""))
            throw new IllegalArgumentException();

        String line;
        ArrayList file = new ArrayList();

        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            if (!in.ready())
                throw new IOException();

            while ((line = in.readLine()) != null)
                file.add(line);

            in.close();
        }
        catch (IOException e)
        {
            //System.out.println(e);
            return null;
        }

        return file;
    }

	public List getrewritesql() {
		// TODO Auto-generated method stub
		return this.rewritesql;
	}
	public int gettransactionCount() {
		// TODO Auto-generated method stub
		return this.transactionCount;
	}

}
