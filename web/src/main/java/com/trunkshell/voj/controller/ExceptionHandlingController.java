package com.trunkshell.voj.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import com.trunkshell.voj.exception.ResourceNotFoundException;

/**
 * 异常处理的Controller.
 * @author Xie Haozhe
 */
@ControllerAdvice
public class ExceptionHandlingController {
	/**
	 * 处理ResourceNotFoundException异常的方法.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 返回一个包含异常信息的ModelAndView对象
	 */
	@ResponseStatus(value=HttpStatus.NOT_FOUND)
	@ExceptionHandler(ResourceNotFoundException.class)
	public ModelAndView resourceNotFoundView(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView view = new ModelAndView("errors/404");
		return view;
	}
	
	/**
	 * 处理通用Exception异常的方法.
	 * @param ex - 抛出的异常对象
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 返回一个包含异常信息的ModelAndView对象
	 */
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public ModelAndView internalServerErrorView(
			Exception ex, HttpServletRequest request, HttpServletResponse response) {
		logger.catching(ex);
		
		ModelAndView view = new ModelAndView("errors/500");
		return view;
	}
	
	/**
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(ExceptionHandlingController.class);
}
