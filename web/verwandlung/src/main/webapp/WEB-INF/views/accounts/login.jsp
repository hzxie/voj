<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id='DigestUtils' scope='request' class='com.happystudio.voj.util.DigestUtils'/>
<!doctype html>
<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <title>Welcome Back | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link rel="shortcut icon" href="<c:url value="/assets/img/favicon.png" />">
    <!-- CSS -->
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/bootstrap.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/bootstrap-responsive.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/flat-ui.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/font-awesome.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/style.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/login.css" />">
    <!-- JavaScript -->
    <script type="text/javascript" src="<c:url value="/assets/js/jquery-1.11.1.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/assets/js/bootstrap.min.js" />"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="<c:url value="/assets/js/jquery.placeholder.js" />"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/font-awesome-ie7.min.css" />">
        <script type="text/javascript" sec="<c:url value="/assets/js/icon-font-ie7.js" />"></script>
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='../not-supported';
        </script>
    <![endif]-->
</head>
<body>
    <div id="header" class="row-fluid">
        <div class="container">
            <div id="logo" class="span6">
                <a href="<c:url value="/" />">
                    <img src="<c:url value="/assets/img/logo.png" />" alt="Logo">
                </a>
            </div> <!-- #logo -->
            <div id="nav" class="span6">
                <ul class="inline">
                    <li><a href="<c:url value="/p" />">Problems</a></li>
                    <li><a href="<c:url value="/discussion" />">Discussion</a></li>
                    <li><a href="<c:url value="/contests" />">Contests</a></li>
                    <li><a href="<c:url value="/submissions" />">Submission</a></li>
                    <li><a href="javascript:openDrawerMenu()">More</a></li>
                </ul>
            </div> <!-- #nav -->
        </div> <!-- .container -->
    </div> <!-- #header -->
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
    <div id="drawer-nav">
        <span class="pull-right"><a href="javascript:closeDrawerMenu();">Close &times;</a></span>
        <div id="accounts" class="section">
            <h4>My Accounts</h4>
            <div id="profile">
            <c:choose>
            <c:when test="${isLogin}">
                <img src="http://www.gravatar.com/avatar/${DigestUtils.md5Hex(user.getEmail().toLowerCase())}?s=100&amp;d=mm" alt="avatar" class="img-circle" />
                <h5>${user.getUsername()}</h5>
                <p>${user.getEmail()}</p>
                <p>Accepted/Submit: ${submissionStats.get("accpetedSubmission")}/${submissionStats.get("totalSubmission")}(${submissionStats.get("acRate")}%)</p>
                <p>Language Preference: ${user.getPreferLanguage().getLanguageName()}</p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/dashboard" />">Dashboard</a></li>
                    <li><a href="<c:url value="/accounts/login?logout=true" />">Sign out</a></li>
                </ul>
            </c:when>
            <c:otherwise>
                <p>You are not logged in.</p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/login" />">Sign in</a></li>
                    <li><a href="<c:url value="/accounts/register" />">Sign up</a></li>
                </ul>
            </c:otherwise>
            </c:choose>
            </div> <!-- #profile -->
        </div> <!-- .section -->
        <div id="about" class="section">
            <h4>About</h4>
            <ul>
                <li><a href="#">Judgers</a></li>
                <li><a href="#">Feedback</a></li>
                <li><a href="#">About Us</a></li>
            </ul>
        </div> <!-- .section -->
    </div> <!-- #drawer-nav -->
    <div id="footer">
        <div class="container">
            <p id="copyright">Copyright&copy; 2005-2014 <a href="http://www.zjhzxhz.com/" target="_blank">HApPy Studio</a>. All rights reserved.</p>
        </div> <!-- .container -->
    </div> <!-- #footer -->
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="<c:url value="/assets/js/drawer-menu.js" />"></script>
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-success').hide();
            
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('Please wait...');
            
            var username = $('#username').val(),
                password = $('#password').val();
            doLoginAction(username, password);
        };
    </script>
    <script type="text/javascript">
        function doLoginAction(username, password) {
            var postData = 'username=' + username + '&password=' + password;
            $.ajax({
                    type: 'POST',
                    url: '<c:url value="/accounts/login.action" />',
                    data: postData,
                    dataType: 'JSON',
                    success: function(result){
                        console.log(result);
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