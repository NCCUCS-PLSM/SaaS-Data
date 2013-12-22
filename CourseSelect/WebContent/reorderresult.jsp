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
String TenantName = request.getParameter("TenantName");
String AccountName = (String) session.getAttribute(TenantName+"AccountName");



%>


   <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="Index.jsp?TenantName=<%=TenantName %>&NowPage=1"><%=TenantName %> Online Course Selection</a>
          <div class="nav-collapse collapse">
            <p class="navbar-text pull-right">
		<%

		if(AccountName!=null)
		{
			//out.print("Welcome! <a class='navbar-link'>"+AccountName+"</a>");
			out.print("Welcome! "+AccountName+" <a href = 'signout.jsp?TenantName="+TenantName+"' class='navbar-link'>    Sign out >></a>");


		}
		else
		{
			out.print("Logged in as <a href='welcome.jsp?TenantName="+TenantName+"' class='navbar-link'>Username</a>");
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
              <li class="nav-header">Select Course Management</li>
              <li><a href="Index.jsp?TenantName=<%=TenantName %>&NowPage=1">View All Course List</a></li>
              <li><a href="searchcourse.jsp?TenantName=<%= TenantName%>">Search Course</a></li>
            </ul>
            <ul class="nav nav-list">
              <li class="nav-header">Course List Management</li>
              <li><a href="checkselect.jsp?TenantName=<%=TenantName %>">View Selected Course List</a></li>
              <li class="active"><a href="reorder.jsp?TenantName=<%=TenantName %>">Change Selected Course Priority</a></li>
              <li><a href="removecourse.jsp?TenantName=<%=TenantName%>">Remove Selected Course</a></li>
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

				DatabaseMetaData meta = (DatabaseMetaData) con.getMetaData();
				Statement stmt = new Multitenantstatement(con,"ExtensionTable");
				((Multitenantstatement) stmt).settenantId(TenantName);
				String sql = "SELECT Priority , SelectId FROM SelectCourse WHERE StudentId = '"+AccountName+"'";

				ResultSet rs = (ResultSet) stmt.executeQuery(sql);
				Set<String> set = new HashSet<String>();
				boolean continueflag = true;
				while(rs.next()&&continueflag)
	            {

	            	String name = "text"+rs.getObject("SelectId").toString();
	            	continueflag = set.add(request.getParameter(name));

	            }
				out.print(continueflag);
				if(continueflag)
				{
					rs.beforeFirst();
					while(rs.next())
		            {

		            	String name = "text"+rs.getObject("SelectId").toString();
		            	stmt = new Multitenantstatement(con,"ExtensionTable");
		            	((Multitenantstatement) stmt).settenantId(TenantName);
		    			sql = "UPDATE SelectCourse SET Priority = "+request.getParameter(name)+" WHERE SelectId = "+rs.getObject("SelectId") + " AND StudentId = '"+AccountName+"'";
		    			//out.print(sql);
		    			stmt.executeUpdate(sql);
		    			stmt.close();

		            }




		            stmt.close();
		            con.close();
		            response.sendRedirect("checkselect.jsp?TenantName="+TenantName);
				}
				else
				{
					con.close();
					session.setAttribute("ChangeError", "true");
					response.sendRedirect("reorder.jsp?TenantName="+TenantName);
				}




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