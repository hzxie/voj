        <%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
        <div id="sidebar">
            <div id="logo">
                <a href="<c:url value="/" />">
                    <img src="${cdnUrl}/img/logo-light.png" alt="Logo" />
                </a>
            </div> <!-- #logo -->
            <div id="sidebar-user">
                <div class="row-fluid">
                    <div class="span3">
                        <img src="${cdnUrl}/img/avatar.jpg" alt="avatar">
                    </div> <!-- .span3 -->
                    <div class="offset1 span8">
                        <spring:message code="voj.administration.include.sidebar.welcome-back" text="Welcome back," /> <br>${myProfile.username} <br>
                        <span class="label label-success"><spring:message code="voj.administration.include.sidebar.online" text="Online" /></span>
                    </div> <!-- .span8 -->
                </div> <!-- .row-fluid -->
            </div> <!-- #sidebar-user -->
            <div id="sidebar-nav">
                <ul class="nav">
                    <li class="nav-item primary-nav-item">
                        <a href="<c:url value="/administration" />"><i class="fa fa-dashboard"></i> <spring:message code="voj.administration.include.sidebar.dashboard" text="Dashboard" /></a>
                    </li>
                    <li class="nav-item primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-users"></i> <spring:message code="voj.administration.include.sidebar.users" text="Users" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/all-users" />"><spring:message code="voj.administration.include.sidebar.all-users" text="All Users" /></a></li>
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/new-user" />"><spring:message code="voj.administration.include.sidebar.new-user" text="New User" /></a></li>
                            <li class="nav-item secondary-nav-item hide"><a href="<c:url value="/administration/edit-user" />"><spring:message code="voj.administration.include.sidebar.edit-user" text="Edit User" /></a></li>
                        </ul>
                    </li>
                    <li class="nav-item primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-question-circle"></i> <spring:message code="voj.administration.include.sidebar.problems" text="Problems" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/all-problems" />"><spring:message code="voj.administration.include.sidebar.all-problems" text="All Problems" /></a></li>
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/new-problem" />"><spring:message code="voj.administration.include.sidebar.new-problem" text="New Problem" /></a></li>
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/problem-categories" />"><spring:message code="voj.administration.include.sidebar.problem-categories" text="Categories" /></a></li>
                            <li class="nav-item secondary-nav-item hide"><a href="<c:url value="/administration/edit-problem" />"><spring:message code="voj.administration.include.sidebar.edit-problem" text="Edit Problem" /></a></li>
                        </ul>
                    </li>
                    <li class="nav-item primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-comment"></i> <spring:message code="voj.administration.include.sidebar.discussion" text="Discussion" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/all-discussion" />"><spring:message code="voj.administration.include.sidebar.all-discussion" text="All Discussion" /></a></li>
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/discussion-topics" />"><spring:message code="voj.administration.include.sidebar.discussion-topics" text="Topics" /></a></li>
                        </ul>
                    </li>
                    <li class="nav-item primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-paperclip"></i> <spring:message code="voj.administration.include.sidebar.contests" text="Contests" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/all-contests" />"><spring:message code="voj.administration.include.sidebar.all-contests" text="All Contests" /></a></li>
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/new-contest" />"><spring:message code="voj.administration.include.sidebar.new-contest" text="New Contest" /></a></li>
                            <li class="nav-item secondary-nav-item hide"><a href="<c:url value="/administration/edit-contest" />"><spring:message code="voj.administration.include.sidebar.edit-contest" text="Edit Contest" /></a></li>
                        </ul>
                    </li>
                    <li class="nav-item primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-code"></i> <spring:message code="voj.administration.include.sidebar.submissions" text="Submissions" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/all-submissions" />"><spring:message code="voj.administration.include.sidebar.all-submissions" text="All Submissions" /></a></li>
                            <li class="nav-item secondary-nav-item hide"><a href="<c:url value="/administration/edit-submission" />"><spring:message code="voj.administration.include.sidebar.edit-submission" text="Edit Submission" /></a></li>
                        </ul>
                    </li>
                    <li class="nav-item primary-nav-item nav-item-has-children">
                        <a href="javascript:void(0);"><i class="fa fa-cogs"></i> <spring:message code="voj.administration.include.sidebar.settings" text="Settings" /> <i class="fa fa-caret-right"></i></a>
                        <ul class="sub-nav nav">
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/general-settings" />"><spring:message code="voj.administration.include.sidebar.general-settings" text="General" /></a></li>
                            <li class="nav-item secondary-nav-item"><a href="<c:url value="/administration/language-settings" />"><spring:message code="voj.administration.include.sidebar.language-settings" text="Languages" /></a></li>
                        </ul>
                    </li>
                </ul>
            </div> <!-- #sidebar-nav -->
        </div> <!-- #sidebar -->