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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.controller.DiscussionAdministrationController;
import org.verwandlung.voj.web.controller.ProblemsController;
import org.verwandlung.voj.web.model.JudgerSummary;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.JudgerService;
import org.verwandlung.voj.web.service.ProblemService;

/**
 * The test class for AdministrationChromeInterceptor.
 *
 * @author Haozhe Xie
 */
public class AdministrationChromeInterceptorTest {
  /** Wires mocked services into a fresh interceptor before every test. */
  @BeforeEach
  public void setUp() {
    problemService = mock(ProblemService.class);
    contestService = mock(ContestService.class);
    discussionService = mock(DiscussionService.class);
    judgerService = mock(JudgerService.class);
    when(judgerService.getJudgerSummary()).thenReturn(new JudgerSummary(List.of(), 0, -1, -1));
    interceptor = new AdministrationChromeInterceptor();
    ReflectionTestUtils.setField(interceptor, "problemService", problemService);
    ReflectionTestUtils.setField(interceptor, "contestService", contestService);
    ReflectionTestUtils.setField(interceptor, "discussionService", discussionService);
    ReflectionTestUtils.setField(interceptor, "judgerService", judgerService);
  }

  /** Test case: tests postHandle(...). Test data: the discussion list and the moderation queue pages. Expected: the controller's section, overridden by the moderation handler's; the badges are added. */
  @Test
  public void testAddsTheSectionAndBadgesToPages() throws Exception {
    when(problemService.getNumberOfProblems()).thenReturn(2348L);
    when(discussionService.getNumberOfReportedReplies()).thenReturn(3);

    ModelAndView view =
        postHandle("allDiscussionView", new ModelAndView("pages/administration/all-discussion"));
    Assertions.assertEquals("discussion", view.getModel().get("adminSection"));
    Assertions.assertEquals("2.3k", view.getModel().get("navProblemsBadge"));
    Assertions.assertEquals(3, view.getModel().get("navModerationBadge"));
    Assertions.assertNull(view.getModel().get("navJudgersBadge"));

    view = postHandle("moderationView", new ModelAndView("pages/administration/moderation"));
    Assertions.assertEquals("moderation", view.getModel().get("adminSection"));
  }

  /** Test case: tests postHandle(...). Test data: a JSON action (no ModelAndView) and a page outside the console. Expected: nothing is counted. */
  @Test
  public void testSkipsActionsAndOtherPages() throws Exception {
    postHandle("deleteThreadsAction", null);
    HandlerMethod publicPage =
        new HandlerMethod(
            new ProblemsController(),
            ProblemsController.class.getMethod(
                "problemsView", parameterTypes(ProblemsController.class, "problemsView")));
    ModelAndView view = new ModelAndView("pages/problems/problems");
    interceptor.postHandle(
        new MockHttpServletRequest(), new MockHttpServletResponse(), publicPage, view);
    Assertions.assertNull(view.getModel().get("adminSection"));
    verifyNoInteractions(problemService, contestService, discussionService);
    verify(judgerService, never()).getJudgerSummary();
  }

  /** Runs postHandle for a handler method of DiscussionAdministrationController. */
  private ModelAndView postHandle(String methodName, ModelAndView view) throws Exception {
    HandlerMethod handler =
        new HandlerMethod(
            new DiscussionAdministrationController(),
            DiscussionAdministrationController.class.getMethod(
                methodName, parameterTypes(DiscussionAdministrationController.class, methodName)));
    interceptor.postHandle(
        new MockHttpServletRequest(), new MockHttpServletResponse(), handler, view);
    return view;
  }

  /** Gets the parameter types of the public method of the given name. */
  private static Class<?>[] parameterTypes(Class<?> type, String methodName) {
    for (java.lang.reflect.Method method : type.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method.getParameterTypes();
      }
    }
    throw new IllegalArgumentException(methodName);
  }

  /** The mocked ProblemService. */
  private ProblemService problemService;

  /** The mocked ContestService. */
  private ContestService contestService;

  /** The mocked DiscussionService. */
  private DiscussionService discussionService;

  /** The mocked JudgerService. */
  private JudgerService judgerService;

  /** The AdministrationChromeInterceptor object under test. */
  private AdministrationChromeInterceptor interceptor;
}
