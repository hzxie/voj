<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title>${discussionThread.discussionThreadTitle} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/discussion/thread.css?v=${version}" />
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
        <div id="discussion-thread">
            <div class="header row-fluid">
                <div class="span9">
                    <h3>${discussionThread.discussionThreadTitle}</h3>
                    <p class="hide"><input id="discussion-thread-title" type="text" /></p>
                    <ul class="inline">
                        <li>
                            <i class="fa fa-clock-o"></i>
                            <span class="datetime"><fmt:formatDate value="${discussionThread.discussionThreadCreateTime}" pattern="yyyy-MM-dd HH:mm:ss" /></span>
                        </li>
                        <li>
                            <i class="fa fa-user"></i>
                            <a href="<c:url value="/accounts/user/" />${discussionThread.discussionThreadCreator.uid}">${discussionThread.discussionThreadCreator.username}</a>
                        </li>
                        <li>
                            <i class="fa fa-comments"></i>
                            <span id="number-of-comments">
                                <c:choose>
                                <c:when test="${discussionThread.discussionTopic.discussionTopicSlug == 'solutions'}">${discussionThread.numberOfReplies}</c:when>
                                <c:otherwise>${discussionThread.numberOfReplies <= 1 ?  0 : discussionThread.numberOfReplies - 1}</c:otherwise>
                                </c:choose>
                            </span>
                            <spring:message code="voj.discussion.thread.comments" text="Comment(s)" />
                        </li>
                    </ul>
                </div> <!-- .span9 -->
                <div class="span3 text-right">
                <c:if test="${myProfile.uid == discussionThread.discussionThreadCreator.uid}">
                    <!-- TODO -->
                    <button class="btn btn-default hide" onclick="javascript:editDiscussion();"><spring:message code="voj.discussion.thread.edit-discussion" text="Edit" /></button>
                </c:if>
                <c:if test="${isLogin}">
                    <button class="btn btn-primary" onclick="window.location.href='<c:url value="/discussion/new" />'"><spring:message code="voj.discussion.thread.new-discussion" text="New Discussion" /></button>
                </c:if>
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
            <div class="body row-fluid">
                <div class="span9">
                    <ul id="discussion-replies"></ul>
                    <div id="more-discussion-replies" class="text-center">
                        <p class="availble"><a href="javascript:void(0);"><spring:message code="voj.discussion.thread.more-replies" text="More Replies..." /></a></p>
                        <img src="${cdnUrl}/img/loading.gif?v=${version}" alt="Loading" class="hide" />
                    </div> <!-- #more-discussion-replies -->
                    <c:if test="${isLogin}">
                    <div id="editor" class="row-fluid">
                        <div class="span2">
                            <div class="avatar">
                                <img src="${cdnUrl}/img/avatar.jpg?v=${version}" alt="gravatar">
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
                                    <div class="markdown-editor">
                                        <div class="wmd-panel">
                                            <div id="wmd-button-bar" class="wmd-button-bar"></div> <!-- #wmd-button-bar -->
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
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/x-mathjax-config">
        MathJax.Hub.Config({
            tex2jax: {inlineMath: [['$','$'], ['\\(','\\)']]}
        });
    </script>
    <script type="text/javascript" async src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.2/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
    <script type="text/javascript">
        $.when(
            $.getScript('${cdnUrl}/js/markdown.min.js?v=${version}'),
            $.getScript('${cdnUrl}/js/moment.min.js?v=${version}'),
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
            <c:if test="${isLogin}">
                initializeMarkdownEditor(discussionReplies[i]['discussionReplyId']);
            </c:if>
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
                '            <img src="${cdnUrl}/img/avatar.jpg?v=${version}" alt="gravatar" />' +
                '        </div> <!-- .avatar -->' + 
                '    </div> <!-- .span2 -->' + 
                '    <div class="span10">' + 
                '        <div class="discussion-reply %s" data-value="%s">'.format(discussionReplyCreator['uid'] == '${myProfile.uid}' ? 'current-user' : '', discussionReplyId) +
                '            <div class="reply-header">' +
                '                <a href="<c:url value="/accounts/user/" />%s">%s</a> @ %s'.format(discussionReplyCreator['uid'], discussionReplyCreator['username'], getTimeElapsed(discussionReplyCreateTime)) + 
                '                <ul class="inline pull-right %s">'.format(discussionReplyCreator['uid'] == '${myProfile.uid}' || '${myProfile.userGroup.userGroupSlug}' == 'administrators' ? '' : 'hide') +
                '                    <li><a href="javascript:void(0);"><i class="fa fa-pencil"></i></a></li>' +
                '                    <li><a href="javascript:void(0);"><i class="fa fa-times"></i></a></li>' +
                '                </ul> ' +
                '            </div> <!-- .reply-header -->' + 
                '            <div class="reply-body">' + 
                '                <div class="markdown">%s</div> <!-- .markdown -->'.format(converter.makeHtml(discussionReplyContent.replace(/\\\n/g, '\\n'))) +
                <c:if test="${isLogin}">
                '                <div class="markdown-editor hide">' + 
                '                    <div class="wmd-panel">' + 
                '                        <div id="wmd-button-bar-%s" class="wmd-button-bar"></div>'.format(discussionReplyId) + 
                '                        <textarea id="wmd-input-%s" class="wmd-input">%s</textarea>'.format(discussionReplyId, discussionReplyContent) + 
                '                    </div> <!-- .wmd-panel -->' + 
                '                    <div id="wmd-preview-%s" class="wmd-panel wmd-preview"></div>'.format(discussionReplyId) + 
                '                    <ul class="inline text-right">' + 
                '                        <li><button class="btn btn-primary"><spring:message code="voj.discussion.thread.update-reply" text="Update reply" /></button></li>' + 
                '                        <li><button class="btn btn-default"><spring:message code="voj.discussion.thread.cancel" text="Cancel" /></button></li>' + 
                '                    </ul>' + 
                '                </div> <!-- .markdown-editor -->' + 
                </c:if>
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
        function initializeMarkdownEditor(discussionReplyId) {
            editor    = new Markdown.Editor(converter, '-%s'.format(discussionReplyId));
            editor.run();
        }
    </script>
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
                success: function(result) {
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
        $('#discussion-replies').on('click', 'i.fa-pencil', function() {
            var discussionReplyId = $(this).parent().parent().parent().parent().parent().attr('data-value'),
                replyObject       = $(this).parent().parent().parent().parent().parent().parent().parent(),
                markdownEditor    = $('.markdown-editor', replyObject);

            if ( $(markdownEditor).is(':visible') ) {
                $(markdownEditor).addClass('hide');
                $('.markdown', replyObject).removeClass('hide');
            } else {
                $('.markdown', replyObject).addClass('hide');
                $(markdownEditor).removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#discussion-replies').on('click', '.markdown-editor .btn-primary', function() {
            var discussionReplyId = $(this).parent().parent().parent().parent().parent().attr('data-value'),
                replyObject       = $(this).parent().parent().parent().parent().parent().parent().parent(),
                markdownEditor    = $('.markdown-editor', replyObject),
                replyContent      = $('.wmd-input', replyObject).val(),
                postData          = {
                    'discussionReplyId': discussionReplyId,
                    'replyContent': replyContent,
                    'csrfToken': $('#csrf-token').val()
                };

            $('.btn-primary', replyObject).attr('disabled', 'disabled');
            $('.btn-primary', replyObject).html('<spring:message code="voj.discussion.thread.please-wait" text="Please wait..." />');
            $.ajax({
                type: 'POST',
                url: '<c:url value="/discussion/${discussionThread.discussionThreadId}/editDiscussionReply.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result) {
                    if ( result['isSuccessful'] ) {
                        $(markdownEditor).addClass('hide');
                        $('.markdown', replyObject).html(converter.makeHtml(replyContent.replace(/\\\n/g, '\\n')));
                        $('.markdown', replyObject).removeClass('hide');
                    } else {
                        alert('<spring:message code="voj.discussion.thread.failed-to-update" text="Failed to update this reply, please try again." />');
                    }
                    $('.btn-primary', replyObject).removeAttr('disabled');
                    $('.btn-primary', replyObject).html('<spring:message code="voj.discussion.thread.update-reply" text="Update Reply" />');
                }
            });
        });
    </script>
    <script type="text/javascript">
        $('#discussion-replies').on('click', '.markdown-editor .btn-default', function() {
            var discussionReplyId = $(this).parent().parent().parent().parent().parent().attr('data-value'),
                replyObject       = $(this).parent().parent().parent().parent().parent().parent().parent(),
                markdownEditor    = $('.markdown-editor', replyObject);

            if ( $(markdownEditor).is(':visible') ) {
                $(markdownEditor).addClass('hide');
                $('.markdown', replyObject).removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#discussion-replies').on('click', 'i.fa-times', function() {
            if ( !confirm('<spring:message code="voj.discussion.thread.continue-or-not" text="Are you sure to continue?" />') ) {
                return;
            }

            var discussionReplyId = $(this).parent().parent().parent().parent().parent().attr('data-value'),
                replyObject       = $(this).parent().parent().parent().parent().parent().parent().parent(),
                numberOfComments  = parseInt($('#number-of-comments').html());
                postData          = {
                    'discussionReplyId': discussionReplyId,
                    'csrfToken': $('#csrf-token').val()
                };
            $.ajax({
                type: 'POST',
                url: '<c:url value="/discussion/${discussionThread.discussionThreadId}/deleteDiscussionReply.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result) {
                    if ( result['isSuccessful'] ) {
                        $(replyObject).remove();
                        $('#number-of-comments').html(numberOfComments - 1);
                    } else {
                        alert('<spring:message code="voj.discussion.thread.failed-to-delete" text="Failed to delete this reply, please try again." />');
                    }
                }
            });
        });
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
                success: function(result) {
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
    <c:if test="${myProfile.uid == discussionThread.discussionThreadCreator.uid}">
    <script type="text/javascript">
        function editDiscussion() {
            if ( !$('#discussion-thread-title').is(':visible') ) {
                setEditControlsVisibility(true);
            } else {
            }
        }
    </script>
    <script type="text/javascript">
        function setEditControlsVisibility(isVisible) {
            if ( isVisible ) {

            } else {
                var discussionThreadTitle = $('#discussion-thread > .header h3').html();
                $('#discussion-thread-title').val(discussionThreadTitle);
                $('#discussion-thread > .header h3').addClass('hide');
                $('#discussion-thread-title').removeClass('hide');
            }
        }
    </script>
    <script type="text/javascript">
        function doEditDiscussionThreadAction() {
            var discussionThreadTitle     = '',
                discussionThreadTopicSlug = '';

            $.ajax({

            });
        }
    </script>
    </c:if>
    <c:if test="${GoogleAnalyticsCode != ''}">
        ${googleAnalyticsCode}
    </c:if>
</body>
</html>