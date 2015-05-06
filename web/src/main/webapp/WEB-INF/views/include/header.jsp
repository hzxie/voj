<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
    <div id="header" class="row-fluid">
        <div class="container">
            <div id="logo" class="span6">
                <a href="<c:url value="/" />">
                    <img src="${cdnUrl}/img/logo.png" alt="Logo" />
                </a>
            </div> <!-- #logo -->
            <div id="nav" class="span6">
                <ul class="inline">
                    <li><a href="<c:url value="/p" />"><spring:message code="voj.include.header.problems" text="Problems" /></a></li>
                    <li><a href="<c:url value="/discussion" />"><spring:message code="voj.include.header.discussion" text="Discussion" /></a></li>
                    <li><a href="<c:url value="/contests" />"><spring:message code="voj.include.header.contests" text="Contests" /></a></li>
                    <li><a href="<c:url value="/submission" />"><spring:message code="voj.include.header.submission" text="Submission" /></a></li>
                    <li><a href="javascript:openDrawerMenu()"><spring:message code="voj.include.header.more" text="More" /></a></li>
                </ul>
            </div> <!-- #nav -->
        </div> <!-- .container -->
    </div> <!-- #header -->
    <div id="drawer-nav">
        <span class="pull-right"><a href="javascript:closeDrawerMenu();"><spring:message code="voj.include.header.close" text="Close" /> &times;</a></span>
        <div id="accounts" class="section">
            <h4><spring:message code="voj.include.header.my-accounts" text="My Accounts" /></h4>
            <div id="profile">
            <c:choose>
            <c:when test="${isLogin}">
                <img src="${cdnUrl}/img/avatar.jpg" alt="avatar" class="img-circle" />
                <h5>${user.username}</h5>
                <p>${user.email}</p>
                <p><spring:message code="voj.include.header.accepted" text="Accepted" />/<spring:message code="voj.include.header.submit" text="Submit" />: ${submissionStats.get("acceptedSubmission")}/${submissionStats.get("totalSubmission")}(${submissionStats.get("acRate")}%)</p>
                <p><spring:message code="voj.include.header.language-preference" text="Language Preference" />: ${user.preferLanguage.languageName}</p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/dashboard" />"><spring:message code="voj.include.header.dashboard" text="Dashboard" /></a></li>
                    <li><a href="<c:url value="/accounts/login?logout=true" />"><spring:message code="voj.include.header.sign-out" text="Sign out" /></a></li>
                </ul>
            </c:when>
            <c:otherwise>
                <p><spring:message code="voj.include.header.not-logged-in" text="You are not logged in." /></p>
                <ul class="inline">
                    <li><a href="<c:url value="/accounts/login" />"><spring:message code="voj.include.header.sign-in" text="Sign in" /></a></li>
                    <li><a href="<c:url value="/accounts/register" />"><spring:message code="voj.include.header.sign-up" text="Sign up" /></a></li>
                </ul>
            </c:otherwise>
            </c:choose>
            </div> <!-- #profile -->
        </div> <!-- .section -->
        <div id="about" class="section">
            <h4><spring:message code="voj.include.header.about" text="About" /></h4>
            <ul>
                <li><a href="#"><spring:message code="voj.include.header.judgers" text="Judgers" /></a></li>
                <li><a href="#"><spring:message code="voj.include.header.feedback" text="Feedback" /></a></li>
                <li><a href="#"><spring:message code="voj.include.header.about-us" text="About us" /></a></li>
            </ul>
        </div> <!-- .section -->
    </div> <!-- #drawer-nav -->