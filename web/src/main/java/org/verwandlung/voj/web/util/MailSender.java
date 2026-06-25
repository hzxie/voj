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
package org.verwandlung.voj.web.util;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * The email sending service.
 *
 * @author Haozhe Xie
 */
@Component("vojMailSender")
public class MailSender {
  /**
   * The constructor of MailSender.
   *
   * @param templateEngine - the shared Thymeleaf template engine
   * @param mailSender - the JavaMailSender object
   */
  @Autowired
  private MailSender(ITemplateEngine templateEngine, JavaMailSender mailSender) {
    this.templateEngine = templateEngine;
    this.mailSender = mailSender;
  }

  /**
   * Renders the content of an email template.
   *
   * @param templateName - the name of the email template, resolved against
   *     classpath:/templates/ (e.g. {@code mail/reset-password})
   * @param model - the additional information of the email
   * @return the rendered email content
   */
  public String getMailContent(String templateName, Map<String, Object> model) {
    model.put("baseUrl", baseUrl);

    Context context = new Context();
    context.setVariables(model);
    return templateEngine.process(templateName, context);
  }

  /**
   * Sends an email to the specified recipient.
   *
   * @param recipient - the email address of the recipient
   * @param subject - the subject of the email
   * @param body - the content of the email
   */
  public void sendMail(final String recipient, final String subject, final String body) {
    final MimeMessagePreparator preparator =
        new MimeMessagePreparator() {
          public void prepare(MimeMessage mimeMessage)
              throws MessagingException, UnsupportedEncodingException {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
            message.setTo(recipient);
            message.setFrom(senderMail, senderName);
            message.setSubject(subject);
            message.setText(body, true);
          }
        };
    new Thread(
            new Runnable() {
              public void run() {
                mailSender.send(preparator);
                LOGGER.info(
                    String.format(
                        "An Email{Recipient: %s, Subject: %s} has been sent.",
                        new Object[] {recipient, subject}));
              }
            })
        .start();
  }

  /** The shared Thymeleaf engine, used to render the HTML mail bodies. */
  private final ITemplateEngine templateEngine;

  /** The autowired JavaMailSender object. Used to send emails. */
  private final JavaMailSender mailSender;

  /** The email address of the sender. */
  @Value("${mail.senderMail}")
  private String senderMail;

  /** The name of the email sender. */
  @Value("${mail.senderName}")
  private String senderName;

  /** The URL of the website. Used to generate the links in emails. */
  @Value("${url.base}")
  private String baseUrl;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MailSender.class);
}
