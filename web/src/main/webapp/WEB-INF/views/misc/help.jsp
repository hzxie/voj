<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.help.title" text="Help Center" /> | ${websiteName}</title>
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
                            <h3 id="browsers"><spring:message code="voj.misc.help.browsers-support" text="Supported Browsers" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.help.browsers-support-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="io"><spring:message code="voj.misc.help.input-and-output" text="Input and Output" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.help.input-and-output-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="judge-results"><spring:message code="voj.misc.help.judge-results-meaning" text="Meaning of Judge Results" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.help.judge-results-meaning-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="compile-error"><spring:message code="voj.misc.help.compile-error-reason" text="Reason for Compile Error" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.help.compile-error-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="more"><spring:message code="voj.misc.help.more" text="More" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.help.more-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                    </div> <!-- #main-content -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div id="sidebar-nav">
                        <h5><spring:message code="voj.misc.help.help-and-support" text="Help and Support" /></h5>
                        <ul class="contents">
                            <li><a href="#browsers"><spring:message code="voj.misc.help.browsers-support" text="Supported Browsers" /></a></li>
                            <li><a href="#io"><spring:message code="voj.misc.help.input-and-output" text="Input and Output" /></a></li>
                            <li><a href="#judge-results"><spring:message code="voj.misc.help.judge-results-meaning" text="Meaning of Judge Results" /></a></li>
                            <li><a href="#compile-error"><spring:message code="voj.misc.help.compile-error-reason" text="Reason for Compile Error" /></a></li>
                            <li><a href="#more"><spring:message code="voj.misc.help.more" text="More" /></a></li>
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
        $.getScript('${cdnUrl}/js/markdown.min.js?v=${version}', function() {
            converter = Markdown.getSanitizingConverter();

            $('.markdown').each(function() {
                var plainContent    = $(this).text(),
                    markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                
                $(this).html(markdownContent);
            });
        });
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>