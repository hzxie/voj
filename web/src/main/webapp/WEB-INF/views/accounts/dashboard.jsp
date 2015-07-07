<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.dashboard.title" text="Dashboard" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/dashboard.css" />
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
        <div id="sub-nav">
            <ul class="nav nav-tabs">
                <li><a href="#tab-statistics" data-toggle="tab"><spring:message code="voj.accounts.dashboard.statistics" text="Statistics" /></a></li>
                <li class="active"><a href="#tab-notifications" data-toggle="tab"><spring:message code="voj.accounts.dashboard.notifications" text="Notifications" /></a></li>
                <li><a href="#tab-customize" data-toggle="tab"><spring:message code="voj.accounts.dashboard.customize" text="Customize" /></a></li>
                <li><a href="#tab-messages" data-toggle="tab"><spring:message code="voj.accounts.dashboard.messages" text="Messages" /></a></li>
                <li><a href="#tab-accounts" data-toggle="tab"><spring:message code="voj.accounts.dashboard.accounts" text="Accounts" /></a></li>
            <c:if test="${myProfile.userGroup.userGroupSlug == 'administrators'}">
                <li><a href="<c:url value="/administration" />"><spring:message code="voj.accounts.dashboard.system-administration" text="System Administration" /></a></li>
            </c:if>
            </ul>
        </div> <!-- #sub-nav -->
        <div id="main-content" class="tab-content">
            <div class="tab-pane" id="tab-statistics">
                <div class="section">
                    <div class="header">
                        <h4><spring:message code="voj.accounts.dashboard.submissions" text="Submissions" /></h4>
                    </div> <!-- .header -->
                    <div class="body">
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
            </div> <!-- #tab-statistics -->
            <div class="tab-pane active" id="tab-notifications">
                notifications
            </div> <!-- #tab-notifications -->
            <div class="tab-pane" id="tab-customize">
                customize
            </div> <!-- #tab-customize -->
            <div class="tab-pane" id="tab-messages">
                messages
            </div> <!-- #tab-messages -->
            <div class="tab-pane" id="tab-accounts">
                <form id="change-password-form" class="section" method="POST" onSubmit="onChangePasswordSubmit(); return false;">
                    <h4><spring:message code="voj.accounts.dashboard.change-password" text="Change Password" /></h4>
                    <div class="row-fluid">
                        <div class="alert alert-error hide"></div>
                        <div class="alert alert-success hide"><spring:message code="voj.accounts.dashboard.password-changed" text="You've changed your password." /></div>
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="old-password"><spring:message code="voj.accounts.dashboard.old-password" text="Old Password" /></label>
                        </div> <!-- .span4 -->
                        <div class="span8">
                            <div class="control-group">
                                <input id="old-password" class="span8" type="password" maxlength="16" />
                            </div> <!-- .control-group -->
                        </div> <!-- .span8 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="new-password"><spring:message code="voj.accounts.dashboard.new-password" text="New Password" /></label>
                        </div> <!-- .span4 -->
                        <div class="span8">
                            <div class="control-group">
                                <input id="new-password" class="span8" type="password" maxlength="16" />
                            </div> <!-- .control-group -->
                        </div> <!-- .span8 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="confirm-new-password"><spring:message code="voj.accounts.dashboard.confirm-new-password" text="Confirm New Password" /></label>
                        </div> <!-- .span4 -->
                        <div class="span8">
                            <div class="control-group">
                                <input id="confirm-new-password" class="span8" type="password" maxlength="16" />
                            </div> <!-- .control-group -->
                        </div> <!-- .span8 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span12">
                            <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.accounts.dashboard.change-password" text="Change Password" /></button>
                        </div> <!-- .span12 -->
                    </div> <!-- .row-fluid -->
                </form>
            </div> <!-- #tab-accounts -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        function onChangePasswordSubmit() {
            $('.alert-success', '#change-password-form').addClass('hide');
            $('.alert-error', '#change-password-form').addClass('hide');
            $('button[type=submit]', '#change-password-form').attr('disabled', 'disabled');
            $('button[type=submit]', '#change-password-form').html('<spring:message code="voj.accounts.dashboard.please-wait" text="Please wait..." />');

            var oldPassword     = $('#old-password').val(),
                newPassword     = $('#new-password').val(),
                confirmPassword = $('#confirm-new-password').val();

            return doChangePasswordAction(oldPassword, newPassword, confirmPassword);
        }
    </script>
    <script type="text/javascript">
        function doChangePasswordAction(oldPassword, newPassword, confirmPassword) {
            var postData = {
                'oldPassword': oldPassword,
                'newPassword': newPassword,
                'confirmPassword': confirmPassword
            };
            
            $.ajax({
                type: 'POST',
                url: '<c:url value="/accounts/changePassword.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processChangePasswordResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processChangePasswordResult(result) {
            if ( result['isSuccessful'] ) {
                $('.alert-success', '#change-password-form').removeClass('hide');
            } else {
                var errorMessage  = '';

                if ( !result['isOldPasswordCorrect'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.old-password-incorrect" text="Old password is incorrect." /><br>';
                }
                if ( result['isNewPasswordEmpty'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.new-password-empty" text="You can&acute;t leave new password empty." /><br>';
                }
                if ( !result['isNewPasswordLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.new-password-illegal" text="The length of password must between 6 and 16 characters." /><br>';
                }
                if ( !result['isConfirmPasswordMatched'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.new-password-not-matched" text="New passwords don&acute;t match." /><br>';
                }
                $('.alert-error', '#change-password-form').html(errorMessage);
                $('.alert-error', '#change-password-form').removeClass('hide');
            }
            $('button[type=submit]', '#change-password-form').html('<spring:message code="voj.accounts.dashboard.change-password" text="Change Password" />');
            $('button[type=submit]', '#change-password-form').removeAttr('disabled');
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    <script type="text/javascript">${googleAnalyticsCode}</script>
    </c:if>
</body>
</html>