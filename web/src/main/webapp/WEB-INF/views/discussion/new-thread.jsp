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
    <title><spring:message code="voj.discussion.new-thread.create-discussion" text="Create Discussion" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/discussion/new-thread.css?v=${version}" />
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
            <div class="alert alert-error hide"></div> <!-- .alert-error -->
            <div class="header row-fluid">
                <div class="span9">
                    <p>
                        <input id="discussion-thread-title" placeholder="<spring:message code="voj.discussion.new-thread.thread-title" text="Title" />" type="text" maxlength="128" />
                    </p>
                </div> <!-- .span9 -->
                <div class="span3 text-right">
                    <input type="hidden" id="csrf-token" value="${csrfToken}" />
                    <button class="btn btn-primary" onclick="javascript:void(0);"><spring:message code="voj.discussion.new-thread.create-discussion" text="Create Discussion" /></button>
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
            <div class="body row-fluid">
                <div class="span9">
                    <div id="editor">
                        <div class="markdown-editor">
                            <div class="wmd-panel">
                                <div id="wmd-button-bar" class="wmd-button-bar"></div> <!-- #wmd-button-bar -->
                                <textarea id="wmd-input" class="wmd-input"></textarea>
                            </div> <!-- .wmd-panel -->
                            <div id="wmd-preview" class="wmd-panel wmd-preview"></div> <!-- .wmd-preview -->
                        </div> <!-- #markdown-editor -->
                    </div> <!-- .#editor -->
                </div> <!-- .span9 -->
                <div class="span3">
                    <div class="section">
                        <h5><spring:message code="voj.discussion.new-thread.discussion-topic" text="Discussion Topic" /></h5>
                        <select id="discussion-topic">
                        <c:forEach var="discussionTopic" items="${discussionTopics}">
                            <c:if test="${relatedProblem != null or discussionTopic.discussionTopicSlug != 'solutions'}">
                                <option value="${discussionTopic.discussionTopicSlug}">${discussionTopic.discussionTopicName}</option>
                            </c:if>
                        </c:forEach>
                        </select>
                    </div> <!-- .section -->
                    <c:if test="${relatedProblem != null}">
                    <div class="section">
                        <h5><spring:message code="voj.discussion.new-thread.related-problem" text="Related Problem" /></h5>
                        <p>
                            <a href="<c:url value="/p/${relatedProblem.problemId}" />">P<span id="related-problem">${relatedProblem.problemId} </span>${relatedProblem.problemName}</a>
                        </p>
                    </div> <!-- .section -->
                    </c:if>
                </div> <!-- .span3 -->
            </div> <!-- .row-fluid -->
        </div> <!-- #discussion-thread -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript">
        $.when(
            $.getScript('${cdnUrl}/js/markdown.min.js?v=${version}'),
            $.Deferred(function(deferred) {
                $(deferred.resolve);
            })
        ).done(function() {
            converter = Markdown.getSanitizingConverter();
            editor    = new Markdown.Editor(converter);
            editor.run();
        }); 
    </script>
    <script type="text/javascript">
        $('.btn-primary').click(function() {
            var relatedProblemId = $('#related-problem').html() || '',
                discussionTopic  = $('#discussion-topic').val(),
                threadTitle      = $('#discussion-thread-title').val(),
                threadContent    = $('#wmd-input').val(),
                csrfToken        = $('#csrf-token').val();
        
            $('.alert-error').addClass('hide');
            $('.btn-primary').attr('disabled', 'disabled');
            $('.btn-primary').html('<spring:message code="voj.discussion.new-thread.please-wait" text="Please wait..." />');
            return createDiscussionThread(relatedProblemId, discussionTopic, threadTitle, threadContent, csrfToken);
        });
    </script>
    <script type="text/javascript">
        function createDiscussionThread(relatedProblemId, discussionTopic, threadTitle, threadContent, csrfToken) {
            var postData         = {
                'relatedProblemId': relatedProblemId, 
                'discussionTopicSlug': discussionTopic, 
                'threadTitle': threadTitle, 
                'threadContent': threadContent, 
                'csrfToken': csrfToken
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/discussion/createDiscussionThread.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processResult(result) {
            if ( result['isSuccessful'] ) {
                var threadId = result['discussionThreadId'];
                window.location.href = '<c:url value="/discussion/" />' + threadId;
            } else {
                var errorMessage = '';

                if ( !result['isCsrfTokenValid'] ) {
                    errorMessage += '<spring:message code="voj.discussion.new-thread.csrf-token-invalid" text="Invalid token." /><br>';
                }
                if ( !result['isThreadCreatorExists'] || !result['isThreadCreatorLegal'] ) {
                    errorMessage += '<spring:message code="voj.discussion.new-thread.not-allow-to-create-discussion" text="You&acute;re not allow to create discussion." /><br>';
                }
                if ( !result['isDiscussionTopicExists'] ) {
                    errorMessage += '<spring:message code="voj.discussion.new-thread.discussion-topic-invalid" text="The discussion topic doesn&acute;t exist." /><br>';
                }
                if ( result['isThreadTitleEmpty'] ) {
                    errorMessage += '<spring:message code="voj.discussion.new-thread.discussion-thread-title-empty" text="You can&acute;t leave Title empty." /><br>';
                } else if ( !result['isThreadTitleLegal'] ) {
                    errorMessage += '<spring:message code="voj.discussion.new-thread.discussion-thread-title-too-long" text="The length of Title cannot exceed 128 characters." /><br>';
                }
                if ( result['isThreadContentEmpty'] ) {
                    errorMessage += '<spring:message code="voj.discussion.new-thread.discussion-thread-content-empty" text="You can&acute;t leave Content empty." /><br>';
                }
                $('.alert-error').html(errorMessage);
                $('.alert-error').removeClass('hide');
            }
            $('.btn-primary').html('<spring:message code="voj.discussion.new-thread.create-discussion" text="Create Discussion" />');
            $('.btn-primary').removeAttr('disabled');
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
        ${googleAnalyticsCode}
    </c:if>
</body>
</html>