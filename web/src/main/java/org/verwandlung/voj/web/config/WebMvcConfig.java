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

import java.util.Locale;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * The Spring MVC configuration. Replaces the internationalization and localization configuration
 * from the original dispatcher-servlet.xml.
 *
 * <p>Static assets live under {@code classpath:/static/assets} and are served at {@code /assets/**}
 * by Spring Boot's default static-resource handling, so no explicit resource handler is needed. The
 * views are rendered by Thymeleaf (auto-configured by Spring Boot) from {@code
 * classpath:/templates}.
 *
 * @author Haozhe Xie
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  /* (non-Javadoc)
   * Registers the language switching interceptor (switches the display language via ?language=xx_XX).
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }

  /**
   * Enables the Thymeleaf Layout Dialect, which provides the {@code
   * layout:decorate} / {@code layout:fragment} decorator model used by the page
   * templates to share a single master layout.
   */
  @Bean
  public LayoutDialect layoutDialect() {
    return new LayoutDialect();
  }

  /** The internationalization resources. */
  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:localization/voj");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }

  /** The Session-based Locale resolver, with the default language being en_US. */
  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver localeResolver = new SessionLocaleResolver();
    localeResolver.setDefaultLocale(Locale.US);
    return localeResolver;
  }

  /** The language switching interceptor. */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    localeChangeInterceptor.setParamName("language");
    return localeChangeInterceptor;
  }
}
