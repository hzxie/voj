package com.trunkshell.voj.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 通过过滤器抓取session
 * 
 * @author Luo Guofu
 *
 */
public class SessionLoaderFilter implements Filter {
	@SuppressWarnings("unused")
	private FilterConfig filterConfig;
	
	private static HttpSession session;

	public static HttpSession getSession() {
		return session;
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		this.filterConfig = config;
	}

	@Override
	public void destroy() {
		this.filterConfig = null;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
	           FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		session = req.getSession();
		chain.doFilter(request, response);
	}
}
