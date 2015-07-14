<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.judgers.title" text="Judgers" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/misc/about.css" />
    <style type="text/css">
        table td.key {
            width: 25%;
        }

        table td.value {
            width: 75%;
        }

        span.online {
            color: #f00;
        }
    </style>
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
                                        <th><spring:message code="voj.misc.judgers.language" text="Language" /></th>
                                        <th><spring:message code="voj.misc.judgers.compile-command" text="Compile Command" /></th>
                                    </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="language" items="${languages}">
                                    <tr>
                                        <td class="key">${language.languageName}</td>
                                        <td class="value">${language.compileCommand}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="judgers"><spring:message code="voj.misc.judgers.judgers" text="Judgers" /></h3>
                            <p id="no-judgers"><spring:message code="voj.misc.judgers.no-judgers" text="No Judgers." /></p>
                            <table id="judgers-list" class="table table-striped">
                                <thead>
                                    <tr>
                                        <th class="key"><spring:message code="voj.misc.judgers.judger-name" text="Name" /></th>
                                        <th class="value"><spring:message code="voj.misc.judgers.judger-description" text="Description" /></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>Demo Judger #1</td>
                                        <td><span class="online">[Online]</span> Intel Xeon E5-2630 2.30GHz / 256 GB RAM</td>
                                    </tr>
                                    <tr>
                                        <td>Demo Judger #2</td>
                                        <td><span class="offline">[Offline]</span> Intel Xeon E3-1230 3.30GHz / 32 GB RAM</td>
                                    </tr>
                                </tbody>
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
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
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
    <c:if test="${GoogleAnalyticsCode != ''}">
    <script type="text/javascript">${googleAnalyticsCode}</script>
    </c:if>
</body>
</html>