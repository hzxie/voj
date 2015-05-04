<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('cdn.url')" var="cdnUrl" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title>P${problem.problemId} ${problem.problemName} | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge, chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link href="${cdnUrl}/img/favicon.ico" rel="shortcut icon">
    <!-- StyleSheets -->
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/bootstrap-responsive.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/flat-ui.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/codemirror.min.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/style.css" />
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/problems/problem.css" />
    <!-- JavaScript -->
    <script type="text/javascript" src="${cdnUrl}/js/jquery-1.11.1.min.js"></script>
    <script type="text/javascript" src="${cdnUrl}/js/bootstrap.min.js"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="${cdnUrl}/js/jquery.placeholder.min.js"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/font-awesome-ie7.min.css" />
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='../not-supported';
        </script>
    <![endif]-->
</head>
<body>
    <!-- Header -->
    <%@ include file="/WEB-INF/views/include/header.jsp" %>
    <!-- Content -->
    <div id="content" class="container">
        <div class="row-fluid">
            <div id="main-content" class="span9">
                <div class="problem">
                    <div class="header">
                    <c:if test="${isLogin}">
                        <span class="pull-right">Accepted</span>
                    </c:if>
                        <span class="name">P${problem.problemId} ${problem.problemName}</span>
                    </div> <!-- .header -->
                    <div class="body">
                        <div class="section">
                            <h4>Description</h4>
                            <div class="description markdown">${problem.description}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4>Format</h4>
                            <h5>Input</h5>
                            <div class="description">${problem.inputFormat}</div> <!-- .description -->
                            <h5>Output</h5>
                            <div class="description">${problem.outputFormat}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <div id="io-sample" class="section">
                            <h4>Samples</h4>
                            <h5>Sample Input</h5>
                            <div class="description"><pre>${problem.sampleInput}</pre></div> <!-- .description -->
                            <h5>Sample Output</h5>
                            <div class="description"><pre>${problem.sampleOutput}</pre></div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4>Restrictions</h4>
                            <div class="description">
                                <p><strong>Time Limit: </strong>${problem.timeLimit} ms</p>
                                <p><strong>Memory Limit: </strong>${problem.memoryLimit} KB</p>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${problem.hint != null}">
                        <div class="section">
                            <h4>Hint</h4>
                            <div class="description markdown">${problem.hint}</div> <!-- .description -->
                        </div> <!-- .section -->
                        </c:if>
                        <form id="code-editor" onsubmit="onSubmit(); return false;" method="POST">
                            <textarea name="codemirror-editor" id="codemirror-editor"></textarea>
                            <div class="row-fluid">
                                <div class="span4">
                                    <select id="languages">
                                        <option value="text/x-csrc">C</option>
                                        <option value="text/x-c++src">C++</option>
                                        <option value="text/x-java">Java</option>
                                        <option value="text/x-pascal">Pascal</option>
                                        <option value="text/x-python">Python</option>
                                        <option value="text/x-ruby">Ruby</option>
                                    </select>
                                </div> <!-- .span4 -->
                                <div class="span8 text-right">
                                    <button type="submit" class="btn btn-primary">Submit</button>
                                    <button id="close-submission" class="btn">Cancel</button>
                                </div> <!-- .span8 -->
                            </div> <!-- .row-fluid -->
                        </form> <!-- #code-editor-->
                        <div id="mask" class="hide"></div> <!-- #mask -->
                    </div> <!-- .body -->
                </div> <!-- .problem -->
            </div> <!-- #main-content -->
            <div id="sidebar" class="span3">
                <div id="actions" class="section">
                    <h5>Actions</h5>
                    <ul>
                    <c:if test="${isLogin}">
                        <li><a id="submit-solution" href="javascript:void(0);">Submit Solution</a></li>
                    </c:if>
                        <li><a href="#">View Solution</a></li>
                        <li><a href="#">View Submission</a></li>
                    </ul>
                </div> <!-- #actions -->
                <div id="submission" class="section">
                    <h5>Submission</h5>
                </div> <!-- submission -->
                <div id="discussion" class="section">
                    <h5>Discussion</h5>
                </div> <!-- discussion -->
            </div> <!-- #sidebar -->
        </div> <!-- .row-fluid -->
    </div> <!-- #content -->
    <!-- Footer -->
    <%@ include file="/WEB-INF/views/include/footer.jsp" %>
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="${cdnUrl}/js/site.js"></script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/markdown.min.js', function() {
            converter = Markdown.getSanitizingConverter();
            converter.hooks.chain("preBlockGamut", function (text, rbg) {
                return text.replace(/^ {0,3}""" *\\n((?:.*?\\n)+?) {0,3}""" *$/gm, function (whole, inner) {
                    return "<blockquote>" + rbg(inner) + "</blockquote>\\n";
                });
            });

            $('.markdown').each(function() {
                var plainContent    = $(this).html(),
                    markdownContent = converter.makeHtml(plainContent.replace(/\\\n/g, '\\n'));
                
                $(this).html(markdownContent);
            });
        });
    </script>
    <script type="text/javascript">
        $.getScript('${cdnUrl}/js/codemirror.min.js', function() {
           $.when(
                $.getScript('${cdnUrl}/mode/clike.min.js'),
                $.getScript('${cdnUrl}/mode/pascal.min.js'),
                $.getScript('${cdnUrl}/mode/python.min.js'),
                $.getScript('${cdnUrl}/mode/ruby.min.js'),
                $.Deferred(function(deferred) {
                    $(deferred.resolve);
                })
            ).done(function() {
                window.codeMirrorEditor = CodeMirror.fromTextArea(document.getElementById('codemirror-editor'), {
                    mode: $('select#languages').val(),
                    tabMode: 'indent',
                    theme: 'neat',
                    tabSize: 4,
                    indentUnit: 4,
                    lineNumbers: true,
                    lineWrapping: true
                });
            }); 
        });
    </script>
    <script type="text/javascript">
        $('select#languages').change(function() {
            window.codeMirrorEditor.setOption('mode', $(this).val());
        });
    </script>
    <script type="text/javascript">
        $('#submit-solution').click(function() {
            $('#mask').removeClass('hide');
            $('#code-editor').addClass('fade');
        });
    </script>
    <script type="text/javascript">
        $('#close-submission').click(function() {
            $('#code-editor').removeClass('fade');
            $('#mask').addClass('hide');
        });
    </script>
    <script type="text/javascript">
        function onSubmit() {
            var code = window.codeMirrorEditor.getValue();
        }
    </script>
</body>
</html>