            <%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
            <div id="header">
                <a id="sidebar-toggle" href="javascript:void(0);"><i class="fa fa-bars"></i></a>
                <ul class="nav inline">
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" role="button" href="javascript:void(0);">
                            ${myProfile.username} <img src="${cdnUrl}/img/avatar.jpg" alt="avatar">
                        </a>
                        <ul class="dropdown-menu" role="menu">
                            <li><a href="<c:url value="/worldwide?forward=${requestScope['javax.servlet.forward.request_uri']}" />"><i class="fa fa-language"></i> <spring:message code="voj.administration.include.header.change-language" text="Change Language" /></a></li>
                            <li class="divider"></li>
                            <li><a href="<c:url value="/accounts/login?logout=true" />"><i class="fa fa-sign-out"></i> <spring:message code="voj.administration.include.header.sign-out" text="Sign out" /></a></li>
                        </ul>
                    </li>
                </ul>
            </div> <!-- #header -->