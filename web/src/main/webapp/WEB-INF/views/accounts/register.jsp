<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.register.title" text="Create Account" /> | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon" type="image/x-icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/register.css" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome-ie7.min.css" />
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
        <div id="register">
            <h2><spring:message code="voj.accounts.register.create-account" text="Create Account" /></h2>
            <div class="alert alert-error hide"></div>
            <form id="register-form" method="post" onsubmit="onSubmit(); return false;">
                <p class="row-fluid">
                    <label for="username"><spring:message code="voj.accounts.register.username" text="Username" /></label>
                    <input id="username" name="username" class="span12" type="text" maxlength="16" />
                </p>
                <p class="row-fluid">
                    <label for="email"><spring:message code="voj.accounts.register.email" text="Email" /></label>
                    <input id="email" name="email" class="span12" type="text" maxlength="64" />
                </p>
                <p class="row-fluid">
                    <label for="password"><spring:message code="voj.accounts.register.password" text="Password" /></label>
                    <input id="password" name="password" class="span12" type="password" maxlength="16" />
                </p>
                <p class="row-fluid">
                    <label for="language-preference"><spring:message code="voj.accounts.register.language-preference" text="Language Preference" /></label>
                    <select id="language-preference" class="span12">
                    <c:forEach var="language" items="${languages}">
                        <option value="${language.languageSlug}">${language.languageName}</option>
                    </c:forEach>
                    </select>
                </p>
                <p>
                    <c:url var="termsUrl" value="/terms" />
                    <c:url var="privacyUrl" value="/privacy" />
                    <spring:message code="voj.accounts.register.agree-terms" 
                        text="By clicking Create Account, I agree to the Terms of Service and Privacy Policy." 
                        arguments="${termsUrl}, ${privacyUrl}" />
                </p>
                <p>
                    <input type="hidden" value="${csrfToken}" />
                    <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.accounts.register.create-account" text="Create Account" /></button>
                </p>
            </form> <!-- #register-form -->
        </div> <!-- #register -->
        <p class="text-center">
            <spring:message code="voj.accounts.register.already-have-account" text="Already have an account?" /><br />
            <a href="<c:url value="/accounts/login" />"><spring:message code="voj.accounts.register.sign-in" text="Sign in" /></a>
        </p>
        </div> <!-- #register -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
</body>
</html>