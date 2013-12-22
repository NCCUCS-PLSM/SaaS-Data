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
//String PageName = "addfield.jsp";
//session.setAttribute("PageName", PageName);

String TenantName = (String) session.getAttribute("TenantName");
String TenantRealName = (String) session.getAttribute("TenantRealName");

String NowPage = request.getParameter("NowPage");



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
              <li><a href="IndexForTenant.jsp">Course List</a></li>
              <li><a href="showcloseitem.jsp">Closed Course List</a></li>
              <li><a href="addproduct.jsp">Add Course information</a></li>
              <li class="nav-header">Information Column Management</li>
              <li><a href="addfield.jsp">Add Customized Column </a></li>
              <li><a href="updatefield.jsp">Update Customized Column</a></li>
              <li><a href="dropfield.jsp">Remove Customized Column</a></li>
              <li class="nav-header">Course Selection Result Management</li>
              <li class="active"><a href="courseselection.jsp">Course Selection Result</a></li>
            </ul>
          </div><!--/.well -->
        </div><!--/span-->


        <div class="span9">

          <%
          if(TenantName!=null)
		  {
        	  //out.print("----------"+TenantName);
        	  out.print("<h2>Course Selection Result Search</h2>");
        	  out.print("<form class=form-horizontal action = courseselectionresult.jsp method=post name = courseselectionform>");






              out.print("<div class=control-group>");
           	  out.print("<label class=control-label for=inputPassword>Student Id</label>");
           	  out.print("<div class=controls>");
           	  out.print("<input type=text id=inputPassword placeholder='Student Id' name = idvalue>");
              out.print("</div>");
         	  out.print("</div>");
              out.print("<div class=control-group>");
          	  out.print("<div class=controls>");


          	  out.print("<button type = submit class= btn onclick = submitcourseselectionform()>Search Now >></button></form></div>");
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
function submitcourseselectionform()
{
	//alert(name);

	courseselectionform.submit();
}


</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>