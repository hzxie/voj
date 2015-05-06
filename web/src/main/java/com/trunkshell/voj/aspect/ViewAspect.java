package com.trunkshell.voj.aspect;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.model.User;
import com.trunkshell.voj.service.SubmissionService;
import com.trunkshell.voj.service.UserService;

/**
 * 视图的切面类.
 * 在加载页面前加载已登录用户的个人信息及答题情况. 
 * @author Xie Haozhe
 */
@Aspect
public class ViewAspect {
	/**
	 * 加载已登录用户的个人信息及答题情况.
	 * @param proceedingJoinPoint - ProceedingJoinPoint对象
	 * @param request - HttpRequest对象
	 * @return 一个包含预期视图的ModelAndView对象
	 * @throws Throwable - ResourceNotFound异常
	 */
	@Around(value = "execution(* com.trunkshell.voj.controller.*.*View(..)) && args(.., request)")
	public ModelAndView getUserProfile(ProceedingJoinPoint proceedingJoinPoint, HttpServletRequest request) throws Throwable {
		ModelAndView view = null;
		HttpSession session = request.getSession();
		
		view = (ModelAndView) proceedingJoinPoint.proceed();
		view.addObject("language", getUserLanguage(request, session));
		
		boolean isLoggedIn = isLoggedIn(session);
		if ( isLoggedIn ) {
			long uid = (Long)session.getAttribute("uid");
			User user = userService.getUserUsingUid(uid);
			
			view.addObject("isLogin", isLoggedIn)
				.addObject("user", user)
				.addObject("submissionStats", submissionService.getUserSubmissionStats(user.getUid()));
		}
		return view;
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
	 * 获取当前用户的显示语言.
	 * @param request - HttpRequest对象
	 * @param session - HttpSession对象
	 * @return 当前用户显示语言的唯一英文缩写
	 */
	private String getUserLanguage(HttpServletRequest request, HttpSession session) {
		Object languageAttribute = session.getAttribute("language");
		
		if ( languageAttribute == null ) {
			String preferNaturalLanguage = getPreferNaturalLanguage(request);
			session.setAttribute("language", preferNaturalLanguage);
			return preferNaturalLanguage;
		}
		return (String) languageAttribute;
	}
	
	/**
	 * 根据用户浏览器语言和系统支持的语言推荐默认语言.
	 * @param request - HttpRequest对象
	 * @return 推荐语言的代码(例如zh_CN)
	 */
	private String getPreferNaturalLanguage(HttpServletRequest request) {
		final String DEFAULT_LANGUAGE = "en_US";
		final String[] supportedLanguages = { "en_US", "zh_CN" };
		Locale browserLocale = getBrowserLocale(request);
		
		for ( String supportedLanguage : supportedLanguages ) {
			Locale supportLanguageLocale = getLocaleOfSupportedLanguage(supportedLanguage);
			if ( supportLanguageLocale.getLanguage().equals(browserLocale.getLanguage()) ) {
				return supportedLanguage;
			}
		}
		return DEFAULT_LANGUAGE;
	}
	
	/**
	 * 根据浏览器语言获取用户所在地区.
	 * @param request - HttpRequest对象
	 * @return 一个包含用户所在地区信息的Locale对象
	 */
	private Locale getBrowserLocale(HttpServletRequest request) {
		Locale locale = request.getLocale();
		return locale;
	}
	
	/**
	 * 根据IETF Language Tag获取对应的Locale对象.
	 * @param languageName - 语言的名称(例如zh_CN)
	 * @return 预期的Locale对象
	 */
	private Locale getLocaleOfSupportedLanguage(String languageName) {
		String[] localeMeta = languageName.split("_");
		String language = localeMeta[0];
		String country = localeMeta[1];
		
		return new Locale(language, country);
	}
	
	/**
	 * 自动注入的UserService对象.
	 */
	@Autowired
	private UserService userService;
	
	/**
	 * 自动注入的SubmissionService对象.
	 */
	@Autowired
	private SubmissionService submissionService;
}
