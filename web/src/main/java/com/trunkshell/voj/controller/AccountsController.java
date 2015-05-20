package com.trunkshell.voj.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import com.trunkshell.voj.model.Language;
import com.trunkshell.voj.model.User;
import com.trunkshell.voj.service.LanguageService;
import com.trunkshell.voj.service.UserService;
import com.trunkshell.voj.util.CsrfProtector;
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
	 * @param fowardUrl - 登录后跳转的地址(相对路径)
	 * @param request - Http Servlet Request对象
	 * @param response - HttpResponse对象
	 * @return 包含登录页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView loginView(
			@RequestParam(value="logout", required=false, defaultValue="false") boolean isLogout,
			@RequestParam(value="forward", required=false, defaultValue="") String forwardUrl,
			HttpServletRequest request, HttpServletResponse response) {
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
			view.addObject("forwardUrl", forwardUrl);
		}
		return view;
	}
	
	/**
	 * 为注销的用户销毁Session.
	 * @param request - HttpServletRequest对象
	 * @param session - HttpSession 对象
	 */
	private void destroySession(HttpServletRequest request, HttpSession session) {
		User currentUser = HttpSessionParser.getCurrentUser(request.getSession());
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		logger.info(String.format("%s logged out at %s", new Object[] {currentUser, ipAddress}));
		
		session.setAttribute("isLoggedIn", false);
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
	 * 处理用户的登录请求.
	 * @param username - 用户名
	 * @param password - 密码(已使用MD5加密)
	 * @param request - Http Servlet Request对象
	 * @return 一个包含登录验证结果的Map<String, Boolean>对象
	 */
	@RequestMapping(value = "/login.action", method = RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> loginAction(
			@RequestParam(value="username", required=true) String username,
			@RequestParam(value="password", required=true) String password,
			@RequestParam(value="rememberMe", required=true) boolean isAutoLoginAllowed,
			HttpServletRequest request) {
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		Map<String, Boolean> result = userService.isAccountValid(username, password);
		logger.info(String.format("User: [Username=%s] tried to log in at %s", new Object[] {username, ipAddress}));
		if ( result.get("isSuccessful") ) {
			User user = userService.getUserUsingUsernameOrEmail(username);
			getSession(request, user, isAutoLoginAllowed);
		}
		return result;
	}

	/**
	 * 为登录的用户创建Session.
	 * @param request - HttpServletRequest对象
	 * @param user - 一个User对象, 包含用户的基本信息
	 * @param isAutoLoginAllowed - 是否保存登录状态
	 */
	private void getSession(HttpServletRequest request, User user, boolean isAutoLoginAllowed) {
		HttpSession session = request.getSession();
		session.setAttribute("isLoggedIn", true);
		session.setAttribute("isAutoLoginAllowed", isAutoLoginAllowed);
		session.setAttribute("uid", user.getUid());
		
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		logger.info(String.format("%s logged in at %s", new Object[] {user, ipAddress}));
	}
	
	/**
	 * 显示用户注册的页面.
	 * @param request - Http Servlet Request对象
	 * @param response - HttpResponse对象
	 * @return 包含注册页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "/register", method = RequestMethod.GET)
	public ModelAndView registerView(
			@RequestParam(value="forward", required=false, defaultValue="") String forwardUrl,
			HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		ModelAndView view = null;
		if ( isLoggedIn(session) ) {
			view = new ModelAndView("redirect:/");
		} else {
			List<Language> languages = languageService.getAllLanguages();
			
			view = new ModelAndView("accounts/register");
			view.addObject("languages", languages);
			view.addObject("csrfToken", CsrfProtector.getCsrfToken(session));
		}
		return view;
	}
	
	/**
	 * 处理用户注册的请求.
	 * @param username - 用户名
	 * @param password - 密码
	 * @param email - 电子邮件地址
	 * @param languageSlug - 偏好语言的唯一英文缩写
	 * @param csrfToken - Csrf的Token
	 * @param request - HttpRequest对象
	 * @return 一个包含账户创建结果的Map<String, Boolean>对象
	 */
	@RequestMapping(value = "/register.action", method = RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> registerAction(
			@RequestParam(value="username", required=true) String username,
			@RequestParam(value="password", required=true) String password,
			@RequestParam(value="email", required=true) String email,
			@RequestParam(value="languagePreference", required=true) String languageSlug,
			@RequestParam(value="csrfToken", required=true) String csrfToken,
			HttpServletRequest request) {
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, request.getSession());
		Map<String, Boolean> result = userService.createUser(username, password, email, languageSlug, isCsrfTokenValid);

		if ( result.get("isSuccessful") ) {
			User user = userService.getUserUsingUsernameOrEmail(username);
			getSession(request, user, false);
			
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			logger.info(String.format("User: [Username=%s] created at %s.", 
					new Object[] {username, ipAddress}));
		}
		return result;
	}
	
	/**
	 * 自动注入的UserService对象.
	 */
	@Autowired
	private UserService userService;
	
	/**
	 * 自动注入的LanguageService对象.
	 */
	@Autowired
	private LanguageService languageService;
	
	/**
	 * 日志记录器.
	 */
	private Logger logger = LogManager.getLogger(AccountsController.class);
}