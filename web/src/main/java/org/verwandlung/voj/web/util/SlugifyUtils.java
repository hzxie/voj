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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Pattern;

/**
 * 字符串Slug处理类.
 * Ref: 
 * - https://github.com/slugify/slugify
 * - http://slugify.net/libraries
 * 
 * @author Haozhe Xie
 */
public class SlugifyUtils {
	/**
	 * 获取字符串的Slug.
	 * @param str - 待获取Slug的字符串
	 * @return 字符串对应的Slug
	 */
	public static String getSlug(String str) {
		if ( str == null ) {
			return "";
		}
		
		// Rid of White Spaces
		String noWhiteSpace = WHITESPACE.matcher(str.trim()).replaceAll("-");
		// Processing Non-ASCII Characters
		try {
			noWhiteSpace = URLEncoder.encode(noWhiteSpace, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Never reach here
		}
		// Slugify String
		String normalized = Normalizer.normalize(noWhiteSpace, Form.NFD);
		
		return normalized.toLowerCase();
	}
	
	/**
	 * 空格的正则表达式模式. 
	 */
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
}
