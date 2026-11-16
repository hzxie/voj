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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import org.verwandlung.voj.web.model.PublicationStatus;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.util.HttpSessionParser;

/**
 * Takes the site offline for everyone except administrators while the {@code maintenanceMode} option
 * is enabled. Authentication and static assets stay reachable so administrators can still sign in
 * and the maintenance page can render; every other request is forwarded to the maintenance page with
 * an HTTP 503 status.
 *
 * @author Haozhe Xie
 */
@Component
public class MaintenanceModeInterceptor implements HandlerInterceptor {
  /* (non-Javadoc)
   * @see org.springframework.web.servlet.HandlerInterceptor#preHandle
   */
  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (!optionService.isOptionEnabled("maintenanceMode", false)) {
      return true;
    }

    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (isAllowedDuringMaintenance(path) || PublicationStatus.isAdministrator(HttpSessionParser.getCurrentUser())) {
      return true;
    }

    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    request.getRequestDispatcher("/maintenance").forward(request, response);
    return false;
  }

  /**
   * Checks whether the request path stays reachable during maintenance: the maintenance page itself,
   * authentication, static assets, and the informational pages still linked from the footer
   * (about / terms / privacy / help / judgers) so visitors can read them while the site is offline.
   * The judger API is machine-to-machine (judgers download checkpoints from it) and must keep
   * working, otherwise queued submissions cannot be judged during or after the maintenance window.
   *
   * @param path - the request path relative to the context root
   * @return whether the request is allowed during maintenance
   */
  private boolean isAllowedDuringMaintenance(String path) {
    return path.startsWith("/maintenance")
        || path.startsWith("/api/judger/")
        || path.startsWith("/accounts")
        || path.startsWith("/assets")
        || path.equals("/favicon.ico")
        || path.equals("/about")
        || path.equals("/terms")
        || path.equals("/privacy")
        || path.equals("/help")
        || path.equals("/judgers");
  }

  /** The autowired OptionService object. Used to resolve the maintenance-mode option. */
  @Autowired private OptionService optionService;
}
