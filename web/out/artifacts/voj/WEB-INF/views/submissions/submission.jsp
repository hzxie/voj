<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.submissions.submission.title" text="Submission #" />${submission.submissionId} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/submissions/submission.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/highlight.min.css?v=${version}" />
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
            <div id="main-content" class="span9">
                <div class="submission">
                    <div class="header">
                        <span class="pull-right"></span>
                        <span class="name">P${submission.problem.problemId} ${submission.problem.problemName}</span>
                    </div> <!-- .header -->
                    <div class="body">
                        <div class="section">
                            <h4><spring:message code="voj.submissions.submission.overview" text="Overview" /></h4>
                            <div class="description">
                                <table class="table">
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.judge-result" text="Judge Result" /></td>
                                        <td id="judge-result" class="flag-${submission.judgeResult.judgeResultSlug}">${submission.judgeResult.judgeResultName}</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.problem" text="Problem" /></td>
                                        <td id="problem-summery"><a href="<c:url value="/p/${submission.problem.problemId}" />">P${submission.problem.problemId} ${submission.problem.problemName}</a></td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.submit-time" text="Submit Time" /></td>
                                        <td id="submit-time"><fmt:formatDate value="${submission.submitTime}" type="both" dateStyle="long" timeStyle="medium"/></td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.language" text="Language" /></td>
                                        <td id="language-name">${submission.language.languageName}</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.judger" text="Judger" /></td>
                                        <td id="judger-name">Default Judger</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.used-time" text="Used Time" /></td>
                                        <td id="used-time">${submission.usedTime} ms</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.used-memory" text="Used Memory" /></td>
                                        <td id="used-memory">${submission.usedMemory} K</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.execute-time" text="Execute Time" /></td>
                                        <td id="execute-time"><fmt:formatDate value="${submission.executeTime}" type="both" dateStyle="long" timeStyle="medium"/></td>
                                    </tr>
                                </table>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4><spring:message code="voj.submissions.submission.judge-result" text="Judge Result" /></h4>
                            <div id="judge-log" class="description markdown">${submission.judgeLog}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${submission.user == myProfile or myProfile.userGroup.userGroupSlug == 'administrators'}">
                        <div class="section">
                            <h4><spring:message code="voj.submissions.submission.code" text="Code" /></h4>
                            <div class="description">
                                <pre><code>${submission.code.replace("<", "&lt;").replace(">", "&gt;")}</code></pre>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        </c:if>
                    </div> <!-- .body -->
                </div> <!-- .submission -->
            </div> <!-- #main-content -->
            <div id="sidebar" class="span3">
                <div id="submit-user" class="section">
                    <h5><spring:message code="voj.submissions.submission.user" text="User" /></h5>
                    <img src="${cdnUrl}/img/avatar.jpg?v=${version}" alt="User Avatar" class="img-circle" />
                    <p>
                        <spring:message code="voj.submissions.submission.submitted-by" text="Submitted by" /> <a href="<c:url value="/accounts/user/${submission.user.uid}" />">${submission.user.username}</a>
                    </p>
                </div> <!-- #profile -->
                <div id="problem" class="section">
                    <h5><spring:message code="voj.submissions.submission.action" text="Action" /></h5>
                    <ul>
                        <li><a href="<c:url value="/p/${submission.problem.problemId}" />"><spring:message code="voj.submissions.submission.view-problem" text="View Problem" /></a></li>
                        <li><a href="<c:url value="/p/${submission.problem.problemId}/solution" />"><spring:message code="voj.submissions.submission.view-solution" text="View Solution" /></a></li>
                        <li><a href="<c:url value="/submission?pid=${submission.problem.problemId}" />"><spring:message code="voj.submissions.submission.view-submission" text="View Submission" /></a></li>
                    </ul>
                </div> <!-- problem -->
            </div> <!-- #sidebar -->
        </div> <!-- .row-fluid -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
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
    <c:if test="${submission.judgeResult.judgeResultName == 'Pending'}">
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/date-${language}.min.js?v=${version}', function() {
            var currentJudgeResult = 'Pending',
                getterInterval     = setInterval(function() {
                    getRealTimeJudgeResult();
                    currentJudgeResult = $('#judge-result').html();

                    if ( currentJudgeResult != 'Pending' ) {
                        clearInterval(getterInterval);
                    }
                }, 10000);
        });
    </script>
    <script type="text/javascript">
        $(function() {
            var subscriptionUrl = '<c:url value="/submission/getRealTimeJudgeResult.action?submissionId=${submission.submissionId}&csrfToken=${csrfToken}" />',
                source          = new EventSource(subscriptionUrl),
                lastMessage     = '';

            source.onmessage    = function(e) {
                var message     = e['data'];

                if ( message == lastMessage ) {
                    return;
                }
                lastMessage     = message;

                if ( message == 'Established' ) {
                    $('#judge-log').append('<p>Connected to Server.</p>');
                    return;
                }
                var mapMessage  = JSON.parse(message),
                    judgeResult = mapMessage['judgeResult'],
                    judgeLog    = mapMessage['message'];
                    
                $('#judge-result').html(judgeResult);
                $('#judge-log').append(converter.makeHtml(judgeLog));
            }
        });
    </script>
    <script type="text/javascript">
        function getRealTimeJudgeResult() {
            var pageRequests = {
                'submissionId': ${submission.submissionId}
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/submission/getSubmission.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        if ( result['submission']['judgeResult']['judgeResultSlug'] != 'PD' ) {
                            $('#judge-result').removeClass();
                            $('#judge-result').addClass("flag-" + result['submission']['judgeResult']['judgeResultSlug'])
                            $('#judge-result').html(result['submission']['judgeResult']['judgeResultName']);
                            $('#used-time').html(result['submission']['usedTime'] + " ms");
                            $('#used-memory').html(result['submission']['usedMemory'] + " KB");
                            $('#execute-time').html(getFormatedDateString(result['submission']['executeTime'], '${language}'));
                            $('#judge-log').html(converter.makeHtml(result['submission']['judgeLog'].replace(/\\\n/g, '\\n')));
                        }
                    }
                }
            });
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
    </c:if>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highlight.min.js?v=${version}', function() {
            $('pre code').each(function(i, block) {
                hljs.highlightBlock(block);
            });
        });
    </script>
    <script type="text/javascript">
        $(function() {
            var imageObject       = $('img', '#submit-user'),
                hashCode          = md5('${submission.user.email}'),
                gravatarSeriveUrl = 'https://secure.gravatar.com/';
                
            $.ajax({
                type: 'GET',
                url: gravatarSeriveUrl + hashCode + '.json',
                dataType: 'jsonp',
                success: function(result){
                    if ( result != null ) {
                        var imageUrl    = result['entry'][0]['thumbnailUrl'],
                            requrestUrl = imageUrl + '?s=200';
                        $(imageObject).attr('src', requrestUrl);
                    }
                }
            });
        });
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>