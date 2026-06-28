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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.EmailValidationMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.model.EmailValidation;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

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
    Map<String, Boolean> result = userService.isAllowedToLogin("zjhzxhz", PLAINTEXT_PASSWORD);
    Assertions.assertTrue(result.get("isAccountValid"));
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: a judger account (a user group not allowed to log in). Expected: the account is valid but access is not allowed. */
  @Test
  public void testIsAllowedToLoginWithDisallowedUserGroup() {
    Map<String, Boolean> result =
        userService.isAllowedToLogin("support@verwandlung.org", PLAINTEXT_PASSWORD);
    Assertions.assertTrue(result.get("isAccountValid"));
    Assertions.assertFalse(result.get("isAllowedToAccess"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: a wrong password. Expected: the account is invalid. */
  @Test
  public void testIsAllowedToLoginWithWrongPassword() {
    Map<String, Boolean> result = userService.isAllowedToLogin("zjhzxhz", "wrong-password-hash");
    Assertions.assertFalse(result.get("isAccountValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the isAllowedToLogin(...) method. Test data: an empty username. Expected: login fails. */
  @Test
  public void testIsAllowedToLoginWithEmptyUsername() {
    Map<String, Boolean> result = userService.isAllowedToLogin("", PASSWORD_HASH);
    Assertions.assertTrue(result.get("isUsernameEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser (6-argument) method. Test data: valid registration information. Expected: creation succeeds. */
  @Test
  public void testCreateUserSuccessfully() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser01", "password", "newuser01@example.com", "users", "text/x-java", true);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("newuser01"));
  }

  /** Test case: tests the createUser (7-argument) method. Test data: valid info plus a display name and bio. Expected: creation succeeds and both extras persist as user meta. */
  @Test
  public void testCreateUserWithDisplayNameAndBio() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser02", "password", "newuser02@example.com", "users", "text/x-java",
            "Display Name", "Hello world");
    Assertions.assertTrue(result.get("isSuccessful"));
    User user = userService.getUserUsingUsernameOrEmail("newuser02");
    Assertions.assertNotNull(user);
    Map<String, Object> meta = userService.getUserMetaUsingUid(user);
    Assertions.assertEquals("Display Name", meta.get("displayName"));
    Assertions.assertEquals("Hello world", meta.get("aboutMe"));
  }

  /** Test case: tests the createUser (6-argument) method. Test data: the username already exists. Expected: creation fails. */
  @Test
  public void testCreateUserWithExistingUsername() {
    Map<String, Boolean> result =
        userService.createUser(
            "zjhzxhz", "password", "unique@example.com", "users", "text/x-java", true);
    Assertions.assertTrue(result.get("isUsernameExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser (6-argument) method. Test data: an illegal username. Expected: creation fails. */
  @Test
  public void testCreateUserWithIllegalUsername() {
    Map<String, Boolean> result =
        userService.createUser(
            "ab1", "password", "unique@example.com", "users", "text/x-java", true);
    Assertions.assertFalse(result.get("isUsernameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser (6-argument) method. Test data: the email already exists. Expected: creation fails. */
  @Test
  public void testCreateUserWithExistingEmail() {
    Map<String, Boolean> result =
        userService.createUser(
            "uniqueuser", "password", "cshzxie@gmail.com", "users", "text/x-java", true);
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser (6-argument) method. Test data: the system does not allow registration. Expected: creation fails. */
  @Test
  public void testCreateUserWhenRegisterDisallowed() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser02", "password", "newuser02@example.com", "users", "text/x-java", false);
    Assertions.assertFalse(result.get("isAllowRegister"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createUser (5-argument, administrator) method. Test data: valid information. Expected: creation succeeds. */
  @Test
  public void testCreateUserByAdministrator() {
    Map<String, Boolean> result =
        userService.createUser(
            "adminmade1", "password", "adminmade1@example.com", "users", "text/x-java");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the changePreferLanguage(...) method. Test data: a legal / illegal language slug. Expected: success or failure accordingly. */
  @Test
  public void testChangePreferLanguage() {
    User user = userService.getUserUsingUid(1000);
    Assertions.assertTrue(userService.changePreferLanguage(user, "text/x-java").get("isSuccessful"));
    Assertions.assertFalse(userService.changePreferLanguage(user, "no-lang").get("isSuccessful"));
  }

  /** Test case: tests the changePassword(...) method. Test data: the old password is left empty (treated as correct) and the new password is valid. Expected: the change succeeds. */
  @Test
  public void testChangePasswordSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result = userService.changePassword(user, "", "new-password", "new-password");
    Assertions.assertTrue(result.get("isSuccessful"));
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
        userService.changePassword(user, "", "new-password", "another-password");
    Assertions.assertFalse(result.get("isConfirmPasswordMatched"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: a valid profile. Expected: the update succeeds. */
  @Test
  public void testUpdateProfileSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "cshzxie@gmail.com", "Haozhe Xie", "Beijing", "https://haozhexie.com", "{}",
            "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("Haozhe Xie", userService.getUserMetaUsingUid(user).get("displayName"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: the email is already taken by another user. Expected: the update fails. */
  @Test
  public void testUpdateProfileWithExistingEmail() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "support@verwandlung.org", "", "", "", "{}", "About me");
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfile (profile) method. Test data: an illegal homepage address. Expected: the update fails. */
  @Test
  public void testUpdateProfileWithIllegalWebsite() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(user, "cshzxie@gmail.com", "", "", "ftp://invalid", "{}", "Me");
    Assertions.assertFalse(result.get("isWebsiteLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfile (administrator) method. Test data: a valid password / user group / language. Expected: the update succeeds. */
  @Test
  public void testUpdateProfileByAdministratorSuccessfully() {
    User user = userService.getUserUsingUid(1002);
    Map<String, Boolean> result =
        userService.updateProfile(user, "new-password", "users", "text/x-java");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfile (administrator) method. Test data: a non-existing user group. Expected: the update fails. */
  @Test
  public void testUpdateProfileByAdministratorWithIllegalUserGroup() {
    User user = userService.getUserUsingUid(1002);
    Map<String, Boolean> result =
        userService.updateProfile(user, "new-password", "no-group", "text/x-java");
    Assertions.assertFalse(result.get("isUserGroupLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateProfileByAdmin(...) method. Test data: a valid email / display name / bio. Expected: the update succeeds and the display name persists. */
  @Test
  public void testUpdateProfileByAdminSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfileByAdmin(user, "cshzxie@gmail.com", "Haozhe Xie", "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("Haozhe Xie", userService.getUserMetaUsingUid(user).get("displayName"));
  }

  /** Test case: tests the updateProfileByAdmin(...) method. Test data: an over-long display name. Expected: the update fails and personal profile fields are left untouched. */
  @Test
  public void testUpdateProfileByAdminWithIllegalDisplayName() {
    User user = userService.getUserUsingUid(1000);
    String location = (String) userService.getUserMetaUsingUid(user).get("location");
    StringBuilder displayName = new StringBuilder();
    for (int i = 0; i < 65; ++i) {
      displayName.append('x');
    }
    Map<String, Boolean> result =
        userService.updateProfileByAdmin(user, "cshzxie@gmail.com", displayName.toString(), "Bio");
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
            user, "changed@example.com", "Haozhe Xie", "", "", "{}", "About me");
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
            user, user.getEmail(), "Haozhe Xie", "Beijing", "", "{}", "About me");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNull(result.get("isEmailVerificationSent"));
    Assertions.assertTrue(userService.getUserUsingUid(1000).isEmailVerified());
  }

  /** Enables the requireEmailVerification option within the current test transaction. */
  private void enableEmailVerificationRequirement() {
    Option option = optionMapper.getOption("requireEmailVerification");
    option.setOptionValue("1");
    optionMapper.updateOption(option);
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

    userService.deleteUser(user.getUid());
    Assertions.assertNull(userService.getUserUsingUid(user.getUid()));
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

  /** The Mapper used to construct email validation credentials within the test transaction. */
  @Autowired private EmailValidationMapper emailValidationMapper;

  /** The Mapper used to toggle the email-verification requirement within the test transaction. */
  @Autowired private OptionMapper optionMapper;
}
