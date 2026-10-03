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

import java.util.regex.Pattern;

import org.jsoup.Jsoup;

/**
 * Helpers for search-engine metadata.
 *
 * @author Haozhe Xie
 */
public class SeoUtils {
  /** Utility classes should not have a public constructor. */
  private SeoUtils() {}

  /**
   * Builds a meta description from Markdown/HTML content: strips the markup, collapses whitespace
   * and truncates the result to {@link #MAX_DESCRIPTION_LENGTH} characters.
   *
   * @param text - the Markdown or HTML content (may be null)
   * @return the plain-text summary, or null if the content has no text
   */
  public static String getDescription(String text) {
    if (text == null) {
      return null;
    }
    // Jsoup drops the HTML tags, decodes the entities and collapses the whitespace.
    String description = Jsoup.parse(text).text();
    description = MARKDOWN_IMAGE.matcher(description).replaceAll("");
    description = MARKDOWN_LINK.matcher(description).replaceAll("$1");
    description = MARKDOWN_SYNTAX.matcher(description).replaceAll("");
    description = WHITESPACES.matcher(description).replaceAll(" ").trim();
    if (description.isEmpty()) {
      return null;
    }
    if (description.codePointCount(0, description.length()) <= MAX_DESCRIPTION_LENGTH) {
      return description;
    }
    int end = description.offsetByCodePoints(0, MAX_DESCRIPTION_LENGTH - 1);
    return description.substring(0, end).trim() + "…";
  }

  /** The maximum length of a meta description, the length search engines usually display. */
  private static final int MAX_DESCRIPTION_LENGTH = 160;

  /** Markdown images: ![alt](url). */
  private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");

  /** Markdown links: [text](url), replaced by their text. */
  private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]*)\\]\\([^)]*\\)");

  /** Markdown emphasis, heading, code and math delimiters. */
  private static final Pattern MARKDOWN_SYNTAX = Pattern.compile("[#*`~$]+");

  /** Runs of whitespace left behind by the removed markup. */
  private static final Pattern WHITESPACES = Pattern.compile("\\s+");
}
