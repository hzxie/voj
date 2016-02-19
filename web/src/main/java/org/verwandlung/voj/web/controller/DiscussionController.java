package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * 处理讨论的相关请求.
 * 
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/discussion")
public class DiscussionController {
    /**
     * 显示讨论主题列表页面.
     * @param request - HttpRequest对象
     * @param response - HttpResponse对象
     * @return 包含讨论主题列表页面内容的ModelAndView对象
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ModelAndView discussionView(
            HttpServletRequest request, HttpServletResponse response) {
        ModelAndView view = new ModelAndView("discussion/discussion");
        return view;
    }
}
