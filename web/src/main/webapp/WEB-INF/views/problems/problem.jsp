<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title>P${problem.problemId} ${problem.problemName} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/codemirror.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/problems/problem.css" />
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
        <div class="row-fluid">
            <div id="main-content" class="span9">
                <div class="problem">
                    <div class="header">
                    <c:if test="${isLogin}">
                        <c:if test="${latestSubmission[problem.problemId] != null}">
                            <span class="pull-right">${latestSubmission[problem.problemId].judgeResult.judgeResultName}</span>
                        </c:if>
                    </c:if>
                        <span class="name">P${problem.problemId} ${problem.problemName}</span>
                    </div> <!-- .header -->
                    <div class="body">
                        <div class="section">
                            <h4><spring:message code="voj.problems.problem.description" text="Description" /></h4>
                            <div class="description markdown">${problem.description}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4><spring:message code="voj.problems.problem.format" text="Format" /></h4>
                            <h5><spring:message code="voj.problems.problem.input" text="Input" /></h5>
                            <div class="description">${problem.inputFormat}</div> <!-- .description -->
                            <h5><spring:message code="voj.problems.problem.output" text="Output" /></h5>
                            <div class="description">${problem.outputFormat}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <div id="io-sample" class="section">
                            <h4><spring:message code="voj.problems.problem.samples" text="Samples" /></h4>
                            <h5><spring:message code="voj.problems.problem.sample-input" text="Sample Input" /></h5>
                            <div class="description"><pre>${problem.sampleInput}</pre></div> <!-- .description -->
                            <h5><spring:message code="voj.problems.problem.sample-output" text="Sample Output" /></h5>
                            <div class="description"><pre>${problem.sampleOutput}</pre></div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4><spring:message code="voj.problems.problem.restrictions" text="Restrictions" /></h4>
                            <div class="description">
                                <p><strong><spring:message code="voj.problems.problem.time-limit" text="Time Limit" />: </strong>${problem.timeLimit} ms</p>
                                <p><strong><spring:message code="voj.problems.problem.memory-limit" text="Memory Limit" />: </strong>${problem.memoryLimit} KB</p>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${problem.hint != null and problem.hint != ''}">
                        <div class="section">
                            <h4><spring:message code="voj.problems.problem.hint" text="Hint" /></h4>
                            <div class="description markdown">${problem.hint.replace("<", "&lt;").replace(">", "&gt;")}</div> <!-- .description -->
                        </div> <!-- .section -->
                        </c:if>
                        <form id="code-editor" onsubmit="onSubmit(); return false;" method="POST">
                            <textarea name="codemirror-editor" id="codemirror-editor"></textarea>
                            <div class="row-fluid">
                                <div class="span4">
                                    <select id="languages">
                                    <c:forEach var="language" items="${languages}">
                                        <option value="${language.languageSlug}">${language.languageName}</option>
                                    </c:forEach>
                                    </select>
                                </div> <!-- .span4 -->
                                <div id="submission-error" class="offset1 span3"></div> <!-- #submission-error -->
                                <div id="submission-action" class="span4">
                                    <input type="hidden" id="csrf-token" value="${csrfToken}" />
                                    <button type="submit" class="btn btn-primary"><spring:message code="voj.problems.problem.submit" text="Submit" /></button>
                                    <button id="close-submission" class="btn"><spring:message code="voj.problems.problem.cancel" text="Cancel" /></button>
                                </div> <!-- #submission-action -->
                            </div> <!-- .row-fluid -->
                        </form> <!-- #code-editor-->
                        <div id="mask" class="hide"></div> <!-- #mask -->
                    </div> <!-- .body -->
                </div> <!-- .problem -->
            </div> <!-- #main-content -->
            <div id="sidebar" class="span3">
                <div id="actions" class="section">
                    <h5><spring:message code="voj.problems.problem.actions" text="Actions" /></h5>
                    <ul>
                    <c:if test="${isLogin}">
                        <li><a id="submit-solution" href="javascript:void(0);"><spring:message code="voj.problems.problem.submit-solution" text="Submit Solution" /></a></li>
                    </c:if>
                        <li><a href="<c:url value="/p/${problem.problemId}/solution" />"><spring:message code="voj.problems.problem.view-solution" text="View Solution" /></a></li>
                        <li><a href="<c:url value="/submission?problemId=${problem.problemId}" />"><spring:message code="voj.problems.problem.view-submission" text="View Submission" /></a></li>
                    </ul>
                </div> <!-- #actions -->
                <c:if test="${isLogin}">
                <div id="submission" class="section">
                    <h5><spring:message code="voj.problems.problem.submission" text="My Submission" /></h5>
                    <c:if test="${submissions == null || submissions.size() == 0}">
                        <p><spring:message code="voj.problems.problem.no-submission" text="No submission" /></p>
                    </c:if>
                    <ul>
                    <c:forEach var="submission" items="${submissions}">
                        <li class="row-fluid">
                            <div class="span4">
                                <a href="<c:url value="/submission/${submission.submissionId}" />">
                                    <fmt:formatDate value="${submission.submitTime}" type="date" dateStyle="short" timeStyle="short"/>
                                </a>
                            </div> <!-- .span4 -->
                            <div class="span8 flag-${submission.judgeResult.judgeResultSlug}">
                                ${submission.judgeResult.judgeResultName}
                            </div> <!-- .span8 -->
                        </li>
                    </c:forEach>
                    </ul>
                </div> <!-- submission -->
                </c:if>
                <div id="discussion" class="section">
                    <h5><spring:message code="voj.problems.problem.discussion" text="Discussion" /></h5>
                    <c:if test="${discussionThreads == null || discussionThreads.size() == 0}">
                        <p><spring:message code="voj.problems.problem.no-discussion" text="No discussion" /></p>
                    </c:if>
                    <ul>
                    <c:forEach var="discussionThread" items="${discussionThreads}">
                        <li class="row-fluid">
                            <div class="span4">
                                <fmt:formatDate value="${discussionThread.discussionThreadCreateTime}" type="date" dateStyle="short" timeStyle="short"/>
                            </div> <!-- .span4 -->
                            <div class="span8">
                                <a href="<c:url value="/discussion/${discussionThread.discussionThreadId}" />">
                                    ${discussionThread.discussionThreadTitle}
                                </a>
                            </div> <!-- .span8 -->
                        </li>
                    </c:forEach>
                    </ul>
                </div> <!-- discussion -->
            </div> <!-- #sidebar -->
        </div> <!-- .row-fluid -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript" async src="https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/markdown.min.js', function() {
            converter = Markdown.getSanitizingConverter();

            $('.markdown').each(function() {
                var plainContent    = $(this).text(),
                    markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                
                $(this).html(markdownContent);
            });
        });
    </script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/codemirror.min.js', function() {
           $.when(
                $.getScript('${cdnUrl}/mode/clike.min.js'),
                $.getScript('${cdnUrl}/mode/go.min.js'),
                $.getScript('${cdnUrl}/mode/pascal.min.js'),
                $.getScript('${cdnUrl}/mode/perl.min.js'),
                $.getScript('${cdnUrl}/mode/php.min.js'),
                $.getScript('${cdnUrl}/mode/python.min.js'),
                $.getScript('${cdnUrl}/mode/ruby.min.js'),
                $.Deferred(function(deferred) {
                    $(deferred.resolve);
                })
            ).done(function() {
                window.codeMirrorEditor = CodeMirror.fromTextArea(document.getElementById('codemirror-editor'), {
                    mode: $('select#languages').val(),
                    tabMode: 'indent',
                    theme: 'neat',
                    tabSize: 4,
                    indentUnit: 4,
                    lineNumbers: true,
                    lineWrapping: true
                });
            }); 
        });
    </script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highlight.min.js', function() {
            $('code').each(function(i, block) {
                hljs.highlightBlock(block);
            });
        });
    </script>
    <script type="text/javascript">
        $(function() {
            var preferLanguage = '${myProfile.preferLanguage.languageSlug}';
            $('select#languages').val(preferLanguage);
        });
    </script>
    <script type="text/javascript">
        $('select#languages').change(function() {
            window.codeMirrorEditor.setOption('mode', $(this).val());
        });
    </script>
    <script type="text/javascript">
        $('#submit-solution').click(function() {
            $('#mask').removeClass('hide');
            $('#code-editor').addClass('fade');
        });
    </script>
    <script type="text/javascript">
        $('#close-submission').click(function(e) {
        	e.preventDefault();
            
            $('#code-editor').removeClass('fade');
            $('#mask').addClass('hide');
        });
    </script>
    <script type="text/javascript">
        function onSubmit() {
            var problemId   = ${problem.problemId},
                language    = $('select#languages').val(),
                code        = window.codeMirrorEditor.getValue(),
                csrfToken   = $('#csrf-token').val();

            $('button[type=submit]', '#code-editor').attr('disabled', 'disabled');
            $('button[type=submit]', '#code-editor').html('<spring:message code="voj.problems.problem.please-wait" text="Please wait..." />');

            return createSubmissionAction(problemId, language, code, csrfToken);
        }
    </script>
    <script type="text/javascript">
        function createSubmissionAction(problemId, languageSlug, code, csrfToken) {
            var postData = {
                'problemId': problemId,
                'languageSlug': languageSlug,
                'code': code,
                'csrfToken': csrfToken
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/p/createSubmission.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        var submissionId = result['submissionId'];
                        window.location.href = '<c:url value="/submission/" />' + submissionId;
                    } else {
                        var errorMessage = '';

                        if ( !result['isCsrfTokenValid'] ) {
                            errorMessage = '<spring:message code="voj.problems.problem.invalid-token" text="Invalid token." />';
                        } else if ( !result['isUserLogined'] ) {
                            errorMessage = '<spring:message code="voj.problems.problem.user-not-login" text="Please sign in first." />';
                        } else if ( !result['isProblemExists'] ) {
                            errorMessage = '<spring:message code="voj.problems.problem.problem-not-exists" text="The problem not exists." />';
                        } else if ( !result['isLanguageExists'] ) {
                            errorMessage = '<spring:message code="voj.problems.problem.language-not-exists" text="The language not exists." />';
                        } else if ( result['isCodeEmpty'] ) {
                            errorMessage = '<spring:message code="voj.problems.problem.empty-code" text="Please enter the code." />';
                        }
                        $('#submission-error').html(errorMessage);
                    }

                    $('button[type=submit]', '#code-editor').removeAttr('disabled');
                    $('button[type=submit]', '#code-editor').html('<spring:message code="voj.problems.problem.submit" text="Submit" />');
                }
            });
        }
    </script>
    <script type="text/x-mathjax-config">
        MathJax.Hub.Config({
            tex2jax: {
                inlineMath: [
                    ['$','$'], 
                    ['\\(','\\)']
                ]
            }
        });
    </script>
    <script type="text/javascript" src="https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>