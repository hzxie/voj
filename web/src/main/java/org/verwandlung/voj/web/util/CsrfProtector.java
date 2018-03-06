/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.util;

import java.util.Map;

import javax.servlet.http.HttpSession;

/**
 * CSRF防护服务.
 * 说明: Spring Security中所集成的服务过于复杂, 因此没有使用.
 * 
 * @author Haozhe Xie
 */
public class CsrfProtector {
	/**
	 * Utility classes should not have a public constructor.
	 */
	private CsrfProtector() { }
	
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
	 * @param csrfToken - 待验证的CSRF Token
	 * @param session - HttpSession对象
	 * @return CSRF的Token是否正确
	 */
	public static boolean isCsrfTokenValid(String csrfToken, HttpSession session) {
		Object csrfTokenAttribute = session.getAttribute("CsrfToken");
		return isCsrfTokenValid(csrfToken, csrfTokenAttribute);
	}
	
	/**
	 * 验证CSRF的Token是否正确.
	 * @param csrfToken - 待验证的CSRF Token
	 * @param sessionAttributes - Session中的全部属性
	 * @return CSRF的Token是否正确
	 */
	public static boolean isCsrfTokenValid(String csrfToken, Map<String, Object> sessionAttributes) {
		Object csrfTokenAttribute = sessionAttributes.get("CsrfToken");
		return isCsrfTokenValid(csrfToken, csrfTokenAttribute);
	}
	
	/**
	 * 验证CSRF的Token是否正确.
	 * @param csrfToken - 待验证的CSRF Token
	 * @param csrfTokenAttribute - Session中Csrf Token的属性值
	 * @return CSRF的Token是否正确
	 */
	private static boolean isCsrfTokenValid(String csrfToken, Object csrfTokenAttribute) {
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
