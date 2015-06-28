package com.trunkshell.voj.web.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.web.util.LocaleUtils;

/**
 * 处理应用程序公共的请求.
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/")
public class DefaultController {
	/**
	 * 显示应用程序的首页.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含首页内容的ModelAndView对象
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView indexView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("index");
		return view;
	}
	
	/**
	 * 显示语言切换的页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含语言切换页面内容的ModelAndView对象
	 */
	@RequestMapping(value = "/worldwide", method = RequestMethod.GET)
	public ModelAndView worldwideView(
			@RequestParam(value="forward", required=false, defaultValue="") String forwardUrl,
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/worldwide");
		view.addObject("forwardUrl", forwardUrl);
		return view;
	}

	/**
	 * 处理用户切换语言的请求.
	 * @param language - 需要切换的语言代码
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 语言切换操作结果的HashMap<String, Boolean>对象
	 */
	@RequestMapping(value = "/worldwide.action", method = RequestMethod.GET)
	public @ResponseBody Map<String, Boolean> localizationAction(
			@RequestParam(value="language", required=true) String language,
			HttpServletRequest request, HttpServletResponse response) {
		LocaleUtils.setLocale(request, response, language);
		
		Map<String, Boolean> result = new HashMap<String, Boolean>(2, 1);
		result.put("isSuccessful", true);
		return result;
	}
	
	/**
	 * 对于所有未正常映射URL的页面, 显示页面未找到.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 返回一个包含异常信息的ModelAndView对象
	 */
	@RequestMapping(value = "/*", method = RequestMethod.GET)
	public ModelAndView notFoundView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("errors/404");
		return view;
	}
	
	/**
	 * 显示升级浏览器页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含升级浏览器页面内容的ModelAndView对象
	 */
	@RequestMapping(value = "/not-supported", method = RequestMethod.GET)
	public ModelAndView notSupportedView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/not-supported");
		return view;
	}
}
