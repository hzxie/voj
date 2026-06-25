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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Pattern;

/**
 * The string Slug processing class. Ref: - https://github.com/slugify/slugify -
 * http://slugify.net/libraries
 *
 * @author Haozhe Xie
 */
public class SlugifyUtils {
  /**
   * Gets the Slug of a string.
   *
   * @param str - the string to get the Slug of
   * @return the Slug corresponding to the string
   */
  public static String getSlug(String str) {
    if (str == null) {
      return "";
    }

    // Rid of White Spaces
    String noWhiteSpace = WHITESPACE.matcher(str.trim()).replaceAll("-");
    // Processing Non-ASCII Characters
    try {
      noWhiteSpace = URLEncoder.encode(noWhiteSpace, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Never reach here
    }
    // Slugify String
    String normalized = Normalizer.normalize(noWhiteSpace, Form.NFD);

    return normalized.toLowerCase();
  }

  /** The regular expression pattern for whitespace. */
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
}
