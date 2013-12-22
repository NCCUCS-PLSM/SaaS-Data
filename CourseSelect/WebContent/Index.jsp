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
String TenantName = request.getParameter("TenantName");
String AccountName = (String) session.getAttribute(TenantName+"AccountName");

String NowPage = request.getParameter("NowPage");
if(NowPage==null)
{
	//out.print(NowPage);
	session.removeAttribute(TenantName+"AccountName");
	NowPage = "1";
}
//TenantId = TenantId.replace("\"", "");
session.setAttribute("PageName", "Index.jsp?TenantName="+TenantName+"&NowPage="+NowPage);


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
            <ul class="nav nav-list">
            <li class="nav-header">Course Selection Result Management</li>
              <li><a href="courseselection.jsp?TenantName=<%=TenantName %>">Course Selection Result</a></li>
            </ul>
          </div><!--/.well -->
        </div><!--/span-->


        <div class="span9">




 		  <form action = Index.jsp method=post name = form5 id = form5>
			<input name="TenantName" value="" type="hidden">
			<input name="NowPage" value="" type="hidden">
		  <form>

			<%

			//out.print("----------"+TenantName);
	        Class.forName("com.mysql.jdbc.Driver");
	        Connection con = (Connection) DriverManager.getConnection(
		  		      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
				      "root","9326691");

	        Statement stmt = new Multitenantstatement(con,"ExtensionTable");


	    	String sql = "SELECT Count(CourseId) FROM CourseInfo WHERE IsClosed = 0";
	    	((Multitenantstatement) stmt).settenantId(TenantName);
			ResultSet rs = (ResultSet) stmt.executeQuery(sql);


			int TotalCount = 0;
			while(rs.next())
         	{
         		if(rs.getObject(1) == null)
         		{
         			TotalCount = 0;
         		}
         		else
         		{
         			TotalCount = rs.getInt(1);

         		}

         	}

			int TotalPage = TotalCount / 6;

			if(!((TotalCount % 6)==0))
   		    {
   				TotalPage = TotalPage + 1;
   		    }

			stmt.close();

			out.print("<select onchange = redirect('"+TenantName+"',this.value)>");
     		  out.print("<option selected='selected'>"+NowPage);
     		  for(int i = 1;i <= TotalPage;i++)
     		  {
     			  if(i == Integer.parseInt(NowPage))
     			  {

     			  }
     			  else
     			  {
     				out.print("<option value="+i+">"+i+"</option>");
     			  }

     		  }
     		  out.print("</select>");


     		 stmt = new Multitenantstatement(con,"ExtensionTable");

	    	int OffsetValue = (Integer.parseInt(NowPage)-1) * 6;


			sql = "SELECT CourseId, CourseName, Instructors,Days, Time FROM CourseInfo WHERE IsClosed = 0 LIMIT 6 OFFSET "+Integer.toString(OffsetValue);
			((Multitenantstatement) stmt).settenantId(TenantName);
			rs = (ResultSet) stmt.executeQuery(sql);


			out.print("<div class='row-fluid'>");
              //int piccount = 0;

            ResultSetMetaData rsmd = (ResultSetMetaData) rs.getMetaData();
	            	int numColumns = rsmd.getColumnCount();
	            	//out.print(numColumns);

	            	out.println("<table class= table >");


	                out.print("<tr>");

	                numColumns = rsmd.getColumnCount();
	            	for (int i=1; i<numColumns+1; i++)
	            	{

	            			out.print("<td>");
	            	    	out.print(rsmd.getColumnName(i));
	            	    	out.println("</td>");

	            	}
	            	out.print("<td>");
	            	out.print("</td>");
	                out.println("</tr>");
              while(rs.next())
          	  {



            	  rsmd = (ResultSetMetaData) rs.getMetaData();

      			for (int i=1; i<numColumns+1; i++)
      			{


      					out.print("<td>");
      					out.print(rs.getObject(rsmd.getColumnName(i)));
      					out.println("</td>");



      			}
      			out.print("<td><a href='CourseInfoDetail.jsp?TenantName="+TenantName+"&CourseId="+rs.getObject("CourseId")+"' class = 'btn'>View detail >></a></td>");

      			out.println("</tr>");




                  //out.print("<p><input type = button value = 'View details >>' onclick = redirect('"+rs.getObject("TenantName")+"',1) class = 'btn'></p></div><!--/span-->");


              }
              out.print("</div>");

			  stmt.close();
			  con.close();

           %>







       </div><!--/span-->
      </div><!--/row-->

      <hr>



    </div>


<script>
function redirect(name,page)
{

	//document.form5.TenantName.value=name;
	//document.form5.NowPage.value=page;

	//form5.submit();

	//alert("Index.jsp?TenantName="+name+"&NowPage="+page)
	location.href="Index.jsp?TenantName="+name+"&NowPage="+page;


}
</script>





    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>