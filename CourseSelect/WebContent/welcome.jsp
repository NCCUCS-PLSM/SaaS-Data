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
String AccountName = (String) session.getAttribute("AccountName");
String AccountError = (String) session.getAttribute("AccountError");
String PasswordError = (String) session.getAttribute("PasswordError");
//session.setAttribute("PageName", "Customer.jsp");



%>


   <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="welcome.jsp">Online Course Selection</a>
          <div class="nav-collapse collapse">
            <p class="navbar-text pull-right">
		<%
		if(AccountName!=null)
		{
			//out.print("Welcome! <a class='navbar-link'>"+AccountName+"</a>");
			out.print("Welcome! "+AccountName+" <a href = 'checkselect.jsp?TenantName="+TenantName+"' class='navbar-link'>  Check Your Order >></a>    <a href = 'signout.jsp?TenantName="+TenantName+"' class='navbar-link'>  Sign out >></a>");


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



        <div class="span15">
          <div class="hero-unit">
            <h1>Welcome!</h1>
            <p>Welcome to CourseSelect. On this site, I have gathered all the data from the Arts & Science calendar along with course codes, section codes, instructors, times, etc. Hopefully, this will aid you in picking your courses for the upcoming semester.</p>
            <p><a class="btn btn-primary btn-large" href='newmember.jsp' >Let's Join Us Now !!</a></p>
          </div>

      <div class="row">
        <div class="span3">

        </div>
        <div class="span8">




		  <form class="form-horizontal" action = membercheck.jsp? method=post name = form3>
  <div class="control-group">
    <label class="control-label" for="inputEmail">StudentId</label>
    <div class="controls">
      <input type="text" id="inputEmail" placeholder="AccountName" name = AccountName>
      <%
        if(AccountError!=null)
  		{
  			out.print("Account is not correct !!");
  			session.removeAttribute("AccountError");
  			session.removeAttribute("PasswordError");
  		}
      %>
    </div>
  </div>
  <div class="control-group">
    <label class="control-label" for="inputPassword">Password</label>
    <div class="controls">
      <input type="password" id="inputPassword" placeholder="Password" name = Password>
      <%
        if(PasswordError!=null)
  		{
  			out.print("Password is not correct !!");
  			session.removeAttribute("AccountError");
  			session.removeAttribute("PasswordError");
  		}
      %>
    </div>
  </div>


  <div class="control-group">
    <label class="control-label" for="inputPassword">School Name</label>
    <div class="controls">
      <%
        Class.forName("com.mysql.jdbc.Driver");
		Connection con = (Connection) DriverManager.getConnection(
	  		      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
			      "root","9326691");
  	    Statement stmt = new Multitenantstatement(con,"ExtensionTable");


		String sql = "SELECT TenantName,TenantRealName FROM TenantPrivateTable";
		ResultSet rs = (ResultSet) stmt.executeQuery(sql);
		out.print("<select name = TenantName>");
		while(rs.next())
		{
			out.print("<option value="+rs.getObject("TenantName")+">"+rs.getObject("TenantRealName")+"</option>");
		}
		out.print("</select>");



      %>
    </div>
  </div>

  <div class="control-group">
    <div class="controls">

			<input name="NowPage" value="" type="hidden">

	<button type = submit class="btn" onclick = "submitform3()">Sign in</button>

    </div>



  </div>
</form>

			* Do you have a Account ? If you don't !! Let's <a href='newmember.jsp' >Join Us </a>Now !!








        </div><!--/span-->
      </div><!--/row-->
		</div>

      </div>
      <hr>


    </div>





<script>

function submitform3(name)
{

	form3.submit();

}

function redirect(name)
{

	location.href=name+".jsp";

}

</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>