<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5" import = "java.util.Collection,java.util.List,java.util.ArrayList,java.util.Properties"%>
<%@ page import="rewriting.*,com.mysql.jdbc.*,java.sql.DriverManager"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<link rel="stylesheet" href="style.css" type="text/css"></link>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>商家帳號密碼檢查</title>
</head>
<body>
<%

String TenantName;
String TenantPassword;
//String PageName = (String) session.getAttribute("PageName");
TenantName = new String(request.getParameter("TenantName").getBytes("ISO-8859-1"));
TenantPassword = new String(request.getParameter("TenantPassword").getBytes("ISO-8859-1"));

Class.forName("com.mysql.jdbc.Driver");
Connection con = (Connection) DriverManager.getConnection(
	      "jdbc:mysql://localhost/ExtensionTable?useUnicode=true&characterEncoding=utf-8",
	      "root","9326691");
Statement stmt = new Multitenantstatement(con,"ExtensionTable");


String sql = "SELECT TenantName , Password , TenantRealName FROM TenantPrivateTable WHERE TenantName = '"+TenantName+"'";
ResultSet rs = (ResultSet) stmt.executeQuery(sql);

if(!(rs.next()))
{
	session.setAttribute("TenantError", "true");
	response.sendRedirect("tenant.jsp");
}
else
{

	if(!(rs.getObject("TenantName").equals(TenantName)))
	{
		session.setAttribute("TenantError", "true");
		response.sendRedirect("tenant.jsp");
	}
	else if(rs.getObject("Password").equals(TenantPassword))
	{

		session.setAttribute("TenantName", TenantName);
		session.setAttribute("TenantRealName", rs.getObject("TenantRealName"));
		response.sendRedirect("IndexForTenant.jsp");
	}
	else
	{
		session.setAttribute("PasswordError", "true");
		response.sendRedirect("tenant.jsp");
	}

}
stmt.close();
con.close();
//out.print(AccountId+"   "+Password);
%>
</body>
</html>