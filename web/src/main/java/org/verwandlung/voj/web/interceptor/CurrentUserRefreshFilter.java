/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.web.interceptor;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.LogSanitizer;

/**
 * Re-validates the logged-in user on every request.
 *
 * <p>The login caches the {@link User} (and the role derived from its user group) in the security
 * context stored in the HTTP session, so banning, demoting or deleting a user used to take effect
 * only after that user logged out. This filter runs inside the Spring Security chain, before the
 * authorization checks, and reloads the user by its primary key:
 *
 * <ul>
 *   <li>a deleted user, or one whose group may no longer log in (e.g. banned to {@code forbidden}),
 *       is logged out: the security context is cleared and the session invalidated, and the request
 *       continues anonymously;
 *   <li>otherwise the fresh user replaces the cached one for this request, so the authorities and
 *       {@code HttpSessionParser.getCurrentUser()} always reflect the database; when the user group
 *       changed, the refreshed context is also written back to the session.
 * </ul>
 *
 * <p>It is not a Spring bean on purpose (that would also register it in the servlet container's
 * filter chain); SecurityConfig adds it to the security filter chain.
 *
 * @author Haozhe Xie
 */
public class CurrentUserRefreshFilter extends OncePerRequestFilter {
  /**
   * Creates the filter.
   *
   * @param userService - the service used to reload the logged-in user
   */
  public CurrentUserRefreshFilter(UserService userService) {
    this.userService = userService;
  }

  /* (non-Javadoc)
   * @see org.springframework.web.filter.OncePerRequestFilter#shouldNotFilter
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Static assets neither depend on nor reveal the user; skip the lookup for them.
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return path.startsWith("/assets/") || path.equals("/favicon.ico");
  }

  /* (non-Javadoc)
   * @see org.springframework.web.filter.OncePerRequestFilter#doFilterInternal
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof VojUserDetails) {
      refreshCurrentUser(
          ((VojUserDetails) authentication.getPrincipal()).getUser(),
          authentication,
          request,
          response);
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Reloads the cached user and either refreshes or terminates the authenticated session.
   *
   * @param cachedUser - the user cached in the security context at login
   * @param authentication - the current authentication
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   */
  private void refreshCurrentUser(
      User cachedUser,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    User user = userService.getUserUsingUid(cachedUser.getUid());
    if (user == null || !userService.isAllowedToAccess(user)) {
      LOGGER.info(
          "{} is no longer allowed to log in; terminating the session",
          LogSanitizer.forLog(cachedUser));
      new SecurityContextLogoutHandler().logout(request, response, authentication);
      return;
    }

    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, null, userDetails.getAuthorities()));
    SecurityContextHolder.setContext(securityContext);
    if (!Objects.equals(getUserGroupSlug(cachedUser), getUserGroupSlug(user))) {
      securityContextRepository.saveContext(securityContext, request, response);
    }
  }

  /**
   * Gets the slug of the user's group.
   *
   * @param user - the user
   * @return the slug of the user's group, or null if the user has no group
   */
  private static String getUserGroupSlug(User user) {
    UserGroup userGroup = user.getUserGroup();
    return userGroup == null ? null : userGroup.getUserGroupSlug();
  }

  /** The service used to reload the logged-in user. */
  private final UserService userService;

  /** Writes a refreshed security context back to the HTTP session. */
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(CurrentUserRefreshFilter.class);
}
