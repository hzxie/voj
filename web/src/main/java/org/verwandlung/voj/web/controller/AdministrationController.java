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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.Checkpoint;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.CsrfProtector;
import org.verwandlung.voj.web.util.DateUtils;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.SessionListener;

/**
 * 用于处理系统管理的请求.
 * 
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value="/administration")
public class AdministrationController {
	/**
	 * 加载系统管理首页.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含系统管理页面信息的ModelAndView对象
	 */
	@RequestMapping(value="", method=RequestMethod.GET)
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
		long numberOfTotalProblems = getTotalProblems();
		long numberOfPublicProblems = problemService.getNumberOfProblemsUsingFilters(null, "", true);
		return numberOfTotalProblems - numberOfPublicProblems;
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
		
		calendar.set(year, month, date + 1, 0, 0, 0);
		Date endTime = calendar.getTime();
		return submissionService.getNumberOfSubmissionsUsingDate(startTime, endTime);
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
	private long getOnlineJudgers() {
		return eventListener.getOnlineJudgers();
	}
	
	/**
	 * 获取系统一段时间内的提交次数.
	 * @param period - 时间间隔的天数
	 * @param request - HttpServletRequest对象
	 * @return 包含提交次数与时间的 Map 对象
	 */
	@RequestMapping(value="/getNumberOfSubmissions.action", method=RequestMethod.GET)
	public @ResponseBody Map<String, Object> getNumberOfSubmissionsAction(
			@RequestParam(value="period") int period,
			HttpServletRequest request) {
		Map<String, Object> submissions = new HashMap<>(2, 1);
		Date today = new Date();
		Date previousDate = DateUtils.getPreviousDate(period);
		Map<String, Long> totalSubmissions = submissionService.getNumberOfSubmissionsUsingDate(previousDate, today, 0, false);
		Map<String, Long> acceptedSubmissions = submissionService.getNumberOfSubmissionsUsingDate(previousDate, today, 0, true);
		
		submissions.put("totalSubmissions", totalSubmissions);
		submissions.put("acceptedSubmissions", acceptedSubmissions);
		return submissions;
	}
	
	/**
	 * 加载用户列表页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含用户列表页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/all-users", method=RequestMethod.GET)
	public ModelAndView allUsersView(
			@RequestParam(value="userGroup", required=false, defaultValue="") String userGroupSlug,
			@RequestParam(value="username", required=false, defaultValue="") String username,
			@RequestParam(value="page", required=false, defaultValue="1") long pageNumber,
			HttpServletRequest request, HttpServletResponse response) {
		final int NUMBER_OF_USERS_PER_PAGE = 100;
		List<UserGroup> userGroups = userService.getUserGroups();
		UserGroup userGroup = userService.getUserGroupUsingSlug(userGroupSlug);
		long totalUsers = userService.getNumberOfUsersUsingUserGroupAndUsername(userGroup, username);
		long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_USERS_PER_PAGE;
		List<User> users = userService.getUserUsingUserGroupAndUsername(userGroup, username, offset, NUMBER_OF_USERS_PER_PAGE);
		
		ModelAndView view = new ModelAndView("administration/all-users");
		view.addObject("userGroups", userGroups);
		view.addObject("selectedUserGroup", userGroupSlug);
		view.addObject("username", username);
		view.addObject("currentPage", pageNumber);
		view.addObject("totalPages", (long) Math.ceil(totalUsers * 1.0 / NUMBER_OF_USERS_PER_PAGE));
		view.addObject("users", users);
		return view;
	}
	
	/**
	 * 删除选定的用户.
	 * @param users - 用户ID的集合, 以逗号(, )分隔
	 * @param request - HttpServletRequest对象
	 * @return 提交记录的删除结果
	 */
	@RequestMapping(value="/deleteUsers.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> deleteUsersAction(
			@RequestParam(value="users") String users,
			HttpServletRequest request) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		List<Long> userList = JSON.parseArray(users, Long.class);
		
		for ( Long userId : userList ) {
			userService.deleteUser(userId);
		}
		result.put("isSuccessful", true);
		return result;
	}
	
	/**
	 * 加载编辑用户信息的页面.
	 * @param userId - 用户的唯一标识符
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含编辑用户信息的ModelAndView对象
	 */
	@RequestMapping(value="/edit-user/{userId}", method=RequestMethod.GET)
	public ModelAndView editUserView(
			@PathVariable(value = "userId") long userId,
			HttpServletRequest request, HttpServletResponse response) {
		User user = userService.getUserUsingUid(userId);
		Map<String, Object> userMeta = userService.getUserMetaUsingUid(user);
		if ( user == null ) {
			throw new ResourceNotFoundException();
		}
		
		List<UserGroup> userGroups = userService.getUserGroups();
		List<Language> languages = languageService.getAllLanguages();
		ModelAndView view = new ModelAndView("administration/edit-user");
		view.addObject("user", user);
		view.addAllObjects(userMeta);
		view.addObject("userGroups", userGroups);
		view.addObject("languages", languages);
		return view;
	}
	
	/**
	 * 编辑用户个人信息.
	 * @param uid - 用户的唯一标识符.
	 * @param password - 用户的密码(未经MD5加密)
	 * @param email - 用户的电子邮件地址
	 * @param userGroupSlug - 用户组的别名
	 * @param preferLanguageSlug - 用户的偏好语言的别名
	 * @param location - 用户的所在地区
	 * @param website - 用户的个人主页
	 * @param socialLinks - 用户的社交网络信息
	 * @param aboutMe - 用户的个人简介
	 * @param request - HttpServletRequest对象
	 * @return 一个包含个人资料修改结果的Map<String, Boolean>对象
	 */
	@RequestMapping(value="/editUser.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> editUserAction(
			@RequestParam(value="uid") long uid,
			@RequestParam(value="password") String password,
			@RequestParam(value="email") String email,
			@RequestParam(value="userGroup") String userGroupSlug,
			@RequestParam(value="preferLanguage") String preferLanguageSlug,
			@RequestParam(value="location") String location,
			@RequestParam(value="website") String website,
			@RequestParam(value="socialLinks") String socialLinks,
			@RequestParam(value="aboutMe") String aboutMe,
			HttpServletRequest request) {
		User user = userService.getUserUsingUid(uid);
		Map<String, Boolean> result = new HashMap<>(12, 1);
		result.put("isSuccessful", false);
		result.put("isUserExists", false);
		
		if ( user != null ) {
			Map<String, Boolean> updateProfileResult = userService.updateProfile(user, password, userGroupSlug, preferLanguageSlug);
			Map<String, Boolean> updateUserMetaResult = userService.updateProfile(user, email, location, website, socialLinks, aboutMe);
			boolean isUpdateProfileSuccessful = updateProfileResult.get("isSuccessful");
			boolean isUpdateUserMetaSuccessful = updateUserMetaResult.get("isSuccessful");
			
			result.putAll(updateProfileResult);
			result.putAll(updateUserMetaResult);
			result.put("isUserExists", true);
			result.put("isSuccessful", isUpdateProfileSuccessful && isUpdateUserMetaSuccessful);
		}
		return result;
	}
	
	/**
	 * 加载创建用户页面.
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含创建用户页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/new-user", method=RequestMethod.GET)
	public ModelAndView newUserView(
			HttpServletRequest request, HttpServletResponse response) {
		List<UserGroup> userGroups = userService.getUserGroups();
		List<Language> languages = languageService.getAllLanguages();
		ModelAndView view = new ModelAndView("administration/new-user");
		view.addObject("userGroups", userGroups);
		view.addObject("languages", languages);
		return view;
	}

	/**
	 * 创建新用户.
	 * @param username - 用户名
	 * @param password - 密码
	 * @param email - 电子邮件地址
	 * @param userGroupSlug - 用户组的别名
	 * @param preferLanguageSlug - 偏好语言的别名
	 * @param request - HttpServletRequest对象
	 * @return 一个包含账户创建结果的Map<String, Boolean>对象
	 */
	@RequestMapping(value="/newUser.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> newUserAction(
			@RequestParam(value="username") String username,
			@RequestParam(value="password") String password,
			@RequestParam(value="email") String email,
			@RequestParam(value="userGroup") String userGroupSlug,
			@RequestParam(value="preferLanguage") String preferLanguageSlug,
			HttpServletRequest request) {
		Map<String, Boolean> result = userService.createUser(username, password, email, userGroupSlug, preferLanguageSlug);

		if ( result.get("isSuccessful") ) {
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			LOGGER.info(String.format("User: [Username=%s] was created by administrator at %s.", 
					new Object[] {username, ipAddress}));
		}
		return result;
	}
	
	/**
	 * 加载试题列表页面.
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含提交列表页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/all-problems", method=RequestMethod.GET)
	public ModelAndView allProblemsView(
			@RequestParam(value="keyword", required=false, defaultValue="") String keyword,
			@RequestParam(value="problemCategory", required=false, defaultValue="") String problemCategorySlug,
			@RequestParam(value="problemTag", required=false, defaultValue="") String problemTagSlug,
			@RequestParam(value="page", required=false, defaultValue="1") long pageNumber,
			HttpServletRequest request, HttpServletResponse response) {
		final int NUMBER_OF_PROBLEMS_PER_PAGE = 100;
		List<ProblemCategory> problemCategories = problemService.getProblemCategories();
		long totalProblems = problemService.getNumberOfProblemsUsingFilters(keyword, problemCategorySlug, false);

		long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_PROBLEMS_PER_PAGE;
		long problemIdLowerBound = problemService.getFirstIndexOfProblems() + offset;
		long problemIdUpperBound = problemIdLowerBound + NUMBER_OF_PROBLEMS_PER_PAGE - 1;

		List<Problem> problems = problemService.getProblemsUsingFilters(problemIdLowerBound, keyword, problemCategorySlug, problemTagSlug, false, NUMBER_OF_PROBLEMS_PER_PAGE);
		Map<Long, List<ProblemCategory>> problemCategoryRelationships =
				problemService.getProblemCategoriesOfProblems(problemIdLowerBound, problemIdUpperBound);
		Map<Long, List<ProblemTag>> problemTagRelationships =
				problemService.getProblemTagsOfProblems(problemIdLowerBound, problemIdUpperBound);

		ModelAndView view = new ModelAndView("administration/all-problems");
		view.addObject("problemCategories", problemCategories);
		view.addObject("selectedProblemCategory", problemCategorySlug);
		view.addObject("keyword", keyword);
		view.addObject("currentPage", pageNumber);
		view.addObject("totalPages", (long) Math.ceil(totalProblems * 1.0 / NUMBER_OF_PROBLEMS_PER_PAGE));
		view.addObject("problems", problems);
		view.addObject("problemCategoryRelationships", problemCategoryRelationships);
		view.addObject("problemTagRelationships", problemTagRelationships);
		return view;
	}

	/**
	 * 删除选定的试题.
	 * @param problems - 试题ID的集合, 以逗号(, )分隔
	 * @param request - HttpServletRequest对象
	 * @return 试题的删除结果
	 */
	@RequestMapping(value="/deleteProblems.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> deleteProblemsAction(
			@RequestParam(value="problems") String problems,
			HttpServletRequest request) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		List<Long> problemList = JSON.parseArray(problems, Long.class);
		
		for ( Long problemId : problemList ) {
			problemService.deleteProblem(problemId);
			
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			LOGGER.info(String.format("Problem: [ProblemId=%s] was deleted by administrator at %s.", 
					new Object[] {problemId, ipAddress}));
		}
		result.put("isSuccessful", true);
		return result;
	}
	
	/**
	 * 加载创建试题页面.
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含创建试题页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/new-problem", method=RequestMethod.GET)
	public ModelAndView newProblemView(
			HttpServletRequest request, HttpServletResponse response) {
		Map<ProblemCategory, List<ProblemCategory>> problemCategories = problemService.getProblemCategoriesWithHierarchy();
		
		ModelAndView view = new ModelAndView("administration/new-problem");
		view.addObject("problemCategories", problemCategories);
		return view;
	}
	
	/**
	 * 处理用户创建试题的请求.
	 * @param problemName - 试题名称
	 * @param timeLimit - 时间限制
	 * @param memoryLimit - 内存占用限制
	 * @param description - 试题描述
	 * @param hint - 试题提示
	 * @param inputFormat - 输入格式
	 * @param outputFormat - 输出格式
	 * @param inputSample - 输入样例
	 * @param outputSample - 输出样例
	 * @param testCases - 测试用例(JSON 格式)
	 * @param problemCategories - 试题分类(JSON 格式)
	 * @param problemTags - 试题标签((JSON 格式)
	 * @param isPublic - 试题是否公开
	 * @param isExactlyMatch - 测试点是否精确匹配
	 * @param request - HttpServletRequest对象
	 * @return 包含试题创建结果的 Map<String, Boolean>对象
	 */
	@RequestMapping(value="/createProblem.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> createProblemAction(
			@RequestParam(value="problemName") String problemName,
			@RequestParam(value="timeLimit") String timeLimit, 
			@RequestParam(value="memoryLimit") String memoryLimit, 
			@RequestParam(value="description") String description, 
			@RequestParam(value="hint") String hint, 
			@RequestParam(value="inputFormat") String inputFormat, 
			@RequestParam(value="outputFormat") String outputFormat, 
			@RequestParam(value="inputSample") String inputSample, 
			@RequestParam(value="outputSample") String outputSample, 
			@RequestParam(value="testCases") String testCases, 
			@RequestParam(value="problemCategories") String problemCategories, 
			@RequestParam(value="problemTags") String problemTags, 
			@RequestParam(value="isPublic") boolean isPublic, 
			@RequestParam(value="isExactlyMatch") boolean isExactlyMatch,
			HttpServletRequest request) {
		if ( timeLimit.isEmpty() || !StringUtils.isNumeric(timeLimit) ) {
			timeLimit = "-1";
		}
		if ( memoryLimit.isEmpty() || !StringUtils.isNumeric(memoryLimit) ) {
			memoryLimit = "-1";
		}
		Map<String, Object> result = problemService.createProblem(problemName, Integer.parseInt(timeLimit), 
				Integer.parseInt(memoryLimit), description, hint, inputFormat, outputFormat, inputSample, 
				outputSample, testCases, problemCategories, problemTags, isPublic, isExactlyMatch);
		
		if ( (boolean) result.get("isSuccessful") ) {
			long problemId = (Long) result.get("problemId");
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			
			LOGGER.info(String.format("Problem: [ProblemId=%s] was created by administrator at %s.", 
					new Object[] {problemId, ipAddress}));
		}
		return result;
	}
	
	/**
	 * 加载编辑试题页面.
	 * @param problemId - 试题的唯一标识符
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含提交列表页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/edit-problem/{problemId}", method=RequestMethod.GET)
	public ModelAndView editProblemsView(
			@PathVariable(value = "problemId") long problemId,
			HttpServletRequest request, HttpServletResponse response) {
		Problem problem = problemService.getProblem(problemId);
		
		if ( problem == null ) {
			throw new ResourceNotFoundException();
		}
		List<Checkpoint> checkpoints = problemService.getCheckpointsUsingProblemId(problemId);
		List<ProblemCategory> selectedProblemCategories = problemService.getProblemCategoriesUsingProblemId(problemId);
		Map<ProblemCategory, List<ProblemCategory>> problemCategories = problemService.getProblemCategoriesWithHierarchy();
		List<ProblemTag> problemTags = problemService.getProblemTagsUsingProblemId(problemId);
		
		ModelAndView view = new ModelAndView("administration/edit-problem");
		view.addObject("problem", problem);
		view.addObject("checkpoints", checkpoints);
		view.addObject("problemCategories", problemCategories);
		view.addObject("selectedProblemCategories", selectedProblemCategories);
		view.addObject("problemTags", problemTags);
		return view;
	}
	
	/**
	 * 处理用户编辑试题的请求.
	 * @param problemName - 试题名称
	 * @param timeLimit - 时间限制
	 * @param memoryLimit - 内存占用限制
	 * @param description - 试题描述
	 * @param hint - 试题提示
	 * @param inputFormat - 输入格式
	 * @param outputFormat - 输出格式
	 * @param inputSample - 输入样例
	 * @param outputSample - 输出样例
	 * @param testCases - 测试用例(JSON 格式)
	 * @param problemCategories - 试题分类(JSON 格式)
	 * @param problemTags - 试题标签((JSON 格式)
	 * @param isPublic - 试题是否公开
	 * @param isExactlyMatch - 测试点是否精确匹配
	 * @param request - HttpServletRequest对象
	 * @return 包含试题编辑结果的 Map<String, Boolean>对象
	 */
	@RequestMapping(value="/editProblem.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> editProblemAction(
			@RequestParam(value="problemId") long problemId,
			@RequestParam(value="problemName") String problemName,
			@RequestParam(value="timeLimit") String timeLimit, 
			@RequestParam(value="memoryLimit") String memoryLimit, 
			@RequestParam(value="description") String description, 
			@RequestParam(value="hint") String hint, 
			@RequestParam(value="inputFormat") String inputFormat, 
			@RequestParam(value="outputFormat") String outputFormat, 
			@RequestParam(value="inputSample") String inputSample, 
			@RequestParam(value="outputSample") String outputSample, 
			@RequestParam(value="testCases") String testCases, 
			@RequestParam(value="problemCategories") String problemCategories, 
			@RequestParam(value="problemTags") String problemTags, 
			@RequestParam(value="isPublic") boolean isPublic, 
			@RequestParam(value="isExactlyMatch") boolean isExactlyMatch,
			HttpServletRequest request) {
		if ( timeLimit.isEmpty() || !StringUtils.isNumeric(timeLimit) ) {
			timeLimit = "-1";
		}
		if ( memoryLimit.isEmpty() || !StringUtils.isNumeric(memoryLimit) ) {
			memoryLimit = "-1";
		}
		Map<String, Boolean> result = problemService.editProblem(problemId, problemName, Integer.parseInt(timeLimit), 
				Integer.parseInt(memoryLimit), description, hint, inputFormat, outputFormat, inputSample, 
				outputSample, testCases, problemCategories, problemTags, isPublic, isExactlyMatch);
		
		if ( result.get("isSuccessful") ) {
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			
			LOGGER.info(String.format("Problem: [ProblemId=%s] was edited by administrator at %s.", 
					new Object[] {problemId, ipAddress}));
		}
		return result;
	}
	
	/**
	 * 加载试题分类页面.
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含试题分类页面信息的ModelAndView对象.
	 */
	@RequestMapping(value="/problem-categories", method=RequestMethod.GET)
	public ModelAndView problemCategoriesView(
			HttpServletRequest request, HttpServletResponse response) {
		List<ProblemCategory> problemCategories = problemService.getProblemCategories();
		
		ModelAndView view = new ModelAndView("administration/problem-categories");
		view.addObject("problemCategories", problemCategories);
		return view;
	}
	
	/**
	 * 创建试题分类.
	 * @param problemCategorySlug - 试题分类的别名
	 * @param problemCategoryName - 试题分类的名称
	 * @param parentProblemCategorySlug - 父级试题分类的别名
	 * @param request - HttpServletRequest对象
	 * @return 包含试题分类的创建结果的Map<String, Object>对象
	 */
	@RequestMapping(value="/createProblemCategory.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> createProblemCategoryAction(
			@RequestParam(value="problemCategorySlug") String problemCategorySlug,
			@RequestParam(value="problemCategoryName") String problemCategoryName,
			@RequestParam(value="parentProblemCategory") String parentProblemCategorySlug,
			HttpServletRequest request) {
		Map<String, Object> result = problemService.createProblemCategory(
				problemCategorySlug, problemCategoryName, parentProblemCategorySlug);
		
		if ( (boolean) result.get("isSuccessful") ) {
			long problemCategoryId = (Long) result.get("problemCategoryId");
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			
			LOGGER.info(String.format("ProblemCategory: [ProblemCategoryId=%s] was created by administrator at %s.", 
					new Object[] {problemCategoryId, ipAddress}));
		}
		return result;
	}
	
	/**
	 * 编辑试题分类.
	 * @param problemCategoryId - 试题分类的唯一标识符
	 * @param problemCategorySlug - 试题分类的别名
	 * @param problemCategoryName - 试题分类的名称
	 * @param parentProblemCategorySlug - 父级试题分类的别名
	 * @param request - HttpServletRequest对象
	 * @return 包含试题分类的编辑结果的Map<String, Boolean>对象
	 */
	@RequestMapping(value="/editProblemCategory.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> editProblemCategoryAction(
			@RequestParam(value="problemCategoryId") String problemCategoryId,
			@RequestParam(value="problemCategorySlug") String problemCategorySlug,
			@RequestParam(value="problemCategoryName") String problemCategoryName,
			@RequestParam(value="parentProblemCategory") String parentProblemCategorySlug,
			HttpServletRequest request) {
		Map<String, Boolean> result = problemService.editProblemCategory(
				Integer.parseInt(problemCategoryId), problemCategorySlug, 
				problemCategoryName, parentProblemCategorySlug);

		if ( result.get("isSuccessful") ) {
			String ipAddress = HttpRequestParser.getRemoteAddr(request);

			LOGGER.info(String.format("ProblemCategory: [ProblemCategoryId=%s] was edited by administrator at %s.",
					new Object[] {problemCategoryId, ipAddress}));
		}
		return result;
	}

	/**
	 * 删除试题分类.
	 * @param problemCategories - 试题分类的唯一标识符集合
	 * @param request - HttpServletRequest对象
	 * @return 包含试题分类的删除结果的Map<String, Boolean>对象
	 */
	@RequestMapping(value="/deleteProblemCategories.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> deleteProblemCategoryAction(
			@RequestParam(value="problemCategories") String problemCategories,
			HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>(3, 1);
		List<Integer> problemCategoryList = JSON.parseArray(problemCategories, Integer.class);
		List<Integer> deletedProblemCategories = new ArrayList<>();

		for ( int problemCategoryId : problemCategoryList ) {
			if ( problemService.deleteProblemCategory(problemCategoryId) ) {
				deletedProblemCategories.add(problemCategoryId);
			}
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			LOGGER.info(String.format("ProblemCategory: [ProblemCategoryId=%s] was deleted by administrator at %s.",
					new Object[] {problemCategoryId, ipAddress}));
		}
		result.put("isSuccessful", true);
		result.put("deletedProblemCategories", deletedProblemCategories);
		return result;
	}
	
	/**
	 * 加载提交列表页面.
	 * @param problemId - 提交对应试题的唯一标识符
	 * @param username - 提交者的用户名
	 * @param pageNumber - 当前页面的页码
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含提交列表页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/all-submissions", method=RequestMethod.GET)
	public ModelAndView allSubmissionsView(
			@RequestParam(value="problemId", required=false, defaultValue="0") long problemId,
			@RequestParam(value="username", required=false, defaultValue="") String username,
			@RequestParam(value="page", required=false, defaultValue="1") long pageNumber,
			HttpServletRequest request, HttpServletResponse response) {
		final int NUMBER_OF_SUBMISSIONS_PER_PAGE = 100;
		
		long totalSubmissions = submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(problemId, username);
		long latestSubmissionId = submissionService.getLatestSubmissionId();
		long offset = latestSubmissionId - (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_SUBMISSIONS_PER_PAGE;
		List<Submission> submissions = submissionService.getSubmissions(problemId, username, offset, NUMBER_OF_SUBMISSIONS_PER_PAGE);
		
		ModelAndView view = new ModelAndView("administration/all-submissions");
		view.addObject("problemId", problemId);
		view.addObject("username", username);
		view.addObject("currentPage", pageNumber);
		view.addObject("totalPages", (long) Math.ceil(totalSubmissions * 1.0 / NUMBER_OF_SUBMISSIONS_PER_PAGE));
		view.addObject("submissions", submissions);
		return view;
	}
	
	/**
	 * 删除选定的提交记录.
	 * @param submissions - 提交记录ID的集合, 以逗号(, )分隔
	 * @param request - HttpServletRequest对象
	 * @return 提交记录的删除结果
	 */
	@RequestMapping(value="/deleteSubmissions.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> deleteSubmissionsAction(
			@RequestParam(value="submissions") String submissions,
			HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>(3, 1);
		List<Long> submissionList = JSON.parseArray(submissions, Long.class);
		List<Long> deletedSubmissions = new ArrayList<>();

		for ( Long submissionId : submissionList ) {
			if ( submissionService.deleteSubmission(submissionId) ) {
				deletedSubmissions.add(submissionId);
			}
			String ipAddress = HttpRequestParser.getRemoteAddr(request);
			LOGGER.info(String.format("Submission: [SubmissionId=%s] deleted by administrator at %s.", 
					new Object[] {submissionId, ipAddress}));
		}
		result.put("isSuccessful", true);
		result.put("deletedSubmissions", deletedSubmissions);
		return result;
	}
	
	/**
	 * 重新评测选定的提交记录.
	 * @param submissions - 提交记录ID的集合, 以逗号(, )分隔
	 * @param request - HttpServletRequest对象
	 * @return 重新评测请求的执行结果
	 */
	@RequestMapping(value="/restartSubmissions.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> restartSubmissionsAction(
			@RequestParam(value="submissions") String submissions,
			HttpServletRequest request) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		List<Long> submissionList = JSON.parseArray(submissions, Long.class);
		
		for ( Long submissionId : submissionList ) {
			submissionService.createSubmissionTask(submissionId);
		}
		result.put("isSuccessful", true);
		return result;
	}
	
	/**
	 * 查看提交记录.
	 * @param submissionId - 提交记录的唯一标识符
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含提交记录信息的ModelAndView对象
	 */
	@RequestMapping(value="/edit-submission/{submissionId}", method=RequestMethod.GET)
	public ModelAndView editSubmissionView(
			@PathVariable(value = "submissionId") long submissionId,
			HttpServletRequest request, HttpServletResponse response) {
		Submission submission = submissionService.getSubmission(submissionId);
		if ( submission == null ) {
			throw new ResourceNotFoundException();
		}
		ModelAndView view = new ModelAndView("administration/edit-submission");
		view.addObject("submission", submission);
		view.addObject("csrfToken", CsrfProtector.getCsrfToken(request.getSession()));
		return view;
	}
	
	/**
	 * 加载常规选项页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含常规选项页面信息的ModelAndView对象
	 */
	@RequestMapping(value="/general-settings", method=RequestMethod.GET)
	public ModelAndView generalSettingsView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("administration/general-settings");
		view.addObject("options", getOptions());
		return view;
	}
	
	/**
	 * 获取系统全部的选项, 以键值对的形式返回.
	 * @return 键值对形式的系统选项
	 */
	private Map<String, String> getOptions() {
		Map<String, String> optionMap = new HashMap<>();
		List<Option> options = optionService.getOptions();
		
		for ( Option option : options ) {
			optionMap.put(option.getOptionName(), option.getOptionValue());
		}
		return optionMap;
	}
	
	/**
	 * 更新网站常规选项.
	 * @param websiteName - 网站名称
	 * @param websiteDescription - 网站描述
	 * @param copyright - 网站版权信息
	 * @param allowUserRegister - 是否允许用户注册
	 * @param icpNumber - 网站备案号
	 * @param policeIcpNumber - 公安备案号
	 * @param googleAnalyticsCode - Google Analytics代码
	 * @param offensiveWords - 敏感词列表
	 * @param request - HttpServletRequest对象
	 * @return 网站常规选项的更新结果
	 */
	@RequestMapping(value="/updateGeneralSettings.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> updateGeneralSettingsAction(
			@RequestParam(value="websiteName") String websiteName,
			@RequestParam(value="websiteDescription") String websiteDescription,
			@RequestParam(value="copyright") String copyright,
			@RequestParam(value="allowUserRegister") boolean allowUserRegister,
			@RequestParam(value="icpNumber") String icpNumber,
			@RequestParam(value="policeIcpNumber") String policeIcpNumber,
			@RequestParam(value="googleAnalyticsCode") String googleAnalyticsCode,
			@RequestParam(value="offensiveWords") String offensiveWords,
			HttpServletRequest request) {
		Map<String, Boolean> result = optionService.updateOptions(websiteName, websiteDescription, 
				copyright, allowUserRegister, icpNumber, policeIcpNumber, googleAnalyticsCode, offensiveWords);
		return result;
	}
	
	/**
	 * 加载编程语言设置页面.
	 * @param request - HttpServletRequest对象
	 * @param response - HttpServletResponse对象
	 * @return 包含编程语言设置信息的ModelAndView对象
	 */
	@RequestMapping(value="/language-settings", method=RequestMethod.GET)
	public ModelAndView languageSettingsView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("administration/language-settings");
		view.addObject("languages", languageService.getAllLanguages());
		return view;
	}
	
	/**
	 * 更新网站编程语言选项.
	 * @param languages - 包含编程语言设置的数组
	 * @param request - HttpServletRequest对象
	 * @return 编程语言选项的更新结果
	 */
	@RequestMapping(value="/updateLanguageSettings.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> updateLanguageSettingsAction(
			@RequestParam(value="languages") String languages,
			HttpServletRequest request) {
		List<Language> languagesList = JSON.parseArray(languages, Language.class);
		Map<String, Object> result = languageService.updateLanguageSettings(languagesList);
		return result;
	}
	
	/**
	 * 自动注入的UserService对象.
	 */
	@Autowired
	private UserService userService;
	
	/**
	 * 自动注入的ProblemService对象.
	 * 用于获取试题记录信息.
	 */
	@Autowired
	private ProblemService problemService;
	
	/**
	 * 自动注入的SubmissionService对象.
	 * 用于获取提交记录信息.
	 */
	@Autowired
	private SubmissionService submissionService;

	/**
	 * 自动注入的OptionService对象.
	 * 用于获取系统中的设置选项.
	 */
	@Autowired
	private OptionService optionService;
	
	/**
	 * 自动注入的LanguageService对象.
	 * 用于获取系统中的编程语言选项.
	 */
	@Autowired
	private LanguageService languageService;
	
	/**
	 * 自动注入的ApplicationEventListener对象.
	 * 用于获取在线评测机的数量.
	 */
	@Autowired
	private ApplicationEventListener eventListener;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(AdministrationController.class);
}
