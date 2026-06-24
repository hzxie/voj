/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
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
 */
package org.verwandlung.voj.web.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.verwandlung.voj.web.util.SessionListener;

/**
 * Servlet 监听器注册. 取代原 web.xml 中的会话监听器声明.
 *
 * <p>原 web.xml 中挂载于 /druid/* 的 Druid 监控 Servlet 已移除: 该 Servlet 仍基于
 * {@code javax.servlet} 命名空间, 与 Jakarta Servlet 容器不兼容, 且会将连接池监控页暴露在
 * 公网上.
 *
 * @author Haozhe Xie
 */
@Configuration
public class ServletConfig {
  /** 统计在线用户数量的会话监听器. */
  @Bean
  public ServletListenerRegistrationBean<SessionListener> sessionListener() {
    return new ServletListenerRegistrationBean<>(new SessionListener());
  }
}
