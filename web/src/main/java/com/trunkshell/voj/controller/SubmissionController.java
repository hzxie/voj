package com.trunkshell.voj.controller;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.exception.ResourceNotFoundException;
import com.trunkshell.voj.model.Submission;
import com.trunkshell.voj.service.SubmissionService;

/**
 * 加载/显示评测的相关信息.
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/submission")
public class SubmissionController {
	/**
	 * 显示提交列表的页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含提交列表的ModelAndView对象 
	 */
	@RequestMapping(value = "")
    public ModelAndView submissionsView(
    		HttpServletRequest request, HttpServletResponse response) {
		List<Submission> submissions = submissionService.getSubmissions(NUMBER_OF_SUBMISSION_PER_PAGE);
		return new ModelAndView("submissions/submissions")
					.addObject("submissions", submissions);
	}
	
	/**
	 * 获取提交信息的列表.
	 * @param startIndex - 起始记录的提交唯一标识符
	 * @param request - HttpRequest对象
	 * @return 一个包含提交记录列表的HashMap对象
	 */
	@RequestMapping(value = "/getSubmissions.action")
	public @ResponseBody HashMap<String, Object> getSubmissionAction(
			@RequestParam(value="startIndex", required=true) long startIndex,
			HttpServletRequest request) {
		HashMap<String, Object> result = new HashMap<String, Object>(3, 1);

		List<Submission> submissions = submissionService.getSubmissions(startIndex, NUMBER_OF_SUBMISSION_PER_PAGE);
		result.put("isSuccessful", submissions != null && submissions.size() > 0);
		result.put("submissions", submissions);
		
		return result;
	}
	
	/**
	 * 显示提交详细信息的页面.
	 * @param submissionId - 提交的唯一标识符
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含提交详细信息的ModelAndView对象 
	 */
	@RequestMapping(value = "/{submissionId}")
    public ModelAndView submissionView(
    		@PathVariable("submissionId") int submissionId,
    	    HttpServletRequest request, HttpServletResponse response) {
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
