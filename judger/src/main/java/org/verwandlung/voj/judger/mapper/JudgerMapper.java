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
package org.verwandlung.voj.judger.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

/**
 * Judger Data Access Object: the registry of the running judgers (voj_judgers), which the web
 * application reads to show the judgers and to tell the languages that a judger taking
 * submissions supports. Every judger process has an entry of its own, by a random identifier.
 *
 * @author Haozhe Xie
 */
public interface JudgerMapper {
  /**
   * Records a heartbeat of a judger process: the languages it supports, whether it takes
   * submissions, the load of its host, and the time of the heartbeat, by the database's clock.
   * Registers the process on its first heartbeat, with its start time.
   *
   * @param instance - the identifier of the judger process, random
   * @param judgerUid - the uid of the judger's account
   * @param description - the description of the judger
   * @param languageIds - the unique identifiers of the languages it supports, comma-separated
   * @param isReady - whether it takes submissions
   * @param cpuLoad - the CPU load of its host, in percent, or -1 if unknown
   * @param memoryUsage - the memory usage of its host, in percent, or -1 if unknown
   */
  @Insert(
      "INSERT INTO voj_judgers (judger_instance, judger_uid, judger_description,"
          + " judger_language_ids, judger_is_ready, judger_start_time, judger_heartbeat_time,"
          + " judger_cpu_load, judger_memory_usage) VALUES (#{instance}, #{judgerUid},"
          + " #{description}, #{languageIds}, #{isReady}, NOW(), NOW(), #{cpuLoad},"
          + " #{memoryUsage}) ON DUPLICATE KEY UPDATE judger_description = #{description},"
          + " judger_language_ids = #{languageIds}, judger_is_ready = #{isReady},"
          + " judger_heartbeat_time = NOW(), judger_cpu_load = #{cpuLoad},"
          + " judger_memory_usage = #{memoryUsage}")
  @Options(timeout = SubmissionMapper.STATEMENT_TIMEOUT_SECONDS)
  void recordHeartbeat(
      @Param("instance") String instance,
      @Param("judgerUid") long judgerUid,
      @Param("description") String description,
      @Param("languageIds") String languageIds,
      @Param("isReady") boolean isReady,
      @Param("cpuLoad") int cpuLoad,
      @Param("memoryUsage") int memoryUsage);

  /**
   * Removes the entries of the judger processes whose last heartbeat is more than a day old (by the
   * database's clock): they stopped long ago, and every start of a judger adds one.
   *
   * @return the number of entries removed
   */
  @Delete("DELETE FROM voj_judgers WHERE judger_heartbeat_time < NOW() - INTERVAL 1 DAY")
  @Options(timeout = SubmissionMapper.STATEMENT_TIMEOUT_SECONDS)
  int deleteStoppedJudgers();
}
