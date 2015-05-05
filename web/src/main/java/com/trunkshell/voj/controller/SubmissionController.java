package com.trunkshell.voj.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.exception.ResourceNotFoundException;
import com.trunkshell.voj.model.Submission;
import com.trunkshell.voj.service.SubmissionService;

/**
 * 处理用户的提交评测请求.
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/submission")
public class SubmissionController {
	@RequestMapping(value = "")
    public ModelAndView submissionsView(HttpServletRequest request) {
		List<Submission> submissions = submissionService.getSubmissions(NUMBER_OF_SUBMISSION_PER_PAGE);
		return new ModelAndView("submissions/submissions")
					.addObject("submissions", submissions);
	}
	
	@RequestMapping(value = "/{submissionId}")
    public ModelAndView submissionView(
    		@PathVariable("submissionId") int submissionId,
    	    HttpServletRequest request) {
		Submission submission = submissionService.getSubmission(submissionId);
		if ( submission == null ) {
			throw new ResourceNotFoundException();
		}
		ModelAndView view = new ModelAndView("submissions/submission");
        view.addObject("submission", submission);
        return view;
	}
	
	/**
	 * 每次请求所加载评测记录的数量.
	 */
	private static final int NUMBER_OF_SUBMISSION_PER_PAGE = 100;
	
	/**
     * 自动注入的SubmissionService对象.
     */
    @Autowired
    private SubmissionService submissionService;
    
    /**
     * 日志记录器.
     */
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger(SubmissionController.class);
}
