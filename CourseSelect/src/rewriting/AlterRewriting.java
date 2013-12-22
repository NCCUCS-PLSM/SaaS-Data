package rewriting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.CreateTable;


public class AlterRewriting {

	private String sql;
	private String tableName;
	private String[] partSQL;
	private String tenantId;
	private String altersqlstm;
	private String altersqltype;
	private String altersqltable;
	private String columnname;
	private String newcolumnname;
	private String newcolumntype;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	public List rewritesql;
	public int transactionCount;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";

	public AlterRewriting(String sql, String tenantId) throws SQLException {
		// TODO Auto-generated constructor stub
		this.sql = sql;
		this.tenantId = tenantId;
		transactionCount = 0;
		rewritesql = new ArrayList();
		partSQL = sql.split(" ");
		this.tableName=partSQL[2];
		String alterType=partSQL[3];
		if(alterType.equalsIgnoreCase("add")){
			if(partSQL.length==7)
			{
				doAlterAdd(partSQL[4],partSQL[5],partSQL[6]);
			}
			else
			{
				doAlterAdd(partSQL[4],partSQL[5],"");
			}

		}else if(alterType.equalsIgnoreCase("change")){
			doAlterChange(partSQL[4],partSQL[5],partSQL[6]);
		}else if(alterType.equalsIgnoreCase("modify")){
			doAlterModify(partSQL[4],partSQL[5]);
		}else if(alterType.equalsIgnoreCase("drop")){
			doAlterDrop(partSQL[4]);
		}

	}

	private void doAlterDrop(String string) throws SQLException {
		// TODO Auto-generated method stub
		ArrayList lines = loadFile(filePath);
		String tmpLine="";

		if(tableName.contains("CommonFields"))
		{

			rewritesql.add("ALTER TABLE "+tableName+ " DROP "+string);
			transactionCount++;


			for (int i=0; i<lines.size(); i++){
				if(!(lines.get(i).toString().indexOf(tableName)>-1))
				{
					System.out.println(lines.get(i));
					tmpLine+=lines.get(i).toString()+"\n";
				}
				else
				{
					tmpLine+=lines.get(i).toString().replace(", "+string,"")+"\n";
				}


	        }
			FileWriter fstream;
			try {
				fstream = new FileWriter(filePath);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(tmpLine);
				//Close the output stream
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{

			for (int i=0; i<lines.size(); i++){
				if(!(lines.get(i).toString().indexOf(tenantId+tableName)>-1))
				{

					System.out.println(lines.get(i));
					tmpLine+=lines.get(i).toString()+"\n";

				}
				else
				{


					tmpLine+=lines.get(i).toString().replace(", "+string,"")+"\n";

				}

	        }

			rewritesql.add("ALTER TABLE "+tenantId+tableName+ " DROP "+string);
			transactionCount++;




			FileWriter fstream;
			try {
				fstream = new FileWriter(filePath);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(tmpLine);
				//Close the output stream
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


	   }
	}

	private void doAlterChange(String string, String string2, String string3) throws SQLException {
		// TODO Auto-generated method stub
		ArrayList lines = loadFile(filePath);
		String tmpLine="";
		if(tableName.contains("CommonFields"))
		{


		    rewritesql.add("ALTER TABLE "+tableName+ " CHANGE "+string+" "+string2+" "+string3);
		    transactionCount++;




			for (int i=0; i<lines.size(); i++){
				if(!(lines.get(i).toString().indexOf(tableName)>-1))
				{
					System.out.println(lines.get(i));
					tmpLine+=lines.get(i).toString()+"\n";
				}
				else
				{
					tmpLine+=lines.get(i).toString().replace(", "+string,", "+string2)+"\n";
				}


	        }
			FileWriter fstream;
			try {
				fstream = new FileWriter(filePath);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(tmpLine);
				//Close the output stream
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{

			for (int i=0; i<lines.size(); i++){
				if(!(lines.get(i).toString().indexOf(tenantId+tableName)>-1))
				{

					System.out.println(lines.get(i));
					tmpLine+=lines.get(i).toString()+"\n";

				}
				else
				{


					tmpLine+=lines.get(i).toString().replace(", "+string,", "+string2)+"\n";

				}

	        }

			rewritesql.add("ALTER TABLE "+tenantId+tableName+ " CHANGE "+string+" "+string2+" "+string3);
		    transactionCount++;





			FileWriter fstream;
			try {
				fstream = new FileWriter(filePath);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(tmpLine);
				//Close the output stream
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void doAlterModify(String string, String string2) throws SQLException {
		// TODO Auto-generated method stub
		ArrayList lines = loadFile(filePath);
		String tmpLine="";
		if(tableName.contains("CommonFields"))
		{
			rewritesql.add("ALTER TABLE "+tableName+ " MODIFY "+string+" "+string2);
		    transactionCount++;

		}
		else
		{
			rewritesql.add("ALTER TABLE "+tenantId+tableName+ " MODIFY "+string+" "+string2);
		    transactionCount++;

		}
		System.out.println(rewritesql);

	}

	private void doAlterAdd(String string, String string2, String string3) throws SQLException {
		// TODO Auto-generated method stub
		System.out.println("hello");
		ArrayList lines = loadFile(filePath);
		String tmpLine="";
		if(tableName.contains("CommonFields"))
		{
			if(string3.equals(""))
			{
				rewritesql.add("ALTER TABLE "+tableName+ " ADD "+string+" "+string2);
				transactionCount++;
			}
			else
			{
				rewritesql.add("ALTER TABLE "+tableName+ " ADD "+string+" "+string2+" "+string3);

				transactionCount++;
			}




			for (int i=0; i<lines.size(); i++){
				if(!(lines.get(i).toString().indexOf(tableName)>-1))
				{
					System.out.println("-------------------------------------------------:"+lines.get(i));
					tmpLine+=lines.get(i).toString()+"\n";
				}
				else
				{
					tmpLine+=lines.get(i).toString()+", "+string+"\n";
				}


	        }
			FileWriter fstream;
			try {
				fstream = new FileWriter(filePath);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(tmpLine);
				//Close the output stream
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{

			for (int i=0; i<lines.size(); i++){
				if(!(lines.get(i).toString().indexOf(tenantId+tableName)>-1))
				{

					System.out.println(lines.get(i));
					tmpLine+=lines.get(i).toString()+"\n";

				}
				else
				{


					tmpLine+=lines.get(i).toString()+", "+string+"\n";

				}

	        }
			if(string3.equals(""))
			{

				rewritesql.add("ALTER TABLE "+tenantId+tableName+ " ADD "+string+" "+string2);
				transactionCount++;
			}
			else
			{
				rewritesql.add("ALTER TABLE "+tenantId+tableName+ " ADD "+string+" "+string2+" "+string3);
				transactionCount++;
			}



				FileWriter fstream;
				try {
					fstream = new FileWriter(filePath);
					BufferedWriter out = new BufferedWriter(fstream);
					out.write(tmpLine);
					//Close the output stream
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


		}

		System.out.println(rewritesql);
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
