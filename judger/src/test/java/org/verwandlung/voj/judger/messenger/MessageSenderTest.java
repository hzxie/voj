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
package org.verwandlung.voj.judger.messenger;

import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

/**
 * Test class for MessageSender.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
public class MessageSenderTest {
  /**
   * Test case: tests the sendMessage method. Expected result: the message is delegated to
   * JmsTemplate.convertAndSend.
   */
  @Test
  public void testSendMessage() {
    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "KeepAlive");

    messageSender.sendMessage(mapMessage);

    verify(jmsTemplate).convertAndSend(mapMessage);
  }

  /** The mocked JmsTemplate object. */
  @Mock private JmsTemplate jmsTemplate;

  /** The MessageSender object under test. */
  @InjectMocks private MessageSender messageSender;
}
