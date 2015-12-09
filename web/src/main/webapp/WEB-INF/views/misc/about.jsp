<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.about.title" text="About Us" /> | ${websiteName}</title>
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
                            <h3 id="about-project"><spring:message code="voj.misc.about.about-project" text="About the Project" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.about.about-project-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h3 id="how-to-contribute"><spring:message code="voj.misc.about.how-to-contribute" text="How to Contribute" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.about.how-to-contribute-content" text="" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                        <div class="section">
                            <c:url var="termsUrl" value="/terms" />
                            <h3 id="license"><spring:message code="voj.misc.about.license" text="License" /></h3>
                            <div class="markdown"><spring:message code="voj.misc.about.license-content" text="" arguments="${termsUrl}" /></div> <!-- .markdown -->
                        </div> <!-- .section -->
                    </div> <!-- #main-content -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div id="sidebar-nav">
                        <h5><spring:message code="voj.misc.about.about-us" text="About Us" /></h5>
                        <ul class="contents">
                            <li><a href="#about-project"><spring:message code="voj.misc.about.about-project" text="About the Project" /></a></li>
                            <li><a href="#how-to-contribute"><spring:message code="voj.misc.about.how-to-contribute" text="How to Contribute" /></a></li>
                            <li><a href="#license"><spring:message code="voj.misc.about.license" text="License" /></a></li>
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
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>