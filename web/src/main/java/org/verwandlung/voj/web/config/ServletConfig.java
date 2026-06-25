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
 * The Servlet listener registration. Replaces the session listener declaration in the original
 * web.xml.
 *
 * <p>The Druid monitoring Servlet mounted at /druid/* in the original web.xml has been removed: that
 * Servlet is still based on the {@code javax.servlet} namespace, which is incompatible with the
 * Jakarta Servlet container, and it would expose the connection pool monitoring page to the public
 * internet.
 *
 * @author Haozhe Xie
 */
@Configuration
public class ServletConfig {
  /** The session listener that counts the number of online users. */
  @Bean
  public ServletListenerRegistrationBean<SessionListener> sessionListener() {
    return new ServletListenerRegistrationBean<>(new SessionListener());
  }
}
