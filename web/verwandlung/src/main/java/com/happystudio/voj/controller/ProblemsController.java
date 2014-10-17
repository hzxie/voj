package com.happystudio.voj.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * 处理用户的查看试题/提交评测等请求.
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/p")
public class ProblemsController {
	/**
	 * 显示试题库中的全部试题.
	 * @param startIndex - 试题的起始下标
	 * @param request - Http Servlet Request对象
     * @param session - Http Session对象
	 * @return 包含试题库页面信息的ModelAndView对象
	 */
	@RequestMapping(value = "")
    public ModelAndView problemsView(
            @RequestParam(value="start", required = false, defaultValue = "1") int startIndex,
            HttpServletRequest request, HttpSession session) {
        return new ModelAndView("problems/problems");
    }
	
	@RequestMapping(value = "/{problemID}")
    public ModelAndView problemView(
    		@PathVariable("problemID") int problemID,
    		HttpServletRequest request, HttpSession session) {
        return new ModelAndView("problems/problem");
    }
}
