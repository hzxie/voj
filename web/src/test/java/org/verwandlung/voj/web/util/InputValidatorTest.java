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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for InputValidator.
 *
 * @author Haozhe Xie
 */
public class InputValidatorTest {
  /** Test case: tests isEmail(String). Test data: plain, plus-tagged and sub-domain addresses. Expected: all accepted. */
  @Test
  public void testIsEmailAcceptsCommonAddresses() {
    Assertions.assertTrue(InputValidator.isEmail("alice@example.com"));
    Assertions.assertTrue(InputValidator.isEmail("alice+oj@example.com"));
    Assertions.assertTrue(InputValidator.isEmail("a.b_c-d@mail.example.co.uk"));
  }

  /** Test case: tests isEmail(String). Test data: malformed and over-long addresses. Expected: all rejected. */
  @Test
  public void testIsEmailRejectsMalformedAddresses() {
    Assertions.assertFalse(InputValidator.isEmail(null));
    Assertions.assertFalse(InputValidator.isEmail("not-an-email"));
    Assertions.assertFalse(InputValidator.isEmail("alice@localhost"));
    Assertions.assertFalse(InputValidator.isEmail("alice@@example.com"));
    Assertions.assertFalse(InputValidator.isEmail("a b@example.com"));
    Assertions.assertFalse(InputValidator.isEmail("a".repeat(60) + "@example.com"));
  }

  /** Test case: tests isHttpUrl(String). Test data: http(s) URLs with and without a path. Expected: all accepted. */
  @Test
  public void testIsHttpUrlAcceptsPaths() {
    Assertions.assertTrue(InputValidator.isHttpUrl("https://example.org"));
    Assertions.assertTrue(InputValidator.isHttpUrl("https://github.com/alice"));
    Assertions.assertTrue(InputValidator.isHttpUrl("http://example.org:8080/a/b?c=d#e"));
  }

  /** Test case: tests isHttpUrl(String). Test data: other schemes, relative and malformed URLs. Expected: all rejected. */
  @Test
  public void testIsHttpUrlRejectsOtherUrls() {
    Assertions.assertFalse(InputValidator.isHttpUrl("ftp://example.org"));
    Assertions.assertFalse(InputValidator.isHttpUrl("javascript:alert(1)"));
    Assertions.assertFalse(InputValidator.isHttpUrl("/relative/path"));
    Assertions.assertFalse(InputValidator.isHttpUrl("https://exa mple.org"));
    Assertions.assertFalse(InputValidator.isHttpUrl("https://example.org/" + "a".repeat(255)));
  }
}
