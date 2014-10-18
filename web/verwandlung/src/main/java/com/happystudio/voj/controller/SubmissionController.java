package com.happystudio.voj.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.happystudio.voj.service.SubmissionService;

/**
 * 处理用户的提交评测请求.
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/submission")
public class SubmissionController {
	@RequestMapping(value = "")
    public ModelAndView submissionsView(
    		@RequestParam(value="start", required = false, defaultValue = "1") int startIndex,
            HttpServletRequest request, HttpSession session) {
		if ( startIndex < START_INDEX_OF_SUBMISSION ) {
			startIndex = START_INDEX_OF_SUBMISSION;
		}
		return new ModelAndView("submission/submissions")
					.addObject("submissions", submissionService.getSubmissions(START_INDEX_OF_SUBMISSION, NUMBER_OF_SUBMISSION_PER_PAGE));
	}
	
	/**
	 * 系统中评测记录的起始序号.
	 */
	private static final int START_INDEX_OF_SUBMISSION = 1;
	
	/**
	 * 每次请求所加载评测记录的数量.
	 */
	private static final int NUMBER_OF_SUBMISSION_PER_PAGE = 100;
	
	/**
     * 自动注入的UserService对象.
     */
    @Autowired
    SubmissionService submissionService;
    
    /**
     * 日志记录器.
     */
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger(SubmissionController.class);
}
