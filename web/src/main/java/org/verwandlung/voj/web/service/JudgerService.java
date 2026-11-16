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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.verwandlung.voj.web.mapper.JudgerMapper;

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
   * Gets the running judgers, with their live telemetry: for each, its {@code username}, {@code
   * description}, {@code languageIds} (comma-separated), {@code isReady} (whether it takes
   * submissions), {@code cpuLoad} and {@code memoryUsage} (percentages of its host, -1 if unknown)
   * and {@code uptime} (ms).
   *
   * @return the running judgers, by username
   */
  public List<Map<String, Object>> getOnlineJudgers() {
    return judgerMapper.getOnlineJudgers(JUDGER_TIMEOUT_SECONDS);
  }

  /**
   * Gets the number of online judgers: the judger accounts that at least one running judger runs
   * as. Every page that counts the online judgers counts them so (an account several judgers run
   * as is one judger, as the judgers page shows it).
   *
   * @return the number of online judgers
   */
  public long getNumberOfOnlineJudgers() {
    Set<Object> usernames = new HashSet<>();
    for (Map<String, Object> judger : getOnlineJudgers()) {
      usernames.add(judger.get("username"));
    }
    return usernames.size();
  }

  /**
   * Gets the average CPU load of the hosts of the running judgers that report it.
   *
   * @return the average CPU load (0..100), or -1 when no running judger reports it
   */
  public double getAverageCpuLoad() {
    double total = 0;
    int counted = 0;
    for (Map<String, Object> judger : getOnlineJudgers()) {
      Object cpuLoad = judger.get("cpuLoad");
      if (cpuLoad instanceof Number && ((Number) cpuLoad).intValue() >= 0) {
        total += ((Number) cpuLoad).doubleValue();
        ++counted;
      }
    }
    return counted == 0 ? -1 : total / counted;
  }

  /**
   * Gets the languages that a running judger taking submissions supports.
   *
   * @return the unique identifiers of the languages
   */
  public Set<Integer> getLanguageIdsOfReadyJudgers() {
    Set<Integer> languageIds = new HashSet<>();
    for (String languageIdsOfJudger :
        judgerMapper.getLanguageIdsOfReadyJudgers(JUDGER_TIMEOUT_SECONDS)) {
      for (String languageId : languageIdsOfJudger.split(",")) {
        if (!languageId.isBlank()) {
          languageIds.add(Integer.valueOf(languageId.trim()));
        }
      }
    }
    return languageIds;
  }

  /** The autowired JudgerMapper object. */
  @Autowired private JudgerMapper judgerMapper;
}
