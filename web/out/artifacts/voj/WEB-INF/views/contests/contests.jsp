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
    <title><spring:message code="voj.contests.contests.title" text="Contests" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/contests/contests.css?v=${version}" />
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
        <div id="main-content" class="row-fluid">
            <div id="contests" class="span8">
                <table class="table">
                <c:forEach var="contest" items="${contests}">
                    <c:choose>
                        <c:when test="${currentTime.before(contest.startTime)}">
                            <c:set var="contestStatus" value="Ready" />
                        </c:when>
                        <c:when test="${currentTime.after(contest.endTime)}">
                            <c:set var="contestStatus" value="Done" />
                        </c:when>
                        <c:when test="${currentTime.after(contest.startTime) and currentTime.before(contest.endTime)}">
                            <c:set var="contestStatus" value="Live" />
                        </c:when>
                    </c:choose>
                    <tr class="contest ${contestStatus}">
                        <td class="overview">
                            <h5><a href="<c:url value="/contest/${contest.contestId}" />">${contest.contestName}</a></h5>
                            <ul class="inline">
                                <li>${contest.contestMode}</li>
                                <li><spring:message code="voj.contests.contests.start-time" text="Start Time" />: <fmt:formatDate value="${contest.startTime}" type="both" dateStyle="long" timeStyle="medium" /></li>
                                <li><spring:message code="voj.contests.contests.end-time" text="End Time" />: <fmt:formatDate value="${contest.endTime}" type="both" dateStyle="long" timeStyle="medium" /></li>
                            </ul>
                        </td>
                        <td class="status">${contestStatus}</td>
                    </tr>
                </c:forEach>
                </table> <!-- .table -->
                <div id="more-contests">
                    <p class="availble"><spring:message code="voj.contests.contests.more-contests" text="More Contests..." /></p>
                    <img src="${cdnUrl}/img/loading.gif?v=${version}" alt="Loading" class="hide" />
                </div>
            </div> <!-- #contests -->
            <div id="sidebar" class="span4">
                <div id="search-widget" class="widget">
                    <h4><spring:message code="voj.contests.contests.search" text="Search" /></h4>
                    <form id="search-form" action="<c:url value="/contest" />">
                        <div class="control-group">
                            <input name="keyword" class="span12" type="text" placeholder="<spring:message code="voj.contests.contests.keyword" text="Keyword" />" value="${param.keyword}" />
                        </div>
                        <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.contests.contests.search" text="Search" /></button>
                    </form> <!-- #search-form -->
                </div> <!-- #search-widget -->
            </div> <!-- #sidebar -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/date-${language}.min.js?v=${version}"></script>
    <script type="text/javascript">
        function setLoadingStatus(isLoading) {
            if ( isLoading ) {
                $('p', '#more-contests').addClass('hide');
                $('img', '#more-contests').removeClass('hide');
            } else {
                $('img', '#more-contests').addClass('hide');
                $('p', '#more-contests').removeClass('hide');
            }
        }
    </script>
    <script type="text/javascript">
        $('#more-contests').click(function() {
            var isLoading         = $('img', this).is(':visible'),
                hasNextRecord     = $('p', this).hasClass('availble'),
                numberOfContests  = $('tr.contest').length;

            if ( !isLoading && hasNextRecord ) {
                setLoadingStatus(true);
                return getMoreContests(numberOfContests);
            }
        });
    </script>
    <script type="text/javascript">
        function getMoreContests(startIndex) {
            var pageRequests = {
                'keyword': '${param.keyword}',
                'startIndex': startIndex
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/contest/getContests.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    return processContestsResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processContestsResult(result) {
            if ( result['isSuccessful'] ) {
                displayContests(result['contests']);
            } else {
                $('p', '#more-contests').removeClass('availble');
                $('p', '#more-contests').html('<spring:message code="voj.contests.contests.no-more-contests" text="No more contests" />');
                $('#more-contests').css('cursor', 'default');
            }
            setLoadingStatus(false);
        }
    </script>
    <script type="text/javascript">
        function displayContests(contests) {
            for ( var i = 0; i < contests.length; ++ i ) {
                var contestStatus    = 'Done',
                    currentTime      = new Date(),
                    contestStartTime = new Date(contests[i]['startTime']),
                    contestEndTime   = new Date(contests[i]['endTime']);
                
                if ( currentTime < contestStartTime ) {
                    contestStatus = 'Ready';
                } else if ( currentTime >= contestStartTime && currentTime <= contestEndTime ) {
                    contestStatus = 'Live';
                }
                $('#contests tbody').append('<tr class="contest %s">'.format(contestStatus) + 
                    '    <td class="overview">' + 
                    '        <h5><a href="<c:url value="/contest/" />%s">%s</a></h5>'.format(contests[i]['contestId'], contests[i]['contestName']) + 
                    '        <ul class="inline">' + 
                    '            <li>%s</li>'.format(contests[i]['contestMode']) + 
                    '            <li><spring:message code="voj.contests.contests.start-time" text="Start Time" />: %s</li>'.format(getFormatedDateString(contests[i]['startTime'], '${language}')) + 
                    '            <li><spring:message code="voj.contests.contests.end-time" text="End Time" />: %s</li>'.format(getFormatedDateString(contests[i]['endTime'], '${language}')) + 
                    '        </ul>' + 
                    '    </td>' + 
                    '    <td class="status">%s</td>'.format(contestStatus) + 
                    '</tr>');
            }
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>