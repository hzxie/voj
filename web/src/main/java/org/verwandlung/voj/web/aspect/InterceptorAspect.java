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
package org.verwandlung.voj.web.aspect;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.web.servlet.view.RedirectView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.UserService;

/**
 * The aspect of the interceptors. Used to perform the permission control of the system.
 *
 * @author Haozhe Xie
 */
@Aspect
@Component
public class InterceptorAspect {
  /**
   * The aspect of the dashboard view. Used to check whether the user has permission to load the
   * dashboard view.
   *
   * @param proceedingJoinPoint - the ProceedingJoinPoint object
   * @param request - the HttpServletRequest object
   * @return a ModelAndView object containing the expected view
   * @throws Throwable
   */
  @Around(
      value =
          "execution(*"
              + " org.verwandlung.voj.web.controller.AccountsController.dashboardView(..)) &&"
              + " args(request, ..)",
      argNames = "proceedingJoinPoint,request"
  )
  public ModelAndView dashboardViewInterceptor(
      ProceedingJoinPoint proceedingJoinPoint, HttpServletRequest request) throws Throwable {
    ModelAndView view = null;
    HttpSession session = request.getSession();

    if (!isAllowToAccess(session, new String[] {"users", "administrators"})) {
      RedirectView redirectView = new RedirectView(request.getContextPath() + "/accounts/login");
      redirectView.setExposeModelAttributes(false);
      return new ModelAndView(redirectView);
    }
    view = (ModelAndView) proceedingJoinPoint.proceed();
    return view;
  }

  /**
   * The aspect of the asynchronous operation requests in the dashboard. Checks whether the user has
   * permission to perform the operation.
   *
   * @param proceedingJoinPoint - the ProceedingJoinPoint object
   * @param request - the HttpServletRequest object
   * @return the expected operation result (a Map<String, Boolean> object)
   * @throws Throwable
   */
  @Around(
      value =
          "execution(*"
              + " org.verwandlung.voj.web.controller.AccountsController.*InDashboardAction(..)) &&"
              + " args(.., request)",
      argNames = "proceedingJoinPoint,request"
  )
  public Map<String, Boolean> dashboardActionInterceptor(
      ProceedingJoinPoint proceedingJoinPoint, HttpServletRequest request) throws Throwable {
    HttpSession session = request.getSession();

    if (!isAllowToAccess(session, new String[] {"users", "administrators"})) {
      throw new ResourceNotFoundException();
    }
    @SuppressWarnings("unchecked")
    Map<String, Boolean> result = (Map<String, Boolean>) proceedingJoinPoint.proceed();
    return result;
  }

  /**
   * The aspect of the system administration controller. Used to check whether the user has
   * permission to perform the corresponding operation.
   *
   * @param proceedingJoinPoint - the ProceedingJoinPoint object
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the expected view
   * @throws Throwable
   */
  @Around(
      value =
          "execution(* org.verwandlung.voj.web.controller.AdministrationController.*View(..)) &&"
              + " args(.., request, response)",
      argNames = "proceedingJoinPoint,request,response"
  )
  public ModelAndView administrationViewInterceptor(
      ProceedingJoinPoint proceedingJoinPoint,
      HttpServletRequest request,
      HttpServletResponse response)
      throws Throwable {
    ModelAndView view = null;
    HttpSession session = request.getSession();

    if (!isAllowToAccess(session, new String[] {"administrators"})) {
      RedirectView redirectView =
          new RedirectView(request.getContextPath() + "/accounts/dashboard");
      redirectView.setExposeModelAttributes(false);
      return new ModelAndView(redirectView);
    }
    view = (ModelAndView) proceedingJoinPoint.proceed();
    return view;
  }

  /**
   * The aspect of the system administration controller. Used to check whether the user has
   * permission to perform the corresponding operation.
   *
   * @param proceedingJoinPoint - the ProceedingJoinPoint object
   * @param request - the HttpRequest object
   * @return a Map<String, Object> object containing the expected result
   * @throws Throwable
   */
  @SuppressWarnings("unchecked")
  @Around(
      value =
          "execution(* org.verwandlung.voj.web.controller.AdministrationController.*Action(..)) &&"
              + " args(.., request)",
      argNames = "proceedingJoinPoint,request"
  )
  public Map<String, Object> AdministrationActionInterceptor(
      ProceedingJoinPoint proceedingJoinPoint, HttpServletRequest request) throws Throwable {
    Map<String, Object> result = null;
    HttpSession session = request.getSession();

    if (!isAllowToAccess(session, new String[] {"administrators"})) {
      return null;
    }
    result = (Map<String, Object>) proceedingJoinPoint.proceed();
    return result;
  }

  /**
   * Checks whether the user has permission to perform the operation.
   *
   * @param session - the HttpSession object
   * @param expectedUserGroupSlugs - the user groups allowed to perform the operation
   * @return whether the user has permission to perform the operation
   */
  private boolean isAllowToAccess(HttpSession session, String[] expectedUserGroupSlugs) {
    Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
    if (isLoggedIn == null || !isLoggedIn.booleanValue()) {
      return false;
    }

    long uid = (Long) session.getAttribute("uid");
    User user = userService.getUserUsingUid(uid);
    String userGroupSlug = user.getUserGroup().getUserGroupSlug();

    for (String expectedUserGroupSlug : expectedUserGroupSlugs) {
      if (userGroupSlug.equals(expectedUserGroupSlug)) {
        return true;
      }
    }
    return false;
  }

  /** The autowired UserService object. Used to query the user group the user belongs to. */
  @Autowired private UserService userService;
}
