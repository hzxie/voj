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
 * The entry point of the web application of Verwandlung Online Judge.
 *
 * <p>The application is packaged as an executable WAR: it can run standalone via {@code java -jar
 * voj.web.war} (with embedded Tomcat), and can also be deployed to an external Servlet container.
 *
 * @author Haozhe Xie
 */
@SpringBootApplication
@MapperScan("org.verwandlung.voj.web.mapper")
@PropertySource("classpath:voj.properties")
public class VojWebApplication extends SpringBootServletInitializer {
  /** The entry point of the application. */
  public static void main(String[] args) {
    SpringApplication.run(VojWebApplication.class, args);
  }

  /* (non-Javadoc)
   * When deployed to an external Servlet container, this method is invoked by the container to
   * assemble the application.
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(VojWebApplication.class);
  }
}
