package com.trunkshell.voj.controller;

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
import com.trunkshell.voj.service.ProblemService;

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
		if ( startIndex < START_INDEX_OF_PROBLEMS ) {
			startIndex = START_INDEX_OF_PROBLEMS;
		}
        
		ModelAndView view = new ModelAndView("problems/problems");
        view.addObject("problems", problemService.getProblems(startIndex, NUMBER_OF_PROBLEMS_PER_PAGE))
        	.addObject("startIndexOfProblems", START_INDEX_OF_PROBLEMS)
        	.addObject("numberOfProblemsPerPage", NUMBER_OF_PROBLEMS_PER_PAGE)
        	.addObject("totalProblems", problemService.getNumberOfProblems());
        return view;
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
    		HttpServletRequest request, HttpSession session) {
		Problem problem = problemService.getProblem(problemId);
		if ( problem == null || !problem.isPublic() ) {
			throw new ResourceNotFoundException();
		}
		
		ModelAndView view = new ModelAndView("problems/problem");
        view.addObject("problem", problem);
        
        // view.addObject("submission", submissionService.getSubmissionUsingProblemIDAndUserID(problemID, userID, limit));
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
     * 自动注入的UserService对象.
     */
    @Autowired
    ProblemService problemService;
    
    /**
     * 日志记录器.
     */
	@SuppressWarnings("unused")
	private Logger logger = LogManager.getLogger(ProblemsController.class);
}
