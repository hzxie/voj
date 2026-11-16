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
package org.verwandlung.voj.web.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

/**
 * Judger Data Access Object: reads the registry of the running judgers (voj_judgers), which every
 * judger updates with its heartbeats.
 *
 * @author Haozhe Xie
 */
public interface JudgerMapper {
  /**
   * Gets the judgers whose last heartbeat is at most some seconds old, by the database's clock: for
   * each, its {@code username}, {@code description}, {@code languageIds} (comma-separated), {@code
   * isReady} (whether it takes submissions), {@code cpuLoad} and {@code memoryUsage} (percentages
   * of its host, -1 if unknown) and {@code uptime} (ms).
   *
   * @param timeoutSeconds - how old (seconds) the last heartbeat of a running judger may be
   * @return the judgers, by username
   */
  List<Map<String, Object>> getOnlineJudgers(@Param("timeoutSeconds") int timeoutSeconds);
}
