<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/submissions.css" />">
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
                    <li><a href="<c:url value="/competition" />">Competition</a></li>
                    <li><a href="<c:url value="/submission" />">Submission</a></li>
                    <li><a href="javascript:void(0)">More</a></li>
                </ul>
            </div> <!-- #nav -->
        </div> <!-- .container -->
    </div> <!-- #header -->
    <div id="content" class="container">
        <div id="main-content" class="row-fluid">
            <div id="submission" class="span12">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th class="flag">Result</th>
                            <th class="score">Score</th>
                            <th class="time">Time</th>
                            <th class="memory">Memory</th>
                            <th class="name">Name</th>
                            <th class="user">User</th>
                            <th class="language">Language</th>
                            <th class="submit-time">Submit Time</th>
                        </tr>
                    </thead>
                    <tbody>
                    	<c:forEach var="submission" items="${submissions}">
                        <tr>
                            <td class="flag-${submission.getRuntimeResult().getRuntimeResultSlug().toLowerCase()}"><a href="<c:url value="/submission/${submission.getSubmissionID()}" />">${submission.getRuntimeResult().getRuntimeResultName()}</a></td>
							<td class="score">${submission.getRuntimeScore()}</td>
							<td class="time">${submission.getUsedTime()} ms</td>
							<td class="memory">${submission.getUsedMemory()} K</td>
							<td class="name"><a href="<c:url value="/p/${submission.getProblem().getProblemID()}" />">P${submission.getProblem().getProblemID()} ${submission.getProblem().getProblemName()}</a></td>
							<td class="user"><a href="<c:url value="/accounts/user?${submission.getUser().getUid()}" />">${submission.getUser().getUsername()}</a></td>
							<td class="language">${submission.getLanguage().getLanguageName()}</td>
							<td class="submit-time">${submission.getSubmitTime()}</td>
                        </tr>
                        </c:forEach>
                        <tr class="more-submissions">
                            <td colspan="8">More Submission...</td>
                        </tr>
                    </tbody>
                </table>
            </div> <!-- #problems -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <div id="footer">
        <div class="container">
            <p id="copyright">Copyright&copy; 2005-2014 <a href="http://www.zjhzxhz.com/" target="_blank">HApPy Studio</a>. All rights reserved.</p>
        </div> <!-- .container -->
    </div> <!-- #footer -->
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
</body>
</html>