<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<jsp:useBean id="date" class="java.util.Date" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.misc.not-supported.title" text="Browser no longer supported" /> | ${websiteName}</title>
    <meta name="robots" content="noindex">
    <meta name="author" content="Haozhe Xie">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon" type="image/x-icon">
    <!-- StyleSheets -->
    <style type="text/css">
        body {
            margin: 50px;
            font: 13px arial,sans-serif;
        }

        a {
            color:#0152A6;
            cursor:pointer;
            outline:medium none;
            text-decoration:none;
        }

        a img {
            border: 0;
        }

        h1 {
            font-size: 17px;
        }

        .footer {
            font-size: 13px;
            text-align: center;
            margin-top: 40px;
        }
    </style>
</head>
<body>
    <img src="${cdnUrl}/img/logo.jpg" alt="Logo" width="218px" height="34px" />
    <h1><spring:message code="voj.misc.not-supported.browser-not-supported" text="Your Browser is no longer supported." /></h1>
    <p><spring:message code="voj.misc.not-supported.message" text="Verwandlung Online Judge no longer supports your browser. Please upgrade your browser." arguments="${websiteName}" /></p>
    <table width="650px" cellpadding="5">
        <tbody>
            <tr>
                <td align="center" valign="center" width="120px">
                    <a href="http://www.google.com/chrome/?hl=${language}">
                        <img src="${cdnUrl}/img/browsers/chrome.jpg" width="120px" height="120px" />
                    </a>
                </td>
                <td align="center" valign="center" width="120px">
                    <a href="http://www.mozilla.com/firefox/">
                        <img src="${cdnUrl}/img/browsers/firefox.jpg" width="120px" height="120px" />
                    </a>
                </td>
                <td align="center" valign="center" width="120px">
                    <a href="http://www.microsoft.com/windows/internet-explorer/default.aspx">
                        <img src="${cdnUrl}/img/browsers/ie.jpg" width="120px" height="120px" />
                    </a>
                </td>
                <td align="center" valign="center" width="120px">
                    <a href="http://www.opera.com/">
                        <img src="${cdnUrl}/img/browsers/opera.jpg" width="120px" height="120px" />
                    </a>
                </td>
                <td align="center" valign="center" width="120px">
                    <a href="http://www.apple.com/safari/download/">
                        <img src="${cdnUrl}/img/browsers/safari.jpg" width="120px" height="120px" />
                    </a>
                </td>
            </tr>
            <tr>
                <td align="center" valign="top"><a href="http://www.google.com/chrome/?hl=${language}"><spring:message code="voj.misc.not-supported.download" text="Download" /> Google Chrome</a></td>
                <td align="center" valign="top"><a href="http://www.mozilla.com/firefox/"><spring:message code="voj.misc.not-supported.download" text="Download" /> Firefox</a></td>
                <td align="center" valign="top"><a href="http://www.microsoft.com/windows/internet-explorer/default.aspx"><spring:message code="voj.misc.not-supported.download" text="Download" /> Internet Explorer</a></td>
                <td align="center" valign="top"><a href="http://www.opera.com/"><spring:message code="voj.misc.not-supported.download" text="Download" /> Opera</a></td>
                <td align="center" valign="top"><a href="http://www.apple.com/safari/download/"><spring:message code="voj.misc.not-supported.download" text="Download" /> Safari</a></td>
            </tr>
        </tbody>
    </table>
    <div class="footer">&copy;<%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> <a href="http://zjhzxhz.com/" target="_blank">Infinite Script</a></div>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>