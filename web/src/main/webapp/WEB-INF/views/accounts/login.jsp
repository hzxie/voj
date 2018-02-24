<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.login.title" text="Welcome Back" /> | ${websiteName}</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="${description}">
    <meta name="author" content="Haozhe Xie">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico?v=${version}" rel="shortcut icon" type="image/x-icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/login.css?v=${version}" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/flat-ui.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js?v=${version}"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js?v=${version}"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome-ie7.min.css?v=${version}" />
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='<c:url value="/not-supported" />';
        </script>
    <![endif]-->
</head>
<body>
    <!-- Header -->
    <%@ include file="/WEB-INF/views/include/header.jsp" %>
    <!-- Content -->
    <div id="content">
        <div id="login">
            <h2><spring:message code="voj.accounts.login.sign-in" text="Sign in" /></h2>
            <div class="alert alert-error hide"></div>
            <c:if test="${isLogout}">
            <div class="alert alert-success"><spring:message code="voj.accounts.login.sign-out" text="You are now logged out." /></div>
            </c:if>
            <form id="login-form" method="POST" onsubmit="onSubmit(); return false;">
                <p class="row-fluid">
                    <label for="username"><spring:message code="voj.accounts.login.username" text="Username or Email" /></label>
                    <input id="username" name="username" class="span12" type="text" maxlength="32" />
                </p>
                <p class="row-fluid">
                    <label for="password">
                        <spring:message code="voj.accounts.login.password" text="Password" />
                        <span class="pull-right">
                            <a href="<c:url value="/accounts/reset-password" />">
                                <spring:message code="voj.accounts.login.forgot-password" text="Forgot password?" />
                            </a>
                        </span>
                    </label>
                    <input id="password" name="password" class="span12" type="password" maxlength="16" />
                </p>
                <p>
                    <label class="checkbox" for="remember-me">
                        <input id="remember-me" type="checkbox" data-toggle="checkbox" /> <spring:message code="voj.accounts.login.remember-me" text="Remember Me" />
                    </label>
                </p>
                <p>
                    <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.accounts.login.sign-in" text="Sign in" /></button>
                </p>
            </form> <!-- #login-form -->
        </div> <!-- #login -->
        <p class="text-center">
            <spring:message code="voj.accounts.login.dont-have-account" text="Don't have an account?" /><br />
            <a href="<c:url value="/accounts/register" />"><spring:message code="voj.accounts.login.create-account" text="Create an account" /></a>
        </p>
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-success').addClass('hide');
            $('.alert-error').addClass('hide');
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('<spring:message code="voj.accounts.login.please-wait" text="Please wait..." />');
            
            var username   = $('#username').val(),
                password   = md5($('#password').val()),
                rememberMe = $('input#remember-me').is(':checked');
            
            $('#password').val(password);
            return doLoginAction(username, password, rememberMe);
        };
    </script>
    <script type="text/javascript">
        function doLoginAction(username, password, rememberMe) {
            var postData = {
                'username': username,
                'password': password,
                'rememberMe': rememberMe
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
                var forwardUrl = '${forwardUrl}' || '<c:url value="/" />';
                window.location.href = forwardUrl;
            } else {
                var errorMessage = '';
                if ( !result['isAccountValid'] ) {
                    errorMessage = '<spring:message code="voj.accounts.login.incorrect-password" text="Incorrect username or password." />';
                } else if ( !result['isAllowedToAccess'] ) {
                    errorMessage = '<spring:message code="voj.accounts.login.forbidden-user" text="You&acute;re not allowed to sign in." />';
                }
                $('#password').val('');
                $('.alert-error').html(errorMessage);
                $('.alert-error').removeClass('hide');
            }

            $('button[type=submit]').html('<spring:message code="voj.accounts.login.sign-in" text="Sign in" />');
            $('button[type=submit]').removeAttr('disabled');
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>