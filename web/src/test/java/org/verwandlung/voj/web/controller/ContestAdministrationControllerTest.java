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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.ContestService;

/**
 * The test class for ContestAdministrationController - the contestants of the contest editor. (Only
 * administrators reach these actions, and only with a CSRF token: see SecurityFilterChainTest.)
 *
 * @author Haozhe Xie
 */
public class ContestAdministrationControllerTest {
  /** Wires a mocked ContestService into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    contestService = mock(ContestService.class);
    controller = new ContestAdministrationController();
    ReflectionTestUtils.setField(controller, "contestService", contestService);
  }

  /** Test case: tests addContestantAction(...). Test data: a username to register for contest 3. Expected: ContestService.addContestant's result is returned. */
  @Test
  public void testAddContestantDelegates() {
    Map<String, Object> serviceResult =
        Map.of("isSuccessful", true, "uid", 1002L, "username", "another-user");
    when(contestService.addContestant(3L, "another-user")).thenReturn(serviceResult);

    Map<String, Object> result =
        controller.addContestantAction(3L, "another-user", new MockHttpServletRequest());

    Assertions.assertSame(serviceResult, result);
  }

  /** Test case: tests removeContestantAction(...). Test data: a contestant of contest 3, then a user who is not one. Expected: isSuccessful reports whether a contestant was removed. */
  @Test
  public void testRemoveContestantReportsWhetherRemoved() {
    when(contestService.removeContestant(3L, 1002L)).thenReturn(true);

    Assertions.assertEquals(
        Boolean.TRUE,
        controller
            .removeContestantAction(3L, 1002L, new MockHttpServletRequest())
            .get("isSuccessful"));
    Assertions.assertEquals(
        Boolean.FALSE,
        controller
            .removeContestantAction(3L, 1000L, new MockHttpServletRequest())
            .get("isSuccessful"));
    verify(contestService).removeContestant(3L, 1002L);
  }

  /** The mocked ContestService. */
  private ContestService contestService;

  /** The ContestAdministrationController under test. */
  private ContestAdministrationController controller;
}
