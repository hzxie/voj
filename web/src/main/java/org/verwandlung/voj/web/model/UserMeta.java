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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 用户元信息.
 * 
 * @author Haozhe Xie
 */
public class UserMeta implements Serializable {
	/**
	 * UserMeta类的默认构造函数.
	 */
	public UserMeta() { }
	
	/**
	 * UserMeta类的构造函数.
	 * @param user - 用户对象
	 * @param metaKey - 元信息的键
	 * @param metaValue - 元信息的值
	 */
	public UserMeta(User user, String metaKey, String metaValue) {
		this.user = user;
		this.metaKey = metaKey;
		this.metaValue = metaValue;
	}
	
	/**
	 * UserMeta类的构造函数.
	 * @param metaId - 用户元信息的唯一标识符
	 * @param user - 用户对象
	 * @param metaKey - 元信息的键
	 * @param metaValue - 元信息的值
	 */
	public UserMeta(long metaId, User user, String metaKey, String metaValue) {
		this(user, metaKey, metaValue);
		this.metaId = metaId;
	}
	
	/**
	 * 获取用户元信息的唯一标识符.
	 * @return 用户元信息的唯一标识符
	 */
	public long getMetaId() {
		return metaId;
	}

	/**
	 * 设置用户元信息的唯一标识符.
	 * @param metaId - 用户元信息的唯一标识符
	 */
	public void setMetaId(long metaId) {
		this.metaId = metaId;
	}
	
	/**
	 * 获取元信息对应的用户对象.
	 * @return 元信息对应的用户对象
	 */
	public User getUser() {
		return user;
	}

	/**
	 * 设置元信息对应的用户对象.
	 * @param user - 元信息对应的用户对象
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * 获取元信息的键.
	 * @return 元信息的键
	 */
	public String getMetaKey() {
		return metaKey;
	}

	/**
	 * 设置元信息的键.
	 * @param metaKey - 元信息的键
	 */
	public void setMetaKey(String metaKey) {
		this.metaKey = metaKey;
	}

	/**
	 * 获取元信息的值.
	 * @return 元信息的值
	 */
	public String getMetaValue() {
		return metaValue;
	}

	/**
	 * 设置元信息的值.
	 * @param metaValue - 元信息的值
	 */
	public void setMetaValue(String metaValue) {
		this.metaValue = metaValue;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("UserMeta: [ID=%s, User={%s}, Key=%s, Value=%s]", 
				new Object[] { metaId, user, metaKey, metaValue});
	}

	/**
	 * 用户元信息的唯一标识符. 
	 */
	private long metaId;
	
	/**
	 * 元信息对应的用户对象.
	 */
	private User user;
	
	/**
	 * 元信息的键.
	 */
	private String metaKey;
	
	/**
	 * 元信息的值.
	 */
	private String metaValue;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 1362748371508853051L;
}
