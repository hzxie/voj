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

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for JudgerMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class JudgerMapperTest {
  /**
   * Test case: tests the recordHeartbeat method. Test data: a process of the judger account #1001,
   * not registered; its first heartbeat, then a later one (after its start and heartbeat times were
   * set back an hour). Expected result: the first heartbeat registers it with its account,
   * description, languages, readiness and load, started and seen now by the database's clock; a
   * later one updates all of that but the start time.
   */
  @Test
  public void testRecordHeartbeat() {
    judgerMapper.recordHeartbeat("process-1", 1001, "Judger #1", "2,3", true, 12, 34);
    Map<String, Object> judger = getJudger();
    Assertions.assertEquals(1001L, ((Number) judger.get("judger_uid")).longValue());
    Assertions.assertEquals("Judger #1", judger.get("judger_description"));
    Assertions.assertEquals("2,3", judger.get("judger_language_ids"));
    Assertions.assertEquals(true, judger.get("judger_is_ready"));
    Assertions.assertEquals(12, ((Number) judger.get("judger_cpu_load")).intValue());
    Assertions.assertEquals(34, ((Number) judger.get("judger_memory_usage")).intValue());
    Assertions.assertTrue(getSecondsAgo(judger, "started") <= 5, judger.toString());
    Assertions.assertTrue(getSecondsAgo(judger, "seen") <= 5, judger.toString());

    jdbcTemplate()
        .update(
            "UPDATE voj_judgers SET judger_start_time = NOW() - INTERVAL 1 HOUR,"
                + " judger_heartbeat_time = NOW() - INTERVAL 1 HOUR"
                + " WHERE judger_instance = 'process-1'");
    judgerMapper.recordHeartbeat("process-1", 1001, "Judger #2", "3", false, -1, -1);
    judger = getJudger();
    Assertions.assertEquals("Judger #2", judger.get("judger_description"));
    Assertions.assertEquals("3", judger.get("judger_language_ids"));
    Assertions.assertEquals(false, judger.get("judger_is_ready"));
    Assertions.assertEquals(-1, ((Number) judger.get("judger_cpu_load")).intValue());
    Assertions.assertTrue(getSecondsAgo(judger, "started") >= 3595, judger.toString());
    Assertions.assertTrue(getSecondsAgo(judger, "seen") <= 5, judger.toString());
  }

  /**
   * Test case: tests the recordHeartbeat and deleteStoppedJudgers methods. Test data: two processes
   * of the judger account #1001, one of which last reported 25 hours ago. Expected result: each has
   * an entry of its own; only the entry of the one that stopped a day ago is removed.
   */
  @Test
  public void testDeleteStoppedJudgers() {
    judgerMapper.recordHeartbeat("process-1", 1001, "", "2", true, -1, -1);
    judgerMapper.recordHeartbeat("process-2", 1001, "", "2", true, -1, -1);
    jdbcTemplate()
        .update(
            "UPDATE voj_judgers SET judger_heartbeat_time = NOW() - INTERVAL 25 HOUR"
                + " WHERE judger_instance = 'process-2'");

    Assertions.assertEquals(1, judgerMapper.deleteStoppedJudgers());
    Assertions.assertEquals(
        java.util.List.of("process-1"),
        jdbcTemplate()
            .queryForList(
                "SELECT judger_instance FROM voj_judgers WHERE judger_uid = 1001", String.class));
  }

  /** Gets the registry entry of the process process-1, with its ages by the database's clock. */
  private Map<String, Object> getJudger() {
    return jdbcTemplate()
        .queryForMap(
            "SELECT *, TIMESTAMPDIFF(SECOND, judger_start_time, NOW()) AS started,"
                + " TIMESTAMPDIFF(SECOND, judger_heartbeat_time, NOW()) AS seen"
                + " FROM voj_judgers WHERE judger_instance = 'process-1'");
  }

  /** Gets an age (in seconds) of a registry entry. */
  private static long getSecondsAgo(Map<String, Object> judger, String age) {
    return ((Number) judger.get(age)).longValue();
  }

  /** Gets a JdbcTemplate on the data source, which runs in the transaction of the test. */
  private JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource);
  }

  /** The JudgerMapper object under test. */
  @Autowired private JudgerMapper judgerMapper;

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;
}
