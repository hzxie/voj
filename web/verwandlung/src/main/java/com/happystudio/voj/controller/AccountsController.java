package com.happystudio.voj.controller;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.happystudio.voj.model.User;
import com.happystudio.voj.service.UserService;
import com.happystudio.voj.util.DigestUtils;

/**
 * 处理用户的登录/注册请求.
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/accounts")
public class AccountsController {
    /**
     * 显示用户的登录页面.
     * @param isLogout - 是否处于登出状态
     * @param request - Http Servlet Request对象
     * @param session - Http Session对象
     * @return 包含登录页面信息的ModelAndView对象
     */
    @RequestMapping(value = "/login")
    public ModelAndView loginView(
            @RequestParam(value="logout", required=false, defaultValue="false") boolean isLogout,
            HttpServletRequest request,
            HttpSession session) {
        if ( isLogout ) {
            destroySession(request, session);
        }
        
        ModelAndView view = null;
        if ( isLoggedIn(session) ) {
            view = new ModelAndView("redirect:/");
        } else {
            view = new ModelAndView("accounts/login");
            view.addObject("isLogout", isLogout);
        }
        return view;
    }
    
    /**
     * 为注销的用户销毁Session.
     * @param request - HttpServletRequest对象
     * @param session - HttpSession 对象
     */
    private void destroySession(HttpServletRequest request, HttpSession session) {
        session.removeAttribute("isLoggedIn");
        
        String ipAddress = request.getRemoteAddr();
        logger.info(String.format("%s logged out at %s", new Object[] {user, ipAddress}));
    }
    
    /**
     * 检查用户是否已经登录.
     * @param session - HttpSession 对象
     * @return 用户是否已经登录
     */
    private boolean isLoggedIn(HttpSession session) {
        Boolean isLoggedIn = (Boolean)session.getAttribute("isLoggedIn");
        if ( isLoggedIn == null || !isLoggedIn.booleanValue() ) {
            return false;
        }
        return true;
    }
    
    /**
     * 处理用户的异步登录请求.
     * @param username - 用户名
     * @param password - 密码
     * @param request - Http Servlet Request对象
     * @param session - Http Session对象
     * @return 一个包含若干标志位的JSON数据
     */
    @RequestMapping(value = "/login.action", method = RequestMethod.POST)
    public @ResponseBody HashMap<String, Boolean> loginAction(
            @RequestParam(value="username", required=true) String username,
            @RequestParam(value="password", required=true) String password,
            HttpServletRequest request,
            HttpSession session) {
        String ipAddress = request.getRemoteAddr();
        HashMap<String, Boolean> result = getLoginResult(username, password);
        logger.info(String.format("User: [Username=%s] tried to log in at %s", new Object[] {username, ipAddress}));
        if ( result.get("isSuccessful") ) {
            getSession(request, session, this.user);
        }
        return result;
    }
    
    /**
     * 获取用户登录验证结果.
     * @param username - 用户名
     * @param password - 密码
     * @return 一个包含若干标志位的HashMap
     */
    private HashMap<String, Boolean> getLoginResult(String username, String password) {
        HashMap<String, Boolean> result = new HashMap<String, Boolean>();
        result.put("isUsernameEmpty", username.isEmpty());
        result.put("isPasswordEmpty", password.isEmpty());
        result.put("isAccountValid", false);
        result.put("isSuccessful", false);
        
        if ( !result.get("isUsernameEmpty") && !result.get("isPasswordEmpty") ) {
            this.user = userService.isAccountValid(username, DigestUtils.md5Hex(password));
            if ( user != null ) {
                result.put("isAccountValid", true);
                result.put("isSuccessful", true);
            }
        }
        return result;
    }
    
    /**
     * 为登录的用户创建Session.
     * @param request - HttpServletRequest对象
     * @param session - HttpSession对象
     * @param user - 一个User对象, 包含用户的基本信息
     */
    private void getSession(HttpServletRequest request, HttpSession session, User user) {
        session.setAttribute("isLoggedIn", true);
        session.setAttribute("user", user);
        
        String ipAddress = request.getRemoteAddr();
        logger.info(String.format("%s logged in at %s", new Object[] {user, ipAddress}));
    }
 
    /**
     * 显示用户的注册页面
     * @param request - Http Servlet Request对象
     * @param session - Http Session对象
     * @return 包含注册页面信息的ModelAndView对象
     */
    @RequestMapping(value = "/register")
    public ModelAndView registerView(HttpServletRequest request, HttpSession session) {
        return new ModelAndView("accounts/register");
    }

    /**
     * 用户对象, 包含用户的基本信息.
     */
    private User user = null;
    
    /**
     * 自动注入的UserService对象.
     */
    @Autowired
    UserService userService;
    
    /**
     * 日志记录器.
     */
    private Logger logger = LogManager.getLogger(AccountsController.class);
}
