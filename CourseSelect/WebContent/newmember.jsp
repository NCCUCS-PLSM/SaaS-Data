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




%>


   <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="Index.jsp?TenantName=<%=TenantName %>&NowPage=1"> Online Course Selection</a>
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

	  <div class="span15">
          <div class="hero-unit">
            <h1>Welcome!</h1>
            <p>Welcome to CourseSelect. On this site, I have gathered all the data from the Arts & Science calendar along with course codes, section codes, instructors, times, etc. Hopefully, this will aid you in picking your courses for the upcoming semester.</p>
            <p><a class="btn btn-primary btn-large" href='newmember.jsp' >Let's Join Us Now !!</a></p>
          </div>

        <div class="row">
        <div class="span3">

        </div>
        <div class="span6">


<%


//out.print("----------"+TenantName);
Class.forName("com.mysql.jdbc.Driver");
Connection con = (Connection) DriverManager.getConnection(
	      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
	      "root","9326691");
MultitenantDatabaseMetaData metadata = new MultitenantDatabaseMetaData(con,"ExtensionTable");
ResultSet schemas = (ResultSet) metadata.getColumns("", "", "StudentInfo", "", "Nccu");








out.print("<form action = registerresult.jsp class='form-horizontal' method=post name = form8 id = form8>");

while(schemas.next())
{
	if(schemas.getString("COLUMN_NAME").equals("Password"))
	{
		out.print("<div class= control-group >");
		out.print("<label class= control-label for= inputEmail >"+schemas.getString("COLUMN_NAME")+"</label>");
		out.print("<div class= controls >");
		out.print("<input type= password  id= inputEmail  placeholder= "+schemas.getString("COLUMN_NAME")+" name = "+schemas.getString("COLUMN_NAME")+">");
		out.print("</div>");
		out.print("</div>");

		//out.print("<br>"+schemas.getString("COLUMN_NAME")+": <input type = text name ="+schemas.getString("COLUMN_NAME")+"></br>");
	}
	else
	{
		out.print("<div class= control-group >");
		out.print("<label class= control-label for= inputEmail >"+schemas.getString("COLUMN_NAME")+"</label>");
		out.print("<div class= controls >");
		out.print("<input type= text  id= inputEmail  placeholder= "+schemas.getString("COLUMN_NAME")+" name = "+schemas.getString("COLUMN_NAME")+">");
		out.print("</div>");
		out.print("</div>");

		//out.print("<br>"+schemas.getString("COLUMN_NAME")+": <input type = text name ="+schemas.getString("COLUMN_NAME")+"></br>");
	}


}
out.print("<div class= control-group >");
out.print("<label class= control-label for= inputPassword >School Name</label>");
out.print("<div class= controls >");
Statement stmt = new Multitenantstatement(con,"ExtensionTable");


String sql = "SELECT TenantName,TenantRealName FROM TenantPrivateTable";
ResultSet rs = (ResultSet) stmt.executeQuery(sql);
out.print("<select name = TenantName>");
while(rs.next())
{
	out.print("<option value="+rs.getObject("TenantName")+">"+rs.getObject("TenantRealName")+"</option>");
}
out.print("</select>");
stmt.close();
con.close();



out.print("</div>");
out.print("</div>");

out.print("<div class= control-group >");
out.print("<div class= controls >");
out.print("<input name= NowPage  value='' type= hidden >");
out.print("<button type = submit class= btn  onclick = redirect() >Register Now !!</button>");
out.print("</div>");
out.print("</div>");
out.print("</form>");



%>




</div><!--/span-->
      </div><!--/row-->

    </div>

      </div>
      <hr>
      </div>





<script>

function redirect()
{
	form8.submit();


}
</script>

    <script src="/force/js/bootstrap.min.js"></script>

</body>
</html>