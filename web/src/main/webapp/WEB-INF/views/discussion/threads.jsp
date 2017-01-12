<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.discussion.discussion.title" text="Discussion" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/discussion/discussion.css" />
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
        <div id="main-content" class="row-fluid">
            <div id="discussion" class="span8">
                <table class="table">
                <c:forEach var="discussionThread" items="${discussionThreads}">
                    <tr class="discussion-thread">
                        <td class="avatar" data-value="${fn:toLowerCase(discussionThread.discussionThreadCreator.email)}">
                            <img src="${cdnUrl}/img/avatar.jpg" alt="avatar" />
                        </td>
                        <td class="overview">
                            <h5><a href="<c:url value="/discussion/" />${discussionThread.discussionThreadId}">${discussionThread.discussionThreadTitle}</a></h5>
                            <ul class="inline">
                                <li>
                                    <spring:message code="voj.index.author" text="Author" />: 
                                    <a href="<c:url value="/accounts/user/" />${discussionThread.discussionThreadCreator.uid}">${discussionThread.discussionThreadCreator.username}</a>
                                </li>
                                <li>
                                    <spring:message code="voj.index.posted-in" text="Posted in" />: 
                                <c:choose>
                                <c:when test="${discussionThread.problem == null}">
                                    <a href="<c:url value="/discussion?topicSlug=" />${discussionThread.discussionTopic.discussionTopicSlug}">${discussionThread.discussionTopic.discussionTopicName}</a>
                                </c:when>
                                <c:otherwise>
                                    <a href="<c:url value="/p/" />${discussionThread.problem.problemId}">P${discussionThread.problem.problemId}: ${discussionThread.problem.problemName}</a>
                                </c:otherwise>
                                </c:choose>
                                </li>
                                <li>
                                    <spring:message code="voj.index.latest-reply" text="Latest reply" />:
                                    <a href="<c:url value="/accounts/user/" />${discussionThread.latestDiscussionReply.discussionReplyCreator.uid}">${discussionThread.latestDiscussionReply.discussionReplyCreator.username}</a> 
                                    @<span class="reply-datetime"><fmt:formatDate value="${discussionThread.latestDiscussionReply.discussionReplyCreateTime}" type="both" dateStyle="default" timeStyle="default" /></span>
                                </li>
                            </ul>
                        </td>
                        <td class="reply-count">${discussionThread.numberOfReplies <= 1 ?  0 : discussionThread.numberOfReplies - 1}</td>
                    </tr>
                </c:forEach>
                </table> <!-- .table -->
                <div id="more-discussion">
                    <p class="availble"><spring:message code="voj.discussion.discussion.more-discussion" text="More Discussion..." /></p>
                    <img src="${cdnUrl}/img/loading.gif" alt="Loading" class="hide" />
                </div>
            </div> <!-- #discussion -->
            <div id="sidebar" class="span4">
                <div id="topics-widget" class="widget">
                    <h4><spring:message code="voj.discussion.discussion.topics" text="Discussion Topics" /></h4>
                    <c:forEach var="entry" items="${discussionTopics}">
                        <h6>
                            <a 
                                <c:if test="${selectedTopicSlug == entry.key.discussionTopicSlug}">class="active" </c:if>
                                href="<c:url value="/discussion?topicSlug=${entry.key.discussionTopicSlug}" />">
                                ${entry.key.discussionTopicName}
                            </a>
                        </h6>
                        <ul class="inline">
                        <c:forEach var="discussionTopic" items="${entry.value}">
                            <li>
                                <a 
                                    <c:if test="${selectedTopicSlug == discussionTopic.discussionTopicSlug}">class="active" </c:if>
                                    href="<c:url value="/discussion?topicSlug=${discussionTopic.discussionTopicSlug}" />">
                                    ${discussionTopic.discussionTopicName}
                                </a>
                            </li>
                        </c:forEach>
                        </ul>
                    </c:forEach>

                </div> <!-- .widgets -->
            </div> <!-- #sidebar -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/moment.min.js', function() {
            moment.locale('${language}');
            $('span.reply-datetime').each(function() {
                var dateTime = $(this).html();
                $(this).html(getTimeElapsed(dateTime));
            });
        });
    </script>
    <script type="text/javascript">
        function getTimeElapsed(dateTimeString) {
            var dateTime = moment(dateTimeString);
            return dateTime.fromNow();
        }
    </script>
    <script type="text/javascript">
        $(function() {
            $('.avatar', '.discussion-thread').each(function() {
                var hash    = md5($(this).attr('data-value')),
                    avatar  = $('img', $(this));

                $.ajax({
                    type: 'GET',
                    url: 'https://secure.gravatar.com/' + hash + '.json',
                    dataType: 'jsonp',
                    success: function(result){
                        if ( result != null ) {
                            var imageUrl    = result['entry'][0]['thumbnailUrl'],
                                requrestUrl = imageUrl + '?s=120';
                            $(avatar).attr('src', requrestUrl);
                        }
                    }
                });
            });
        });
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>