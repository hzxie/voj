<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title>Welcome Back | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/login.css" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome-ie7.min.css" />
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='../not-supported';
        </script>
    <![endif]-->
</head>
<body>
    <!-- Header -->
    <%@ include file="/WEB-INF/views/include/header.jsp" %>
    <!-- Content -->
    <div id="content">
        <div id="login">
            <h2>Sign in</h2>
            <div class="alert alert-error hide">Incorrect username or password.</div>
            <c:if test="${isLogout}">
            <div class="alert alert-success">You are now logged out.</div>
            </c:if>
            <form id="login-form" method="post" onsubmit="onSubmit(); return false;">
                <p class="row-fluid">
                    <label for="username">Username</label>
                    <input id="username" name="username" class="span12" type="text" maxlength="16" />
                </p>
                <p class="row-fluid">
                    <label for="password">Password</label>
                    <input id="password" name="password" class="span12" type="password" maxlength="16" />
                </p>
                <p>
                    <label for="remember-me">
                        <input type="checkbox" /> Remember Me
                    </label>
                </p>
                <p>
                    <button class="btn btn-primary btn-block" type="submit">Sign in</button>
                </p>
            </form> <!-- #login-form -->
        </div> <!-- #login -->
        <p class="text-center">
            Don't have an account?<br /><a href="<c:url value="/accounts/register" />">Create an account</a>
        </p>
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-success').hide();
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('Please wait...');
            
            var username = $('#username').val(),
                password = $('#password').val();
            
            return doLoginAction(username, password);
        };
    </script>
    <script type="text/javascript">
        function doLoginAction(username, password) {
            var postData = {
                'username': username,
                'password': password
            };
            
            $.ajax({
                type: 'POST',
                url: '<c:url value="/accounts/login.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processLoginResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processLoginResult(result) {
            if ( result['isSuccessful'] ) {
                window.location.href='<c:url value="/" />';
            } else {
                $('#password').val('');
                $('.alert-error').removeClass('hide');
            }

            $('button[type=submit]').html('Sign in');
            $('button[type=submit]').removeAttr('disabled');
        }
    </script>
</body>
</html>