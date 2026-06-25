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
package org.verwandlung.voj.judger.application;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.util.DigestUtils;

/**
 * The application heartbeat, used to send Keep-Alive messages to the web module.
 *
 * @author Haozhe Xie
 */
@Component
public class ApplicationHeartbeat implements Runnable {
  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    if (!isIdentityValid()) {
      LOGGER.error("Unauthorized: Please check your username and password.");
      System.exit(-1);
    }
    Calendar calendar = Calendar.getInstance();
    long currentTime = calendar.getTimeInMillis();

    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "KeepAlive");
    mapMessage.put("username", judgerUsername);
    mapMessage.put("description", getDescription());
    mapMessage.put("heartbeatTime", currentTime);
    messageSender.sendMessage(mapMessage);
    LOGGER.info("Heartbeat sent to the web server.");
  }

  /**
   * Checks whether the judger's identity information is valid.
   *
   * @return whether the judger's identity information is valid
   */
  private boolean isIdentityValid() {
    User user = userMapper.getUserUsingUsername(judgerUsername);

    if (user != null
        && "judgers".equals(user.getUserGroup().getUserGroupSlug())
        && user.getPassword().equals(DigestUtils.md5Hex(judgerPassword))) {
      return true;
    }
    return false;
  }

  /**
   * Gets the judger's description. TODO use JNI to obtain the computer's hardware information.
   *
   * @return the judger's description
   */
  private String getDescription() {
    return judgerDescription;
  }

  /** The judger's identity information: the judger's username. */
  @Value("${judger.username}")
  private String judgerUsername;

  /** The judger's identity information: the judger's password. */
  @Value("${judger.password}")
  private String judgerPassword;

  /** The judger's identity information: the judger's description. */
  @Value("${judger.description}")
  private String judgerDescription;

  /** The autowired MessageSender object, used to send messages to the message queue. */
  @Autowired private MessageSender messageSender;

  /** The autowired UserMapper object, used to verify the judger's identity information. */
  @Autowired private UserMapper userMapper;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationHeartbeat.class);
}
