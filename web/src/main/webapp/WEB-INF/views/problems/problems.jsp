<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.problems.problems.title" text="Problems" /> | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/problems/problems.css" />
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
        <div id="locator">
            <ul class="inline">
                <li><spring:message code="voj.problems.problems.locator" text="Locator" />:</li>
                <c:forEach var="locatorID" begin="${startIndexOfProblems}" end="${startIndexOfProblems + totalProblems}" step="${numberOfProblemsPerPage}">
                <li><a href="<c:url value="/p?start=${locatorID}" />">P${locatorID}</a></li>
                </c:forEach>
            </ul>
        </div> <!-- #locator -->
        <div id="main-content" class="row-fluid">
            <div id="problems" class="span8">
                <table class="table table-striped">
                    <thead>
                        <tr>
                        <c:if test="${isLogin}">
                            <th class="flag"><spring:message code="voj.problems.problems.result" text="Result" /></th>
                        </c:if>
                            <th class="name"><spring:message code="voj.problems.problems.name" text="Name" /></th>
                            <th class="submission"><spring:message code="voj.problems.problems.submission" text="Submission" /></th>
                            <th class="ac-rate">AC%</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="problem" items="${problems}">
                        <tr>
                        <c:if test="${isLogin}">
                            <c:choose>
                                <c:when test="${submissionOfProblems[problem.problemId] == null}"><td></td></c:when>
                                <c:otherwise>
                                    <td class="flag-${submissionOfProblems[problem.problemId].judgeResult.judgeResultSlug}">
                                        <a href="<c:url value="/submission/${submissionOfProblems[problem.problemId].submissionId}" />">
                                            ${submissionOfProblems[problem.problemId].judgeResult.judgeResultSlug}
                                        </a>
                                    </td>
                                </c:otherwise>
                            </c:choose>
                        </c:if>
                            <td class="name"><a href="<c:url value="/p/${problem.problemId}" />">P${problem.problemId} ${problem.problemName}</a></td>
                            <td>${problem.totalSubmission}</td>
                            <td>
                            <c:choose>
                                <c:when test="${problem.totalSubmission == 0}">0%</c:when>
                                <c:otherwise>
                                    <fmt:formatNumber type="number"  maxFractionDigits="0" value="${problem.acceptedSubmission * 100 / problem.totalSubmission}" />%
                                </c:otherwise>
                            </c:choose>
                            </td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <div id="more-problems">
                    <p class="availble"><spring:message code="voj.problems.problems.more-problems" text="More Problems..." /></p>
                    <img src="${cdnUrl}/img/loading.gif" alt="Loading" class="hide" />
                </div>
            </div> <!-- #problems -->
            <div id="sidebar" class="span4">
                <div id="search-widget" class="widget">
                    <h4><spring:message code="voj.problems.problems.search" text="Search" /></h4>
                    <form id="search-form" action="#">
                    </form>
                </div> <!-- #search-widget -->
                <div id="filter-widget" class="widget">
                    <h4><spring:message code="voj.problems.problems.filter" text="Filter" /></h4>
                </div> <!-- #filter-widget -->
            </div> <!-- #sidebar -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
</body>
</html>