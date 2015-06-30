<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.index.title" text="System Administration" /> | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/dashboard.css" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/pace.min.js"></script>
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
    <div id="wrapper">
        <!-- Sidebar -->
        <%@ include file="/WEB-INF/views/administration/include/sidebar.jsp" %>
        <div id="container">
            <!-- Header -->
            <%@ include file="/WEB-INF/views/administration/include/header.jsp" %>
            <!-- Content -->
            <div id="content">
                <h2 class="page-header">
                    <i class="fa fa-dashboard"></i> <spring:message code="voj.administration.index.page-header" text="Dashboard" />
                </h2>
                <div id="overview" class="row-fluid">
                    <div class="span3">
                        <div id="overview-users" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-users"></i>
                                </div> <!-- .span4 -->
                                <div class="span8">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.total-users" text="Total Users" /></span>
                                    <h2>0</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <div class="row-fluid">
                                <div class="span6 border-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.new-users-today" text="New Users Today" /></span>
                                    <h4>0</h4>
                                </div> <!-- .span6 -->
                                <div class="span6">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.online-users" text="Online Users" /></span>
                                    <h4>0</h4>
                                </div> <!-- .span6 -->
                            </div> <!-- .row-fluid -->
                        </div> <!-- #overview-users -->
                    </div> <!-- .span3 -->
                    <div class="span3">
                        <div id="overview-problems" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-question-circle"></i>
                                </div> <!-- .span4 -->
                                <div class="span8">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.total-problems" text="Total Problems" /></span>
                                    <h2>0</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <div class="row-fluid">
                                <div class="span6 border-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.this-month" text="Added this Month" /></span>
                                    <h4>0</h4>
                                </div> <!-- .span6 -->
                                <div class="span6">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.private-problems" text="Private Problems" /></span>
                                    <h4>0</h4>
                                </div> <!-- .span6 -->
                            </div> <!-- .row-fluid -->
                        </div> <!-- #overview-problems -->
                    </div> <!-- .span3 -->
                    <div class="span3">
                        <div id="overview-contests" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-paperclip"></i>
                                </div> <!-- .span4 -->
                                <div class="span8 text-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.upcoming-contests" text="Upcoming Contests" /></span>
                                    <h2>0</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <a href="<c:url value="/administration/all-contests" />" class="more">
                                <spring:message code="voj.administration.index.more-contests" text="More Contests" /> <i class="fa fa-arrow-circle-right"></i> 
                            </a>
                        </div> <!-- #overview-contests -->
                    </div> <!-- .span3 -->
                    <div class="span3">
                        <div id="overview-submissions" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-code"></i>
                                </div> <!-- .span4 -->
                                <div class="span8 text-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.submissions-today" text="Submissions Today" /></span>
                                    <h2>0</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <a href="<c:url value="/administration/all-submissions" />" class="more">
                                <spring:message code="voj.administration.index.more-submissions" text="More Submissions" /> <i class="fa fa-arrow-circle-right"></i> 
                            </a>
                        </div> <!-- #overview-submissions -->
                    </div> <!-- .span3 -->
                </div> <!-- #overview -->
                <div class="row-fluid">
                    <div class="span8">
                        Chart
                    </div> <!-- .span8 -->
                    <div class="span4">
                        System Status
                    </div> <!-- .span4 -->
                </div> <!-- .row-fluid -->
                </div> <!-- .row-fluid -->
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
</body>
</html>