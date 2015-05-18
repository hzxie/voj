package com.trunkshell.voj.util;

import javax.servlet.http.HttpSession;

/**
 * CSRF防护服务.
 * 说明: Spring Security中所集成的服务过于复杂, 因此没有使用.
 * @author Xie Haozhe
 */
public class CsrfProtector {
	/**
	 * 生成CSRF的Token, 并存放于Session中以作验证.
	 * @param session - HttpSession对象
	 * @return 生成CSRF的Token
	 */
	public static String getCsrfToken(HttpSession session) {
		String csrfToken = DigestUtils.getGuid();
		session.setAttribute("CsrfToken", csrfToken);
		
		return csrfToken;
	}
	
	/**
	 * 验证CSRF的Token是否正确.
	 * @param csrfToken - CSRF的Token
	 * @param session - HttpSession对象
	 * @return CSRF的Token是否正确
	 */
	public static boolean isCsrfTokenValid(String csrfToken, HttpSession session) {
		Object csrfTokenAttribute = session.getAttribute("CsrfToken");
		
		if ( csrfTokenAttribute == null ) {
			return false;
		}
		String generatedToken = (String)csrfTokenAttribute;
		
		if ( !generatedToken.equals(csrfToken) ) {
			return false;
		}
		return true;
	}
}
