package com.trunkshell.voj.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 程序测评模块的加载器.
 * 
 * @author Xie Haozhe
 */
public class ApplicationBootstrap {
	/**
	 * 应用程序入口.
	 */
	public static void main(String[] args) {
		ApplicationBootstrap app = new ApplicationBootstrap();
		app.getApplicationContext();
	}

	/**
	 * 加载应用程序配置.
	 */
	private void getApplicationContext() {
		applicationContext = new 
				ClassPathXmlApplicationContext("application-context.xml");
	}

	/**
	 * 应用程序配置.
	 */
	private ApplicationContext applicationContext;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(ApplicationBootstrap.class);
}
