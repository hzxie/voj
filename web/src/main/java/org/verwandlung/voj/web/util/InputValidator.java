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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * The syntax rules for the email addresses and links users and administrators enter.
 *
 * @author Haozhe Xie
 */
public class InputValidator {
  /** Utility classes should not have a public constructor. */
  private InputValidator() {}

  /**
   * Checks whether a value is an email address: a local part of letters, digits and {@code
   * ._%+-}, an {@code @}, and a domain whose last label is at least two letters, in at most {@value
   * #MAX_EMAIL_LENGTH} characters (the size of the email columns).
   *
   * @param email - the value to check
   * @return whether the value is an email address
   */
  public static boolean isEmail(String email) {
    return email != null
        && email.length() <= MAX_EMAIL_LENGTH
        && EMAIL_PATTERN.matcher(email).matches();
  }

  /**
   * Checks whether a value is an absolute http(s) URL with a host, of at most {@value
   * #MAX_URL_LENGTH} characters. Paths, queries and fragments are allowed.
   *
   * @param url - the value to check
   * @return whether the value is an http(s) URL
   */
  public static boolean isHttpUrl(String url) {
    if (url == null || url.length() > MAX_URL_LENGTH) {
      return false;
    }
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          && uri.getHost() != null;
    } catch (URISyntaxException ex) {
      return false;
    }
  }

  /** The maximum length of an email address. */
  public static final int MAX_EMAIL_LENGTH = 64;

  /** The maximum length of a URL. */
  public static final int MAX_URL_LENGTH = 255;

  /** The syntax of an email address. */
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
}
