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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.service.UserService;

/**
 * The test class for DefaultController.
 *
 * <p>Covers the controller-specific assembly of {@code getJudgersAction}: each judger row pairs the
 * username with the description resolved from the keep-alive listener, and the {@code isSuccessful}
 * flag reflects whether any judger was returned.
 *
 * @author Haozhe Xie
 */
public class DefaultControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    userService = mock(UserService.class);
    keepAliveEventListener = mock(ApplicationEventListener.class);

    controller = new DefaultController();
    ReflectionTestUtils.setField(controller, "userService", userService);
    ReflectionTestUtils.setField(controller, "keepAliveEventListener", keepAliveEventListener);
  }

  /** Test case: tests getJudgersAction(...). Test data: one judger in the "judgers" group. Expected: isSuccessful is true and the row carries the judger's username and description. */
  @Test
  public void testGetJudgersActionReturnsJudgers() {
    UserGroup judgersGroup = new UserGroup(4, "judgers", "Judgers");
    when(userService.getUserGroupUsingSlug("judgers")).thenReturn(judgersGroup);
    User judger = new User(1001, "judger-1", "p", "j@b.com", judgersGroup, null);
    when(userService.getUserUsingUserGroup(eqGroup(judgersGroup), anyLong(), anyInt()))
        .thenReturn(List.of(judger));
    when(keepAliveEventListener.getJudgerDescription("judger-1")).thenReturn("x86 node");

    Map<String, Object> result = controller.getJudgersAction(0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    @SuppressWarnings("unchecked")
    List<Map<String, String>> judgers = (List<Map<String, String>>) result.get("judgers");
    Assertions.assertEquals(1, judgers.size());
    Assertions.assertEquals("judger-1", judgers.get(0).get("username"));
    Assertions.assertEquals("x86 node", judgers.get(0).get("description"));
  }

  /** Test case: tests getJudgersAction(...). Test data: no judgers in the group. Expected: isSuccessful is false and the judger list is empty. */
  @Test
  public void testGetJudgersActionEmpty() {
    UserGroup judgersGroup = new UserGroup(4, "judgers", "Judgers");
    when(userService.getUserGroupUsingSlug("judgers")).thenReturn(judgersGroup);
    when(userService.getUserUsingUserGroup(eqGroup(judgersGroup), anyLong(), anyInt()))
        .thenReturn(List.of());

    Map<String, Object> result = controller.getJudgersAction(0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    @SuppressWarnings("unchecked")
    List<Map<String, String>> judgers = (List<Map<String, String>>) result.get("judgers");
    Assertions.assertTrue(judgers.isEmpty());
  }

  /** Matches the exact UserGroup instance passed to the stub. */
  private static UserGroup eqGroup(UserGroup group) {
    return org.mockito.ArgumentMatchers.eq(group);
  }

  /** The mocked UserService. */
  private UserService userService;

  /** The mocked keep-alive listener (online judger metadata). */
  private ApplicationEventListener keepAliveEventListener;

  /** The DefaultController object under test. */
  private DefaultController controller;
}
