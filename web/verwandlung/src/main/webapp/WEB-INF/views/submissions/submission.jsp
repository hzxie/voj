<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:useBean id='DigestUtils' scope='request' class='com.happystudio.voj.util.DigestUtils'/>
<!doctype html>
<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <title>Submission | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link rel="shortcut icon" href="<c:url value="/assets/img/favicon.png" />">
    <!-- CSS -->
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/bootstrap.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/bootstrap-responsive.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/flat-ui.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/font-awesome.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/style.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/submission.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/highlight.css" />">
    <!-- JavaScript -->
    <script type="text/javascript" src="<c:url value="/assets/js/jquery-1.11.1.min.js" />"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="<c:url value="/assets/js/jquery.placeholder.js" />"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/font-awesome-ie7.min.css" />">
        <script type="text/javascript" sec="<c:url value="/assets/js/icon-font-ie7.js" />"></script>
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='../not-supported';
        </script>
    <![endif]-->
</head>
<body>
    <div id="header" class="row-fluid">
        <div class="container">
            <div id="logo" class="span6">
                <a href="<c:url value="/" />">
                    <img src="<c:url value="/assets/img/logo.png" />" alt="Logo">
                </a>
            </div> <!-- #logo -->
            <div id="nav" class="span6">
                <ul class="inline">
                    <li><a href="<c:url value="/p" />">Problems</a></li>
                    <li><a href="<c:url value="/discussion" />">Discussion</a></li>
                    <li><a href="<c:url value="/contests" />">Contests</a></li>
                    <li><a href="<c:url value="/submissions" />">Submission</a></li>
                    <li><a href="javascript:openDrawerMenu()">More</a></li>
                </ul>
            </div> <!-- #nav -->
        </div> <!-- .container -->
    </div> <!-- #header -->
    <div id="content" class="container">
        <div class="row-fluid">
            <div id="main-content" class="span9">
                <div class="submission">
                    <div class="header">
                        <span class="pull-right"></span>
                        <span class="name">P${submission.problem.problemID} ${submission.problem.problemName}</span>
                    </div> <!-- .header -->
                    <div class="body">
                        <div class="section">
                            <h4>Overview</h4>
                            <div class="description">
                                <table class="table">
                                    <tr>
                                        <td>Runtime Result</td>
                                        <td class="flag-${submission.runtimeResult.runtimeResultSlug.toLowerCase()}">${submission.runtimeResult.runtimeResultName}</td>
                                    </tr>
                                    <tr>
                                        <td>Problem</td>
                                        <td><a href="<c:url value="/p/${submission.problem.problemID}" />">P${submission.problem.problemID} ${submission.problem.problemName}</a></td>
                                    </tr>
                                    <tr>
                                        <td>Submit Time</td>
                                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss" value="${submission.submitTime}" /></td>
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
                                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss" value="${submission.executeTime}" /></td>
                                    </tr>
                                </table>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4>Runtime Result</h4>
                            <div class="description">${submission.runtimeLog}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${submission.getUser() != null}">
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
    <div id="drawer-nav">
        <span class="pull-right"><a href="javascript:closeDrawerMenu();">Close &times;</a></span>
        <div id="accounts" class="section">
            <h4>My Accounts</h4>
            <div id="profile">
            <c:choose>
            <c:when test="${isLogin}">
                <img src="http://www.gravatar.com/avatar/${DigestUtils.md5Hex(user.getEmail().toLowerCase())}?s=100&amp;d=mm" alt="avatar" class="img-circle" />
                <h5>${user.getUsername()}</h5>
                <p>${user.getEmail()}</p>
                <p>Accepted/Submit: ${submissionStats.get("acceptedSubmission")}/${submissionStats.get("totalSubmission")}(${submissionStats.get("acRate")}%)</p>
                <p>Language Preference: ${user.getPreferLanguage().getLanguageName()}</p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/dashboard" />">Dashboard</a></li>
                    <li><a href="<c:url value="/accounts/login?logout=true" />">Sign out</a></li>
                </ul>
            </c:when>
            <c:otherwise>
                <p>You are not logged in.</p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/login" />">Sign in</a></li>
                    <li><a href="<c:url value="/accounts/register" />">Sign up</a></li>
                </ul>
            </c:otherwise>
            </c:choose>
            </div> <!-- #profile -->
        </div> <!-- .section -->
        <div id="about" class="section">
            <h4>About</h4>
            <ul>
                <li><a href="#">Judgers</a></li>
                <li><a href="#">Feedback</a></li>
                <li><a href="#">About Us</a></li>
            </ul>
        </div> <!-- .section -->
    </div> <!-- #drawer-nav -->
    <div id="footer">
        <div class="container">
            <p id="copyright">Copyright&copy; 2005-2014 <a href="http://www.zjhzxhz.com/" target="_blank">HApPy Studio</a>. All rights reserved.</p>
        </div> <!-- .container -->
    </div> <!-- #footer -->
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="<c:url value="/assets/js/drawer-menu.js" />"></script>
    <script type="text/javascript" src="<c:url value="/assets/js/highlight.pack.js" />"></script>
    <script type="text/javascript">hljs.initHighlightingOnLoad();</script>
</body>
</html>