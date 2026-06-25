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

import java.io.IOException;
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
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

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
   * @param freeMarkerConfigurer - the FreeMarker configurer
   * @param mailSender - the JavaMailSender object
   */
  @Autowired
  private MailSender(FreeMarkerConfigurer freeMarkerConfigurer, JavaMailSender mailSender) {
    this.freeMarkerConfigurer = freeMarkerConfigurer;
    this.mailSender = mailSender;
  }

  /**
   * Parses the content of an email template.
   *
   * @param templateLocation - the relative path of the email template
   * @param model - the additional information of the email
   * @return the parsed email content
   * @throws TemplateException
   * @throws IOException
   * @throws ParseException
   * @throws MalformedTemplateNameException
   * @throws TemplateNotFoundException
   */
  public String getMailContent(String templateLocation, Map<String, Object> model)
      throws TemplateNotFoundException,
          MalformedTemplateNameException,
          ParseException,
          IOException,
          TemplateException {
    model.put("baseUrl", baseUrl);

    return FreeMarkerTemplateUtils.processTemplateIntoString(
        freeMarkerConfigurer.getConfiguration().getTemplate(templateLocation), model);
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

  /** The autowired Configuration object. */
  private final FreeMarkerConfigurer freeMarkerConfigurer;

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
