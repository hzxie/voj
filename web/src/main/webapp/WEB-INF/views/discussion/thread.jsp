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
                            <span id="number-of-comments">${discussionThread.numberOfReplies <= 1 ?  0 : discussionThread.numberOfReplies - 1}</span>
                            <spring:message code="voj.discussion.thread.comments" text="Comment(s)" />
                        </li>
                    </ul>
                </div> <!-- .span9 -->
                <c:if test="${isLogin}">
                <div class="span3 text-right">
                    <button class="btn btn-primary"><spring:message code="voj.discussion.thread.new-discussion" text="New Discussion" /></button>
                </div> <!-- .span3 -->
                </c:if>
            </div> <!-- .row-fluid -->
            <div class="body row-fluid">
                <div class="span9">
                    <ul id="discussion-replies"></ul>
                    <div id="more-discussion-replies" class="text-center">
                        <p class="availble"><a href="javascript:void(0);"><spring:message code="voj.discussion.thread.more-replies" text="More Replies..." /></a></p>
                        <img src="${cdnUrl}/img/loading.gif" alt="Loading" class="hide" />
                    </div> <!-- #more-discussion-replies -->
                    <c:if test="${isLogin}">
                    <div id="editor" class="row-fluid">
                        <div class="span2">
                            <div class="avatar">
                                <img src="${cdnUrl}/img/avatar.jpg" alt="gravatar">
                            </div> <!-- .avatar -->
                        </div> <!-- .span2 -->
                        <div class="span10">
                            <div class="discussion-reply current-user">
                                <div class="reply-header">
                                    <label for="wmd-input">
                                        <spring:message code="voj.discussion.thread.leave-a-comment" text="Leave a comment" />
                                        <input type="hidden" id="csrf-token" value="${csrfToken}" />
                                        <button id="create-discussion-reply" class="btn btn-primary"><spring:message code="voj.discussion.thread.comment" text="Comment" /></button>
                                    </label>
                                </div> <!-- .reply-header -->
                                <div class="reply-body">
                                    <div class="alert alert-error hide"></div> <!-- .alert-error -->
                                    <div id="markdown-editor">
                                        <div class="wmd-panel">
                                            <div id="wmd-button-bar"></div> <!-- #wmd-button-bar -->
                                            <textarea id="wmd-input" class="wmd-input"></textarea>
                                        </div> <!-- .wmd-panel -->
                                        <div id="wmd-preview" class="wmd-panel wmd-preview"></div> <!-- .wmd-preview -->
                                    </div> <!-- #markdown-editor -->
                                </div> <!-- .reply-body -->
                            </div> <!-- .discussion-reply -->
                        </div> <!-- .span10 -->
                    </div> <!-- .#editor -->
                    </c:if>
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
        <c:if test="${isLogin}">
            editor    = new Markdown.Editor(converter);
            editor.run();
            getGravatarUrl('${myProfile.email}', $('img', 'div#editor div.avatar'));
        </c:if>
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
                    getDiscussionReplyContent(discussionReplies[i]['discussionReplyId'], discussionReplies[i]['discussionReplyCreator'],
                        discussionReplies[i]['discussionReplyCreateTime'], discussionReplies[i]['discussionReplyContent'],
                        discussionReplies[i]['discussionReplyVotes'])
                );
                getGravatarUrl(discussionReplies[i]['discussionReplyCreator']['email'], $('img', 'ul#discussion-replies li:last-child div.avatar'));
            }
        }
    </script>
    <script type="text/javascript">
        function getDiscussionReplyContent(discussionReplyId, discussionReplyCreator, discussionReplyCreateTime, discussionReplyContent, discussionReplyVotes) {
            discussionReplyVotes = JSON.parse(discussionReplyVotes);
            var replyHtml =
                '<li class="row-fluid">' + 
                '    <div class="span2">' + 
                '        <div class="avatar" data-value="%s">'.format(discussionReplyCreator['email']) + 
                '            <img src="${cdnUrl}/img/avatar.jpg" alt="gravatar" />' +
                '        </div> <!-- .avatar -->' + 
                '    </div> <!-- .span2 -->' + 
                '    <div class="span10">' + 
                '        <div class="discussion-reply %s" data-value="%s">'.format(discussionReplyCreator['uid'] == '${myProfile.uid}' ? 'current-user' : '', discussionReplyId) +
                '            <div class="reply-header">' +
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
                '                        <span class="vote-ups">%s</span>'.format(discussionReplyVotes['numberOfVoteUp'] || 0) + 
                '                    </li>' + 
                '                    <li>' + 
                '                        <a href="javascript:void(0);" class="%s" title="<spring:message code="voj.discussion.thread.useless-reply" text="This reply is not useful" />">'.format(discussionReplyVotes['isVotedDown'] ? 'active': '') + 
                '                            <i class="fa fa-thumbs-down"></i>' + 
                '                        </a>' + 
                '                        <span class="vote-downs">%s</span>'.format(discussionReplyVotes['numberOfVoteDown'] || 0) + 
                '                    </li>' + 
                '                </ul>' + 
                '            </div> <!-- .reply-footer -->' + 
                '        </div> <!-- .discussion-reply -->' + 
                '    </div> <!-- .span10 -->' + 
                '</li>';
            return replyHtml;
        }
    </script>
    <c:if test="${isLogin}">
    <script type="text/javascript">
        $('#discussion-replies').on('click', 'i.fa-thumbs-up', function() {
            var discussionReplyId = $(this).parent().parent().parent().parent().parent().attr('data-value'),
                csrfToken         = $('#csrf-token').val(),
                isVotedUp         = $(this).parent().hasClass('active'),
                isVotedDown       = $('i.fa-thumbs-down', $(this).parent().parent().parent()).parent().hasClass('active'),
                voteUp            = isVotedUp ? -1 : 1,
                voteDown          = isVotedDown ? -1 : 0;
            
            return voteDiscussionReply(discussionReplyId, voteUp, voteDown, csrfToken);
        });

        $('#discussion-replies').on('click', 'i.fa-thumbs-down', function() {
            var discussionReplyId = $(this).parent().parent().parent().parent().parent().attr('data-value'),
                csrfToken         = $('#csrf-token').val(),
                isVotedUp         = $('i.fa-thumbs-up', $(this).parent().parent().parent()).parent().hasClass('active'),
                isVotedDown       = $(this).parent().hasClass('active'),
                voteUp            = isVotedUp ? -1 : 0,
                voteDown          = isVotedDown ? -1 : 1;

            return voteDiscussionReply(discussionReplyId, voteUp, voteDown, csrfToken);
        });
    </script>
    <script type="text/javascript">
        function voteDiscussionReply(discussionReplyId, voteUp, voteDown, csrfToken) {
            var postData = {
                'discussionReplyId': discussionReplyId,
                'voteUp': voteUp,
                'voteDown': voteDown,
                'csrfToken': csrfToken
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/discussion/${discussionThread.discussionThreadId}/voteDiscussionReply.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        var thumbUpButton   = $('.fa-thumbs-up', 'div[data-value=%s]'.format(discussionReplyId)).parent(),
                            thumbDownButton = $('.fa-thumbs-down', 'div[data-value=%s]'.format(discussionReplyId)).parent(),
                            voteUpDisplay   = $('.vote-ups', 'div[data-value=%s]'.format(discussionReplyId)),
                            voteDownDisplay = $('.vote-downs', 'div[data-value=%s]'.format(discussionReplyId)),
                            voteUps         = parseInt($('.vote-ups', 'div[data-value=%s]'.format(discussionReplyId)).html()),
                            voteDowns       = parseInt($('.vote-downs', 'div[data-value=%s]'.format(discussionReplyId)).html());

                        // Setup vote up thumbs
                        if ( voteUp == 1 ) {
                            $(thumbUpButton).addClass('active');
                        } else if ( voteUp == -1 ) {
                            $(thumbUpButton).removeClass('active');
                        }
                        // Setup vote down thumbs
                        if ( voteDown == 1 ) {
                            $(thumbDownButton).addClass('active');
                        } else if ( voteDown == -1 ) {
                            $(thumbDownButton).removeClass('active');
                        }
                        $(voteUpDisplay).html(voteUps + voteUp);
                        $(voteDownDisplay).html(voteDowns + voteDown);
                    } else {
                        alert('<spring:message code="voj.discussion.thread.failed-to-vote" text="Failed to vote this reply, please try again." />');
                    }
                }
            });
        }
    </script>
    <script type="text/javascript">
        $('.btn-primary', '#editor').click(function() {
            var postData = {
                'replyContent': $('#wmd-input').val(),
                'csrfToken': $('#csrf-token').val()
            };

            $('.btn-primary', '#editor').attr('disabled', 'disabled');
            $('.btn-primary', '#editor').html('<spring:message code="voj.discussion.thread.please-wait" text="Please wait..." />');
            $.ajax({
                type: 'POST',
                url: '<c:url value="/discussion/${discussionThread.discussionThreadId}/createDiscussionReply.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    processDiscussionReplyCreationResult(result);
                }
            });
        });
    </script>
    <script type="text/javascript">
        function processDiscussionReplyCreationResult(result) {
            if ( result['isSuccessful'] ) {
                var discussionReply   = result['discussionReply'],
                    discussionReplies = [discussionReply],
                    numberOfComments  = parseInt($('#number-of-comments').html());
                
                $('#wmd-input').val('');
                $('#wmd-preview').html('');
                $('#number-of-comments').html(numberOfComments + 1);
                displayDiscussionReplyRecords(discussionReplies);
            } else {
                var errorMessage  = '';
                
                if ( result['isCsrfTokenValid'] ) {
                    errorMessage += '<spring:message code="voj.discussion.thread.csrf-token-invalid" text="Invalid token." /><br>';
                }
                if ( !result['isDiscussionThreadExists'] ) {
                    errorMessage += '<spring:message code="voj.discussion.thread.thread-not-exists" text="The discussion thread doesn&acute;t exist." /><br>';
                }
                if ( !result['isReplyCreatorExists'] ) {
                    errorMessage += '<spring:message code="voj.discussion.thread.user-not-login" text="Please sign in first." /><br>';
                } else if ( !result['isReplyCreatorLegal'] ) {
                    errorMessage += '<spring:message code="voj.discussion.thread.user-not-permitted" text="You&acute;tre not allowed to post a reply." /><br>';
                }
                if ( result['isReplyContentEmpty'] ) {
                    errorMessage += '<spring:message code="voj.discussion.thread.empty-reply" text="Please enter reply content." /><br>';
                }
                $('.alert-error', '#editor').html(errorMessage);
                $('.alert-error', '#editor').removeClass('hide');
            }
            $('.btn-primary', '#editor').removeAttr('disabled');
            $('.btn-primary', '#editor').html('<spring:message code="voj.discussion.thread.comment" text="Comment" />');
        }
    </script>
    </c:if>
    <c:if test="${GoogleAnalyticsCode != ''}">
        ${googleAnalyticsCode}
    </c:if>
</body>
</html>