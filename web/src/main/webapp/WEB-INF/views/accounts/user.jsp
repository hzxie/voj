<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.user.title" text="User" />: ${user.username} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/user.css" />
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
        <div class="row-fluid">
            <div id="sidebar" class="span3">
                <div id="vcard" class="section">
                    <img src="${cdnUrl}/img/avatar.jpg" alt="User Avatar" />
                    <h5>${user.username}</h5>
                </div> <!-- #vcard -->
            <c:if test="${not empty socialLinks && fn:length(socialLinks) > 0}">
                <div id="social-links" class="section">
                    <ul class="inline">
                    <c:if test="${not empty socialLinks['Facebook']}">
                        <li><a href="https://facebook.com/${socialLinks['Facebook']}" target="_blank" class="facebook" title="Facebook"><i class="fa fa-facebook"></i></a></li>
                    </c:if>
                    <c:if test="${not empty socialLinks['Twitter']}">
                        <li><a href="https://twitter.com/${socialLinks['Twitter']}" target="_blank" class="twitter" title="Twitter"><i class="fa fa-twitter"></i></a></li>
                    </c:if>
                    <c:if test="${not empty socialLinks['Weibo']}">
                        <li><a href="http://weibo.com/${socialLinks['Weibo']}" target="_blank" class="weibo" title="Weibo"><i class="fa fa-weibo"></i></a></li>
                    </c:if>
                    <c:if test="${not empty socialLinks['Instagram']}">
                        <li><a href="https://instagram.com/${socialLinks['Instagram']}" target="_blank" class="instagram" title="Instagram"><i class="fa fa-instagram"></i></a></li>
                    </c:if>
                    <c:if test="${not empty socialLinks['GitHub']}">
                        <li><a href="https://github.com/${socialLinks['GitHub']}" target="_blank" class="github" title="GitHub"><i class="fa fa-github"></i></a></li>
                    </c:if>
                    <c:if test="${not empty socialLinks['StackOverflow']}">
                        <li><a href="http://stackoverflow.com/users/${socialLinks['StackOverflow']}" target="_blank" class="stackoverflow" title="StackOverflow"><i class="fa fa-stack-overflow"></i></a></li>
                    </c:if>
                    <c:if test="${not empty socialLinks['LinkedIn']}">
                        <li><a href="https://www.linkedin.com/profile/view?id=${socialLinks['LinkedIn']}" target="_blank" class="linkedin" title="LinkedIn"><i class="fa fa-linkedin-square"></i></a></li>
                    </c:if>
                    </ul>
                </div> <!-- #social-links -->
            </c:if>
                <div id="vcard-details" class="section">
                    <ul>
                    <c:if test="${not empty location}">
                        <li><span class="icon"><i class="fa fa-map-marker"></i></span> ${location}</li>
                    </c:if>
                        <li><span class="icon"><i class="fa fa-envelope-o"></i></span> ${user.email}</li>
                    <c:if test="${not empty website}">
                        <li><span class="icon"><i class="fa fa-link"></i></span> <a href="${website}" target="_blank">${website}</a></li>
                    </c:if>
                        <li><span class="icon"><i class="fa fa-users"></i></span> ${user.userGroup.userGroupName}</li>
                        <li>
                            <span class="icon"><i class="fa fa-clock-o"></i></span> 
                            <spring:message code="voj.accounts.user.joined-on" text="Joined on" />
                            <fmt:parseDate pattern="yyyy-MM-dd HH:mm:ss" value="${registerTime}" var="parsedDateTime" />
                            <fmt:formatDate value="${parsedDateTime}" type="date" dateStyle="long" />
                        </li>
                    </ul>
                </div> <!-- vcard-details -->
                <div id="vcard-stats">
                    <div class="row-fluid">
                        <div class="span6">
                            <h3>${submissionStats.get("acceptedSubmission")}</h3>
                            <spring:message code="voj.accounts.user.accpeted" text="Accpeted" />
                        </div> <!-- .span6 -->
                        <div class="span6">
                            <h3>${submissionStats.get("totalSubmission")}</h3>
                            <spring:message code="voj.accounts.user.submit" text="Submit" />
                        </div> <!-- .span6 -->
                    </div> <!-- .row-fluid -->
                </div> <!-- #vcard-stats -->
            </div> <!-- .span4 -->
            <div id="main-content" class="span9">
            <c:if test="${not empty aboutMe}">
                <div class="section">
                    <div class="header">
                        <h4><spring:message code="voj.accounts.user.about-me" text="About Me" /></h4>
                    </div> <!-- .header -->
                    <div class="body markdown">${aboutMe}</div> <!-- .body -->
                </div> <!-- .section -->
            </c:if>
                <div class="section">
                    <div class="header">
                        <h4><spring:message code="voj.accounts.user.submissions" text="Submissions" /></h4>
                    </div> <!-- .header -->
                    <div class="body">
                        <div id="submissions-calendar"></div> <!-- #submissions-calendar -->
                        <table id="submissions" class="table table-striped">
                            <thead>
                                <tr>
                                    <th class="flag"><spring:message code="voj.problems.problems.result" text="Result" /></th>
                                    <th class="name"><spring:message code="voj.problems.problems.name" text="Name" /></th>
                                    <th class="submission"><spring:message code="voj.problems.problems.submission" text="Submission" /></th>
                                    <th class="ac-rate">AC%</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="submission" items="${submissions}">
                                <tr>
                                    <td class="flag-${submission.value.judgeResult.judgeResultSlug}">
                                        <a href="<c:url value="/submission/${submission.value.submissionId}" />">
                                            ${submission.value.judgeResult.judgeResultSlug}
                                        </a>
                                    </td>
                                    <td class="name"><a href="<c:url value="/p/${submission.key}" />">P${submission.key} ${submission.value.problem.problemName}</a></td>
                                    <td>${submission.value.problem.totalSubmission}</td>
                                    <td>
                                    <c:choose>
                                        <c:when test="${submission.value.problem.totalSubmission == 0}">0%</c:when>
                                        <c:otherwise>
                                            <fmt:formatNumber type="number"  maxFractionDigits="0" value="${submission.value.problem.acceptedSubmission * 100 / submission.value.problem.totalSubmission}" />%
                                        </c:otherwise>
                                    </c:choose>
                                    </td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div> <!-- .body -->
                </div> <!-- .section -->
            </div> <!-- .span8 -->
        </div> <!-- .row-fluid -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
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
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highcharts.min.js', function() {
            Highcharts.setOptions({
                colors: ['#34495e', '#e74c3c']
            });

            $('#submissions-calendar').highcharts({
            	chart: {
            		backgroundColor: null,
            	},
                title: {
                    text: 'Submissions in the Past 30 Days'
                },
                xAxis: {
                    categories: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
                },
                yAxis: {
                    title: {
                        text: 'Number of Submissions'
                    }
                },
                tooltip: {
                    shared: true,
                    crosshairs: true
                },
                series: [
                    {
                        name: 'Total Submissions',
                        lineWidth: 4,
                        marker: {
                            radius: 4
                        },
                        "data":[5.7879,6.6286,6.1724,5.3125,7.1481,6.1333,4.5769]
                    },
                    {
                        name: 'Accepted Submissions',
                        "data":[3.7879,7.86,6.1724,4.3125,2.1481,9.1333,3.5769]
                    }
                ]
            });
        });
    </script>
    <script type="text/javascript">
        $(function() {
            var hash = md5('${user.email}');

            $.ajax({
                type: 'GET',
                url: 'https://secure.gravatar.com/' + hash + '.json',
                dataType: 'jsonp',
                success: function(result){
                    if ( result != null ) {
                        var imageUrl    = result['entry'][0]['thumbnailUrl'],
                            requrestUrl = imageUrl + '?s=240';

                        $('img', '#vcard').attr('src', requrestUrl);
                    }
                }
            });
        });
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    <script type="text/javascript">${googleAnalyticsCode}</script>
    </c:if>
</body>
</html>