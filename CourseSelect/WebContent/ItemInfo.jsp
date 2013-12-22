<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" pageEncoding="BIG5" import = "java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
<%@ page import="rewriting.*,java.sql.*"%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
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
session.setAttribute("TenantName", TenantName);
String NowPage = request.getParameter("NowPage");
session.setAttribute("NowPage", NowPage);
String AccountName = (String) session.getAttribute("AccountName");
//String PageName = request.getParameter("Index.jsp");
session.setAttribute("PageName", "ItemInfo.jsp?TenantName="+TenantName+"&NowPage=1");

%>


   <div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="Index.jsp">PLSM Shopping Store</a>
          <div class="nav-collapse collapse">
            <p class="navbar-text pull-right">
        <%
        if(AccountName!=null)
		{
			//out.print("Welcome! <a class='navbar-link'>"+AccountName+"</a>");
			out.print("Welcome! "+AccountName+" <a href = 'checkorder.jsp' class='navbar-link'>  Check Your Order >></a>    <a href = 'signout.jsp' class='navbar-link'>  Sign out >></a>");


		}
		else
		{
			out.print("Logged in as <a href='customer.jsp' class='navbar-link'>Username</a>");
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
              <li class="nav-header">Store Name</li>
              <li><a href="Index.jsp">Hot Product</a></li>

              <%

              String dbUrl = "local:/tmp/databases/test20120921";
              //ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
              String username = "admin";
              String password = "admin";
              //db.open(username, password);
              Properties info = new Properties();
              info.put("user", username);
              info.put("password", password);
              OrientJdbcDriver Driver = new OrientJdbcDriver();

              OrientJdbcConnection con = (OrientJdbcConnection) Driver.connect("jdbc:orient:" + dbUrl, info);
              OrientJdbcStatement stmt = (OrientJdbcStatement) con.createStatement();
              String sql = "SELECT TenantRealName , TenantName , TenantNo FROM TenantPrivateTable";

              OrientJdbcResultSet rs = (OrientJdbcResultSet) stmt.executeQuery(sql);
//			  int TenantCount = 1;
              while(rs.next())
          	  {
            	  if(TenantName.equals(rs.getString("TenantName")))
            	  {
            		  out.print("<li class='active'><a href='ItemInfo.jsp?TenantName="+rs.getString("TenantName")+"&NowPage=1'>"+rs.getString("TenantRealName")+"</a></li>");
            		 //TenantCount++;
            	  }
            	  else
            	  {
            		  out.print("<li><a href='ItemInfo.jsp?TenantName="+rs.getString("TenantName")+"&NowPage=1'>"+rs.getString("TenantRealName")+"</a></li>");
            		  //TenantCount++;
            	  }

          	  }

              stmt.close();
	%>



            </ul>
          </div><!--/.well -->
        </div><!--/span-->
        <div class="span9">
          <div class="hero-unit">
            <h1>Welcome!</h1>
            <p>Established in June 2012, PLSM is the Taiwan's independent online shopping store. There are over 30 online retailers of books, movies, music and games along with electronics, toys, apparel, sports, tools, groceries and general home and garden items.</p>
            <p><a class="btn btn-primary btn-large" href='newmember.jsp' >Let's Join Us Now !!</a></p>
          </div>
          <form action = ItemInfo.jsp method=post name = form1 id = form1>
			<input name="TenantName" value="" type="hidden">
			<input name="NowPage" value="" type="hidden">
		  <form>

          <%


              stmt = (OrientJdbcStatement) con.createStatement();

              stmt.settenantId(TenantName);

              sql = "SELECT Max(ItemDetailNo) FROM ItemInfo WHERE IsClosed = 'false'";
				//out.print(sql);
           	  rs = (OrientJdbcResultSet) stmt.executeQuery(sql);

           	  int TotalPage = 0;
           	  int TotalCount = 0;
           	  int Count = 0;
           	  while(rs.next())
           	  {
           		if(rs.getObject("Max") == null)
           		{
           			TotalCount = 0;
           		}
           		else
           		{
           			TotalCount = rs.getInt("Max");
           			/*
           			if(!((rs.getInt("Max") % 6)==0))
           			{
           				TotalPage = TotalPage + 1;
           			}
           			*/
           		}
           		//out.print(newId);
           	  }
           	  //out.print(TatalPage);
              stmt.close();


              stmt = (OrientJdbcStatement) con.createStatement();

              stmt.settenantId(TenantName);

              sql = "SELECT Count(ItemDetailNo) FROM ItemInfo WHERE IsClosed = 'true'";

              int TotalDeleteCount = 0;
           	  rs = (OrientJdbcResultSet) stmt.executeQuery(sql);

           	  while(rs.next())
           	  {
           		if(rs.getObject("Count") == null)
           		{
           			TotalDeleteCount = 0;
           		}
           		else
           		{
           			String tmp = rs.getObject("Count").toString();

           			TotalDeleteCount = Integer.parseInt(tmp);
           		}
           		//out.print(newId);
           	  }
           	  //out.print(TatalPage);
              stmt.close();

              Count = TotalCount - TotalDeleteCount;
              TotalPage = Count / 6;
              if(!((Count % 6)==0))
     		  {
     				TotalPage = TotalPage + 1;
     		  }

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



              stmt = (OrientJdbcStatement) con.createStatement();
           	  stmt.settenantId(TenantName);
			  int OffsetValue = (Integer.parseInt(NowPage)-1) * 6;


              sql = "SELECT ItemInfo.Picture , ItemInfo.ItemDetailNo ,ItemInfo.TenantName, ItemInfo.ItemName , ItemInfo.Price FROM ItemInfo WHERE IsClosed = false LIMIT 6 OFFSET "+Integer.toString(OffsetValue);
			  //out.print(sql);
              rs = (OrientJdbcResultSet) stmt.executeQuery(sql);
			  //out.print(rs.getFetchSize());
              int piccount = 0;

              while(rs.next())
          	  {
            	  //out.print(piccount);
            	  piccount++;
            	  if(piccount % 3 == 1)
            	  {
            		  out.print("<div class='row-fluid'>");
            	  }

                  out.print("<div class='span4'><h2>"+rs.getObject("ItemName")+"</h2>");
                  out.print("<img src='Image/"+rs.getObject("Picture")+"' width='40'>");
                  out.print("<p><table class='table'>");


                  out.print("<tr><th>ItemName:"+rs.getObject("ItemName")+"</th>");
                  out.print("<input type = hidden name = TenantNo value = "+TenantName+">");
                  out.print("<tr><th>Price:"+rs.getObject("Price")+"</th>");
                  out.print("<p></table>");




                  out.print("<p><a href='ItemInfoDetail.jsp?TenantName="+rs.getObject("TenantName")+"&ItemDetailNo="+rs.getObject("ItemDetailNo")+"' class = 'btn'>View detail >></a></p></div><!--/span-->");
                  if(piccount % 3 == 0)
            	  {
            		  out.print("</div>");
            	  }

              }


			  stmt.close();
			  con.close();
			  //db.close();

           %>






        </div><!--/span-->
      </div><!--/row-->

      <hr>



    </div>
<script>
function redirect(name,page)
{
	//alert(name+" "+page);
	document.form1.TenantName.value=name;
	document.form1.NowPage.value=page;
	//alert(name);
	form1.submit();
	//alert(name);


}
</script>
</body>
</html>