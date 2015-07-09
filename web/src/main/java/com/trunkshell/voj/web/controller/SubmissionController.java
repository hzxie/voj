package com.trunkshell.voj.web.controller;

import java.io.IOException;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.trunkshell.voj.web.exception.ResourceNotFoundException;
import com.trunkshell.voj.web.messenger.ApplicationEventListener;
import com.trunkshell.voj.web.model.Submission;
import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.service.SubmissionService;
import com.trunkshell.voj.web.util.CsrfProtector;
import com.trunkshell.voj.web.util.HttpSessionParser;

/**
 * 加载/显示评测的相关信息.
 * 
 * @author Xie Haozhe
 */
@Controller
@RequestMapping(value = "/submission")
public class SubmissionController {
    /**
     * 显示评测列表的页面.
     * @param request - HttpRequest对象
     * @param response - HttpResponse对象
     * @return 包含提交列表的ModelAndView对象 
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ModelAndView submissionsView(
            HttpServletRequest request, HttpServletResponse response) {
        List<Submission> submissions = submissionService.getSubmissions(NUMBER_OF_SUBMISSION_PER_PAGE);
        return new ModelAndView("submissions/submissions")
                    .addObject("submissions", submissions);
    }
    
    /**
     * 获取历史评测信息的列表.
     * @param startIndex - 当前加载的最后一条记录的提交唯一标识符
     * @param request - HttpRequest对象
     * @return 一个包含提交记录列表的HashMap对象
     */
    @RequestMapping(value = "/getSubmissions.action", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getSubmissionsAction(
            @RequestParam(value = "startIndex", required = true) long startIndex,
            HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>(3, 1);

        List<Submission> submissions = submissionService.getSubmissions(startIndex, NUMBER_OF_SUBMISSION_PER_PAGE);
        result.put("isSuccessful", submissions != null && submissions.isEmpty());
        result.put("submissions", submissions);
        
        return result;
    }
    
    /**
     * 获取最新的评测信息的列表.
     * @param startIndex - 当前加载的最新一条记录的提交唯一标识符
     * @param request - HttpRequest对象
     * @return 一个包含提交记录列表的HashMap对象
     */
    @RequestMapping(value = "/getLatestSubmissions.action", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getLatestSubmissionsAction(
            @RequestParam(value = "startIndex", required = true) long startIndex,
            HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>(3, 1);

        List<Submission> submissions = submissionService.getLatestSubmissions(startIndex, NUMBER_OF_SUBMISSION_PER_PAGE);
        result.put("isSuccessful", submissions != null && submissions.isEmpty());
        result.put("submissions", submissions);
        
        return result;
    }
    
    /**
     * 显示提交记录详细信息的页面.
     * @param submissionId - 提交记录的唯一标识符
     * @param request - HttpRequest对象
     * @param response - HttpResponse对象
     * @return 包含提交详细信息的ModelAndView对象 
     */
    @RequestMapping(value = "/{submissionId}", method = RequestMethod.GET)
    public ModelAndView submissionView(
            @PathVariable("submissionId") long submissionId,
            HttpServletRequest request, HttpServletResponse response) {
        Submission submission = submissionService.getSubmission(submissionId);
        if ( submission == null ) {
            throw new ResourceNotFoundException();
        }
        ModelAndView view = new ModelAndView("submissions/submission");
        view.addObject("submission", submission);
        view.addObject("csrfToken", CsrfProtector.getCsrfToken(request.getSession()));
        return view;
    }
    
    /**
     * 获取实时的评测结果.
     * @param submissionId - 提交记录的唯一标识符
     * @return 包含评测结果信息的StreamingResponseBody对象
     * @throws IOException 
     */
    @RequestMapping("/getRealTimeJudgeResult.action")
    public SseEmitter getRealTimeJudgeResultAction(
            @RequestParam(value = "submissionId", required = true) long submissionId,
            @RequestParam(value = "csrfToken", required = true) String csrfToken,
            HttpServletRequest request) throws IOException {
        User currentUser = HttpSessionParser.getCurrentUser(request.getSession());
        boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, request.getSession());
        Submission submission = submissionService.getSubmission(submissionId);
        
        if ( !isCsrfTokenValid || submission == null || 
                !submission.getUser().equals(currentUser) ||
                !submission.getJudgeResult().getJudgeResultSlug().equals("PD") ) {
            throw new ResourceNotFoundException();
        }
        
        SseEmitter sseEmitter = new SseEmitter();
        submissionEventListener.addSseEmitters(submissionId, sseEmitter);
        sseEmitter.send("Established");
        return sseEmitter;
    }
    
    /**
     * 获取提交记录的详细信息.
     * @param submissionId - 提交记录的唯一标识符
     * @param request - HttpRequest对象
     * @return 包含提交记录详细信息的HashMap对象
     */
    @RequestMapping(value = "/getSubmission.action", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getSubmissionAction(
            @RequestParam(value = "submissionId", required = true) long submissionId,
            HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>(3, 1);

        Submission submission = submissionService.getSubmission(submissionId);
        result.put("isSuccessful", submission != null);
        result.put("submission", submission);
        
        return result;
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
     * 自动注入的ApplicationEventListener对象.
     * 用于向其中注册sseEmitter.
     */
    @Autowired
    private ApplicationEventListener submissionEventListener;
}
