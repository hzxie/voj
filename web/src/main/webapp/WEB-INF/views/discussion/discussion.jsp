<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.discussion.discussion.title" text="Discussion" /> | ${WebsiteName}</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="${Description}">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon" type="image/x-icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/discussion/discussion.css" />
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
        <div id="main-content" class="row-fluid">
            <div id="discussion" class="span8">
                <table class="table">
                    <tr class="discussion-threads">
                        <td class="avatar">
                            <img src="${cdnUrl}/img/avatar.jpg" alt="avatar" />
                        </td>
                        <td class="overview">
                            <h5><a href="#">Discussion Threads Demo</a></h5>
                            <ul class="inline">
                                <li><spring:message code="voj.discussion.discussion.author" text="Author" />: <a href="#">zjhzxhz</a></li>
                                <li><spring:message code="voj.discussion.discussion.posted-in" text="Posted in" />: <a href="#">Forum #1</a></li>
                                <li><spring:message code="voj.discussion.discussion.latest-reply" text="Latest reply" />: <a href="#">voj-tester</a> @6 hours ago</li>
                            </ul>
                        </td>
                        <td class="reply-count">0</td>
                    </tr>
                </table> <!-- .table -->
                <div id="more-discussion">
                    <p class="availble"><spring:message code="voj.discussion.discussion.more-discussion" text="More Discussion..." /></p>
                    <img src="${cdnUrl}/img/loading.gif" alt="Loading" class="hide" />
                </div>
            </div> <!-- #discussion -->
            <div id="sidebar" class="span4">
                <div id="topics" class="widget">
                    <h4><spring:message code="voj.discussion.discussion.topics" text="Discussion Topics" /></h4>
                </div> <!-- .widgets -->
            </div> <!-- #sidebar -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    <script type="text/javascript">${GoogleAnalyticsCode}</script>
    </c:if>
</body>
</html>