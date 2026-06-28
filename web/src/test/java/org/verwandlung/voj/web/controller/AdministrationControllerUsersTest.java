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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;

/**
 * The test class for AdministrationController - the user-administration area.
 *
 * <p>Covers the controller-specific logic: the standard-competition ranking computed for the user
 * list, the comma/JSON id list fan-out of {@code deleteUsersAction}, and the existence guard plus
 * AND-combination of the two service results in {@code editUserAction}.
 *
 * @author Haozhe Xie
 */
public class AdministrationControllerUsersTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    userService = mock(UserService.class);
    submissionService = mock(SubmissionService.class);

    controller = new AdministrationController();
    ReflectionTestUtils.setField(controller, "userService", userService);
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
  }

  /** Test case: tests computeUserRanks(Map). Test data: solved counts with a tie. Expected: standard competition ranking (ties share the lowest rank, the next distinct count skips positions). */
  @Test
  public void testComputeUserRanksUsesStandardCompetitionRanking() {
    Map<Long, Long> solvedCounts = new LinkedHashMap<>();
    solvedCounts.put(10L, 5L);
    solvedCounts.put(11L, 5L);
    solvedCounts.put(12L, 3L);
    solvedCounts.put(13L, 1L);

    @SuppressWarnings("unchecked")
    Map<Long, Long> ranks =
        (Map<Long, Long>) ReflectionTestUtils.invokeMethod(
            controller, "computeUserRanks", solvedCounts);

    // Two users tie for first, so the third-place user is rank 3 (position 2 is skipped).
    Assertions.assertEquals(1L, ranks.get(10L));
    Assertions.assertEquals(1L, ranks.get(11L));
    Assertions.assertEquals(3L, ranks.get(12L));
    Assertions.assertEquals(4L, ranks.get(13L));
  }

  /** Test case: tests deleteUsersAction(...). Test data: a JSON array of two user ids. Expected: each id is deleted and isSuccessful is true. */
  @Test
  public void testDeleteUsersActionDeletesEach() {
    Map<String, Boolean> result =
        controller.deleteUsersAction("[1000, 1001]", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    verify(userService).deleteUser(1000L);
    verify(userService).deleteUser(1001L);
  }

  /** Test case: tests editUserAction(...). Test data: an unknown user id. Expected: isUserExists and isSuccessful are both false and no update is attempted. */
  @Test
  public void testEditUserActionUnknownUser() {
    when(userService.getUserUsingUid(9999L)).thenReturn(null);

    Map<String, Boolean> result =
        controller.editUserAction(
            9999L, "pw", "a@b.com", "users", "en_US", "Name", "Bio", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isUserExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(userService, never()).updateProfile(org.mockito.ArgumentMatchers.any(), eq("pw"), eq("users"), eq("en_US"));
  }

  /** Test case: tests editUserAction(...). Test data: an existing user where both updates succeed. Expected: isUserExists is true and isSuccessful is true. */
  @Test
  public void testEditUserActionBothUpdatesSucceed() {
    User user = userWithGroup("users");
    when(userService.getUserUsingUid(1000L)).thenReturn(user);
    when(userService.updateProfile(user, "pw", "users", "en_US")).thenReturn(map("isSuccessful", true));
    when(userService.updateProfileByAdmin(user, "a@b.com", "Name", "Bio"))
        .thenReturn(map("isSuccessful", true));

    Map<String, Boolean> result =
        controller.editUserAction(
            1000L, "pw", "a@b.com", "users", "en_US", "Name", "Bio", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isUserExists"));
    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
  }

  /** Test case: tests editUserAction(...). Test data: an existing user where the meta update fails. Expected: isUserExists is true but isSuccessful is false (the two results are AND-combined). */
  @Test
  public void testEditUserActionMetaUpdateFails() {
    User user = userWithGroup("users");
    when(userService.getUserUsingUid(1000L)).thenReturn(user);
    when(userService.updateProfile(user, "pw", "users", "en_US")).thenReturn(map("isSuccessful", true));
    when(userService.updateProfileByAdmin(user, "a@b.com", "Name", "Bio"))
        .thenReturn(map("isSuccessful", false));

    Map<String, Boolean> result =
        controller.editUserAction(
            1000L, "pw", "a@b.com", "users", "en_US", "Name", "Bio", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isUserExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests newUserAction(...). Test data: valid creation parameters. Expected: the request is delegated to UserService.createUser and its result returned. */
  @Test
  public void testNewUserActionDelegates() {
    Map<String, Boolean> serviceResult = map("isSuccessful", true);
    when(userService.createUser("zjhzxhz", "pw", "a@b.com", "users", "en_US", "Name", "Bio"))
        .thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.newUserAction(
            "zjhzxhz", "pw", "a@b.com", "users", "en_US", "Name", "Bio",
            new MockHttpServletRequest());

    Assertions.assertSame(serviceResult, result);
  }

  /** Test case: tests getNumberOfSubmissionsAction(...). Test data: a stubbed per-day count. Expected: the per-day counts are returned under the totalSubmissions key. */
  @Test
  public void testGetNumberOfSubmissionsAction() {
    Map<String, Long> perDay = Map.of("2026-06-28", 9L);
    when(submissionService.getNumberOfSubmissionsGroupByDay(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq(0L),
            eq(false)))
        .thenReturn(perDay);

    Map<String, Object> result =
        controller.getNumberOfSubmissionsAction(new MockHttpServletRequest());

    Assertions.assertSame(perDay, result.get("totalSubmissions"));
  }

  /** Builds a User in the given group. */
  private static User userWithGroup(String groupSlug) {
    return new User(1000, "zjhzxhz", "p", "a@b.com", new UserGroup(2, groupSlug, groupSlug), null);
  }

  /** Builds a mutable single-entry boolean result map (the controller mutates the returned maps). */
  private static Map<String, Boolean> map(String key, boolean value) {
    Map<String, Boolean> map = new HashMap<>();
    map.put(key, value);
    return map;
  }

  /** The mocked UserService. */
  private UserService userService;

  /** The mocked SubmissionService. */
  private SubmissionService submissionService;

  /** The AdministrationController object under test. */
  private AdministrationController controller;
}
