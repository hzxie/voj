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

import java.util.Calendar;
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
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.model.EmailValidation;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * UserService测试类.
 *
 * <p>种子数据: 用户1000 zjhzxhz (管理员, 邮箱cshzxie@gmail.com), 用户1001 voj@judger (评测者, 邮箱support@verwandlung.org),
 * 用户1002 another-user (普通用户, 邮箱noreply@verwandlung.org); 三者密码哈希相同. 用户组: forbidden/users/judgers/administrators.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserServiceTest {
  /** 用户1000的密码哈希 (用于免去明文密码的登录验证). */
  private static final String PASSWORD_HASH = "785ee107c11dfe36de668b1ae7baacbb";

  /** 测试用例: 测试getUserUsingUid(long)方法 测试数据: 存在/不存在的用户标识符 预期结果: 返回对应用户或空引用 */
  @Test
  public void testGetUserUsingUid() {
    Assertions.assertNotNull(userService.getUserUsingUid(1000));
    Assertions.assertNull(userService.getUserUsingUid(0));
  }

  /** 测试用例: 测试getUserUsingUsernameOrEmail(String)方法 测试数据: 用户名/邮箱/不存在 预期结果: 返回对应用户或空引用 */
  @Test
  public void testGetUserUsingUsernameOrEmail() {
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("zjhzxhz"));
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("cshzxie@gmail.com"));
    Assertions.assertNull(userService.getUserUsingUsernameOrEmail("nonexistent"));
  }

  /** 测试用例: 测试getUserMetaUsingUid(User)方法 测试数据: 存在元信息的用户/空用户 预期结果: 返回元信息或空Map */
  @Test
  public void testGetUserMetaUsingUid() {
    Map<String, Object> userMeta = userService.getUserMetaUsingUid(userService.getUserUsingUid(1000));
    Assertions.assertTrue(userMeta.containsKey("registerTime"));
    Assertions.assertTrue(userService.getUserMetaUsingUid(null).isEmpty());
  }

  /** 测试用例: 测试getUserGroupUsingSlug(String)方法 测试数据: 存在/不存在的用户组别名 预期结果: 返回用户组或空引用 */
  @Test
  public void testGetUserGroupUsingSlug() {
    Assertions.assertNotNull(userService.getUserGroupUsingSlug("administrators"));
    Assertions.assertNull(userService.getUserGroupUsingSlug("nonexistent"));
  }

  /** 测试用例: 测试getUserGroups()方法 测试数据: N/a 预期结果: 返回全部用户组 */
  @Test
  public void testGetUserGroups() {
    Assertions.assertEquals(4, userService.getUserGroups().size());
  }

  /** 测试用例: 测试getNumberOfUsers(UserGroup)方法 测试数据: users用户组 预期结果: 返回该组用户数量 */
  @Test
  public void testGetNumberOfUsers() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Assertions.assertEquals(1, userService.getNumberOfUsers(userGroup));
  }

  /** 测试用例: 测试getNumberOfUserRegisteredToday()方法 测试数据: 种子数据注册时间均为历史 预期结果: 返回0 */
  @Test
  public void testGetNumberOfUserRegisteredToday() {
    Assertions.assertEquals(0, userService.getNumberOfUserRegisteredToday());
  }

  /** 测试用例: 测试getNumberOfUsersUsingUserGroupAndUsername(...)方法 测试数据: users用户组+部分用户名 预期结果: 返回匹配数量 */
  @Test
  public void testGetNumberOfUsersUsingUserGroupAndUsername() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Assertions.assertEquals(
        1, userService.getNumberOfUsersUsingUserGroupAndUsername(userGroup, "another"));
  }

  /** 测试用例: 测试getUserUsingUserGroupAndUsername(...)方法 测试数据: users用户组+部分用户名 预期结果: 返回匹配用户列表 */
  @Test
  public void testGetUserUsingUserGroupAndUsername() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    List<User> users = userService.getUserUsingUserGroupAndUsername(userGroup, "another", 0, 10);
    Assertions.assertEquals(1, users.size());
  }

  /** 测试用例: 测试getUserUsingUserGroup(...)方法 测试数据: users用户组 预期结果: 返回该组用户列表 */
  @Test
  public void testGetUserUsingUserGroup() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Assertions.assertEquals(1, userService.getUserUsingUserGroup(userGroup, 0, 10).size());
  }

  /** 测试用例: 测试isAllowedToLogin(...)方法 测试数据: 管理员账户及正确密码 预期结果: 允许登录 */
  @Test
  public void testIsAllowedToLoginSuccessfully() {
    Map<String, Boolean> result = userService.isAllowedToLogin("zjhzxhz", PASSWORD_HASH);
    Assertions.assertTrue(result.get("isAccountValid"));
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试isAllowedToLogin(...)方法 测试数据: 评测者账户 (不允许登录的用户组) 预期结果: 账户有效但不允许访问 */
  @Test
  public void testIsAllowedToLoginWithDisallowedUserGroup() {
    Map<String, Boolean> result =
        userService.isAllowedToLogin("support@verwandlung.org", PASSWORD_HASH);
    Assertions.assertTrue(result.get("isAccountValid"));
    Assertions.assertFalse(result.get("isAllowedToAccess"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试isAllowedToLogin(...)方法 测试数据: 错误的密码 预期结果: 账户无效 */
  @Test
  public void testIsAllowedToLoginWithWrongPassword() {
    Map<String, Boolean> result = userService.isAllowedToLogin("zjhzxhz", "wrong-password-hash");
    Assertions.assertFalse(result.get("isAccountValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试isAllowedToLogin(...)方法 测试数据: 空用户名 预期结果: 登录失败 */
  @Test
  public void testIsAllowedToLoginWithEmptyUsername() {
    Map<String, Boolean> result = userService.isAllowedToLogin("", PASSWORD_HASH);
    Assertions.assertTrue(result.get("isUsernameEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createUser(7参数)方法 测试数据: 合法的注册信息 预期结果: 创建成功 */
  @Test
  public void testCreateUserSuccessfully() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser01", "password", "newuser01@example.com", "users", "text/x-java", true, true);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNotNull(userService.getUserUsingUsernameOrEmail("newuser01"));
  }

  /** 测试用例: 测试createUser(7参数)方法 测试数据: 用户名已存在 预期结果: 创建失败 */
  @Test
  public void testCreateUserWithExistingUsername() {
    Map<String, Boolean> result =
        userService.createUser(
            "zjhzxhz", "password", "unique@example.com", "users", "text/x-java", true, true);
    Assertions.assertTrue(result.get("isUsernameExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createUser(7参数)方法 测试数据: 非法的用户名 预期结果: 创建失败 */
  @Test
  public void testCreateUserWithIllegalUsername() {
    Map<String, Boolean> result =
        userService.createUser(
            "ab1", "password", "unique@example.com", "users", "text/x-java", true, true);
    Assertions.assertFalse(result.get("isUsernameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createUser(7参数)方法 测试数据: 邮箱已存在 预期结果: 创建失败 */
  @Test
  public void testCreateUserWithExistingEmail() {
    Map<String, Boolean> result =
        userService.createUser(
            "uniqueuser", "password", "cshzxie@gmail.com", "users", "text/x-java", true, true);
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createUser(7参数)方法 测试数据: 系统不允许注册 预期结果: 创建失败 */
  @Test
  public void testCreateUserWhenRegisterDisallowed() {
    Map<String, Boolean> result =
        userService.createUser(
            "newuser02", "password", "newuser02@example.com", "users", "text/x-java", true, false);
    Assertions.assertFalse(result.get("isAllowRegister"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试createUser(5参数, 管理员)方法 测试数据: 合法的信息 预期结果: 创建成功 */
  @Test
  public void testCreateUserByAdministrator() {
    Map<String, Boolean> result =
        userService.createUser(
            "adminmade1", "password", "adminmade1@example.com", "users", "text/x-java");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试changePreferLanguage(...)方法 测试数据: 合法/非法的语言别名 预期结果: 对应成功或失败 */
  @Test
  public void testChangePreferLanguage() {
    User user = userService.getUserUsingUid(1000);
    Assertions.assertTrue(userService.changePreferLanguage(user, "text/x-java").get("isSuccessful"));
    Assertions.assertFalse(userService.changePreferLanguage(user, "no-lang").get("isSuccessful"));
  }

  /** 测试用例: 测试changePassword(...)方法 测试数据: 旧密码留空 (视为正确) 且新密码合法 预期结果: 修改成功 */
  @Test
  public void testChangePasswordSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result = userService.changePassword(user, "", "new-password", "new-password");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试changePassword(...)方法 测试数据: 旧密码错误 预期结果: 修改失败 */
  @Test
  public void testChangePasswordWithWrongOldPassword() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.changePassword(user, "wrong-old", "new-password", "new-password");
    Assertions.assertFalse(result.get("isOldPasswordCorrect"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试changePassword(...)方法 测试数据: 两次新密码不一致 预期结果: 修改失败 */
  @Test
  public void testChangePasswordWithMismatchedConfirmPassword() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.changePassword(user, "", "new-password", "another-password");
    Assertions.assertFalse(result.get("isConfirmPasswordMatched"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateProfile(个人资料)方法 测试数据: 合法的个人资料 预期结果: 更新成功 */
  @Test
  public void testUpdateProfileSuccessfully() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "cshzxie@gmail.com", "Beijing", "https://haozhexie.com", "{}", "About me", true);
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateProfile(个人资料)方法 测试数据: 邮箱已被其他用户占用 预期结果: 更新失败 */
  @Test
  public void testUpdateProfileWithExistingEmail() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(
            user, "support@verwandlung.org", "", "", "{}", "About me", true);
    Assertions.assertTrue(result.get("isEmailExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateProfile(个人资料)方法 测试数据: 非法的个人主页地址 预期结果: 更新失败 */
  @Test
  public void testUpdateProfileWithIllegalWebsite() {
    User user = userService.getUserUsingUid(1000);
    Map<String, Boolean> result =
        userService.updateProfile(user, "cshzxie@gmail.com", "", "ftp://invalid", "{}", "Me", true);
    Assertions.assertFalse(result.get("isWebsiteLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateProfile(管理员)方法 测试数据: 合法的密码/用户组/语言 预期结果: 更新成功 */
  @Test
  public void testUpdateProfileByAdministratorSuccessfully() {
    User user = userService.getUserUsingUid(1002);
    Map<String, Boolean> result =
        userService.updateProfile(user, "new-password", "users", "text/x-java");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试updateProfile(管理员)方法 测试数据: 不存在的用户组 预期结果: 更新失败 */
  @Test
  public void testUpdateProfileByAdministratorWithIllegalUserGroup() {
    User user = userService.getUserUsingUid(1002);
    Map<String, Boolean> result =
        userService.updateProfile(user, "new-password", "no-group", "text/x-java");
    Assertions.assertFalse(result.get("isUserGroupLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试isEmailValidationValid(...)方法 测试数据: 有效的验证凭据 预期结果: 返回true */
  @Test
  public void testIsEmailValidationValid() {
    // voj_email_validation.email 外键引用真实用户邮箱; 使用尚无验证凭据的用户1002邮箱.
    emailValidationMapper.createEmailValidation(
        new EmailValidation("noreply@verwandlung.org", "valid-token", tomorrow()));
    Assertions.assertTrue(
        userService.isEmailValidationValid("noreply@verwandlung.org", "valid-token"));
    Assertions.assertFalse(
        userService.isEmailValidationValid("noreply@verwandlung.org", "wrong-token"));
  }

  /** 测试用例: 测试isEmailValidationValid(...)方法 测试数据: 不存在的验证凭据 预期结果: 返回false */
  @Test
  public void testIsEmailValidationValidWhenNotExists() {
    Assertions.assertFalse(userService.isEmailValidationValid("nobody@example.com", "token"));
  }

  /** 测试用例: 测试resetPassword(...)方法 测试数据: 有效凭据且新密码合法一致 预期结果: 重置成功 */
  @Test
  public void testResetPasswordSuccessfully() {
    emailValidationMapper.createEmailValidation(
        new EmailValidation("cshzxie@gmail.com", "reset-token", tomorrow()));
    Map<String, Boolean> result =
        userService.resetPassword(
            "cshzxie@gmail.com", "reset-token", "new-password", "new-password", true);
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试resetPassword(...)方法 测试数据: 无效的验证凭据 预期结果: 重置失败 */
  @Test
  public void testResetPasswordWithInvalidToken() {
    Map<String, Boolean> result =
        userService.resetPassword(
            "cshzxie@gmail.com", "invalid-token", "new-password", "new-password", true);
    Assertions.assertFalse(result.get("isEmailValidationValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试sendVerificationEmail(...)方法 测试数据: CSRF令牌无效 预期结果: 不发送邮件且失败 */
  @Test
  public void testSendVerificationEmailWithInvalidCsrfToken() {
    Map<String, Boolean> result =
        userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com", false);
    Assertions.assertFalse(result.get("isCsrfTokenValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试sendVerificationEmail(...)方法 测试数据: 用户名与邮箱不匹配 预期结果: 用户不存在且失败 */
  @Test
  public void testSendVerificationEmailWithMismatchedEmail() {
    Map<String, Boolean> result =
        userService.sendVerificationEmail("zjhzxhz", "wrong@example.com", true);
    Assertions.assertFalse(result.get("isUserExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试deleteUser(long)方法 测试数据: 无关联数据的用户 预期结果: 用户被删除 */
  @Test
  public void testDeleteUser() {
    // 通过Mapper直接插入一个没有任何关联数据 (提交记录/元信息等) 的用户, 以避免外键约束.
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    Language language = new Language();
    language.setLanguageId(1);
    User user =
        new User("throwaway", PASSWORD_HASH, "throwaway@example.com", userGroup, language);
    userMapper.createUser(user);

    userService.deleteUser(user.getUid());
    Assertions.assertNull(userService.getUserUsingUid(user.getUid()));
  }

  /** 返回明天此刻的时间, 用于构造未过期的邮箱验证凭据. */
  private Date tomorrow() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, 1);
    return calendar.getTime();
  }

  /** 待测试的UserService对象. */
  @Autowired private UserService userService;

  /** 用于在测试事务内构造无关联用户数据的Mapper. */
  @Autowired private UserMapper userMapper;

  /** 用于在测试事务内构造邮箱验证凭据的Mapper. */
  @Autowired private EmailValidationMapper emailValidationMapper;
}
