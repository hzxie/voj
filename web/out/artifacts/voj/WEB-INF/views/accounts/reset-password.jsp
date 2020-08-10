<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.reset-password.title" text="Reset Password" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/reset-password.css?v=${version}" />
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
    <div id="content">
        <div id="reset-password">
            <h2><spring:message code="voj.accounts.reset-password.reset-password" text="Reset Password" /></h2>
            <div class="alert alert-error hide"></div> <!-- .alert-error -->
            <div class="alert alert-success hide"></div> <!-- .alert-success -->
        <c:choose>
        <c:when test="${isTokenValid}">
            <form id="reset-password-form" method="POST" onSubmit="onSubmit(); return false;">
                <p class="row-fluid">
                    <label for="email"><spring:message code="voj.accounts.reset-password.email" text="email" /></label>
                    <input id="email" name="email" class="span12" type="text" value="${email}" maxlength="64" disabled="disabled" />
                </p>
                <p class="row-fluid">
                    <label for="new-password"><spring:message code="voj.accounts.reset-password.new-password" text="New Password" /></label>
                    <input id="new-password" name="new-password" class="span12" type="password" maxlength="16" />
                </p>
                <p class="row-fluid">
                    <label for="confirm-new-password"><spring:message code="voj.accounts.reset-password.confirm-new-password" text="Confirm New Password" /></label>
                    <input id="confirm-new-password" name="confirm-new-password" class="span12" type="password" maxlength="16" />
                </p>
                <p>
                    <input id="csrf-token" type="hidden" value="${csrfToken}" />
                    <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.accounts.reset-password.reset-password" text="Reset Password" /></button>
                </p>
            </form> <!-- #reset-password-form -->
        </c:when>
        <c:otherwise>
            <form id="reset-password-form" method="POST" onSubmit="onSubmit(); return false;">
                <p class="row-fluid">
                    <label for="username"><spring:message code="voj.accounts.reset-password.username" text="Username" /></label>
                    <input id="username" name="username" class="span12" type="text" maxlength="16" />
                </p>
                <p class="row-fluid">
                    <label for="email"><spring:message code="voj.accounts.reset-password.email" text="Email" /></label>
                    <input id="email" name="email" class="span12" type="text" maxlength="64" />
                </p>
                <p>
                    <input id="csrf-token" type="hidden" value="${csrfToken}" />
                    <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.accounts.reset-password.send-verification-email" text="Send Verification Email" /></button>
                </p>
            </form> <!-- #reset-password-form -->
        </c:otherwise>
        </c:choose>
        </div> <!-- #reset-password -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
<c:choose>
<c:when test="${isTokenValid}">
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-error').addClass('hide');
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('<spring:message code="voj.accounts.reset-password.please-wait" text="Please wait..." />');

            var email           = '${email}',
                token           = '${token}',
                newPassword     = $('#new-password').val()
                confirmPassword = $('#confirm-new-password').val(),
                csrfToken       = $('#csrf-token').val();

            return doResetPasswordAction(email, token, newPassword, confirmPassword, csrfToken);
        }
    </script>
    <script type="text/javascript">
        function doResetPasswordAction(email, token, newPassword, confirmPassword, csrfToken) {
            var postData = {
                'email': email,
                'token': token,
                'newPassword': newPassword,
                'confirmPassword': confirmPassword,
                'csrfToken': csrfToken
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/accounts/resetPassword.action" />',
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
                $('.alert-success').html('<spring:message code="voj.accounts.reset-password.password-resetted" text="Your password has been resetted." />');
                $('.alert-success').removeClass('hide');
                $('#reset-password-form').addClass('hide');
            } else {
                var errorMessage  = '';

                if ( !result['isCsrfTokenValid'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.invalid-csrf-token" text="Invalid CSRF Token." /><br>';
                }
                if ( !result['isEmailValidationValid'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.invalid-email-token" text="The token of resetting password seems invalid." /><br>';
                }
                if ( result['isNewPasswordEmpty'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.new-password-empty" text="You can&acute;t leave New Password empty." /><br>';
                } else if ( !result['isNewPasswordLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.new-password-illegal" text="The length of password must between 6 and 16 characters." /><br>';
                }
                if ( !result['isConfirmPasswordMatched'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.new-password-not-matched" text="New passwords don&acute;t match." /><br>';
                }
                $('.alert-error').html(errorMessage);
                $('.alert-error').removeClass('hide');
            }

            $('button[type=submit]').html('<spring:message code="voj.accounts.reset-password.reset-password" text="Reset Password" />');
            $('button[type=submit]').removeAttr('disabled');
        }
    </script>
</c:when>
<c:otherwise>
    <c:if test="${not empty token}">
    <script type="text/javascript">
        $(function() {
            $('.alert-error').html('<spring:message code="voj.accounts.reset-password.invalid-email-token" text="The token of resetting password seems invalid." />');
            $('.alert-error').removeClass('hide');
            $('#reset-password-form').addClass('hide');
        });
    </script>
    </c:if>
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-error').addClass('hide');
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('<spring:message code="voj.accounts.reset-password.please-wait" text="Please wait..." />');
            
            var username    = $('#username').val(),
                email       = $('#email').val(),
                csrfToken   = $('#csrf-token').val();

            return doResetPasswordAction(username, email, csrfToken);
        }
    </script>
    <script type="text/javascript">
        function doResetPasswordAction(username, email, csrfToken) {
            var postData = {
                'username': username,
                'email': email,
                'csrfToken': csrfToken
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/accounts/forgotPassword.action" />',
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
                $('.alert-success').html('<spring:message code="voj.accounts.reset-password.mail-sent" text="An email with password reset instructions has been sent to your email address." />');
                $('.alert-success').removeClass('hide');
                $('#reset-password-form').addClass('hide');
            } else {
                var errorMessage  = '';

                if ( !result['isCsrfTokenValid'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.invalid-csrf-token" text="Invalid CSRF Token." /><br>';
                } else if ( !result['isUserExists'] ) {
                    errorMessage += '<spring:message code="voj.accounts.reset-password.user-not-exists" text="Incorrect username or email." /><br>';
                }
                $('.alert-error').html(errorMessage);
                $('.alert-error').removeClass('hide');
            }

            $('button[type=submit]').html('<spring:message code="voj.accounts.reset-password.send-verification-email" text="Send Verification Email" />');
            $('button[type=submit]').removeAttr('disabled');
        }
    </script>
</c:otherwise>
</c:choose>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>