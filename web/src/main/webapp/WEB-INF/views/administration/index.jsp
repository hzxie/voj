<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.index.title" text="System Administration" /> | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css" />
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
    <div id="wrapper">
        <div id="sidebar">
            <div id="logo">
                <a href="<c:url value="/" />">
                    <img src="${cdnUrl}/img/logo-light.png" alt="Logo" />
                </a>
            </div> <!-- #logo -->
            <div id="sidebar-user">
                <div class="row-fluid">
                    <div class="span3">
                        <img src="${cdnUrl}/img/avatar.jpg" alt="avatar">
                    </div> <!-- .span3 -->
                    <div class="offset1 span8">
                        <spring:message code="voj.administration.index.welcome-back" text="Welcome back," /> <br>${user.username} <br>
                        <span class="label label-success"><spring:message code="voj.administration.index.online" text="Online" /></span>
                    </div> <!-- .span8 -->
                </div> <!-- .row-fluid -->
            </div> <!-- #sidebar-user -->
            <div id="sidebar-nav">
                <ul class="nav">
                    <li class="primary-nav-item">
                        <a href="#dashboard"><i class="fa fa-dashboard"></i> <spring:message code="voj.administration.index.dashboard" text="Dashboard" /></a>
                    </li>
                    <li class="primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-users"></i> <spring:message code="voj.administration.index.users" text="Users" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="secondary-nav-item"><a href="#all-users"><spring:message code="voj.administration.index.all-users" text="All Users" /></a></li>
                            <li class="secondary-nav-item"><a href="#new-user"><spring:message code="voj.administration.index.new-user" text="New User" /></a></li>
                            <li class="secondary-nav-item hide"><a href="#edit-user"><spring:message code="voj.administration.index.edit-user" text="Edit User" /></a></li>
                        </ul>
                    </li>
                    <li class="primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-question-circle"></i> <spring:message code="voj.administration.index.problems" text="Problems" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="secondary-nav-item"><a href="#all-problems"><spring:message code="voj.administration.index.all-problems" text="All Problems" /></a></li>
                            <li class="secondary-nav-item"><a href="#new-problem"><spring:message code="voj.administration.index.new-problem" text="New Problem" /></a></li>
                            <li class="secondary-nav-item"><a href="#problem-categories"><spring:message code="voj.administration.index.problem-categories" text="Categories" /></a></li>
                            <li class="secondary-nav-item"><a href="#problem-tags"><spring:message code="voj.administration.index.problem-tags" text="Tags" /></a></li>
                            <li class="secondary-nav-item hide"><a href="#edit-problem"><spring:message code="voj.administration.index.edit-problem" text="Edit Problem" /></a></li>
                        </ul>
                    </li>
                    <li class="primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-comment"></i> <spring:message code="voj.administration.index.discussion" text="Discussion" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="secondary-nav-item"><a href="#all-discussion"><spring:message code="voj.administration.index.all-discussion" text="All Discussion" /></a></li>
                            <li class="secondary-nav-item"><a href="#discussion-topics"><spring:message code="voj.administration.index.discussion-topics" text="Topics" /></a></li>
                        </ul>
                    </li>
                    <li class="primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-paperclip"></i> <spring:message code="voj.administration.index.contests" text="Contests" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="secondary-nav-item"><a href="#all-contests"><spring:message code="voj.administration.index.all-contests" text="All Contests" /></a></li>
                            <li class="secondary-nav-item"><a href="#new-contest"><spring:message code="voj.administration.index.new-contest" text="New Contest" /></a></li>
                            <li class="secondary-nav-item hide"><a href="#edit-contest"><spring:message code="voj.administration.index.edit-contest" text="Edit Contest" /></a></li>
                        </ul>
                    </li>
                    <li class="primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-code"></i> <spring:message code="voj.administration.index.submissions" text="Submissions" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="secondary-nav-item"><a href="#all-submissions"><spring:message code="voj.administration.index.all-submissions" text="All Submissions" /></a></li>
                            <li class="secondary-nav-item hide"><a href="#edit-submission"><spring:message code="voj.administration.index.edit-submission" text="Edit Submission" /></a></li>
                        </ul>
                    </li>
                    <li class="primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-cogs"></i> <spring:message code="voj.administration.index.settings" text="Settings" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="secondary-nav-item"><a href="#general-settings"><spring:message code="voj.administration.index.general-settings" text="General" /></a></li>
                        </ul>
                    </li>
                </ul>
            </div> <!-- #sidebar-nav -->
        </div> <!-- #sidebar -->
        <div id="container">
            <div id="header">
                <a id="sidebar-toggle" href="javascript:void(0);"><i class="fa fa-bars"></i></a>
                <ul class="nav inline pull-right">
                    <li><a href="javascript:void(0);"><i class="fa fa-tasks"></i></a></li>
                    <li><a href="javascript:void(0);"><i class="fa fa-bell"></i></a></li>
                    <li><a href="javascript:void(0);">${user.username} <img src="${cdnUrl}/img/avatar.jpg" alt="avatar"></a></li>
                </ul>
            </div> <!-- #header -->
            <div id="content">
                Content
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        $('#sidebar-toggle').click(function() {
            var isSidebarShown = $('#sidebar').is(':visible');

            if ( isSidebarShown ) {
                $('#sidebar').css('display', 'none');
                $('#header').css('padding-left', 0);
                $('#content').css('margin-left', 0);
            } else {
                $('#sidebar').css('display', 'block');
                $('#header').css('padding-left', 250);
                $('#content').css('margin-left', 250);
            }
        });
    </script>
    <script type="text/javascript">
        $('li.nav-item-has-children > a').click(function() {
            var navItem       = $(this).parent(),
                isSubNavShown = $('ul.sub-nav', navItem).is(':visible');

            if ( isSubNavShown ) {
                $('i.fa-caret-down', navItem).addClass('fa-caret-right');
                $('i.fa-caret-down', navItem).removeClass('fa-caret-down');
                $('ul.sub-nav', navItem).slideUp(120);
            } else {
                $('i.fa-caret-right', navItem).addClass('fa-caret-down');
                $('i.fa-caret-right', navItem).removeClass('fa-caret-right');
                $('ul.sub-nav', navItem).slideDown(120);
            }
        });
    </script>
    <script type="text/javascript">
        $(function() {
            var pageName = window.location.href.match(/#.*$/);

            if ( pageName != null ) {
                getPageContent(window.location.href.match(/#.*$/)[0].substr(1));
            } else {
                window.location.href = '#dashboard';
            }
        });
    </script>
    <script type="text/javascript">
        if ( ("onhashchange" in window) && !navigator.userAgent.toLowerCase().match(/msie/) ) {
            $(window).on('hashchange', function() {
                getPageContent(window.location.href.match(/#.*$/)[0].substr(1));
            });
        } else {
            var prevHash = window.location.hash;
            window.setInterval(function () {
               if (window.location.hash != prevHash) {
                  prevHash = window.location.hash;
                  getPageContent(window.location.href.match(/#.*$/)[0].substr(1));
               }
            }, 100);
        }
    </script>
    <script type="text/javascript">
        function getPageContent(pageName) {
        	var pageRequests = getPageRequests(pageName),
                pageName     = pageRequests['pageName'],
                pageRequest  = pageRequests['pageRequest'];

            highlightNavItem(pageName);
            $.ajax({
                
            });
        }
    </script>
    <script type="text/javascript">
        function getPageRequests(pageName) {
            var pageRequests = pageName.split('?');
            return {
                'pageName' : pageRequests[0],
                'pageRequest' : pageRequests[1] || ''
            };
        }
    </script>
    <script type="text/javascript">
        function highlightNavItem(pageName) {
            $('.active').removeClass('active');

            var navItem = $('a[href=#' + pageName + ']');

            if ( navItem.length != 0 ) {
                /* HighLight */
                $(navItem.parent()).addClass('active');
                $('li.primary-nav-item', '#sidebar-nav').each(function() {
                    if ( $('li.secondary-nav-item.active', $(this)).length != 0 ) {
                        $(this).addClass('active');
                    }
                });

                /* Set Window Title */
                var windowTitle    = $(navItem).html();
                if ( typeof(windowTitle) != 'undefined' ) {
                    windowTitle    = $(navItem).html().replace(/<i .*><\/i> /g, '');
                    document.title = windowTitle + ' | Verwandlung Online Judge';
                }
            }
        }
    </script>
</body>
</html>