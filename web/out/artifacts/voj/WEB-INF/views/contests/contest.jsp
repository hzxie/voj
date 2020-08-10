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
    <title>${contest.contestName} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/contests/contest.css?v=${version}" />
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
    <div id="content" class="container">
        <div class="row-fluid">
            <div id="main-content" class="span8">
                <div class="contest">
                    <div class="header">
                        <span class="pull-right">
                        <c:if test="${isLogin}">
                        <c:choose>
                            <c:when test="${isAttended}"><spring:message code="voj.contests.contest.attended" text="Attended" /></c:when>
                            <c:otherwise><spring:message code="voj.contests.contest.not-attended" text="Not attended" /></c:otherwise>
                        </c:choose>
                        </c:if>
                        </span>
                        <span class="name">${contest.contestName}</span>
                    </div> <!-- .header -->
                    <div class="body">
                        <div class="section">
                            <h4><spring:message code="voj.contests.contest.instruction" text="Instruction" /></h4>
                            <c:choose>
                                <c:when test="${contest.contestNotes == ''}"><p><spring:message code="voj.contests.contest.no-instruction" text="No instruction available." /></p></c:when>
                                <c:otherwise><div class="markdown">${contest.contestNotes}</div> <!-- .markdown --></c:otherwise>
                            </c:choose>
                        </div> <!-- .section -->
                    <c:if test="${(currentTime.after(contest.startTime) and isAttended) or currentTime.after(contest.endTime)}">
                        <div class="section">
                            <h4><spring:message code="voj.contests.contest.problems" text="Problems" /></h4>
                            <table id="problems" class="table table-striped">
                            <c:if test="${isLogin}">
                                <thead>
                                    <tr>
                                        <th><spring:message code="voj.contests.contest.status" text="Status" /></th>
                                        <th><spring:message code="voj.contests.contest.problem" text="Problem" /></th>
                                    </tr>
                                </thead>
                            </c:if>
                                <tbody>
                                <c:forEach var="problem" items="${problems}">
                                    <tr>
                                    <c:if test="${isLogin}">
                                    <c:set var="submission" value="${submissions[problem.problemId]}" />
                                    <c:choose>
                                    <c:when test="${submission != null}">
                                        <td class="flag-${submission.submission.judgeResult.judgeResultSlug}"><a href="<c:url value="/submission/${submission.submission.submissionId}" />">${submission.submission.judgeResult.judgeResultName}</a></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td><spring:message code="voj.contests.contest.no-submissions" text="No submissions" /></td>
                                    </c:otherwise>
                                    </c:choose>
                                    </c:if>
                                        <td>
                                            <a href="<c:url value="/contest/${contest.contestId}/p/${problem.problemId}" />">P${problem.problemId} ${problem.problemName}</a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div> <!-- .section -->
                    </c:if>
                    </div> <!-- .body -->
                </div> <!-- .contest -->
            </div> <!-- #main-content -->
            <div id="sidebar" class="span4">
                <div class="section">
                    <h5><spring:message code="voj.contests.contest.basic-information" text="Basic Information" /></h5>
                    <div id="basic-info">
                        <div class="row-fluid">
                            <div class="span5"><spring:message code="voj.contests.contest.status" text="Status" /></div> <!-- .span5 -->
                            <div class="span7 ${contestStatus}">${contestStatus}</div> <!-- .span7 -->
                        </div> <!-- .row-fluid -->
                        <div class="row-fluid">
                            <div class="span5"><spring:message code="voj.contests.contest.start-time" text="Start Time" /></div> <!-- .span5 -->
                            <div class="span7"><fmt:formatDate value="${contest.startTime}" pattern="yyyy-MM-dd HH:mm:ss" /></div> <!-- .span7 -->
                        </div> <!-- .row-fluid -->
                        <div class="row-fluid">
                            <div class="span5"><spring:message code="voj.contests.contest.end-time" text="End Time" /></div> <!-- .span5 -->
                            <div class="span7"><fmt:formatDate value="${contest.endTime}" pattern="yyyy-MM-dd HH:mm:ss" /></div> <!-- .span7 -->
                        </div> <!-- .row-fluid -->
                        <div class="row-fluid">
                            <div class="span5"><spring:message code="voj.contests.contest.rule" text="Rule" /></div> <!-- .span5 -->
                            <div class="span7">${contest.contestMode}</div> <!-- .span7 -->
                        </div> <!-- .row-fluid -->
                        <div class="row-fluid">
                            <div class="span5"><spring:message code="voj.contests.contest.number-of-problems" text="# Problems" /></div> <!-- .span5 -->
                            <div class="span7">${problems.size()}</div> <!-- .span7 -->
                        </div> <!-- .row-fluid -->
                        <div class="row-fluid">
                            <div class="span5"><spring:message code="voj.contests.contest.number-of-contestants" text="# Contestants" /></div> <!-- .span5 -->
                            <div id="number-of-contestants" class="span7">${numberOfContestants}</div> <!-- .span7 -->
                        </div> <!-- .row-fluid -->
                    </div> <!-- #basic-info -->
                </div> <!-- .section -->
                <div class="section">
                    <h5><spring:message code="voj.contests.contest.actions" text="Actions" /></h5>
                    <ul>
                    <c:if test="${not isAttended and contestStatus != 'Done'}">
                        <li><a id="attend-contest-button" href="javascript:attendContest();"><spring:message code="voj.contests.contest.attend-contest" text="Attend the contest" /></a></li>
                    </c:if>
                    <c:if test="${contestStatus != 'Ready'}">
                        <li><a href="<c:url value="/contest/${contest.contestId}/leaderboard" />"><spring:message code="voj.contests.contest.view-leaderboard" text="View leaderboard" /></a></li>
                    </c:if>
                    <c:if test="${isAttended and contestStatus == 'Ready'}">
                        <li><spring:message code="voj.contests.contest.already-attended" text="You have already attended the contest." /></li>
                        <li><spring:message code="voj.contests.contest.no-actions" text="No actions are available." /></li>
                    </c:if>
                    </ul>
                </div> <!-- .section -->
            </div> <!-- #sidebar -->
        </div> <!-- .row-fluid -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/x-mathjax-config">
        MathJax.Hub.Config({
            tex2jax: {inlineMath: [['$','$'], ['\\(','\\)']]}
        });
    </script>
    <script type="text/javascript" async src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.2/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/markdown.min.js?v=${version}', function() {
            converter = Markdown.getSanitizingConverter();

            $('.markdown').each(function() {
                var plainContent    = $(this).text(),
                    markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                
                $(this).html(markdownContent);
            });
        });
    </script>
<c:if test="${not isAttended and contestStatus != 'Done'}">
<c:choose>
    <c:when test="${isLogin}">
    <script type="text/javascript">
        function attendContest() {
            var postData = {
                'csrfToken': '${csrfToken}'
            };

            $.ajax({                
                type: 'POST',
                url: '<c:url value="/contest/${contest.contestId}/attend.action" />',
                data: postData, 
                dataType: 'JSON',
                success: function(result) {
                    if ( result['isSuccessful'] ) {
                        $('#attend-contest-button').removeAttr('href');
                        setContestAttended();
                    } else {
                        if ( !result['isCsrfTokenValid'] ) {
                            alert('<spring:message code="voj.contests.contest.invalid-token" text="Invalid Token." />');
                        } else if ( !result['isContestExists'] ) {
                            alert('<spring:message code="voj.contests.contest.contest-not-exists" text="The contest doesn&acute;t exist." />');
                        } else if ( !result['isContestReady'] ) {
                            alert('<spring:message code="voj.contests.contest.contest-not-ready" text="The contest is started or finished." />');
                        } else if ( !result['isUserLogin'] ) {
                            alert('<spring:message code="voj.contests.contest.user-not-login" text="You are not logged in." />');
                        } else if ( result['isAttendedContest'] ) {
                            alert('<spring:message code="voj.contests.contest.already-attended" text="You have already attended the contest." />');
                            setContestAttended();
                        }
                    }
                }
            });
        }
    </script>
    <script type="text/javascript">
        function setContestAttended() {
            var numberOfContestants = parseInt($('#number-of-contestants').html());

            $('.pull-right', '.contest .header').html('<spring:message code="voj.contests.contest.attended" text="Attended" />');
            $('#number-of-contestants').html(numberOfContestants + 1);
            $('#attend-contest-button').html('<spring:message code="voj.contests.contest.attended-contest" text="You&acute;ve attended the contest." />');
            setTimeout(function(){
                $('#attend-contest-button').remove();
            }, 1500);
        }
    </script>
    </c:when>
    <c:otherwise>
    <script type="text/javascript">
        function attendContest() {
            window.location.href = '<c:url value="/accounts/login?forward=" />${requestScope['javax.servlet.forward.request_uri']}';
        }
    </script>
    </c:otherwise>
    </c:choose>
    </c:if>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>