package rewriting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.parser.CCJSqlParserManager;

public class CreateTenantRewriting {

	private String sql;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	private String tenantId;
	public List rewritesql;
	public int transactionCount;
	private String rewritesqltmp;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";
	public CreateTenantRewriting(String sql, String tenantId) throws SQLException {
		// TODO Auto-generated constructor stub
		this.sql = sql;
		this.tenantId = tenantId;
		transactionCount = 0;
		rewritesql = new ArrayList();
		String tmpLine="";
		ArrayList lines = loadFile(filePath);
		for (int i=0; i<lines.size(); i++)
		{
			if(lines.get(i).toString().indexOf("CommonFields")>-1)
		    {

		    	  String[] LineArray = lines.get(i).toString().split(",");
				  String CreateTableName = tenantId+LineArray[0].replace("CommonFields","");
				  System.out.println(CreateTableName);

				  rewritesqltmp = "CREATE TABLE "+CreateTableName+" ( TenantId char(30) not null , Row integer not null , Primary key (TenantId, Row))";
				  tmpLine = tmpLine+CreateTableName+"\n";

				  rewritesql.add(rewritesqltmp);
				  transactionCount++;
				  System.out.println(rewritesqltmp);

		    }
        }

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
