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
import org.springframework.web.servlet.ModelAndView;

/**
 * Decorates every rendered page with the shared model attributes via {@link CommonModelPopulator}.
 *
 * <p>Replaces the {@code @Around} ViewAspect: instead of an AspectJ pointcut that relied on the
 * {@code *View} naming convention and the {@code (.., request, response)} argument order, this hooks
 * the first-class Spring MVC extension point and decorates any handler that produced a
 * {@link ModelAndView}.
 *
 * @author Haozhe Xie
 */
@Component
public class CommonModelInterceptor implements HandlerInterceptor {
  /* (non-Javadoc)
   * @see org.springframework.web.servlet.HandlerInterceptor#postHandle
   */
  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    // @ResponseBody / JSON action endpoints produce no ModelAndView; nothing to decorate.
    if (modelAndView == null) {
      return;
    }
    // Redirects render no template; merging attributes here would leak them onto
    // the redirect URL as query parameters via RedirectView.
    String viewName = modelAndView.getViewName();
    if (viewName != null && viewName.startsWith("redirect:")) {
      return;
    }
    commonModelPopulator.populate(modelAndView, request, response);
  }

  /** The autowired CommonModelPopulator object. */
  @Autowired private CommonModelPopulator commonModelPopulator;
}
