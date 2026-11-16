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
package org.verwandlung.voj.web.model;

import java.util.List;
import java.util.Map;

/**
 * The running judgers, as every page shows them: the judger processes (nodes) with their
 * telemetry, the number of judgers online (the judger accounts at least one node runs as, so several
 * nodes of one account count once) and the average load of the nodes that report it.
 *
 * @author Haozhe Xie
 */
public class JudgerSummary {
  /**
   * Creates a summary.
   *
   * @param nodes - the running judger processes
   * @param onlineJudgers - the number of judger accounts at least one node runs as
   * @param averageCpuLoad - the average CPU load (0..100) of the nodes' hosts, or -1 if unknown
   * @param averageMemoryUsage - the average memory usage (0..100), or -1 if unknown
   */
  public JudgerSummary(
      List<Map<String, Object>> nodes,
      int onlineJudgers,
      int averageCpuLoad,
      int averageMemoryUsage) {
    this.nodes = nodes;
    this.onlineJudgers = onlineJudgers;
    this.averageCpuLoad = averageCpuLoad;
    this.averageMemoryUsage = averageMemoryUsage;
  }

  /**
   * Gets the running judger processes, by username: each with its {@code username}, {@code
   * description}, {@code cpuLoad} and {@code memoryUsage} (percentages, -1 if unknown) and {@code
   * uptime} (formatted, e.g. {@code 3d 4h}).
   *
   * @return the running judger processes
   */
  public List<Map<String, Object>> getNodes() {
    return nodes;
  }

  /**
   * Gets the number of judgers online: the judger accounts at least one node runs as.
   *
   * @return the number of judgers online
   */
  public int getOnlineJudgers() {
    return onlineJudgers;
  }

  /**
   * Gets the average CPU load of the hosts of the nodes that report it.
   *
   * @return the average CPU load (0..100), or -1 when no node reports it
   */
  public int getAverageCpuLoad() {
    return averageCpuLoad;
  }

  /**
   * Gets the average memory usage of the hosts of the nodes that report it.
   *
   * @return the average memory usage (0..100), or -1 when no node reports it
   */
  public int getAverageMemoryUsage() {
    return averageMemoryUsage;
  }

  /** The running judger processes. */
  private final List<Map<String, Object>> nodes;

  /** The number of judger accounts at least one node runs as. */
  private final int onlineJudgers;

  /** The average CPU load of the nodes, or -1. */
  private final int averageCpuLoad;

  /** The average memory usage of the nodes, or -1. */
  private final int averageMemoryUsage;
}
