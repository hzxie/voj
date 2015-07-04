package com.trunkshell.voj.web.util;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Session监听器.
 * 用于统计在线用户数据.
 * 
 * @author Xie Haozhe
 */
public class SessionListener implements HttpSessionListener {
	/**
	 * 获取在线用户数量.
	 * @return 在线用户数量
	 */
	public static long getTotalSessions() {
		return totalSessions;
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
	 */
	@Override
	public void sessionCreated(HttpSessionEvent se) {
		synchronized (this) {
			++ totalSessions;
        }
 
        System.out.println("Session Created: " + se.getSession().getId());
        System.out.println("Total Sessions: " + totalSessions);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
	 */
	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		synchronized (this) {
			-- totalSessions;
			
			if ( totalSessions < 0 ) {
				totalSessions = 0;
			}
        }
 
        System.out.println("Session Destroyed: " + se.getSession().getId());
        System.out.println("Total Sessions: " + totalSessions);
	}

	/**
	 * 活动的Session数量.
	 */
	private static long totalSessions = 0;
}