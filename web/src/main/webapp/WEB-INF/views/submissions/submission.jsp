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
    <title><spring:message code="voj.submissions.submission.title" text="Submission #" />${submission.submissionId} | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/submissions/submission.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/highlight.min.css" />
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
        <div id="websocket-status" class="alert"></div> <!-- #websocket-status -->
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
                                        <td class="flag-${submission.judgeResult.judgeResultSlug}">${submission.judgeResult.judgeResultName}</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.problem" text="Problem" /></td>
                                        <td><a href="<c:url value="/p/${submission.problem.problemId}" />">P${submission.problem.problemId} ${submission.problem.problemName}</a></td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.submit-time" text="Submit Time" /></td>
                                        <td><fmt:formatDate value="${submission.submitTime}" type="both" dateStyle="default" timeStyle="default"/></td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.language" text="Language" /></td>
                                        <td>${submission.language.languageName}</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.judger" text="Judger" /></td>
                                        <td>Default Judger</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.used-time" text="Used Time" /></td>
                                        <td>${submission.usedTime} ms</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.used-memory" text="Used Memory" /></td>
                                        <td>${submission.usedMemory} K</td>
                                    </tr>
                                    <tr>
                                        <td><spring:message code="voj.submissions.submission.execute-time" text="Execute Time" /></td>
                                        <td><fmt:formatDate value="${submission.executeTime}" type="both" dateStyle="default" timeStyle="default"/></td>
                                    </tr>
                                </table>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4><spring:message code="voj.submissions.submission.judge-result" text="Judge Result" /></h4>
                            <div class="description markdown">${submission.judgeLog}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${submission.user == user}">
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
                    <img src="${cdnUrl}/img/avatar.jpg" alt="User Avatar" class="img-circle" />
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
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/markdown.min.js', function() {
            converter = Markdown.getSanitizingConverter();
            converter.hooks.chain("preBlockGamut", function (text, rbg) {
                return text.replace(/^ {0,3}""" *\\n((?:.*?\\n)+?) {0,3}""" *$/gm, function (whole, inner) {
                    return "<blockquote>" + rbg(inner) + "</blockquote>\\n";
                });
            });

            $('.markdown').each(function() {
                var plainContent    = $(this).html(),
                    markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                
                $(this).html(markdownContent);
            });
        });
    </script>
    <c:if test="${submission.judgeResult.judgeResultName == 'Pending'}">
    <script type="text/javascript">
        $.when(
            $.getScript('${cdnUrl}/js/sockjs-1.0.0.min.js'),
            $.getScript('${cdnUrl}/js/stomp-2.3.4.min.js'),
            $.Deferred(function(deferred){
                $(deferred.resolve);
            })
        ).done(function(){
            hasConntected   = false;

            var socket      = new SockJS('<c:url value="/websocket" />'),
                stompClient = Stomp.over(socket);

            stompClient.connect({}, function(frame) {
                hasConntected = true;
                displayWebSocketConnectionStatus(true, hasConntected);

                stompClient.send('/voj/authorization.action', {}, JSON.stringify({
                    'key': 'csrfToken',
                    'value': 'x${csrfToken}'
                }));
                
                stompClient.subscribe('/user/message/authorization', function(message){
                	console.log(message);
                });

                stompClient.subscribe('/voj/getRealTimeJudgeResult.action/${submission.submissionId}', function(message){
                    console.log(message);
                });
            }, function() {
                return displayWebSocketConnectionStatus(false, hasConntected);
            });
        });
    </script>
    <script type="text/javascript">
        function displayWebSocketConnectionStatus(isConnected, hasConntected) {
            var message     = '',
                alertClass  = '';

            if ( isConnected ) {
                message     = '<i class="fa fa-smile-o"></i> <spring:message code="voj.submissions.submission.websocket-established" text="WebSocket connection has been established." />';
                alertClass  = 'alert-success';
            } else {
                if ( hasConntected ) {
                    message     = '<i class="fa fa-exclamation-circle"></i> <spring:message code="voj.submissions.submission.websocket-closed" text="WebSocket connection has been closed." />';
                    alertClass  = 'alert-info';
                } else {
                    message     = '<i class="fa fa-frown-o"></i> <spring:message code="voj.submissions.submission.websocket-established-failed" text="Cannot establish WebSocket connection." />';
                    alertClass  = 'alert-error';
                }
            }

            $('#websocket-status').html(message);
            $("#websocket-status").removeClass (function (index, css) {
                return (css.match (/(^|\s)alert\-\S+/g) || []).join(' ');
            });
            $('#websocket-status').addClass(alertClass);
            $('#websocket-status').fadeIn();

            setTimeout(function() {
                $('#websocket-status').fadeOut();
            }, 5000);
        }
    </script>
    </c:if>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highlight.min.js', function() {
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
</body>
</html>