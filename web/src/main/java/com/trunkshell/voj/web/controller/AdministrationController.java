package com.trunkshell.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.service.UserService;

/**
 * 用于处理系统管理的请求.
 * 
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/administration")
public class AdministrationController {
	/**
	 * 加载系统管理首页.
	 * @param response - HttpResponse对象
	 * @param request - HttpRequest对象
	 * @return 包含系统管理页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView indexView(
			HttpServletResponse response, HttpServletRequest request) {
		ModelAndView view = new ModelAndView("administration/index");
		view.addObject("user", getAdministratorProfile(request.getSession()));
		return view;
	}
	
	/**
	 * 获取管理员用户的个人资料.
	 * @param session - HttpSession对象
	 * @return 管理员对应的用户对象
	 */
	private User getAdministratorProfile(HttpSession session) {
		Boolean isLoggedIn = (Boolean)session.getAttribute("isLoggedIn");
		if ( isLoggedIn == null || !isLoggedIn.booleanValue() ) {
			return null;
		}
		
		long uid = (Long)session.getAttribute("uid");
		User user = userService.getUserUsingUid(uid);		
		return user;
	}
	
	/**
	 * 自动注入的UserService对象.
	 */
	@Autowired
	private UserService userService;
}
