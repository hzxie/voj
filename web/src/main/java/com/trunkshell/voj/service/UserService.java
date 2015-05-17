package com.trunkshell.voj.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.mapper.UserMapper;
import com.trunkshell.voj.model.User;

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
	 * 自动注入的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
}
