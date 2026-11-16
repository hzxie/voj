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
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * The mail service configuration: the SMTP server the emails are sent through, when {@code
 * mail.host} names one. Without it the application sends no email (see MailSender#isEnabled): an
 * empty host would not disable JavaMail, which connects to localhost instead.
 *
 * <p>The HTML mail body is rendered by the shared Thymeleaf engine (auto-configured by Spring Boot)
 * from templates under classpath:/templates/mail, so no dedicated templating configuration is
 * required here.
 *
 * @author Haozhe Xie
 */
@Configuration
public class MailConfig {
  /** The SMTP-based mail sender, when an SMTP server is configured. */
  @Bean
  @Conditional(MailHostCondition.class)
  public JavaMailSender javaMailSender(
      @Value("${mail.host}") String host,
      @Value("${mail.port}") int port,
      @Value("${mail.username}") String username,
      @Value("${mail.password}") String password) {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host.trim());
    mailSender.setPort(port);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties javaMailProperties = new Properties();
    javaMailProperties.put("mail.smtp.auth", "true");
    javaMailProperties.put("mail.smtp.starttls.enable", "true");
    mailSender.setJavaMailProperties(javaMailProperties);
    return mailSender;
  }

  /** Matches when {@code mail.host} names an SMTP server. */
  static class MailHostCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !context.getEnvironment().getProperty("mail.host", "").isBlank();
    }
  }
}
