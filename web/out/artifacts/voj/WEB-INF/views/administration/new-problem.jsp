<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.new-problem.title" text="New Problem" /> | ${websiteName}</title>
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
                <h2 class="page-header"><i class="fa fa-file"></i> <spring:message code="voj.administration.new-problem.new-problem" text="New Problem" /></h2>
                <form id="problem-form" onSubmit="onSubmit(); return false;">
                    <div class="row-fluid">
                        <div class="span8">
                            <div class="alert alert-error hide"></div> <!-- .alert-error -->
                            <div class="control-group row-fluid">
                                <label for="problem-name"><spring:message code="voj.administration.new-problem.problem-name" text="Problem Name" /></label>
                                <input id="problem-name" class="span12" type="text" maxlength="128" />
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="time-limit"><spring:message code="voj.administration.new-problem.time-limit" text="Time Limit" /> (ms)</label>
                                <input id="time-limit" class="span12" type="text" maxlength="8" />
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="memory-limit"><spring:message code="voj.administration.new-problem.memory-limit" text="Memory Limit" /> (KB)</label>
                                <input id="memory-limit" class="span12" type="text" maxlength="8" />
                            </div> <!-- .control-group -->
                            <div class="row-fluid">
                                <div class="span12">
                                    <label for="wmd-input"><spring:message code="voj.administration.new-problem.problem-description" text="Description" /></label>    
                                    <div id="markdown-editor">
                                        <div class="wmd-panel">
                                            <div id="wmd-button-bar" class="wmd-button-bar"></div> <!-- #wmd-button-bar -->
                                            <textarea id="wmd-input" class="wmd-input"></textarea>
                                        </div> <!-- .wmd-panel -->
                                        <div id="wmd-preview" class="wmd-panel wmd-preview"></div> <!-- .wmd-preview -->
                                    </div> <!-- #markdown-editor -->
                                </div> <!-- .span12 -->
                            </div> <!-- .row-fluid -->
                            <div class="control-group row-fluid">
                                <label for="hint"><spring:message code="voj.administration.new-problem.hint" text="Hint" /></label>
                                <textarea id="hint" class="span12"></textarea>
                            </div> <!-- .control-group -->
                            <h4><spring:message code="voj.administration.new-problem.input-output" text="Input / Output" /></h4>
                            <div class="control-group row-fluid">
                                <label for="input-format"><spring:message code="voj.administration.new-problem.input-format" text="Input Format" /></label>
                                <textarea id="input-format" class="span12"></textarea>
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="output-format"><spring:message code="voj.administration.new-problem.output-format" text="Output Format" /></label>
                                <textarea id="output-format" class="span12"></textarea>
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="input-sample"><spring:message code="voj.administration.new-problem.input-sample" text="Input Sample" /></label>
                                <textarea id="input-sample" class="span12"></textarea>
                            </div> <!-- .control-group -->
                            <div class="control-group row-fluid">
                                <label for="output-sample"><spring:message code="voj.administration.new-problem.output-sample" text="Output Sample" /></label>
                                <textarea id="output-sample" class="span12"></textarea>
                            </div> <!-- .control-group -->
                            <div class="row-fluid">
                                <div class="span6">
                                    <h4><spring:message code="voj.administration.new-problem.test-cases" text="Test Cases" /></h4>
                                </div> <!-- .span6 -->
                                <div class="span6 text-right">
                                    <a id="new-test-case" title="<spring:message code="voj.administration.new-problem.new-test-case" text="New test case" />" href="javascript:void(0);">
                                        <i class="fa fa-plus-circle"></i>
                                    </a>
                                </div> <!-- .span6 -->
                            </div> <!-- .row-fluid -->
                            <div class="row-fluid">
                                <div class="span12">
                                    <div id="test-cases">
                                        <p id="no-test-cases"><spring:message code="voj.administration.new-problem.no-test-cases" text="No Test Cases." /></p>
                                        <ul></ul>
                                    </div> <!-- #test-cases -->
                                </div> <!-- .span12 -->
                            </div> <!-- .row-fluid -->
                        </div> <!-- .span8 -->
                        <div class="span4">
                            <div class="section">
                                <div class="header">
                                    <h5><spring:message code="voj.administration.new-problem.create-problem" text="Create Problem" /></h5>
                                </div> <!-- .header -->
                                <div class="body">
                                    <div class="row-fluid">
                                        <div class="span8">
                                            <spring:message code="voj.administration.new-problem.is-public" text="Public to Users?" />
                                        </div> <!--- .span8 -->
                                        <div class="span4 text-right">
                                            <input id="problem-is-public" type="checkbox" data-toggle="switch" checked="checked" />
                                        </div> <!-- .span4 -->
                                    </div> <!-- .row-fluid -->
                                    <div class="row-fluid">
                                        <div class="span8">
                                            <spring:message code="voj.administration.new-problem.test-case-exactly-match" text="Test Case Exactly Match" />
                                        </div> <!--- .span8 -->
                                        <div class="span4 text-right">
                                            <input id="problem-is-exactly-match" type="checkbox" data-toggle="switch" />
                                        </div> <!-- .span4 -->
                                    </div> <!-- .row-fluid -->
                                </div> <!-- .body -->
                                <div class="footer text-right">
                                    <button class="btn btn-primary" type="submit"><spring:message code="voj.administration.new-problem.publish-problem" text="Publish" /></button>
                                </div> <!-- .footer -->
                            </div> <!-- .section -->
                            <div class="section">
                                <div class="header">
                                    <h5><spring:message code="voj.administration.new-problem.problem-categories" text="Categories" /></h5>
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
                                    <h5><spring:message code="voj.administration.new-problem.problem-tags" text="Tags" /></h5>
                                </div> <!-- .header -->
                                <div class="body">
                                    <input id="problem-tags" class="tagsinput" type="hidden" value="" />
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
        function getTestCaseContainer(testCaseId) {
            var containerTemplate = '<li class="test-case">' +
                                    '    <div class="header">' +
                                    '        <h5><spring:message code="voj.administration.new-problem.test-case" text="Test Case" /> #%s</h5>' +
                                    '        <ul class="inline">' +
                                    '            <li><a href="javascript:void(0);"><i class="fa fa-edit"></i></a></li>' +
                                    '            <li><a href="javascript:void(0);"><i class="fa fa-trash"></i></a></li>' +
                                    '        </ul>' +
                                    '    </div> <!-- .header -->' +
                                    '    <div class="body">' +
                                    '        <div class="row-fluid">' +
                                    '            <div class="span4">' +
                                    '                <label><spring:message code="voj.administration.new-problem.standard-input" text="Standard Input" /></label>' +
                                    '            </div> <!-- .span4 -->' +
                                    '            <div class="span8">' +
                                    '                <textarea class="standard-input span12"></textarea>' + 
                                    '            </div> <!-- .span8 -->' +
                                    '        </div> <!-- .row-fluid -->' +
                                    '        <div class="row-fluid">' +
                                    '            <div class="span4">' +
                                    '                <label><spring:message code="voj.administration.new-problem.standard-output" text="Standard Output" /></label>' +
                                    '            </div> <!-- .span4 -->' +
                                    '            <div class="span8">' +
                                    '                <textarea class="standard-output span12"></textarea>' + 
                                    '            </div> <!-- .span8 -->' +
                                    '        </div> <!-- .row-fluid -->' +
                                    '    </div> <!-- .body -->' +
                                    '</li> <!-- .test-case -->';

            return containerTemplate.format(testCaseId);
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
                testCaseName      = '<spring:message code="voj.administration.new-problem.test-case" text="Test Case" /> #%s';

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
            var problemName         = $('#problem-name').val(),
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
            $('button[type=submit]', '#problem-form').html('<spring:message code="voj.administration.new-problem.please-wait" text="Please wait..." />');

            return createProblem(problemName, timeLimit, memoryLimit, description, hint, 
                    inputFormat, outputFormat, inputSample, outputSample, testCases, 
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
        function createProblem(problemName, timeLimit, memoryLimit, description, hint, inputFormat, outputFormat, 
                    inputSample, outputSample, testCases, problemCategories, problemTags, isPublic, isExactlyMatch) {
            var postData = {
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
                url: '<c:url value="/administration/createProblem.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    return processCreateProblemResult(result);
                }
            });
        }
    </script>
    <script type="text/javascript">
        function processCreateProblemResult(result) {
            if ( result['isSuccessful'] ) {
                var problemId = result['problemId'];
                window.location.href = '<c:url value="/administration/edit-problem/" />' + problemId;
            } else {
                var errorMessage  = '';

                if ( result['isProblemNameEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.problem-name-empty" text="You can&acute;t leave Problem Name empty." /><br>';
                } else if ( !result['isProblemNameLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.problem-name-illegal" text="The length of Problem Name CANNOT exceed 128 characters." /><br>';
                }
                if ( !result['isTimeLimitLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.time-limit-illegal" text="The Time Limit should be an integer greater than 0." /><br>';
                } 
                if ( !result['isMemoryLimitLegal'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.memory-limit-illegal" text="The Memory Limit should be an integer greater than 0." /><br>';
                } 
                if ( result['isDescriptionEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.description-empty" text="You can&acute;t leave Description empty." /><br>';
                }
                if ( result['isInputFormatEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.input-format-empty" text="You can&acute;t leave Input Format empty." /><br>';
                } 
                if ( result['isOutputFormatEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.output-format-empty" text="You can&acute;t leave Output Format empty." /><br>';
                } 
                if ( result['isInputSampleEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.input-sample-empty" text="You can&acute;t leave Input Sample empty." /><br>';
                } 
                if ( result['isOutputSampleEmpty'] ) {
                    errorMessage += '<spring:message code="voj.administration.new-problem.output-sample-empty" text="You can&acute;t leave Output Sample empty." /><br>';
                } 

                $('.alert-error', '#problem-form').html(errorMessage);
                $('.alert-error', '#problem-form').removeClass('hide');
            }
            $('button[type=submit]', '#problem-form').removeAttr('disabled');
            $('button[type=submit]', '#problem-form').html('<spring:message code="voj.administration.new-problem.publish-problem" text="Publish" />');
        }
    </script>
</body>
</html>