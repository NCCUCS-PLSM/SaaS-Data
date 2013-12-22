<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
<%@ page import="rewriting.*,com.mysql.jdbc.*,java.sql.DriverManager"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<link rel="stylesheet" href="style.css" type="text/css"></link>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>會員帳號密碼檢查</title>
</head>
<body>
<%
String TenantName = request.getParameter("TenantName");
//String PageName = (String) session.getAttribute("PageName");
String AccountId;
String Password;
AccountId = new String(request.getParameter("AccountName").getBytes("ISO-8859-1"));
Password = new String(request.getParameter("Password").getBytes("ISO-8859-1"));

//out.print("----------"+TenantName);
Class.forName("com.mysql.jdbc.Driver");
Connection con = (Connection) DriverManager.getConnection(
	      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
	      "root","9326691");
Statement stmt = new Multitenantstatement(con,"ExtensionTable");
((Multitenantstatement) stmt).settenantId(TenantName);


String sql = "SELECT StudentId , Password FROM StudentInfo WHERE StudentId = '"+AccountId+"'";
ResultSet rs = (ResultSet) stmt.executeQuery(sql);

if(!(rs.next()))
{
	session.setAttribute("AccountError", "true");
	response.sendRedirect("welcome.jsp");

}
else
{
		if(!(rs.getObject("StudentId").equals(AccountId)))
		{
			session.setAttribute("AccountError", "true");
			response.sendRedirect("welcome.jsp");
		}
		else if(rs.getObject("Password").equals(Password))
		{
			out.print("123");

			session.setAttribute(TenantName+"AccountName", AccountId);
			session.setAttribute("Password", Password);



			response.sendRedirect("Index.jsp?TenantName="+TenantName+"&NowPage=1");


		}
		else
		{
			session.setAttribute("PasswordError", "true");
			response.sendRedirect("welcome.jsp");
		}

}
stmt.close();
con.close();

%>
</body>
</html>