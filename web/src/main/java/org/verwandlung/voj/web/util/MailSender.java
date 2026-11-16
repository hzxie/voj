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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import org.verwandlung.voj.web.service.OptionService;

/**
 * The email sending service. It sends nothing when no SMTP server is configured (see MailConfig).
 *
 * @author Haozhe Xie
 */
@Component("vojMailSender")
public class MailSender {
  /**
   * The constructor of MailSender.
   *
   * @param templateEngine - the shared Thymeleaf template engine
   * @param mailSender - the JavaMailSender object, absent when no SMTP server is configured
   */
  @Autowired
  private MailSender(ITemplateEngine templateEngine, Optional<JavaMailSender> mailSender) {
    this.templateEngine = templateEngine;
    this.mailSender = mailSender.orElse(null);
  }

  /**
   * Tells whether emails can be sent: whether an SMTP server is configured ({@code mail.host}).
   *
   * @return whether emails can be sent
   */
  public boolean isEnabled() {
    return mailSender != null;
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
   * Sends an email once the current transaction commits (at once when there is none), so nothing is
   * sent for a change that is rolled back and the link in the email is valid when it arrives. The
   * email is delivered by a background thread; a failure is logged and reported to {@code
   * onFailure}.
   *
   * @param recipient - the email address of the recipient
   * @param subject - the subject of the email
   * @param body - the content of the email
   * @param onFailure - run (on the delivery thread) when the email cannot be sent
   * @throws IllegalStateException if emails cannot be sent (see {@link #isEnabled})
   */
  public void sendAfterCommit(String recipient, String subject, String body, Runnable onFailure) {
    if (!isEnabled()) {
      throw new IllegalStateException("No SMTP server is configured (mail.host)");
    }
    Runnable delivery =
        () -> deliveryExecutor.execute(() -> deliver(recipient, subject, body, onFailure));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              delivery.run();
            }
          });
    } else {
      delivery.run();
    }
  }

  /**
   * Sends an email.
   *
   * @param recipient - the email address of the recipient
   * @param subject - the subject of the email
   * @param body - the content of the email
   * @param onFailure - run when the email cannot be sent
   */
  private void deliver(String recipient, String subject, String body, Runnable onFailure) {
    try {
      mailSender.send(
          mimeMessage -> {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
            message.setTo(recipient);
            message.setFrom(senderMail, senderName);
            String contactEmail = getContactEmail();
            if (!contactEmail.isEmpty()) {
              message.setReplyTo(contactEmail);
            }
            message.setSubject(subject);
            message.setText(body, true);
          });
      LOGGER.info(
          String.format(
              "An Email{Recipient: %s, Subject: %s} has been sent.",
              LogSanitizer.forLog(recipient), LogSanitizer.forLog(subject)));
    } catch (RuntimeException ex) {
      LOGGER.error(
          String.format(
              "Failed to send an Email{Recipient: %s, Subject: %s}.",
              LogSanitizer.forLog(recipient), LogSanitizer.forLog(subject)),
          ex);
      try {
        onFailure.run();
      } catch (RuntimeException failureEx) {
        LOGGER.error("Failed to handle the failed email.", failureEx);
      }
    }
  }

  /**
   * Gets the configured contact email address, used as the Reply-To of system mail.
   *
   * @return the contact email address, or an empty string when none is configured
   */
  private String getContactEmail() {
    return optionService.getStringOption("contactEmail", "").trim();
  }

  /** The shared Thymeleaf engine, used to render the HTML mail bodies. */
  private final ITemplateEngine templateEngine;

  /** The autowired OptionService object. Used to resolve the contact email Reply-To address. */
  @Autowired private OptionService optionService;

  /** Delivers the emails one after another, off the request threads. */
  private final ExecutorService deliveryExecutor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "voj-mail");
            thread.setDaemon(true);
            return thread;
          });

  /** The autowired JavaMailSender object, used to send emails; null when none can be sent. */
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
