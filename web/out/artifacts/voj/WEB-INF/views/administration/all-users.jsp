<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:eval expression="@propertyConfigurer.getProperty('url.cdn')" var="cdnUrl" />
<spring:eval expression="@propertyConfigurer.getProperty('build.version')" var="version" />
<!DOCTYPE html>
<html lang="${language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="voj.administration.all-users.title" text="All Users" /> | ${websiteName}</title>
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
    <link rel="stylesheet" type="text/css" href="${cdnUrl}/css/administration/all-users.css?v=${version}" />
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
                <h2 class="page-header"><i class="fa fa-users"></i> <spring:message code="voj.administration.all-users.all-users" text="All Users" /></h2>
                <div class="alert alert-error hide"></div> <!-- .alert-error -->
                <div id="filters" class="row-fluid">
                    <div class="span4">
                        <div class="row-fluid">
                            <div class="span8">
                                <select id="actions">
                                    <option value="delete"><spring:message code="voj.administration.all-users.delete" text="Delete" /></option>
                                </select>
                            </div> <!-- .span8 -->
                            <div class="span4">
                                <button class="btn btn-danger btn-block"><spring:message code="voj.administration.all-users.apply" text="Apply" /></button>
                            </div> <!-- .span4 -->
                        </div> <!-- .row-fluid -->
                    </div> <!-- .span4 -->
                    <div class="span8 text-right">
                        <form action="<c:url value="/administration/all-users" />" method="GET" class="row-fluid">
                            <div class="span4">
                                <select id="user-group" name="userGroup">
                                    <option value=""><spring:message code="voj.administration.all-users.all-user-groups" text="All User Groups" /></option>
                                <c:forEach var="userGroup" items="${userGroups}">
                                    <option value="${userGroup.userGroupSlug}" <c:if test="${userGroup.userGroupSlug == selectedUserGroup}">selected</c:if> >${userGroup.userGroupName}</option>
                                </c:forEach>
                                </select>
                            </div> <!-- .span4 -->
                            <div class="span5">
                                <div class="control-group">
                                    <input id="username" name="username" class="span12" type="text" value="${username}" placeholder="<spring:message code="voj.administration.all-users.username" text="Username" />" />
                                </div> <!-- .control-group -->
                            </div> <!-- .span5 -->
                            <div class="span3">
                                <button class="btn btn-primary btn-block"><spring:message code="voj.administration.all-users.filter" text="Filter" /></button>
                            </div> <!-- .span3 -->
                        </form> <!-- .row-fluid -->
                    </div> <!-- .span8 -->
                </div> <!-- .row-fluid -->
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th class="check-box">
                                <label class="checkbox" for="all-users">
                                    <input id="all-users" type="checkbox" data-toggle="checkbox">
                                </label>
                            </th>
                            <th class="username"><spring:message code="voj.administration.all-users.username" text="Username" /></th>
                            <th class="email"><spring:message code="voj.administration.all-users.email" text="Email" /></th>
                            <th class="user-group"><spring:message code="voj.administration.all-users.user-group" text="User Group" /></th>
                            <th class="prefer-language"><spring:message code="voj.administration.all-users.prefer-language" text="Prefer Language" /></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="user" items="${users}">
                        <tr data-value="${user.uid}">
                            <td class="check-box">
                                <label class="checkbox" for="user-${user.uid}">
                                    <input id="user-${user.uid}" type="checkbox" value="${user.uid}" data-toggle="checkbox" />
                                </label>
                            </td>
                            <td class="username">
                                <a href="<c:url value="/administration/edit-user/${user.uid}" />">${user.username}</a>
                            </td>
                            <td class="email">${user.email}</td>
                            <td class="user-group">${user.userGroup.userGroupName}</td>
                            <td class="prefer-language">${user.preferLanguage.languageName}</td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <div id="pagination" class="pagination pagination-centered">
                    <c:set var="lowerBound" value="${currentPage - 5 > 0 ? currentPage - 5 : 1}" />
                    <c:set var="upperBound" value="${currentPage + 5 < totalPages ? currentPage + 5 : totalPages}" />
                    <c:set var="baseUrl" value="/administration/all-users?userGroup=${selectedUserGroup}&username=${username}" />
                    <ul>
                        <li class="previous <c:if test="${currentPage <= 1}">disabled</c:if>">
                        <a href="
                        <c:choose>
                            <c:when test="${currentPage <= 1}">javascript:void(0);</c:when>
                            <c:otherwise><c:url value="${baseUrl}&page=${currentPage - 1}" /></c:otherwise>
                        </c:choose>
                        ">&lt;</a>
                        </li>
                        <c:forEach begin="${lowerBound}" end="${upperBound}" var="pageNumber">
                        <li <c:if test="${pageNumber == currentPage}">class="active"</c:if>><a href="<c:url value="${baseUrl}&page=${pageNumber}" />">${pageNumber}</a></li>
                        </c:forEach>
                        <li class="next <c:if test="${currentPage >= totalPages}">disabled</c:if>">
                        <a href="
                        <c:choose>
                            <c:when test="${currentPage >= totalPages}">javascript:void(0);</c:when>
                            <c:otherwise><c:url value="${baseUrl}&page=${currentPage + 1}" /></c:otherwise>
                        </c:choose>
                        ">&gt;</a>
                        </li>
                    </ul>
                </div> <!-- #pagination-->
            </div> <!-- #content -->
        </div> <!-- #container -->
    </div> <!-- #wrapper -->
    <!-- Java Script -->
    <!-- Placed at the end of the document so the pages load faster -->
    <%@ include file="/WEB-INF/views/administration/include/footer-script.jsp" %>
    <script type="text/javascript">
        $('label[for=all-users]').click(function() {
            // Fix the bug for Checkbox in FlatUI 
            var isChecked = false;
            setTimeout(function() {
                isChecked = $('label[for=all-users]').hasClass('checked');
                
                if ( isChecked ) {
                    $('label.checkbox').addClass('checked');
                } else {
                    $('label.checkbox').removeClass('checked');
                }
            }, 100);
        });
    </script>
    <script type="text/javascript">
        $('button.btn-danger', '#filters').click(function() {
            if ( !confirm('<spring:message code="voj.administration.all-users.continue-or-not" text="Are you sure to continue?" />') ) {
                return;
            }
            $('.alert-error').addClass('hide');
            $('button.btn-danger', '#filters').attr('disabled', 'disabled');
            $('button.btn-danger', '#filters').html('<spring:message code="voj.administration.all-users.please-wait" text="Please wait..." />');

            var users = [];
            $('label.checkbox', 'table tbody').each(function() {
                if ( $(this).hasClass('checked') ) {
                    var uid = $('input[type=checkbox]', $(this)).val();
                    users.push(uid);
                }
            });
            return doDeleteUsersAction(users);
        });
    </script>
    <script type="text/javascript">
        function doDeleteUsersAction(users) {
            var postData = {
                'users': JSON.stringify(users)
            };

            $.ajax({
                type: 'POST',
                url: '<c:url value="/administration/deleteUsers.action" />',
                data: postData,
                dataType: 'JSON',
                success: function(result){
                    if ( result['isSuccessful'] ) {
                        for ( var i = 0; i < users.length; ++ i ) {
                            $('tr[data-value=%s]'.format(users[i])).remove();
                        }
                    } else {
                        $('.alert').html('<spring:message code="voj.administration.all-users.delete-error" text="Some errors occurred while deleting users." />');
                        $('.alert').removeClass('hide');
                    }
                    $('button.btn-danger', '#filters').removeAttr('disabled');
                    $('button.btn-danger', '#filters').html('<spring:message code="voj.administration.all-users.apply" text="Apply" />');
                }
            });
        }
    </script>
</body>
</html>