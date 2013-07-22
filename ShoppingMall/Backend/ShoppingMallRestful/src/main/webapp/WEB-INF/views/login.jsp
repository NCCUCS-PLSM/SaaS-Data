<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" %>

 <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Login Page</title>
    </head>
    <body>
        <c:if test="${param.error != null}">
            <h2>Username or password wrong!</h2>
        </c:if>
        <form method="post" action="j_spring_security_check">
            <label>
                username:
            </label>
            <input type="text" name="j_username"/>
            <br/>
            <label>
                password:
            </label>
            <input type="password" name="j_password"/>
            <br/>
            <input type="checkbox" name="_spring_security_remember_me"/>remember me
            <br/>
            <input type="submit"/>
        </form>
    </body>
</html>