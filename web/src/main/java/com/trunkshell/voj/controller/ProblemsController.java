package com.trunkshell.voj.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.exception.ResourceNotFoundException;
import com.trunkshell.voj.model.Problem;
import com.trunkshell.voj.model.Submission;
import com.trunkshell.voj.service.ProblemService;
import com.trunkshell.voj.service.SubmissionService;

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
            HttpServletRequest request) {
		if ( startIndex < START_INDEX_OF_PROBLEMS ) {
			startIndex = START_INDEX_OF_PROBLEMS;
		}
        
		ModelAndView view = new ModelAndView("problems/problems");
        view.addObject("problems", problemService.getProblems(startIndex, NUMBER_OF_PROBLEMS_PER_PAGE))
        	.addObject("startIndexOfProblems", START_INDEX_OF_PROBLEMS)
        	.addObject("numberOfProblemsPerPage", NUMBER_OF_PROBLEMS_PER_PAGE)
        	.addObject("totalProblems", problemService.getNumberOfProblems());
        
        HttpSession session = request.getSession();
        if ( isLoggedIn(session) ) {
        	long userId = (Long)session.getAttribute("uid");
        	Map<Long, Submission> submissionOfProblems = submissionService.getSubmissionOfProblems(userId, startIndex, startIndex + NUMBER_OF_PROBLEMS_PER_PAGE);
        	view.addObject("submissionOfProblems", submissionOfProblems);
        }
        return view;
    }
	
	/**
	 * 检查用户是否已经登录.
	 * @param session - HttpSession 对象
	 * @return 用户是否已经登录
	 */
	private boolean isLoggedIn(HttpSession session) {
		Boolean isLoggedIn = (Boolean)session.getAttribute("isLoggedIn");
		if ( isLoggedIn == null || !isLoggedIn.booleanValue() ) {
			return false;
		}
		return true;
	}
	
	/**
	 * 加载试题的详细信息.
	 * @param problemID - 试题的唯一标识符
	 * @param request - Http Servlet Request对象
     * @param session - Http Session对象
	 * @return 包含试题详细信息的ModelAndView对象
	 */
	@RequestMapping(value = "/{problemId}")
    public ModelAndView problemView(
    		@PathVariable("problemId") int problemId,
    		HttpServletRequest request) {
		Problem problem = problemService.getProblem(problemId);
		if ( problem == null || !problem.isPublic() ) {
			throw new ResourceNotFoundException();
		}
		
		ModelAndView view = new ModelAndView("problems/problem");
        view.addObject("problem", problem);
        
        HttpSession session = request.getSession();
        if ( isLoggedIn(session) ) {
        	long userId = (Long)session.getAttribute("uid");
        	Map<Long, Submission> submissionOfProblems = submissionService.getSubmissionOfProblems(userId, problemId, problemId + 1);
        	List<Submission> submissions = submissionService.getSubmissionUsingProblemIdAndUserId(problemId, userId, NUMBER_OF_SUBMISSIONS_PER_PROBLEM);
        	
        	view.addObject("latestSubmission", submissionOfProblems);
        	view.addObject("submissions", submissions);
        }
        return view;
    }
	
	/**
	 * 系统中试题的起始序号.
	 */
	private static final int START_INDEX_OF_PROBLEMS = 1000;
	
	/**
	 * 每次请求所加载试题数量.
	 */
	private static final int NUMBER_OF_PROBLEMS_PER_PAGE = 100;
	
	/**
	 * 每个试题加载最近提交的数量.
	 */
	private static final int NUMBER_OF_SUBMISSIONS_PER_PROBLEM = 10;
	
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
     * 日志记录器.
     */
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger(ProblemsController.class);
}
