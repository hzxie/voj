<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.new-user.title" text="New User" /> | ${websiteName}</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="${description}">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon" type="image/x-icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/new-user.css" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/flat-ui.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/pace.min.js"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <script type="text/javascript"> 
            window.location.href='<c:url value="/not-supported" />';
        </script>
    <![endif]-->
</head>
<body>
    <div id="wrapper">
        <!-- Sidebar -->
        <%@ include file="/WEB-INF/views/administration/include/sidebar.jsp" %>
        <div id="container">
            <!-- Header -->
            <%@ include file="/WEB-INF/views/administration/include/header.jsp" %>
            <!-- Content -->
            <div id="content">
                <h2 class="page-header"><i class="fa fa-user"></i> <spring:message code="voj.administration.new-user.new-user" text="New User" /></h2>
                <form id="profile-form" onSubmit="onSubmit(); return false;">
                    <div class="alert alert-error hide"></div> <!-- .alert-error -->
                    <div class="alert alert-success hide"><spring:message code="voj.administration.add-user.user-created" text="The user has been created successfully." /></div> <!-- .alert-success -->
                    <div class="control-group row-fluid">
                        <label for="username"><spring:message code="voj.administration.add-user.username" text="Username" /></label>
                        <input id="username" class="span12" type="text" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="password"><spring:message code="voj.administration.add-user.password" text="Password" /></label>
                        <input id="password" class="span12" type="password" maxlength="16" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="email"><spring:message code="voj.administration.add-user.email" text="Email" /></label>
                        <input id="email" class="span12" type="text" maxlength="64" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="user-group"><spring:message code="voj.administration.add-user.user-group" text="User Group" /></label>
                        <select id="user-group" name="userGroup">
                        <c:forEach var="userGroup" items="${userGroups}">
                            <option value="${userGroup.userGroupSlug}">${userGroup.userGroupName}</option>
                        </c:forEach>
                        </select>
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="prefer-language"><spring:message code="voj.administration.add-user.prefer-language" text="Prefer Language" /></label>
                        <select id="prefer-language" name="preferLanguage">
                        <c:forEach var="language" items="${languages}">
                            <option value="${language.languageSlug}">${language.languageName}</option>
                        </c:forEach>
                        </select>
                    </div> <!-- .control-group -->
                    <div class="row-fluid">
                        <div class="span12">
                            <button class="btn btn-primary" type="submit"><spring:message code="voj.administration.add-user.create-account" text="Create Account" /></button>
                        </div> <!-- .span12 -->
                    </div> <!-- .row-fluid -->
                </form> <!-- #profile-form -->
            </div> <!-- #content -->    
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <c:if test="${GoogleAnalyticsCode != ''}">
    <script type="text/javascript">${googleAnalyticsCode}</script>
    </c:if>
</body>
</html>