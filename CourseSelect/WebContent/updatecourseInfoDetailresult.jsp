<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
<%@ page import="rewriting.*,com.mysql.jdbc.*,java.sql.DriverManager"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.oreilly.servlet.*" %>
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
<%
String AccountName = (String) session.getAttribute("AccountName");

String CourseId = (String) session.getAttribute("CourseId");
String TenantName = (String) session.getAttribute("TenantName");
String TenantRealName = (String) session.getAttribute("TenantRealName");
//
//session.setAttribute("TenantName", TenantName);

//String NowPage = request.getParameter("NowPage");


//宣告將上傳之檔案放置到伺服器的C:\Upload目錄中
//宣告限制上傳之檔案大小為 5 MB
String saveDirectory = "D://NewJava/ExtWebPro/WebContent/Image";
int    maxPostSize = 5 * 1024 * 1024 ;

//宣告上傳檔案名稱
String FileName = null;

//宣告上傳檔案型態
String ContentType = null;

//宣告敘述上傳檔案內容敘述
String Description = null;

//計算上傳檔案之個數
int filecount = 0 ;

//宣告上傳檔案檔名所使用的編碼，預設值為 ISO-8859-1，
//若改為Big5或MS950則支援中文檔名
String enCoding = "big5";

//產一個新的MultipartRequest 的物件，multi
MultipartRequest multi = new MultipartRequest(request, saveDirectory, maxPostSize, enCoding);
//取得所有上傳之檔案輸入型態名稱及敘述
Enumeration filesname = multi.getFileNames();
Enumeration filesdc = multi.getParameterNames();

while (filesname.hasMoreElements())
{
String name = (String)filesname.nextElement();
String dc = (String)filesdc.nextElement();
FileName = multi.getFilesystemName(name);
ContentType = multi.getContentType(name);
Description = multi.getParameter(dc);

if (FileName != null)
{
   filecount ++;
}
}


%>


   <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="IndexForTenant.jsp">Online Course Selection Admin</a>
          <div class="nav-collapse collapse">
            <p class="navbar-text pull-right">
		<%
		if(TenantName!=null)
		{
			//out.print("Welcome! <a class='navbar-link'>"+AccountName+"</a>");
			out.print("Welcome! "+TenantName+"   <a href = 'signoutfortenant.jsp' class='navbar-link'>  Sign out >></a>");


		}
		else
		{
			out.print("Logged in as <a href='tenant.jsp' class='navbar-link'>Tenantname</a>");
		}

		%>


            </p>

          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>

    <div class="container-fluid">
      <div class="row-fluid">
      <div class="span3">
          <div class="well sidebar-nav">
            <ul class="nav nav-list">
              <li class="nav-header">Course information Management</li>
              <li class="active"><a href="IndexForTenant.jsp">Course List</a></li>
              <li><a href="showcloseitem.jsp">Closed Course List</a></li>
              <li><a href="addproduct.jsp">Add Course information</a></li>
              <li class="nav-header">Information Column Management</li>
              <li><a href="addfield.jsp">Add Customized Column </a></li>
              <li><a href="updatefield.jsp">Update Customized Column</a></li>
              <li><a href="dropfield.jsp">Remove Customized Column</a></li>
              <li class="nav-header">Course Selection Result Management</li>
              <li><a href="courseselection.jsp">Course Selection Result</a></li>
            </ul>
          </div><!--/.well -->
        </div><!--/span-->


        <div class="span9">

          <form action = IndexForTenant.jsp method=post name = form21 id = form21>
			<input name="TenantName" value="" type="hidden">
			<input name="NowPage" value="" type="hidden">
		  <form>


			<%

			if(TenantName!=null)
			{
				Class.forName("com.mysql.jdbc.Driver");
				Connection con = (Connection) DriverManager.getConnection(
			  		      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
					      "root","9326691");

				MultitenantDatabaseMetaData meta = new MultitenantDatabaseMetaData(con,"ExtensionTable");

	            ResultSet schemas = (ResultSet) meta.getColumns("", "","CourseInfo", "",TenantName);


	            String setfield ="";
	            String Encode;

	            int count = 1;
	            int counttmp = schemas.getFetchSize();
	            while(schemas.next())
	            {
	            	if(schemas.getString("COLUMN_NAME").equals("CourseId"))
	            	{

	            	}
	            	else
	            	{

	            		if(schemas.getString("TYPE_NAME").equals("INT"))
            			{
            					Encode = (String) multi.getParameter(schemas.getString("COLUMN_NAME"));
            					setfield = setfield+"," + schemas.getString("COLUMN_NAME") + " = " + Encode;
            			}
            			else
            			{
            					Encode = (String) multi.getParameter(schemas.getString("COLUMN_NAME"));
            					setfield = setfield+"," + schemas.getString("COLUMN_NAME") + " = '" + Encode +"'";
            			}

	            	}

	            }
	            setfield = setfield.replaceFirst(",", "");
	            Statement stmt = new Multitenantstatement(con,"ExtensionTable");
	            String updatesql = "UPDATE CourseInfo SET "+setfield+" WHERE CourseId = '"+multi.getParameter("CourseId")+"'";
	            //out.println(" "+updatesql);
	            ((Multitenantstatement) stmt).settenantId(TenantName);
	            stmt.executeUpdate(updatesql);
	            stmt.close();

	            CourseId = multi.getParameter("CourseId");



	            String sql = "SELECT * FROM CourseInfo WHERE CourseId = '"+CourseId+"'";

	            stmt = new Multitenantstatement(con,"ExtensionTable");
	            ((Multitenantstatement) stmt).settenantId(TenantName);
	            ResultSet rs = (ResultSet) stmt.executeQuery(sql);


	            ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();
	        	int numColumns = rsmd.getColumnCount();




	        	while(rs.next())
	        	{


	        		rsmd = (ResultSetMetaData) rs.getMetaData();

	        		for (int i=1; i <numColumns+1; i++)
	        		{
	        			if(rsmd.getColumnName(i).equals("CourseId"))
	        			{

	        				out.print("<h2>"+rsmd.getColumnName(i)+" : "+rs.getObject(rsmd.getColumnName(i))+" </h2>");
	        				//out.print("<h2>""</h2>");
	        				out.println("<table class= table >");

	        			}
	        			else if(rsmd.getColumnName(i).equals("CourseId"))
	        			{

	        			}
	        			else
	        			{
	        				out.println("<tr>");
	        				out.print("<td>");
	        				out.print(rsmd.getColumnName(i));
	        				out.println("</td>");
	        			    out.print("<td>");
	        				out.print(rs.getObject(rsmd.getColumnName(i)));
	        				out.println("</td>");
	        				out.println("</tr>");
	        			}








	        		}


	        		out.println("</table>");

	        	}


	            stmt.close();
	            con.close();



			}
			else
			{
				response.sendRedirect("tenant.jsp");

			}



	%>


        </div><!--/span-->
      </div><!--/row-->

      <hr>



    </div>




<script>
function redirect(name,page)
{
	//alert(name+" "+page);
	document.form21.TenantName.value=name;
	document.form21.NowPage.value=page;
	//alert(name);
	form21.submit();
	//alert(name);


}
</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>