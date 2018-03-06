/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.aspect;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.web.servlet.view.RedirectView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.UserService;

/**
 * 拦截器的切面.
 * 用于完成系统的权限控制.
 * 
 * @author Haozhe Xie
 */
@Aspect
public class InterceptorAspect {
	/**
	 * 控制板视图的切面.
	 * 用于检查用户是否有权限加载控制板视图.
	 * @param proceedingJoinPoint - ProceedingJoinPoint对象
	 * @param request - HttpServletRequest对象
	 * @return 一个包含预期试图的ModelAndView对象
	 * @throws Throwable 
	 */
	@Around(value = "execution(* org.verwandlung.voj.web.controller.AccountsController.dashboardView(..)) && args(request, ..)")
	public ModelAndView dashboardViewInterceptor(ProceedingJoinPoint proceedingJoinPoint, 
			HttpServletRequest request) throws Throwable {
		ModelAndView view = null;
		HttpSession session = request.getSession();
		
		if ( !isAllowToAccess(session, new String[] { "users", "administrators" }) ) {
			RedirectView redirectView = new RedirectView(request.getContextPath() + "/accounts/login");
			redirectView.setExposeModelAttributes(false);
			return new ModelAndView(redirectView);
		}
		view = (ModelAndView) proceedingJoinPoint.proceed();
		return view;
	}
	
	/**
	 * 控制板中异步操作请求的切面.
	 * 检查用户是否有权限执行该操作.
	 * @param proceedingJoinPoint - ProceedingJoinPoint对象
	 * @param request - HttpServletRequest对象
	 * @return 预期的操作结果(Map<String, Boolean>对象)
	 * @throws Throwable
	 */
	@Around(value = "execution(* org.verwandlung.voj.web.controller.AccountsController.*InDashboardAction(..)) && args(.., request)")
	public Map<String, Boolean> dashboardActionInterceptor(ProceedingJoinPoint proceedingJoinPoint, 
			HttpServletRequest request) throws Throwable {
		HttpSession session = request.getSession();
		
		if ( !isAllowToAccess(session, new String[] { "users", "administrators" }) ) {
			throw new ResourceNotFoundException();
		}
		@SuppressWarnings("unchecked")
		Map<String, Boolean> result = (Map<String, Boolean>) proceedingJoinPoint.proceed();
		return result;
	}
	
	/**
	 * 系统管理控制器的切面.
	 * 用于检查用户是否有权限执行对应操作.
	 * @param proceedingJoinPoint - ProceedingJoinPoint对象
	 * @param request - HttpServletRequest对象
	 * @param request - HttpServletResponse对象
	 * @return 一个包含预期试图的ModelAndView对象
	 * @throws Throwable 
	 */
	@Around(value = "execution(* org.verwandlung.voj.web.controller.AdministrationController.*View(..)) && args(.., request, response)")
	public ModelAndView administrationViewInterceptor(ProceedingJoinPoint proceedingJoinPoint, 
			HttpServletRequest request, HttpServletResponse response) throws Throwable {
		ModelAndView view = null;
		HttpSession session = request.getSession();
		
		if ( !isAllowToAccess(session, new String[] { "administrators" }) ) {
			RedirectView redirectView = new RedirectView(request.getContextPath() + "/accounts/dashboard");
			redirectView.setExposeModelAttributes(false);
			return new ModelAndView(redirectView);
		}
		view = (ModelAndView) proceedingJoinPoint.proceed();
		return view;
	}
	
	/**
	 * 系统管理控制器的切面.
	 * 用于检查用户是否有权限执行对应操作.
	 * @param proceedingJoinPoint - ProceedingJoinPoint对象
	 * @param request - HttpRequest对象
	 * @return 一个包含预期结果的Map<String, Object>对象
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	@Around(value = "execution(* org.verwandlung.voj.web.controller.AdministrationController.*Action(..)) && args(.., request)")
	public Map<String, Object> AdministrationActionInterceptor(ProceedingJoinPoint proceedingJoinPoint, 
			HttpServletRequest request) throws Throwable {
		Map<String, Object> result = null;
		HttpSession session = request.getSession();
		
		if ( !isAllowToAccess(session, new String[] { "administrators" }) ) {
			return null;
		}
		result = (Map<String, Object>) proceedingJoinPoint.proceed();
		return result;
	}
	
	/**
	 * 检查用户是否有权限执行该操作.
	 * @param session - HttpSession对象
	 * @param expectedUserGroupSlugs - 允许执行该操作对应的用户组
	 * @return 用户是否有权限执行该操作 
	 */
	private boolean isAllowToAccess(HttpSession session, String[] expectedUserGroupSlugs) {
		Boolean isLoggedIn = (Boolean)session.getAttribute("isLoggedIn");
		if ( isLoggedIn == null || !isLoggedIn.booleanValue() ) {
			return false;
		}
		
		long uid = (Long)session.getAttribute("uid");
		User user = userService.getUserUsingUid(uid);
		String userGroupSlug = user.getUserGroup().getUserGroupSlug();
		
		for ( String expectedUserGroupSlug : expectedUserGroupSlugs ) {
			if ( userGroupSlug.equals(expectedUserGroupSlug) ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 自动注入的UserService对象.
	 * 用于查询用户所属的用户组.
	 */
	@Autowired
	private UserService userService;
}
