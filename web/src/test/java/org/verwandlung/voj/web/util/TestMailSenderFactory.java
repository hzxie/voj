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

import static org.mockito.Mockito.mock;

import org.springframework.mail.javamail.JavaMailSender;

/**
 * Supplies the JavaMailSender used by the test Spring context. The tests never reach a real SMTP
 * server, so this hands back a Mockito mock whose send() is a no-op. This keeps service tests (e.g.
 * UserServiceTest) that trigger mail delivery from spawning background threads that fail with
 * AuthenticationFailedException and clutter the test log.
 *
 * @author Haozhe Xie
 */
public final class TestMailSenderFactory {
  private TestMailSenderFactory() {}

  /** Builds a no-op JavaMailSender for the test context. */
  public static JavaMailSender createMailSender() {
    return mock(JavaMailSender.class);
  }
}
