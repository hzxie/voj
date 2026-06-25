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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.LocaleUtils;

/**
 * The aspect class of the views. Loads the profile information and answering status of the
 * logged-in user before loading the page.
 *
 * @author Haozhe Xie
 */
@Aspect
@Component
public class ViewAspect {
  /**
   * Loads the profile information and answering status of the logged-in user.
   *
   * @param proceedingJoinPoint - the ProceedingJoinPoint object
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the expected view
   * @throws Throwable - the ResourceNotFound exception
   */
  @Around(
      value =
          "execution(* org.verwandlung.voj.web.controller.*.*View(..)) &&"
              + "args(.., request, response)",
      argNames = "proceedingJoinPoint,request,response"
  )
  public ModelAndView getUserProfile(
      ProceedingJoinPoint proceedingJoinPoint,
      HttpServletRequest request,
      HttpServletResponse response)
      throws Throwable {
    ModelAndView view = null;
    HttpSession session = request.getSession();

    view = (ModelAndView) proceedingJoinPoint.proceed();
    view.addAllObjects(getSystemOptions());
    view.addObject("language", getUserLanguage(request, response));

    boolean isLoggedIn = isLoggedIn(session);
    if (isLoggedIn) {
      long userId = (Long) session.getAttribute("uid");
      User user = userService.getUserUsingUid(userId);

      view.addObject("isLogin", isLoggedIn)
          .addObject("myProfile", user)
          .addObject("mySubmissionStats", submissionService.getSubmissionStatsOfUser(userId));
    }
    return view;
  }

  /**
   * Checks whether the user has logged in.
   *
   * @param session - the HttpSession object
   * @return whether the user has logged in
   */
  private boolean isLoggedIn(HttpSession session) {
    Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
    if (isLoggedIn == null || !isLoggedIn.booleanValue()) {
      return false;
    }
    return true;
  }

  /**
   * Loads the system-defined options (Option).
   *
   * @return a list of key-value pairs containing the system-defined options
   */
  private Map<String, String> getSystemOptions() {
    List<Option> options = optionService.getAutoloadOptions();
    Map<String, String> optionMap = new HashMap<>();

    for (Option option : options) {
      String key = option.getOptionName();
      String value = option.getOptionValue();
      optionMap.put(key, value);
    }
    return optionMap;
  }

  /**
   * Gets the display language of the current user.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return the slug of the display language of the current user
   */
  private String getUserLanguage(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession();
    Object languageAttribute = session.getAttribute("language");

    if (languageAttribute == null) {
      String preferNaturalLanguage = getPreferNaturalLanguage(request, response);
      LocaleUtils.setLocale(request, response, preferNaturalLanguage);
      return preferNaturalLanguage;
    }
    return (String) languageAttribute;
  }

  /**
   * Recommends the default language based on the user's browser language and the languages supported
   * by the system.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return the code of the recommended language (e.g. zh_CN)
   */
  private String getPreferNaturalLanguage(
      HttpServletRequest request, HttpServletResponse response) {
    final String DEFAULT_LANGUAGE = "en_US";
    final String[] supportedLanguages = {"en_US", "zh_CN"};
    Locale browserLocale = getBrowserLocale(request);

    for (String supportedLanguage : supportedLanguages) {
      Locale supportLanguageLocale = LocaleUtils.getLocaleOfLanguage(supportedLanguage);
      if (supportLanguageLocale.getLanguage().equals(browserLocale.getLanguage())) {
        return supportedLanguage;
      }
    }
    return DEFAULT_LANGUAGE;
  }

  /**
   * Gets the user's locale based on the browser language.
   *
   * @param request - the HttpRequest object
   * @return a Locale object containing the user's locale information
   */
  private Locale getBrowserLocale(HttpServletRequest request) {
    Locale locale = request.getLocale();
    return locale;
  }

  /** The autowired UserService object. */
  @Autowired private UserService userService;

  /** The autowired SubmissionService object. */
  @Autowired private SubmissionService submissionService;

  /** The autowired OptionService object. */
  @Autowired private OptionService optionService;
}
