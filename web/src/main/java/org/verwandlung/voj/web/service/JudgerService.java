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
package org.verwandlung.voj.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.verwandlung.voj.web.mapper.JudgerMapper;
import org.verwandlung.voj.web.model.JudgerSummary;

/**
 * The business logic layer of the judgers: reads the registry of the running judgers, which every
 * judger updates with a heartbeat every 10 seconds (the languages it supports, whether it takes
 * submissions, the load of its host).
 *
 * @author Haozhe Xie
 */
@Service
public class JudgerService {
  /**
   * How old (seconds) the last heartbeat of a running judger may be, by the database's clock: three
   * heartbeats of a judger (every 10 seconds) may go missing before it counts as stopped.
   */
  public static final int JUDGER_TIMEOUT_SECONDS = 30;

  /**
   * Gets the running judgers (one query), as every page that shows them counts them.
   *
   * @return the running judgers
   */
  public JudgerSummary getJudgerSummary() {
    List<Map<String, Object>> nodes = new ArrayList<>();
    Set<Object> usernames = new HashSet<>();
    long cpuLoadTotal = 0;
    long memoryUsageTotal = 0;
    int nodesWithMetrics = 0;
    for (Map<String, Object> judger : judgerMapper.getOnlineJudgers(JUDGER_TIMEOUT_SECONDS)) {
      int cpuLoad = toInt(judger.get("cpuLoad"), -1);
      int memoryUsage = toInt(judger.get("memoryUsage"), -1);
      Map<String, Object> node = new HashMap<>(8, 1);
      node.put("username", judger.get("username"));
      node.put("description", judger.get("description"));
      node.put("cpuLoad", cpuLoad);
      node.put("memoryUsage", memoryUsage);
      Object uptime = judger.get("uptime");
      node.put("uptime", formatUptime(uptime instanceof Number ? ((Number) uptime).longValue() : 0));
      nodes.add(node);
      usernames.add(judger.get("username"));
      if (cpuLoad >= 0) {
        cpuLoadTotal += cpuLoad;
        memoryUsageTotal += Math.max(memoryUsage, 0);
        ++nodesWithMetrics;
      }
    }
    return new JudgerSummary(
        nodes,
        usernames.size(),
        nodesWithMetrics == 0 ? -1 : Math.round((float) cpuLoadTotal / nodesWithMetrics),
        nodesWithMetrics == 0 ? -1 : Math.round((float) memoryUsageTotal / nodesWithMetrics));
  }

  /**
   * Coerces a telemetry value to an int.
   *
   * @param value - the value (a Number, or null)
   * @param defaultValue - the value when it is not a Number
   * @return the int value
   */
  private static int toInt(Object value, int defaultValue) {
    return value instanceof Number ? ((Number) value).intValue() : defaultValue;
  }

  /**
   * Formats an uptime (in milliseconds) as {@code Nd Nh}, {@code Nh Nm} or {@code Nm}.
   *
   * @param uptimeMillis - the uptime in milliseconds
   * @return the formatted uptime
   */
  static String formatUptime(long uptimeMillis) {
    long totalMinutes = uptimeMillis / 60000L;
    long days = totalMinutes / 1440L;
    long hours = (totalMinutes % 1440L) / 60L;
    long minutes = totalMinutes % 60L;
    if (days > 0) {
      return days + "d " + hours + "h";
    }
    if (hours > 0) {
      return hours + "h " + minutes + "m";
    }
    return minutes + "m";
  }

  /** The autowired JudgerMapper object. */
  @Autowired private JudgerMapper judgerMapper;
}
