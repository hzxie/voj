<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.new-problem.title" text="New Problem" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/new-problem.css" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/flat-ui.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/md5.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/pace.min.js"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js"></script>
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
                <form id="problem-form" onSubmit="onSubimt(); return false;">
                    <div class="row-fluid">
                        <div class="span8">
                            <div class="alert alert-error hide"></div> <!-- .alert-error -->
                            <div class="alert alert-success hide"><spring:message code="voj.administration.new-problem.problem-created" text="The problem has been created successfully." /></div> <!-- .alert-success -->    
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
                                            <div id="wmd-button-bar"></div> <!-- #wmd-button-bar -->
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
                                            <input id="problem-is-exactly-match" type="checkbox" data-toggle="switch" checked="checked" />
                                        </div> <!-- .span4 -->
                                    </div> <!-- .row-fluid -->
                                </div> <!-- .body -->
                                <div class="footer text-right">
                                    <button class="btn btn-primary"><spring:message code="voj.administration.new-problem.create-problem" text="Create Problem" /></button>
                                </div> <!-- .footer -->
                            </div> <!-- .section -->
                            <div class="section">
                                <div class="header">
                                    <h5><spring:message code="voj.administration.new-problem.problem-categories" text="Categories" /></h5>
                                </div> <!-- .header -->
                                <div class="body">
                                    <c:forEach var="entry" items="${problemCategories}">
                                    <ul>                                        
                                        <li>
                                            <label class="checkbox" for="${entry.key.problemCategorySlug}">
                                                <input id="${entry.key.problemCategorySlug}" type="checkbox" data-toggle="checkbox"> ${entry.key.problemCategoryName}
                                            </label>
                                            <ul class="sub-categories">
                                            <c:forEach var="problemCategory" items="${entry.value}">
                                                <li>
                                                    <label class="checkbox" for="${problemCategory.problemCategorySlug}">
                                                        <input id="${entry.key.problemCategorySlug}" type="checkbox" data-toggle="checkbox"> ${problemCategory.problemCategoryName}
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
                                    <input id="sensitive-words" class="tagsinput" type="hidden" value="" />
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
            var problemTags         = JSON.parse('[]'),
                problemTagsValue    = '';
            for ( var i = 0; i < problemTags.length; ++ i ) {
                problemTagsValue += problemTags[i] + ',';
            }
            
            $('#offensive-words').val(problemTagsValue);
            $('.tagsinput').tagsInput();
            $('[data-toggle=switch]').wrap('<div class="switch" />').parent().bootstrapSwitch();
        });
    </script>
    <script type='text/x-mathjax-config'>
        MathJax.Hub.Config({
            tex2jax: {
                inlineMath: [
                    ['$','$'], 
                    ['\\(','\\)']
                ]
            }
        });
    </script>
    <script type='text/javascript'>
        $.getScript('https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML', function() {
            MathJax.Hub.Register.StartupHook('End', function () {
                $.getScript('${cdnUrl}/js/markdown.min.js', function() {
                    converter = Markdown.getSanitizingConverter();
                    editor    = new Markdown.Editor(converter);
                    mjpd      = new MJPD();
                    
                    mjpd.Editing.prepareWmdForMathJax(editor, '', [['$', '$']]);
                    editor.run();

                    $('.markdown').each(function() {
                        var plainContent    = $(this).text(),
                            markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                        
                        $(this).html(markdownContent);
                    });
                });
            });
        });
    </script>
    <script type="text/javascript">
        $('#new-test-case').click(function() {
            var testCaseId = $('li.test-case', '#test-cases').length; 
            
            $('#no-test-cases').addClass('hide');
            $('#test-cases > ul').append(getSocialLinkContainer(testCaseId));
        });
    </script>
    <script type="text/javascript">
        function getSocialLinkContainer(testCaseId) {
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
                                    '</li> <!-- .social-link -->';

            return containerTemplate.format(testCaseId);
        }
    </script>
    <script type="text/javascript">
        $('#test-cases').delegate('i.fa-edit', 'click', function() {
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
        $('#test-cases').delegate('i.fa-trash', 'click',function() {
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
</body>
</html>