package rewriting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.CreateTable;



public class CreateRewriting {
	private String sql;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	private CreateTable create;
	private List columnDefinitions;
	private Table table;
	private List tableOptionsStrings;
	private String rewritesqltmp;
	public List rewritesql;
	public int transactionCount;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";
	public CreateRewriting(String sql) throws JSQLParserException, SQLException {
		// TODO Auto-generated constructor stub
		this.sql = sql;
		transactionCount = 0;
		rewritesql = new ArrayList();
		create = (CreateTable) pm.parse(new StringReader(sql));
		columnDefinitions = create.getColumnDefinitions();
		System.out.println(columnDefinitions);
		table = create.getTable();
		System.out.println(table);
		tableOptionsStrings = create.getTableOptionsStrings();
		System.out.println(tableOptionsStrings);

		Iterator columnitr = columnDefinitions.listIterator();

		ArrayList lines = loadFile(filePath);


		String tmpLine = table.toString();
		String columntmp = "";
		String[] columnArray;

		if(table.toString().contains("PrivateTable"))
		{
			rewritesqltmp = sql;


			while(columnitr.hasNext())
			{
				columntmp = columnitr.next().toString().trim();
				columnArray = new String[columntmp.split(" ").length];
				columnArray = columntmp.split(" ");


				tmpLine = tmpLine + ", "+columnArray[0];
				//System.out.println(columnitr.next());
			}
			tmpLine = tmpLine+"\n";

			rewritesql.add(rewritesqltmp);
			transactionCount++;

		}
		else
		{
			rewritesqltmp = "CREATE TABLE "+table+" ( TenantId char(50) not null , Row integer not null ";

			while(columnitr.hasNext())
			{
				columntmp = columnitr.next().toString().trim();
				columnArray = new String[columntmp.split(" ").length];
				columnArray = columntmp.split(" ");
				rewritesqltmp = rewritesqltmp + ", "+columntmp;

				tmpLine = tmpLine + ", "+columnArray[0];
				//System.out.println(columnitr.next());
			}
			tmpLine = tmpLine+" \n";
			rewritesqltmp = rewritesqltmp.trim() + " , Primary key (TenantId, Row) )";
			rewritesql.add(rewritesqltmp);
			transactionCount++;

		}





		System.out.println(tmpLine);
		System.out.println(rewritesql);
		FileWriter fstream;
		try {
			fstream = new FileWriter(filePath,true);
			fstream.append(tmpLine);
			//Close the output stream
			fstream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	public List getrewritesql() {
		// TODO Auto-generated method stub
		return this.rewritesql;
	}
	public int gettransactionCount() {
		// TODO Auto-generated method stub
		return this.transactionCount;
	}


}
