package rewriting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.update.Update;


public class UpdateRewriting {
	private String sql;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	private Update update;
	private List columns;
	private List expressions;
	private String table;
	private String where;
	private String tenantId;
	public List rewritesql;
	public int transactionCount;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";

	public UpdateRewriting(String sql, String tenantId) throws JSQLParserException, SQLException {
		// TODO Auto-generated constructor stub
		this.sql = sql;
		this.tenantId = tenantId;
		transactionCount = 0;
		rewritesql = new ArrayList();
		update = (Update) pm.parse(new StringReader(sql));
		columns = update.getColumns();
		//System.out.println(columns);
		expressions = update.getExpressions();
		//System.out.println(expressions);
		table = update.getTable().toString();

		if(update.getWhere()!=null)
		{
			where = update.getWhere().toString();
		}




		//System.out.println(where);

		ArrayList lines = loadFile(filePath);
		String CommonColumn = "";
		String PrivateColumn = "";
		for (int i=0; i<lines.size(); i++){
			if(lines.get(i).toString().indexOf(tenantId+table)>-1)
			{

				PrivateColumn = lines.get(i).toString();

			}
			else if(lines.get(i).toString().indexOf(table+"CommonFields")>-1)
			{


				CommonColumn = lines.get(i).toString();

			}

        }
		String columntmp;
		String valuetmp;
		Iterator columnitr = columns.iterator();
		Iterator valuesitr = expressions.iterator();
		String StrCommonSet = "";
		String StrPrivateSet  = "";
		int commonflag = 0;
		int privateflag = 0;
		//System.out.println("Size:"+columns.size());
		CommonColumn = CommonColumn.replace(",", "")+" ";
		PrivateColumn = PrivateColumn.replace(",", "")+" ";
		while(columnitr.hasNext())
		{
			columntmp = columnitr.next().toString().trim();
			valuetmp = valuesitr.next().toString().trim();
			//System.out.println(columntmp);
			if(CommonColumn.contains(" "+columntmp+" "))
			{
				StrCommonSet = StrCommonSet+","+columntmp+" = "+valuetmp;
				commonflag++;

			}
			else if(PrivateColumn.contains(" "+columntmp+" "))
			{
				StrPrivateSet = StrPrivateSet+","+columntmp+" = "+valuetmp;
				privateflag++;

			}

		}
		List RowResult = getRowNum(table,tenantId);
		Iterator Rowitr = RowResult.iterator();
		String CommonUpdate = "";
		String PrivateUpdate = "";
		//System.out.println( RowResult.size());
		while(Rowitr.hasNext())
		{
		    int RowNum = (int) Rowitr.next();
		    //System.out.println("RowNum:"+RowNum+" "+commonflag+" "+privateflag);
			CommonUpdate = "UPDATE "+table+"CommonFields SET "+StrCommonSet.replaceFirst(",", "")+" WHERE TenantId = '"+tenantId+"' AND Row = "+RowNum;
			PrivateUpdate = "UPDATE "+tenantId+table+" SET "+StrPrivateSet.replaceFirst(",", "")+" WHERE TenantId = '"+tenantId+"' AND Row = "+RowNum;


			if(commonflag > 0)
			{

			  rewritesql.add(CommonUpdate);
			  transactionCount++;
			  //System.out.println( CommonUpdate );
			}
		    if(privateflag > 0)
		    {
			  rewritesql.add(PrivateUpdate);
			  transactionCount++;
			  //System.out.println( PrivateUpdate );
			}
		}


	}

	public List getrewritesql() {
		// TODO Auto-generated method stub
		return this.rewritesql;
	}
	public int gettransactionCount() {
		// TODO Auto-generated method stub
		return this.transactionCount;
	}


	public List getRowNum(String table, String tenantid){
		String mysql = "";
		//System.out.println("where:"+where);
		if(update.getWhere()!=null)
		{
			mysql ="SELECT Row FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'"+ " AND ( "+where+" )";
		}
		else
		{
			mysql="SELECT Row FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'";
		}

		//System.out.println("sql:"+mysql);
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
            System.out.println(e);
            return null;
        }

        return file;
    }

}
