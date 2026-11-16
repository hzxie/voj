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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.model.JudgerSummary;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.JudgerService;
import org.verwandlung.voj.web.service.ProblemService;

/**
 * Adds the chrome of the administration console to the pages it renders: the active sidebar
 * section (see {@link AdministrationSection}) and the sidebar badges. The JSON actions of the
 * console render no page, so they skip the badge queries; a page that loaded the {@code
 * judgerSummary} already lends it to the judgers badge.
 *
 * @author Haozhe Xie
 */
@Component
public class AdministrationChromeInterceptor implements HandlerInterceptor {
  /* (non-Javadoc)
   * @see org.springframework.web.servlet.HandlerInterceptor#postHandle
   */
  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    if (modelAndView == null || !(handler instanceof HandlerMethod)) {
      return;
    }
    String viewName = modelAndView.getViewName();
    HandlerMethod handlerMethod = (HandlerMethod) handler;
    AdministrationSection section =
        AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(), AdministrationSection.class);
    if (section == null) {
      section =
          AnnotatedElementUtils.findMergedAnnotation(
              handlerMethod.getBeanType(), AdministrationSection.class);
    }
    if (section == null || viewName == null || viewName.startsWith("redirect:")) {
      return;
    }
    modelAndView.addObject("adminSection", section.value());
    modelAndView.addObject("navProblemsBadge", toCompactNumber(problemService.getNumberOfProblems()));

    long liveContests = contestService.getNumberOfLiveContests();
    if (liveContests > 0) {
      modelAndView.addObject("navContestsBadge", liveContests);
    }
    int reportedReplies = discussionService.getNumberOfReportedReplies();
    if (reportedReplies > 0) {
      modelAndView.addObject("navDiscussionBadge", reportedReplies);
      modelAndView.addObject("navModerationBadge", reportedReplies);
      modelAndView.addObject("navAlertCount", reportedReplies);
    }
    Object pageSummary = modelAndView.getModel().get("judgerSummary");
    JudgerSummary judgerSummary =
        pageSummary instanceof JudgerSummary
            ? (JudgerSummary) pageSummary
            : judgerService.getJudgerSummary();
    int onlineJudgers = judgerSummary.getOnlineJudgers();
    if (onlineJudgers > 0) {
      modelAndView.addObject("navJudgersBadge", onlineJudgers);
    }
  }

  /**
   * Formats a count compactly for the sidebar badges (e.g. 2348 -&gt; "2.3k").
   *
   * @param number - the number to format
   * @return the compact string representation of the number
   */
  static String toCompactNumber(long number) {
    if (number >= 1000) {
      return String.format("%.1fk", number / 1000.0);
    }
    return String.valueOf(number);
  }

  /** The autowired ProblemService object, for the number of problems. */
  @Autowired private ProblemService problemService;

  /** The autowired ContestService object, for the number of live contests. */
  @Autowired private ContestService contestService;

  /** The autowired DiscussionService object, for the number of reported replies. */
  @Autowired private DiscussionService discussionService;

  /** The autowired JudgerService object, for the number of online judgers. */
  @Autowired private JudgerService judgerService;
}
