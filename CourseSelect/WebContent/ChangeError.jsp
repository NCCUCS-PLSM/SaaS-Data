<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
<%@ page import="rewriting.*,com.mysql.jdbc.*,java.sql.DriverManager"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<link rel="stylesheet" href="/force/css/bootstrap.min.css"" type="text/css"></link>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">

<!-- Le styles -->
    <link href="../assets/css/bootstrap.css" rel="stylesheet">
    <style type="text/css">
      body {
        padding-top: 60px;
        padding-bottom: 40px;
      }
      .sidebar-nav {
        padding: 9px 0;
      }
    </style>
    <link href="../assets/css/bootstrap-responsive.css" rel="stylesheet">

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="../assets/ico/favicon.ico">
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

//session.setAttribute("TenantName", TenantName);

String CourseId = request.getParameter("CourseId");
//session.setAttribute("ItemDetailNo", ItemDetailNo);
String AccountName = (String) session.getAttribute(TenantName+"AccountName");
//session.setAttribute("PageName","ItemInfoDetail.jsp?TenantName="+TenantName+"&ItemNo="+ItemNo);

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




          <%


	        Class.forName("com.mysql.jdbc.Driver");
	        Connection con = (Connection) DriverManager.getConnection(
		  		      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
				      "root","9326691");
	    	Statement stmt = new Multitenantstatement(con,"ExtensionTable");

	    	String sql = "SELECT SelectCourse.Priority , CourseInfo.CourseId,CourseInfo.CourseName , CourseInfo.TeacherName , CourseInfo.Location ,CourseInfo.Credit ,CourseInfo.Days,CourseInfo.Time,CourseInfo.Department, SelectCourse.Date  FROM SelectCourse INNER JOIN CourseInfo ON SelectCourse.CourseId = CourseInfo.CourseId WHERE SelectCourse.StudentId = '"+AccountName+"' AND SelectCourse.CourseId = '"+CourseId+"'";
	    	((Multitenantstatement) stmt).settenantId(TenantName);
			ResultSet rs = (ResultSet) stmt.executeQuery(sql);


			while(rs.next())
        	  {
          	  //out.print(piccount);

                out.print("<div class='row-fluid'>");



                //out.print("<div class='span4'><h2>"+rs.getObject("CourseName")+"</h2>");
                //out.print("<div class='span4'><h2>"+rs.getObject("Date")+"</h2>");
                out.print("<p> "+rs.getObject("CourseName")+" 已經在 "+rs.getObject("Date")+" 加入選課清單中");
                out.print("<p><table class='table'>");

                //out.print(rs.getMetaData().getColumnCount());
                for(int i = 1;i <= rs.getMetaData().getColumnCount();i++)
            	{

              	  if(rs.getMetaData().getColumnName(i).equals("IsClosed")||rs.getMetaData().getColumnName(i).equals("Date"))
              	  {

              	  }
              	  else
              	  {
              			out.print("<tr><th>"+rs.getMetaData().getColumnName(i)+":"+rs.getObject(rs.getMetaData().getColumnName(i))+"</th>");
              	  }
            	}

                out.print("<p></table>");

            }

			  stmt.close();
			  con.close();


          %>










      </div><!--/row-->

      <hr>



    </div>
<script>


function gotoshoppingcart()
{



	form7.submit();


}


</script>
</body>
</html>