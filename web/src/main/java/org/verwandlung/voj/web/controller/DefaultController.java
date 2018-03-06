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
package org.verwandlung.voj.web.controller;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.service.*;
import org.verwandlung.voj.web.util.LocaleUtils;

/**
 * 处理应用程序公共的请求.
 * 
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value="/")
public class DefaultController {
	/**
	 * 显示应用程序的首页.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含首页内容的ModelAndView对象
	 */
	@RequestMapping(value="/", method=RequestMethod.GET)
	public ModelAndView indexView(
			HttpServletRequest request, HttpServletResponse response) {
		List<Contest> contests = contestService.getContests(null, 0, NUMBER_OF_CONTESTS_PER_REQUEST);
		List<DiscussionThread> discussionThreads = discussionService.getDiscussionThreadsOfTopic(
				null, 0, NUMBER_OF_DISCUSSION_THREADS_PER_REQUEST);
		List<BulletinBoardMessage> bulletinBoardMessages = bulletinBoardService.getBulletinBoardMessages(
				0, NUMBER_OF_BULLETIN_MESSAGES_PER_REQUEST);

		ModelAndView view = new ModelAndView("index");
		view.addObject("currentTime", new Date());
		view.addObject("contests", contests);
		view.addObject("discussionThreads", discussionThreads);
		view.addObject("bulletinBoardMessages", bulletinBoardMessages);
		return view;
	}
	
	/**
	 * 显示使用条款页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含使用条款页面内容的ModelAndView对象
	 */
	@RequestMapping(value="/terms", method=RequestMethod.GET)
	public ModelAndView termsView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/terms");
		return view;
	}

	/**
	 * 显示隐私页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含隐私页内面容的ModelAndView对象
	 */
	@RequestMapping(value="/privacy", method=RequestMethod.GET)
	public ModelAndView privacyView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/privacy");
		return view;
	}

	/**
	 * 显示评测机信息页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含评测机信息页面内容的ModelAndView对象
	 */
	@RequestMapping(value="/judgers", method=RequestMethod.GET)
	public ModelAndView judgersView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/judgers");
		view.addObject("languages", languageService.getAllLanguages());
		return view;
	}
	
	/**
	 * 获取评测机列表.
	 * @param offset - 当前加载评测机的UID
	 * @param request - HttpRequest对象
	 * @return 一个包含评测机列表信息的List<Map<String, String>>对象
	 */
	@RequestMapping(value="/getJudgers.action", method=RequestMethod.GET)
	public @ResponseBody Map<String, Object> getJudgersAction(
			@RequestParam(value="startIndex", required=false, defaultValue="0") long offset,
			HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		List<Map<String, String>> judgers = getJudgers(offset);
		
		result.put("isSuccessful", judgers != null && !judgers.isEmpty());
		result.put("judgers", judgers);
		return result;
	}
	
	/**
	 * 获取评测机的详细信息.
	 * @param offset - 当前加载评测机的UID
	 * @return 包含评测机的详细信息的List<Map<String, String>>对象
	 */
	private List<Map<String, String>> getJudgers(long offset) {
		UserGroup userGroup = userService.getUserGroupUsingSlug("judgers");
		List<User> judgersList = userService.getUserUsingUserGroup(userGroup, offset, NUMBER_OF_JUDGERS_PER_REQUEST);
		List<Map<String, String>> judgers = new ArrayList<Map<String, String>>();
		
		for ( User judger : judgersList ) {
			Map<String, String> judgerInformation = new HashMap<>(3, 1);
			String username = judger.getUsername();
			String description = keepAliveEventListener.getJudgerDescription(username);
			
			judgerInformation.put("username", username);
			judgerInformation.put("description", description);
			judgers.add(judgerInformation);
		}
		return judgers;
	}
	
	/**
	 * 显示帮助页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含帮助页面内容的ModelAndView对象
	 */
	@RequestMapping(value="/help", method=RequestMethod.GET)
	public ModelAndView helpView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/help");
		return view;
	}
	
	/**
	 * 显示关于页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含关于页面内容的ModelAndView对象
	 */
	@RequestMapping(value="/about", method=RequestMethod.GET)
	public ModelAndView aboutView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("misc/about");
		return view;
	}
	
	/**
	 * 显示语言切换的页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 一个包含语言切换页面内容的ModelAndView对象
	 */
	@RequestMapping(value="/worldwide", method=RequestMethod.GET)
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
	@RequestMapping(value="/worldwide.action", method=RequestMethod.GET)
	public @ResponseBody Map<String, Boolean> localizationAction(
			@RequestParam(value="language") String language,
			HttpServletRequest request, HttpServletResponse response) {
		LocaleUtils.setLocale(request, response, language);
		
		Map<String, Boolean> result = new HashMap<>(2, 1);
		result.put("isSuccessful", true);
		return result;
	}
	
	/**
	 * 对于所有未正常映射URL的页面, 显示页面未找到.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 返回一个包含异常信息的ModelAndView对象
	 */
	@RequestMapping(value="/*", method=RequestMethod.GET)
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
	@RequestMapping(value="/not-supported", method=RequestMethod.GET)
	public ModelAndView notSupportedView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("errors/not-supported");
		return view;
	}
	
	/**
	 * 每次加载评测机的数量.
	 */
	private static final int NUMBER_OF_JUDGERS_PER_REQUEST = 10;

	/**
	 * 每次加载讨论帖子的数量.
	 */
	private static final int NUMBER_OF_DISCUSSION_THREADS_PER_REQUEST = 10;

	/**
	 * 每次加载竞赛的数量.
	 */
	private static final int NUMBER_OF_CONTESTS_PER_REQUEST = 5;

	/**
	 * 每次加载布告栏消息的数量.
	 */
	private static final int NUMBER_OF_BULLETIN_MESSAGES_PER_REQUEST = 10;
	
	/**
	 * 自动注入的UserService对象.
	 * 用于获取评测机页面的评测机列表.
	 */
	@Autowired
	private UserService userService;
	
	/**
	 * 自动注入的LanguageService对象.
	 * 用于获取评测机页面的编译命令.
	 */
	@Autowired
	private LanguageService languageService;

	/**
	 * 自动注入的ContestService对象.
	 * 用于获取最新的竞赛信息.
	 */
	@Autowired
	private ContestService contestService;

	/**
	 * 自动注入的DiscussionService对象.
	 * 用于在首页获取讨论帖子.
	 */
	@Autowired
	private DiscussionService discussionService;

	/**
	 * 自动注入的BulletinBoardService对象.
	 * 用于获取布告板的最新消息.
	 */
	@Autowired
	private BulletinBoardService bulletinBoardService;
	
	/**
	 * 自动注入的ApplicationEventListener对象.
	 * 用于获取评测机的在线状态.
	 */
	@Autowired
	private ApplicationEventListener keepAliveEventListener;
}
