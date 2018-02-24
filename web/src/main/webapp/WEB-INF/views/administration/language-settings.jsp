<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.language-settings.title" text="Languages Settings" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/language-settings.css?v=${version}" />
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
                <h2 class="page-header"><i class="fa fa-code"></i> <spring:message code="voj.administration.language-settings.language-settings" text="Language Settings" /></h2>
                <form id="language-form" onSubmit="onSubmit(); return false;">
                    <div class="alert alert-warning">
                        <button type="button" class="close" data-dismiss="alert">×</button>
                        <h6><spring:message code="voj.administration.language-settings.be-careful" text="Please be Careful!" /></h6>
                        <p><spring:message code="voj.administration.language-settings.be-careful-message" text="Please don&acute;t change following settings unless you really know what you&acute;re doing." /></p>
                    </div> <!-- .alert-warning -->
                    <div class="alert alert-error hide"></div> <!-- .alert-error -->
                    <div class="alert alert-success hide"></div> <!-- .alert-success -->
                    <p>
                        <a id="new-language" href="javascript:void(0);">
                            <i class="fa fa-plus-circle"></i> 
                            <spring:message code="voj.administration.language-settings.new-language" text="New Language" />
                        </a>
                    </p>
                    <p id="no-languages" class="<c:if test="${languages.size() != 0}">hide</c:if>"><spring:message code="voj.administration.language-settings.no-languages" text="No languages available." /></p>
                    <ul id="languages">
                    <c:forEach items="${languages}" var="language">
                        <li class="language">
                            <div class="header">
                                <h5>${language.languageName}</h5>
                                <ul class="inline">
                                    <li><a href="javascript:void(0);"><i class="fa fa-edit"></i></a></li>
                                    <li><a href="javascript:void(0);"><i class="fa fa-trash"></i></a></li>
                                </ul>
                            </div> <!-- .header -->
                            <div class="body hide">
                                <div class="row-fluid">
                                    <div class="span4">
                                        <label>
                                            <spring:message code="voj.administration.language-settings.language-mode" text="Language Mode" />
                                            <a href="https://codemirror.net/mode/" target="_blank">(<spring:message code="voj.administration.language-settings.what-is-this" text="What&acute;s this?" />)</a>
                                        </label>
                                    </div> <!-- .span4 -->
                                    <div class="span8">
                                        <div class="control-group">
                                            <input class="language-slug span8" type="text" value="${language.languageSlug}" maxlength="16" />
                                        </div> <!-- .control-group -->
                                    </div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                                <div class="row-fluid">
                                    <div class="span4">
                                        <label><spring:message code="voj.administration.language-settings.language-name" text="Language Name" /></label>
                                    </div> <!-- .span4 -->
                                    <div class="span8">
                                        <div class="control-group">
                                            <input class="language-id span8" type="hidden" value="${language.languageId}" />
                                            <input class="language-name span8" type="text" value="${language.languageName}" maxlength="16" />
                                        </div> <!-- .control-group -->
                                    </div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                                <div class="row-fluid">
                                    <div class="span4">
                                        <label><spring:message code="voj.administration.language-settings.compile-command" text="Compile Command" /></label>
                                    </div> <!-- .span4 -->
                                    <div class="span8">
                                        <div class="control-group">
                                            <input class="compile-command span8" type="text" value="${language.compileCommand}" maxlength="128" />
                                        </div> <!-- .control-group -->
                                    </div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                                <div class="row-fluid">
                                    <div class="span4">
                                        <label><spring:message code="voj.administration.language-settings.run-command" text="Run Command" /></label>
                                    </div> <!-- .span4 -->
                                    <div class="span8">
                                        <div class="control-group">
                                            <input class="run-command span8" type="text" value="${language.runCommand}" maxlength="128" />
                                        </div> <!-- .control-group -->
                                    </div> <!-- .span8 -->
                                </div> <!-- .row-fluid -->
                            </div> <!-- .body -->
                        </li>
                    </c:forEach>
                    </ul>
                    <div class="row-fluid">
                        <button class="btn btn-primary" type="submit"><spring:message code="voj.administration.language-settings.save-changes" text="Save Changes" /></button>
                    </div> <!-- .row-fluid -->
                </form> <!-- #language-form -->
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <script type="text/javascript">
        $('#languages').on('click', 'i.fa-edit', function() {
            var languageContainer = $(this).parent().parent().parent().parent().parent(),
                isBodyUnfolded    = $('.body', $(languageContainer)).is(':visible');

            if ( isBodyUnfolded ) {
                $('.body', $(languageContainer)).addClass('hide');
            } else {
                $('.body', $(languageContainer)).removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#languages').on('click', 'i.fa-trash', function() {
            var languageContainer = $(this).parent().parent().parent().parent().parent(),
                languages         = $('li.language', '#languages').length;
            
            $(languageContainer).remove();

            if ( languages == 1 ) {
                $('#no-languages').removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#languages').on('change', 'input.language-name', function() {
            var languageContainer = $(this).parent().parent().parent().parent().parent(),
                languageName      = $(this).val();
            
            $('h5', $(languageContainer)).html(languageName);
        });
    </script>
    <script type="text/javascript">
        function onSubmit() {
            $('.alert-success').addClass('hide');
            $('.alert-error').addClass('hide');
            $('button[type=submit]').attr('disabled', 'disabled');
            $('button[type=submit]').html('<spring:message code="voj.administration.language-settings.please-wait" text="Please wait..." />');

            return updateLanguageSettings();
        }
    </script>
    <script type="text/javascript">
        function updateLanguageSettings(languages) {
            var postData = {
                'languages': getLanguages()
            }

            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/updateLanguageSettings.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function getLanguages() {
            var languages = [];
            
            $('.language').each(function() {
                var languageId     = $('.language-id', $(this)).val() || 0,
                    languageSlug   = $('.language-slug', $(this)).val(),
                    languageName   = $('.language-name', $(this)).val(),
                    compileCommand = $('.compile-command', $(this)).val(),
                    runCommand     = $('.run-command', $(this)).val(),
                    language       = {
                        'languageId': languageId,
                        'languageSlug': languageSlug,
                        'languageName': languageName,
                        'compileCommand': compileCommand,
                        'runCommand': runCommand
                    };
                languages.push(language);
            });
            return JSON.stringify(languages);
        }
    </script>
    <script type="text/javascript">
        function processResult(result) {
            if ( result['isSuccessful'] ) {
                var message  = '<p><spring:message code="voj.administration.language-settings.settings-saved" text="Settings saved." /></p>';

                if ( result['languageCreated'].length != 0 ) {
                    var totalLanguages = result['languageCreated'].length;
                    
                    message += '<p style="padding-left: 15px;"><spring:message code="voj.administration.language-settings.language-created" text="Following languages has been created:" /> ';
                    for ( var i = 0; i < totalLanguages; ++ i ) {
                        var languageId   = result['languageCreated'][i]['languageId'],
                            languageSlug = result['languageCreated'][i]['languageSlug'],
                            languageName = result['languageCreated'][i]['languageName'];

                        message += languageName + ( i != totalLanguages - 1 ? ', ' : '' );
                        setLanguageId(languageSlug, languageId);
                    }
                    message += '</p>';
                }
                if ( result['languageUpdated'].length != 0 ) {
                    var totalLanguages = result['languageUpdated'].length;

                    message += '<p style="padding-left: 15px;"><spring:message code="voj.administration.language-settings.language-updated" text="Following languages has been updated:" /> ';
                    for ( var i = 0; i < totalLanguages; ++ i ) {
                        message += result['languageUpdated'][i]['languageName'] + ( i != totalLanguages - 1 ? ', ' : '' );
                    }
                    message += '</p>';
                }
                if ( result['languageDeleted'].length != 0 ) {
                    var totalLanguages = result['languageDeleted'].length;

                    message += '<p style="padding-left: 15px;"><spring:message code="voj.administration.language-settings.language-deleted" text="Following languages has been deleted:" /> ';
                    for ( var i = 0; i < totalLanguages; ++ i ) {
                        message += result['languageDeleted'][i]['languageName'] + ( i != totalLanguages - 1 ? ', ' : '' );
                    }
                    message += '</p>';
                }
                $('.alert-success').html(message);
                $('.alert-success').removeClass('hide');
            } else {
                var message  = '';

                for ( var languageName in result ) {
                    var languageResult = result[languageName];

                    if ( typeof(languageResult) != 'object' ) {
                        continue;
                    }
                    if ( !languageResult['isSuccessful'] ) {
                        message += '<p>';
                        message += '<strong>' + languageName + ' <spring:message code="voj.administration.language-settings.language" text="language" /></strong><br>';
                        if ( 'isLanguageSlugEmpty' in languageResult && languageResult['isLanguageSlugEmpty'] ) {
                            message += '<spring:message code="voj.administration.language-settings.language-slug-empty" text="You can&acute;t leave Language Mode empty." /><br>';
                        } else if ( 'isLanguageSlugLegal' in languageResult && !languageResult['isLanguageSlugLegal'] ) {
                            message += '<spring:message code="voj.administration.language-settings.language-slug-illegal" text="The length of Language Mode CANNOT execeed 16 characters." /><br>';
                        } else if ( 'isLanguageSlugExists' in languageResult && languageResult['isLanguageSlugExists'] ) {
                            message += '<spring:message code="voj.administration.language-settings.language-slug-exists" text="Some other languages has taken the Language Mode." /><br>';
                        }
                        if ( 'isLanguageNameEmpty' in languageResult && languageResult['isLanguageNameEmpty'] ) {
                            message += '<spring:message code="voj.administration.language-settings.language-name-empty" text="You can&acute;t leave Language Name empty." /><br>';
                        } else if ( 'isLanguageNameLegal' in languageResult && !languageResult['isLanguageNameLegal'] ) {
                            message += '<spring:message code="voj.administration.language-settings.language-name-illegal" text="The length of Language Name CANNOT execeed 16 characters." /><br>';
                        }
                        if ( 'isCompileCommandEmpty' in languageResult && languageResult['isCompileCommandEmpty'] ) {
                            message += '<spring:message code="voj.administration.language-settings.compile-command-empty" text="You can&acute;t leave Compile Command empty." /><br>';
                        } else if ( 'isCompileCommandLegal' in languageResult && !languageResult['isCompileCommandLegal'] ) {
                            message += '<spring:message code="voj.administration.language-settings.compile-command-illegal" text="The Compile Command seems invalid." /><br>';
                        }
                        if ( 'isRunCommandEmpty' in languageResult && languageResult['isRunCommandEmpty'] ) {
                            message += '<spring:message code="voj.administration.language-settings.run-command-empty" text="You can&acute;t leave Run Command empty." /><br>';
                        } else if ( 'isRunCommandLegal' in languageResult && !languageResult['isRunCommandLegal'] ) {
                            message += '<spring:message code="voj.administration.language-settings.run-command-illegal" text="The Run Command seems invalid." /><br>';
                        }
                        if ( 'isLangaugeInUse' in languageResult && languageResult['isLangaugeInUse'] ) {
                            message += '<spring:message code="voj.administration.language-settings.language-in-use" text="The language can&acute;t be deleted." /><br>';
                        }
                        message += '</p>';
                    }
                }
                $('.alert-error').html(message);
                $('.alert-error').removeClass('hide');
            }

            $('button[type=submit]').html('<spring:message code="voj.administration.language-settings.save-changes" text="Save Changes" />');
            $('button[type=submit]').removeAttr('disabled');
            $('html, body').animate({ scrollTop: 0 }, 100);
        }
    </script>
    <script type="text/javascript">
        function setLanguageId(languageSlug, languageId) {
            var languageSlugInput = null;
            $('.language-slug', '#languages').each(function() {
                if ( $(this).val() == languageSlug ) {
                    languageSlugInput = $(this);
                }
            });

            var languageContainer = $(languageSlugInput).parent().parent().parent().parent(),
                languageIdInput   = $('.language-id', $(languageContainer));

            $(languageIdInput).val(languageId);
        }
    </script>
    <script type="text/javascript">
        $('#new-language').click(function() {
            $('#languages').append(
                '<li class="language">' + 
                '    <div class="header">' + 
                '        <h5></h5>' + 
                '        <ul class="inline">' + 
                '            <li><a href="javascript:void(0);"><i class="fa fa-edit"></i></a></li>' + 
                '            <li><a href="javascript:void(0);"><i class="fa fa-trash"></i></a></li>' + 
                '        </ul>' + 
                '    </div> <!-- .header -->' +
                '    <div class="body">' + 
                '        <div class="row-fluid">' +
                '            <div class="span4">' +
                '                <label>' +
                '                    <spring:message code="voj.administration.language-settings.language-mode" text="Language Mode" />' +
                '                    <a href="https://codemirror.net/mode/" target="_blank">(<spring:message code="voj.administration.language-settings.what-is-this" text="What&acute;s this?" />)</a>' +
                '                </label>' +
                '            </div> <!-- .span4 -->' +
                '            <div class="span8">' +
                '                <div class="control-group">' +
                '                    <input class="language-slug span8" type="text" maxlength="16" />' +
                '                </div> <!-- .control-group -->' +
                '            </div> <!-- .span8 -->' +
                '        </div> <!-- .row-fluid -->' +
                '        <div class="row-fluid">' +
                '            <div class="span4">' +
                '                <label><spring:message code="voj.administration.language-settings.language-name" text="Language Name" /></label>' +
                '            </div> <!-- .span4 -->' +
                '            <div class="span8">' +
                '                <div class="control-group">' +
                '                    <input class="language-id span8" type="hidden" value="0" />' +
                '                    <input class="language-name span8" type="text" maxlength="16" />' +
                '                </div> <!-- .control-group -->' +
                '            </div> <!-- .span8 -->' +
                '        </div> <!-- .row-fluid -->' +
                '        <div class="row-fluid">' +
                '            <div class="span4">' +
                '                <label><spring:message code="voj.administration.language-settings.compile-command" text="Compile Command" /></label>' +
                '            </div> <!-- .span4 -->' +
                '            <div class="span8">' +
                '                <div class="control-group">' +
                '                    <input class="compile-command span8" type="text" maxlength="128" />' +
                '                </div> <!-- .control-group -->' +
                '            </div> <!-- .span8 -->' +
                '        </div> <!-- .row-fluid -->' +
                '        <div class="row-fluid">' +
                '            <div class="span4">' +
                '                <label><spring:message code="voj.administration.language-settings.run-command" text="Run Command" /></label>' +
                '            </div> <!-- .span4 -->' +
                '            <div class="span8">' +
                '                <div class="control-group">' +
                '                    <input class="run-command span8" type="text" maxlength="128" />' +
                '                </div> <!-- .control-group -->' +
                '            </div> <!-- .span8 -->' +
                '        </div> <!-- .row-fluid -->' +
                '    </div> <!-- .body -->' +
                '</li>'
            );
        });
    </script>
</body>
</html>