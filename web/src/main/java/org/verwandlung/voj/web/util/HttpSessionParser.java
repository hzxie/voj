package org.verwandlung.voj.web.util;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.UserService;

/**
 * HttpSession解析器.
 * @author Xie Haozhe
 */
@Component
public class HttpSessionParser {
    /**
     * HttpSessionParser的构造函数.
     * @param userService - 自动注入的UserService对象
     */
    @Autowired
    private HttpSessionParser(UserService userService) {
        HttpSessionParser.userService = userService;
    }
    
    /**
     * 获取Session中的用户对象.
     * @param session - HttpSession对象
     * @return Session中的用户对象
     */
    public static User getCurrentUser(HttpSession session) {
        Object isLoggedInAttribute = session.getAttribute("isLoggedIn");
        Object uidAttribute = session.getAttribute("uid");
        User user = null;
        
        if ( isLoggedInAttribute == null || uidAttribute == null ) {
            return null;
        }
        boolean isLoggedIn = (Boolean)isLoggedInAttribute;
        long uid = (Long)uidAttribute;
        
        if ( isLoggedIn ) {
            user = userService.getUserUsingUid(uid);
        }
        return user;
    }
    
    /**
     * 自动注入的UserService对象.
     */
    private static UserService userService;
}
