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

import org.springframework.beans.factory.annotation.Autowired;

import org.verwandlung.voj.web.messenger.MessageReceiver;

/**
 * The test class for MessageReceiver. Note: for development and debugging use only.
 *
 * @author Haozhe Xie
 */
/*
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
*/
public class MessageReceiverTest {
  /* @Test */
  public void testOnMessage() throws InterruptedException {
    Thread.sleep(5000);
  }

  /** The autowired MessageReceiver object. */
  @Autowired private MessageReceiver messageReceiver;
}
