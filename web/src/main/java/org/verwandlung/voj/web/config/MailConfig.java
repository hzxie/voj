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

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * 邮件服务配置. 取代原 dispatcher-servlet.xml 中的 JavaMailSender 与 FreeMarker 定义.
 *
 * <p>邮件正文使用位于 classpath:/mails 下的 FreeMarker 模板渲染. Spring Boot 自带的
 * FreeMarker MVC 视图解析器通过 {@code spring.freemarker.enabled=false} 关闭, 以免与
 * JSP 视图解析器冲突.
 *
 * @author Haozhe Xie
 */
@Configuration
public class MailConfig {
  /** 用于渲染邮件模板的 FreeMarker 配置. */
  @Bean
  public FreeMarkerConfigurer freeMarkerConfigurer() {
    FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
    freeMarkerConfigurer.setTemplateLoaderPath("classpath:/mails");
    return freeMarkerConfigurer;
  }

  /** 基于 SMTP 的邮件发送器. */
  @Bean
  public JavaMailSender javaMailSender(
      @Value("${mail.host}") String host,
      @Value("${mail.username}") String username,
      @Value("${mail.password}") String password) {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties javaMailProperties = new Properties();
    javaMailProperties.put("mail.smtp.auth", "true");
    javaMailProperties.put("mail.smtp.starttls.enable", "true");
    mailSender.setJavaMailProperties(javaMailProperties);
    return mailSender;
  }
}
