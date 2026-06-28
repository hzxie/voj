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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for HttpRequestParser.
 *
 * @author Haozhe Xie
 */
public class HttpRequestParserTest {
  /** Test case: tests getRemoteAddr(HttpServletRequest). Test data: a request carrying an X-Real-IP header set by a reverse proxy. Expected: the address from the X-Real-IP header. */
  @Test
  public void testGetRemoteAddrBehindReverseProxy() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.7");
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");

    Assertions.assertEquals("203.0.113.7", HttpRequestParser.getRemoteAddr(request));
  }

  /** Test case: tests getRemoteAddr(HttpServletRequest). Test data: a request without the X-Real-IP header. Expected: the address from the underlying connection. */
  @Test
  public void testGetRemoteAddrWithoutReverseProxy() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("198.51.100.42");

    Assertions.assertEquals("198.51.100.42", HttpRequestParser.getRemoteAddr(request));
  }
}
