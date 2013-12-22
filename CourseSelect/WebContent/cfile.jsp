<%@ page language="java" contentType="text/html; charset=BIG5"
    pageEncoding="BIG5"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.oreilly.servlet.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=BIG5">
<title>Insert title here</title>
</head>
<%
    // 宣告將上傳之檔案放置到伺服器的C:\Upload目錄中
    // 宣告限制上傳之檔案大小為 5 MB
    String saveDirectory = "D://NewJava/force/WebContent/Image";
    int    maxPostSize = 5 * 1024 * 1024 ;

    // 宣告上傳檔案名稱
    String FileName = null;

    // 宣告上傳檔案型態
    String ContentType = null;

    // 宣告敘述上傳檔案內容敘述
    String Description = null;

    //  計算上傳檔案之個數
    int count = 0 ;

    // 宣告上傳檔案檔名所使用的編碼，預設值為 ISO-8859-1，
    // 若改為Big5或MS950則支援中文檔名
    String enCoding = "big5";

    // 產一個新的MultipartRequest 的物件，multi
    MultipartRequest multi = new MultipartRequest(request, saveDirectory, maxPostSize, enCoding);
%>
<body>
<%
    //  取得所有上傳之檔案輸入型態名稱及敘述
    Enumeration filesname = multi.getFileNames();
    Enumeration filesdc = multi.getParameterNames();

    while (filesname.hasMoreElements())
    {
        String name = (String)filesname.nextElement();
        String dc = (String)filesdc.nextElement();
        FileName = multi.getFilesystemName(name);
        ContentType = multi.getContentType(name);
        Description = multi.getParameter(dc);

        if (FileName != null)
        {
            count ++;

%>
<font color="red">你上傳的第<%= count %>個的檔案：</font><br>
檔案名稱為：<%= FileName %><br>
檔案型態為：<%= ContentType %><br>
檔案的敘述：<%= Description %><br><br>

<%
         } // end if
    } // end while
%>
您總共上傳<font color="red"><%= count %></font>個檔案
</body>
</html>