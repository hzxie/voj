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
package org.verwandlung.voj.web.util;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * The Locale-related helper functions.
 *
 * @author Haozhe Xie
 */
public class LocaleUtils {
  /** Utility classes should not have a public constructor. */
  private LocaleUtils() {}

  /**
   * Gets the corresponding Locale object based on an IETF Language Tag.
   *
   * @param languageName - the name of the language (e.g. zh_CN)
   * @return the expected Locale object
   */
  public static Locale getLocaleOfLanguage(String languageName) {
    String[] localeMeta = languageName.split("_");
    String language = localeMeta[0];
    String country = localeMeta[1];

    return new Locale(language, country);
  }

  /**
   * Sets the Locale information based on the user's language.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @param language - the name of the language (e.g. zh_CN)
   */
  public static void setLocale(
      HttpServletRequest request, HttpServletResponse response, String language) {
    Locale locale = LocaleUtils.getLocaleOfLanguage(language);
    LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
    localeResolver.setLocale(request, response, locale);
    request.getSession().setAttribute("language", language);
  }
}
