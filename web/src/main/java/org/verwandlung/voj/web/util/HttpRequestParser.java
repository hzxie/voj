package org.verwandlung.voj.web.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Http请求头解析服务.
 * @author Xie Haozhe
 */
public class HttpRequestParser {
    /**
     * Utility classes should not have a public constructor.
     */
    private HttpRequestParser() { }
    
    /**
     * 在使用反向代理情况下解析用户的真实IP.
     * @param request - HttpRequest对象
     * @return 用户的真实IP
     */
    public static String getRemoteAddr(HttpServletRequest request) {
        if ( request.getHeader("X-Real-IP") != null ) {
            return request.getHeader("X-Real-IP");
        }
        return request.getRemoteAddr();
    }
}
