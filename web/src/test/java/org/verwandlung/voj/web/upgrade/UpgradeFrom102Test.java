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
 * Runs the one-time upgrade script sql/upgrades/upgrade-from-1.0.2.sql against a database in the
 * state schema.sql of 1.0.2 created: contest foreign keys without an ON DELETE rule, the
 * contest_mode and contest_is_rated columns, no submission_judge_generation column, and problem
 * statements and contest notes written for HTML output (with HTML entities).
 *
 * <p>The script changes the schema (DDL commits implicitly), so this test is not {@code
 * Transactional}: it puts the shared test database back into the 1.0.2 state before each test and
 * leaves it upgraded - i.e. as the current schema.sql creates it - afterwards.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UpgradeFrom102Test {
  /** The upgrade script, copied onto the test classpath from sql/upgrades. */
  private static final String SCRIPT = "db/upgrades/upgrade-from-1.0.2.sql";

  /** The option that marks the upgrade as done. */
  private static final String MARKER = "upgradeFrom102Applied";

  /** The problem inserted by this test, with a statement written for HTML output. */
  private static final long PROBLEM_ID = 9101;

  /**
   * The text fields of that problem: { column, value as written for 1.0.2, value after the upgrade
   * }. The hint was written as text in 1.0.2 already, so it is not decoded.
   */
  private static final String[][] PROBLEM_FIELDS = {
    {"problem_description", "If $a &lt; b$, print &quot;&lt;&quot;.", "If $a < b$, print \"<\"."},
    {"problem_input_format", "x &amp;&amp; y, it&#39;s&nbsp;fine", "x && y, it's\u00a0fine"},
    {"problem_output_format", "Literal &amp;lt; stays", "Literal &lt; stays"},
    {"problem_sample_input", "1 2", "1 2"},
    {"problem_sample_output", "&gt; = &lt; = &lt;", "> = < = <"},
    {"problem_hint", "Use `a &lt; b`", "Use `a &lt; b`"},
  };

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

  /**
   * The contests inserted by this test: { id, contest_mode, contest_scoring as stored by 1.0.2,
   * contest_scoring after the upgrade }. 9101 was created before the scoring setting existed (the
   * column was added with the default ICPC) and ranked OI-style; 9102 was created with the
   * Codeforces rule, which was ranked ICPC-style.
   */
  private static final String[][] CONTESTS = {
    {"9101", "OI", "ICPC", "IOI"},
    {"9102", "ACM", "Codeforces", "ICPC"},
    {"9103", "OI", "IOI", "IOI"},
    {"9104", "ACM", "ICPC", "ICPC"},
  };

  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM voj_options WHERE option_name = ?", MARKER);
    // The contest columns as 1.0.2 created them.
    jdbcTemplate.execute(
        "ALTER TABLE voj_contests ADD COLUMN contest_mode varchar(4) NOT NULL DEFAULT ''"
            + " AFTER contest_end_time");
    jdbcTemplate.execute(
        "ALTER TABLE voj_contests ADD COLUMN contest_is_rated tinyint(1) NOT NULL DEFAULT '1'"
            + " AFTER contest_freeze");
    jdbcTemplate.update(
        "UPDATE voj_contests SET contest_mode = IF(contest_scoring = 'IOI', 'OI', 'ACM')");
    // The submissions as 1.0.2 created them, without a judge generation.
    jdbcTemplate.execute("ALTER TABLE voj_submissions DROP COLUMN submission_judge_generation");
    // A statement and contest notes written for HTML output.
    StringBuilder columns = new StringBuilder();
    Object[] values = new Object[PROBLEM_FIELDS.length + 1];
    values[0] = PROBLEM_ID;
    for (int i = 0; i < PROBLEM_FIELDS.length; ++i) {
      columns.append(", ").append(PROBLEM_FIELDS[i][0]);
      values[i + 1] = PROBLEM_FIELDS[i][1];
    }
    jdbcTemplate.update(
        "INSERT INTO voj_problems (problem_id, problem_name, problem_time_limit,"
            + " problem_memory_limit"
            + columns
            + ") VALUES (?, 'Legacy', 1000, 65536"
            + ", ?".repeat(PROBLEM_FIELDS.length)
            + ")",
        values);
    for (String[] contest : CONTESTS) {
      jdbcTemplate.update(
          "INSERT INTO voj_contests (contest_id, contest_name, contest_notes, contest_start_time,"
              + " contest_end_time, contest_mode, contest_problems, contest_scoring)"
              + " VALUES (?, 'Legacy', 'Rule: a &lt;= b', NOW(), NOW(), ?, '[]', ?)",
          Long.parseLong(contest[0]),
          contest[1],
          contest[2]);
    }
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
    jdbcTemplate.update("DELETE FROM voj_contests WHERE contest_id BETWEEN 9101 AND 9104");
    jdbcTemplate.update("DELETE FROM voj_problems WHERE problem_id = ?", PROBLEM_ID);
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
   * Test case: runs the upgrade on a 1.0.2 database, then once more after an administrator changed
   * a scoring rule. Test data: contests whose contest_scoring disagrees with the contest_mode that
   * ranked them, and the fixture contests. Expected: contest_scoring is set to the rule each
   * contest was ranked by, contest_mode and contest_is_rated are dropped, and the second run keeps
   * the administrator's change.
   */
  @Test
  public void testKeepsOnlyTheScoringColumn() throws SQLException {
    runUpgrade();

    for (String[] contest : CONTESTS) {
      Assertions.assertEquals(contest[3], getScoring(contest[0]), contest[0]);
    }
    Assertions.assertEquals("IOI", getScoring("1"));
    Assertions.assertEquals("ICPC", getScoring("2"));
    Assertions.assertFalse(hasColumn("voj_contests", "contest_mode"));
    Assertions.assertFalse(hasColumn("voj_contests", "contest_is_rated"));

    jdbcTemplate.update(
        "UPDATE voj_contests SET contest_scoring = 'IOI' WHERE contest_id = 9102");
    runUpgrade();
    Assertions.assertEquals("IOI", getScoring("9102"));
  }

  /**
   * Test case: runs the upgrade on a 1.0.2 database, then once more. Test data: no judge
   * generation column. Expected: the column is added with 0 for the existing submissions, and the
   * second run leaves it (and a generation a rejudge set since) as it is.
   */
  @Test
  public void testAddsTheJudgeGeneration() throws SQLException {
    Assertions.assertFalse(hasColumn("voj_submissions", "submission_judge_generation"));

    runUpgrade();
    Assertions.assertTrue(hasColumn("voj_submissions", "submission_judge_generation"));
    Assertions.assertEquals(0, getJudgeGeneration(1000));

    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_generation = 2 WHERE submission_id = 1000");
    runUpgrade();
    Assertions.assertEquals(2, getJudgeGeneration(1000));
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_generation = 0 WHERE submission_id = 1000");
  }

  /**
   * Test case: runs the upgrade on a 1.0.2 database, then once more after an administrator wrote
   * entities in a statement on the new version. Test data: a statement and contest notes written
   * for HTML output. Expected: the first run decodes the entities of the description, the formats,
   * the samples and the notes ({@code &amp;} last, so {@code &amp;lt;} becomes {@code &lt;}) but not
   * of the hint; the second run decodes nothing.
   */
  @Test
  public void testDecodesTheEntitiesOfStatementsOnce() throws SQLException {
    runUpgrade();

    for (String[] field : PROBLEM_FIELDS) {
      Assertions.assertEquals(field[2], getProblemField(field[0]), field[0]);
    }
    Assertions.assertEquals(
        "Rule: a <= b",
        jdbcTemplate.queryForObject(
            "SELECT contest_notes FROM voj_contests WHERE contest_id = 9101", String.class));
    Assertions.assertEquals(1, getNumberOfMarkers());

    jdbcTemplate.update(
        "UPDATE voj_problems SET problem_sample_output = 'a &amp; b' WHERE problem_id = ?",
        PROBLEM_ID);
    runUpgrade();
    Assertions.assertEquals("a &amp; b", getProblemField("problem_sample_output"));
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

  private String getScoring(String contestId) {
    return jdbcTemplate.queryForObject(
        "SELECT contest_scoring FROM voj_contests WHERE contest_id = ?",
        String.class,
        Long.parseLong(contestId));
  }

  private boolean hasColumn(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                + " AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class,
            tableName,
            columnName)
        > 0;
  }

  private int getJudgeGeneration(long submissionId) {
    return jdbcTemplate.queryForObject(
        "SELECT submission_judge_generation FROM voj_submissions WHERE submission_id = ?",
        Integer.class,
        submissionId);
  }

  private String getProblemField(String column) {
    return jdbcTemplate.queryForObject(
        "SELECT " + column + " FROM voj_problems WHERE problem_id = ?", String.class, PROBLEM_ID);
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
