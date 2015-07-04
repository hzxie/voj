package com.trunkshell.voj.web.controller;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserGroup;
import com.trunkshell.voj.web.service.OptionService;
import com.trunkshell.voj.web.service.ProblemService;
import com.trunkshell.voj.web.service.SubmissionService;
import com.trunkshell.voj.web.service.UserService;
import com.trunkshell.voj.web.util.SessionListener;

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
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含系统管理页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView indexView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("administration/index");
		view.addObject("totalUsers", getTotalUsers());
		view.addObject("newUsersToday", getNumberOfUserRegisteredToday());
		view.addObject("onlineUsers", getOnlineUsers());
		view.addObject("totalProblems", getTotalProblems());
		view.addObject("numberOfCheckpoints", getNumberOfCheckpoints());
		view.addObject("privateProblems", getPrivateProblems());
		view.addObject("submissionsToday", getSubmissionsToday());
		view.addObject("productVersion", PRODUCT_VERSION);
		view.addObject("memoryUsage", getCurrentMemoryUsage());
		view.addObject("onlineJudgers", getOnlineJudgers());
		return view;
	}
	
	/**
	 * 获取系统中注册用户的总数.
	 * @return 系统中注册用户的总数
	 */
	private long getTotalUsers() {
		UserGroup userGroup = userService.getUserGroupUsingSlug("users");
		return userService.getNumberOfUsers(userGroup);
	}
	
	/**
	 * 获取今日注册的用户数量.
	 * @return 今日注册的用户数量
	 */
	public long getNumberOfUserRegisteredToday() {
		return userService.getNumberOfUserRegisteredToday();
	}
	
	/**
	 * 获取在线用户的数量.
	 * @return 在线用户的数量
	 */
	private long getOnlineUsers() {
		return SessionListener.getTotalSessions();
	}
	
	/**
	 * 获取全部试题的总数量.
	 * @return 全部试题的总数量
	 */
	private long getTotalProblems() {
		return problemService.getNumberOfProblems();
	}
	
	/**
	 * 获取私有试题的数量.
	 * @return 私有试题的数量
	 */
	private long getPrivateProblems() {
		return problemService.getNumberOfProblems(false);
	}
	
	/**
	 * 获取全部试题测试点的数量(包括私有试题).
	 * @return 全部试题测试点的数量
	 */
	private long getNumberOfCheckpoints() {
		return problemService.getNumberOfCheckpoints();
	}

	/**
	 * 获取今日的提交数量.
	 * @return 今日的提交数量
	 */
	private long getSubmissionsToday() {
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int date = calendar.get(Calendar.DAY_OF_MONTH);
		
		calendar.set(year, month, date, 0, 0, 0);
		Date startTime = calendar.getTime();
		
		calendar.set(year, month, date, 23, 59, 59);
		Date endTime = calendar.getTime();
		return submissionService.getNumberOfSubmissions(startTime, endTime);
	}
	
	/**
	 * 获取Web应用当前内存占用情况.
	 * @return Web应用当前内存占用(MB)
	 */
	private long getCurrentMemoryUsage() {
		long totalMemory = Runtime.getRuntime().totalMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		
		return (totalMemory - freeMemory) / 1048576;
	}
	
	/**
	 * 获取在线的评测机数量.
	 * 通过获取监听消息队列的Consumer数量.
	 * @return 在线的评测机数量
	 */
	private int getOnlineJudgers() {
		return 0;
	}
	
	/**
	 * 加载用户列表页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含用户列表页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "/all-users", method = RequestMethod.GET)
	public ModelAndView allUsersView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("administration/all-users");
		return view;
	}
	
	/**
	 * 自动注入的UserService对象.
	 */
	@Autowired
	private UserService userService;
	
	/**
	 * 自动注入的ProblemService对象.
	 */
	@Autowired
	private ProblemService problemService;
	
	/**
	 * 自动注入的SubmissionService对象.
	 */
	@Autowired
	private SubmissionService submissionService;

	/**
	 * 自动注入的OptionService对象.
	 */
	@Autowired
	private OptionService optionService;
	
	/**
	 * 产品版本信息.
	 */
	private final static String PRODUCT_VERSION = "0.1.0";
}
