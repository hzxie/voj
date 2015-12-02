package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * 处理比赛的相关请求.
 * 
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/contests")
public class ContestsController {
    /**
     * 显示比赛列表页面.
     * @param request - HttpRequest对象
     * @param response - HttpResponse对象
     * @return 一个包含比赛列表页面内容的ModelAndView对象
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ModelAndView contestsView(
            HttpServletRequest request, HttpServletResponse response) {
        ModelAndView view = new ModelAndView("contests/contests");
        return view;
    }
}
