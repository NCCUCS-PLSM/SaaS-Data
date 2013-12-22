<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
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

//String TenantName = (String) session.getAttribute("TenantName");
String TenantName = request.getParameter("TenantName");
String AccountName = (String) session.getAttribute(TenantName+"AccountName");
String ChangeError = (String) session.getAttribute("ChangeError");


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





<%

Class.forName("com.mysql.jdbc.Driver");
Connection con = (Connection) DriverManager.getConnection(
	      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
	      "root","9326691");



DatabaseMetaData metadata = (DatabaseMetaData) con.getMetaData();
//ResultSet schemas = (ResultSet) metadata.getColumns("", "", "StudentInfo", "", TenantName);
Statement stmt = new Multitenantstatement(con,"ExtensionTable");
((Multitenantstatement) stmt).settenantId(TenantName);

String sql = "SELECT Count(SelectId) FROM SelectCourse WHERE StudentId = '"+AccountName+"'";
ResultSet rs = (ResultSet) stmt.executeQuery(sql);
int PriorityCount = 0;
while(rs.next())
	{
		if(rs.getObject(1) == null)
		{
			PriorityCount = 0;
		}
		else
		{
			PriorityCount = rs.getInt(1);

		}

	}


stmt.close();
stmt = new Multitenantstatement(con,"ExtensionTable");
((Multitenantstatement) stmt).settenantId(TenantName);
sql = "SELECT SelectCourse.SelectId,SelectCourse.Priority,CourseInfo.CourseId,CourseInfo.CourseName , CourseInfo.Instructors , CourseInfo.Credit ,CourseInfo.Days,CourseInfo.Time, SelectCourse.SelectDate  FROM SelectCourse INNER JOIN CourseInfo ON SelectCourse.CourseId = CourseInfo.CourseId WHERE SelectCourse.StudentId = '"+AccountName+"' ORDER BY SelectCourse.Priority ASC";

rs = (ResultSet) stmt.executeQuery(sql);

if(!(rs.next()))
{
	out.print("<h2>No Course Detail!!</h2>");
}
else
{
	//out.print("<select class= span1>");

	//out.print("</select>");
	rs.prev();
	ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();
	int numColumns = rsmd.getColumnCount();

	out.print("<form method=post action = 'reorderresult.jsp?TenantName="+TenantName+"' name = form20 id = form20>");
    out.println("<table class= table >");


	out.print("<tr>");

	for (int i=2; i<numColumns+1; i++)
	{

			out.print("<td>");
	    	out.print(rsmd.getColumnName(i));
	    	out.println("</td>");

	}
    out.println("</tr>");

	int Priority = 0;


    while(rs.next())
	{
    	Priority++;

    	out.print("<tr>");
  	  	rsmd = (ResultSetMetaData) rs.getMetaData();

		for (int i=1; i<numColumns+1; i++)
		{

			if(rsmd.getColumnName(i).equals("SelectId"))
			{
				String name = "text"+rs.getObject("SelectId");
				String value = rs.getObject("Priority").toString();

				out.print("<td width = 100>");


		    	out.print("<select class = span6 name = "+name+" >");
				out.print("<option selected='selected'>"+Priority);
				for(int j = 1;j <= PriorityCount;j++)
				{
					  out.print("<option value="+j+">"+j+"</option>");
				}
				out.print("</select>");
				out.println("</td>");


			}
			else if(rsmd.getColumnName(i).equals("Priority"))
			{

			}
			else
			{
				out.print("<td>");
				out.print(rs.getObject(rsmd.getColumnName(i)));
				out.println("</td>");
			}







		}
		out.println("</tr>");
		//out.print("<input type = hidden name = tat value = "+TenantName+">");


    }
    out.print("<input type = hidden name = Tenant value = "+TenantName+">");
	out.print("</form>");
    out.print("</div>");

	}

out.println("</table>");



if(ChangeError!=null)
{
		out.print("<p><input type = button value = 'Change Order Now >>' onclick = redirect('"+TenantName+"') class = 'btn'>");
		out.print("   The priority is error !!</p>");
		session.removeAttribute("ChangeError");

}
else
{
	out.print("<p><input type = button value = 'Change Order Now >>' onclick = redirect('"+TenantName+"') class = 'btn'></p>");
}








stmt.close();
con.close();

%>


        </div><!--/span-->
      </div><!--/row-->

      <hr>



    </div>

<script>
function redirect(name)
{

	//document.form5.TenantName.value=name;
	//document.form5.NowPage.value=page;

	form20.submit();

	//alert("Index.jsp?TenantName="+name+"&NowPage="+page)
	//location.href="reorderresult.jsp?TenantName="+name;


}
</script>






    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>