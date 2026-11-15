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
package org.verwandlung.voj.web.upgrade;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Runs the one-time upgrade script sql/upgrades/decode-legacy-html-entities.sql against rows
 * stored entity-encoded by the HtmlTextFilter of 1.0.2 and earlier.
 *
 * <p>The script manages its own transaction (it must, so that the marker and the updates commit
 * together), so this test is not {@code @Transactional}: it inserts its own rows and removes them,
 * and restores the seeded marker option, afterwards.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DecodeLegacyHtmlEntitiesTest {
  /** The upgrade script, copied onto the test classpath from sql/upgrades. */
  private static final String SCRIPT = "db/upgrades/decode-legacy-html-entities.sql";

  /** The option that marks the upgrade as done. */
  private static final String MARKER = "legacyHtmlEntitiesDecoded";

  /** The IDs of the rows inserted by this test (well above the fixtures in test-data.sql). */
  private static final long THREAD_ID = 9001;

  private static final long PLAIN_THREAD_ID = 9002;
  private static final long REPLY_ID = 9001;
  private static final long FIRST_META_ID = 9001;
  private static final long UID = 1002;

  /**
   * The user meta rows inserted by this test: { meta key, value as stored by the old filter, value
   * after decoding }. The last key was never filtered and must not be touched.
   */
  private static final String[][] USER_META = {
    {"displayName", "Tom &amp; Jerry", "Tom & Jerry"},
    {"location", "&lt;Earth&gt;", "<Earth>"},
    {"website", "https://example.org/?a=1&amp;b=2", "https://example.org/?a=1&b=2"},
    {"socialLinks", "{\"GitHub\":\"a&amp;b&lt;c&gt;\"}", "{\"GitHub\":\"a&b<c>\"}"},
    {"aboutMe", "x&nbsp;&gt;&nbsp;y 'single' \"double\"", "x > y 'single' \"double\""},
    {"registerTime", "&amp;", "&amp;"},
  };

  /**
   * A thread title as stored by the old filter. The user typed {@code vector<int> && a<NBSP>b
   * &lt;br&gt;}: the literal entity text must come back as typed, not as {@code <br>}.
   */
  private static final String LEGACY_TITLE = "vector&lt;int&gt; &amp;&amp; a&nbsp;b &amp;lt;br&amp;gt;";

  private static final String DECODED_TITLE = "vector<int> && a b &lt;br&gt;";

  private static final String LEGACY_REPLY = "if (a &lt; b &amp;&amp; c &gt; d)\n  \"ok\";";
  private static final String DECODED_REPLY = "if (a < b && c > d)\n  \"ok\";";

  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM voj_options WHERE option_name = ?", MARKER);
    jdbcTemplate.update(
        "INSERT INTO voj_discussion_threads (discussion_thread_id, discussion_thread_creator_uid,"
            + " discussion_topic_id, discussion_thread_name) VALUES (?, ?, 1, ?), (?, ?, 1, ?)",
        THREAD_ID,
        UID,
        LEGACY_TITLE,
        PLAIN_THREAD_ID,
        UID,
        "Plain title");
    jdbcTemplate.update(
        "INSERT INTO voj_discussion_replies (discussion_reply_id, discussion_thread_id,"
            + " discussion_reply_uid, discussion_reply_content) VALUES (?, ?, ?, ?)",
        REPLY_ID,
        THREAD_ID,
        UID,
        LEGACY_REPLY);
    for (int i = 0; i < USER_META.length; ++i) {
      jdbcTemplate.update(
          "INSERT INTO voj_usermeta (meta_id, uid, meta_key, meta_value) VALUES (?, ?, ?, ?)",
          FIRST_META_ID + i,
          UID,
          USER_META[i][0],
          USER_META[i][1]);
    }
  }

  @AfterEach
  public void tearDown() {
    jdbcTemplate.update(
        "DELETE FROM voj_usermeta WHERE meta_id BETWEEN ? AND ?",
        FIRST_META_ID,
        FIRST_META_ID + USER_META.length - 1);
    jdbcTemplate.update("DELETE FROM voj_discussion_replies WHERE discussion_reply_id = ?", REPLY_ID);
    jdbcTemplate.update(
        "DELETE FROM voj_discussion_threads WHERE discussion_thread_id IN (?, ?)",
        THREAD_ID,
        PLAIN_THREAD_ID);
    // Put the marker back exactly as seed.sql has it.
    jdbcTemplate.update("DELETE FROM voj_options WHERE option_name = ?", MARKER);
    jdbcTemplate.update(
        "INSERT INTO voj_options (option_id, option_name, option_value, is_autoload)"
            + " VALUES (30, ?, '1', 0)",
        MARKER);
  }

  /**
   * Test case: runs the upgrade on an installation upgraded from 1.0.2, then runs it again after a
   * user has typed text containing entities. Test data: rows stored entity-encoded by the old
   * filter. Expected: the first run decodes exactly the old encoding (with &amp;amp; last) in the
   * filtered columns only and records the marker; the second run changes nothing.
   */
  @Test
  public void testDecodesLegacyRowsOnceOnly() throws SQLException {
    runUpgrade();

    Assertions.assertEquals(DECODED_TITLE, getThreadName(THREAD_ID));
    Assertions.assertEquals("Plain title", getThreadName(PLAIN_THREAD_ID));
    Assertions.assertEquals(DECODED_REPLY, getReplyContent());
    for (int i = 0; i < USER_META.length; ++i) {
      Assertions.assertEquals(USER_META[i][2], getUserMeta(i), USER_META[i][0]);
    }
    Assertions.assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM voj_options WHERE option_name = ?", Integer.class, MARKER));

    // After the upgrade text is stored verbatim; a user may legitimately type "&amp;".
    jdbcTemplate.update(
        "UPDATE voj_discussion_threads SET discussion_thread_name = ?"
            + " WHERE discussion_thread_id = ?",
        "Tom &amp; Jerry &lt;3",
        PLAIN_THREAD_ID);
    runUpgrade();

    Assertions.assertEquals("Tom &amp; Jerry &lt;3", getThreadName(PLAIN_THREAD_ID));
    Assertions.assertEquals(DECODED_TITLE, getThreadName(THREAD_ID));
    Assertions.assertEquals(DECODED_REPLY, getReplyContent());
    for (int i = 0; i < USER_META.length; ++i) {
      Assertions.assertEquals(USER_META[i][2], getUserMeta(i), USER_META[i][0]);
    }
  }

  /**
   * Test case: runs the upgrade on a new installation, which gets the marker from seed.sql. Test
   * data: rows containing entity text and the marker option. Expected: nothing is decoded.
   */
  @Test
  public void testDoesNothingWhenAlreadyMarked() throws SQLException {
    jdbcTemplate.update(
        "INSERT INTO voj_options (option_name, option_value, is_autoload) VALUES (?, '1', 0)",
        MARKER);

    runUpgrade();

    Assertions.assertEquals(LEGACY_TITLE, getThreadName(THREAD_ID));
    Assertions.assertEquals(LEGACY_REPLY, getReplyContent());
    for (int i = 0; i < USER_META.length; ++i) {
      Assertions.assertEquals(USER_META[i][1], getUserMeta(i), USER_META[i][0]);
    }
  }

  /** Runs the upgrade script as an operator would: on one connection, statement by statement. */
  private void runUpgrade() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(
          connection, new EncodedResource(new ClassPathResource(SCRIPT), StandardCharsets.UTF_8));
    }
  }

  private String getThreadName(long threadId) {
    return jdbcTemplate.queryForObject(
        "SELECT discussion_thread_name FROM voj_discussion_threads WHERE discussion_thread_id = ?",
        String.class,
        threadId);
  }

  private String getReplyContent() {
    return jdbcTemplate.queryForObject(
        "SELECT discussion_reply_content FROM voj_discussion_replies"
            + " WHERE discussion_reply_id = ?",
        String.class,
        REPLY_ID);
  }

  private String getUserMeta(int index) {
    return jdbcTemplate.queryForObject(
        "SELECT meta_value FROM voj_usermeta WHERE meta_id = ?",
        String.class,
        FIRST_META_ID + index);
  }

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;

  /** Runs the fixture and assertion queries (auto-commit, outside any test transaction). */
  private JdbcTemplate jdbcTemplate;
}
