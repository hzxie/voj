<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <title>P${problem.getProblemID()} ${problem.getProblemName()} | Verwandlung Online Judge</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="author" content="谢浩哲">
    <!-- Icon -->
    <link rel="shortcut icon" href="<c:url value="/assets/img/favicon.png" />">
    <!-- CSS -->
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/bootstrap.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/bootstrap-responsive.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/flat-ui.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/font-awesome.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/style.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/problem.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/codemirror.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/codemirror-neat.css" />">
    <!-- JavaScript -->
    <script type="text/javascript" src="<c:url value="/assets/js/jquery-1.11.1.min.js" />"></script>
    <!--[if lte IE 9]>
        <script type="text/javascript" src="<c:url value="/assets/js/jquery.placeholder.js" />"></script>
    <![endif]-->
    <!--[if lte IE 7]>
        <link rel="stylesheet" type="text/css" href="<c:url value="/assets/css/font-awesome-ie7.min.css" />">
        <script type="text/javascript" sec="<c:url value="/assets/js/icon-font-ie7.js" />"></script>
    <![endif]-->
    <!--[if lte IE 6]>
        <script type="text/javascript"> 
            window.location.href='../not-supported';
        </script>
    <![endif]-->
</head>
<body>
    <div id="header" class="row-fluid">
        <div class="container">
            <div id="logo" class="span6">
                <a href="<c:url value="/" />">
                    <img src="<c:url value="/assets/img/logo.png" />" alt="Logo">
                </a>
            </div> <!-- #logo -->
            <div id="nav" class="span6">
                <ul class="inline">
                    <li><a href="<c:url value="/p" />">Problems</a></li>
                    <li><a href="<c:url value="/discussion" />">Discussion</a></li>
                    <li><a href="<c:url value="/competition" />">Competition</a></li>
                    <li><a href="<c:url value="/submission" />">Submission</a></li>
                    <li><a href="javascript:void(0)">More</a></li>
                </ul>
            </div> <!-- #nav -->
        </div> <!-- .container -->
    </div> <!-- #header -->
    <div id="content" class="container">
        <div class="row-fluid">
            <div id="main-content" class="span9">
                <div class="problem">
                    <div class="header">
                        <span class="pull-right">Accepted</span>
                        <span class="name">P${problem.getProblemID()} ${problem.getProblemName()}</span>
                    </div> <!-- .header -->
                    <div class="body">
                        <div class="section">
                            <h4>Description</h4>
                            <div class="description">${problem.getDescription()}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4>Format</h4>
                            <h5>Input</h5>
                            <div class="description">${problem.getInputFormat()}</div> <!-- .description -->
                            <h5>Output</h5>
                            <div class="description">${problem.getOutputFormat()}</div> <!-- .description -->
                        </div> <!-- .section -->
                        <div id="io-sample" class="section">
                            <h4>Samples</h4>
                            <h5>Sample Input</h5>
                            <div class="description"><pre>${problem.getSampleInput()}</pre></div> <!-- .description -->
                            <h5>Sample Output</h5>
                            <div class="description"><pre>${problem.getSampleOutput()}</pre></div> <!-- .description -->
                        </div> <!-- .section -->
                        <div class="section">
                            <h4>Restrictions</h4>
                            <div class="description">
                                <p><strong>Time Limit: </strong>${problem.getTimeLimit()} ms</p>
                                <p><strong>Memory Limit: </strong>${problem.getMemoryLimit()} KB</p>
                            </div> <!-- .description -->
                        </div> <!-- .section -->
                        <c:if test="${problem.getHint() != null}">
                        <div class="section">
                            <h4>Hint</h4>
                            <div class="description">${problem.getHint()}</div> <!-- .description -->
                        </div> <!-- .section -->
                        </c:if>
                        <form id="code-editor" class="hide" onsubmit="onSubmit(); return false;" method="POST">
                            <textarea name="codemirror-editor" id="codemirror-editor"></textarea>
                            <div class="row-fluid">
                                <div class="span4">
                                    <select id="languages">
                                        <option value="text/x-c">C</option>
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
                        <li><a id="submit-solution" href="javascript:void(0);">Submit Solution</a></li>
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
    <div id="footer">
        <div class="container">
            <p id="copyright">Copyright&copy; 2005-2014 <a href="http://www.zjhzxhz.com/" target="_blank">HApPy Studio</a>. All rights reserved.</p>
        </div> <!-- .container -->
    </div> <!-- #footer -->
    <!-- JavaScript -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script type="text/javascript" src="<c:url value="/assets/js/codemirror.js" />"></script>
    <script type="text/javascript" src="<c:url value="/assets/mode/clike.js" />"></script>
    <script type="text/javascript" src="<c:url value="/assets/mode/pascal.js" />"></script>
    <script type="text/javascript" src="<c:url value="/assets/mode/python.js" />"></script>
    <script type="text/javascript" src="<c:url value="/assets/mode/ruby.js" />"></script>
    <script type="text/javascript">
        editor = CodeMirror.fromTextArea(document.getElementById("codemirror-editor"), {
            mode: $('select#languages').val(),
            tabMode: "indent",
            theme: "neat",
            indentUnit: 4,
            tabSize: 4,
            matchBrackets: true,
            lineNumbers: true,
            lineWrapping: true
        });
    </script>
    <script type="text/javascript">
        $('select#languages').change(function() {
            editor.setOption("mode", $(this).val());
        });
    </script>
    <script type="text/javascript">
        $('#submit-solution').click(function() {
            $('#mask').removeClass('hide');
            $('#code-editor').removeClass('hide');
        });
    </script>
    <script type="text/javascript">
        $('#close-submission').click(function() {
            $('#code-editor').addClass('hide');
            $('#mask').addClass('hide');
        });
    </script>
    <script type="text/javascript">
        function onSubmit() {

        }
    </script>
</body>
</html>