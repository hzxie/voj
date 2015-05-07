<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<jsp:useBean id="date" class="java.util.Date" />
    <div id="footer">
        <div class="container">
            <ul id="footer-nav" class="inline">
            	<li><a href="<c:url value="/terms" />"><spring:message code="voj.include.footer.terms" text="Terms of Use" /></a></li>
                <li><a href="<c:url value="/privacy" />"><spring:message code="voj.include.footer.privacy" text="Privacy & Cookies" /></a></li>
                <li><a href="<c:url value="/judgers" />"><spring:message code="voj.include.footer.judgers" text="Judgers" /></a></li>
                <li><a href="<c:url value="/help" />"><spring:message code="voj.include.footer.help" text="Help" /></a></li>
                <li><a href="<c:url value="/about" />"><spring:message code="voj.include.footer.about-us" text="About Us" /></a></li>
                <li>
                    <a href="<c:url value="/locale" />">
                        <i class="fa fa-globe"></i>
                        <span id="current-language">${language}</span>
                    </a>
                </li>
            </ul>
            <p id="copyright">
                <spring:message code="voj.include.footer.copyright" text="Copyright" />&copy; 2005-<%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> <a href="http://zjhzxhz.com/" target="_blank">Trunk Shell</a>. 
                <spring:message code="voj.include.footer.all-rights-reserved" text="All rights reserved." />
            </p>
            <p>
                <iframe src="https://ghbtns.com/github-btn.html?user=zjhzxhz&repo=voj&type=star&count=false" frameborder="0" scrolling="0"></iframe>
            </p>
        </div> <!-- .container -->
    </div> <!-- #footer -->