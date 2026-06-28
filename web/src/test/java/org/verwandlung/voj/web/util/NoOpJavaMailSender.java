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

import java.io.InputStream;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * A no-op {@link JavaMailSender} for the test Spring context: every {@code send} call is discarded,
 * so service tests that trigger mail delivery never reach a real SMTP server (which would spawn
 * background threads failing with AuthenticationFailedException and clutter the test log).
 *
 * <p>This is a hand-written stub rather than a Mockito mock on purpose - the bean is instantiated
 * while the Spring context is bootstrapping, and creating a mock there forces Mockito's inline
 * mock-maker to self-attach a Java agent at an awkward point in start-up, which is fragile across
 * JVMs/CI environments. A plain class needs no agent and behaves identically everywhere.
 *
 * @author Haozhe Xie
 */
public final class NoOpJavaMailSender implements JavaMailSender {
  /** A shared, default mail session used only to construct empty MimeMessage instances. */
  private final Session session = Session.getInstance(new Properties());

  @Override
  public MimeMessage createMimeMessage() {
    return new MimeMessage(session);
  }

  @Override
  public MimeMessage createMimeMessage(InputStream contentStream) {
    try {
      return new MimeMessage(session, contentStream);
    } catch (MessagingException e) {
      throw new MailParseException("Could not parse raw MIME content", e);
    }
  }

  @Override
  public void send(MimeMessage mimeMessage) {
    // no-op
  }

  @Override
  public void send(MimeMessage... mimeMessages) {
    // no-op
  }

  @Override
  public void send(MimeMessagePreparator mimeMessagePreparator) {
    // no-op
  }

  @Override
  public void send(MimeMessagePreparator... mimeMessagePreparators) {
    // no-op
  }

  @Override
  public void send(SimpleMailMessage simpleMessage) {
    // no-op
  }

  @Override
  public void send(SimpleMailMessage... simpleMessages) {
    // no-op
  }
}
