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
import java.util.regex.Pattern;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.Top;
import net.sf.jsqlparser.statement.select.Union;


public class SelectRewriting implements SelectVisitor {

	private String sql;
	private CCJSqlParserManager pm = new CCJSqlParserManager();
	private Select select;
	private Distinct distinct;
	private String fromtable;
	private List groupbycolumnreferences;
	private Expression having;
	private Table into;
	private List joins;
	private Limit limit;
	private List orderbyelements;
	private String selectitems;
	private Top top;
	private Expression where;
	private List tables;
	private Expression joincondition;
	private FromItem jointable;
	private Join joinstatement;
	private PlainSelect plainselect;
	private String tenantId;
	public String rewritesql;
	private String filePath = "D:\\NewJava\\CourseSelect\\SchemaMappingTable.txt";
	public ResultSet finalresult = null;

	public SelectRewriting(String sql, String tenantId) throws JSQLParserException {
		// TODO Auto-generated constructor stub
		//String regularsql = sql.replace("/r", "").replace("/n", "").replaceAll(" {2,}", "");
		this.sql = sql;
				//regularsql.replace("( ", "(");
		this.tenantId = tenantId;
		System.out.println(sql);
		select = (Select) pm.parse(new StringReader(sql));
		select.getSelectBody().accept(this);
		//finalresult

	}

	@Override
	public void visit(PlainSelect plainselect) {
		// TODO Auto-generated method stub
		distinct = plainselect.getDistinct();
		fromtable = plainselect.getFromItem().toString();
		groupbycolumnreferences = plainselect.getGroupByColumnReferences();
		having = plainselect.getHaving();
		into = plainselect.getInto();
		joins = plainselect.getJoins();
		limit = plainselect.getLimit();
		orderbyelements = plainselect.getOrderByElements();
		selectitems = plainselect.getSelectItems().toString().replace("[", "").replace("]", "");
		top = plainselect.getTop();
		where = plainselect.getWhere();
/*
		System.out.println(jointable);
		System.out.println("---!"+joincondition);
		System.out.println(distinct);
		System.out.println(fromtable);
		System.out.println(groupbycolumnreferences);
		System.out.println(having);
		System.out.println(into);
		System.out.println(joins);
		System.out.println("limit:             "+limit);
		System.out.println(selectitems.toString().equals("[*]"));
		System.out.println(orderbyelements);
		System.out.println(top);
		System.out.println(where);
*/

		//String
		String CommonColumn = "";
		String PrivateColumn = "";
		String AllColumn = "";

		if(!(joins == null))
		{
			String FromTableName = plainselect.getFromItem().toString();
			String CommonTableName = FromTableName+"CommonFields";
			String PrivateTableName = tenantId+FromTableName;
			//System.out.println(CommonTableName+" "+PrivateTableName);
			String ColumnList = "";
			ArrayList lines = loadFile(filePath);
			for (int i=0; i<lines.size(); i++){
				if(lines.get(i).toString().indexOf(PrivateTableName)>-1)
				{

					PrivateColumn = lines.get(i).toString();
					PrivateColumn = PrivateColumn.replace(PrivateTableName, "");

				}
				else if(lines.get(i).toString().indexOf(CommonTableName)>-1)
				{


					CommonColumn = lines.get(i).toString();
					CommonColumn = CommonColumn.replace(CommonTableName+",", "");

				}

	        }
			AllColumn = CommonColumn+PrivateColumn;


			rewritesql = sql;
			String FromTableRewrite = "";


			FromTableRewrite = "( SELECT "+AllColumn+" FROM "+CommonTableName+" NATURAL JOIN "+PrivateTableName+" ) AS "+FromTableName;


			//rewritesql = "SELECT "+selectitems+" FROM ( SELECT "+AllColumn+" FROM "+CommonTableName+" LEFT JOIN "+PrivateTableName+" ON "+CommonTableName+".TenantId = "+PrivateTableName+".TenantId AND "+CommonTableName+".Row = "+PrivateTableName+".Row ) AS "+FromTableName;
			rewritesql = rewritesql.replace(" "+FromTableName+" "," "+FromTableRewrite+" ");
			//System.out.println("111111111111:"+rewritesql);


			Iterator joinitr = joins.listIterator();
			while(joinitr.hasNext())
			{

				Join joinstatement = (Join) joinitr.next();
				String jointype = "";
				jointable = joinstatement.getRightItem();
				joincondition = joinstatement.getOnExpression();
				//System.out.println(jointable+"                          "+joincondition );

				CommonTableName = jointable+"CommonFields";
				PrivateTableName = tenantId+jointable;
				PrivateColumn = "";
				CommonColumn = "";
				ColumnList = "";
				lines = loadFile(filePath);
				for (int i=0; i<lines.size(); i++){
					if(lines.get(i).toString().indexOf(PrivateTableName)>-1)
					{
						//System.out.println("123456+7");
						PrivateColumn = lines.get(i).toString();
						PrivateColumn = PrivateColumn.replace(PrivateTableName, "");


					}
					else if(lines.get(i).toString().indexOf(CommonTableName)>-1)
					{

						//System.out.println("123456+8");
						CommonColumn = lines.get(i).toString();
						CommonColumn = CommonColumn.replace(CommonTableName+",", "");

					}

		        }
				AllColumn = CommonColumn+PrivateColumn;

				//System.out.println(CommonColumn+PrivateColumn+"AllColumn:"+AllColumn);
				String JoinTableRewrite = "";


				JoinTableRewrite = "( SELECT "+AllColumn+" FROM "+CommonTableName+" NATURAL JOIN "+PrivateTableName+" ) AS "+jointable;

				//rewritesql = "SELECT "+selectitems+" FROM ( SELECT "+AllColumn+" FROM "+CommonTableName+" LEFT JOIN "+PrivateTableName+" ON "+CommonTableName+".TenantId = "+PrivateTableName+".TenantId AND "+CommonTableName+".Row = "+PrivateTableName+".Row ) AS "+FromTableName;
				rewritesql = rewritesql.replace(" "+jointable+" "," "+JoinTableRewrite+" ");
				System.out.println(rewritesql);




			}




		}
		else
		{
			String CommonTableName = fromtable+"CommonFields";
			String PrivateTableName = tenantId+fromtable;
			//IsPrivateTableExist = 0;
			if(fromtable.contains("PrivateTable"))
			{
				rewritesql = sql;
			}
			else
			{

					String ColumnList = "";
					ArrayList lines = loadFile(filePath);
					for (int i=0; i<lines.size(); i++){
						if(lines.get(i).toString().indexOf(PrivateTableName)>-1)
						{

							PrivateColumn = lines.get(i).toString();
							PrivateColumn = PrivateColumn.replace(PrivateTableName, "");

						}
						else if(lines.get(i).toString().indexOf(CommonTableName)>-1)
						{
							CommonColumn = lines.get(i).toString();
							CommonColumn = CommonColumn.replace(CommonTableName+",", "");

						}

			        }
					AllColumn = CommonColumn+PrivateColumn;
					//System.out.println(AllColumn);

					//System.out.println("1231");
					String JoinTableRewrite = CommonTableName+" NATURAL JOIN "+PrivateTableName;

					rewritesql = sql;
					//rewritesql = rewritesql.replace("*",AllColumn).replace(" "+fromtable+" ", " "+JoinTableRewrite+" ");
					if(where == null)
					{
						rewritesql = rewritesql.replace(" "+fromtable," "+JoinTableRewrite);
						rewritesql = rewritesql.replace(" *", AllColumn);
					}
					else
					{
						//String wherereplacement = " TenantId = '"+tenantId+"' AND ( "+where.toString()+" )";
						//System.out.println("wherereplacement:"+wherereplacement);
						//System.out.println("where:"+where);
						System.out.println("resql:"+rewritesql);
						//System.out.println(rewritesql.compareToIgnoreCase(wherereplacement));
						rewritesql = rewritesql.replace(" "+fromtable+" "," "+JoinTableRewrite+" ");
						rewritesql = rewritesql.replace(" *", AllColumn);
						//System.out.println("resql:"+rewritesql);
					}




			}
			/*
			System.out.println(sql);
			System.out.println(where);
			*/
			System.out.println(rewritesql);


		}




	}





	private String addlimit(String rewritesql) {
		// TODO Auto-generated method stub
		return rewritesql;
	}

	private String addorderby(String rewritesql) {
		// TODO Auto-generated method stub
		return rewritesql;
	}

	private String adddistinct(String rewritesql) {
		// TODO Auto-generated method stub
		return rewritesql;
	}

	private String addwhere(String rewritesql) {
		// TODO Auto-generated method stub
		return rewritesql;
	}

	@Override
	public void visit(Union arg0) {
		// TODO Auto-generated method stub

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
