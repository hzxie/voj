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

/**
 * Normalises user-supplied text (discussion titles and replies, display names, bios, ...) before
 * it is stored.
 *
 * <p>User text is stored as <em>plain text</em>, exactly as typed: it is neither stripped of
 * anything that looks like an HTML tag nor entity-encoded. Stripping mangled ordinary programming
 * text such as {@code #include <iostream>} or {@code vector<int> v;}, and storing entity-encoded
 * text made every output path that escapes (as all of them must) show {@code A &amp;amp; B}
 * instead of {@code A & B}. Protection against HTML injection is therefore the job of the output
 * side, which must always escape this text: {@code th:text} / {@code th:value} in templates, jQuery
 * {@code .text()} or an explicit escape when building markup in JavaScript, and markdown-it with
 * {@code html: false} for Markdown fields. Never render these fields with {@code th:utext}.
 *
 * <p>The only normalisation performed is removing control characters other than tab, line feed
 * and carriage return, which have no legitimate use in such text.
 *
 * @author Haozhe Xie
 */
public class HtmlTextFilter {
  /** Utility classes should not have a public constructor. */
  private HtmlTextFilter() {}

  /**
   * Normalises a user-supplied string into the plain text that is stored.
   *
   * @param text - the string to normalise, may be {@code null}
   * @return the normalised plain text, or {@code null} when the text is {@code null}
   */
  public static String filter(String text) {
    if (text == null) {
      return null;
    }
    StringBuilder normalisedText = null;
    for (int i = 0; i < text.length(); ++i) {
      char c = text.charAt(i);
      if (isDisallowedControlCharacter(c)) {
        if (normalisedText == null) {
          normalisedText = new StringBuilder(text.length()).append(text, 0, i);
        }
      } else if (normalisedText != null) {
        normalisedText.append(c);
      }
    }
    return normalisedText == null ? text : normalisedText.toString();
  }

  /**
   * Checks whether a character is a control character that is removed from user text: the C0 and
   * C1 control characters and DEL, except tab, line feed and carriage return.
   *
   * @param c - the character to check
   * @return whether the character is removed
   */
  private static boolean isDisallowedControlCharacter(char c) {
    return Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r';
  }
}
