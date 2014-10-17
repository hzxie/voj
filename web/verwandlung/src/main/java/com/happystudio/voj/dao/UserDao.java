package com.happystudio.voj.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.happystudio.voj.model.User;

/**
 * User类的Data Access Object.
 * @author Xie Haozhe
 */
@Repository
public class UserDao {
    /**
     * 通过用户唯一标识符获取用户对象.
     * @param uid - 用户唯一标识符
     * @return 一个用户对象的实例
     */
    @Transactional
    public User getUserUsingUid(int uid) {
        Session session = sessionFactory.getCurrentSession();
        User user = (User)session.get(User.class, uid);
        return user;
    }
    
    /**
     * 通过用户名获取用户对象.
     * @param username - 用户名
     * @return 一个用户对象的实例
     */
    @Transactional
    public User getUserUsingUsername(String username) {
        Session session = sessionFactory.getCurrentSession();
        @SuppressWarnings("unchecked")
        List<User> users = (List<User>)session.createQuery("from User where username = ?")
                                                .setString(0, username).list();
        for (User user : users ) {
            if ( user.getUsername().equals(username) ) {
                return user;
            }
        }
        return null;
    }
    
    /**
     * 创建一个新的用户对象.
     * @param user - 用户对象的实例
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean createUser(User user) {
        Session session = sessionFactory.getCurrentSession();
        String username = user.getUsername();
		if ( username.isEmpty() ) {
			return false;
		}
		
        session.save(user);
        session.flush();
        return true;
    }
    
    /**
     * 更新用户信息.
     * @param user - 用户对象的实例
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean updateUser(User user) {
        Session session = sessionFactory.getCurrentSession();
        if ( session.get(User.class, user.getUid()) == null ) {
            return false;
        }
        session.merge(user);
        session.flush();
        return true;
    }
    
    /**
     * 通过用户名删除一个用户对象.
     * @param uid - 用户的唯一标识符
     * @return 操作是否成功完成
     */
    @Transactional
    public boolean deleteUser(int uid) {
        Session session = sessionFactory.getCurrentSession();
        User user = (User)session.get(User.class, uid);
        
        if ( user == null ) {
            return false;
        }
        session.delete(user);
        return true;
    }

    /**
     * 自动注入的SessionFactory.
     */
    @Autowired
    private SessionFactory sessionFactory;
}
