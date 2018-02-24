<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.edit-problem.title" text="Edit Problem" /> - ${problem.problemName} | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/new-problem.css?v=${version}" />
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
                <h2 class="page-header"><i class="fa fa-file"></i> <spring:message code="voj.administration.edit-problem.edit-problem" text="Edit Problem" /></h2>
                <form id="problem-form" onSubmit="onSubmit(); return false;">
                    <div class="row-fluid">
                        <div class="span8">
                            <div class="alert alert-error hide"></div> <!-- .alert-error -->
                            <div class="alert alert-success hide"><spring:message code="voj.administration.edit-problem.problem-edited" text="Problem updated." /> <a href="<c:url value="/p/" />${problem.problemId}"><spring:message code="voj.administration.edit-problem.view-problem" text="View problem" /></a></div> <!-- .alert-success -->    
                            <div class="control-group row-fluid">
                                <label for="problem-name"><spring:message code="voj.administration.edit-problem.problem-name" text="Problem Name" /></label>
                                <input id="problem-name" class="span12" type="text" maxlength="128" value="${problem.problemName}" />
                                <input id="problem-id" type="hidden" value="${problem.problemId}" />
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="time-limit"><spring:message code="voj.administration.edit-problem.time-limit" text="Time Limit" /> (ms)</label>
                                <input id="time-limit" class="span12" type="text" maxlength="8" value="${problem.timeLimit}" />
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="memory-limit"><spring:message code="voj.administration.edit-problem.memory-limit" text="Memory Limit" /> (KB)</label>
                                <input id="memory-limit" class="span12" type="text" maxlength="8" value="${problem.memoryLimit}" />
                            </div> <!-- .control-group -->
                            <div class="row-fluid">
                                <div class="span12">
                                    <label for="wmd-input"><spring:message code="voj.administration.edit-problem.problem-description" text="Description" /></label>    
                                    <div id="markdown-editor">
                                        <div class="wmd-panel">
                                            <div id="wmd-button-bar" class="wmd-button-bar"></div> <!-- #wmd-button-bar -->
                                            <textarea id="wmd-input" class="wmd-input">${problem.description}</textarea>
                                        </div> <!-- .wmd-panel -->
                                        <div id="wmd-preview" class="wmd-panel wmd-preview"></div> <!-- .wmd-preview -->
                                    </div> <!-- #markdown-editor -->
                                </div> <!-- .span12 -->
                            </div> <!-- .row-fluid -->
                            <div class="control-group row-fluid">
                                <label for="hint"><spring:message code="voj.administration.edit-problem.hint" text="Hint" /></label>
                                <textarea id="hint" class="span12">${problem.hint}</textarea>
                            </div> <!-- .control-group -->
                            <h4><spring:message code="voj.administration.edit-problem.input-output" text="Input / Output" /></h4>
                            <div class="control-group row-fluid">
                                <label for="input-format"><spring:message code="voj.administration.edit-problem.input-format" text="Input Format" /></label>
                                <textarea id="input-format" class="span12">${problem.inputFormat}</textarea>
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="output-format"><spring:message code="voj.administration.edit-problem.output-format" text="Output Format" /></label>
                                <textarea id="output-format" class="span12">${problem.outputFormat}</textarea>
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="input-sample"><spring:message code="voj.administration.edit-problem.input-sample" text="Input Sample" /></label>
                                <textarea id="input-sample" class="span12">${problem.sampleInput}</textarea>
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="output-sample"><spring:message code="voj.administration.edit-problem.output-sample" text="Output Sample" /></label>
                                <textarea id="output-sample" class="span12">${problem.sampleOutput}</textarea>
                            </div> <!-- .control-group -->
                            <div class="row-fluid">
                                <div class="span6">
                                    <h4><spring:message code="voj.administration.edit-problem.test-cases" text="Test Cases" /></h4>
                                </div> <!-- .span6 -->
                                <div class="span6 text-right">
                                    <a id="new-test-case" title="<spring:message code="voj.administration.edit-problem.new-test-case" text="New test case" />" href="javascript:void(0);">
                                        <i class="fa fa-plus-circle"></i>
                                    </a>
                                </div> <!-- .span6 -->
                            </div> <!-- .row-fluid -->
                            <div class="row-fluid">
                                <div class="span12">
                                    <div id="test-cases">
                                        <p id="no-test-cases"><spring:message code="voj.administration.edit-problem.no-test-cases" text="No Test Cases." /></p>
                                        <ul>
                                        <c:forEach var="checkpoint" items="${checkpoints}">
                                            <li class="test-case">
                                                <div class="header">
                                                    <h5><spring:message code="voj.administration.edit-problem.test-case" text="Test Case" /> #${checkpoint.checkpointId}</h5>
                                                    <ul class="inline">
                                                        <li><a href="javascript:void(0);"><i class="fa fa-edit"></i></a></li>
                                                        <li><a href="javascript:void(0);"><i class="fa fa-trash"></i></a></li>
                                                    </ul>
                                                </div> <!-- .header -->
                                                <div class="body">
                                                    <div class="row-fluid">
                                                        <div class="span4">
                                                            <label><spring:message code="voj.administration.edit-problem.standard-input" text="Standard Input" /></label>
                                                        </div> <!-- .span4 -->
                                                        <div class="span8">
                                                            <textarea class="standard-input span12">${checkpoint.input}</textarea>
                                                        </div> <!-- .span8 -->
                                                    </div> <!-- .row-fluid -->
                                                    <div class="row-fluid">
                                                        <div class="span4">
                                                            <label><spring:message code="voj.administration.edit-problem.standard-output" text="Standard Output" /></label>
                                                        </div> <!-- .span4 -->
                                                        <div class="span8">
                                                            <textarea class="standard-output span12">${checkpoint.output}</textarea>
                                                        </div> <!-- .span8 -->
                                                    </div> <!-- .row-fluid -->
                                                </div> <!-- .body -->
                                            </li> <!-- .test-case -->
                                        </c:forEach>
                                        </ul>
                                    </div> <!-- #test-cases -->
                                </div> <!-- .span12 -->
                            </div> <!-- .row-fluid -->
                        </div> <!-- .span8 -->
                        <div class="span4">
                            <div class="section">
                                <div class="header">
                                    <h5><spring:message code="voj.administration.edit-problem.edit-problem" text="Edit Problem" /></h5>
                                </div> <!-- .header -->
                                <div class="body">
                                    <div class="row-fluid">
                                        <div class="span8">
                                            <spring:message code="voj.administration.edit-problem.is-public" text="Public to Users?" />
                                        </div> <!--- .span8 -->
                                        <div class="span4 text-right">
                                            <input id="problem-is-public" type="checkbox" data-toggle="switch" <c:if test="${problem.isPublic()}">checked="checked"</c:if> />
                                        </div> <!-- .span4 -->
                                    </div> <!-- .row-fluid -->
                                    <div class="row-fluid">
                                        <div class="span8">
                                            <spring:message code="voj.administration.edit-problem.test-case-exactly-match" text="Test Case Exactly Match" />
                                        </div> <!--- .span8 -->
                                        <div class="span4 text-right">
                                            <input id="problem-is-exactly-match" type="checkbox" data-toggle="switch" />
                                        </div> <!-- .span4 -->
                                    </div> <!-- .row-fluid -->
                                </div> <!-- .body -->
                                <div class="footer text-right">
                                    <button class="btn btn-primary" type="submit"><spring:message code="voj.administration.edit-problem.update-problem" text="Update" /></button>
                                </div> <!-- .footer -->
                            </div> <!-- .section -->
                            <div class="section">
                                <div class="header">
                                    <h5><spring:message code="voj.administration.edit-problem.problem-categories" text="Categories" /></h5>
                                </div> <!-- .header -->
                                <div class="body">
                                    <c:forEach var="entry" items="${problemCategories}">
                                    <ul class="parent-categories">                                        
                                        <li>
                                            <label class="checkbox parent-category" for="${entry.key.problemCategorySlug}">
                                                <input id="${entry.key.problemCategorySlug}" type="checkbox" data-toggle="checkbox"> ${entry.key.problemCategoryName}
                                            </label>
                                            <ul class="sub-categories">
                                            <c:forEach var="problemCategory" items="${entry.value}">
                                                <li>
                                                    <label class="checkbox child-category" for="${problemCategory.problemCategorySlug}">
                                                        <input id="${problemCategory.problemCategorySlug}" type="checkbox" data-toggle="checkbox"> ${problemCategory.problemCategoryName}
                                                    </label>
                                                </li>
                                            </c:forEach>
                                            </ul>
                                        </li>
                                    </ul>
                                </c:forEach>
                                </div> <!-- .body -->
                            </div> <!-- .section -->
                            <div class="section">
                                <div class="header">
                                    <h5><spring:message code="voj.administration.edit-problem.problem-tags" text="Tags" /></h5>
                                </div> <!-- .header -->
                                <div class="body">
                                    <input id="problem-tags" class="tagsinput" type="hidden" value="<c:forEach var="problemTag" items="${problemTags}">${problemTag.problemTagName},</c:forEach>" />
                                </div> <!-- .body -->
                            </div> <!-- .section -->
                        </div> <!-- .span4 -->
                    </div> <!-- .row-fluid -->
                </form>
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <script type="text/javascript">
        $(function() {
        <c:if test="${fn:length(checkpoints) != 0}">
            $('#no-test-cases').addClass('hide');
        </c:if>

        <c:forEach var="problemCategory" items="${selectedProblemCategories}">
            $('#${problemCategory.problemCategorySlug}').parent().addClass('checked');
        </c:forEach>

            $('.tagsinput').tagsInput();
            $('[data-toggle=switch]').wrap('<div class="switch" />').parent().bootstrapSwitch();
        });
    </script>
    <script type='text/javascript'>
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
        $('#new-test-case').click(function() {
            var testCaseId = $('li.test-case', '#test-cases').length; 
            
            $('#no-test-cases').addClass('hide');
            $('#test-cases > ul').append(getTestCaseContainer(testCaseId));
        });
    </script>
    <script type="text/javascript">
        function getTestCaseContainer(testCaseId, standardInput, standardOutput) {
            var containerTemplate = '<li class="test-case">' +
                                    '    <div class="header">' +
                                    '        <h5><spring:message code="voj.administration.edit-problem.test-case" text="Test Case" /> #%s</h5>' +
                                    '        <ul class="inline">' +
                                    '            <li><a href="javascript:void(0);"><i class="fa fa-edit"></i></a></li>' +
                                    '            <li><a href="javascript:void(0);"><i class="fa fa-trash"></i></a></li>' +
                                    '        </ul>' +
                                    '    </div> <!-- .header -->' +
                                    '    <div class="body">' +
                                    '        <div class="row-fluid">' +
                                    '            <div class="span4">' +
                                    '                <label><spring:message code="voj.administration.edit-problem.standard-input" text="Standard Input" /></label>' +
                                    '            </div> <!-- .span4 -->' +
                                    '            <div class="span8">' +
                                    '                <textarea class="standard-input span12">%s</textarea>' + 
                                    '            </div> <!-- .span8 -->' +
                                    '        </div> <!-- .row-fluid -->' +
                                    '        <div class="row-fluid">' +
                                    '            <div class="span4">' +
                                    '                <label><spring:message code="voj.administration.edit-problem.standard-output" text="Standard Output" /></label>' +
                                    '            </div> <!-- .span4 -->' +
                                    '            <div class="span8">' +
                                    '                <textarea class="standard-output span12">%s</textarea>' + 
                                    '            </div> <!-- .span8 -->' +
                                    '        </div> <!-- .row-fluid -->' +
                                    '    </div> <!-- .body -->' +
                                    '</li> <!-- .test-case -->';

            return containerTemplate.format(testCaseId, 
                typeof(standardInput) == 'undefined' ? '' : standardInput, 
                typeof(standardOutput) == 'undefined' ? '' : standardOutput);
        }
    </script>
    <script type="text/javascript">
        $('#test-cases').on('click', 'i.fa-edit', function() {
            var testCaseContainer = $(this).parent().parent().parent().parent().parent(),
                isBodyUnfolded      = $('.body', $(testCaseContainer)).is(':visible');

            if ( isBodyUnfolded ) {
                $('.body', $(testCaseContainer)).addClass('hide');
            } else {
                $('.body', $(testCaseContainer)).removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('#test-cases').on('click', 'i.fa-trash', function() {
            var testCaseContainer = $(this).parent().parent().parent().parent().parent(),
                testCases         = $('li.test-case', '#test-cases').length,
                testCaseName      = '<spring:message code="voj.administration.edit-problem.test-case" text="Test Case" /> #%s';

            $(testCaseContainer).remove();
            $('li.test-case', '#test-cases').each(function(index) {
                $('h5', this).html(testCaseName.format(index));
            });

            if ( testCases == 1 ) {
                $('#no-test-cases').removeClass('hide');
            }
        });
    </script>
    <script type="text/javascript">
        $('label.checkbox.parent-category').click(function() {
            var currentControl = $(this);
            // Fix the bug for Checkbox in FlatUI 
            setTimeout(function() {
                var isChecked = $(currentControl).hasClass('checked');

                if ( !isChecked ) {
                    $('label.checkbox.child-category', $(currentControl).parent()).removeClass('checked');
                }
            }, 50);
        });
    </script>
    <script type="text/javascript">
        $('label.checkbox.child-category').click(function() {
            var currentControl = $(this);
            // Fix the bug for Checkbox in FlatUI 
            setTimeout(function() {
                var isChecked = $(currentControl).hasClass('checked');

                if ( isChecked ) {
                    $('label.checkbox.parent-category', $(currentControl).parent().parent().parent()).addClass('checked');
                }
            }, 50);
        });
    </script>
    <script type="text/javascript">
        function onSubmit() {
            var problemId           = $('#problem-id').val(),
                problemName         = $('#problem-name').val(),
                timeLimit           = $('#time-limit').val(),
                memoryLimit         = $('#memory-limit').val(),
                description         = $('#wmd-input').val(),
                hint                = $('#hint').val(),
                inputFormat         = $('#input-format').val(),
                outputFormat        = $('#output-format').val(),
                inputSample         = $('#input-sample').val(),
                outputSample        = $('#output-sample').val(),
                testCases           = getTestCases(),
                problemCategories   = getProblemCategories(),
                problemTags         = getProblemTags(),
                isPublic            = $('#problem-is-public').parent().hasClass('switch-on'),
                isExactlyMatch      = $('#problem-is-exactly-match').parent().hasClass('switch-on');

            $('.alert-success', '#problem-form').addClass('hide');
            $('.alert-error', '#problem-form').addClass('hide');
            $('button[type=submit]', '#problem-form').attr('disabled', 'disabled');
            $('button[type=submit]', '#problem-form').html('<spring:message code="voj.administration.edit-problem.please-wait" text="Please wait..." />');

            return editProblem(problemId, problemName, timeLimit, memoryLimit, description, 
                    hint, inputFormat, outputFormat, inputSample, outputSample, testCases, 
                    problemCategories, problemTags, isPublic, isExactlyMatch);
        }
    </script>
    <script type="text/javascript">
        function getTestCases() {
            var testCases   = [];

            $('li.test-case').each(function() {
                var input   = $('.standard-input', $(this)).val(),
                    output  = $('.standard-output', $(this)).val();

                testCases.push({
                    'input': input,
                    'output': output
                });
            });
            return JSON.stringify(testCases);
        }
    </script>
    <script type="text/javascript">
        function getProblemCategories() {
            var problemCategories = [];

            $('label.checked', '.parent-categories').each(function() {
                problemCategories.push($(this).attr('for'));
            });
            return JSON.stringify(problemCategories);
        }
    </script>
    <script type="text/javascript">
        function getProblemTags() {
            var problemTags = $('#problem-tags').val();

            if ( problemTags == '' ) {
                problemTags = [];
            } else {
                problemTags = problemTags.split(',');
            }
            return JSON.stringify(problemTags);
        }
    </script>
    <script type="text/javascript">
        function editProblem(problemId, problemName, timeLimit, memoryLimit, description, hint, inputFormat, outputFormat, 
                    inputSample, outputSample, testCases, problemCategories, problemTags, isPublic, isExactlyMatch) {
            var postData = {
                'problemId': problemId,
                'problemName': problemName,
                'timeLimit': timeLimit,
                'memoryLimit': memoryLimit,
                'description': description,
                'hint': hint,
                'inputFormat': inputFormat,
                'outputFormat': outputFormat,
                'inputSample': inputSample,
                'outputSample': outputSample,
                'testCases': testCases,
                'problemCategories': problemCategories,
                'problemTags': problemTags,
                'isPublic': isPublic,
                'isExactlyMatch': isExactlyMatch
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/editProblem.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processEditProblemResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processEditProblemResult(result) {
            if ( result['isSuccessful'] ) {
                $('.alert-error').addClass('hide');
                $('.alert-success').removeClass('hide');
            } else {
                var errorMessage  = '';

                if ( !result['isProblemExists'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.problem-not-exist" text="Problem not exists." /><br>';
                } 
                if ( result['isProblemNameEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.problem-name-empty" text="You can&acute;t leave Problem Name empty." /><br>';
                } else if ( !result['isProblemNameLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.problem-name-illegal" text="The length of Problem Name CANNOT exceed 128 characters." /><br>';
                }
                if ( !result['isTimeLimitLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.time-limit-illegal" text="The Time Limit should be an integer greater than 0." /><br>';
                } 
                if ( !result['isMemoryLimitLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.memory-limit-illegal" text="The Memory Limit should be an integer greater than 0." /><br>';
                } 
                if ( result['isDescriptionEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.description-empty" text="You can&acute;t leave Description empty." /><br>';
                }
                if ( result['isInputFormatEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.input-format-empty" text="You can&acute;t leave Input Format empty." /><br>';
                } 
                if ( result['isOutputFormatEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.output-format-empty" text="You can&acute;t leave Output Format empty." /><br>';
                } 
                if ( result['isInputSampleEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.input-sample-empty" text="You can&acute;t leave Input Sample empty." /><br>';
                } 
                if ( result['isOutputSampleEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.edit-problem.output-sample-empty" text="You can&acute;t leave Output Sample empty." /><br>';
                } 

                $('.alert-error', '#problem-form').html(errorMessage);
                $('.alert-error', '#problem-form').removeClass('hide');
            }
            $('button[type=submit]', '#problem-form').removeAttr('disabled');
            $('button[type=submit]', '#problem-form').html('<spring:message code="voj.administration.edit-problem.update-problem" text="Update" />');
        }
    </script>
</body>
</html>