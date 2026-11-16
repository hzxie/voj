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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import org.verwandlung.voj.web.interceptor.AdministrationChromeInterceptor;
import org.verwandlung.voj.web.interceptor.CommonModelInterceptor;
import org.verwandlung.voj.web.interceptor.MaintenanceModeInterceptor;

/**
 * The Spring MVC configuration: the interceptors of every request (the beans the views are rendered
 * with are in {@link ViewConfig}).
 *
 * <p>Static assets live under {@code classpath:/static/assets} and are served at {@code /assets/**}
 * by Spring Boot's default static-resource handling, so no explicit resource handler is needed.
 *
 * @author Haozhe Xie
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  /** The interceptor that injects the shared page model attributes. */
  @Autowired private CommonModelInterceptor commonModelInterceptor;

  /** The interceptor that takes the site offline for non-administrators during maintenance. */
  @Autowired private MaintenanceModeInterceptor maintenanceModeInterceptor;

  /** The interceptor that adds the sidebar section and badges to the administration pages. */
  @Autowired private AdministrationChromeInterceptor administrationChromeInterceptor;

  /* (non-Javadoc)
   * Registers the language switching interceptor (switches the display language via ?language=xx_XX),
   * the maintenance-mode gate, the interceptor that injects the shared page model attributes, and
   * the one that adds the chrome of the administration pages.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
    registry.addInterceptor(maintenanceModeInterceptor);
    registry.addInterceptor(commonModelInterceptor);
    registry.addInterceptor(administrationChromeInterceptor);
  }

  /** The language switching interceptor. */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    localeChangeInterceptor.setParamName("language");
    return localeChangeInterceptor;
  }
}
