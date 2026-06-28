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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;

/**
 * The test class for CommonModelInterceptor.
 *
 * @author Haozhe Xie
 */
public class CommonModelInterceptorTest {
  /** Wires a mocked CommonModelPopulator into a fresh interceptor before every test. */
  @BeforeEach
  public void setUp() {
    populator = mock(CommonModelPopulator.class);
    interceptor = new CommonModelInterceptor();
    ReflectionTestUtils.setField(interceptor, "commonModelPopulator", populator);
  }

  /** Test case: tests postHandle(...). Test data: a regular ModelAndView rendering a template. Expected: the populator decorates the view exactly once. */
  @Test
  public void testPostHandleDecoratesTemplateView() {
    ModelAndView modelAndView = new ModelAndView("pages/index");
    interceptor.postHandle(request, response, new Object(), modelAndView);

    verify(populator, times(1)).populate(modelAndView, request, response);
  }

  /** Test case: tests postHandle(...). Test data: a null ModelAndView (a JSON @ResponseBody endpoint). Expected: the populator is never invoked. */
  @Test
  public void testPostHandleSkipsNullModelAndView() {
    interceptor.postHandle(request, response, new Object(), null);

    verify(populator, never()).populate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  /** Test case: tests postHandle(...). Test data: a redirect view. Expected: the populator is never invoked, so attributes do not leak onto the redirect URL. */
  @Test
  public void testPostHandleSkipsRedirectView() {
    ModelAndView modelAndView = new ModelAndView("redirect:/accounts/login");
    interceptor.postHandle(request, response, new Object(), modelAndView);

    verify(populator, never()).populate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  /** The mocked CommonModelPopulator the interceptor delegates to. */
  private CommonModelPopulator populator;

  /** The CommonModelInterceptor object under test. */
  private CommonModelInterceptor interceptor;

  /** The mock request passed through to the interceptor. */
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  /** The mock response passed through to the interceptor. */
  private final MockHttpServletResponse response = new MockHttpServletResponse();
}
