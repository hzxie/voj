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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.LocaleUtils;

/**
 * Populates a page's {@link ModelAndView} with the attributes every page shares: the system options,
 * the display language, the asset CDN/version metadata and the logged-in user's profile and
 * answering status.
 *
 * <p>Invoked from {@link CommonModelInterceptor} for regular page handlers, and directly from {@code
 * ExceptionHandlingController} for the error pages (whose {@code @ExceptionHandler} dispatch never
 * reaches a {@code HandlerInterceptor.postHandle}).
 *
 * @author Haozhe Xie
 */
@Component
public class CommonModelPopulator {
  /**
   * Adds the shared attributes to the given view.
   *
   * @param view - the ModelAndView object to populate
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   */
  public void populate(
      ModelAndView view, HttpServletRequest request, HttpServletResponse response) {
    view.addAllObjects(getSystemOptions());
    view.addObject("language", getUserLanguage(request, response));

    // The CDN base URL and the cache-busting version key of every page's asset references.
    view.addObject("cdnUrl", applicationProperties.getProperty("url.cdn"))
        .addObject("version", applicationProperties.getProperty("build.version"))
        .addObject("productVersion", applicationProperties.getProperty("product.version"));

    // The header/footer build the "forward" parameter of the sign-in / sign-up / language links
    // from it, so the user returns to the current page afterwards.
    view.addObject("forwardUri", request.getRequestURI());
    addLanguageUrls(view, request);

    User user = HttpSessionParser.getCurrentUser();
    if (user != null) {
      view.addObject("isLogin", true).addObject("myProfile", user);
    } else {
      // Templates use isLogin in compound expressions (${isLogin and ...}), where SpEL rejects a
      // null operand.
      view.addObject("isLogin", false);
    }
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
    boolean hasExplicitLanguage =
        request.getParameter("language") != null || session.getAttribute("language") != null;
    if (!hasExplicitLanguage) {
      // First visit without an explicit ?language=xx_XX: seed the locale from the browser's
      // preferred supported language (SessionLocaleResolver otherwise defaults to en_US,
      // ignoring the Accept-Language header).
      LocaleUtils.setLocale(request, response, getPreferNaturalLanguage(request, response));
    }
    // The display language follows Spring's resolved locale, which the ?language=xx_XX switch
    // updates through the LocaleChangeInterceptor, so the dates match the #{} labels.
    Locale locale = RequestContextUtils.getLocale(request);
    String language = locale.getLanguage() + "_" + locale.getCountry();
    session.setAttribute("language", language);
    return language;
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
    Locale browserLocale = getBrowserLocale(request);

    // Prefer an exact language + country match (e.g. zh-TW -> zh_TW, distinguishing
    // Traditional from Simplified Chinese) before falling back to a language-only
    // match (e.g. any other zh-* -> the first supported zh variant).
    String languageOnlyMatch = null;
    for (String supportedLanguage : HREFLANGS_OF_SUPPORTED_LANGUAGES.keySet()) {
      Locale supportLanguageLocale = LocaleUtils.getLocaleOfLanguage(supportedLanguage);
      if (!supportLanguageLocale.getLanguage().equals(browserLocale.getLanguage())) {
        continue;
      }
      if (supportLanguageLocale.getCountry().equals(browserLocale.getCountry())) {
        return supportedLanguage;
      }
      if (languageOnlyMatch == null) {
        languageOnlyMatch = supportedLanguage;
      }
    }
    return languageOnlyMatch != null ? languageOnlyMatch : DEFAULT_LANGUAGE;
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

  /**
   * Adds the canonical URL and the per-language alternate URLs used by search engines. The display
   * language is chosen with the ?language=xx_XX parameter (falling back to the browser language),
   * so every language version of a page has its own URL.
   *
   * <p>Pages whose main content is authored in a single language (problems, discussion threads,
   * contests) set {@code isAuthoredContent}: switching the display language only translates the
   * page chrome, so all the language versions share one canonical URL and no alternates are listed
   * — search engines index the page once and detect its language from the content.
   *
   * @param view - the ModelAndView object to populate
   * @param request - the HttpRequest object
   */
  private void addLanguageUrls(ModelAndView view, HttpServletRequest request) {
    String baseUrl = applicationProperties.getProperty("url.base");
    UriComponentsBuilder pageUrl =
        baseUrl == null || baseUrl.isBlank()
            ? ServletUriComponentsBuilder.fromContextPath(request)
            : UriComponentsBuilder.fromUriString(baseUrl);
    pageUrl
        .path(request.getRequestURI().substring(request.getContextPath().length()))
        .query(request.getQueryString())
        .replaceQueryParam("language");
    String defaultUrl = pageUrl.build().toUriString();

    String language = request.getParameter("language");
    boolean isAuthoredContent = Boolean.TRUE.equals(view.getModel().get("isAuthoredContent"));
    if (isAuthoredContent || !HREFLANGS_OF_SUPPORTED_LANGUAGES.containsKey(language)) {
      view.addObject("canonicalUrl", defaultUrl);
    } else {
      view.addObject(
          "canonicalUrl", pageUrl.replaceQueryParam("language", language).build().toUriString());
    }
    if (!isAuthoredContent) {
      Map<String, String> alternateUrls = new LinkedHashMap<>();
      HREFLANGS_OF_SUPPORTED_LANGUAGES.forEach(
          (supportedLanguage, hreflang) ->
              alternateUrls.put(
                  hreflang,
                  pageUrl
                      .replaceQueryParam("language", supportedLanguage)
                      .build()
                      .toUriString()));
      alternateUrls.put("x-default", defaultUrl);
      view.addObject("alternateUrls", alternateUrls);
    }
  }

  /** The supported display languages, mapped to the hreflang codes of their alternate URLs. */
  private static final Map<String, String> HREFLANGS_OF_SUPPORTED_LANGUAGES = new LinkedHashMap<>();

  static {
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("en_US", "en");
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("zh_CN", "zh-CN");
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("zh_TW", "zh-TW");
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("ja_JP", "ja");
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("ko_KR", "ko");
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("vi_VN", "vi");
    HREFLANGS_OF_SUPPORTED_LANGUAGES.put("ru_RU", "ru");
  }

  /** The autowired OptionService object. */
  @Autowired private OptionService optionService;

  /**
   * voj.properties + version.properties, exposing url.cdn / build.version /
   * product.version. Defined as the {@code propertyConfigurer} Bean in
   * PersistenceConfig.
   */
  @Autowired
  @Qualifier("propertyConfigurer")
  private Properties applicationProperties;
}
