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
String AccountName = (String) session.getAttribute("AccountName");
//String PageName = "IndexForTenant.jsp";
//session.setAttribute("PageName", PageName);
String CourseId = request.getParameter("CourseId");
//session.setAttribute("ItemDetailNo", ItemDetailNo);

String TenantName = (String) session.getAttribute("TenantName");
String TenantRealName = (String) session.getAttribute("TenantRealName");
//
//session.setAttribute("TenantName", TenantName);

String NowPage = request.getParameter("NowPage");
//request.getParameter("NowPage");
//session.setAttribute("NowPage", NowPage);


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
			out.print("Welcome! "+TenantName+"    <a href = 'signoutfortenant.jsp' class='navbar-link'>  Sign out >></a>");


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
              <li><a href="IndexForTenant.jsp">Course List</a></li>
              <li class="active"><a href="showcloseitem.jsp">Closed Course List</a></li>
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

          <form action = IndexForTenant.jsp method=post name = form16 id = form16>
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

	            Statement stmt = new Multitenantstatement(con,"ExtensionTable");




	            ((Multitenantstatement) stmt).settenantId(TenantName);

	            String sql = "SELECT * FROM CourseInfo WHERE CourseId = '"+CourseId+"'";

	            ResultSet rs = (ResultSet) stmt.executeQuery(sql);


	            while(rs.next())
	        	  {
	          	  //out.print(piccount);

	                //out.print("<div class='row-fluid'>");



	                //out.print("<div class='span4'><h2>"+rs.getObject("CourseName")+"</h2>");
					out.print("<h2>"+rs.getObject("CourseName")+"</h2>");
	                out.print("<p><table class='table'>");
	                out.print("<form action = selectupdate.jsp?TenantName="+TenantName+" method=post name = form7 id = form7>");
	                //out.print(rs.getMetaData().getColumnCount());
	                for(int i = 1;i <= rs.getMetaData().getColumnCount();i++)
	            	{

	              	  if(rs.getMetaData().getColumnName(i).equals("IsClosed"))
	              	  {

	              	  }
	              	  else
	              	  {
	              		out.println("<tr>");
	    				out.print("<td>");
	    				out.print(rs.getMetaData().getColumnName(i));
	    				out.println("</td>");
	    			    out.print("<td>");
	    				out.print(rs.getObject(rs.getMetaData().getColumnName(i)));
	    				out.println("</td>");
	    				out.println("</tr>");
	              	  }
	            	}



	                out.print("<p></table>");


	         		out.print("<p>");
	         		out.print("<input type = hidden name = CourseId value = "+rs.getObject("CourseId")+">");


	         	    //out.print("<p><input type = button value = 'Add Now!! >>' onclick = gotoshoppingcart() class = 'btn'></p></div><!--/span-->");
	                out.print("<p><a href='updateclosedcourseInfoDetail.jsp?CourseId="+rs.getObject("CourseId")+"' class = 'btn'>Update Course Information >></a><p><a href='opencourseinfodetail.jsp?CourseId="+rs.getObject("CourseId")+"' class = 'btn'>Open Course Information >></a></p>");


	                out.print("</form>");
	                out.print("</div>");




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
	document.form16.TenantName.value=name;
	document.form16.NowPage.value=page;
	//alert(name);
	form16.submit();
	//alert(name);


}
</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>