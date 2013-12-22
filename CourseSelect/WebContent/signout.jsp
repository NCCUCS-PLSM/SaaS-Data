<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
</head>
<body>

<%

String TenantName = request.getParameter("TenantName");

session.removeAttribute(TenantName+"AccountName");

//String PageName = (String) session.getAttribute("PageName");
//out.print(TenantName);
response.sendRedirect("welcome.jsp");

%>


</body>
</html>