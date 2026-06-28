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
package org.verwandlung.voj.web.messenger;

import java.util.Date;

import org.springframework.context.ApplicationEvent;

/**
 * The judger heartbeat event message.
 *
 * @author Haozhe Xie
 */
public class KeepAliveEvent extends ApplicationEvent {
  /**
   * The constructor of KeepAliveEvent.
   *
   * @param source - the message publishing source
   * @param judgerUsername - the username of the judger
   * @param judgerDescription - the description information of the judger
   */
  public KeepAliveEvent(
      Object source,
      String judgerUsername,
      String judgerDescription,
      Date heartbeatTime,
      int cpuLoad,
      int memoryUsage,
      long uptime) {
    super(source);
    this.judgerUsername = judgerUsername;
    this.judgerDescription = judgerDescription;
    this.heartbeatTime = heartbeatTime;
    this.cpuLoad = cpuLoad;
    this.memoryUsage = memoryUsage;
    this.uptime = uptime;
  }

  /**
   * Gets the username of the judger.
   *
   * @return the username of the judger
   */
  public String getJudgerUsername() {
    return judgerUsername;
  }

  /**
   * Gets the description information of the judger.
   *
   * @return the description information of the judger
   */
  public String getJudgerDescription() {
    return judgerDescription;
  }

  /**
   * Gets the time when the judger sent the heartbeat.
   *
   * @return the time when the judger sent the heartbeat
   */
  public Date getHeartbeatTime() {
    return heartbeatTime;
  }

  /**
   * Gets the judger's CPU load percentage (0-100).
   *
   * @return the judger's CPU load percentage
   */
  public int getCpuLoad() {
    return cpuLoad;
  }

  /**
   * Gets the judger's memory usage percentage (0-100).
   *
   * @return the judger's memory usage percentage
   */
  public int getMemoryUsage() {
    return memoryUsage;
  }

  /**
   * Gets the judger's uptime in milliseconds.
   *
   * @return the judger's uptime in milliseconds
   */
  public long getUptime() {
    return uptime;
  }

  /** The username of the judger. */
  private final String judgerUsername;

  /** The description information of the judger. */
  private final String judgerDescription;

  /** The time of the judger's heartbeat. */
  private final Date heartbeatTime;

  /** The judger's CPU load percentage (0-100). */
  private final int cpuLoad;

  /** The judger's memory usage percentage (0-100). */
  private final int memoryUsage;

  /** The judger's uptime in milliseconds. */
  private final long uptime;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -9218788192064705664L;
}
