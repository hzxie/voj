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
    <title><spring:message code="voj.administration.edit-submission.title" text="Edit Submission" /> #${submission.submissionId} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/edit-submission.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/highlight.min.css?v=${version}" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/pace.min.js?v=${version}"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js?v=${version}"></script>
    <![endif]-->
    <!--[if lte IE 7]>
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
                <h2 class="page-header"><i class="fa fa-edit"></i> <spring:message code="voj.administration.edit-submission.edit-submission" text="Edit Submission" /></h2>
                <div class="section">
                    <h4><spring:message code="voj.administration.edit-submission.overview" text="Overview" /></h4>
                    <div class="description">
                        <table class="table">
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.judge-result" text="Judge Result" /></td>
                                <td id="judge-result" class="flag-${submission.judgeResult.judgeResultSlug}">${submission.judgeResult.judgeResultName}</td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.problem" text="Problem" /></td>
                                <td id="problem-summery"><a href="<c:url value="/administration/edit-problem/${submission.problem.problemId}" />">P${submission.problem.problemId} ${submission.problem.problemName}</a></td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.submit-time" text="Submit Time" /></td>
                                <td id="submit-time"><fmt:formatDate value="${submission.submitTime}" type="both" dateStyle="long" timeStyle="medium"/></td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.language" text="Language" /></td>
                                <td id="language-name">${submission.language.languageName}</td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.submit-user" text="Submit User" /></td>
                                <td id="submit-user"><a href="<c:url value="/administration/edit-user/${submission.user.uid}" />">${submission.user.username}</a></td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.used-time" text="Used Time" /></td>
                                <td id="used-time">${submission.usedTime} ms</td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.used-memory" text="Used Memory" /></td>
                                <td id="used-memory">${submission.usedMemory} K</td>
                            </tr>
                            <tr>
                                <td><spring:message code="voj.administration.edit-submission.execute-time" text="Execute Time" /></td>
                                <td id="execute-time"><fmt:formatDate value="${submission.executeTime}" type="both" dateStyle="long" timeStyle="medium"/></td>
                            </tr>
                        </table>
                    </div> <!-- .description -->
                </div> <!-- .section -->
                <div class="section">
                    <h4><spring:message code="voj.administration.edit-submission.code" text="Code" /></h4>
                    <div class="description">
                        <pre><code>${submission.code.replace("<", "&lt;").replace(">", "&gt;")}</code></pre>
                    </div> <!-- .description -->
                </div> <!-- .section -->
                <div class="section">
                    <h4><spring:message code="voj.administration.edit-submission.judge-result" text="Judge Result" /></h4>
                    <div id="judge-log" class="description markdown">${submission.judgeLog}</div> <!-- .description -->
                </div> <!-- .section -->
                <div class="section">
                    <button class="btn btn-warning"><spring:message code="voj.administration.edit-submission.restart-submission" text="Restart Submission" /></button>                    
                    <button class="btn btn-danger"><spring:message code="voj.administration.edit-submission.delete-submission" text="Delete Submission" /></button>                    
                </div> <!-- .section -->
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
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
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highlight.min.js?v=${version}', function() {
            $('code').each(function(i, block) {
                hljs.highlightBlock(block);
            });
        });
    </script>
    <script type="text/javascript">
        $('button.btn-warning').click(function() {
            if ( !confirm('<spring:message code="voj.administration.edit-submission.continue-or-not" text="Are you sure to continue?" />') ) {
                return;
            }
            $('.alert-error').addClass('hide');
            $('button.btn-warning').attr('disabled', 'disabled');
            $('button.btn-warning').html('<spring:message code="voj.administration.edit-submission.please-wait" text="Please wait..." />');

            var submissions = [${submission.submissionId}];
            return doRestartSubmissionsAction(submissions);
        });
    </script>
    <script type="text/javascript">
        $('button.btn-danger').click(function() {
            if ( !confirm('<spring:message code="voj.administration.edit-submission.continue-or-not" text="Are you sure to continue?" />') ) {
                return;
            }
            $('.alert-error').addClass('hide');
            $('button.btn-danger').attr('disabled', 'disabled');
            $('button.btn-danger').html('<spring:message code="voj.administration.edit-submission.please-wait" text="Please wait..." />');

            var submissions = [${submission.submissionId}];
            return doDeleteSubmissionsAction(submissions);
        });
    </script>
    <script type="text/javascript">
        function doDeleteSubmissionsAction(submissions) {
            var postData = {
                'submissions': JSON.stringify(submissions)
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/deleteSubmissions.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        for ( var i = 0; i < submissions.length; ++ i ) {
                            window.location.href = '<c:url value="/administration/all-submissions" />';
                        }
                    } else {
                        alert('<spring:message code="voj.administration.edit-submission.delete-error" text="Some errors occurred while deleting this submission." />');
                    }
                    $('button.btn-danger').removeAttr('disabled');
                    $('button.btn-danger').html('<spring:message code="voj.administration.edit-submission.delete-submission" text="Delete Submission" />');
                }
            });
        }
    </script>
    <script type="text/javascript">
        function doRestartSubmissionsAction(submissions) {
            var postData = {
                'submissions': JSON.stringify(submissions)
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/restartSubmissions.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        window.location.reload();
                    } else {
                        alert('<spring:message code="voj.administration.edit-submission.restart-error" text="Some errors occurred while restarting this submission." />');
                    }
                    $('button.btn-danger').removeAttr('disabled');
                    $('button.btn-danger').html('<spring:message code="voj.administration.edit-submission.restart-submission" text="Restart Submission" />');
                }
            });
        }
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
</body>
</html>