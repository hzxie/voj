package com.happystudio.voj.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

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
	 * @param model - 登录页面的Model
	 * @return 包含登录页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "/login")
    public ModelAndView loginView(
    		@RequestParam(value="logout", required=false, defaultValue="false") boolean isLogout,
    		HttpServletRequest request,
    		HttpSession session) {
		if ( isLogout ) {
			// destroySession(request, session);
		}
        return new ModelAndView("accounts/login");
    }
	
	/**
	 * 显示用户的注册页面
	 * @param model - 注册页面的Model
	 * @return 包含注册页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "/register")
	public ModelAndView registerView(HttpServletRequest request, HttpSession session) {
		return new ModelAndView("accounts/register");
	}
	
	/**
	 * 日志记录器.
	 */
	private Logger logger = LogManager.getLogger(AccountsController.class);
}
