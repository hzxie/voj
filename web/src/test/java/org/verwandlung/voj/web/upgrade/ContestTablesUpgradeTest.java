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
 * Runs the one-time upgrade script sql/upgrades/contest-tables.sql against a database in the state
 * schema.sql of 1.0.2 created.
 *
 * <p>The script changes the schema (DDL commits implicitly), so this test is not {@code
 * Transactional}: it puts the shared test database back into the 1.0.2 state before each test and
 * leaves it upgraded - i.e. as the current schema.sql creates it - afterwards.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestTablesUpgradeTest {
  /** The upgrade script, copied onto the test classpath from sql/upgrades. */
  private static final String SCRIPT = "db/upgrades/contest-tables.sql";

  /** The option that marks the upgrade as done. */
  private static final String MARKER = "contestTablesUpgraded";

  /** The foreign keys the upgrade turns into cascading ones: { table, constraint, definition }. */
  private static final String[][] FOREIGN_KEYS = {
    {
      "voj_contest_contestants",
      "voj_contest_contestants_ibfk_1",
      "FOREIGN KEY (contest_id) REFERENCES voj_contests (contest_id)"
    },
    {
      "voj_contest_contestants",
      "voj_contest_contestants_ibfk_2",
      "FOREIGN KEY (contestant_uid) REFERENCES voj_users (uid)"
    },
    {
      "voj_contest_submissions",
      "voj_contest_submissions_ibfk_1",
      "FOREIGN KEY (contest_id) REFERENCES voj_contests (contest_id)"
    },
    {
      "voj_contest_submissions",
      "voj_contest_submissions_ibfk_2",
      "FOREIGN KEY (submission_id) REFERENCES voj_submissions (submission_id)"
    },
  };

  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM voj_options WHERE option_name = ?", MARKER);
    // The foreign keys as 1.0.2 created them: without an ON DELETE rule.
    for (String[] foreignKey : FOREIGN_KEYS) {
      jdbcTemplate.execute(
          String.format("ALTER TABLE %s DROP FOREIGN KEY %s", foreignKey[0], foreignKey[1]));
      jdbcTemplate.execute(
          String.format(
              "ALTER TABLE %s ADD CONSTRAINT %s %s",
              foreignKey[0], foreignKey[1], foreignKey[2]));
    }
  }

  @AfterEach
  public void tearDown() throws SQLException {
    // Whatever a test did, leave the schema upgraded and the marker exactly as seed.sql has it.
    runUpgrade();
    jdbcTemplate.update("DELETE FROM voj_options WHERE option_name = ?", MARKER);
    jdbcTemplate.update(
        "INSERT INTO voj_options (option_id, option_name, option_value, is_autoload)"
            + " VALUES (31, ?, '1', 0)",
        MARKER);
  }

  /**
   * Test case: runs the upgrade on a 1.0.2 database, then once more. Test data: the contest foreign
   * keys without an ON DELETE rule. Expected: every one of them cascades on delete and on update
   * afterwards, the marker is recorded once, and the second run changes nothing.
   */
  @Test
  public void testMakesContestForeignKeysCascade() throws SQLException {
    for (String[] foreignKey : FOREIGN_KEYS) {
      Assertions.assertNotEquals("CASCADE", getDeleteRule(foreignKey[1]), foreignKey[1]);
    }

    runUpgrade();
    for (String[] foreignKey : FOREIGN_KEYS) {
      Assertions.assertEquals("CASCADE", getDeleteRule(foreignKey[1]), foreignKey[1]);
      Assertions.assertEquals("CASCADE", getUpdateRule(foreignKey[1]), foreignKey[1]);
    }
    Assertions.assertEquals(1, getNumberOfMarkers());

    runUpgrade();
    for (String[] foreignKey : FOREIGN_KEYS) {
      Assertions.assertEquals("CASCADE", getDeleteRule(foreignKey[1]), foreignKey[1]);
    }
    Assertions.assertEquals(1, getNumberOfMarkers());
  }

  /**
   * Test case: completes an interrupted upgrade. Test data: one of the foreign keys is missing, as
   * after a run that stopped between dropping and adding it again. Expected: the key is restored
   * with its cascading rules.
   */
  @Test
  public void testRestoresForeignKeyDroppedByInterruptedRun() throws SQLException {
    jdbcTemplate.execute(
        "ALTER TABLE voj_contest_submissions DROP FOREIGN KEY voj_contest_submissions_ibfk_2");
    Assertions.assertNull(getDeleteRule("voj_contest_submissions_ibfk_2"));

    runUpgrade();

    Assertions.assertEquals("CASCADE", getDeleteRule("voj_contest_submissions_ibfk_2"));
  }

  /** Runs the upgrade script as an operator would: on one connection, statement by statement. */
  private void runUpgrade() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(
          connection, new EncodedResource(new ClassPathResource(SCRIPT), StandardCharsets.UTF_8));
    }
  }

  private String getDeleteRule(String constraintName) {
    return getRule("DELETE_RULE", constraintName);
  }

  private String getUpdateRule(String constraintName) {
    return getRule("UPDATE_RULE", constraintName);
  }

  private String getRule(String column, String constraintName) {
    return jdbcTemplate
        .queryForList(
            "SELECT "
                + column
                + " FROM information_schema.REFERENTIAL_CONSTRAINTS"
                + " WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = ?",
            String.class,
            constraintName)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private int getNumberOfMarkers() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM voj_options WHERE option_name = ?", Integer.class, MARKER);
  }

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;

  /** Runs the fixture and assertion queries (auto-commit, outside any test transaction). */
  private JdbcTemplate jdbcTemplate;
}
