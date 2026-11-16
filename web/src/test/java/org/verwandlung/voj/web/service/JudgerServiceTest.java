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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.JudgerSummary;

/**
 * Test class for JudgerService, on the registry of the judgers in the database.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class JudgerServiceTest {
  /**
   * Registers, by the database's clock: voj@judger (#1001), which takes submissions in C++ and
   * Java and reported a heartbeat 5 s ago; another-user (#1002, standing in for a second judger),
   * which runs but takes no submissions (in Python); zjhzxhz (#1000), which stopped a minute ago.
   */
  @BeforeEach
  public void setUp() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM voj_judgers");
    jdbcTemplate.update(
        "INSERT INTO voj_judgers (judger_instance, judger_uid, judger_description,"
            + " judger_language_ids, judger_is_ready, judger_start_time, judger_heartbeat_time,"
            + " judger_cpu_load, judger_memory_usage) VALUES"
            + " ('a', 1001, 'Judger #1', '2,3', 1, NOW() - INTERVAL 2 HOUR,"
            + " NOW() - INTERVAL 5 SECOND, 40, 50),"
            + " ('b', 1002, 'Judger #2', '5', 0, NOW(), NOW(), -1, -1),"
            + " ('c', 1000, 'Judger #3', '1', 1, NOW() - INTERVAL 1 DAY,"
            + " NOW() - INTERVAL 1 MINUTE, 90, 90)");
    // A second process of voj@judger, which is still one judger online.
    jdbcTemplate.update(
        "INSERT INTO voj_judgers (judger_instance, judger_uid, judger_description,"
            + " judger_language_ids, judger_is_ready, judger_start_time, judger_heartbeat_time,"
            + " judger_cpu_load, judger_memory_usage) VALUES"
            + " ('d', 1001, 'Judger #1', '2,3', 0, NOW() - INTERVAL 2 HOUR, NOW(), -1, -1)");
  }

  /**
   * Test case: tests getJudgerSummary(). Expected: the three running judger processes, by username,
   * with their telemetry and uptime, which are two judgers online (two of the processes run as one
   * account); the average load is the one of the process that reports it.
   */
  @Test
  public void testJudgerSummary() {
    JudgerSummary summary = judgerService.getJudgerSummary();
    List<Map<String, Object>> nodes = summary.getNodes();

    Assertions.assertEquals(3, nodes.size());
    Assertions.assertEquals(2, summary.getOnlineJudgers());
    Assertions.assertEquals("another-user", nodes.get(0).get("username"));
    Map<String, Object> node =
        nodes.stream()
            .filter(process -> ((Number) process.get("cpuLoad")).intValue() >= 0)
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals("voj@judger", node.get("username"));
    Assertions.assertEquals("Judger #1", node.get("description"));
    Assertions.assertEquals(40, node.get("cpuLoad"));
    Assertions.assertEquals(50, node.get("memoryUsage"));
    Assertions.assertEquals("2h 0m", node.get("uptime"));
    Assertions.assertEquals(true, node.get("isReady"));
    Assertions.assertEquals(List.of(2, 3), node.get("languageIds"));
    Assertions.assertEquals(false, nodes.get(0).get("isReady"));
    Assertions.assertEquals(List.of(5), nodes.get(0).get("languageIds"));
    Assertions.assertEquals(40, summary.getAverageCpuLoad());
    Assertions.assertEquals(50, summary.getAverageMemoryUsage());
  }

  /** Test case: tests formatUptime(long). Test data: minutes, hours and days. Expected: Nm, Nh Nm and Nd Nh. */
  @Test
  public void testFormatUptime() {
    Assertions.assertEquals("0m", JudgerService.formatUptime(59_000));
    Assertions.assertEquals("1h 5m", JudgerService.formatUptime(65 * 60_000L));
    Assertions.assertEquals("40d 3h", JudgerService.formatUptime((40 * 24 + 3) * 3_600_000L));
  }

  /** Test case: tests toLanguageIds(String). Test data: identifiers, none and null. Expected: the identifiers, in order. */
  @Test
  public void testToLanguageIds() {
    Assertions.assertEquals(List.of(2, 3, 10), JudgerService.toLanguageIds("2,3, 10"));
    Assertions.assertEquals(List.of(), JudgerService.toLanguageIds(""));
    Assertions.assertEquals(List.of(), JudgerService.toLanguageIds(null));
  }

  /** The JudgerService object under test. */
  @Autowired private JudgerService judgerService;

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;
}
