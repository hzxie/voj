<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.general-settings.title" text="General Settings" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/general-settings.css?v=${version}" />
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
                <h2 class="page-header"><i class="fa fa-cog"></i> <spring:message code="voj.administration.general-settings.general-settings" text="General Settings" /></h2>
                <form id="option-form" onSubmit="onSubmit(); return false;">
                    <div class="alert alert-error hide"></div> <!-- .alert-error -->
                    <div class="alert alert-success hide"><spring:message code="voj.administration.general-settings.settings-saved" text="Settings saved." /></div> <!-- .alert-success -->
                    <div class="control-group row-fluid">
                        <label for="website-name"><spring:message code="voj.administration.general-settings.website-name" text="Website Name" /></label>
                        <input id="website-name" class="span12" type="text" value="${options['websiteName']}" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="website-description"><spring:message code="voj.administration.general-settings.website-description" text="Website Description" /></label>
                        <input id="website-description" class="span12" type="text" value="${options['description']}" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="copyright"><spring:message code="voj.administration.general-settings.copyright" text="Copyright" /></label>
                        <input id="copyright" class="span12" type="text" value="${options['copyright'].replace("\"", "\'")}" /> <!-- " -->
                    </div> <!-- .control-group -->
                    <div class="control-group switch-container row-fluid">
                        <div class="span8">
                            <label for="allow-register"><spring:message code="voj.administration.general-settings.allow-register" text="Allowing User Registration" /></label>
                        </div> <!-- .span8 -->
                        <div class="span4">
                            <input id="allow-register" type="checkbox" data-toggle="switch" <c:if test="${options['allowUserRegister'] != 0}">checked</c:if> />
                        </div> <!-- .span4 -->
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="icp-number"><spring:message code="voj.administration.general-settings.icp-number" text="ICP Number (for Chinese users)" /></label>
                        <input id="icp-number" class="span12" type="text" value="${options['icpNumber']}" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="police-icp-number"><spring:message code="voj.administration.general-settings.police-icp-number" text="Police ICP Number (for Chinese users)" /></label>
                        <input id="police-icp-number" class="span12" type="text" value="${options['policeIcpNumber']}" />
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="google-analytics-code">
                            <spring:message code="voj.administration.general-settings.google-analytics-code" text="Google Analytics Code" /> 
                            (<a href="https://www.google.com/analytics" target="_blank"><spring:message code="voj.administration.general-settings.learn-more" text="Learn More" /></a>)
                        </label>
                        <textarea id="google-analytics-code" class="span12" rows="5">${options['googleAnalyticsCode']}</textarea>
                    </div> <!-- .control-group -->
                    <div class="control-group row-fluid">
                        <label for="offensive-words"><spring:message code="voj.administration.general-settings.offensive-words" text="Offensive Words and Expressions" /></label>
                        <input id="offensive-words" class="tagsinput" type="hidden" value="" />
                    </div> <!-- .control-group -->
                    <div class="row-fluid">
                        <button class="btn btn-primary" type="submit"><spring:message code="voj.administration.general-settings.save-changes" text="Save Changes" /></button>
                    </div> <!-- .row-fluid -->
                </form> <!-- #option-form -->
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <script type="text/javascript">
        $(function() {
            var offensiveWordsList  = JSON.parse('${options['offensiveWords']}'),
                offensiveWordsValue = '';
            for ( var i = 0; i < offensiveWordsList.length; ++ i ) {
                offensiveWordsValue += offensiveWordsList[i] + ',';
            }
            
            $('#offensive-words').val(offensiveWordsValue);
            $('.tagsinput').tagsInput();
            $("[data-toggle='switch']").wrap('<div class="switch" />').parent().bootstrapSwitch();
        });
    </script>
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-success').addClass('hide');
            $('.alert-error').addClass('hide');
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('<spring:message code="voj.administration.general-settings.please-wait" text="Please wait..." />');

            var websiteName         = $('#website-name').val(),
                websiteDescription  = $('#website-description').val(),
                copyright           = $('#copyright').val(),
                allowUserRegister   = $('#allow-register').is(':checked'),
                icpNumber           = $('#icp-number').val(),
                policeIcpNumber     = $('#police-icp-number').val(),
                googleAnalyticsCode = $('#google-analytics-code').val(),
                offensiveWords      = $('#offensive-words').val();

            return doUpdateGeneralSettingsAction(websiteName, websiteDescription, copyright, allowUserRegister, icpNumber, policeIcpNumber, googleAnalyticsCode, offensiveWords);
        }
    </script>
    <script type="text/javascript">
        function doUpdateGeneralSettingsAction(websiteName, websiteDescription, copyright, allowUserRegister, icpNumber, policeIcpNumber, googleAnalyticsCode, offensiveWords) {
            var postData = {
                'websiteName': websiteName, 
                'websiteDescription': websiteDescription, 
                'copyright': copyright, 
                'allowUserRegister': allowUserRegister, 
                'policeIcpNumber': policeIcpNumber, 
                'icpNumber': icpNumber, 
                'googleAnalyticsCode': googleAnalyticsCode, 
                'offensiveWords': offensiveWords
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/updateGeneralSettings.action" />',
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
                $('.alert-success').removeClass('hide');
            } else {
                var errorMessage  = '';

                if ( result['isWebsiteNameEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.website-name-empty" text="You can&apos;t leave Website Name empty." /><br>';
                } else if ( !result['isWebisteNameLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.website-name-illegal" text="The length of Website Name CANNOT exceed 32 characters." /><br>';
                }
                if ( result['isDescriptionEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.website-description-empty" text="You can&apos;t leave Website Description empty." /><br>';
                } else if ( !result['isDescriptionLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.website-description-illegal" text="The length of Website Description CANNOT exceed 128 characters." /><br>';
                }
                if ( result['isCopyrightEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.copyright-empty" text="You can&apos;t leave Copyright empty." /><br>';
                } else if ( !result['isCopyrightLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.copyright-illegal" text="The length of Copyright CANNOT exceed 128 characters." /><br>';
                }
                if ( !result['isIcpNumberLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.icp-number-illegal" text="The ICP Number seems invalid." /><br>';
                } 
                if ( !result['isPoliceIcpNumberLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.police-icp-number-illegal" text="The Police ICP Number seems invalid." /><br>';
                } 
                if ( !result['isAnalyticsCodeLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.general-settings.analytics-code-illegal" text="The Google Analytics code seems invalid." /><br>';
                }
                $('.alert-error').html(errorMessage);
                $('.alert-error').removeClass('hide');
            }

            $('button[type=submit]').html('<spring:message code="voj.administration.general-settings.save-changes" text="Save Changes" />');
            $('button[type=submit]').removeAttr('disabled');
            $('html, body').animate({ scrollTop: 0 }, 100);
        }
    </script>
</body>
</html>