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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Locale相关的辅助函数.
 * @author Haozhe Xie
 */
public class LocaleUtils {
	/**
	 * Utility classes should not have a public constructor.
	 */
	private LocaleUtils() { }
	
	/**
	 * 根据IETF Language Tag获取对应的Locale对象.
	 * @param languageName - 语言的名称(例如zh_CN)
	 * @return 预期的Locale对象
	 */
	public static Locale getLocaleOfLanguage(String languageName) {
		String[] localeMeta = languageName.split("_");
		String language = localeMeta[0];
		String country = localeMeta[1];
		
		return new Locale(language, country);
	}

	/**
	 * 根据用户语言设置Locale信息.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @param language - 语言的名称(例如zh_CN)
	 */
	public static void setLocale(HttpServletRequest request, HttpServletResponse response, String language) {
		Locale locale = LocaleUtils.getLocaleOfLanguage(language);
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		localeResolver.setLocale(request, response, locale);
		request.getSession().setAttribute("language", language);
	}
}
