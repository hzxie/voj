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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.EmailValidationMapper;
import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.UserGroupMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.mapper.UserMetaMapper;
import org.verwandlung.voj.web.model.EmailValidation;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.UserMeta;
import org.verwandlung.voj.web.util.DigestUtils;
import org.verwandlung.voj.web.util.HtmlTextFilter;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.MailSender;
import org.verwandlung.voj.web.util.OffensiveWordFilter;

/**
 * The business logic layer of the User class.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class UserService {
  /** The number of days an email validation credential remains valid after it is issued. */
  private static final long EMAIL_VALIDATION_VALID_DAYS = 1;

  /** The length of the rolling rate-limit window for verification / password-reset emails. */
  private static final long EMAIL_RATE_LIMIT_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(1);

  /**
   * The fallback maximum number of verification / password-reset emails sent to one address per
   * {@link #EMAIL_RATE_LIMIT_WINDOW_MILLIS} window, used when the {@code emailDailyLimit} option is
   * missing or malformed.
   */
  private static final int DEFAULT_EMAIL_DAILY_LIMIT = 3;

  /**
   * Gets a user object by its unique identifier.
   *
   * @param userId - the unique identifier of the user
   * @return the expected user object, or a null reference
   */
  public User getUserUsingUid(long userId) {
    return userMapper.getUserUsingUid(userId);
  }

  /**
   * [For administrators only] Bans a user by moving them to the {@code forbidden} user group, which
   * blocks login and posting. Used by the moderation review page.
   *
   * @param userId - the unique identifier of the user to ban
   * @return a Map containing whether the ban was successful
   */
  public Map<String, Boolean> banUser(long userId) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    User user = userMapper.getUserUsingUid(userId);
    UserGroup forbidden = userGroupMapper.getUserGroupUsingSlug("forbidden");
    boolean isSuccessful = user != null && forbidden != null;
    if (isSuccessful) {
      user.setUserGroup(forbidden);
      userMapper.updateUser(user);
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Gets the registration time of a batch of users, keyed by user identifier. Reads the {@code
   * registerTime} user-meta entry (stored as {@code yyyy-MM-dd HH:mm:ss}) for every requested user in
   * a single query; users without a parseable registration time are omitted from the result. Backs
   * the admin user list's JOINED column.
   *
   * @param uids - the user identifiers to look up
   * @return a map from user identifier to registration date
   */
  public Map<Long, Date> getRegisterTimes(List<Long> uids) {
    Map<Long, Date> registerTimes = new HashMap<>(uids.size() * 4 / 3 + 1);
    if (uids.isEmpty()) {
      return registerTimes;
    }
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    List<Map<String, Object>> rows =
        userMetaMapper.getUserMetaValuesUsingMetaKey(uids, "registerTime");
    for (Map<String, Object> row : rows) {
      long uid = ((Number) row.get("uid")).longValue();
      Object metaValue = row.get("metaValue");
      if (metaValue == null) {
        continue;
      }
      try {
        registerTimes.put(uid, formatter.parse(metaValue.toString()));
      } catch (java.text.ParseException e) {
        // Ignore unparseable registration times; the JOINED cell will simply be blank.
      }
    }
    return registerTimes;
  }

  /**
   * Gets the list of users in a user group.
   *
   * @param userGroup - the user group object the users belong to
   * @param offset - the starting number of the user identifier
   * @param limit - the number of users to fetch
   * @return the list of users
   */
  public List<User> getUserUsingUserGroup(UserGroup userGroup, long offset, int limit) {
    return userMapper.getUserUsingUserGroup(userGroup, offset, limit);
  }

  /**
   * Gets the meta information of a user.
   *
   * @param user - the user object the meta information corresponds to
   * @return the key-value pairs of the user's meta information
   */
  public Map<String, Object> getUserMetaUsingUid(User user) {
    Map<String, Object> userMetaMap = new HashMap<>();
    if (user == null) {
      return userMetaMap;
    }
    List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
    for (UserMeta userMeta : userMetaList) {
      String key = userMeta.getMetaKey();
      Object value = userMeta.getMetaValue();

      if ("socialLinks".equals(key)) {
        value = JsonUtils.toMap((String) value);
      } else if (CENSORED_PROFILE_FIELDS.contains(key)) {
        // Censor the displayable prose fields at read time so refreshed dictionaries and the
        // auto-censor option take effect retroactively. URL fields (website, socialLinks) are left
        // untouched, as masking substrings would break the links.
        value = offensiveWordFilter.censor((String) value);
      }
      userMetaMap.put(key, value);
    }
    // Ensure socialLinks is always present as a (possibly empty) map. New users
    // have no socialLinks meta row, leaving the attribute null; the JSP views
    // tolerated indexing a null map, but Thymeleaf's SpEL throws EL1012E
    // ("Cannot index into a null value"). Defaulting to an empty map keeps
    // socialLinks['Facebook'] et al. resolving to null as the templates expect.
    userMetaMap.putIfAbsent("socialLinks", new HashMap<String, Object>());
    return userMetaMap;
  }

  /**
   * Gets a user object by username or email address.
   *
   * @param username - the username or email address
   * @return a User object, or a null reference
   */
  public User getUserUsingUsernameOrEmail(String username) {
    boolean isUsingEmail = username.indexOf('@') != -1;
    User user = null;

    if (!isUsingEmail) {
      user = userMapper.getUserUsingUsername(username);
    } else {
      user = userMapper.getUserUsingEmail(username);
    }
    return user;
  }

  /**
   * Verifies whether the user's identity is valid.
   *
   * @param username - the username or email address
   * @param password - the plain-text password
   * @return a Map<String, Boolean> object containing the login verification result
   */
  public Map<String, Boolean> isAllowedToLogin(String username, String password) {
    Map<String, Boolean> result = new HashMap<>(6, 1);
    result.put("isUsernameEmpty", username.isEmpty());
    result.put("isPasswordEmpty", password.isEmpty());
    result.put("isAccountValid", false);
    result.put("isAllowedToAccess", false);
    result.put("isSuccessful", false);

    if (!result.get("isUsernameEmpty") && !result.get("isPasswordEmpty")) {
      User user = getUserUsingUsernameOrEmail(username);
      if (user != null && passwordEncoder.matches(password, user.getPassword())) {
        upgradePasswordEncodingIfNeeded(user, password);
        result.put("isAccountValid", true);
        if (isAllowedToAccess(user.getUserGroup())) {
          result.put("isAllowedToAccess", true);
          result.put("isSuccessful", true);
        }
      }
    }
    return result;
  }

  /**
   * Re-encodes the user's password with the preferred encoder (bcrypt) when the stored value uses a
   * weaker, legacy encoding (e.g. a bare MD5 digest). Called after a successful credential match.
   *
   * @param user - the authenticated user
   * @param rawPassword - the plain-text password the user just authenticated with
   */
  private void upgradePasswordEncodingIfNeeded(User user, String rawPassword) {
    if (passwordEncoder.upgradeEncoding(user.getPassword())) {
      user.setPassword(passwordEncoder.encode(rawPassword));
      userMapper.updateUser(user);
    }
  }

  /**
   * Verifies whether the user is allowed to log in.
   *
   * @param userGroup - the object of the user group the user belongs to
   * @return whether the user is allowed to log in
   */
  private boolean isAllowedToAccess(UserGroup userGroup) {
    String[] allowedUserGroups = {"users", "administrators"};
    for (String allowedUserGroup : allowedUserGroups) {
      if (allowedUserGroup.equals(userGroup.getUserGroupSlug())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates the account and creates a user.
   *
   * @param username - the username
   * @param password - the password (not encrypted with MD5)
   * @param email - the email address
   * @param userGroupSlug - the alias of the user group
   * @param languageSlug - the alias of the preferred language
   * @param isAllowRegister - whether the system allows new user registration
   * @return a Map<String, Boolean> object containing the account creation result
   */
  public Map<String, Boolean> createUser(
      String username,
      String password,
      String email,
      String userGroupSlug,
      String languageSlug,
      boolean isAllowRegister) {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
    Language languagePreference = languageMapper.getLanguageUsingSlug(languageSlug);
    User user =
        new User(username, passwordEncoder.encode(password), email, userGroup, languagePreference);

    Map<String, Boolean> result = getUserCreationResult(user, password, isAllowRegister);
    if (result.get("isSuccessful")) {
      userMapper.createUser(user);
      createUserMeta(user);
    }
    return result;
  }

  /**
   * [For administrators only] Validates the account and creates a user.
   *
   * @param username - the username
   * @param password - the password (not encrypted with MD5)
   * @param email - the email address
   * @param userGroupSlug - the alias of the user group
   * @param languageSlug - the alias of the preferred language
   * @return a Map<String, Boolean> object containing the account creation result
   */
  public Map<String, Boolean> createUser(
      String username, String password, String email, String userGroupSlug, String languageSlug) {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
    Language languagePreference = languageMapper.getLanguageUsingSlug(languageSlug);
    User user =
        new User(username, passwordEncoder.encode(password), email, userGroup, languagePreference);

    Map<String, Boolean> result = getUserCreationResult(user, password, true);
    if (result.get("isSuccessful")) {
      userMapper.createUser(user);
      createUserMeta(user);
    }
    return result;
  }

  /**
   * Creates a new user (from the admin console) and, when supplied, seeds the optional display name
   * and bio. Both extras are HTML-filtered and only persisted when non-empty.
   *
   * @param username - the username
   * @param password - the password (not encrypted with MD5)
   * @param email - the email address
   * @param userGroupSlug - the alias of the user group
   * @param languageSlug - the alias of the preferred language
   * @param displayName - the optional display name
   * @param aboutMe - the optional personal bio
   * @return a Map<String, Boolean> object containing the account creation result
   */
  public Map<String, Boolean> createUser(
      String username,
      String password,
      String email,
      String userGroupSlug,
      String languageSlug,
      String displayName,
      String aboutMe) {
    Map<String, Boolean> result =
        createUser(username, password, email, userGroupSlug, languageSlug);
    if (result.get("isSuccessful")) {
      User user = userMapper.getUserUsingUsername(username);
      updateUserMeta(user, "displayName", HtmlTextFilter.filter(displayName));
      updateUserMeta(user, "aboutMe", HtmlTextFilter.filter(aboutMe));
    }
    return result;
  }

  /**
   * Creates the user meta information.
   *
   * @param user - the corresponding user object
   */
  private void createUserMeta(User user) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Calendar calendar = Calendar.getInstance();
    String registerTime = formatter.format(calendar.getTime());
    UserMeta registerTimeMeta = new UserMeta(user, "registerTime", registerTime);
    userMetaMapper.createUserMeta(registerTimeMeta);
  }

  /**
   * Validates the legality of the user information to be created.
   *
   * @param user - the User object to create
   * @param password - the password (not encrypted with MD5)
   * @param isAllowRegister - whether the system allows new user registration
   * @return a Map<String, Boolean> object containing the account information validation result
   */
  private Map<String, Boolean> getUserCreationResult(
      User user, String password, boolean isAllowRegister) {
    Map<String, Boolean> result = new HashMap<>(13, 1);
    result.put("isUsernameEmpty", user.getUsername().isEmpty());
    result.put("isUsernameLegal", isUsernameLegal(user.getUsername()));
    result.put("isUsernameExists", isUsernameExists(user.getUsername()));
    result.put("isPasswordEmpty", password.isEmpty());
    result.put("isPasswordLegal", isPasswordLegal(password));
    result.put("isEmailEmpty", user.getEmail().isEmpty());
    result.put("isEmailLegal", isEmailLegal(user.getEmail()));
    result.put("isEmailExists", isEmailExists(user.getEmail()));
    result.put("isUserGroupLegal", user.getUserGroup() != null);
    result.put("isLanguageLegal", user.getPreferLanguage() != null);
    result.put("isAllowRegister", isAllowRegister);

    boolean isSuccessful =
        !result.get("isUsernameEmpty")
            && result.get("isUsernameLegal")
            && !result.get("isUsernameExists")
            && !result.get("isPasswordEmpty")
            && result.get("isPasswordLegal")
            && !result.get("isEmailEmpty")
            && result.get("isEmailLegal")
            && !result.get("isEmailExists")
            && result.get("isUserGroupLegal")
            && result.get("isLanguageLegal")
            && result.get("isAllowRegister");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Checks whether the email validation credential is valid.
   *
   * @param email - the user's email address
   * @param token - the token used for validation
   * @return whether the email validation credential is valid
   */
  public boolean isEmailValidationValid(String email, String token) {
    EmailValidation emailValidation = emailValidationMapper.getEmailValidation(email);
    Date now = new Date();

    if (emailValidation != null
        && emailValidation.getToken().equals(token)
        && now.before(emailValidation.getExpireTime())) {
      return true;
    }
    return false;
  }

  /**
   * Validates the account and sends a password reset email.
   *
   * @param username - the user's username
   * @param email - the user's email address
   * @return a Map<String, Boolean> object containing the account validation result
   */
  public Map<String, Boolean> sendVerificationEmail(String username, String email) {
    boolean isSuccessful = true;
    boolean isUserExists = false;
    Map<String, Boolean> result = new HashMap<>(4, 1);

    User user = userMapper.getUserUsingUsername(username);
    if (user != null && user.getEmail().equals(email)) {
      isUserExists = true;
      try {
        dispatchVerificationEmail(username, email);
      } catch (Exception e) {
        e.printStackTrace();
        isSuccessful = false;
      }
    }
    result.put("isUserExists", isUserExists);
    result.put("isSuccessful", isSuccessful && isUserExists);

    return result;
  }

  /**
   * Sends the password reset email.
   *
   * @param username - the user's username
   * @param email - the user's email address
   */
  private void dispatchVerificationEmail(String username, String email) {
    String token = DigestUtils.getGuid();
    // Rate-limit: skip the send once the address has reached its daily email limit, so the endpoint
    // cannot be abused to flood an inbox with reset emails.
    EmailValidation emailValidation = nextEmailValidation(email, token);
    if (emailValidation == null) {
      return;
    }
    Map<String, Object> model = new HashMap<>(4, 1);
    model.put("username", username);
    model.put("email", email);
    model.put("token", token);

    emailValidationMapper.deleteEmailValidation(email);
    emailValidationMapper.createEmailValidation(emailValidation);

    String templateName = "mail/reset-password";
    String subject = "Password Reset Request";
    String body = mailSender.getMailContent(templateName, model);
    mailSender.sendMail(email, subject, body);
  }

  /**
   * Checks whether email verification is currently required, i.e. whether the {@code
   * requireEmailVerification} option is enabled.
   *
   * @return whether email verification is required
   */
  public boolean isEmailVerificationRequired() {
    Option option = optionMapper.getOption("requireEmailVerification");
    return option != null && "1".equals(option.getOptionValue());
  }

  /**
   * Marks a freshly-registered user as unverified and emails them a verification link. Used by the
   * registration flow when the {@code requireEmailVerification} option is enabled.
   *
   * @param user - the newly-created user
   */
  public void requireEmailVerification(User user) {
    userMapper.updateEmailVerified(user.getUid(), false);
    user.setEmailVerified(false);
    try {
      dispatchEmailVerificationEmail(user.getUsername(), user.getEmail());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Validates an email-verification token and, when valid, marks the user's email as verified.
   *
   * @param email - the user's email address
   * @param token - the token from the verification link
   * @return a Map object containing the verification result
   */
  public Map<String, Boolean> verifyEmail(String email, String token) {
    boolean isTokenValid = email != null && token != null && isEmailValidationValid(email, token);
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isTokenValid", isTokenValid);

    if (isTokenValid) {
      User user = userMapper.getUserUsingEmail(email);
      if (user != null) {
        userMapper.updateEmailVerified(user.getUid(), true);
      }
      emailValidationMapper.deleteEmailValidation(email);
    }
    result.put("isSuccessful", isTokenValid);
    return result;
  }

  /**
   * Sends the email-verification email containing the confirmation link.
   *
   * @param username - the user's username
   * @param email - the user's email address
   */
  private void dispatchEmailVerificationEmail(String username, String email) {
    String token = DigestUtils.getGuid();
    // Rate-limit: skip the send once the address has reached its daily email limit, so repeated
    // registrations / email changes cannot flood an inbox with verifications.
    EmailValidation emailValidation = nextEmailValidation(email, token);
    if (emailValidation == null) {
      return;
    }
    Map<String, Object> model = new HashMap<>(4, 1);
    model.put("username", username);
    model.put("email", email);
    model.put("token", token);

    emailValidationMapper.deleteEmailValidation(email);
    emailValidationMapper.createEmailValidation(emailValidation);

    String body = mailSender.getMailContent("mail/verify-email", model);
    mailSender.sendMail(email, "Verify Your Email Address", body);
  }

  /**
   * Gets the expiration time of the email validation token.
   *
   * @return the expiration time of the email validation token
   */
  private Date getExpireTime() {
    Date date = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, (int) EMAIL_VALIDATION_VALID_DAYS);
    date = calendar.getTime();

    return date;
  }

  /**
   * Builds the email validation credential to persist for a new verification / password-reset email,
   * carrying forward the per-address send counter, or returns {@code null} when the address has
   * already reached its daily email limit within the current rate-limit window. A {@code null}
   * return tells the caller to silently skip the send so the email endpoints cannot be abused to
   * flood an inbox.
   *
   * @param email - the email address
   * @param token - the freshly generated validation token
   * @return the credential to persist, or null if the daily limit has been reached
   */
  private EmailValidation nextEmailValidation(String email, String token) {
    EmailValidation existing = emailValidationMapper.getEmailValidation(email);
    long now = System.currentTimeMillis();

    int dailyCount;
    Date windowStart;
    boolean withinWindow =
        existing != null
            && existing.getWindowStart() != null
            && now - existing.getWindowStart().getTime() < EMAIL_RATE_LIMIT_WINDOW_MILLIS;
    if (withinWindow) {
      if (existing.getDailyCount() >= getEmailDailyLimit()) {
        return null;
      }
      dailyCount = existing.getDailyCount() + 1;
      windowStart = existing.getWindowStart();
    } else {
      dailyCount = 1;
      windowStart = new Date(now);
    }
    return new EmailValidation(email, token, getExpireTime(), dailyCount, windowStart);
  }

  /**
   * Resolves the configured maximum number of verification / password-reset emails per address per
   * rate-limit window, falling back to {@link #DEFAULT_EMAIL_DAILY_LIMIT} when the {@code
   * emailDailyLimit} option is missing or not a positive integer.
   *
   * @return the maximum number of emails allowed per address per window
   */
  private int getEmailDailyLimit() {
    Option option = optionMapper.getOption("emailDailyLimit");
    if (option != null) {
      try {
        int limit = Integer.parseInt(option.getOptionValue().trim());
        if (limit > 0) {
          return limit;
        }
      } catch (NumberFormatException e) {
        // Fall through to the default when the stored value is malformed.
      }
    }
    return DEFAULT_EMAIL_DAILY_LIMIT;
  }

  /**
   * Validates the data and resets the user's password.
   *
   * @param email - the user's email address
   * @param token - the token used for validation
   * @param newPassword - the new password
   * @param confirmPassword - the confirmation of the new password
   * @return a Map<String, Boolean> object containing the password reset result
   */
  public Map<String, Boolean> resetPassword(
      String email, String token, String newPassword, String confirmPassword) {
    boolean isEmailValidationValid = isEmailValidationValid(email, token);
    Map<String, Boolean> result =
        getResetPasswordResult(newPassword, confirmPassword, isEmailValidationValid);

    if (result.get("isSuccessful")) {
      emailValidationMapper.deleteEmailValidation(email);

      User user = userMapper.getUserUsingEmail(email);
      user.setPassword(passwordEncoder.encode(newPassword));
      userMapper.updateUser(user);
    }
    return result;
  }

  /**
   * Validates the reset-password data.
   *
   * @param newPassword - the new password
   * @param confirmPassword - the confirmation of the new password
   * @param isEmailValidationValid - whether the email validation credential is valid
   * @return a Map<String, Boolean> object containing the password reset result
   */
  private Map<String, Boolean> getResetPasswordResult(
      String newPassword, String confirmPassword, boolean isEmailValidationValid) {
    Map<String, Boolean> result = new HashMap<>(7, 1);
    result.put("isEmailValidationValid", isEmailValidationValid);
    result.put("isNewPasswordEmpty", newPassword.isEmpty());
    result.put("isNewPasswordLegal", isPasswordLegal(newPassword));
    result.put("isConfirmPasswordMatched", newPassword.equals(confirmPassword));

    boolean isSuccessful =
        result.get("isEmailValidationValid")
            && !result.get("isNewPasswordEmpty")
            && result.get("isNewPasswordLegal")
            && result.get("isConfirmPasswordMatched");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Validates the programming language preference and changes it.
   *
   * @param user - the user object whose programming language preference is to be changed
   * @param preferLanguageSlug - the alias of the programming language
   * @return a Map<String, Boolean> object containing the programming language preference change
   *     result
   */
  public Map<String, Boolean> changePreferLanguage(User user, String preferLanguageSlug) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    Language preferLanguage = languageMapper.getLanguageUsingSlug(preferLanguageSlug);

    if (preferLanguage != null) {
      user.setPreferLanguage(preferLanguage);
      userMapper.updateUser(user);
    }
    result.put("isSuccessful", preferLanguage != null);
    return result;
  }

  /**
   * Validates the old password and changes the password.
   *
   * @param user - the user object whose password is to be changed
   * @param oldPassword - the old password
   * @param newPassword - the new password
   * @param confirmPassword - the confirmation of the new password
   * @return a Map<String, Boolean> object containing the password validation result
   */
  public Map<String, Boolean> changePassword(
      User user, String oldPassword, String newPassword, String confirmPassword) {
    Map<String, Boolean> result =
        getChangePasswordResult(user, oldPassword, newPassword, confirmPassword);

    if (result.get("isSuccessful")) {
      user.setPassword(passwordEncoder.encode(newPassword));
      userMapper.updateUser(user);
    }
    return result;
  }

  /**
   * Validates the correctness of the old password and the legality of the new password.
   *
   * @param user - the user object whose password is to be changed
   * @param oldPassword - the old password
   * @param newPassword - the new password
   * @param confirmPassword - the confirmation of the new password
   * @return a Map<String, Boolean> object containing the password validation result
   */
  private Map<String, Boolean> getChangePasswordResult(
      User user, String oldPassword, String newPassword, String confirmPassword) {
    Map<String, Boolean> result = new HashMap<>(5, 1);
    result.put("isOldPasswordCorrect", isOldPasswordCorrect(user.getPassword(), oldPassword));
    result.put("isNewPasswordEmpty", newPassword.isEmpty());
    result.put("isNewPasswordLegal", isPasswordLegal(newPassword));
    result.put("isConfirmPasswordMatched", newPassword.equals(confirmPassword));

    boolean isSuccessful =
        result.get("isOldPasswordCorrect")
            && !result.get("isNewPasswordEmpty")
            && result.get("isNewPasswordLegal")
            && result.get("isConfirmPasswordMatched");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Validates the new profile and updates the personal profile.
   *
   * @param user - the user whose profile is to be changed
   * @param email - the user's email address
   * @param displayName - the user's display name
   * @param location - the user's location
   * @param website - the user's personal homepage
   * @param socialLinks - the user's social network information
   * @param aboutMe - the user's personal bio
   * @return a Map<String, Boolean> object containing the personal profile update result
   */
  public Map<String, Boolean> updateProfile(
      User user,
      String email,
      String displayName,
      String location,
      String website,
      String socialLinks,
      String aboutMe) {
    displayName = HtmlTextFilter.filter(displayName);
    location = HtmlTextFilter.filter(location);
    website = HtmlTextFilter.filter(website);
    socialLinks = HtmlTextFilter.filter(socialLinks);
    aboutMe = HtmlTextFilter.filter(aboutMe);
    Map<String, Boolean> result =
        getUpdateProfileResult(user, email, displayName, location, website, socialLinks, aboutMe);

    if (result.get("isSuccessful")) {
      boolean isEmailChanged = !email.equals(user.getEmail());
      user.setEmail(email);
      userMapper.updateUser(user);

      updateUserMeta(user, "displayName", displayName);
      updateUserMeta(user, "location", location);
      updateUserMeta(user, "website", website);
      updateUserMeta(user, "socialLinks", socialLinks);
      updateUserMeta(user, "aboutMe", aboutMe);

      // A changed email address has not been verified yet. Mirror the registration flow and force
      // re-verification so a previously verified account cannot silently switch to an unverified
      // address and keep its verified privileges.
      if (isEmailChanged && isEmailVerificationRequired()) {
        requireEmailVerification(user);
        result.put("isEmailVerificationSent", true);
      }
    }
    return result;
  }

  /**
   * Validates and updates the subset of a user's profile that an administrator manages from the
   * admin console: the email address, the display name and the bio. Personal-profile fields the
   * admin editor does not surface (location, website, social links) are intentionally left
   * untouched so that re-saving here does not wipe data the user maintains on their own dashboard.
   *
   * @param user - the user whose profile is to be changed
   * @param email - the user's email address
   * @param displayName - the user's display name
   * @param aboutMe - the user's personal bio
   * @return a Map<String, Boolean> object containing the personal profile update result
   */
  public Map<String, Boolean> updateProfileByAdmin(
      User user, String email, String displayName, String aboutMe) {
    displayName = HtmlTextFilter.filter(displayName);
    aboutMe = HtmlTextFilter.filter(aboutMe);

    Map<String, Boolean> result = new HashMap<>(6, 1);
    result.put("isEmailEmpty", email.isEmpty());
    result.put("isEmailLegal", isEmailLegal(email));
    result.put("isEmailExists", isEmailExists(user.getEmail(), email));
    result.put("isDisplayNameLegal", displayName.length() <= 64);
    result.put("isAboutMeLegal", aboutMe.length() <= 256);

    boolean isSuccessful =
        !result.get("isEmailEmpty")
            && result.get("isEmailLegal")
            && !result.get("isEmailExists")
            && result.get("isDisplayNameLegal")
            && result.get("isAboutMeLegal");
    result.put("isSuccessful", isSuccessful);

    if (isSuccessful) {
      boolean isEmailChanged = !email.equals(user.getEmail());
      user.setEmail(email);
      userMapper.updateUser(user);

      updateUserMeta(user, "displayName", displayName);
      updateUserMeta(user, "aboutMe", aboutMe);

      // A changed email address has not been verified yet. Force re-verification so the account
      // cannot keep its verified privileges on an unverified address.
      if (isEmailChanged && isEmailVerificationRequired()) {
        requireEmailVerification(user);
      }
    }
    return result;
  }

  /**
   * Validates the new profile.
   *
   * @param user - the user whose profile is to be changed
   * @param email - the user's email address
   * @param displayName - the user's display name
   * @param location - the user's location
   * @param website - the user's personal homepage
   * @param socialLinks - the user's social network information
   * @param aboutMe - the user's personal bio
   * @return a Map<String, Boolean> object containing the personal profile update result
   */
  private Map<String, Boolean> getUpdateProfileResult(
      User user,
      String email,
      String displayName,
      String location,
      String website,
      String socialLinks,
      String aboutMe) {
    Map<String, Boolean> result = new HashMap<>(8, 1);
    result.put("isEmailEmpty", email.isEmpty());
    result.put("isEmailLegal", isEmailLegal(email));
    result.put("isEmailExists", isEmailExists(user.getEmail(), email));
    result.put("isDisplayNameLegal", displayName.length() <= 64);
    result.put("isLocationLegal", location.length() <= 128);
    result.put("isWebsiteLegal", isWebsiteLegal(website));
    result.put("isAboutMeLegal", aboutMe.length() <= 256);

    boolean isSuccessful =
        !result.get("isEmailEmpty")
            && result.get("isEmailLegal")
            && !result.get("isEmailExists")
            && result.get("isDisplayNameLegal")
            && result.get("isLocationLegal")
            && result.get("isWebsiteLegal")
            && result.get("isAboutMeLegal");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Updates the user meta information.
   *
   * @param user - the user whose meta information is to be updated
   * @param metaKey - the key of the meta information
   * @param metaValue - the value of the meta information
   */
  private void updateUserMeta(User user, String metaKey, String metaValue) {
    UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, metaKey);

    if (userMeta == null) {
      if (metaValue.isEmpty()) {
        return;
      }
      userMeta = new UserMeta(user, metaKey, metaValue);
      userMetaMapper.createUserMeta(userMeta);
    } else {
      userMeta.setMetaValue(metaValue);
      userMetaMapper.updateUserMeta(userMeta);
    }
  }

  /**
   * Validates the legality of a username. Rule: the username should consist of [A-Za-z0-9_], start
   * with a letter, and be 6-16 characters long.
   *
   * @param username - the username
   * @return whether the username is legal
   */
  private boolean isUsernameLegal(String username) {
    return username.matches("^[A-Za-z][A-Za-z0-9_]{5,15}$");
  }

  /**
   * Checks whether a username exists.
   *
   * @param username - the username
   * @return whether the username exists
   */
  private boolean isUsernameExists(String username) {
    User user = userMapper.getUserUsingUsername(username);
    return user != null;
  }

  /**
   * Checks whether a password is legal. Rule: the password should be 6-16 characters long.
   *
   * @param password - the password (not encrypted with MD5)
   * @return whether the password is legal
   */
  private boolean isPasswordLegal(String password) {
    int passwordLength = password.length();
    return passwordLength >= 6 && passwordLength <= 16;
  }

  /**
   * Verifies whether the user's old password is correct when changing the password.
   *
   * @param oldPassword - the user's stored (encoded) password
   * @param submitedPassword - the plain-text old password submitted for verification
   * @return whether the user's old password is correct
   */
  private boolean isOldPasswordCorrect(String oldPassword, String submitedPassword) {
    if (submitedPassword.isEmpty()) {
      return true;
    }
    return passwordEncoder.matches(submitedPassword, oldPassword);
  }

  /**
   * Checks whether an email address is legal. Rule: a valid email address not exceeding 64
   * characters.
   *
   * @param email - the email address
   * @return whether the email address is legal
   */
  private boolean isEmailLegal(String email) {
    int emailLength = email.length();
    return emailLength <= 64
        && email.matches("^[A-Za-z0-9\\._-]+@[A-Za-z0-9_-]+\\.[A-Za-z0-9\\._-]+$");
  }

  /**
   * Checks whether an email address exists. Note: only used when a user creates a new account.
   *
   * @param email - the email address
   * @return whether the email address exists
   */
  private boolean isEmailExists(String email) {
    User user = userMapper.getUserUsingEmail(email);
    return user != null;
  }

  /**
   * Checks whether an email address exists. Note: only used when a user edits their profile.
   *
   * @param currentEmail - the email address previously used
   * @param email - the email address to update to
   * @return whether the email address exists
   */
  private boolean isEmailExists(String currentEmail, String email) {
    if (currentEmail.equals(email)) {
      return false;
    }
    User user = userMapper.getUserUsingEmail(email);
    return user != null;
  }

  /**
   * Checks whether a personal homepage address is legal. Rule: a valid HTTP(S) protocol URL not
   * exceeding 64 characters.
   *
   * @param website - the personal homepage address
   * @return whether the personal homepage address is legal
   */
  private boolean isWebsiteLegal(String website) {
    int websiteLength = website.length();
    return website.isEmpty()
        || (websiteLength <= 64
            && website.matches("^(http|https):\\/\\/[A-Za-z0-9-]+\\.[A-Za-z0-9_.]+$"));
  }

  /**
   * Gets a user group object by its alias.
   *
   * @param userGroupSlug - the alias of the user group
   * @return the user group object, or a null reference
   */
  public UserGroup getUserGroupUsingSlug(String userGroupSlug) {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
    return userGroup;
  }

  /**
   * [For administrators only] Gets all user group objects.
   *
   * @return a list of all user group objects
   */
  public List<UserGroup> getUserGroups() {
    List<UserGroup> userGroups = userGroupMapper.getUserGroups();
    return userGroups;
  }

  /**
   * [For administrators only] Gets the total number of registered users in the system.
   *
   * @param userGroup - the user group object the users belong to
   * @return the total number of registered users in the system
   */
  public long getNumberOfUsers(UserGroup userGroup) {
    return userMapper.getNumberOfUsersUsingUserGroup(userGroup);
  }

  /**
   * [For administrators only] Gets the number of users registered today.
   *
   * @return the number of users registered today
   */
  public long getNumberOfUserRegisteredToday() {
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int date = calendar.get(Calendar.DAY_OF_MONTH);

    calendar.set(year, month, date, 0, 0, 0);
    Date startTime = calendar.getTime();
    calendar.set(year, month, date, 23, 59, 59);
    Date endTime = calendar.getTime();

    return userMetaMapper.getNumberOfUserRegistered(startTime, endTime);
  }

  /**
   * [For administrators only] Gets the total number of matching users by user group and username.
   *
   * @param userGroup - the user group object
   * @param username - part or all of the username
   * @return the total number of users in the user group whose username contains the given string
   */
  public long getNumberOfUsersUsingUserGroupAndUsername(UserGroup userGroup, String username) {
    return userMapper.getNumberOfUsersUsingUserGroupAndUsername(userGroup, username);
  }

  /**
   * [For administrators only] Filters user objects by user group and username.
   *
   * @param userGroup - the user group object
   * @param username - part or all of the username
   * @param offset - the starting number of the user identifier
   * @param limit - the number of users to fetch
   * @return the list of matching users
   */
  public List<User> getUserUsingUserGroupAndUsername(
      UserGroup userGroup, String username, long offset, int limit) {
    return userMapper.getUserUsingUserGroupAndUsername(userGroup, username, offset, limit);
  }

  /**
   * [For administrators only] Updates a user's profile.
   *
   * @param user - the user whose profile is to be changed
   * @param password - the user's password
   * @param userGroupSlug - the alias of the user group the user belongs to
   * @param preferLanguageSlug - the alias of the user's preferred programming language
   * @return a Map<String, Boolean> object containing the user profile update result
   */
  public Map<String, Boolean> updateProfile(
      User user, String password, String userGroupSlug, String preferLanguageSlug) {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
    Language preferLanguage = languageMapper.getLanguageUsingSlug(preferLanguageSlug);
    Map<String, Boolean> result = getUpdateProfileResult(password, userGroup, preferLanguage);

    if (result.get("isSuccessful")) {
      if (!password.isEmpty()) {
        user.setPassword(passwordEncoder.encode(password));
      }
      user.setUserGroup(userGroup);
      user.setPreferLanguage(preferLanguage);
      userMapper.updateUser(user);
    }
    return result;
  }

  /**
   * [For administrators only] Changes a user's basic information.
   *
   * @param password - the user's password
   * @param userGroup - the user group the user belongs to
   * @param preferLanguage - the user's preferred programming language
   * @return a Map<String, Boolean> object containing the user profile update result
   */
  private Map<String, Boolean> getUpdateProfileResult(
      String password, UserGroup userGroup, Language preferLanguage) {
    Map<String, Boolean> result = new HashMap<>(6, 1);
    result.put("isPasswordEmpty", password.isEmpty());
    result.put("isPasswordLegal", isPasswordLegal(password));
    result.put("isUserGroupLegal", userGroup != null);
    result.put("isPreferLanguageLegal", preferLanguage != null);

    boolean isSuccessful =
        result.get("isPasswordEmpty") ^ result.get("isPasswordLegal")
            && result.get("isUserGroupLegal")
            && result.get("isPreferLanguageLegal");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * [For administrators only] Deletes a user by their unique identifier.
   *
   * @param uid - the unique identifier of the user
   */
  public void deleteUser(long uid) {
    userMapper.deleteUser(uid);
  }

  /** The autowired UserMapper object, used to obtain basic user information. */
  @Autowired private UserMapper userMapper;

  /** The autowired UserMetaMapper object, used to obtain user meta information. */
  @Autowired private UserMetaMapper userMetaMapper;

  /** The autowired UserGroupMapper object, used to obtain user group information. */
  @Autowired private UserGroupMapper userGroupMapper;

  /** The autowired LanguageMapper object, used to load the language preferences on the registration page. */
  @Autowired private LanguageMapper languageMapper;

  /** The autowired EmailValidationMapper object, used to generate password reset tokens. */
  @Autowired private EmailValidationMapper emailValidationMapper;

  /** The autowired OptionMapper object, used to resolve the email-verification requirement. */
  @Autowired private OptionMapper optionMapper;

  /** The autowired password encoder, used to encode and verify user passwords. */
  @Autowired private PasswordEncoder passwordEncoder;

  /** The autowired OffensiveWordFilter object, used to filter offensive words in user content. */
  @Autowired private OffensiveWordFilter offensiveWordFilter;

  /**
   * The displayable, prose profile meta fields that are censored at read time. URL fields (website,
   * socialLinks) are deliberately excluded, as masking substrings would break the links.
   */
  private static final Set<String> CENSORED_PROFILE_FIELDS =
      new HashSet<>(Arrays.asList("displayName", "location", "aboutMe"));

  /** The autowired MailSender object, used to send emails to users' mailboxes. */
  @Autowired
  @Qualifier("vojMailSender")
  private MailSender mailSender;
}
