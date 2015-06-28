<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.dashboard.title" text="Dashboard" /> | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/dashboard.css" />
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
    <div id="content" class="container">
        <div id="sub-nav">
            <ul class="nav nav-tabs">
                <li><a href="#tab-statistics" data-toggle="tab"><spring:message code="voj.accounts.dashboard.statistics" text="Statistics" /></a></li>
                <li class="active"><a href="#tab-notifications" data-toggle="tab"><spring:message code="voj.accounts.dashboard.notifications" text="Notifications" /></a></li>
                <li><a href="#tab-customize" data-toggle="tab"><spring:message code="voj.accounts.dashboard.customize" text="Customize" /></a></li>
                <li><a href="#tab-messages" data-toggle="tab"><spring:message code="voj.accounts.dashboard.messages" text="Messages" /></a></li>
                <li><a href="#tab-accounts" data-toggle="tab"><spring:message code="voj.accounts.dashboard.accounts" text="Accounts" /></a></li>
            <c:if test="${user.userGroup.userGroupSlug == 'administrators'}">
                <li><a href="<c:url value="/administration" />"><spring:message code="voj.accounts.dashboard.system-administration" text="System Administration" /></a></li>
            </c:if>
            </ul>
        </div> <!-- #sub-nav -->
        <div id="main-content" class="tab-content">
            <div class="tab-pane" id="tab-statistics">
                statistics
            </div> <!-- #tab-statistics -->
            <div class="tab-pane active" id="tab-notifications">
                notifications
            </div> <!-- #tab-notifications -->
            <div class="tab-pane" id="tab-customize">
                customize
            </div> <!-- #tab-customize -->
            <div class="tab-pane" id="tab-messages">
                messages
            </div> <!-- #tab-messages -->
            <div class="tab-pane" id="tab-accounts">
                accounts
            </div> <!-- #tab-accounts -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
</body>
</html>