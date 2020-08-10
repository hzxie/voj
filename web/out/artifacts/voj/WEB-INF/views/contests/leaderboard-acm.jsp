<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title>${contest.contestName} <spring:message code="voj.contests.leaderboard.leaderboard" text="Leaderboard" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/contests/leaderboard.css?v=${version}" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js?v=${version}"></script>
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
    <div id="content" class="container">
        <div class="row-fluid">
            <div class="span12">
                <div class="leaderboard">
                    <div class="header">
                        ${contest.contestName} <spring:message code="voj.contests.leaderboard.leaderboard" text="Leaderboard" />
                    </div> <!-- .header -->
                    <div class="body">
                        <table class="table table-striped">
                            <thead>
                                <th class="rank"><spring:message code="voj.contests.leaderboard.rank" text="Rank" /></th>
                                <th class="contestant"><spring:message code="voj.contests.leaderboard.contestant" text="Contestant" /></th>
                                <th class="score"><spring:message code="voj.contests.leaderboard.solved" text="Solved" /></th>
                                <th class="time"><spring:message code="voj.contests.leaderboard.penalty" text="Penalty" /></th>
                            <c:forEach var="problem" items="${problems}">
                                <th class="submission problem-${problem.problemId}"><a href="<c:url value="/contest/${contest.contestId}/p/${problem.problemId}" />" target="_blank">P${problem.problemId}</a></th>
                            </c:forEach>
                            </thead>
                            <tbody>
                            <c:forEach var="contestant" items="${contestants}">
                                <tr>
                                    <td class="rank">${contestant.rank}</td>
                                    <td class="contestant"><a href="<c:url value="/accounts/user/${contestant.contestant.uid}" />" target="_blank">${contestant.contestant.username}</a></td>
                                    <td class="score">${contestant.score}</td>
                                    <td class="time"><fmt:formatNumber pattern="00" value="${contestant.time % 3600 / 3600}" />:<fmt:formatNumber pattern="00" value="${contestant.time / 60 % 60}" />:<fmt:formatNumber pattern="00" value="${contestant.time % 60}" /></td>
                                <c:forEach var="problem" items="${problems}">
                                    <td class="submission problem-${problem.problemId}">
                                    <c:set var="submission" value="${submissions[contestant.contestant.uid][problem.problemId]}" />
                                    <c:choose>
                                    <c:when test="${submission == null}">-</c:when>
                                    <c:otherwise>
                                        <a href="<c:url value="/submission/${submission.submissionId}" />" target="_blank"><fmt:formatNumber pattern="00" value="${submission.usedTime / 3600}" />:<fmt:formatNumber pattern="00" value="${submission.usedTime / 60 % 60}" />:<fmt:formatNumber pattern="00" value="${submission.usedTime % 60}" /></a>
                                    </c:otherwise>
                                    </c:choose>
                                    </td>
                                </c:forEach>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div> <!-- .body -->
                </div> <!-- .leaderboard -->
            </div> <!-- .span12 -->
        </div> <!-- .row-fluid -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript">
        $(window).scroll(function() {
            var offset = $('table').offset().top - $('thead').outerHeight() - $(window).scrollTop();

            if ( offset <= 0 ) {
                $('thead').css('position', 'fixed');
            <c:forEach var="problem" items="${problems}">
                $('th.problem-${problem.problemId}').width($('td.problem-${problem.problemId}').width());
            </c:forEach>
            } else {
                $('thead').css('position', 'relative');
            }
        });
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>
