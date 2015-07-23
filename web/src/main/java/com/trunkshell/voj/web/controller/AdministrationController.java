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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.web.messenger.ApplicationEventListener;
import com.trunkshell.voj.web.model.Option;
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
            HttpServletRequest request, HttpServletResponse response) {
        ModelAndView view = new ModelAndView("administration/all-users");
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
     * @param request - HttpRequest对象
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
     * 用于获取系统中的设置选项.
     */
    @Autowired
    private OptionService optionService;
    
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
