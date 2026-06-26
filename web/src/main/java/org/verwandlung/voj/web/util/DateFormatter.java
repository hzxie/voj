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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Locale-aware date formatting for the Thymeleaf views, reproducing the JSTL {@code
 * <fmt:formatDate>} style-based output that the JSP views relied on. The display locale is derived
 * from the user's language slug (e.g. {@code en_US}, {@code zh_CN}), which every view receives as
 * the {@code language} model attribute.
 *
 * <p>Used from templates as {@code ${@dateFormatter.dateTimeLongMedium(date, language)}}. Fixed
 * numeric patterns (e.g. {@code yyyy-MM-dd HH:mm:ss}) do not need this helper and are formatted with
 * Thymeleaf's built-in {@code #dates.format(date, pattern)} instead.
 *
 * @author Haozhe Xie
 */
@Component("dateFormatter")
public class DateFormatter {
  /**
   * Formats a date and time using a long date style and a medium time style, equivalent to {@code
   * <fmt:formatDate type="both" dateStyle="long" timeStyle="medium" />}.
   *
   * @param date - the date to format (may be {@code null})
   * @param language - the user's language slug (e.g. en_US)
   * @return the formatted date-time, or an empty string when {@code date} is {@code null}
   */
  public String dateTimeLongMedium(Date date, String language) {
    if (date == null) {
      return "";
    }
    Locale locale = LocaleUtils.getLocaleOfLanguage(language);
    return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, locale).format(date);
  }

  /**
   * Formats a date and time using a medium date style and a short time style, producing a compact
   * single-line label (e.g. {@code Feb 25, 2018, 8:04 AM}) suitable for dense table cells such as
   * the submissions list, where the long/medium style wraps onto multiple lines.
   *
   * @param date - the date to format (may be {@code null})
   * @param language - the user's language slug (e.g. en_US)
   * @return the formatted date-time, or an empty string when {@code date} is {@code null}
   */
  public String dateTimeMediumShort(Date date, String language) {
    if (date == null) {
      return "";
    }
    Locale locale = LocaleUtils.getLocaleOfLanguage(language);
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(date);
  }

  /**
   * Formats a date using a short date style, equivalent to {@code <fmt:formatDate type="date"
   * dateStyle="short" />}.
   *
   * @param date - the date to format (may be {@code null})
   * @param language - the user's language slug (e.g. en_US)
   * @return the formatted date, or an empty string when {@code date} is {@code null}
   */
  public String dateShort(Date date, String language) {
    if (date == null) {
      return "";
    }
    Locale locale = LocaleUtils.getLocaleOfLanguage(language);
    return DateFormat.getDateInstance(DateFormat.SHORT, locale).format(date);
  }

  /**
   * Formats a date using a long date style, equivalent to {@code <fmt:formatDate type="date"
   * dateStyle="long" />}.
   *
   * @param date - the date to format (may be {@code null})
   * @param language - the user's language slug (e.g. en_US)
   * @return the formatted date, or an empty string when {@code date} is {@code null}
   */
  public String dateLong(Date date, String language) {
    if (date == null) {
      return "";
    }
    Locale locale = LocaleUtils.getLocaleOfLanguage(language);
    return DateFormat.getDateInstance(DateFormat.LONG, locale).format(date);
  }

  /**
   * Parses a {@code yyyy-MM-dd HH:mm:ss} timestamp string and formats it with a long date style,
   * equivalent to the JSP {@code <fmt:parseDate>} + {@code <fmt:formatDate type="date"
   * dateStyle="long" />} pair.
   *
   * @param dateTime - the timestamp string (e.g. 2026-06-25 13:45:00; may be {@code null}/empty)
   * @param language - the user's language slug (e.g. en_US)
   * @return the formatted date, or an empty string when {@code dateTime} is missing or unparseable
   */
  public String parseDateLong(String dateTime, String language) {
    if (dateTime == null || dateTime.isEmpty()) {
      return "";
    }
    try {
      Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateTime);
      return dateLong(date, language);
    } catch (ParseException e) {
      return "";
    }
  }
}
