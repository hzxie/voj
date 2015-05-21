<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.worldwide.title" text="Change Language" /> | Verwandlung Online Judge</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/misc/worldwide.css" />
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
    <div id="content" class="container">
        <div id="main-content">
            <h3><spring:message code="voj.misc.worldwide.other-languages" text="This site in other languages:" /></h3>
            <div class="row-fluid">
                <div class="span3">
                    <ul class="languages">
                        <li><a href="javascript:void(0);" data-value="en_US">English</a></li>
                    </ul>
                </div> <!-- .span3 -->
                <div class="span3">
                    <ul class="languages">
                        <li><a href="javascript:void(0);" data-value="zh_CN">简体中文</a></li>
                    </ul>
                </div> <!-- .span3 -->
                <div class="span3">
                </div> <!-- .span3 -->
                <div class="span3">
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        $('a', '.languages').click(function() {
            var languageCode = $(this).attr('data-value');

            var request = {
                'language': languageCode
            };
            $.ajax({
                type: 'GET',
                url: '<c:url value="/worldwide.action" />',
                data: request,
                dataType: 'JSON',
                success: function(result) {
                    if ( result['isSuccessful'] ) {
                        window.location.href = '<c:url value="/" />';
                    }
                }
            });
        });
    </script>
</body>
</html>