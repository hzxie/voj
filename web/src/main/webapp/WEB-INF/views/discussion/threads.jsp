<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.discussion.threads.title" text="Discussion" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/discussion/threads.css?v=${version}" />
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
    <div id="content" class="container">
        <div id="main-content" class="row-fluid">
            <div id="discussion" class="span8">
                <table id="discussion-threads" class="table">
                <c:forEach var="discussionThread" items="${discussionThreads}">
                    <tr class="discussion-thread">
                        <td class="avatar" data-value="${fn:toLowerCase(discussionThread.discussionThreadCreator.email)}">
                            <img src="${cdnUrl}/img/avatar.jpg?v=${version}" alt="avatar" />
                        </td>
                        <td class="overview">
                            <h5><a href="<c:url value="/discussion/" />${discussionThread.discussionThreadId}">${discussionThread.discussionThreadTitle}</a></h5>
                            <ul class="inline">
                                <li>
                                    <spring:message code="voj.discussion.threads.author" text="Author" />: 
                                    <a href="<c:url value="/accounts/user/" />${discussionThread.discussionThreadCreator.uid}">${discussionThread.discussionThreadCreator.username}</a>
                                </li>
                                <li>
                                    <spring:message code="voj.discussion.threads.posted-in" text="Posted in" />: 
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
                                    <spring:message code="voj.discussion.threads.latest-reply" text="Latest reply" />:
                                    <c:choose>
                                    <c:when test="${discussionThread.latestDiscussionReply == null}">
                                        <a href="<c:url value="/accounts/user/" />${discussionThread.discussionThreadCreator.uid}">${discussionThread.discussionThreadCreator.username}</a> 
                                        @<span class="reply-datetime"><fmt:formatDate value="${discussionThread.discussionThreadCreateTime}" pattern="yyyy-MM-dd HH:mm:ss" /></span>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="<c:url value="/accounts/user/" />${discussionThread.latestDiscussionReply.discussionReplyCreator.uid}">${discussionThread.latestDiscussionReply.discussionReplyCreator.username}</a> 
                                        @<span class="reply-datetime"><fmt:formatDate value="${discussionThread.latestDiscussionReply.discussionReplyCreateTime}" pattern="yyyy-MM-dd HH:mm:ss" /></span>
                                    </c:otherwise>
                                    </c:choose>
                                </li>
                            </ul>
                        </td>
                        <td class="reply-count">
                            <c:choose>
                            <c:when test="${discussionThread.discussionTopic.discussionTopicSlug == 'solutions'}">${discussionThread.numberOfReplies}</c:when>
                            <c:otherwise>${discussionThread.numberOfReplies <= 1 ?  0 : discussionThread.numberOfReplies - 1}</c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </table> <!-- .table -->
                <div id="more-discussion-threads">
                    <p class="availble"><spring:message code="voj.discussion.threads.more-discussion" text="More discussion threads..." /></p>
                    <img src="${cdnUrl}/img/loading.gif?v=${version}" alt="Loading" class="hide" />
                </div>
            </div> <!-- #discussion -->
            <div id="sidebar" class="span4">
                <c:if test="${isLogin}">
                <div id="create-thread-widget" class="widget">
                    <div class="header"></div> <!-- .header -->
                    <div class="body">
                        <button class="btn btn-primary btn-block" onclick="window.location.href='<c:url value="/discussion/new" />'"><spring:message code="voj.discussion.threads.new-discussion" text="New Discussion" /></button>
                    </div> <!-- .body -->
                </div> <!-- .widget -->
                </c:if>
                <div id="topics-widget" class="widget">
                    <div class="header">
                        <h4><spring:message code="voj.discussion.threads.topics" text="Discussion Topics" /></h4>
                    </div> <!-- .header -->
                    <div class="body">
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
                    </div> <!-- .body -->
                </div> <!-- .widget -->
            </div> <!-- #sidebar -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/moment.min.js?v=${version}', function() {
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
                var email         = $(this).attr('data-value'),
                    imgContainer  = $('img', $(this));

                getGravatarUrl(email, imgContainer);
            });
        });
    </script>
    <script type="text/javascript">
        function getGravatarUrl(email, imgContainer) {
            var hash = md5(email.toLowerCase());
            $.ajax({
                type: 'GET',
                url: 'https://secure.gravatar.com/' + hash + '.json',
                dataType: 'jsonp',
                success: function(result){
                    if ( result != null ) {
                        var imageUrl    = result['entry'][0]['thumbnailUrl'],
                            requrestUrl = imageUrl + '?s=120';
                        $(imgContainer).attr('src', requrestUrl);
                    }
                }
            });
        }
    </script>
    <script type="text/javascript">
        function setLoadingStatus(isLoading) {
            if ( isLoading ) {
                $('p', '#more-discussion-threads').addClass('hide');
                $('img', '#more-discussion-threads').removeClass('hide');
            } else {
                $('img', '#more-discussion-threads').addClass('hide');
                $('p', '#more-discussion-threads').removeClass('hide');
            }
        }
    </script>
    <script type="text/javascript">
        $('#more-discussion-threads').click(function() {
            var isLoading         = $('img', this).is(':visible'),
                hasNextRecord     = $('p', this).hasClass('availble'),
                numberOfThreads   = $('.discussion-thread').length;

            if ( !isLoading && hasNextRecord ) {
                setLoadingStatus(true);
                return getMoreDiscussionThreads(numberOfThreads);
            }
        });
    </script>
    <script type="text/javascript">
        function getMoreDiscussionThreads(startIndex) {
            var pageRequests = {
                'startIndex': startIndex,
                'category': '${selectedTopicSlug}',
                'problemId': ${problemId},
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/discussion/getDiscussionThreads.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    return processDiscussionThreadsResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processDiscussionThreadsResult(result) {
            if ( result['isSuccessful'] ) {
                displayDiscussionThreadRecords(result['discussionThreads']);
            } else {
                $('p', '#more-discussion-threads').removeClass('availble');
                $('p', '#more-discussion-threads').html('<spring:message code="voj.discussion.threads.no-more-discussion" text="No more discussion threads" />');
                $('#more-discussion-threads').css('cursor', 'default');
            }
            setLoadingStatus(false);
        }
    </script>
    <script type="text/javascript">
        function displayDiscussionThreadRecords(discussionThreads) {
            for ( var i = 0; i < discussionThreads.length; ++ i ) {
                $('table#discussion-threads > tbody').append(
                    getDiscussionThreadContent(discussionThreads[i]['discussionThreadCreator'], discussionThreads[i]['discussionThreadId'], 
                        discussionThreads[i]['discussionThreadTitle'], discussionThreads[i]['discussionThreadCreateTime'], 
                        discussionThreads[i]['problem'], discussionThreads[i]['discussionTopic'], 
                        discussionThreads[i]['latestDiscussionReply'], discussionThreads[i]['numberOfReplies'])
                );
                getGravatarUrl(discussionThreads[i]['discussionThreadCreator']['email'], $('img', 'table#discussion-threads tr:last-child'));
            }
        }
    </script>
    <script type="text/javascript">
        function getDiscussionThreadContent(discussionThreadCreator, discussionThreadId, discussionThreadTitle, discussionThreadCreateTime, discussionThreadRelatedProblem, discussionTopic, latestDiscussionReply, numberOfReplies) {
            var latestDiscussionReplyCreatorUid      = latestDiscussionReply ? latestDiscussionReply['discussionReplyCreator']['uid'] : discussionThreadCreator['uid'],
                latestDiscussionReplyCreatorUsername = latestDiscussionReply ? latestDiscussionReply['discussionReplyCreator']['username'] : discussionThreadCreator['username'],
                latestDiscussionReplyCreateTime      = latestDiscussionReply ? latestDiscussionReply['discussionReplyCreateTime'] : discussionThreadCreateTime,
                discussionThreadPostedIn             = '';

            if ( discussionThreadRelatedProblem ) {
                discussionThreadPostedIn = '<a href="<c:url value="/p/" />%s">P%s: %s</a>'.format(discussionThreadRelatedProblem['problemId'], discussionThreadRelatedProblem['problemId'], discussionThreadRelatedProblem['problemName']);
            } else {
                discussionThreadPostedIn = '<a href="<c:url value="/discussion?topicSlug=" />%s">%s</a>'.format(discussionTopic['discussionTopicSlug'], discussionTopic['discussionTopicName']);
            }
            var threadHtml = 
                '<tr class="discussion-thread">' + 
                '    <td class="avatar" data-value="%s">'.format(discussionThreadCreator['email']) +
                '        <img src="${cdnUrl}/img/avatar.jpg?v=${version}" alt="avatar" />' + 
                '    </td>' + 
                '    <td class="overview">' + 
                '        <h5><a href="<c:url value="/discussion/" />%s">%s</a></h5>'.format(discussionThreadId, discussionThreadTitle) + 
                '        <ul class="inline">' + 
                '            <li>' + 
                '                <spring:message code="voj.discussion.threads.author" text="Author" />: ' + 
                '                <a href="<c:url value="/accounts/user/" />%s">%s</a>'.format(discussionThreadCreator['uid'], discussionThreadCreator['username']) + 
                '            </li>' + 
                '            <li>' + 
                '                <spring:message code="voj.discussion.threads.posted-in" text="Posted in" />: ' + discussionThreadPostedIn + 
                '            </li>' + 
                '            <li>' + 
                '                <spring:message code="voj.discussion.threads.latest-reply" text="Latest reply" />:' + 
                '                <a href="<c:url value="/accounts/user/" />%s">%s</a> '.format(latestDiscussionReplyCreatorUid, latestDiscussionReplyCreatorUsername) + 
                '                @<span class="reply-datetime">%s</span>'.format(getTimeElapsed(latestDiscussionReplyCreateTime)) + 
                '            </li>' + 
                '        </ul>' + 
                '    </td>' + 
                '    <td class="reply-count">%s</td>'.format(numberOfReplies < 1 ? 0 : numberOfReplies - 1) + 
                '</tr>';
            return threadHtml;
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>