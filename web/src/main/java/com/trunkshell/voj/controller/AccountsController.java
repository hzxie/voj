package com.trunkshell.voj.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.model.User;
import com.trunkshell.voj.util.HttpRequestParser;
import com.trunkshell.voj.util.HttpSessionParser;

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
            HttpServletRequest request) {
    	HttpSession session = request.getSession();
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
        
        User currentUser = HttpSessionParser.getCurrentUser(request.getSession());
        String ipAddress = HttpRequestParser.getRemoteAddr(request);
        logger.info(String.format("%s logged out at %s", new Object[] {currentUser, ipAddress}));
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
     * 日志记录器.
     */
    private Logger logger = LogManager.getLogger(AccountsController.class);
}