package com.happystudio.voj.aspect;

import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;

import com.happystudio.voj.model.User;
import com.happystudio.voj.service.SubmissionService;

/**
 * 视图的切面类.
 * 在加载页面前加载已登录用户的个人信息及答题情况. 
 * @author Xie Haozhe
 */
@Aspect
public class ViewAspect {
	/**
	 * 加载已登录用户的个人信息及答题情况.
	 * @param proceedingJoinPoint - ProceedingJoinPoint对象
	 * @param session - HttpSession对象
	 * @return 一个包含预期视图的ModelAndView对象
	 * @throws Throwable - ResourceNotFound异常
	 */
	@Around(value = "execution(* com.happystudio.voj.controller.*.*View(..)) && args(.., session)")
	public ModelAndView getUserProfile(ProceedingJoinPoint proceedingJoinPoint, HttpSession session) throws Throwable {
		ModelAndView view = null;
		view = (ModelAndView) proceedingJoinPoint.proceed();
		
		boolean isLoggedIn = isLoggedIn(session);
		if ( isLoggedIn ) {
        	User user = (User)session.getAttribute("user");
            view.addObject("isLogin", isLoggedIn)
            	.addObject("profile", user)
          		.addObject("submissionStats", submissionService.getUserSubmissionStats(user.getUid()));
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
     * 自动注入的UserService对象.
     */
    @Autowired
    SubmissionService submissionService;
}
