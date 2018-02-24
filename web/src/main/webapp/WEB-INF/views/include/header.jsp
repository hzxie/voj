<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
    <!--[if lte IE 7]>
    <div id="upgrade-browser">
        <div class="row-fluid container">
            <div class="span8">
                <div class="notice">
                    <c:url var="WebsiteName" value="${websiteName}" />
                    <h6>
                        <spring:message code="voj.include.header.browser-not-supported" 
                            text="Please note that Verwandlung Online Judge no longer supports Internet Explorer 7 and 8." 
                            arguments="${websiteName}" />
                    </h6>
                    <p><spring:message code="voj.include.header.browser-not-supported-message" text="We recommend upgrading to the latest <a href='http://www.microsoft.com/windows/internet-explorer/default.aspx'>Internet Explorer</a>, <a href='http://www.google.com/chrome/'>Google Chrome</a> or <a href='http://www.mozilla.com/firefox/'>Firefox</a>.<br>If you are using IE 9 or later, make sure you turn off 'Compatibility View'." /></p>
                </div>
            </div>
            <div class="span4">
                <button class="btn btn-primary" onclick="window.location.href='<c:url value="/help#browsers" />';"><spring:message code="voj.include.header.learn-more" text="Learn More" /></button>
                <button class="btn btn-danger" onclick="$('#upgrade-browser').fadeOut();"><spring:message code="voj.include.header.ignore" text="Ignore" /></button>
            </div>
        </div>
    </div>
    <![endif]-->
    <div id="header" class="row-fluid">
        <div class="container">
            <div id="logo" class="span6">
                <a href="<c:url value="/" />">
                    <img src="${cdnUrl}/img/logo.png?v=${version}" alt="Logo" />
                </a>
            </div> <!-- #logo -->
            <div id="nav" class="span6">
                <ul class="inline">
                    <li><a href="<c:url value="/p" />"><spring:message code="voj.include.header.problems" text="Problems" /></a></li>
                    <li><a href="<c:url value="/discussion" />"><spring:message code="voj.include.header.discussion" text="Discussion" /></a></li>
                    <li><a href="<c:url value="/contest" />"><spring:message code="voj.include.header.contests" text="Contests" /></a></li>
                    <li><a href="<c:url value="/submission" />"><spring:message code="voj.include.header.submission" text="Submission" /></a></li>
                    <li><a href="javascript:openDrawerMenu()"><spring:message code="voj.include.header.more" text="More" /></a></li>
                </ul>
            </div> <!-- #nav -->
        </div> <!-- .container -->
    </div> <!-- #header -->
    <div id="drawer-nav">
        <span class="close-trigger"><a href="javascript:closeDrawerMenu();"><spring:message code="voj.include.header.close" text="Close" /> &times;</a></span>
        <div id="accounts" class="section">
            <h4><spring:message code="voj.include.header.my-accounts" text="My Accounts" /></h4>
            <div id="profile">
            <c:choose>
            <c:when test="${isLogin}">
                <img src="${cdnUrl}/img/avatar.jpg?v=${version}" alt="avatar" class="img-circle" />
                <h5>${myProfile.username}</h5>
                <p class="email">${myProfile.email}</p>
                <p><spring:message code="voj.include.header.accepted" text="Accepted" />/<spring:message code="voj.include.header.submit" text="Submit" />: ${mySubmissionStats.get("acceptedSubmission")}/${mySubmissionStats.get("totalSubmission")}(${mySubmissionStats.get("acRate")}%)</p>
                <p><spring:message code="voj.include.header.language-preference" text="Language Preference" />: ${myProfile.preferLanguage.languageName}</p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/dashboard" />"><spring:message code="voj.include.header.dashboard" text="Dashboard" /></a></li>
                    <li><a href="<c:url value="/accounts/login?logout=true" />"><spring:message code="voj.include.header.sign-out" text="Sign out" /></a></li>
                </ul>
            </c:when>
            <c:otherwise>
                <p><spring:message code="voj.include.header.not-logged-in" text="You are not logged in." /></p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/login?forward=" />${requestScope['javax.servlet.forward.request_uri']}"><spring:message code="voj.include.header.sign-in" text="Sign in" /></a></li>
                    <li><a href="<c:url value="/accounts/register?forward=" />${requestScope['javax.servlet.forward.request_uri']}"><spring:message code="voj.include.header.sign-up" text="Sign up" /></a></li>
                </ul>
            </c:otherwise>
            </c:choose>
            </div> <!-- #profile -->
        </div> <!-- .section -->
        <div id="about" class="section">
            <h4><spring:message code="voj.include.header.about" text="About" /></h4>
            <ul>
                <li><a href="<c:url value="/judgers" />"><spring:message code="voj.include.header.judgers" text="Judgers" /></a></li>
                <li><a href="https://github.com/zjhzxhz/voj/issues" target="_blank"><spring:message code="voj.include.header.feedback" text="Feedback" /></a></li>
                <li><a href="<c:url value="/about" />"><spring:message code="voj.include.header.about-us" text="About us" /></a></li>
            </ul>
        </div> <!-- .section -->
    </div> <!-- #drawer-nav -->