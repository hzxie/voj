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
package org.verwandlung.voj.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;

/**
 * Verwandlung Online Judge 的 Web 应用程序入口.
 *
 * <p>该应用打包为可执行 WAR: 既能通过 {@code java -jar voj.web.war} 独立运行(内嵌 Tomcat),
 * 也能部署到外部的 Servlet 容器中.
 *
 * @author Haozhe Xie
 */
@SpringBootApplication
@MapperScan("org.verwandlung.voj.web.mapper")
@PropertySource("classpath:voj.properties")
public class VojWebApplication extends SpringBootServletInitializer {
  /** 应用程序入口. */
  public static void main(String[] args) {
    SpringApplication.run(VojWebApplication.class, args);
  }

  /* (non-Javadoc)
   * 当部署到外部 Servlet 容器时, 由容器调用此方法装配应用.
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(VojWebApplication.class);
  }
}
