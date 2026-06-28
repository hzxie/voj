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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.OptionService;

/**
 * The test class for ContestsController.
 *
 * <p>Covers the controller-specific assembly of {@code getContestsAction} - the entrants count and
 * the problem count derived from the contest's JSON problem list - and the current-user pass-through
 * of {@code attendContestAction}.
 *
 * @author Haozhe Xie
 */
public class ContestsControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    contestService = mock(ContestService.class);
    optionService = mock(OptionService.class);
    when(optionService.getIntOption(anyString(), anyInt())).thenReturn(50);

    controller = new ContestsController();
    ReflectionTestUtils.setField(controller, "contestService", contestService);
    ReflectionTestUtils.setField(controller, "optionService", optionService);
  }

  /** Clears the security context after every test. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests getContestsAction(...). Test data: a contest whose JSON problem list holds two ids and five entrants. Expected: the result exposes the contest, its entrant count and a problem count of two. */
  @Test
  public void testGetContestsActionBuildsCountsFromContest() {
    Contest contest = new Contest();
    contest.setContestId(7);
    contest.setProblems("[1000, 1001]");
    when(contestService.getContests(nullable(String.class), eq(0L), eq(50)))
        .thenReturn(List.of(contest));
    when(contestService.getNumberOfContestantsOfContest(7L)).thenReturn(5L);

    Map<String, Object> result =
        controller.getContestsAction(null, 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    @SuppressWarnings("unchecked")
    Map<Long, Long> entrants = (Map<Long, Long>) result.get("entrants");
    @SuppressWarnings("unchecked")
    Map<Long, Integer> problemCount = (Map<Long, Integer>) result.get("problemCount");
    Assertions.assertEquals(5L, entrants.get(7L));
    Assertions.assertEquals(2, problemCount.get(7L));
  }

  /** Test case: tests getContestsAction(...). Test data: no contests. Expected: isSuccessful is false. */
  @Test
  public void testGetContestsActionEmpty() {
    when(contestService.getContests(nullable(String.class), anyLong(), anyInt()))
        .thenReturn(List.of());

    Map<String, Object> result =
        controller.getContestsAction("kw", 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests attendContestAction(...). Test data: an authenticated user attending a contest. Expected: the current user is passed to ContestService.attendContest and the result returned. */
  @Test
  public void testAttendContestActionDelegates() {
    User user = currentUser();
    authenticate(user);
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(contestService.attendContest(42L, user)).thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.attendContestAction(42L, request(), new org.springframework.mock.web.MockHttpServletResponse());

    Assertions.assertSame(serviceResult, result);
    verify(contestService).attendContest(42L, user);
  }

  /** Builds the current user (uid 1000, group "users"). */
  private static User currentUser() {
    return new User(1000, "zjhzxhz", "p", "a@b.com", new UserGroup(2, "users", "Users"), null);
  }

  /** Places the given user into the Spring Security context. */
  private static void authenticate(User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities()));
  }

  /** Builds a request carrying a remote address for the controller's audit logging. */
  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/contest/42/attend.action");
    request.setRemoteAddr("127.0.0.1");
    return request;
  }

  /** The mocked ContestService. */
  private ContestService contestService;

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The ContestsController object under test. */
  private ContestsController controller;
}
