<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.privacy.title" text="Privacy &amp; Cookie" /> | ${websiteName}</title>
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
        <div id="ribbon" style="height: 500px;"></div> <!-- #ribbon -->
        <div class="container">
            <div class="row-fluid">
                <div class="span9">
                    <div id="main-content">
                        <div class="section">
                            <div class="markdown"><spring:message code="voj.misc.privacy.introduction-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="information-gathering-and-usage"><spring:message code="voj.misc.privacy.information-gathering-and-usage" text="Information Gathering and Usage" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.information-gathering-and-usage-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="cookies"><spring:message code="voj.misc.privacy.cookies" text="Cookies" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.cookies-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="information-protection"><spring:message code="voj.misc.privacy.information-protection" text="Protection of Personal Information" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.information-protection-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="information-integrity-and-retention"><spring:message code="voj.misc.privacy.information-integrity-and-retention" text="Integrity and Retention of Personal Information" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.information-integrity-and-retention-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="information-access"><spring:message code="voj.misc.privacy.information-access" text="Access to Personal Information" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.information-access-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="children"><spring:message code="voj.misc.privacy.children" text="Children" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.children-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="disclosure"><spring:message code="voj.misc.privacy.disclosure" text="Disclosure to Third Parties" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.disclosure-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="commitment"><spring:message code="voj.misc.privacy.commitment" text="Commitment to Your Privacy" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.commitment-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="changes"><spring:message code="voj.misc.privacy.changes" text="Changes" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.privacy.changes-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                    </div> <!-- #main-content -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div id="sidebar-nav">
                        <h5><spring:message code="voj.misc.privacy.privacy-and-cookie" text="Privacy &amp; Cookie" /></h5>
                        <ul class="contents">
                            <li><a href="#information-gathering-and-usage"><spring:message code="voj.misc.privacy.information-gathering-and-usage" text="Information Gathering and Usage" /></a></li>
                            <li><a href="#cookies"><spring:message code="voj.misc.privacy.cookies" text="Cookies" /></a></li>
                            <li><a href="#information-protection"><spring:message code="voj.misc.privacy.information-protection" text="Protection of Personal Information" /></a></li>
                            <li><a href="#information-integrity-and-retention"><spring:message code="voj.misc.privacy.information-integrity-and-retention" text="Integrity and Retention of Personal Information" /></a></li>
                            <li><a href="#information-access"><spring:message code="voj.misc.privacy.information-access" text="Access to Personal Information" /></a></li>
                            <li><a href="#children"><spring:message code="voj.misc.privacy.children" text="Children" /></a></li>
                            <li><a href="#disclosure"><spring:message code="voj.misc.privacy.disclosure" text="Disclosure to Third Parties" /></a></li>
                            <li><a href="#commitment"><spring:message code="voj.misc.privacy.commitment" text="Commitment to Your Privacy" /></a></li>
                            <li><a href="#changes"><spring:message code="voj.misc.privacy.changes" text="Changes" /></a></li>
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