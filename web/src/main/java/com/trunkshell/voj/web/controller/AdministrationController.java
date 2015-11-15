package com.trunkshell.voj.web.controller;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.trunkshell.voj.web.exception.ResourceNotFoundException;
import com.trunkshell.voj.web.messenger.ApplicationEventListener;
import com.trunkshell.voj.web.model.Language;
import com.trunkshell.voj.web.model.Option;
import com.trunkshell.voj.web.model.Submission;
import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserGroup;
import com.trunkshell.voj.web.service.LanguageService;
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
    private long getOnlineJudgers() {
        return eventListener.getOnlineJudgers();
    }
    
    /**
     * 加载用户列表页面.
     * @param request - HttpRequest对象
     * @param response - HttpResponse对象
     * @return 包含用户列表页面信息的ModelAndView对象
     */
    @RequestMapping(value = "/all-users", method = RequestMethod.GET)
    public ModelAndView allUsersView(
    		@RequestParam(value = "userGroup", required = false, defaultValue = "") String userGroupSlug,
    		@RequestParam(value = "username", required = false, defaultValue = "") String username,
    		@RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
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
     * 删除选定的提交记录.
     * @param submissions - 提交记录ID的集合, 以逗号(, )分割
     * @param request - HttpServletRequest对象
     * @return 提交记录的删除结果
     */
    @RequestMapping(value = "/deleteUsers.action", method = RequestMethod.POST)
    public @ResponseBody Map<String, Boolean> deleteUsersAction(
            @RequestParam(value = "users", required = true) String users,
            HttpServletRequest request) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(2, 1);
        List<Long> userList = JSON.parseArray(users, Long.class);
        
        for ( Long userId : userList ) {
            userService.deleteUser(userId);
        }
        result.put("isSuccessful", true);
        return result;
    }
    
    /**
     * 加载提交列表页面.
     * @param request - HttpServletRequest对象
     * @param response - HttpServletResponse对象
     * @return 包含提交列表页面信息的ModelAndView对象
     */
    @RequestMapping(value = "/all-submissions", method = RequestMethod.GET)
    public ModelAndView allSubmissionsView(
            @RequestParam(value = "problemId", required = false, defaultValue = "0") long problemId,
            @RequestParam(value = "username", required = false, defaultValue = "") String username,
            @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
            HttpServletRequest request, HttpServletResponse response) {
        final int NUMBER_OF_SUBMISSIONS_PER_PAGE = 100;
        
        long totalSubmissions = submissionService.getNumberOfSubmissions(null, null);
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
     * @param submissions - 提交记录ID的集合, 以逗号(, )分割
     * @param request - HttpServletRequest对象
     * @return 提交记录的删除结果
     */
    @RequestMapping(value = "/deleteSubmissions.action", method = RequestMethod.POST)
    public @ResponseBody Map<String, Boolean> deleteSubmissionsAction(
            @RequestParam(value = "submissions", required = true) String submissions,
            HttpServletRequest request) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(2, 1);
        List<Long> submissionList = JSON.parseArray(submissions, Long.class);
        
        for ( Long submissionId : submissionList ) {
            submissionService.deleteSubmission(submissionId);
        }
        result.put("isSuccessful", true);
        return result;
    }
    
    /**
     * 重新评测选定的提交记录.
     * @param submissions - 提交记录ID的集合, 以逗号(, )分割
     * @param request - HttpServletRequest对象
     * @return 重新评测请求的执行结果
     */
    @RequestMapping(value = "/restartSubmissions.action", method = RequestMethod.POST)
    public @ResponseBody Map<String, Boolean> restartSubmissionsAction(
            @RequestParam(value = "submissions", required = true) String submissions,
            HttpServletRequest request) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(2, 1);
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
    @RequestMapping(value = "/edit-submissions/{submissionId}", method = RequestMethod.GET)
    public ModelAndView editSubmissionView(
    		@PathVariable(value = "submissionId") long submissionId,
            HttpServletRequest request, HttpServletResponse response) {
        Submission submission = submissionService.getSubmission(submissionId);
        if ( submission == null ) {
            throw new ResourceNotFoundException();
        }
        ModelAndView view = new ModelAndView("administration/edit-submission");
        view.addObject("submission", submission);
        return view;
    }
    
    /**
     * 加载常规选项页面.
     * @param request - HttpRequest对象
     * @param response - HttpResponse对象
     * @return 包含常规选项页面信息的ModelAndView对象
     */
    @RequestMapping(value = "/general-settings", method = RequestMethod.GET)
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
        Map<String, String> optionMap = new HashMap<String, String>();
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
     * @param googleAnalyticsCode - Google Analytics代码
     * @param sensitiveWords - 敏感词列表
     * @param request - HttpServletRequest对象
     * @return 网站常规选项的更新结果
     */
    @RequestMapping(value = "/updateGeneralSettings.action", method = RequestMethod.POST)
    public @ResponseBody Map<String, Boolean> updateGeneralSettingsAction(
            @RequestParam(value = "websiteName", required = true) String websiteName,
            @RequestParam(value = "websiteDescription", required = true) String websiteDescription,
            @RequestParam(value = "copyright", required = true) String copyright,
            @RequestParam(value = "allowUserRegister", required = true) boolean allowUserRegister,
            @RequestParam(value = "icpNumber", required = true) String icpNumber,
            @RequestParam(value = "googleAnalyticsCode", required = true) String googleAnalyticsCode,
            @RequestParam(value = "sensitiveWords", required = true) String sensitiveWords,
            HttpServletRequest request) {
        Map<String, Boolean> result = optionService.updateOptions(websiteName, websiteDescription, 
                copyright, allowUserRegister, icpNumber, googleAnalyticsCode, sensitiveWords);
        return result;
    }
    
    /**
     * 加载编程语言设置页面.
     * @param request - HttpServletRequest对象
     * @param response - HttpServletResponse对象
     * @return 包含编程语言设置信息的ModelAndView对象
     */
    @RequestMapping(value = "/language-settings", method = RequestMethod.GET)
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
    @RequestMapping(value = "/updateLanguageSettings.action", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> updateLanguageSettingsAction(
            @RequestParam(value = "languages", required = true) String languages,
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
     * 产品版本信息.
     */
    private final static String PRODUCT_VERSION = "0.1.0";
}
