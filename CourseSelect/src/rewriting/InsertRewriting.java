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
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.SubSelect;

public class InsertRewriting {
	private String sql;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	private Insert insert;
    private List columnsList;
	private String itemsList;
	private String table;
	private String tenantId;
	public List rewritesql;
	public int transactionCount;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";

	public InsertRewriting(String sql, String tenantId) throws JSQLParserException, SQLException {
		// TODO Auto-generated constructor stub
		this.sql = sql;
		this.tenantId = tenantId;
		transactionCount = 0;
		rewritesql = new ArrayList();
		//System.out.println(sql);
		insert = (Insert) pm.parse(new StringReader(sql));
		columnsList = insert.getColumns();
		//System.out.println(columnsList);
		itemsList = insert.getItemsList().toString();
		//System.out.println(itemsList);
		table = insert.getTable().toString();
		ArrayList lines = loadFile(filePath);

		String CommonColumn = "";
		String PrivateColumn = "";
		String PrivateTableInsert = "";
		int IsPrivateTableExist = 0;
		if(table.contains("PrivateTable"))
		{
			PrivateTableInsert = sql;
			rewritesql.add(PrivateTableInsert);
			transactionCount++;
			//System.out.println(PrivateTableInsert);
		}
		else
		{
			for (int i=0; i<lines.size(); i++){
				if(lines.get(i).toString().indexOf(tenantId+table)>-1)
				{
					IsPrivateTableExist++;
					PrivateColumn = lines.get(i).toString();
				}
				else if(lines.get(i).toString().indexOf(table+"CommonFields")>-1)
				{
					CommonColumn = lines.get(i).toString();
				}

	        }
			//System.out.println(CommonColumn+" "+PrivateColumn);
			Iterator columnitr = columnsList.iterator();
			itemsList = itemsList.replace("(", "").replace(")", "");
			String[] valueArray = itemsList.split(",");
			String columntmp;
			String valuetmp;
			String StrCommonColumn = "";
			String StrPrivateColumn = "";
			String StrCommonValue = "";
			String StrPrivateValue = "";
			int valueflag = 0;
			int commonflag = 0;
			int privateflag = 0;
			CommonColumn = CommonColumn.replace(",", "")+" ";
			PrivateColumn = PrivateColumn.replace(",", "")+" ";
			//System.out.println(CommonColumn);
			while(columnitr.hasNext())
			{
				columntmp = columnitr.next().toString().trim();
				valuetmp = valueArray[valueflag].trim();
				if(CommonColumn.contains(" "+columntmp+" "))
				{
					StrCommonColumn = StrCommonColumn+","+columntmp;
					StrCommonValue = StrCommonValue+","+valuetmp;
					commonflag++;
				}
				else if(PrivateColumn.contains(" "+columntmp+" "))
				{
					StrPrivateColumn = StrPrivateColumn+","+columntmp;
					StrPrivateValue = StrPrivateValue+","+valuetmp;
					privateflag++;
				}

				valueflag++;
			}

			//System.out.println(StrPrivateColumn.replaceFirst(",", "")+" "+StrPrivateValue.replaceFirst(",", ""));
			int RowMax = getMaxRowInt(table,tenantId)+1;

			String CommonInsert = "INSERT INTO "+table+"CommonFields ( TenantId,Row,"+StrCommonColumn.replaceFirst(",", "")+" ) VALUES ( '"+tenantId+"',"+RowMax+","+StrCommonValue.replaceFirst(",", "")+" )";
			String PrivateInsert = "INSERT INTO "+tenantId+table+" ( TenantId,Row,"+StrPrivateColumn.replaceFirst(",", "")+" ) VALUES ( '"+tenantId+"',"+RowMax+","+StrPrivateValue.replaceFirst(",", "")+" )";
			//System.out.println("RowMax:"+RowMax);
			if(commonflag == 0)
	        {
				CommonInsert = "INSERT INTO "+table+"CommonFields ( TenantId,Row ) VALUES ( '"+tenantId+"',"+RowMax+" )";
				rewritesql.add(CommonInsert);
				transactionCount++;
				//System.out.println(CommonInsert);
			}
			else
			{
				rewritesql.add(CommonInsert);
				transactionCount++;
				//System.out.println(CommonInsert);
			}
			if(privateflag == 0)
			{
				//System.out.println("3"+ PrivateInsert);
				PrivateInsert = "INSERT INTO "+tenantId+table+" ( TenantId,Row ) VALUES ( '"+tenantId+"',"+RowMax+" )";
				//System.out.println("3"+ PrivateInsert);
				rewritesql.add(PrivateInsert);
				transactionCount++;
			}
			else
			{
				//System.out.println("4"+ PrivateInsert);
				rewritesql.add(PrivateInsert);
				transactionCount++;

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

	public int getMaxRowInt(String table, String tenantid){
		String sql="SELECT MAX(`Row`) FROM "+table+"CommonFields WHERE TenantId = '"+tenantid+"'";
		//System.out.println(sql);
		SQLconn sqlConn = new SQLconn();

		return sqlConn.SelectMax(sql);
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
