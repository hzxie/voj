package com.trunkshell.voj.service;

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
	 * 自动注入的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
}
