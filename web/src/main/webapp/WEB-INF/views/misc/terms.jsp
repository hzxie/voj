<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.terms.title" text="Terms of Use" /> | ${websiteName}</title>
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
                            <div class="markdown"><spring:message code="voj.misc.terms.introduction-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="using-services"><spring:message code="voj.misc.terms.using-services" text="Using our Services" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.using-services-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="your-account"><spring:message code="voj.misc.terms.your-account" text="Your Account" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.your-account-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <c:url var="privacyUrl" value="/privacy" />
                            <h3 id="privacy-and-copyright"><spring:message code="voj.misc.terms.privacy-and-copyright" text="Privacy and Copyright Protection" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.privacy-and-copyright-content" text="" arguments="${websiteName}, ${privacyUrl}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="your-content"><spring:message code="voj.misc.terms.your-content" text="Your Content in our Services" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.your-content-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="software-in-service"><spring:message code="voj.misc.terms.software-in-service" text="About Software in our Services" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.software-in-service-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="service-modify-terminate"><spring:message code="voj.misc.terms.service-modify-terminate" text="Modifying and Terminating our Services" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.service-modify-terminate-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="warranties-and-disclaimers"><spring:message code="voj.misc.terms.warranties-and-disclaimers" text="Our Warranties and Disclaimers" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.warranties-and-disclaimers-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="liability"><spring:message code="voj.misc.terms.liability" text="Liability for our Services" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.liability-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="business-use"><spring:message code="voj.misc.terms.business-use" text="Business uses of our Services" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.business-use-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="about-terms"><spring:message code="voj.misc.terms.about-terms" text="About these Terms" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.terms.about-terms-content" text="" arguments="${websiteName}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                    </div> <!-- #main-content -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div id="sidebar-nav">
                        <h5><spring:message code="voj.misc.terms.terms-of-use" text="Terms of Use" /></h5>
                        <ul class="contents">
                            <li><a href="#using-services"><spring:message code="voj.misc.terms.using-services" text="Using our Services" /></a></li>
                            <li><a href="#your-account"><spring:message code="voj.misc.terms.your-account" text="Your Account" /></a></li>
                            <li><a href="#privacy-and-copyright"><spring:message code="voj.misc.terms.privacy-and-copyright" text="Privacy and Copyright Protection" /></a></li>
                            <li><a href="#your-content"><spring:message code="voj.misc.terms.your-content" text="Your Content in our Services" /></a></li>
                            <li><a href="#software-in-service"><spring:message code="voj.misc.terms.software-in-service" text="About Software in our Services" /></a></li>
                            <li><a href="#service-modify-terminate"><spring:message code="voj.misc.terms.service-modify-terminate" text="Modifying and Terminating our Services" /></a></li>
                            <li><a href="#warranties-and-disclaimers"><spring:message code="voj.misc.terms.warranties-and-disclaimers" text="Our Warranties and Disclaimers" /></a></li>
                            <li><a href="#liability"><spring:message code="voj.misc.terms.liability" text="Liability for our Services" /></a></li>
                            <li><a href="#business-use"><spring:message code="voj.misc.terms.business-use" text="Business uses of our Services" /></a></li>
                            <li><a href="#about-terms"><spring:message code="voj.misc.terms.about-terms" text="About these Terms" /></a></li>
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