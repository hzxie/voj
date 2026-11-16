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
package org.verwandlung.voj.web.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.ContestContestantMapper;
import org.verwandlung.voj.web.mapper.EmailValidationMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.mapper.UserMetaMapper;
import org.verwandlung.voj.web.model.EmailValidation;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.UserMeta;

/**
 * The test class for UserService.
 *
 * <p>Seed data: user 1000 zjhzxhz (administrator, email cshzxie@gmail.com), user 1001 voj@judger
 * (judger, email support@verwandlung.org), user 1002 another-user (regular user, email
 * noreply@verwandlung.org); all three have the same password hash. User groups:
 * forbidden/users/judgers/administrators.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserServiceTest {
  /** The stored (legacy MD5) password of the seed users; equals {@code md5("zjhzxhz")}. */
  private static final String PASSWORD_HASH = "785ee107c11dfe36de668b1ae7baacbb";

  /** The plaintext password of the seed users (the password encoder verifies the plaintext). */
  private static final String PLAINTEXT_PASSWORD = "zjhzxhz";

  /** Test case: tests the getUserUsingUid(long) method. Test data: an existing / non-existing user identifier. Expected: the corresponding user or a null reference. */
  @Test
  public void testGetUserUsingUid() {
    Assertions.assertNotNull(userService.getUserUsingUid(1000));
    Assertions.assertNull(userService.getUserUsingUid(0));
  }

  /** Test case: tests the getUserUsingUsernameOrEmail(String) method. Test data: a username / email / non-existing value. Expected: the corresponding user or a null reference. */
  @Test
  public void testGetUserUsingUsernameOrEmail() {
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("zjhzxhz"));
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("cshzxie@gmail.com"));
    Assertions.assertNull(userService.getUserUsingUsernameOrEmail("nonexistent"));
  }

  /** Test case: tests the getUserMetaUsingUid(User) method. Test data: a user with meta information / a null user. Expected: the meta information or an empty Map. */
  @Test
  public void testGetUserMetaUsingUid() {
    Map<String, Object> userMeta = userService.getUserMetaUsingUid(userService.getUserUsingUid(1000));
    Assertions.assertTrue(userMeta.containsKey("registerTime"));
    Assertions.assertTrue(userService.getUserMetaUsingUid(null).isEmpty());
  }

  /** Test case: tests the getUserGroupUsingSlug(String) method. Test data: an existing / non-existing user group slug. Expected: the user group or a null reference. */
  @Test
  public void testGetUserGroupUsingSlug() {
    Assertions.assertNotNull(userService.getUserGroupUsingSlug("administrators"));
    Assertions.assertNull(userService.getUserGroupUsingSlug("nonexistent"));
  }

  /** Test case: tests the getUserGroups() method. Test data: N/a. Expected: all user groups. */
  @Test
  public void testGetUserGroups() {
    Assertions.assertEquals(4, userService.getUserGroups().size());
  }

  /** Test case: tests the getNumberOfUsers(UserGroup) method. Test data: the users user group. Expected: the number of users in this group. */
  @Test
  public void testGetNumberOfUsers() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Assertions.assertEquals(1, userService.getNumberOfUsers(userGroup));
  }

  /** Test case: tests the getNumberOfUserRegisteredToday() method. Test data: all seed data register times are in the past. Expected: returns 0. */
  @Test
  public void testGetNumberOfUserRegisteredToday() {
    Assertions.assertEquals(0, userService.getNumberOfUserRegisteredToday());
  }

  /**
   * Test case: tests the getNumberOfUserRegisteredToday() method in a time zone far from UTC. Test
   * data: user 1002 registered at the current time, recorded as the registration stores it (a
   * string in the web application's time zone). Expected: the user is counted.
   */
  @Test
  public void testGetNumberOfUserRegisteredTodayInTheLocalTimeZone() {
    java.util.TimeZone defaultTimeZone = java.util.TimeZone.getDefault();
    try {
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Kiritimati"));
      String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
      userMetaMapper.createUserMeta(
          new UserMeta(userMapper.getUserUsingUid(1002), "registerTime", now));

      Assertions.assertEquals(1, userService.getNumberOfUserRegisteredToday());
    } finally {
      java.util.TimeZone.setDefault(defaultTimeZone);
    }
  }

  /** Test case: tests the getNumberOfUsersUsingUserGroupAndUsername(...) method. Test data: the users user group + a partial username. Expected: the number of matches. */
  @Test
  public void testGetNumberOfUsersUsingUserGroupAndUsername() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Assertions.assertEquals(
        1, userService.getNumberOfUsersUsingUserGroupAndUsername(userGroup, "another"));
  }

  /** Test case: tests the getUserUsingUserGroupAndUsername(...) method. Test data: the users user group + a partial username. Expected: the list of matching users. */
  @Test
  public void testGetUserUsingUserGroupAndUsername() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    List<User> users = userService.getUserUsingUserGroupAndUsername(userGroup, "another", 0, 10);
    Assertions.assertEquals(1, users.size());

    // The "another" keyword must resolve to the another-user account, not just
    // any single row - this guards the username LIKE predicate, not only its count.
    Assertions.assertEquals("another-user", users.get(0).getUsername());
  }

  /** Test case: tests the getUserUsingUserGroup(...) method. Test data: the users user group. Expected: the list of users in this group. */
  @Test
  public void testGetUserUsingUserGroup() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    List<User> users = userService.getUserUsingUserGroup(userGroup, 0, 10);
    Assertions.assertEquals(1, users.size());

    // The "users" group holds exactly one seeded account; assert it is that one,
    // so a broken group predicate that returns a same-sized but wrong page fails.
    Assertions.assertEquals("another-user", users.get(0).getUsername());
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: an administrator account with the correct password. Expected: login is allowed. */
  @Test
  public void testIsAllowedToLoginSuccessfully() {
    Map<String, Boolean> result =
        userService.isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD, "127.0.0.1");
    Assertions.assertTrue(result.get("isAccountValid"));
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: a judger account (a user group not allowed to log in). Expected: the account is valid but access is not allowed. */
  @Test
  public void testIsAllowedToLoginWithDisallowedUserGroup() {
    Map<String, Boolean> result =
        userService.isAllowedToLogin("support@verwandlung.org", PLAINTEXT_PASSWORD, "127.0.0.1");
    Assertions.assertTrue(result.get("isAccountValid"));
    Assertions.assertFalse(result.get("isAllowedToAccess"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: a wrong password. Expected: the account is invalid. */
  @Test
  public void testIsAllowedToLoginWithWrongPassword() {
    Map<String, Boolean> result =
        userService.isAllowedToLogin("zjhzxhz", "wrong-password-hash", "127.0.0.1");
    Assertions.assertFalse(result.get("isAccountValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /**
   * Test case: tests the throttling of isAllowedToLogin(...). Test data: 10 wrong passwords for user
   * zjhzxhz from one address, alternating its username and email address, then the right one from
   * that address and from another. Expected: the right password is refused with isThrottled from
   * the first address only; another account still logs in from it.
   */
  @Test
  public void testIsAllowedToLoginThrottlesAnAccountFromAnAddress() {
    guessPasswordOfZjhzxhz("10.0.1.1");
    Map<String, Boolean> result =
        userService.isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD, "10.0.1.1");
    Assertions.assertTrue(result.get("isThrottled"));
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertTrue(
        userService.isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD, "10.0.1.2").get("isSuccessful"));
    Assertions.assertTrue(
        userService
            .isAllowedToLogin("support@verwandlung.org", PLAINTEXT_PASSWORD, "10.0.1.1")
            .get("isAccountValid"));
  }

  /**
   * Test case: tests the throttling of isAllowedToLogin(...) with the addresses of an IPv6 network.
   * Test data: 10 wrong passwords for user zjhzxhz from addresses of one /64. Expected: the right
   * password is refused from another address of that /64, but not from another /64.
   */
  @Test
  public void testIsAllowedToLoginThrottlesAnIpv6Network() {
    for (int i = 0; i < 10; ++i) {
      userService.isAllowedToLogin("zjhzxhz", "wrong-" + i, "2001:db8:1:1::" + (i + 1));
    }
    Assertions.assertTrue(
        userService
            .isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD, "2001:db8:1:1:ffff::1")
            .get("isThrottled"));
    Assertions.assertTrue(
        userService
            .isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD, "2001:db8:1:2::1")
            .get("isSuccessful"));
  }

  /**
   * Test case: tests resetPassword(...) after the logins to an account were throttled. Test data:
   * 10 wrong passwords for user zjhzxhz from one address, then a password reset. Expected: the new
   * password logs in from that address at once.
   */
  @Test
  public void testResetPasswordClearsTheFailedLogins() {
    guessPasswordOfZjhzxhz("10.0.1.3");
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "reset-token", tomorrow()));
    Assertions.assertTrue(
        userService
            .resetPassword("cshzxie@gmail.com", "reset-token", "new-password", "new-password")
            .get("isSuccessful"));

    Assertions.assertTrue(
        userService.isAllowedToLogin("zjhzxhz", "new-password", "10.0.1.3").get("isSuccessful"));
  }

  /**
   * Tries 10 wrong passwords for user zjhzxhz from an address, alternating its username and email
   * address; none of them is throttled.
   *
   * @param ipAddress - the client address
   */
  private void guessPasswordOfZjhzxhz(String ipAddress) {
    for (int i = 0; i < 10; ++i) {
      String login = i % 2 == 0 ? "zjhzxhz" : "cshzxie@gmail.com";
      Map<String, Boolean> result = userService.isAllowedToLogin(login, "wrong-" + i, ipAddress);
      Assertions.assertFalse(result.get("isThrottled"));
      Assertions.assertFalse(result.get("isAccountValid"));
    }
  }

  /**
   * Test case: tests getAddressKey(String). Test data: IPv4 addresses, IPv6 addresses (with a zone
   * and an embedded IPv4 address) and what is not an address. Expected: an IPv4 address is its own
   * key, an IPv6 address is keyed by its /64, and anything else is left as it is.
   */
  @Test
  public void testGetAddressKey() {
    Assertions.assertEquals("192.0.2.1", UserService.getAddressKey("192.0.2.1"));
    Assertions.assertEquals(
        "2001:db8:1:2:0:0:0:0/64", UserService.getAddressKey("2001:db8:1:2:3:4:5:6"));
    Assertions.assertEquals("2001:db8:0:0:0:0:0:0/64", UserService.getAddressKey("2001:db8::1%eth0"));
    Assertions.assertEquals("0:0:0:0:0:0:0:0/64", UserService.getAddressKey("::1"));
    Assertions.assertEquals("192.0.2.1", UserService.getAddressKey("::ffff:192.0.2.1"));
    Assertions.assertEquals("not:an.address", UserService.getAddressKey("not:an.address"));
    Assertions.assertEquals("12::34::56", UserService.getAddressKey("12::34::56"));
    Assertions.assertEquals("null", UserService.getAddressKey(null));
  }

  /** Test case: tests isAllowedToLogin(...) with an over-long password. Test data: a password of 100 bytes. Expected: it is wrong, without an error from the bcrypt encoder. */
  @Test
  public void testIsAllowedToLoginWithOverlongPassword() {
    Map<String, Boolean> result =
        userService.isAllowedToLogin("zjhzxhz", "x".repeat(100), "127.0.0.1");
    Assertions.assertFalse(result.get("isAccountValid"));
  }

  /** Test case: tests the password rule through changePassword(...). Test data: a 64-character password, a 65-character one and 30 three-byte characters (90 bytes). Expected: only the first is legal. */
  @Test
  public void testPasswordLengthLimits() {
    User user = userService.getUserUsingUid(1000);
    String longest = "p".repeat(64);
    Assertions.assertTrue(
        userService.changePassword(user, PLAINTEXT_PASSWORD, longest, longest).get("isSuccessful"));
    Assertions.assertTrue(
        userService.isAllowedToLogin("zjhzxhz", longest, "127.0.0.1").get("isSuccessful"));

    String tooLong = "p".repeat(65);
    Assertions.assertFalse(
        userService.changePassword(user, longest, tooLong, tooLong).get("isNewPasswordLegal"));
    String tooManyBytes = "\u4e2d".repeat(30);
    Assertions.assertFalse(
        userService
            .changePassword(user, longest, tooManyBytes, tooManyBytes)
            .get("isNewPasswordLegal"));
  }

  /** Test case: tests createFirstAdministratorIfNone(). Test data: the test data's administrator, then none. Expected: nothing is created while one exists; then an administrator with a random password that logs in. */
  @Test
  public void testCreateFirstAdministratorIfNone() {
    Assertions.assertNull(userService.createFirstAdministratorIfNone());
    userService.banUser(1000);

    String[] credentials = userService.createFirstAdministratorIfNone();
    Assertions.assertNotNull(credentials);
    Assertions.assertEquals("root", credentials[0]);
    Assertions.assertTrue(credentials[1].length() >= 20);
    User root = userService.getUserUsingUsernameOrEmail("root");
    Assertions.assertEquals("administrators", root.getUserGroup().getUserGroupSlug());
    Assertions.assertTrue(
        userService.isAllowedToLogin("root", credentials[1], "127.0.0.1").get("isSuccessful"));
    Assertions.assertNull(userService.createFirstAdministratorIfNone());
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: an empty username. Expected: login fails. */
  @Test
  public void testIsAllowedToLoginWithEmptyUsername() {
    Map<String, Boolean> result = userService.isAllowedToLogin("", PASSWORD_HASH, "127.0.0.1");
    Assertions.assertTrue(result.get("isUsernameEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser(...) method. Test data: valid registration information. Expected: creation succeeds. */
  @Test
  public void testCreateUserSuccessfully() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser01", "password", "newuser01@example.com", "users", "text/x-java", "", "", true);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("newuser01"));
  }

  /** Test case: tests the createUser(...) method. Test data: valid info plus a display name and bio. Expected: creation succeeds and both extras persist as user meta. */
  @Test
  public void testCreateUserWithDisplayNameAndBio() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser02", "password", "newuser02@example.com", "users", "text/x-java",
            "Display Name", "Hello world", true);
    Assertions.assertTrue(result.get("isSuccessful"));
    User user = userService.getUserUsingUsernameOrEmail("newuser02");
    Assertions.assertNotNull(user);
    Map<String, Object> meta = userService.getUserMetaUsingUid(user);
    Assertions.assertEquals("Display Name", meta.get("displayName"));
    Assertions.assertEquals("Hello world", meta.get("aboutMe"));
  }

  /** Test case: tests the createUser(...) method. Test data: the username already exists. Expected: creation fails. */
  @Test
  public void testCreateUserWithExistingUsername() {
    Map<String, Boolean> result =
        userService.createUser(
            "zjhzxhz", "password", "unique@example.com", "users", "text/x-java", "", "", true);
    Assertions.assertTrue(result.get("isUsernameExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser(...) method. Test data: an illegal username. Expected: creation fails. */
  @Test
  public void testCreateUserWithIllegalUsername() {
    Map<String, Boolean> result =
        userService.createUser(
            "ab1", "password", "unique@example.com", "users", "text/x-java", "", "", true);
    Assertions.assertFalse(result.get("isUsernameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser(...) method. Test data: the email already exists. Expected: creation fails. */
  @Test
  public void testCreateUserWithExistingEmail() {
    Map<String, Boolean> result =
        userService.createUser(
            "uniqueuser", "password", "cshzxie@gmail.com", "users", "text/x-java", "", "", true);
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser(...) method. Test data: the system does not allow registration. Expected: creation fails. */
  @Test
  public void testCreateUserWhenRegisterDisallowed() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser02", "password", "newuser02@example.com", "users", "text/x-java", "", "", false);
    Assertions.assertFalse(result.get("isAllowRegister"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the isAllowedToAccess(User) method. Test data: an administrator, a judger, a banned user and null. Expected: only users and administrators may access. */
  @Test
  public void testIsAllowedToAccess() {
    User user = userService.getUserUsingUid(1000);
    Assertions.assertTrue(userService.isAllowedToAccess(user));
    Assertions.assertFalse(userService.isAllowedToAccess(userService.getUserUsingUid(1001)));
    userService.banUser(1000);
    Assertions.assertFalse(userService.isAllowedToAccess(userService.getUserUsingUid(1000)));
    Assertions.assertFalse(userService.isAllowedToAccess(null));
  }

  /** Test case: tests the changePassword(...) method. Test data: the correct old password and a valid new password. Expected: the change succeeds. */
  @Test
  public void testChangePasswordSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.changePassword(user, PLAINTEXT_PASSWORD, "new-password", "new-password");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /**
   * Test case: the account operations of a user write only what they change. Test data: user 1002
   * loaded at the start of a request; meanwhile an administrator bans the user and another session
   * changes the password; then the request changes the profile (with a new email address) and the
   * password with the stale object. Expected: the ban stays, the email and password are those the
   * request set, and the password change of the other session is overwritten only by the password
   * change (not by the profile change).
   */
  @Test
  public void testAccountOperationsDoNotRevertConcurrentChanges() {
    User staleUser = userService.getUserUsingUid(1002);
    userService.banUser(1002);
    String otherSessionPassword = "{bcrypt}changed-by-another-session";
    userMapper.updatePassword(1002, otherSessionPassword);

    userService.updateProfile(
        staleUser, PLAINTEXT_PASSWORD, "changed-1002@example.com", "", "", "", "{}", "");
    User storedUser = userService.getUserUsingUid(1002);
    Assertions.assertEquals("forbidden", storedUser.getUserGroup().getUserGroupSlug());
    Assertions.assertEquals(otherSessionPassword, storedUser.getPassword());
    Assertions.assertEquals("changed-1002@example.com", storedUser.getEmail());

    Assertions.assertTrue(
        userService
            .changePassword(staleUser, PLAINTEXT_PASSWORD, "new-password", "new-password")
            .get("isSuccessful"));
    storedUser = userService.getUserUsingUid(1002);
    Assertions.assertEquals("forbidden", storedUser.getUserGroup().getUserGroupSlug());
    Assertions.assertEquals("changed-1002@example.com", storedUser.getEmail());
    Assertions.assertTrue(passwordEncoder.matches("new-password", storedUser.getPassword()));
  }

  /**
   * Test case: tests the isAllowedToLogin(...) method with a legacy MD5 password. Test data: user
   * 1000, whose password is a bare MD5 digest. Expected: the login succeeds and the password is
   * re-encoded with bcrypt, without touching the rest of the account.
   */
  @Test
  public void testLoginUpgradesALegacyPasswordOnly() {
    userMapper.updatePreferLanguage(1000, 3);
    Assertions.assertTrue(
        userService.isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD, "127.0.0.1").get("isSuccessful"));
    User storedUser = userService.getUserUsingUid(1000);
    Assertions.assertTrue(storedUser.getPassword().startsWith("{bcrypt}"));
    Assertions.assertTrue(passwordEncoder.matches(PLAINTEXT_PASSWORD, storedUser.getPassword()));
    Assertions.assertEquals(3, storedUser.getPreferLanguage().getLanguageId());
  }

  /** Test case: tests the changePassword(...) method. Test data: the old password is left empty. Expected: the change fails as the old password is not correct. */
  @Test
  public void testChangePasswordWithEmptyOldPassword() {
    User user = userService.getUserUsingUid(1000);
    String encodedPassword = user.getPassword();
    Map<String, Boolean> result = userService.changePassword(user, "", "new-password", "new-password");
    Assertions.assertFalse(result.get("isOldPasswordCorrect"));
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertEquals(encodedPassword, userService.getUserUsingUid(1000).getPassword());
  }

  /**
   * Clears the wrong passwords UserService counted (in memory, in the application context the test
   * classes share), so that the failures of one test do not throttle another.
   */
  @AfterEach
  public void clearPasswordFailures() {
    Object target = org.springframework.test.util.AopTestUtils.getTargetObject(userService);
    UserService fresh = new UserService();
    for (String limiter :
        new String[] {"currentPasswordLimiter", "loginAccountLimiter", "loginAddressLimiter"}) {
      org.springframework.test.util.ReflectionTestUtils.setField(
          target, limiter, org.springframework.test.util.ReflectionTestUtils.getField(fresh, limiter));
    }
  }

  /**
   * Test case: tests the throttling of the current-password checks. Test data: user 1000 enters a
   * wrong current password 5 times (when changing the password and the email address), then the
   * right one. Expected: the 5 wrong ones are checked and reported as wrong; after them even the
   * right password is refused (isPasswordCheckThrottled, nothing is changed), for both operations,
   * while a profile change that needs no password still works. Another user is not affected, and
   * their right password clears their count.
   */
  @Test
  public void testCurrentPasswordChecksAreThrottled() {
    User user = userService.getUserUsingUid(1000);
    String passwordHash = user.getPassword();
    for (int i = 0; i < 4; ++i) {
      Map<String, Boolean> result =
          userService.changePassword(user, "wrong-" + i, "new-password", "new-password");
      Assertions.assertFalse(result.get("isOldPasswordCorrect"));
      Assertions.assertFalse(result.get("isPasswordCheckThrottled"));
    }
    Map<String, Boolean> result =
        userService.updateProfile(user, "wrong-4", "other@example.com", "", "", "", "{}", "");
    Assertions.assertFalse(result.get("isPasswordCorrect"));
    Assertions.assertFalse(result.get("isPasswordCheckThrottled"));

    result = userService.changePassword(user, PLAINTEXT_PASSWORD, "new-password", "new-password");
    Assertions.assertTrue(result.get("isPasswordCheckThrottled"));
    Assertions.assertFalse(result.get("isOldPasswordCorrect"));
    Assertions.assertFalse(result.get("isSuccessful"));
    result =
        userService.updateProfile(
            user, PLAINTEXT_PASSWORD, "other@example.com", "", "", "", "{}", "");
    Assertions.assertTrue(result.get("isPasswordCheckThrottled"));
    Assertions.assertFalse(result.get("isSuccessful"));
    User storedUser = userService.getUserUsingUid(1000);
    Assertions.assertEquals(passwordHash, storedUser.getPassword());
    Assertions.assertEquals("cshzxie@gmail.com", storedUser.getEmail());
    Assertions.assertTrue(
        userService
            .updateProfile(user, "", "cshzxie@gmail.com", "", "", "", "{}", "")
            .get("isSuccessful"));

    User anotherUser = userService.getUserUsingUid(1002);
    Assertions.assertFalse(
        userService
            .changePassword(anotherUser, "wrong", "new-password", "new-password")
            .get("isPasswordCheckThrottled"));
    Assertions.assertTrue(
        userService
            .changePassword(anotherUser, PLAINTEXT_PASSWORD, "new-password", "new-password")
            .get("isSuccessful"));
  }

  /** Test case: tests the changePassword(...) method. Test data: the old password is wrong. Expected: the change fails. */
  @Test
  public void testChangePasswordWithWrongOldPassword() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.changePassword(user, "wrong-old", "new-password", "new-password");
    Assertions.assertFalse(result.get("isOldPasswordCorrect"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the changePassword(...) method. Test data: the two new passwords do not match. Expected: the change fails. */
  @Test
  public void testChangePasswordWithMismatchedConfirmPassword() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.changePassword(user, PLAINTEXT_PASSWORD, "new-password", "another-password");
    Assertions.assertFalse(result.get("isConfirmPasswordMatched"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: a valid profile. Expected: the update succeeds. */
  @Test
  public void testUpdateProfileSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "", "cshzxie@gmail.com", "Haozhe Xie", "Beijing", "https://haozhexie.com", "{}",
            "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("Haozhe Xie", userService.getUserMetaUsingUid(user).get("displayName"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: social links that are not a JSON object of short strings. Expected: the update fails and nothing is stored. */
  @Test
  public void testUpdateProfileWithIllegalSocialLinks() {
    String[] illegalSocialLinks = {
      "x",
      "[\"GitHub\"]",
      "\"GitHub\"",
      "{\"GitHub\": 1}",
      "{\"GitHub\": {\"a\": \"b\"}}",
      "{\"\": \"zjhzxhz\"}",
      "{\"GitHub\": \"" + "a".repeat(65) + "\"}"
    };
    Object storedSocialLinks =
        userService.getUserMetaUsingUid(userService.getUserUsingUid(1000)).get("socialLinks");
    for (String socialLinks : illegalSocialLinks) {
      User user = userService.getUserUsingUid(1000);
      Map<String, Boolean> result =
          userService.updateProfile(
              user, "", "cshzxie@gmail.com", "Haozhe Xie", "Beijing", "", socialLinks, "About me");
      Assertions.assertFalse(result.get("isSocialLinksLegal"), socialLinks);
      Assertions.assertFalse(result.get("isSuccessful"), socialLinks);
    }
    Assertions.assertEquals(
        storedSocialLinks,
        userService.getUserMetaUsingUid(userService.getUserUsingUid(1000)).get("socialLinks"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: legal social links. Expected: they are stored and read back as a map. */
  @Test
  public void testUpdateProfileWithSocialLinks() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "", "cshzxie@gmail.com", "Haozhe Xie", "Beijing", "",
            "{\"GitHub\": \"hzxie\"}", "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(
        Map.of("GitHub", "hzxie"), userService.getUserMetaUsingUid(user).get("socialLinks"));
  }

  /** Test case: tests the getUserMetaUsingUid(...) method. Test data: malformed social links stored before they were validated on write. Expected: they are read as an empty map instead of failing. */
  @Test
  public void testGetUserMetaUsingUidToleratesMalformedSocialLinks() {
    User user = userService.getUserUsingUid(1000);
    UserMeta socialLinks = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "socialLinks");
    if (socialLinks == null) {
      userMetaMapper.createUserMeta(new UserMeta(user, "socialLinks", "x"));
    } else {
      socialLinks.setMetaValue("x");
      userMetaMapper.updateUserMeta(socialLinks);
    }
    Assertions.assertEquals(
        Collections.emptyMap(), userService.getUserMetaUsingUid(user).get("socialLinks"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: the email is already taken by another user. Expected: the update fails. */
  @Test
  public void testUpdateProfileWithExistingEmail() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, PLAINTEXT_PASSWORD, "support@verwandlung.org", "", "", "", "{}", "About me");
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /**
   * Test case: tests the updateProfile (profile) method. Test data: a new email address with a
   * wrong or a missing current password. Expected: the update fails with isPasswordCorrect false
   * and nothing changes.
   */
  @Test
  public void testUpdateProfileRequiresPasswordToChangeEmail() {
    for (String password : new String[] {"wrong-password", ""}) {
      User user = userService.getUserUsingUid(1000);
      Map<String, Boolean> result =
          userService.updateProfile(
              user, password, "attacker@example.com", "Mallory", "", "", "{}", "About me");
      Assertions.assertFalse(result.get("isPasswordCorrect"), password);
      Assertions.assertFalse(result.get("isSuccessful"), password);
      Assertions.assertEquals("cshzxie@gmail.com", userService.getUserUsingUid(1000).getEmail());
      Assertions.assertNotEquals(
          "Mallory", userService.getUserMetaUsingUid(user).get("displayName"));
    }
  }

  /** Test case: tests the updateProfile (profile) method. Test data: an illegal homepage address. Expected: the update fails. */
  @Test
  public void testUpdateProfileWithIllegalWebsite() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "", "cshzxie@gmail.com", "", "", "ftp://invalid", "{}", "Me");
    Assertions.assertFalse(result.get("isWebsiteLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: a homepage with a path and a plus-tagged email address. Expected: both are accepted and stored. */
  @Test
  public void testUpdateProfileWithWebsitePathAndPlusEmail() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "", "cshzxie@gmail.com", "", "", "https://github.com/alice", "{}", "Me");
    Assertions.assertTrue(result.get("isWebsiteLegal"));
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(
        "https://github.com/alice", userService.getUserMetaUsingUid(user).get("website"));

    Map<String, Boolean> creation =
        userService.createUser(
            "plusmail01", "password", "alice+oj@example.com", "users", "text/x-java", "", "", true);
    Assertions.assertTrue(creation.get("isEmailLegal"));
    Assertions.assertTrue(creation.get("isSuccessful"));
  }

  /** Test case: tests the updateUserByAdministrator(...) method. Test data: a valid password / user group / language / email / display name / bio. Expected: the update succeeds and persists. */
  @Test
  public void testUpdateUserByAdministratorSuccessfully() {
    User user = userService.getUserUsingUid(1002);
    String passwordHash = user.getPassword();
    Map<String, Boolean> result =
        userService.updateUserByAdministrator(
            user, "new-password", "administrators", "text/x-java", "new@example.com",
            "Another", "About me");
    Assertions.assertTrue(result.get("isSuccessful"));

    User updatedUser = userService.getUserUsingUid(1002);
    Assertions.assertNotEquals(passwordHash, updatedUser.getPassword());
    Assertions.assertEquals("administrators", updatedUser.getUserGroup().getUserGroupSlug());
    Assertions.assertEquals("new@example.com", updatedUser.getEmail());
    Assertions.assertEquals(
        "Another", userService.getUserMetaUsingUid(updatedUser).get("displayName"));
  }

  /** Test case: tests the updateUserByAdministrator(...) method. Test data: a non-existing user group. Expected: the update fails. */
  @Test
  public void testUpdateUserByAdministratorWithIllegalUserGroup() {
    User user = userService.getUserUsingUid(1002);
    Map<String, Boolean> result =
        userService.updateUserByAdministrator(
            user, "new-password", "no-group", "text/x-java", user.getEmail(), "", "");
    Assertions.assertFalse(result.get("isUserGroupLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /**
   * Test case: tests the updateUserByAdministrator(...) method. Test data: a new password and user
   * group with an email address another user has. Expected: the update fails and nothing is saved -
   * neither the password nor the group.
   */
  @Test
  public void testUpdateUserByAdministratorSavesNothingWhenEmailIsTaken() {
    User user = userService.getUserUsingUid(1002);
    String passwordHash = user.getPassword();
    Map<String, Boolean> result =
        userService.updateUserByAdministrator(
            user, "new-password", "administrators", "text/x-java", "cshzxie@gmail.com", "", "");
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));

    User storedUser = userService.getUserUsingUid(1002);
    Assertions.assertEquals(passwordHash, storedUser.getPassword());
    Assertions.assertEquals("users", storedUser.getUserGroup().getUserGroupSlug());
    Assertions.assertEquals("noreply@verwandlung.org", storedUser.getEmail());
  }

  /** Test case: tests the updateUserByAdministrator(...) method. Test data: an over-long display name. Expected: the update fails and personal profile fields are left untouched. */
  @Test
  public void testUpdateUserByAdministratorWithIllegalDisplayName() {
    User user = userService.getUserUsingUid(1000);
    String location = (String) userService.getUserMetaUsingUid(user).get("location");
    Map<String, Boolean> result =
        userService.updateUserByAdministrator(
            user, "", "administrators", "text/x-java", "cshzxie@gmail.com", "x".repeat(65), "Bio");
    Assertions.assertFalse(result.get("isDisplayNameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
    // The admin editor must not wipe location / website / social links it never edits.
    Assertions.assertEquals(location, userService.getUserMetaUsingUid(user).get("location"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: the email changes while email verification is required. Expected: the account is forced back to unverified and a verification email is dispatched. */
  @Test
  public void testUpdateProfileResetsVerificationWhenEmailChanges() {
    enableEmailVerificationRequirement();
    userMapper.updateEmailVerified(1000, true);
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user,
            PLAINTEXT_PASSWORD,
            "changed@example.com",
            "Haozhe Xie",
            "",
            "",
            "{}",
            "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertTrue(result.get("isEmailVerificationSent"));
    Assertions.assertFalse(userService.getUserUsingUid(1000).isEmailVerified());
  }

  /** Test case: tests the updateProfile (profile) method. Test data: the email is unchanged while email verification is required. Expected: the verified status is preserved and no verification email is dispatched. */
  @Test
  public void testUpdateProfileKeepsVerificationWhenEmailUnchanged() {
    enableEmailVerificationRequirement();
    userMapper.updateEmailVerified(1000, true);
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "", user.getEmail(), "Haozhe Xie", "Beijing", "", "{}", "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNull(result.get("isEmailVerificationSent"));
    Assertions.assertTrue(userService.getUserUsingUid(1000).isEmailVerified());
  }

  /** Test case: tests the updateProfile (profile) method. Test data: a pending password-reset token for the old address, then an email change. Expected: the token is usable for neither address, while the daily email counter stays with the old address. */
  @Test
  public void testUpdateProfileInvalidatesTokenOfOldEmail() {
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "old-token", tomorrow(), 2, new Date()));
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user,
            PLAINTEXT_PASSWORD,
            "new-address@example.com",
            "Haozhe Xie",
            "Beijing",
            "",
            "{}",
            "About me");
    Assertions.assertTrue(result.get("isSuccessful"));

    Assertions.assertFalse(userService.isEmailValidationValid("cshzxie@gmail.com", "old-token"));
    Assertions.assertFalse(
        userService.isEmailValidationValid("new-address@example.com", "old-token"));
    Assertions.assertFalse(
        userService
            .resetPassword("new-address@example.com", "old-token", "new-password", "new-password")
            .get("isSuccessful"));
    EmailValidation kept = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertNotEquals("old-token", kept.getToken());
    Assertions.assertEquals(2, kept.getDailyCount());
    Assertions.assertNull(emailValidationMapper.getEmailValidation("new-address@example.com"));
  }

  /**
   * Test case: the daily email limit of an address that several accounts take turns on. Test data:
   * email verification required, a daily limit of 3; user 1000 switches to victim@example.com and
   * has 3 verification emails sent to it (one by the change, two resent), switches away, then user
   * 1002 switches to it. Expected: the address gets no fourth email, neither for user 1000 nor for
   * user 1002: the counter stays with the address, not with the account.
   */
  @Test
  public void testDailyEmailLimitStaysWithTheAddress() {
    enableEmailVerificationRequirement();
    String victim = "victim@example.com";
    User user = userService.getUserUsingUid(1000);
    Assertions.assertTrue(
        userService
            .updateProfile(user, PLAINTEXT_PASSWORD, victim, "", "", "", "{}", "")
            .get("isSuccessful"));
    user = userService.getUserUsingUid(1000);
    Assertions.assertTrue(userService.resendVerificationEmail(user).get("isSuccessful"));
    Assertions.assertTrue(userService.resendVerificationEmail(user).get("isSuccessful"));
    Assertions.assertTrue(userService.resendVerificationEmail(user).get("isRateLimited"));
    Assertions.assertEquals(3, emailValidationMapper.getEmailValidation(victim).getDailyCount());

    Assertions.assertTrue(
        userService
            .updateProfile(user, PLAINTEXT_PASSWORD, "elsewhere@example.com", "", "", "", "{}", "")
            .get("isSuccessful"));
    User anotherUser = userService.getUserUsingUid(1002);
    Assertions.assertTrue(
        userService
            .updateProfile(anotherUser, PLAINTEXT_PASSWORD, victim, "", "", "", "{}", "")
            .get("isSuccessful"));
    anotherUser = userService.getUserUsingUid(1002);
    Assertions.assertTrue(userService.resendVerificationEmail(anotherUser).get("isRateLimited"));
    Assertions.assertEquals(3, emailValidationMapper.getEmailValidation(victim).getDailyCount());
    // No email went out for user 1002: the address holds no usable token.
    Assertions.assertFalse(
        emailValidationMapper.getEmailValidation(victim).getExpireTime().after(new Date()));
  }

  /** Test case: tests the updateUserByAdministrator(...) method. Test data: a pending verification token for the old address, then an email change by an administrator. Expected: the token is not usable for the new address. */
  @Test
  public void testUpdateUserByAdministratorInvalidatesTokenOfOldEmail() {
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "old-token", tomorrow()));
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateUserByAdministrator(
            user, "", "administrators", "text/x-java", "new-address@example.com", "Haozhe Xie", "");
    Assertions.assertTrue(result.get("isSuccessful"));

    Assertions.assertFalse(
        userService.verifyEmail("new-address@example.com", "old-token").get("isSuccessful"));
    Assertions.assertFalse(
        userService.isEmailValidationValid("new-address@example.com", "old-token"));
  }

  /** Enables the requireEmailVerification option within the current test transaction. */
  private void enableEmailVerificationRequirement() {
    optionService.updateOptions(java.util.Map.of("requireEmailVerification", "1"));
  }

  /** Test case: tests the isEmailValidationValid(...) method. Test data: valid validation credentials. Expected: returns true. */
  @Test
  public void testIsEmailValidationValid() {
    // voj_email_validation.email is a foreign key referencing a real user email; use the email of
    // user 1002, who has no validation credentials yet.
    emailValidationMapper.createEmailValidation(
        new EmailValidation("noreply@verwandlung.org", "valid-token", tomorrow()));
    Assertions.assertTrue(
        userService.isEmailValidationValid("noreply@verwandlung.org", "valid-token"));
    Assertions.assertFalse(
        userService.isEmailValidationValid("noreply@verwandlung.org", "wrong-token"));
  }

  /** Test case: tests the isEmailValidationValid(...) method. Test data: non-existing validation credentials. Expected: returns false. */
  @Test
  public void testIsEmailValidationValidWhenNotExists() {
    Assertions.assertFalse(userService.isEmailValidationValid("nobody@example.com", "token"));
  }

  /** Test case: tests the resetPassword(...) method. Test data: valid credentials and a valid, matching new password. Expected: the reset succeeds. */
  @Test
  public void testResetPasswordSuccessfully() {
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "reset-token", tomorrow()));
    Map<String, Boolean> result =
        userService.resetPassword(
            "cshzxie@gmail.com", "reset-token", "new-password", "new-password");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /**
   * Test case: tests the resetPassword(...) method. Test data: an unverified user resetting the
   * password through the emailed link. Expected: the reset proves that the user owns the address,
   * so it is marked as verified.
   */
  @Test
  public void testResetPasswordVerifiesEmail() {
    userMapper.updateEmailVerified(1000, false);
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "reset-token", tomorrow()));

    Assertions.assertTrue(
        userService
            .resetPassword("cshzxie@gmail.com", "reset-token", "new-password", "new-password")
            .get("isSuccessful"));
    Assertions.assertTrue(userService.getUserUsingUid(1000).isEmailVerified());
  }

  /**
   * Test case: tests the resendVerificationEmail(User) method. Test data: an unverified user.
   * Expected: a new verification token is issued for the address and the email is sent.
   */
  @Test
  public void testResendVerificationEmail() {
    userMapper.updateEmailVerified(1000, false);
    User user = userService.getUserUsingUid(1000);

    Map<String, Boolean> result = userService.resendVerificationEmail(user);

    Assertions.assertTrue(result.get("isSuccessful"));
    EmailValidation emailValidation = emailValidationMapper.getEmailValidation(user.getEmail());
    Assertions.assertNotNull(emailValidation);
    Assertions.assertTrue(
        userService.verifyEmail(user.getEmail(), emailValidation.getToken()).get("isSuccessful"));
    Assertions.assertTrue(userService.getUserUsingUid(1000).isEmailVerified());
  }

  /**
   * Test case: tests the resendVerificationEmail(User) method. Test data: a verified user, an
   * anonymous caller. Expected: nothing is sent.
   */
  @Test
  public void testResendVerificationEmailToVerifiedOrAnonymousUser() {
    userMapper.updateEmailVerified(1000, true);
    Map<String, Boolean> result =
        userService.resendVerificationEmail(userService.getUserUsingUid(1000));
    Assertions.assertTrue(result.get("isEmailVerified"));
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertNull(emailValidationMapper.getEmailValidation("cshzxie@gmail.com"));

    result = userService.resendVerificationEmail(null);
    Assertions.assertFalse(result.get("isUserLogin"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /**
   * Test case: tests the resendVerificationEmail(User) method. Test data: the address already
   * received the daily maximum of emails. Expected: nothing is sent and isRateLimited is true.
   */
  @Test
  public void testResendVerificationEmailIsRateLimited() {
    userMapper.updateEmailVerified(1000, false);
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "pending-token", tomorrow(), 3, new Date()));

    Map<String, Boolean> result =
        userService.resendVerificationEmail(userService.getUserUsingUid(1000));

    Assertions.assertTrue(result.get("isRateLimited"));
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertEquals(
        "pending-token", emailValidationMapper.getEmailValidation("cshzxie@gmail.com").getToken());
  }

  /** Test case: tests the resetPassword(...) method. Test data: invalid validation credentials. Expected: the reset fails. */
  @Test
  public void testResetPasswordWithInvalidToken() {
    Map<String, Boolean> result =
        userService.resetPassword(
            "cshzxie@gmail.com", "invalid-token", "new-password", "new-password");
    Assertions.assertFalse(result.get("isEmailValidationValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the sendVerificationEmail(...) method. Test data: the username and email do not match. Expected: the user does not exist and it fails. */
  @Test
  public void testSendVerificationEmailWithMismatchedEmail() {
    Map<String, Boolean> result =
        userService.sendVerificationEmail("zjhzxhz", "wrong@example.com");
    Assertions.assertFalse(result.get("isUserExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: the per-address daily email limit caps repeated sends. Test data: four sends to one address with the default limit of three. Expected: the credential's counter stops at three and the fourth send leaves it untouched. */
  @Test
  public void testSendVerificationEmailRespectsDailyLimit() {
    // User #1000 (zjhzxhz) owns cshzxie@gmail.com, which has no credential seeded.
    for (int i = 0; i < 3; ++i) {
      userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com");
    }
    EmailValidation atCap = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertEquals(3, atCap.getDailyCount());

    // The fourth send is over the limit: it neither increments the counter nor re-issues the token.
    userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com");
    EmailValidation overCap = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertEquals(3, overCap.getDailyCount());
    Assertions.assertEquals(atCap.getToken(), overCap.getToken());
  }

  /** Test case: the daily email limit is configurable. Test data: the emailDailyLimit option set to one. Expected: the second send to an address is suppressed. */
  @Test
  public void testSendVerificationEmailDailyLimitIsConfigurable() {
    optionService.updateOptions(java.util.Map.of("emailDailyLimit", "1"));

    userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com");
    EmailValidation first = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertEquals(1, first.getDailyCount());

    userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com");
    EmailValidation second = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertEquals(1, second.getDailyCount());
    Assertions.assertEquals(first.getToken(), second.getToken());
  }

  /** Test case: an elapsed rate-limit window resets the counter. Test data: a credential whose window began more than a day ago and is already at the cap. Expected: the next send starts a fresh window at one. */
  @Test
  public void testSendVerificationEmailResetsAfterWindow() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -2);
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "stale-token", tomorrow(), 3, calendar.getTime()));

    userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com");
    EmailValidation reset = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertEquals(1, reset.getDailyCount());
    Assertions.assertNotEquals("stale-token", reset.getToken());
  }

  /** Test case: tests the deleteUser(long) method. Test data: a user with no associated data. Expected: the user is deleted. */
  @Test
  public void testDeleteUser() {
    // Insert a user with no associated data (submission records, meta information, etc.) directly
    // through the Mapper, to avoid foreign-key constraints.
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Language language = new Language();
    language.setLanguageId(1);
    User user =
        new User("throwaway", PASSWORD_HASH, "throwaway@example.com", userGroup, language);
    userMapper.createUser(user);

    Assertions.assertEquals(1, userService.deleteUsers(List.of(user.getUid())));
    Assertions.assertNull(userService.getUserUsingUid(user.getUid()));
  }

  /**
   * Test case: tests the deleteUsers(List) method. Test data: user 1001, who attended contest #1
   * and made contest submissions, and a missing user. Expected: the user, their contest entry and
   * their submissions are deleted.
   */
  @Test
  public void testDeleteUsersWhoAttendedContest() {
    Assertions.assertEquals(1, userService.deleteUsers(List.of(1001L, 0L)));

    Assertions.assertNull(userService.getUserUsingUid(1001));
    Assertions.assertNull(contestContestantMapper.getContestantOfContest(1, 1001));
    Assertions.assertNull(submissionMapper.getSubmission(1002));
  }

  /**
   * Test case: tests the getRegisterTimes(List) method. Test data: users #1000 and #1001. Expected:
   * both users' register-time meta values are parsed into dates, keyed by user identifier (user #1000
   * registered on 2014-10-07).
   */
  @Test
  public void testGetRegisterTimes() {
    Map<Long, Date> registerTimes = userService.getRegisterTimes(Arrays.asList(1000L, 1001L));
    Assertions.assertEquals(2, registerTimes.size());
    Assertions.assertTrue(registerTimes.containsKey(1000L));

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(registerTimes.get(1000L));
    Assertions.assertEquals(2014, calendar.get(Calendar.YEAR));
    Assertions.assertEquals(Calendar.OCTOBER, calendar.get(Calendar.MONTH));
    Assertions.assertEquals(7, calendar.get(Calendar.DAY_OF_MONTH));
  }

  /** Test case: tests the getRegisterTimes(List) method with an empty list. Expected: an empty map (no query is issued). */
  @Test
  public void testGetRegisterTimesEmpty() {
    Assertions.assertTrue(userService.getRegisterTimes(Collections.emptyList()).isEmpty());
  }

  /** Returns the time at this moment tomorrow, used to construct non-expired email validation credentials. */
  private Date tomorrow() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, 1);
    return calendar.getTime();
  }

  /** The UserService object under test. */
  @Autowired private UserService userService;

  /** The Mapper used to construct unassociated user data within the test transaction. */
  @Autowired private UserMapper userMapper;

  /** The autowired PasswordEncoder, used to check the stored password hashes. */
  @Autowired
  private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  /** The SubmissionMapper object, used to check that submissions cascade. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The ContestContestantMapper object, used to check that contest entries cascade. */
  @Autowired private ContestContestantMapper contestContestantMapper;

  /** The autowired UserMetaMapper object, used to seed raw meta values. */
  @Autowired private UserMetaMapper userMetaMapper;

  /** The Mapper used to construct email validation credentials within the test transaction. */
  @Autowired private EmailValidationMapper emailValidationMapper;

  /** The OptionService object, used to change options within the test transaction. */
  @Autowired private OptionService optionService;
}
