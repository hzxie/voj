<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title>${discussionThread.discussionThreadTitle} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/discussion/thread.css" />
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
        <div id="discussion-thread">
            <div class="header row-fluid">
                <div class="span9">
                    <h3>${discussionThread.discussionThreadTitle}</h3>
                    <ul class="inline">
                        <li>
                            <i class="fa fa-clock-o"></i>
                            <span class="datetime"><fmt:formatDate value="${discussionThread.discussionThreadCreateTime}" type="both" dateStyle="default" timeStyle="default" /></span>
                        </li>
                        <li>
                            <i class="fa fa-user"></i>
                            <a href="<c:url value="/accounts/user/" />${discussionThread.discussionThreadCreator.uid}">${discussionThread.discussionThreadCreator.username}</a>
                        </li>
                        <li>
                            <i class="fa fa-comments"></i>
                            ${discussionThread.numberOfReplies <= 1 ?  0 : discussionThread.numberOfReplies - 1} <spring:message code="voj.discussion.thread.comments" text="Comment(s)" />
                        </li>
                    </ul>
                </div> <!-- .span9 -->
                <div class="span3 text-right">
                    <button class="btn btn-primary"><spring:message code="voj.discussion.thread.new-discussion" text="New Discussion" /></button>
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
            <div class="body row-fluid">
                <div class="span9">
                    <ul id="discussion-replies"></ul>
                    <div id="more-discussion-replies" class="text-center">
                        <p class="availble"><a href="javascript:void(0);"><spring:message code="voj.discussion.thread.more-replies" text="More Replies..." /></a></p>
                        <img src="${cdnUrl}/img/loading.gif" alt="Loading" class="hide" />
                    </div> <!-- #more-discussion-replies -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div class="section">
                        <h5><spring:message code="voj.discussion.thread.discussion-topic" text="Discussion Topic" /></h5>
                        <p>
                            <a href="<c:url value="/discussion?topicSlug=" />${discussionThread.discussionTopic.discussionTopicSlug}">${discussionThread.discussionTopic.discussionTopicName}</a>
                        </p>
                    </div> <!-- .section -->
                <c:if test="${discussionThread.problem != null}">
                    <div class="section">
                        <h5><spring:message code="voj.discussion.thread.related-problem" text="Related Problem" /></h5>
                        <p>
                            <a href="<c:url value="/p/" />${discussionThread.problem.problemId}">P${discussionThread.problem.problemId} ${discussionThread.problem.problemName}</a>
                        </p>
                    </div> <!-- .section -->
                </c:if>
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
        </div> <!-- .discussion-thread -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript" async src="https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
    <script type="text/javascript">
        $.when(
            $.getScript('${cdnUrl}/js/markdown.min.js'),
            $.getScript('${cdnUrl}/js/moment.min.js'),
            $.Deferred(function(deferred) {
                $(deferred.resolve);
            })
        ).done(function() {
            converter = Markdown.getSanitizingConverter();
            moment.locale('${language}');
            $('span.datetime').html(getTimeElapsed($('span.datetime').html()));

            setLoadingStatus(true);
            getDiscussionReplies(0);
        }); 
    </script>
    <script type="text/javascript">
        $('#more-discussion-replies').click(function() {
            var isLoading       = $('img', this).is(':visible'),
                hasNextRecord   = $('p', this).hasClass('availble'),
                numberOfReplies = $('.discussion-reply').length;

            if ( !isLoading && hasNextRecord ) {
                setLoadingStatus(true);
                return getDiscussionReplies(numberOfReplies);
            }
        });
    </script>
    <script type="text/javascript">
        function getTimeElapsed(dateTimeString) {
            var dateTime = moment(dateTimeString);
            return dateTime.fromNow();
        }
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
                $('p', '#more-discussion-replies').addClass('hide');
                $('img', '#more-discussion-replies').removeClass('hide');
            } else {
                $('img', '#more-discussion-replies').addClass('hide');
                $('p', '#more-discussion-replies').removeClass('hide');
            }
        }
    </script>
    <script type="text/javascript">
        function getDiscussionReplies(startIndex) {
            var pageRequests = {
                'startIndex': startIndex
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/discussion/${discussionThread.discussionThreadId}/getDiscussionReplies.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    return processDiscussionRepliesResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processDiscussionRepliesResult(result) {
            if ( result['isSuccessful'] ) {
                displayDiscussionReplyRecords(result['discussionReplies']);
            } else {
                $('p', '#more-discussion-replies').removeClass('availble');
                $('p', '#more-discussion-replies').html('<spring:message code="voj.discussion.replies.no-more-discussion" text="No more discussion replies" />');
            }
            setLoadingStatus(false);
        }
    </script>
    <script type="text/javascript">
        function displayDiscussionReplyRecords(discussionReplies) {
            for ( var i = 0; i < discussionReplies.length; ++ i ) {
                $('ul#discussion-replies').append(
                    getDiscussionReplyContent(discussionReplies[i]['discussionReplyCreator'], discussionReplies[i]['discussionReplyCreateTime'], 
                        discussionReplies[i]['discussionReplyContent'], discussionReplies[i]['discussionReplyVotes'])
                );
                getGravatarUrl(discussionReplies[i]['discussionReplyCreator']['email'], $('img', 'ul#discussion-replies li:last-child div.avatar'));
            }
        }
    </script>
    <script type="text/javascript">
        function getDiscussionReplyContent(discussionReplyCreator, discussionReplyCreateTime, discussionReplyContent, discussionReplyVotes) {
            discussionReplyVotes = JSON.parse(discussionReplyVotes);
            var replyHtml =
                '<li class="row-fluid">' + 
                '    <div class="span2">' + 
                '        <div class="avatar" data-value="%s">'.format(discussionReplyCreator['email']) + 
                '            <img src="${cdnUrl}/img/avatar.jpg" alt="avatar" />' + 
                '        </div> <!-- .avatar -->' + 
                '    </div> <!-- .span2 -->' + 
                '    <div class="span10">' + 
                '        <div class="discussion-reply">' + 
                '            <div class="reply-header %s">'.format('') + 
                '                <a href="<c:url value="/accounts/user/" />%s">%s</a> @ %s'.format(discussionReplyCreator['uid'], discussionReplyCreator['username'], getTimeElapsed(discussionReplyCreateTime)) + 
                '            </div> <!-- .reply-header -->' + 
                '            <div class="reply-body">' + 
                '                <div class="markdown">%s</div> <!-- .markdown -->'.format(converter.makeHtml(discussionReplyContent.replace(/\\\n/g, '\\n'))) +
                '            </div> <!-- .reply-body -->' + 
                '            <div class="reply-footer">' + 
                '                <ul class="inline">' + 
                '                    <li>' + 
                '                        <a href="javascript:void(0);" class="%s" title="<spring:message code="voj.discussion.thread.useful-reply" text="This reply is useful" />">'.format(discussionReplyVotes['isVotedUp'] ? 'active': '') + 
                '                            <i class="fa fa-thumbs-up"></i>' + 
                '                        </a>' + 
                '                        <span class="vote-ups">%s</span>'.format(discussionReplyVotes['numberOfVoteUp']) + 
                '                    </li>' + 
                '                    <li>' + 
                '                        <a href="javascript:void(0);" class="%s" title="<spring:message code="voj.discussion.thread.useless-reply" text="This reply is not useful" />">'.format(discussionReplyVotes['isVotedDown'] ? 'active': '') + 
                '                            <i class="fa fa-thumbs-down"></i>' + 
                '                        </a>' + 
                '                        <span class="vote-downs">%s</span>'.format(discussionReplyVotes['numberOfVoteDown']) + 
                '                    </li>' + 
                '                </ul>' + 
                '            </div> <!-- .reply-footer -->' + 
                '        </div> <!-- .discussion-reply -->' + 
                '    </div> <!-- .span10 -->' + 
                '</li>';
            return replyHtml;
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
        ${googleAnalyticsCode}
    </c:if>
</body>
</html>