package com.happystudio.voj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.dao.UserDao;
import com.happystudio.voj.model.User;

/**
 * 用户Service对象. 为Controller提供服务.
 * @author Xie Haozhe
 */
@Service
@Transactional
public class UserService {
    /**
     * 验证用户身份是否有效.
     * @param username - 用户名
     * @param password - 密码(已使用MD5加密)
     * @return 一个User的对象或一个空引用
     */
    public User isAccountValid(String username, String password) {
        User user = userDao.getUserUsingUsername(username);
        if ( user != null && user.getPassword().equals(password) ) {
            return user;
        }
        return null;
    }
    
    /**
     * 自动注入的UserDAO对象.
     */
    @Autowired
    private UserDao userDao;
}

