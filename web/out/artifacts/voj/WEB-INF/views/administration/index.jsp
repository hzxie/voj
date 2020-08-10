<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="build" />
<spring:eval expression="@propertyConfigurer.getProperty('product.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.index.title" text="System Administration" /> | ${websiteName}</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="${description}">
    <meta name="author" content="Haozhe Xie">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico?v=${build}" rel="shortcut icon" type="image/x-icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css?v=${build}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css?v=${build}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css?v=${build}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css?v=${build}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css?v=${build}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/dashboard.css?v=${build}" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js?v=${build}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js?v=${build}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js?v=${build}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/pace.min.js?v=${build}"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js?v=${build}"></script>
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
                <h2 class="page-header">
                    <i class="fa fa-dashboard"></i> <spring:message code="voj.administration.index.page-header" text="Dashboard" />
                </h2>
                <div id="overview" class="row-fluid">
                    <div class="span3">
                        <div id="overview-users" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-users"></i>
                                </div> <!-- .span4 -->
                                <div class="span8">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.total-users" text="Total Users" /></span>
                                    <h2>${totalUsers}</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <div class="row-fluid">
                                <div class="span6 border-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.new-users-today" text="New Users Today" /></span>
                                    <h4>${newUsersToday}</h4>
                                </div> <!-- .span6 -->
                                <div class="span6">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.online-users" text="Online Users" /></span>
                                    <h4>${onlineUsers}</h4>
                                </div> <!-- .span6 -->
                            </div> <!-- .row-fluid -->
                        </div> <!-- #overview-users -->
                    </div> <!-- .span3 -->
                    <div class="span3">
                        <div id="overview-problems" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-question-circle"></i>
                                </div> <!-- .span4 -->
                                <div class="span8">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.total-problems" text="Total Problems" /></span>
                                    <h2>${totalProblems}</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <div class="row-fluid">
                                <div class="span6 border-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.total-checkpoints" text="Total Checkpoints" /></span>
                                    <h4>${numberOfCheckpoints}</h4>
                                </div> <!-- .span6 -->
                                <div class="span6">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.private-problems" text="Private Problems" /></span>
                                    <h4>${privateProblems}</h4>
                                </div> <!-- .span6 -->
                            </div> <!-- .row-fluid -->
                        </div> <!-- #overview-problems -->
                    </div> <!-- .span3 -->
                    <div class="span3">
                        <div id="overview-contests" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-paperclip"></i>
                                </div> <!-- .span4 -->
                                <div class="span8 text-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.upcoming-contests" text="Upcoming Contests" /></span>
                                    <h2>0</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <a href="<c:url value="/administration/all-contests" />" class="more">
                                <spring:message code="voj.administration.index.more-contests" text="More Contests" /> <i class="fa fa-arrow-circle-right"></i> 
                            </a>
                        </div> <!-- #overview-contests -->
                    </div> <!-- .span3 -->
                    <div class="span3">
                        <div id="overview-submissions" class="widget">
                            <div class="row-fluid glance">
                                <div class="span4 text-center">
                                    <i class="fa fa-code"></i>
                                </div> <!-- .span4 -->
                                <div class="span8 text-right">
                                    <span class="text-uppercase"><spring:message code="voj.administration.index.submissions-today" text="Submissions Today" /></span>
                                    <h2>${submissionsToday}</h2>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                            <a href="<c:url value="/administration/all-submissions" />" class="more">
                                <spring:message code="voj.administration.index.more-submissions" text="More Submissions" /> <i class="fa fa-arrow-circle-right"></i> 
                            </a>
                        </div> <!-- #overview-submissions -->
                    </div> <!-- .span3 -->
                </div> <!-- #overview -->
                <div class="row-fluid">
                    <div class="span8">
                        <div id="submissions-panel" class="panel">
                            <div class="header">
                                <div class="row-fluid">
                                    <div class="span8">
                                        <h5>
                                            <i class="fa fa-bar-chart"></i> 
                                            <spring:message code="voj.administration.index.submissions-stats" text="Submissions Stats" />
                                        </h5>
                                    </div> <!-- .span8 -->
                                    <div class="span4">
                                        <select id="submission-period">
                                            <option value="7"><spring:message code="voj.administration.index.1-week" text="1 Week" /></option>
                                            <option value="30"><spring:message code="voj.administration.index.1-month" text="1 Month" /></option>
                                            <option value="365"><spring:message code="voj.administration.index.1-year" text="1 Year" /></option>
                                        </select>
                                    </div> <!-- .span4 -->
                                </div> <!-- .row-fluid -->
                            </div> <!-- .header -->
                            <div class="body">
                                <div id="submissions-calendar"></div> <!-- #submissions-calendar -->
                            </div> <!-- .body -->
                        </div> <!-- #submissions-panel -->
                    </div> <!-- .span8 -->
                    <div class="span4">
                        <div id="system-panel" class="panel">
                            <div class="header">
                                <h5>
                                    <i class="fa fa-info-circle"></i> 
                                    <spring:message code="voj.administration.index.system-info" text="System Info" />
                                </h5>
                            </div> <!-- .header -->
                            <div class="body">
                                <div class="row-fluid">
                                    <div class="span4"><spring:message code="voj.administration.index.product-version" text="Product Version" /></div> <!-- .span4 -->
                                    <div id="product-version" class="span8">${version} (Build: <a href="https://github.com/hzxie/voj/commit/${build}" target="_blank">${build}</a>)</div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                                <div class="row-fluid">
                                    <div class="span4"><spring:message code="voj.administration.index.memory-usage" text="Memory Usage" /></div> <!-- .span4 -->
                                    <div class="span8">${memoryUsage} MB</div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                                <div class="row-fluid">
                                    <div class="span4"><spring:message code="voj.administration.index.online-judgers" text="Online Judgers" /></div> <!-- .span4 -->
                                    <div class="span8">${onlineJudgers}</div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                            </div> <!-- .body -->
                        </div> <!-- #system-panel -->
                    </div> <!-- .span4 -->
                </div> <!-- .row-fluid -->
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highcharts.min.js?v=${build}', function() {
            return getSubmissionsOfUsers(7);
        });
    </script>
    <script type="text/javascript">
        $('#submission-period').change(function() {
            var period = $(this).val();
            return getSubmissionsOfUsers(period);
        });
    </script>
    <script type="text/javascript">
        function getSubmissionsOfUsers(period) {
            var pageRequests = {
                'period': period
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/administration/getNumberOfSubmissions.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    return processSubmissionOfUsers(result['acceptedSubmissions'], result['totalSubmissions']);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processSubmissionOfUsers(acceptedSubmissionsMap, totalSubmissionsMap) {
            var categories          = [],
                totalSubmissions    = [],
                acceptedSubmissions = [];
            
            $.each(totalSubmissionsMap, function(key, value){
                categories.push(key);
                totalSubmissions.push(value);
            });
            $.each(acceptedSubmissionsMap, function(key, value){
                acceptedSubmissions.push(value);
            });
            displaySubmissionsOfUsers(categories, acceptedSubmissions, totalSubmissions);
        }
    </script>
    <script type="text/javascript">
        function displaySubmissionsOfUsers(categories, acceptedSubmissions, totalSubmissions) {
            Highcharts.setOptions({
                colors: ['#34495e', '#e74c3c']
            });

            $('#submissions-calendar').highcharts({
                chart: {
                    backgroundColor: null,
                },
                title: {
                    text: null
                },
                xAxis: {
                    categories: categories
                },
                yAxis: {
                    allowDecimals: false,
                    title: {
                        text: '<spring:message code="voj.administration.index.number-of-submissions" text="Number of Submissions" />'
                    }
                },
                tooltip: {
                    shared: true,
                    crosshairs: true
                },
                series: [
                    {
                        name: '<spring:message code="voj.administration.index.total-submissions" text="Total Submissions" />',
                        lineWidth: 4,
                        marker: {
                            radius: 4
                        },
                        data: totalSubmissions
                    },
                    {
                        name: '<spring:message code="voj.administration.index.accepted-submissions" text="Accepted Submissions" />',
                        data: acceptedSubmissions
                    }
                ]
            });
        }
    </script>
    <script type="text/javascript">
        $(function() {
            var currentVersion = $('#product-version').html(),
                newVersion     = '0.0.0';

            $.ajax({
                type: 'GET',
                url: 'https://api.github.com/repos/zjhzxhz/voj/releases/latest',
                dataType: 'jsonp',
                success: function(result){
                    newVersion = result['data']['name'];
                    if ( newVersion > currentVersion ) {
                        $('#product-version').html('<spring:message code="voj.administration.index.new-version-available" text="New version available" />: ' + newVersion);
                    }
                }
            });
        });
    </script>
</body>
</html>