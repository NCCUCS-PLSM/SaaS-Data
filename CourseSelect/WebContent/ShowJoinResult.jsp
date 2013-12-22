<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "com.orientechnologies.orient.core.metadata.schema.*,java.util.Collection,com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx,rewriting.*,com.orientechnologies.orient.core.record.impl.ODocument,com.orientechnologies.orient.core.sql.query.OSQLSynchQuery,java.util.List,java.util.ArrayList,nccu.com.orientechnologies.orient.jdbc.OrientJdbcConnection,java.sql.*,java.util.Properties,nccu.com.orientechnologies.orient.jdbc.OrientJdbcDriver,nccu.com.orientechnologies.orient.jdbc.OrientJdbcDatabaseMetaData,nccu.com.orientechnologies.orient.jdbc.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<link rel="stylesheet" href="/force/css/bootstrap.min.css" type="text/css"></link>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">

<!-- Le styles -->
    <link href="/force/css/bootstrap.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
      .sidebar-nav {
        padding: 9px 0;
      }
    </style>
    <link href="/force/css/bootstrap-responsive.css" rel="stylesheet">

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="/force/ico/favicon.ico">
    <link rel="apple-touch-icon-precomposed" sizes="144x144" href="../assets/ico/apple-touch-icon-144-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="114x114" href="../assets/ico/apple-touch-icon-114-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="72x72" href="../assets/ico/apple-touch-icon-72-precomposed.png">
    <link rel="apple-touch-icon-precomposed" href="../assets/ico/apple-touch-icon-57-precomposed.png">
  </head>




<title>Insert title here</title>
</head>
<body>


   <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="IndexForTenant.jsp">ShowCreateResult</a>
          <div class="nav-collapse collapse">
            <p class="navbar-text pull-right">



            </p>

          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>

    <div class="container-fluid">
      <div class="row-fluid">



        <div class="span9">




<%


				//String dbUrl = "local:/tmp/databases/exp0913";
                String dbUrl = "local:/tmp/databases/exp0916";


	            String username = "admin";
	            String password = "admin";

	            Properties info = new Properties();
	            info.put("user", username);
	            info.put("password", password);
	            OrientJdbcDriver Driver = new OrientJdbcDriver();

	            OrientJdbcConnection con = (OrientJdbcConnection) Driver.connect("jdbc:orient:" + dbUrl, info);
	            OrientJdbcStatement stmt = (OrientJdbcStatement) con.createStatement();


	            //stmt.settenantId(TenantName);
	            String sql = "SELECT * FROM AlterTablePrivateTable";
	            //String sql = "SELECT * FROM Table1 LEFT JOIN Table2 ON Table1.T1Char = Table2.T2Char AND Table1.T1Integer = Table2.T2Integer RIGHT JOIN Table3 ON Table1.T1Char = Table3.T3Char AND Table1.T1Integer = Table3.T3Integer ";
                //String sql = "SELECT ItemInfo.ItemName FROM ItemInfo";
	            OrientJdbcResultSet rs = (OrientJdbcResultSet) stmt.executeQuery(sql);
	            //out.println(rs.getRow());
	            if(rs.getFetchSize()==0)
	            {
	            	out.print("<h2>No Order Detail</h2>");
	            	stmt.close();
	            	//db.close();
	            }
	            else
	            {


	            	OrientJdbcResultSetMetaData rsmd = (OrientJdbcResultSetMetaData) rs.getMetaData();
	            	int numColumns = rsmd.getColumnCount();
	            	//out.print(numColumns);

	            	out.println("<table class= table >");


	                out.print("<tr>");

	                numColumns = rsmd.getColumnCount();
	            	for (int i=1; i<numColumns+1; i++)
	            	{

	            			out.print("<td>");
	            	    	out.print(rsmd.getColumnName(i).replace("Table2.", "").replace("Table1.", "").replace("Table3.", ""));
	            	    	out.println("</td>");

	            	}
	                out.println("</tr>");

	            	int count = 0;





	            	while(rs.next())
	            	{

	            			rsmd = (OrientJdbcResultSetMetaData) rs.getMetaData();

	            			for (int i=1; i<numColumns+1; i++)
	            			{


	            					out.print("<td>");
	            					out.print(rs.getObject((String) rs.getColumnresult().get(i)));
	            					out.println("</td>");



	            			}
	            			out.println("</tr>");






	            	}


				  stmt.close();
				  con.close();

	            }



%>


        </div><!--/span-->
      </div><!--/row-->

      <hr>



    </div>




<script>

</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>