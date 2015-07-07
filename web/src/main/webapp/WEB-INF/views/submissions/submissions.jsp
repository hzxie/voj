<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.submissions.submissions.title" text="Submission" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/submissions/submissions.css" />
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
            <div id="submission" class="span12">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th class="flag"><spring:message code="voj.submissions.submissions.result" text="Result" /></th>
                            <th class="score"><spring:message code="voj.submissions.submissions.score" text="Score" /></th>
                            <th class="time"><spring:message code="voj.submissions.submissions.time" text="Time" /></th>
                            <th class="memory"><spring:message code="voj.submissions.submissions.memory" text="Memory" /></th>
                            <th class="name"><spring:message code="voj.submissions.submissions.problem" text="Problem" /></th>
                            <th class="user"><spring:message code="voj.submissions.submissions.user" text="User" /></th>
                            <th class="language"><spring:message code="voj.submissions.submissions.language" text="Language" /></th>
                            <th class="submit-time"><spring:message code="voj.submissions.submissions.submit-time" text="Submit Time" /></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="submission" items="${submissions}">
                        <tr data-value="${submission.submissionId}">
                            <td class="flag-${submission.judgeResult.judgeResultSlug}"><a href="<c:url value="/submission/${submission.submissionId}" />">${submission.judgeResult.judgeResultName}</a></td>
                            <td class="score">${submission.judgeScore}</td>
                            <td class="time">${submission.usedTime} ms</td>
                            <td class="memory">${submission.usedMemory} K</td>
                            <td class="name"><a href="<c:url value="/p/${submission.problem.problemId}" />">P${submission.problem.problemId} ${submission.problem.problemName}</a></td>
                            <td class="user"><a href="<c:url value="/accounts/user/${submission.user.uid}" />">${submission.user.username}</a></td>
                            <td class="language">${submission.language.languageName}</td>
                            <td class="submit-time">
                                <fmt:formatDate value="${submission.submitTime}" type="both" dateStyle="default" timeStyle="default" />
                            </td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <div id="more-submissions">
                    <p class="availble"><spring:message code="voj.submissions.submissions.more-submission" text="More Submission..." /></p>
                    <img src="${cdnUrl}/img/loading.gif" alt="Loading" class="hide" />
                </div>
            </div> <!-- #submission -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/date-${language}.min.js"></script>
    <script type="text/javascript">
        setInterval(function() {
            var firstSubmissionRecord = $('tr:first-child', '#submission tbody'),
                firstSubmissionId     = parseInt($(firstSubmissionRecord).attr('data-value'));
            
            getLatestSubmissions(firstSubmissionId + 1);
        }, 10000);
    </script>
    <script type="text/javascript">
        function getLatestSubmissions(startIndex) {
            var pageRequests = {
                'startIndex': startIndex
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/submission/getLatestSubmissions.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        displayLatestSubmissionRecords(result['submissions']);
                    }
                }
            });
        }
    </script>
    <script type="text/javascript">
        function displayLatestSubmissionRecords(submissions) {
            for ( var i = 0; i < submissions.length; ++ i ) {
                $('table > tbody', '#submission').prepend(
                    getSubmissionContent(submissions[i]['submissionId'], submissions[i]['judgeResult'], 
                                         submissions[i]['judgeScore'], submissions[i]['usedTime'], 
                                         submissions[i]['usedMemory'], submissions[i]['problem'], 
                                         submissions[i]['user'], submissions[i]['language'], submissions[i]['submitTime'])
                );
            }
        }
    </script>
    <script type="text/javascript">
        function setLoadingStatus(isLoading) {
            if ( isLoading ) {
                $('p', '#more-submissions').addClass('hide');
                $('img', '#more-submissions').removeClass('hide');
            } else {
                $('img', '#more-submissions').addClass('hide');
                $('p', '#more-submissions').removeClass('hide');
            }
        }
    </script>
    <script type="text/javascript">
        $('#more-submissions').click(function(event) {
            var isLoading            = $('img', this).is(':visible'),
                hasNextRecord        = $('p', this).hasClass('availble'),
                lastSubmissionRecord = $('tr:last-child', '#submission tbody'),
                lastSubmissionId     = parseInt($(lastSubmissionRecord).attr('data-value'));

            if ( !isLoading && hasNextRecord ) {
                setLoadingStatus(true);
                return getMoreHistorySubmissions(lastSubmissionId - 1);
            }
        });
    </script>
    <script type="text/javascript">
        function getMoreHistorySubmissions(startIndex) {
            var pageRequests = {
                'startIndex': startIndex
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/submission/getSubmissions.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    return processHistorySubmissionsResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processHistorySubmissionsResult(result) {
            if ( result['isSuccessful'] ) {
                displayHistorySubmissionRecords(result['submissions']);
            } else {
                $('p', '#more-submissions').removeClass('availble');
                $('p', '#more-submissions').html('<spring:message code="voj.submissions.submissions.no-more-submission" text="No more submission" />');
                $('#more-submissions').css('cursor', 'default');
            }
            setLoadingStatus(false);
        }
    </script>
    <script type="text/javascript">
        function displayHistorySubmissionRecords(submissions) {
            for ( var i = 0; i < submissions.length; ++ i ) {
                $('table > tbody', '#submission').append(
                    getSubmissionContent(submissions[i]['submissionId'], submissions[i]['judgeResult'], 
                                         submissions[i]['judgeScore'], submissions[i]['usedTime'], 
                                         submissions[i]['usedMemory'], submissions[i]['problem'], 
                                         submissions[i]['user'], submissions[i]['language'], submissions[i]['submitTime'])
                );
            }
        }
    </script>
    <script type="text/javascript">
        function getSubmissionContent(submissionId, judgeResult, judgeScore, usedTime, usedMemory, problem, user, language, submitTime) {
            var submissionTemplate = '<tr data-value="%s">' +
                                     '    <td class="flag-%s"><a href="<c:url value="/submission/%s" />">%s</a></td>' +
                                     '    <td class="score">%s</td>' +
                                     '    <td class="time">%s ms</td>' +
                                     '    <td class="memory">%s K</td>' +
                                     '    <td class="name"><a href="<c:url value="/p/%s" />">P%s %s</a></td>' +
                                     '    <td class="user"><a href="<c:url value="/accounts/user/%s" />">%s</a></td>' +
                                     '    <td class="language">%s</td>' +
                                     '    <td class="submit-time">%s</td>' +
                                     '</tr>';

            return submissionTemplate.format(submissionId, judgeResult['judgeResultSlug'], submissionId, 
                                             judgeResult['judgeResultName'], judgeScore, usedTime, usedMemory, 
                                             problem['problemId'], problem['problemId'], problem['problemName'],
                                             user['uid'], user['username'], language['languageName'], getFormatedDateString(submitTime, '${language}'));
        }
    </script>
    <script type="text/javascript">
        function getFormatedDateString(dateTime, locale) {
            var dateObject = new Date(dateTime),
                dateString = dateObject.toString();

            if ( locale == 'en_US' ) {
                dateString = dateObject.toString('MMM d, yyyy h:mm:ss tt');
            } else if ( locale == 'zh_CN' ) {
                dateString = dateObject.toString('yyyy-M-dd HH:mm:ss');
            }

            return dateString;
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    <script type="text/javascript">${googleAnalyticsCode}</script>
    </c:if>
</body>
</html>