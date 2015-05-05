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
    <title>Submission | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon">
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
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome-ie7.min.css" />
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='../not-supported';
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
                            <h4>Overview</h4>
                            <div class="description">
                                <table class="table">
                                    <tr>
                                        <td>Runtime Result</td>
                                        <td class="flag-${submission.judgeResult.judgeResultSlug.toLowerCase()}">${submission.judgeResult.judgeResultName}</td>
                                    </tr>
                                    <tr>
                                        <td>Problem</td>
                                        <td><a href="<c:url value="/p/${submission.problem.problemId}" />">P${submission.problem.problemId} ${submission.problem.problemName}</a></td>
                                    </tr>
                                    <tr>
                                        <td>Submit Time</td>
                                        <td><fmt:formatDate value="${submission.submitTime}" type="both" dateStyle="default" timeStyle="default"/></td>
                                    </tr>
                                    <tr>
                                        <td>Language</td>
                                        <td>${submission.language.languageName}</td>
                                    </tr>
                                    <tr>
                                        <td>Judger</td>
                                        <td>Default Judger</td>
                                    </tr>
                                    <tr>
                                        <td>Used Time</td>
                                        <td>${submission.usedTime} ms</td>
                                    </tr>
                                    <tr>
                                        <td>Used Memory</td>
                                        <td>${submission.usedMemory} K</td>
                                    </tr>
                                    <tr>
                                        <td>Execute Time</td>
                                        <td><fmt:formatDate value="${submission.executeTime}" type="both" dateStyle="default" timeStyle="default"/></td>
                                    </tr>
                                </table>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4>Runtime Result</h4>
                            <div class="description markdown">${submission.judgeLog}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${submission.user == user}">
                        <div class="section">
                            <h4>Code</h4>
                            <div class="description">
                                <pre><code>${submission.code.replace("<", "&lt;").replace(">", "&gt;")}</code></pre>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        </c:if>
                    </div> <!-- .body -->
                </div> <!-- .submission -->
            </div> <!-- #main-content -->
            <div id="sidebar" class="span3">
                <div id="profile" class="section">
                    <h5>User</h5>
                </div> <!-- #profile -->
                <div id="problem" class="section">
                    <h5>Problem</h5>
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
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highlight.min.js', function() {
            $('pre code').each(function(i, block) {
                hljs.highlightBlock(block);
            });
        });
    </script>
</body>
</html>