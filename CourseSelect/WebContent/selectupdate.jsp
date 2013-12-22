<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "java.text.SimpleDateFormat,java.util.Date,java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
<%@ page import="rewriting.*,com.mysql.jdbc.*,java.sql.DriverManager"%>
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
              <li class="active"><a href="Index.jsp?TenantName=<%=TenantName %>&NowPage=1">View All Course List</a></li>
              <li><a href="searchcourse.jsp?TenantName=<%= TenantName%>">Search Course</a></li>
            </ul>
            <ul class="nav nav-list">
              <li class="nav-header">Course List Management</li>
              <li><a href="checkselect.jsp?TenantName=<%=TenantName %>">View Selected Course List</a></li>
              <li><a href="reorder.jsp?TenantName=<%=TenantName %>">Change Selected Course Priority</a></li>
              <li><a href="removecourse.jsp?TenantName=<%=TenantName%>">Remove Selected Course</a></li>
            </ul>
          </div><!--/.well -->
        </div><!--/span-->


        <div class="span9">


          <form action = ItemInfo.jsp method=post name = form14 id = form14>
			<input name="TenantName" value="" type="hidden">
			<input name="NowPage" value="" type="hidden">
		  <form>


<%

			//out.print("----------"+TenantName);
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = (Connection) DriverManager.getConnection(
		  		      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
				      "root","9326691");


			SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			Date Today = new Date();
			//out.print(Today);
			//String strDate = Today.toString();
			String strDate = sdFormat.format(Today);
			//out.print(strDate);
			String CourseId = new String(request.getParameter("CourseId").getBytes("ISO-8859-1"));
			DatabaseMetaData meta = (DatabaseMetaData) con.getMetaData();
			Statement stmt = new Multitenantstatement(con,"ExtensionTable");
			((Multitenantstatement) stmt).settenantId(TenantName);
			String sql = "SELECT CourseId FROM SelectCourse WHERE StudentId = '"+AccountName+"'";
			ResultSet rs = (ResultSet) stmt.executeQuery(sql);
			int flag = 0;

			while(rs.next())
			{

				if(rs.getObject("CourseId").equals(CourseId))
				{
					flag++;

				}
				else
				{

				}
			}

			stmt.close();

			if(flag==0)
			{
				stmt = new Multitenantstatement(con,"ExtensionTable");
				((Multitenantstatement) stmt).settenantId(TenantName);
				sql = "SELECT Max(SelectId) FROM SelectCourse";
				rs = (ResultSet) stmt.executeQuery(sql);

				int OrderDetailIdNum = 0;

				while(rs.next())
				{

					if(rs.getObject(1) == null)
					{
						OrderDetailIdNum = 1;
					}
					else
					{
						OrderDetailIdNum = rs.getInt(1) + 1;
					}
				}

				stmt.close();

				stmt = new Multitenantstatement(con,"ExtensionTable");
				((Multitenantstatement) stmt).settenantId(TenantName);

				sql = "SELECT Max(Priority) FROM SelectCourse WHERE StudentId = '"+AccountName+"'";
				rs = (ResultSet) stmt.executeQuery(sql);

				int Priority = 0;

				while(rs.next())
				{

					if(rs.getObject(1) == null)
					{
						Priority = 1;
					}
					else
					{
						Priority = rs.getInt(1) + 1;
					}
				}

				stmt.close();
				Statement stmt2 = new Multitenantstatement(con,"ExtensionTable");
				((Multitenantstatement) stmt2).settenantId(TenantName);

				String insertsql = "INSERT INTO SelectCourse (SelectId, StudentId, CourseId, SelectDate, Priority) VALUES ("+OrderDetailIdNum+", "+"'"+AccountName+"', '"+CourseId+"', '"+strDate+"', "+Priority+")";
				out.print(insertsql+TenantName);
				stmt2.executeUpdate(insertsql);
				stmt2.close();
				con.close();
				response.sendRedirect("checkselect.jsp?TenantName="+TenantName);
			}
			else
			{
				response.sendRedirect("InsertError.jsp?TenantName="+TenantName+"&CourseId="+CourseId);
			}






















/*

			out.println("<h2>Order Details</h2>");


			stmt = (Statement) con.createStatement();


			sql = "SELECT * FROM OrderDetail INNER JOIN ItemInfo ON OrderDetail.ItemNo = ItemInfo.ItemNo WHERE OrderDetail.OrderNo = "+OrderDetailIdNum;

			stmt.settenantId(TenantName);
			rs = (ResultSet) stmt.executeQuery(sql);


			ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();
			int numColumns = rsmd.getColumnCount();


			out.println("<table class='table'>");
			while(rs.next())
			{

				rsmd = (ResultSetMetaData) rs.getMetaData();

				for (int i=1; i <numColumns+1; i++)
				{
					//out.print(rsmd.getColumnName(i));
					if(rsmd.getColumnName(i).equals("OrderNo")||rsmd.getColumnName(i).equals("ItemNo")||rsmd.getColumnName(i).equals("Picture")||rsmd.getColumnName(i).equals("TenantName")||rsmd.getColumnName(i).equals("IsClosed"))
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

				out.println("<tr>");
				out.print("<td>Picture</td>");

				//out.println("</td>");
			    out.print("<td>");
			    out.print("<img class='img-rounded' src='Image/"+rs.getObject("ItemInfo.Picture")+"' width='300'>");
				//out.print();
				out.println("</td>");
				out.println("</tr>");

				out.println("</table>");



			}


*/

			//stmt.close();
















	/*
			out.print("<div class='span4'><h2>"+rs.getObject("ItemName")+"</h2>");
            out.print("<img src='Image/"+rs.getObject("Picture")+"' width='40'>");
            out.print("<p><table class='table'>");


            out.print("<tr><th>ItemName:"+rs.getObject("ItemName")+"</th>");
            out.print("<input type = hidden name = TenantNo value = "+TenantName+">");
            out.print("<tr><th>Price:"+rs.getObject("Price")+"</th>");
            out.print("<p></table>");

*/
%>


        </div><!--/span-->
      </div><!--/row-->

      <hr>



    </div>





<script>
function redirect(name,page)
{
	//alert(name+" "+page);
	document.form14.TenantName.value=name;
	document.form14.NowPage.value=page;
	//alert(name);
	form14.submit();
	//alert(name);


}
</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>