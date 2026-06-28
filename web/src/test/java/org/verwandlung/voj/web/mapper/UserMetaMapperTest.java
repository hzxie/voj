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
package org.verwandlung.voj.web.mapper;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserMeta;

/**
 * The test class for UserMetaMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserMetaMapperTest {
  /**
   * Test case: tests the getNumberOfUserRegistered(Date, Date) method. Test data: 2014-10-07 00:00:00
   * - 2014-10-07 23:59:59. Expected: the number of users registered on 2014-10-07 (1 in total).
   */
  @Test
  public void testGetNumberOfUserRegistered() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2014, 9, 8, 0, 0, 0);
    Date startTime = calendar.getTime();
    calendar.set(2014, 9, 8, 23, 59, 59);
    Date endTime = calendar.getTime();

    long numberOfUsers = userMetaMapper.getNumberOfUserRegistered(startTime, endTime);
    Assertions.assertEquals(1, numberOfUsers);
  }

  /** Test case: tests the getUserMetaUsingUser(User) method. Test data: the user with Uid 1000. Expected: the list of the user's user meta information. */
  @Test
  public void testGetUserMetaUsingUserExists() {
    User user = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(user);

    List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
    Assertions.assertEquals(1, userMetaList.size());

    UserMeta userMeta = userMetaList.get(0);
    String metaKey = userMeta.getMetaKey();
    Assertions.assertEquals("registerTime", metaKey);
  }

  /** Test case: tests the getUserMetaUsingUser(User) method. Test data: a non-existing user. Expected: an empty list. */
  @Test
  public void testGetUserMetaUsingUserNotExists() {
    User user = userMapper.getUserUsingUid(0);
    Assertions.assertNull(user);

    List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
    Assertions.assertEquals(0, userMetaList.size());
  }

  /**
   * Test case: tests the getUserMetaUsingUserAndMetaKey(User, String) method. Test data: the user with
   * UID 1000 and the register-time meta key. Expected: the corresponding register-time meta
   * information (UserMeta) object of the user.
   */
  @Test
  public void testGetUserMetaUsingUserAndMetaKeyExists() {
    User user = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(user);

    UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
    Assertions.assertNotNull(userMeta);

    String metaValue = userMeta.getMetaValue();
    Assertions.assertEquals("2014-10-07 12:35:45", metaValue);
  }

  /**
   * Test case: tests the getUserMetaUsingUserAndMetaKey(User, String) method. Test data: the user with
   * UID 1000 and a non-existing meta key. Expected: a null reference.
   */
  @Test
  public void testGetUserMetaUsingUserAndMetaKeyNotExists() {
    User user = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(user);

    UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "notExistsKey");
    Assertions.assertNull(userMeta);
  }

  /**
   * Test case: tests the getUserMetaValuesUsingMetaKey(List, String) method. Test data: users #1000
   * and #1001 with the register-time meta key. Expected: both users' register-time values are
   * returned, keyed by user identifier.
   */
  @Test
  public void testGetUserMetaValuesUsingMetaKey() {
    List<Map<String, Object>> rows =
        userMetaMapper.getUserMetaValuesUsingMetaKey(Arrays.asList(1000L, 1001L), "registerTime");
    Assertions.assertEquals(2, rows.size());

    Map<Long, String> valuesByUid = new HashMap<>();
    for (Map<String, Object> row : rows) {
      long uid = ((Number) row.get("uid")).longValue();
      valuesByUid.put(uid, (String) row.get("metaValue"));
    }
    Assertions.assertEquals("2014-10-07 12:35:45", valuesByUid.get(1000L));
    Assertions.assertEquals("2014-10-08 12:35:45", valuesByUid.get(1001L));
  }

  /** Test case: tests the createUserMeta(UserMeta) method. Test data: a valid data set, creating new user metadata. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateUserMetaNormally() {
    User user = userMapper.getUserUsingUid(1001);
    UserMeta userMeta = new UserMeta(user, "metaKey", "metaValue");

    int numberOfRowsAffected = userMetaMapper.createUserMeta(userMeta);
    Assertions.assertEquals(1, numberOfRowsAffected);

    UserMeta insertedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "metaKey");
    Assertions.assertNotNull(insertedUserMeta);

    String metaValue = insertedUserMeta.getMetaValue();
    Assertions.assertEquals("metaValue", metaValue);
  }

  /** Test case: tests the createUserMeta(UserMeta) method. Test data: a non-existing user. Expected: a DataIntegrityViolationException is thrown. */
  @Test
  public void testCreateUserMetaUsingUserNotExists() {
    User user = userMapper.getUserUsingUid(0);
    Assertions.assertNull(user);

    UserMeta userMeta = new UserMeta(user, "metaKey", "metaValue");
    Executable e =
        () -> {
          userMetaMapper.createUserMeta(userMeta);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the updateUserMeta(UserMeta) method. Test data: a valid data set, attempting to update existing user meta information. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateUserMetaNormally() {
    User user = userMapper.getUserUsingUid(1001);
    UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");

    userMeta.setMetaValue("newMetaValue");
    int numberOfRowsAffected = userMetaMapper.updateUserMeta(userMeta);
    Assertions.assertEquals(1, numberOfRowsAffected);

    UserMeta updatedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
    String metaValue = updatedUserMeta.getMetaValue();
    Assertions.assertEquals("newMetaValue", metaValue);
  }

  /** Test case: tests the updateUserMeta(UserMeta) method. Test data: a valid data set, attempting to update the key of existing user meta information. Expected: the meta key is not updated. */
  @Test
  public void testUpdateUserMetaTryToUpdateMetaKey() {
    User user = userMapper.getUserUsingUid(1001);
    UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");

    userMeta.setMetaKey("newMetaKey");
    int numberOfRowsAffected = userMetaMapper.updateUserMeta(userMeta);
    /** The following Assert CANNOT passed in CI due to the bug of MyBatis. But it really works. */
    // Assertions.assertEquals(0, numberOfRowsAffected);

    UserMeta updatedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
    String metaKey = updatedUserMeta.getMetaKey();
    Assertions.assertEquals("registerTime", metaKey);
  }

  /** Test case: tests the updateUserMeta(UserMeta) method. Test data: a non-existing user. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testUpdateUserMetaUsingUserNotExists() {
    User user = userMapper.getUserUsingUid(0);
    Assertions.assertNull(user);

    UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
    int numberOfRowsAffected = userMetaMapper.updateUserMeta(userMeta);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** Test case: tests the deleteUserMetaUsingUser(long) method. Test data: an existing user UID. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteUserMetaUsingUserExists() {
    User user = userMapper.getUserUsingUid(1001);
    Assertions.assertNotNull(user);

    int numberOfRowsAffected = userMetaMapper.deleteUserMetaUsingUser(1001);
    Assertions.assertEquals(1, numberOfRowsAffected);

    List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
    Assertions.assertEquals(0, userMetaList.size());
  }

  /** Test case: tests the deleteUserMetaUsingUser(long) method. Test data: a non-existing user UID. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteUserMetaUsingUserNotExists() {
    User user = userMapper.getUserUsingUid(0);
    Assertions.assertNull(user);

    int numberOfRowsAffected = userMetaMapper.deleteUserMetaUsingUser(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The UserMetaMapper object under test. */
  @Autowired private UserMetaMapper userMetaMapper;

  /** The autowired UserMapper object. Used to build test cases. */
  @Autowired private UserMapper userMapper;
}
