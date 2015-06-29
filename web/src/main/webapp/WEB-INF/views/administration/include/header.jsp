            <div id="header">
                <a id="sidebar-toggle" href="javascript:void(0);"><i class="fa fa-bars"></i></a>
                <ul class="nav inline pull-right">
                    <li><a href="javascript:void(0);"><i class="fa fa-tasks"></i></a></li>
                    <li><a href="javascript:void(0);"><i class="fa fa-bell"></i></a></li>
                    <li class="dropdown">
                        <a class="dropdown-toggle" data-toggle="dropdown" role="button" href="javascript:void(0);">
                            ${user.username} <img src="${cdnUrl}/img/avatar.jpg" alt="avatar">
                        </a>
                        <ul class="dropdown-menu" role="menu">
                            <li><a href="<c:url value="/accounts/login?logout=true" />"><i class="fa fa-sign-out"></i> <spring:message code="voj.administration.index.sign-out" text="Sign out" /></a></li>
                        </ul>
                    </li>
                </ul>
            </div> <!-- #header -->