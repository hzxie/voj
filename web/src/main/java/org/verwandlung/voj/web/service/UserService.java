/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
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
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;

import freemarker.template.TemplateException;

import org.verwandlung.voj.web.mapper.EmailValidationMapper;
import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.UserGroupMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.mapper.UserMetaMapper;
import org.verwandlung.voj.web.model.EmailValidation;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.UserMeta;
import org.verwandlung.voj.web.util.DigestUtils;
import org.verwandlung.voj.web.util.HtmlTextFilter;
import org.verwandlung.voj.web.util.MailSender;
import org.verwandlung.voj.web.util.OffensiveWordFilter;

/**
 * 用户类(User)的业务逻辑层.
 * 
 * @author Haozhe Xie
 */
@Service
@Transactional
public class UserService {
	/**
	 * 通过用户唯一标识符获取用户对象.
	 * @param userId - 用户唯一标识符
	 * @return 预期的用户对象或空引用
	 */
	public User getUserUsingUid(long userId) {
		return userMapper.getUserUsingUid(userId);
	}

	/**
	 * 获取某个用户组中的用户列表.
	 * @param userGroup - 用户所属的用户组对象
	 * @param offset - 用户唯一标识符的起始编号
	 * @param limit - 需要获取的用户的数量
	 * @return 用户列表
	 */
	public List<User> getUserUsingUserGroup(UserGroup userGroup, long offset, int limit) {
		return userMapper.getUserUsingUserGroup(userGroup, offset, limit);
	}
	
	/**
	 * 获取用户的元信息.
	 * @param user - 元信息对应的用户对象
	 * @return 用户元信息的键值对
	 */
	public Map<String, Object> getUserMetaUsingUid(User user) {
		Map<String, Object> userMetaMap = new HashMap<>(); 
		if ( user == null ) {
			return userMetaMap;
		}
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		for ( UserMeta userMeta : userMetaList ) {
			String key = userMeta.getMetaKey();
			Object value = userMeta.getMetaValue();
			
			if ( "socialLinks".equals(key) ) {
				value = JSON.parseObject((String)value);
			}
			userMetaMap.put(key, value);
		}
		return userMetaMap;
	}
	
	/**
	 * 通过用户名或电子邮件地址获取用户对象.
	 * @param username - 用户名或电子邮件地址
	 * @return 一个User对象或空引用
	 */
	public User getUserUsingUsernameOrEmail(String username) {
		boolean isUsingEmail = username.indexOf('@') != -1;
		User user = null;
			
		if ( !isUsingEmail ) {
			user = userMapper.getUserUsingUsername(username);
		} else {
			user = userMapper.getUserUsingEmail(username);
		}
		return user;
	}

	/**
	 * 验证用户身份是否有效.
	 * @param username - 用户名或电子邮件地址
	 * @param password - 密码(已使用MD5加密)
	 * @return 一个包含登录验证结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> isAllowedToLogin(String username, String password) {
		Map<String, Boolean> result = new HashMap<>(6, 1);
		result.put("isUsernameEmpty", username.isEmpty());
		result.put("isPasswordEmpty", password.isEmpty());
		result.put("isAccountValid", false);
		result.put("isAllowedToAccess", false);
		result.put("isSuccessful", false);

		if ( !result.get("isUsernameEmpty") && !result.get("isPasswordEmpty") ) {
			User user = getUserUsingUsernameOrEmail(username);
			if ( user != null && user.getPassword().equals(password) ) {
				result.put("isAccountValid", true);
				if ( isAllowedToAccess(user.getUserGroup()) ) {
					result.put("isAllowedToAccess", true);
					result.put("isSuccessful", true);
				}
			}
		}
		return result;
	}
	
	/**
	 * 验证用户是否被允许登录.
	 * @param userGroup - 用户所属用户组的对象
	 * @return 用户是否被允许登录
	 */
	private boolean isAllowedToAccess(UserGroup userGroup) {
		String[] allowedUserGroups = {"users", "administrators"};
		for ( String allowedUserGroup : allowedUserGroups ) {
			if ( allowedUserGroup.equals(userGroup.getUserGroupSlug()) ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 验证账户有效性并创建用户.
	 * @param username - 用户名
	 * @param password - 密码(未使用MD5加密)
	 * @param email - 电子邮件地址
	 * @param userGroupSlug - 用户组的别名
	 * @param languageSlug - 偏好语言的别名
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @param isAllowRegister - 系统是否允许注册新用户
	 * @return 一个包含账户创建结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> createUser(String username, String password, String email, 
			String userGroupSlug, String languageSlug, boolean isCsrfTokenValid, boolean isAllowRegister) {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
		Language languagePreference = languageMapper.getLanguageUsingSlug(languageSlug);
		User user = new User(username, DigestUtils.md5Hex(password), email, userGroup, languagePreference);
		
		Map<String, Boolean> result = getUserCreationResult(user, password, isCsrfTokenValid, isAllowRegister);
		if ( result.get("isSuccessful") ) {
			userMapper.createUser(user);
			createUserMeta(user);
		}
		return result;
	}

	/**
	 * [此方法仅供管理员使用]
	 * 验证账户有效性并创建用户.
	 * @param username - 用户名
	 * @param password - 密码(未使用MD5加密)
	 * @param email - 电子邮件地址
	 * @param userGroupSlug - 用户组的别名
	 * @param languageSlug - 偏好语言的别名
	 * @return 一个包含账户创建结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> createUser(String username, String password, String email, String userGroupSlug, String languageSlug) {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
		Language languagePreference = languageMapper.getLanguageUsingSlug(languageSlug);
		User user = new User(username, DigestUtils.md5Hex(password), email, userGroup, languagePreference);
		
		Map<String, Boolean> result = getUserCreationResult(user, password, true, true);
		if ( result.get("isSuccessful") ) {
			userMapper.createUser(user);
			createUserMeta(user);
		}
		return result;
	}
	
	/**
	 * 创建用户元信息.
	 * @param user - 对应的用户对象
	 */
	private void createUserMeta(User user) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar = Calendar.getInstance();
		String registerTime = formatter.format(calendar.getTime());
		UserMeta registerTimeMeta = new UserMeta(user, "registerTime", registerTime);
		userMetaMapper.createUserMeta(registerTimeMeta);
	}
	
	/**
	 * 验证待创建用户信息的合法性.
	 * @param user - 待创建的User对象
	 * @param password - 密码(未使用MD5加密)
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @param isAllowRegister - 系统是否允许注册新用户
	 * @return 一个包含账户信息验证结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getUserCreationResult(User user, String password, 
			boolean isCsrfTokenValid, boolean isAllowRegister) {
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
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		result.put("isAllowRegister", isAllowRegister);
		
		boolean isSuccessful = !result.get("isUsernameEmpty")  &&  result.get("isUsernameLegal")  &&
							   !result.get("isUsernameExists") && !result.get("isPasswordEmpty")  &&
								result.get("isPasswordLegal")  && !result.get("isEmailEmpty")	 &&
								result.get("isEmailLegal")	 && !result.get("isEmailExists")	&&
								result.get("isUserGroupLegal") &&  result.get("isLanguageLegal")  &&  
								result.get("isCsrfTokenValid") &&  result.get("isAllowRegister");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 检查电子邮件验证凭据是否有效.
	 * @param email - 用户的电子邮件地址
	 * @param token - 用于验证的Token
	 * @return 电子邮件验证凭据是否有效
	 */
	public boolean isEmailValidationValid(String email, String token) {
		EmailValidation emailValidation = emailValidationMapper.getEmailValidation(email);
		Date now = new Date();
		
		if ( emailValidation != null && emailValidation.getToken().equals(token) &&
				now.before(emailValidation.getExpireTime()) ) {
			return true;
		}
		return false;
	}
	
	/**
	 * 验证账户有效性并发送重设密码邮件.
	 * @param username - 用户的用户名
	 * @param email - 用户的电子邮件地址
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 包含账户验证结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> sendVerificationEmail(String username, 
			String email, boolean isCsrfTokenValid) {
		boolean isSuccessful = true;
		boolean isUserExists = false;
		Map<String, Boolean> result = new HashMap<>(4, 1);
		
		if ( isCsrfTokenValid ) {
			User user = userMapper.getUserUsingUsername(username);
			if ( user != null && user.getEmail().equals(email) ) {
				isUserExists = true;
				try {
					sendVerificationEmail(username, email);
				} catch (IOException | TemplateException e) {
					e.printStackTrace();
					isSuccessful = false;
				}
			}
		}
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		result.put("isUserExists", isUserExists);
		result.put("isSuccessful", isSuccessful && isUserExists);
		
		return result;
	}
	
	/**
	 * 发送重设密码的邮件.
	 * @param username - 用户的用户名
	 * @param email - 用户的电子邮件地
	 * @throws TemplateException 
	 * @throws IOException
	 */
	private void sendVerificationEmail(String username, String email) 
			throws IOException, TemplateException {
		String token = DigestUtils.getGuid();
		Date expireTime = getExpireTime();
		Map<String, Object> model = new HashMap<>(4, 1);
		model.put("username", username);
		model.put("email", email);
		model.put("token", token);
		
		EmailValidation emailValidation = new EmailValidation(email, token, expireTime);
		emailValidationMapper.deleteEmailValidation(email);
		emailValidationMapper.createEmailValidation(emailValidation);
		
		String templatePath = "/reset-password.ftl";
		String subject = "Password Reset Request";
		String body = mailSender.getMailContent(templatePath, model);
		mailSender.sendMail(email, subject, body);
	}
	
	/**
	 * 获取邮件验证Token的失效时间.
	 * @return 邮件验证Token的失效时间
	 */
	private Date getExpireTime() {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 1);
		date = calendar.getTime();
		
		return date;
	}
	
	/**
	 * 验证数据并重置用户密码.
	 * @param email - 用户的电子邮件地址
	 * @param token - 用于验证的Token
	 * @param newPassword - 新密码
	 * @param confirmPassword - 确认新密码
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 包含密码重置结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> resetPassword(String email, String token, 
			String newPassword, String confirmPassword, boolean isCsrfTokenValid) {
		boolean isEmailValidationValid = isEmailValidationValid(email, token);
		Map<String, Boolean> result = getResetPasswordResult(newPassword, 
				confirmPassword, isEmailValidationValid, isCsrfTokenValid);
		
		if ( result.get("isSuccessful") ) {
			emailValidationMapper.deleteEmailValidation(email);
			
			User user = userMapper.getUserUsingEmail(email);
			user.setPassword(DigestUtils.md5Hex(newPassword));
			userMapper.updateUser(user);
		}
		return result;
	}
	
	/**
	 * @param newPassword - 新密码
	 * @param confirmPassword - 确认新密码
	 * @param isEmailValidationValid - 电子邮件验证凭据是否有效
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 包含密码重置结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getResetPasswordResult(String newPassword, 
			String confirmPassword, boolean isEmailValidationValid, 
			boolean isCsrfTokenValid) {
		Map<String, Boolean> result = new HashMap<>(7, 1);
		result.put("isEmailValidationValid", isEmailValidationValid);
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		result.put("isNewPasswordEmpty", newPassword.isEmpty());
		result.put("isNewPasswordLegal", isPasswordLegal(newPassword));
		result.put("isConfirmPasswordMatched", newPassword.equals(confirmPassword));
		
		boolean isSuccessful =  result.get("isEmailValidationValid") && result.get("isCsrfTokenValid")   &&
							   !result.get("isNewPasswordEmpty")	 && result.get("isNewPasswordLegal") &&
								result.get("isConfirmPasswordMatched");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 验证编程语言偏好的有效性并更改编程语言偏好.
	 * @param user - 待更改编程语言偏好的用户对象
	 * @param preferLanguageSlug - 编程语言的别名
	 * @return 一个包含编程语言偏好更改结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> changePreferLanguage(User user, String preferLanguageSlug) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		Language preferLanguage = languageMapper.getLanguageUsingSlug(preferLanguageSlug);
		
		if ( preferLanguage != null ) {
			user.setPreferLanguage(preferLanguage);
			userMapper.updateUser(user);
		}
		result.put("isSuccessful", preferLanguage != null);
		return result;
	}
	
	/**
	 * 验证旧密码正确性并修改密码.
	 * @param user - 待修改密码的用户对象
	 * @param oldPassword - 旧密码
	 * @param newPassword - 新密码
	 * @param confirmPassword - 确认新密码
	 * @return 一个包含密码验证结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> changePassword(User user, String oldPassword, 
			String newPassword, String confirmPassword) {
		Map<String, Boolean> result = getChangePasswordResult(user, oldPassword, newPassword, confirmPassword);
		
		if ( result.get("isSuccessful") ) {
			user.setPassword(DigestUtils.md5Hex(newPassword));
			userMapper.updateUser(user);
		}
		return result;
	}
	
	/**
	 * 验证旧密码的正确性和新密码的合法性.
	 * @param user - 待修改密码的用户对象
	 * @param oldPassword - 旧密码
	 * @param newPassword - 新密码
	 * @param confirmPassword - 确认新密码
	 * @return 一个包含密码验证结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getChangePasswordResult(User user, String oldPassword, 
			String newPassword, String confirmPassword) {
		Map<String, Boolean> result = new HashMap<>(5, 1);
		result.put("isOldPasswordCorrect", isOldPasswordCorrect(user.getPassword(), oldPassword));
		result.put("isNewPasswordEmpty", newPassword.isEmpty());
		result.put("isNewPasswordLegal", isPasswordLegal(newPassword));
		result.put("isConfirmPasswordMatched", newPassword.equals(confirmPassword));
		
		boolean isSuccessful = result.get("isOldPasswordCorrect") && !result.get("isNewPasswordEmpty") &&
							   result.get("isNewPasswordLegal")   &&  result.get("isConfirmPasswordMatched");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 验证新资料的有效性并更新个人资料.
	 * @param user - 待更改资料的用户
	 * @param email - 用户的电子邮件地址
	 * @param location - 用户的所在地区
	 * @param website - 用户的个人主页
	 * @param socialLinks - 用户的社交网络信息
	 * @param aboutMe - 用户的个人简介
	 * @return 一个包含个人资料修改结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> updateProfile(User user, String email, 
			String location, String website, String socialLinks, String aboutMe) {
		location = HtmlTextFilter.filter(location);
		website = HtmlTextFilter.filter(website);
		socialLinks = HtmlTextFilter.filter(socialLinks);
		aboutMe = offensiveWordFilter.filter(HtmlTextFilter.filter(aboutMe));
		Map<String, Boolean> result = getUpdateProfileResult(user, email, location, website, socialLinks, aboutMe);
		
		if ( result.get("isSuccessful") ) {
			user.setEmail(email);
			userMapper.updateUser(user);
			
			updateUserMeta(user, "location", location);
			updateUserMeta(user, "website", website);
			updateUserMeta(user, "socialLinks", socialLinks);
			updateUserMeta(user, "aboutMe", aboutMe);
		}
		return result;
	}
	
	/**
	 * 验证新资料的有效性.
	 * @param user - 待更改资料的用户
	 * @param email - 用户的电子邮件地址
	 * @param location - 用户的所在地区
	 * @param website - 用户的个人主页
	 * @param socialLinks - 用户的社交网络信息
	 * @param aboutMe - 用户的个人简介
	 * @return 一个包含个人资料修改结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getUpdateProfileResult(User user, String email, 
			String location, String website, String socialLinks, String aboutMe) {
		Map<String, Boolean> result = new HashMap<>(7, 1);
		result.put("isEmailEmpty", email.isEmpty());
		result.put("isEmailLegal", isEmailLegal(email));
		result.put("isEmailExists", isEmailExists(user.getEmail(), email));
		result.put("isLocationLegal", location.length() <= 128);
		result.put("isWebsiteLegal", isWebsiteLegal(website));
		result.put("isAboutMeLegal", aboutMe.length() <= 256);
		
		boolean isSuccessful = !result.get("isEmailEmpty")   && result.get("isEmailLegal")	&&
							   !result.get("isEmailExists")  && result.get("isLocationLegal") &&
								result.get("isWebsiteLegal") && result.get("isAboutMeLegal");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 更新用户元信息.
	 * @param user - 待更新元信息的用户
	 * @param metaKey - 元信息的键
	 * @param metaValue - 元信息的值
	 */
	private void updateUserMeta(User user, String metaKey, String metaValue) {
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, metaKey);
		
		if ( userMeta == null ) {
			if ( metaValue.isEmpty() ) {
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
	 * 验证用户名的合法性:
	 * 规则: 用户名应由[A-Za-z0-9_]组成, 以字母起始且长度在6-16个字符.
	 * @param username - 用户名
	 * @return 用户名是否合法
	 */
	private boolean isUsernameLegal(String username) {
		return username.matches("^[A-Za-z][A-Za-z0-9_]{5,15}$");
	}
	
	/**
	 * 检查用户名是否存在.
	 * @param username - 用户名
	 * @return 用户名是否存在
	 */
	private boolean isUsernameExists(String username) {
		User user = userMapper.getUserUsingUsername(username);
		return user != null;
	}
	
	/**
	 * 检查密码是否合法.
	 * 规则: 密码的长度在6-16个字符.
	 * @param password - 密码(未经MD5加密)
	 * @return 密码是否合法
	 */
	private boolean isPasswordLegal(String password) {
		int passwordLength = password.length();
		return passwordLength >= 6 && passwordLength <= 16;
	}
	
	/**
	 * 更改密码时, 验证用户的旧密码是否正确.
	 * @param oldPassword - 用户的旧密码(已使用MD5加密)
	 * @param submitedPassword - 所提交进行验证的旧密码(未使用MD5加密)
	 * @return 用户旧密码是否正确
	 */
	private boolean isOldPasswordCorrect(String oldPassword, String submitedPassword) {
		if ( submitedPassword.isEmpty() ) {
			return true;
		}
		return oldPassword.equals(DigestUtils.md5Hex(submitedPassword));
	}
	
	/**
	 * 检查电子邮件地址是否合法.
	 * 规则: 合法的电子邮件地址且长度不超过64个字符.
	 * @param email - 电子邮件地址
	 * @return 电子邮件地址是否合法
	 */
	private boolean isEmailLegal(String email) {
		int emailLength = email.length();
		return emailLength <= 64 && email.matches("^[A-Za-z0-9\\._-]+@[A-Za-z0-9_-]+\\.[A-Za-z0-9\\._-]+$");
	}
	
	/**
	 * 检查电子邮件地址是否存在.
	 * 说明: 仅用于用户创建新账户
	 * @param email - 电子邮件地址
	 * @return 电子邮件地址是否存在
	 */
	private boolean isEmailExists(String email) {
		User user = userMapper.getUserUsingEmail(email);
		return user != null;
	}
	
	/**
	 * 检查电子邮件地址是否存在.
	 * 说明: 仅用于用户编辑个人资料
	 * @param currentEmail - 之前所使用的Email地址
	 * @param email - 待更新的Email地址
	 * @return 电子邮件地址是否存在
	 */
	private boolean isEmailExists(String currentEmail, String email) {
		if ( currentEmail.equals(email) ) {
			return false;
		}
		User user = userMapper.getUserUsingEmail(email);
		return user != null;
	}
	
	/**
	 * 检查个人主页的地址是否合法.
	 * 规则: 合法的HTTP(S)协议URL且长度不超过64个字符.
	 * @param website - 个人主页的地址
	 * @return 个人主页的地址是否合法
	 */
	private boolean isWebsiteLegal(String website) {
		int websiteLength = website.length();
		return website.isEmpty() || 
			  (websiteLength <= 64 && website.matches("^(http|https):\\/\\/[A-Za-z0-9-]+\\.[A-Za-z0-9_.]+$"));
	}
	
	/**
	 * 通过用户组的别名获取用户组对象.
	 * @param userGroupSlug - 用户组的别名
	 * @return 用户组对象或空引用
	 */
	public UserGroup getUserGroupUsingSlug(String userGroupSlug) {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
		return userGroup;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取全部的用户组对象.
	 * @return 全部的用户组对象的列表
	 */
	public List<UserGroup> getUserGroups() {
		List<UserGroup> userGroups = userGroupMapper.getUserGroups();
		return userGroups;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取系统中注册用户的总数.
	 * @param userGroup - 用户所属的用户组对象
	 * @return 系统中注册用户的总数
	 */
	public long getNumberOfUsers(UserGroup userGroup) {
		return userMapper.getNumberOfUsersUsingUserGroup(userGroup);
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取今日注册的用户数量.
	 * @return 今日注册的用户数量
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
	 * [此方法仅供管理员使用]
	 * 使用用户组和用户名获取符合条件的用户的总数.
	 * @param userGroup - 用户组对象
	 * @param username - 部分或全部用户名
	 * @return 某个用户组中用户名中包含某个字符串的用户的总数
	 */
	public long getNumberOfUsersUsingUserGroupAndUsername(UserGroup userGroup, String username) {
		return userMapper.getNumberOfUsersUsingUserGroupAndUsername(userGroup, username);
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 根据用户组和用户名筛选用户对象.
	 * @param userGroup - 用户组对象
	 * @param username - 部分或全部用户名
	 * @param offset - 用户唯一标识符的起始编号
	 * @param limit - 需要获取的用户的数量
	 * @return 符合条件的用户列表
	 */
	public List<User> getUserUsingUserGroupAndUsername(UserGroup userGroup, 
			String username, long offset, int limit) {
		return userMapper.getUserUsingUserGroupAndUsername(userGroup, username, offset, limit);
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * @param user - 待更改个人信息的用户.
	 * @param password - 用户的密码
	 * @param userGroupSlug - 用户所属用户组的别名
	 * @param preferLanguageSlug - 用户偏好编程语言的别名
	 * @return 包含用户个人信息更改结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> updateProfile(User user, String password, String userGroupSlug, String preferLanguageSlug) {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
		Language preferLanguage = languageMapper.getLanguageUsingSlug(preferLanguageSlug);
		Map<String, Boolean> result = getUpdateProfileResult(password, userGroup, preferLanguage);
		
		if ( result.get("isSuccessful") ) {
			if ( !password.isEmpty() ) {
				user.setPassword(DigestUtils.md5Hex(password));
			}
			user.setUserGroup(userGroup);
			user.setPreferLanguage(preferLanguage);
			userMapper.updateUser(user);
		}
		return result;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 更改用户的基本信息.
	 * @param password - 用户的密码
	 * @param userGroup - 用户所属的用户组
	 * @param preferLanguage - 用户偏爱的编程语言
	 * @return 包含用户个人信息更改结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getUpdateProfileResult(String password, UserGroup userGroup, Language preferLanguage) {
		Map<String, Boolean> result = new HashMap<>(6, 1);
		result.put("isPasswordEmpty", password.isEmpty());
		result.put("isPasswordLegal", isPasswordLegal(password));
		result.put("isUserGroupLegal", userGroup != null);
		result.put("isPreferLanguageLegal", preferLanguage != null);
		
		boolean isSuccessful = result.get("isPasswordEmpty")  ^  result.get("isPasswordLegal") &&
							   result.get("isUserGroupLegal") && result.get("isPreferLanguageLegal");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 根据用户的唯一标识符删除用户.
	 * @param uid - 用户的唯一标识符
	 */
	public void deleteUser(long uid) {
		userMapper.deleteUser(uid);
	}
	
	/**
	 * 自动注入的UserMapper对象.
	 * 用于获取用户基本信息.
	 */
	@Autowired
	private UserMapper userMapper;
	
	/**
	 * 自动注入的UserMetaMapper对象.
	 * 用于获取用户元信息.
	 */
	@Autowired
	private UserMetaMapper userMetaMapper;
	
	/**
	 * 自动注入的UserGroupMapper对象.
	 * 用于获取用户组信息.
	 */
	@Autowired
	private UserGroupMapper userGroupMapper;
	
	/**
	 * 自动注入的LanguageMapper对象.
	 * 用于加载注册页面的语言偏好.
	 */
	@Autowired
	private LanguageMapper languageMapper;
	
	/**
	 * 自动注入的EmailValidationMapper对象.
	 * 用于生成重置密码的Token.
	 */
	@Autowired
	private EmailValidationMapper emailValidationMapper;
	
	/**
	 * 自动注入的OffensiveWordFilter对象.
	 * 用于过滤用户个人信息中的敏感词.
	 */
	@Autowired
	private OffensiveWordFilter offensiveWordFilter;
	
	/**
	 * 自动注入的MailSender对象.
	 * 用于发送电子邮件至用户邮箱.
	 */
	@Autowired
	@Qualifier("vojMailSender")
	private MailSender mailSender;
}
