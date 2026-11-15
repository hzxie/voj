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
package org.verwandlung.voj.web.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.verwandlung.voj.web.interceptor.CommonModelPopulator;

/**
 * The test class for ExceptionHandlingController.
 *
 * <p>A stand-alone MockMvc drives a tiny test controller through the advice, so the mapping from
 * exception to HTTP status is verified exactly as Spring MVC applies it.
 *
 * @author Haozhe Xie
 */
public class ExceptionHandlingControllerTest {
  /** Builds a stand-alone MockMvc with the advice under test and a mocked model populator. */
  @BeforeEach
  public void setUp() {
    ExceptionHandlingController advice = new ExceptionHandlingController();
    ReflectionTestUtils.setField(
        advice, "commonModelPopulator", mock(CommonModelPopulator.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController()).setControllerAdvice(advice).build();
  }

  /** Test case: a non-numeric path variable for a numeric id (e.g. GET /p/abc). Expected: 404. */
  @Test
  public void testPathVariableTypeMismatchIsNotFound() throws Exception {
    mockMvc.perform(get("/test/item/abc")).andExpect(status().isNotFound());
  }

  /** Test case: a non-numeric request parameter for a numeric argument. Expected: 400. */
  @Test
  public void testRequestParameterTypeMismatchIsBadRequest() throws Exception {
    mockMvc.perform(get("/test/list").param("page", "abc")).andExpect(status().isBadRequest());
  }

  /** Test case: a missing required request parameter. Expected: 400. */
  @Test
  public void testMissingRequestParameterIsBadRequest() throws Exception {
    mockMvc.perform(get("/test/list")).andExpect(status().isBadRequest());
  }

  /** Test case: a missing required request header (an ErrorResponse with a 4xx status). Expected: 400. */
  @Test
  public void testMissingRequestHeaderIsBadRequest() throws Exception {
    mockMvc.perform(get("/test/header")).andExpect(status().isBadRequest());
  }

  /** Test case: an unsupported HTTP method. Expected: 405. */
  @Test
  public void testUnsupportedMethodIsMethodNotAllowed() throws Exception {
    mockMvc.perform(post("/test/list").param("page", "1")).andExpect(status().isMethodNotAllowed());
  }

  /** Test case: an unexpected exception thrown by the handler. Expected: 500. */
  @Test
  public void testUnexpectedExceptionIsInternalServerError() throws Exception {
    mockMvc.perform(get("/test/boom")).andExpect(status().isInternalServerError());
  }

  /** Test case: a well-formed request. Expected: 200. */
  @Test
  public void testValidRequestIsOk() throws Exception {
    mockMvc.perform(get("/test/item/42")).andExpect(status().isOk());
  }

  /** A minimal controller exercising each failure mode. */
  @Controller
  static class TestController {
    @GetMapping("/test/item/{id}")
    @ResponseBody
    public String item(@PathVariable("id") long id) {
      return String.valueOf(id);
    }

    @GetMapping("/test/list")
    @ResponseBody
    public String list(@RequestParam("page") int page) {
      return String.valueOf(page);
    }

    @GetMapping("/test/header")
    @ResponseBody
    public String header(@RequestHeader("X-Required") String value) {
      return value;
    }

    @GetMapping("/test/boom")
    @ResponseBody
    public String boom() {
      throw new IllegalStateException("boom");
    }
  }

  /** The stand-alone MockMvc. */
  private MockMvc mockMvc;
}
