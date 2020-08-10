<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.judgers.title" text="Judgers" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/misc/about.css?v=${version}" />
    <style type="text/css">
        table th.key,
        table td.key {
            width: 20%;
        }

        table th.value,
        table td.value {
            width: 80%;
        }

        span.online {
            color: #f00;
        }
    </style>
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
    <div id="content">
        <div id="ribbon"></div> <!-- #ribbon -->
        <div class="container">
            <div class="row-fluid">
                <div class="span9">
                    <div id="main-content">
                        <div class="section">
                            <h3 id="browsers"><spring:message code="voj.misc.judgers.compile-command" text="Compile Command" /></h3>
                            <table class="table table-striped">
                                <thead>
                                    <tr>
                                        <th class="key"><spring:message code="voj.misc.judgers.language" text="Language" /></th>
                                        <th class="value"><spring:message code="voj.misc.judgers.compile-command" text="Compile Command" /></th>
                                    </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="language" items="${languages}">
                                    <tr>
                                        <td>${language.languageName}</td>
                                        <td>${language.compileCommand}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="judgers"><spring:message code="voj.misc.judgers.judgers" text="Judgers" /></h3>
                            <p id="no-judgers"><spring:message code="voj.misc.judgers.no-judgers" text="No Judgers." /></p>
                            <table id="judgers-list" class="table table-striped hide">
                                <thead>
                                    <tr>
                                        <th class="key"><spring:message code="voj.misc.judgers.judger-name" text="Name" /></th>
                                        <th class="value"><spring:message code="voj.misc.judgers.judger-description" text="Description" /></th>
                                    </tr>
                                </thead>
                                <tbody></tbody>
                            </table>
                        </div> <!-- .section -->
                    </div> <!-- #main-content -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div id="sidebar-nav">
                        <h5><spring:message code="voj.misc.judgers.judgers" text="Judgers" /></h5>
                        <ul class="contents">
                            <li><a href="#compile-command"><spring:message code="voj.misc.judgers.compile-command" text="Compile Command" /></a></li>
                            <li><a href="#judgers"><spring:message code="voj.misc.judgers.judgers" text="Judgers" /></a></li>
                        </ul>
                    </div> <!-- #sidebar-nav -->
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
        </div> <!-- .container -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript">
        $(function() {
            $.ajax({
                type: 'GET',
                url: '<c:url value="/getJudgers.action" />',
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        processResult(result['judgers']);

                        $('#no-judgers').addClass('hide');
                        $('#judgers-list').removeClass('hide');
                    }
                }
            });
        });
    </script>
    <script type="text/javascript">
        function processResult(judgers) {
            for ( var i = 0; i < judgers.length; ++ i ) {
                $('#judgers-list').append(
                    getJudgerContent(judgers[i]['username'], judgers[i]['description'])
                );
            }
        }
    </script>
    <script type="text/javascript">
        function getJudgerContent(username, description) {
            var judgerInfoTemplate = '<tr>' + 
                                     '    <td>%s</td>' +
                                     '    <td>%s</td>' +
                                     '</tr>';

            description = description.replace('[Online]', '<span class="online">[Online]</span>');
            description = description.replace('[Offline]', '<span class="offline">[Offline]</span>');
            return judgerInfoTemplate.format(username, description);
        }
    </script>
    <script type="text/javascript"></script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>