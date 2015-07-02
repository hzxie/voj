package com.trunkshell.voj.web.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.mapper.LanguageMapper;
import com.trunkshell.voj.web.mapper.UserGroupMapper;
import com.trunkshell.voj.web.mapper.UserMapper;
import com.trunkshell.voj.web.model.Language;
import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserGroup;
import com.trunkshell.voj.web.util.DigestUtils;

/**
 * 用户类(User)的业务逻辑层.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class UserService {
	/**
	 * 通过用户唯一标识符获取用户对象.
	 * @param uid - 用户唯一标识符
	 * @return 预期的用户对象或空引用
	 */
	public User getUserUsingUid(long uid) {
		return userMapper.getUserUsingUid(uid);
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
	public Map<String, Boolean> isAccountValid(String username, String password) {
		Map<String, Boolean> result = new HashMap<String, Boolean>(5, 1);
		result.put("isUsernameEmpty", username.isEmpty());
		result.put("isPasswordEmpty", password.isEmpty());
		result.put("isAccountValid", false);
		result.put("isSuccessful", false);

		if ( !result.get("isUsernameEmpty") && !result.get("isPasswordEmpty") ) {
			User user = getUserUsingUsernameOrEmail(username);
			if ( user != null && user.getPassword().equals(password) ) {
				result.put("isAccountValid", true);
				result.put("isSuccessful", true);
			}
		}
		return result;
	}
	
	/**
	 * 验证账户有效性并创建用户.
	 * @param username - 用户名
	 * @param password - 密码(未使用MD5加密)
	 * @param email - 电子邮件地址
	 * @param languageSlug - 偏好语言的唯一英文缩写
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 一个包含账户创建结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> createUser(String username, String password, 
			String email, String languageSlug, boolean isCsrfTokenValid) {
		UserGroup userGroup = getUserGroup("users");
		Language languagePreference = languageMapper.getLanguageUsingSlug(languageSlug);
		User user = new User(username, DigestUtils.md5Hex(password), email, userGroup, languagePreference);
		
		Map<String, Boolean> result = getUserCreationResult(user, password, isCsrfTokenValid);
		if ( result.get("isSuccessful") ) {
			userMapper.createUser(user);
		}
		return result;
	}
	
	/**
	 * 验证待创建用户信息的合法性.
	 * @param user - 待创建的User对象
	 * @param password - 密码(未使用MD5加密)
	 * @param isCsrfTokenValid - CSRF的Token是否正确
	 * @return 一个包含账户信息验证结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getUserCreationResult(User user, String password, boolean isCsrfTokenValid) {
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		result.put("isUsernameEmpty", user.getUsername().isEmpty());
		result.put("isUsernameLegal", isUsernameLegal(user.getUsername()));
		result.put("isUsernameExists", isUsernameExists(user.getUsername()));
		result.put("isPasswordEmpty", password.isEmpty());
		result.put("isPasswordLegal", isPasswordLegal(password));
		result.put("isEmailEmpty", user.getEmail().isEmpty());
		result.put("isEmailLegal", isEmailLegal(user.getEmail()));
		result.put("isEmailExists", isEmailExists(user.getEmail()));
		result.put("isLanguageLegal", user.getPreferLanguage() != null);
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		
		boolean isSuccessful = !result.get("isUsernameEmpty")  &&  result.get("isUsernameLegal") &&
							   !result.get("isUsernameExists") && !result.get("isPasswordEmpty") &&
							    result.get("isPasswordLegal")  && !result.get("isEmailEmpty")    &&
							    result.get("isEmailLegal")     && !result.get("isEmailExists")   &&
							    result.get("isLanguageLegal")  &&  result.get("isCsrfTokenValid");
		result.put("isSuccessful", isSuccessful);
		return result;
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
	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
	private boolean isEmailExists(String currentEmail, String email) {
		if ( currentEmail.equals(email) ) {
			return false;
		}
		User user = userMapper.getUserUsingEmail(email);
		return user != null;
	}
	
	/**
	 * 通过用户组的唯一英文缩写获取用户组对象.
	 * @param userGroupSlug - 用户组的唯一英文缩写
	 * @return 用户组对象或空引用
	 */
	private UserGroup getUserGroup(String userGroupSlug) {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
		return userGroup;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取系统中注册用户的总数.
	 * @return 系统中注册用户的总数
	 */
	public long getNumberOfUsers() {
		return userMapper.getNumberOfUsers();
	}
	
	/**
	 * 自动注入的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
	
	/**
	 * 自动注入的UserGroupMapper对象.
	 */
	@Autowired
	private UserGroupMapper userGroupMapper;
	
	/**
	 * 自动注入的LanguageMapper对象.
	 */
	@Autowired
	private LanguageMapper languageMapper;
}
