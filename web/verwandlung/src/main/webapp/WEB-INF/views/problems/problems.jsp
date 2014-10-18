<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <title>Problems | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/problems.css" />">
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
        <div id="locator">
            <ul class="inline">
                <li>Locator:</li>
                <c:forEach var="locatorID" begin="${startIndexOfProblems}" end="${startIndexOfProblems + totalProblems}" step="${numberOfProblemsPerPage}">
                <li><a href="<c:url value="/p?start=${locatorID}" />">P${locatorID}</a></li>
                </c:forEach>
            </ul>
        </div> <!-- #locator -->
        <div id="main-content" class="row-fluid">
            <div id="problems" class="span8">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th class="th-flag">Flag</th>
                            <th class="th-name">Name</th>
                            <th class="th-submission">Submission</th>
                            <th class="th-ac-rate">AC%</th>
                        </tr>
                    </thead>
                    <tbody>
                    	<c:forEach var="problem" items="${problems}">
                        <tr>
                            <td class="flag-ac"><a href="#">AC</a></td>
                            <td class="name"><a href="<c:url value="/p/${problem.getProblemID()}" />">P${problem.getProblemID()} ${problem.getProblemName()}</a></td>
                            <td>37460</td>
                            <td>82%</td>
                        </tr>
                        </c:forEach>
                        <tr class="more-problems">
                            <td colspan="4">More Problems...</td>
                        </tr>
                    </tbody>
                </table>
            </div> <!-- #problems -->
            <div id="sidebar" class="span4">
                <div id="search-widget" class="widget">
                    <h4>Search</h4>
                    <form id="search-form" action="#">
                        
                    </form>
                </div> <!-- #search-widget -->
                <div id="filter-widget" class="widget">
                    <h4>Filter</h4>
                </div> <!-- #filter-widget -->
            </div> <!-- #sidebar -->
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