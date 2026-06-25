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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * The test class for UserMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserMapperTest {
  /** Test case: tests the getNumberOfUsersUsingUserGroup(UserGroup) method. Test data: the Users user group. Expected: the number of users in this user group. */
  @Test
  public void testGetNumberOfUsers() {
    UserGroup userGroup = new UserGroup(2, "users", "Users");
    long totalUsers = userMapper.getNumberOfUsersUsingUserGroup(userGroup);

    Assertions.assertEquals(1, totalUsers);
  }

  /** Test case: tests the getNumberOfUsersUsingLanguage(int) method. Test data: the unique identifier of the C++ language. Expected: the number of users using C++ as their preferred language. */
  @Test
  public void testGetNumberOfUsersUsingLanguage() {
    int languageId = 2;
    long totalUsers = userMapper.getNumberOfUsersUsingLanguage(languageId);
    Assertions.assertEquals(2, totalUsers);
  }

  /** Test case: tests the getUserUsingUid(long) method. Test data: the unique identifier of the user with username zjhzxhz. Expected: the expected user object. */
  @Test
  public void testGetUserUsingUidExists() {
    User user = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(user);

    String username = user.getUsername();
    Assertions.assertEquals("zjhzxhz", username);
  }

  /** Test case: tests the getUserUsingUid(long) method. Test data: a non-existing user unique identifier. Expected: a null reference. */
  @Test
  public void testGetUserUsingUidNotExists() {
    User user = userMapper.getUserUsingUid(0);
    Assertions.assertNull(user);
  }

  /** Test case: tests the getUserUsingUsername(String) method. Test data: the user with username zjhzxhz. Expected: the expected user object. */
  @Test
  public void testGetUserUsingUsernameExists() {
    User user = userMapper.getUserUsingUsername("Zjhzxhz");
    Assertions.assertNotNull(user);

    long uid = user.getUid();
    Assertions.assertEquals(1000, uid);
  }

  /** Test case: tests the getUserUsingUsername(String) method. Test data: a non-existing username. Expected: a null reference. */
  @Test
  public void testGetUserUsingUsernameNotExists() {
    User user = userMapper.getUserUsingUsername("Not-Exists");
    Assertions.assertNull(user);
  }

  /** Test case: tests the getUserUsingEmail(String) method. Test data: the email address of the user with username zjhzxhz. Expected: the expected user object. */
  @Test
  public void testGetUserUsingEmailExists() {
    User user = userMapper.getUserUsingEmail("cshzxie@gmail.com");
    Assertions.assertNotNull(user);

    long uid = user.getUid();
    Assertions.assertEquals(1000, uid);
  }

  /** Test case: tests the getUserUsingEmail(String) method. Test data: a non-existing email address. Expected: a null reference. */
  @Test
  public void testGetUserUsingEmailNotExists() {
    User user = userMapper.getUserUsingEmail("not-exists@verwandlung.org");
    Assertions.assertNull(user);
  }

  /**
   * Test case: tests the getUserUsingUserGroup(UserGroup, long, int) method. Test data: the Users user
   * group. Expected: the expected user list (1 user in total).
   */
  @Test
  public void testGetUserUsingUserGroup() {
    UserGroup userGroup = new UserGroup(2, "users", "Users");
    List<User> users = userMapper.getUserUsingUserGroup(userGroup, 0, 1);
    Assertions.assertEquals(1, users.size());

    User firstUser = users.get(0);
    String username = firstUser.getUsername();
    Assertions.assertEquals("another-user", username);
  }

  /** Test case: tests the createUser(User) method. Test data: a valid data set, and no identical username or email address exists in the table. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateUserNormally() {
    UserGroup userGroup = new UserGroup(1, "users", "Users");
    Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
    User user =
        new User("new-user-0xff", "Password", "new-user-0xff@verwandlung.org", userGroup, language);

    int numberOfRowsAffected = userMapper.createUser(user);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createUser(User) method. Test data: a valid data set, but an identical
   * username already exists in the table. Expected: an org.springframework.dao.DuplicateKeyException
   * is thrown.
   */
  @Test
  public void testCreateUserUsingExistingUsername() {
    UserGroup userGroup = new UserGroup(1, "users", "Users");
    Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
    User user = new User("zjhzxhz", "Password", "noreply@verwandlung.org", userGroup, language);
    Executable e =
        () -> {
          userMapper.createUser(user);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createUser(User) method. Test data: a valid data set, but an identical email
   * address already exists in the table. Expected: an
   * org.springframework.dao.DuplicateKeyException is thrown.
   */
  @Test
  public void testCreateUserUsingExistingEmail() {
    UserGroup userGroup = new UserGroup(1, "users", "Users");
    Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
    User user = new User("new-user-0xfe", "Password", "cshzxie@gmail.com", userGroup, language);
    Executable e =
        () -> {
          userMapper.createUser(user);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createUser(User) method. Test data: an invalid data set (a username that is
   * too long). Expected: an org.springframework.dao.DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateUserUsingTooLongUsername() {
    UserGroup userGroup = new UserGroup(1, "users", "Users");
    Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
    User user =
        new User("new-user-0xffffffff", "Password", "noreply@verwandlung.org", userGroup, language);
    Executable e =
        () -> {
          userMapper.createUser(user);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createUser(User) method. Test data: an invalid data set (a non-existing
   * programming language). Expected: an org.springframework.dao.DataIntegrityViolationException is
   * thrown.
   */
  @Test
  public void testCreateUserUsingNotExistsLanguage() {
    UserGroup userGroup = new UserGroup(1, "users", "Users");
    Language language = new Language(0, "not-exists", "Not Exists", "Not Exists", "Not Exists");
    User user =
        new User("new-user-0xfe", "Password", "noreply@verwandlung.org", userGroup, language);
    Executable e =
        () -> {
          userMapper.createUser(user);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the updateUser(User) method. Test data: a valid data set, and no identical username or email address exists in the table. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateUserNormally() {
    User user = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(user);

    user.setEmail("hzxie@hit.edu.cn");
    int numberOfRowsAffected = userMapper.updateUser(user);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /** Test case: tests the updateUser(User) method. Test data: a valid data set, and no identical username or email address exists in the table. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateUserUsingExistingEmail() {
    User user = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(user);

    user.setEmail("support@verwandlung.org");
    Executable e =
        () -> {
          userMapper.updateUser(user);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /** Test case: tests the deleteUser(long) method. Test data: an existing user unique identifier. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteUserExists() {
    User user = userMapper.getUserUsingUid(1002);
    Assertions.assertNotNull(user);

    int numberOfRowsAffected = userMapper.deleteUser(1002);
    Assertions.assertEquals(1, numberOfRowsAffected);

    user = userMapper.getUserUsingUid(1002);
    Assertions.assertNull(user);
  }

  /** Test case: tests the deleteUser(long) method. Test data: a non-existing user unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteUserNotExists() {
    User user = userMapper.getUserUsingUid(0);
    Assertions.assertNull(user);

    int numberOfRowsAffected = userMapper.deleteUser(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The UserMapper object under test. */
  @Autowired private UserMapper userMapper;
}
