<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.new-user.title" text="New User" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css?v=${version}" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/new-user.css?v=${version}" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/flat-ui.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js?v=${version}"></script>
    <script type="text/javascript" src="${cdnUrl}/js/pace.min.js?v=${version}"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js?v=${version}"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <script type="text/javascript"> 
            window.location.href='<c:url value="/not-supported" />';
        </script>
    <![endif]-->
</head>
<body>
    <div id="wrapper">
        <!-- Sidebar -->
        <%@ include file="/WEB-INF/views/administration/include/sidebar.jsp" %>
        <div id="container">
            <!-- Header -->
            <%@ include file="/WEB-INF/views/administration/include/header.jsp" %>
            <!-- Content -->
            <div id="content">
                <h2 class="page-header"><i class="fa fa-user"></i> <spring:message code="voj.administration.new-user.new-user" text="New User" /></h2>
                <form id="profile-form" onSubmit="onSubmit(); return false;">
                    <div class="alert alert-error hide"></div> <!-- .alert-error -->
                    <div class="alert alert-success hide"><spring:message code="voj.administration.new-user.user-created" text="The user has been created successfully." /></div> <!-- .alert-success -->
                    <div class="control-group row-fluid">
                        <label for="username"><spring:message code="voj.administration.new-user.username" text="Username" /></label>
                        <input id="username" class="span12" type="text" maxlength="16" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="password"><spring:message code="voj.administration.new-user.password" text="Password" /></label>
                        <input id="password" class="span12" type="password" maxlength="16" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="email"><spring:message code="voj.administration.new-user.email" text="Email" /></label>
                        <input id="email" class="span12" type="text" maxlength="64" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="user-group"><spring:message code="voj.administration.new-user.user-group" text="User Group" /></label>
                        <select id="user-group" name="userGroup">
                        <c:forEach var="userGroup" items="${userGroups}">
                            <option value="${userGroup.userGroupSlug}">${userGroup.userGroupName}</option>
                        </c:forEach>
                        </select>
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="prefer-language"><spring:message code="voj.administration.new-user.prefer-language" text="Prefer Language" /></label>
                        <select id="prefer-language" name="preferLanguage">
                        <c:forEach var="language" items="${languages}">
                            <option value="${language.languageSlug}">${language.languageName}</option>
                        </c:forEach>
                        </select>
                    </div> <!-- .control-group -->
                    <div class="row-fluid">
                        <div class="span12">
                            <button class="btn btn-primary" type="submit"><spring:message code="voj.administration.new-user.create-account" text="Create Account" /></button>
                        </div> <!-- .span12 -->
                    </div> <!-- .row-fluid -->
                </form> <!-- #profile-form -->
            </div> <!-- #content -->    
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <script type="text/javascript">
        function onSubmit() {
            var username        = $('#username').val(),
                password        = $('#password').val(),
                email           = $('#email').val(),
                userGroup       = $('#user-group').val(),
                preferLanguage  = $('#prefer-language').val();
            
            $('.alert-success', '#profile-form').addClass('hide');
            $('.alert-error', '#profile-form').addClass('hide');
            $('button[type=submit]', '#profile-form').attr('disabled', 'disabled');
            $('button[type=submit]', '#profile-form').html('<spring:message code="voj.administration.new-user.please-wait" text="Please wait..." />');

            return doCreateUserAction(username, password, email, userGroup, preferLanguage);
        }
    </script>
    <script type="text/javascript">
        function doCreateUserAction(username, password, email, userGroup, preferLanguage) {
            var postData = {
                'username': username,
                'password': password,
                'email': email,
                'userGroup': userGroup,
                'preferLanguage': preferLanguage
            };
            
            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/newUser.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processCreateUserResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processCreateUserResult(result) {
            if ( result['isSuccessful'] ) {
                $('input').val('');
                $('.alert-success', '#profile-form').removeClass('hide');
            } else {
                var errorMessage  = '';

                if ( result['isUsernameEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.username-empty" text="You can&apos;t leave Username empty." /><br>';
                } else if ( !result['isUsernameLegal'] ) {
                    var username = $('#username').val();

                    if ( username.length < 6 || username.length > 16 ) {
                        errorMessage += '<spring:message code="voj.administration.new-user.username-length-illegal" text="The length of Username must between 6 and 16 characters." /><br>';
                    } else if ( !username[0].match(/[a-z]/i) ) {
                        errorMessage += '<spring:message code="voj.administration.new-user.username-beginning-illegal" text="Username must start with a letter(a-z)." /><br>';
                    } else {
                        errorMessage += '<spring:message code="voj.administration.new-user.username-character-illegal" text="Username can only contain letters(a-z), numbers, and underlines(_)." /><br>';
                    }
                } else if ( result['isUsernameExists'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.username-existing" text="Someone already has that username." /><br>';
                }
                if ( result['isPasswordEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.password-empty" text="You can&apos;t leave Password empty." /><br>';
                } else if ( !result['isPasswordLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.password-illegal" text="The length of Password must between 6 and 16 characters." /><br>';
                }
                if ( result['isEmailEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.email-empty" text="You can&apos;t leave Email empty." /><br>';
                } else if ( !result['isEmailLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.email-illegal" text="The Email seems invalid." /><br>';
                } else if ( result['isEmailExists'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.email-existing" text="Someone already use that email." /><br>';
                }
                if ( !result['isUserGroupLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.user-group-empty" text="You can&apos;t leave User Group empty." /><br>';
                }
                if ( !result['isLanguageLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-user.language-empty" text="You can&apos;t leave Language Preference empty." /><br>';
                }
                $('.alert-error', '#profile-form').html(errorMessage);
                $('.alert-error', '#profile-form').removeClass('hide');
            }
            $('button[type=submit]', '#profile-form').removeAttr('disabled');
            $('button[type=submit]', '#profile-form').html('<spring:message code="voj.administration.new-user.create-account" text="Create Account" />');
        }
    </script>
</body>
</html>