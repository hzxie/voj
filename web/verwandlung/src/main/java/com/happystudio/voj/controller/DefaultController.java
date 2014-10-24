package com.happystudio.voj.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

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
     * @param session - HttpSession对象
     * @return 一个包含首页信息的ModelAndView对象
     */
    @RequestMapping(value = "/*", method = RequestMethod.GET)
    public ModelAndView indexView(HttpServletRequest request, HttpSession session) {
        ModelAndView view = new ModelAndView("index");
        return view;
    }
    
    /**
     * 日志记录器.
     */
    @SuppressWarnings("unused")
    private Logger logger = LogManager.getLogger(DefaultController.class);
}
