<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${language}" />
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.accounts.dashboard.title" text="Dashboard" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/accounts/dashboard.css?v=${version}" />
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
        <div id="sub-nav">
            <ul class="nav nav-tabs">
                <li class="active"><a href="#tab-statistics" data-toggle="tab"><spring:message code="voj.accounts.dashboard.statistics" text="Statistics" /></a></li>
                <li><a href="#tab-notifications" data-toggle="tab"><spring:message code="voj.accounts.dashboard.notifications" text="Notifications" /></a></li>
                <li><a href="#tab-customize" data-toggle="tab"><spring:message code="voj.accounts.dashboard.customize" text="Customize" /></a></li>
                <li><a href="#tab-messages" data-toggle="tab"><spring:message code="voj.accounts.dashboard.messages" text="Messages" /></a></li>
                <li><a href="#tab-accounts" data-toggle="tab"><spring:message code="voj.accounts.dashboard.accounts" text="Accounts" /></a></li>
            <c:if test="${myProfile.userGroup.userGroupSlug == 'administrators'}">
                <li><a href="<c:url value="/administration" />"><spring:message code="voj.accounts.dashboard.system-administration" text="System Administration" /></a></li>
            </c:if>
            </ul>
        </div> <!-- #sub-nav -->
        <div id="main-content" class="tab-content">
            <div class="tab-pane active" id="tab-statistics">
                <div class="section">
                    <div class="header">
                        <div class="row-fluid">
                            <div class="span8">
                                <h4><spring:message code="voj.accounts.dashboard.submission-calendar" text="Submission Calendar" /></h4>
                            </div> <!-- .span8 -->
                            <div class="span4">
                                <select id="submission-period">
                                    <option value="7"><spring:message code="voj.accounts.dashboard.1-week" text="1 Week" /></option>
                                    <option value="30"><spring:message code="voj.accounts.dashboard.1-month" text="1 Month" /></option>
                                    <option value="365"><spring:message code="voj.accounts.dashboard.1-year" text="1 Year" /></option>
                                </select>
                            </div> <!-- .span4 -->
                        </div> <!-- .row-fluid -->
                    </div> <!-- .header -->
                    <div class="body">
                        <div id="submissions-calendar"></div> <!-- #submissions-calendar -->
                    </div> <!-- .body -->
                </div> <!-- .section -->
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
            <div class="tab-pane" id="tab-notifications">
                <p>No notifications now.</p>
            </div> <!-- #tab-notifications -->
            <div class="tab-pane" id="tab-customize">
            </div> <!-- #tab-customize -->
            <div class="tab-pane" id="tab-messages">
                <div class="row-fluid">
                    <div class="span8">
                    </div> <!-- .span8 -->
                    <div class="span4">
                    </div> <!-- .span4 -->
                </div> <!-- .row-fluid -->
            </div> <!-- #tab-messages -->
            <div class="tab-pane" id="tab-accounts">
                <form id="password-form" class="section" method="POST" onSubmit="onChangePasswordSubmit(); return false;">
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
                </form> <!-- #password-form -->
                <form id="profile-form" class="section" method="POST" onSubmit="onChangeProfileSubmit(); return false;">
                    <h4><spring:message code="voj.accounts.dashboard.profile" text="Profile" /></h4>
                    <div class="row-fluid">
                        <div class="alert alert-error hide"></div>
                        <div class="alert alert-success hide"><spring:message code="voj.accounts.dashboard.profile-changed" text="You've changed your profile." /></div>
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="email"><spring:message code="voj.accounts.dashboard.email" text="Email" /></label>
                        </div> <!-- .span4 -->
                        <div class="span8">
                            <div class="control-group">
                                <input id="email" class="span8" type="text" value="${user.email}" maxlength="64" placeholder="you@example.com" />
                            </div> <!-- .control-group -->
                        </div> <!-- .span8 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="location"><spring:message code="voj.accounts.dashboard.location" text="Location" /></label>
                        </div> <!-- .span4 -->
                        <div class="span8">
                            <div class="control-group">
                                <input id="location" class="span8" type="text" value="${location}" maxlength="128" placeholder="<spring:message code="voj.accounts.dashboard.location-example" text="New York, USA" />" />
                            </div> <!-- .control-group -->
                        </div> <!-- .span8 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span4">
                            <label for="website"><spring:message code="voj.accounts.dashboard.website" text="Website" /></label>
                        </div> <!-- .span4 -->
                        <div class="span8">
                            <div class="control-group">
                                <input id="website" class="span8" type="text" value="${website}" maxlength="64" placeholder="http://zjhzxhz.com" />
                            </div> <!-- .control-group -->
                        </div> <!-- .span8 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span12">
                            <label for="social-links">
                                <spring:message code="voj.accounts.dashboard.social-links" text="Social Links" />
                                <a id="new-social-link" title="<spring:message code="voj.accounts.dashboard.new-social-link" text="New social link" />" href="javascript:void(0);">
                                    <i class="fa fa-plus-circle"></i>
                                </a>
                            </label>
                            <div id="social-links">
                                <p id="no-social-links"><spring:message code="voj.accounts.dashboard.no-social-links" text="No Social Links." /></p>
                                <ul></ul>
                            </div> <!-- #social-links -->
                        </div> <!-- .span12 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span12">
                            <label for="wmd-input"><spring:message code="voj.accounts.dashboard.about-me" text="About Me" /></label>    
                            <div id="markdown-editor">
                                <div class="wmd-panel">
                                    <div id="wmd-button-bar" class="wmd-button-bar"></div> <!-- #wmd-button-bar -->
                                    <textarea id="wmd-input" class="wmd-input" placeholder="<spring:message code="voj.accounts.dashboard.introduce-yourself" text="Write something about yourself." />">${aboutMe}</textarea>
                                </div> <!-- .wmd-panel -->
                                <div id="wmd-preview" class="wmd-panel wmd-preview"></div> <!-- .wmd-preview -->
                            </div> <!-- #markdown-editor -->
                        </div> <!-- .span12 -->
                    </div> <!-- .row-fluid -->
                    <div class="row-fluid">
                        <div class="span12">
                            <button class="btn btn-primary btn-block" type="submit"><spring:message code="voj.accounts.dashboard.update-profile" text="Update Profile" /></button>
                        </div> <!-- .span12 -->
                    </div> <!-- .row-fluid -->
                </form> <!-- #profile-form -->
            </div> <!-- #tab-accounts -->
        </div> <!-- #main-content -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js?v=${version}"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/highcharts.min.js?v=${version}', function() {
            return getSubmissionsOfUsers(7);
        });
    </script>
    <script type="text/javascript">
        $('#submission-period').change(function() {
            var period = $(this).val();
            return getSubmissionsOfUsers(period);
        });
    </script>
    <script type="text/javascript">
        function getSubmissionsOfUsers(period) {
            var pageRequests = {
                'period': period
            };

            $.ajax({
                type: 'GET',
                url: '<c:url value="/accounts/getNumberOfSubmissionsOfUsers.action" />',
                data: pageRequests,
                dataType: 'JSON',
                success: function(result){
                    return processSubmissionOfUsers(result['acceptedSubmissions'], result['totalSubmissions']);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processSubmissionOfUsers(acceptedSubmissionsMap, totalSubmissionsMap) {
            var categories          = [],
                totalSubmissions    = [],
                acceptedSubmissions = [];
            
            $.each(totalSubmissionsMap, function(key, value){
                categories.push(key);
                totalSubmissions.push(value);
            });
            $.each(acceptedSubmissionsMap, function(key, value){
                acceptedSubmissions.push(value);
            });
            displaySubmissionsOfUsers(categories, acceptedSubmissions, totalSubmissions);
        }
    </script>
    <script type="text/javascript">
        function displaySubmissionsOfUsers(categories, acceptedSubmissions, totalSubmissions) {
            Highcharts.setOptions({
                colors: ['#34495e', '#e74c3c']
            });

            $('#submissions-calendar').highcharts({
                chart: {
                    backgroundColor: null,
                },
                title: {
                    text: null
                },
                xAxis: {
                    categories: categories
                },
                yAxis: {
                    allowDecimals: false,
                    title: {
                        text: '<spring:message code="voj.accounts.dashboard.number-of-submissions" text="Number of Submissions" />'
                    }
                },
                tooltip: {
                    shared: true,
                    crosshairs: true
                },
                series: [
                    {
                        name: '<spring:message code="voj.accounts.dashboard.total-submissions" text="Total Submissions" />',
                        lineWidth: 4,
                        marker: {
                            radius: 4
                        },
                        data: totalSubmissions
                    },
                    {
                        name: '<spring:message code="voj.accounts.dashboard.accepted-submissions" text="Accepted Submissions" />',
                        data: acceptedSubmissions
                    }
                ]
            });
        }
    </script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/markdown.min.js?v=${version}', function() {
            converter = Markdown.getSanitizingConverter();
            editor    = new Markdown.Editor(converter);
            editor.run();

            $('.markdown').each(function() {
                var plainContent    = $(this).text(),
                    markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                
                $(this).html(markdownContent);
            });
        });
    </script>
    <script type="text/javascript">
        function onChangePasswordSubmit() {
            $('.alert-success', '#password-form').addClass('hide');
            $('.alert-error', '#password-form').addClass('hide');
            $('button[type=submit]', '#password-form').attr('disabled', 'disabled');
            $('button[type=submit]', '#password-form').html('<spring:message code="voj.accounts.dashboard.please-wait" text="Please wait..." />');

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
                $('.alert-success', '#password-form').removeClass('hide');
            } else {
                var errorMessage  = '';

                if ( !result['isOldPasswordCorrect'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.old-password-incorrect" text="Old password is incorrect." /><br>';
                }
                if ( result['isNewPasswordEmpty'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.new-password-empty" text="You can&acute;t leave New Password empty." /><br>';
                }
                if ( !result['isNewPasswordLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.new-password-illegal" text="The length of password must between 6 and 16 characters." /><br>';
                }
                if ( !result['isConfirmPasswordMatched'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.new-password-not-matched" text="New passwords don&acute;t match." /><br>';
                }
                $('.alert-error', '#password-form').html(errorMessage);
                $('.alert-error', '#password-form').removeClass('hide');
            }
            $('button[type=submit]', '#password-form').html('<spring:message code="voj.accounts.dashboard.change-password" text="Change Password" />');
            $('button[type=submit]', '#password-form').removeAttr('disabled');
        }
    </script>
    <script type="text/javascript">
        socialServices      = {
            'Facebook'      : 'https://facebook.com/',
            'Twitter'       : 'https://twitter.com/',
            'Weibo'         : 'http://weibo.com/',
            'Instagram'     : 'https://instagram.com/',
            'GitHub'        : 'https://github.com/',
            'StackOverflow' : 'http://stackoverflow.com/users/',
            'LinkedIn'      : 'https://www.linkedin.com/profile/view?id='
        };
    </script>
    <script type="text/javascript">
        $(function() {
            var mySocialLinks   = {
                'Facebook'      : '${socialLinks['Facebook']}',
                'Twitter'       : '${socialLinks['Twitter']}',
                'Weibo'         : '${socialLinks['Weibo']}',
                'Instagram'     : '${socialLinks['Instagram']}',
                'GitHub'        : '${socialLinks['GitHub']}',
                'StackOverflow' : '${socialLinks['StackOverflow']}',
                'LinkedIn'      : '${socialLinks['LinkedIn']}'
            };

            for ( var serviceName in mySocialLinks ) {
                var serviceBaseUrl  = socialServices[serviceName],
                    serviceUsername = mySocialLinks[serviceName],
                    serviceUrl      = serviceBaseUrl + serviceUsername;

                if ( typeof(serviceUsername) != 'undefined' && serviceUsername != '' ) {
                    $('#no-social-links').addClass('hide');
                    $('#social-links > ul').append(getSocialLinkContainer(serviceName, serviceUrl));
                }
            }
        });
    </script>
    <script type="text/javascript">
        $('#new-social-link').click(function() {
            var serviceName = 'Facebook', 
                serviceUrl  = socialServices['Facebook'];
            
            $('#no-social-links').addClass('hide');
            $('#social-links > ul').append(getSocialLinkContainer(serviceName, serviceUrl));
        });
    </script>
    <script type="text/javascript">
        function getSocialLinkContainer(serviceName, serviceUrl) {
            var containerTemplate = '<li class="social-link">' +
                                    '    <div class="header">' +
                                    '        <h5>%s</h5>' +
                                    '        <ul class="inline">' +
                                    '            <li><a href="javascript:void(0);"><i class="fa fa-edit"></i></a></li>' +
                                    '            <li><a href="javascript:void(0);"><i class="fa fa-trash"></i></a></li>' +
                                    '        </ul>' +
                                    '    </div> <!-- .header -->' +
                                    '    <div class="body hide">' +
                                    '        <div class="row-fluid">' +
                                    '            <div class="span4">' +
                                    '                <label><spring:message code="voj.accounts.dashboard.service-name" text="Service Name" /></label>' +
                                    '            </div> <!-- .span4 -->' +
                                    '            <div class="span8">' +
                                    '                <div class="control-group">' +
                                    '                    <select class="service">' + getSocialLinkOptions(serviceName) + '</select>' +
                                    '                </div> <!-- .control-group -->' +
                                    '            </div> <!-- .span8 -->' +
                                    '        </div> <!-- .row-fluid -->' +
                                    '        <div class="row-fluid">' +
                                    '            <div class="span4">' +
                                    '                <label>URL</label>' +
                                    '            </div> <!-- .span4 -->' +
                                    '            <div class="span8">' +
                                    '                <div class="control-group">' +
                                    '                    <input class="url span8" type="text" maxlength="128" value="%s" />' +
                                    '                </div> <!-- .control-group -->' +
                                    '            </div> <!-- .span8 -->' +
                                    '        </div> <!-- .row-fluid -->' +
                                    '    </div> <!-- .body -->' +
                                    '</li> <!-- .social-link -->';

            return containerTemplate.format(serviceName, serviceUrl);
        }
    </script>
    <script type="text/javascript">
        function getSocialLinkOptions(selectedServiceName) {
            var socialLinkOptions       = '',
                optionTemplate          = '<option value="%s">%s</option>',
                selectedOptionTemplate  = '<option value="%s" selected>%s</option>';

            for ( var serviceName in socialServices ) {
                if ( serviceName == selectedServiceName ) {
                    socialLinkOptions  += selectedOptionTemplate.format(serviceName, serviceName);
                } else {
                    socialLinkOptions  += optionTemplate.format(serviceName, serviceName);
                }
            }
            return socialLinkOptions;
        }
    </script>
    <script type="text/javascript">
        $('#social-links').on('click', 'i.fa-edit', function() {
            var socialLinkContainer = $(this).parent().parent().parent().parent().parent(),
                isBodyUnfolded      = $('.body', $(socialLinkContainer)).is(':visible');

            if ( isBodyUnfolded ) {
                $('.body', $(socialLinkContainer)).addClass('hide');
            } else {
                $('.body', $(socialLinkContainer)).removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#social-links').on('click', 'i.fa-trash', function() {
            var socialLinkContainer = $(this).parent().parent().parent().parent().parent(),
                socialLinks         = $('li.social-link', '#social-links').length;
            
            $(socialLinkContainer).remove();

            if ( socialLinks == 1 ) {
                $('#no-social-links').removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#social-links').on('change', 'select.service', function() {
            var socialLinkContainer = $(this).parent().parent().parent().parent().parent(),
                serviceName         = $(this).val(),
                serviceBaseUrl      = socialServices[serviceName];
            
            $('h5', $(socialLinkContainer)).html(serviceName);
            $('input.url', $(socialLinkContainer)).val(serviceBaseUrl);
        });
    </script>
    <script type="text/javascript">
        function getSocialLinks() {
            var socialLinks = {};

            $('.social-link').each(function() {
                var serviceName     = $('select.service', $(this)).val(),
                    serviceUrl      = $('input.url', $(this)).val(),
                    serviceBaseUrl  = socialServices[serviceName],
                    serviceUsername = serviceUrl.substr(serviceBaseUrl.length);

                if ( serviceUrl.indexOf(serviceBaseUrl) != -1 && serviceUsername != '' ) {
                    socialLinks[serviceName] = serviceUsername;
                }
            });
            return JSON.stringify(socialLinks);
        }
    </script>
    <script type="text/javascript">
        function onChangeProfileSubmit() {
            $('.alert-success', '#profile-form').addClass('hide');
            $('.alert-error', '#profile-form').addClass('hide');
            $('button[type=submit]', '#profile-form').attr('disabled', 'disabled');
            $('button[type=submit]', '#profile-form').html('<spring:message code="voj.accounts.dashboard.please-wait" text="Please wait..." />');

            var email       = $('#email').val(),
                location    = $('#location').val(),
                website     = $('#website').val(),
                socialLinks = getSocialLinks(),
                aboutMe     = $('#wmd-input').val();

            return doUpdateProfileAction(email, location, website, socialLinks, aboutMe);
        }
    </script>
    <script type="text/javascript">
        function doUpdateProfileAction(email, location, website, socialLinks, aboutMe) {
            var postData = {
                'email': email,
                'location': location,
                'website': website,
                'socialLinks': socialLinks,
                'aboutMe': aboutMe
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/accounts/updateProfile.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processUpdateProfileResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processUpdateProfileResult(result) {
            if ( result['isSuccessful'] ) {
                $('.alert-success', '#profile-form').removeClass('hide');
            } else {
                var errorMessage  = '';

                if ( result['isEmailEmpty'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.email-empty" text="You can&acute;t leave Email empty." /><br>';
                } else if ( !result['isEmailLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.email-illegal" text="The email seems invalid." /><br>';
                } else if ( result['isEmailExists'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.email-existing" text="Someone already has that email address." /><br>';
                }
                if ( !result['isLocationLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.location-illegal" text="The length of Location CANNOT exceed 128 characters." /><br>';
                }
                if ( !result['isWebsiteLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.website-legal" text="The url of website seems invalid." /><br>';
                }
                if ( !result['isAboutMeLegal'] ) {
                    errorMessage += '<spring:message code="voj.accounts.dashboard.about-me-legal" text="The length of About Me CANNOT exceed 256 characters." /><br>';
                }
                $('.alert-error', '#profile-form').html(errorMessage);
                $('.alert-error', '#profile-form').removeClass('hide');
            }
            $('button[type=submit]', '#profile-form').html('<spring:message code="voj.accounts.dashboard.update-profile" text="Update Profile" />');
            $('button[type=submit]', '#profile-form').removeAttr('disabled');
        }
    </script>
    <c:if test="${GoogleAnalyticsCode != ''}">
    ${googleAnalyticsCode}
    </c:if>
</body>
</html>