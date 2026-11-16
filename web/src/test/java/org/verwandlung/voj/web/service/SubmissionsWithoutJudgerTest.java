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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.mapper.SubmissionMapper;

/**
 * Checks that the pending submissions in a language that no judger taking submissions supports are
 * marked as a System Error once that lasted the timeout, and only those: neither a submission a
 * judging holds, nor one in a language a running judger takes submissions in, however long it
 * waited.
 *
 * <p>The marking runs outside any transaction, so this class is not {@code @Transactional}; the
 * rows a test commits are removed afterwards. The timeout is shortened to {@value #TIMEOUT} ms.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionsWithoutJudgerTest {
  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM voj_judgers");
    setTimeout(TIMEOUT);
    // Observes from now on, as an instance that has just started.
    ((Map<?, ?>) ReflectionTestUtils.getField(getTarget(), "languagesLastSupportedAt")).clear();
    ReflectionTestUtils.setField(getTarget(), "observedSince", System.nanoTime());
  }

  @AfterEach
  public void tearDown() {
    setTimeout(300000);
    jdbcTemplate.update("DELETE FROM voj_judgers");
    for (long submissionId : committedSubmissionIds) {
      jdbcTemplate.update("DELETE FROM voj_submissions WHERE submission_id = ?", submissionId);
    }
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: two pending submissions in C++,
   * which no judger supports. Expected: they wait for the timeout, then both are marked SE by one
   * run, with the judge log that says no judger is available for their language; a further run marks
   * nothing.
   */
  @Test
  public void testUnsupportedLanguageIsSystemErrorAfterTheTimeout() throws Exception {
    long pending = insertSubmission(CPP, null, null);
    long another = insertSubmission(CPP, null, null);

    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("PD", getJudgeResult(pending));
    Thread.sleep(TIMEOUT + 200);

    Assertions.assertEquals(2, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("SE", getJudgeResult(pending));
    Assertions.assertEquals("SE", getJudgeResult(another));
    Assertions.assertEquals(
        SubmissionService.NO_JUDGER_JUDGE_LOG,
        submissionService.getSubmission(pending).getJudgeLog());
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
  }

  /**
   * Test case: tests SubmissionMapper.getLanguageIdsOfReadyJudgers(int). Test data: a running judger
   * that takes submissions in C++ and Java, one that takes none in Python (not ready), and one in
   * Pascal whose last heartbeat is 2 minutes old. Expected: C++ and Java only.
   */
  @Test
  public void testTheLanguagesOfReadyJudgers() {
    registerJudger("3,2", true, 0);
    registerJudger("5", false, 0);
    registerJudger("4", true, 120);

    Assertions.assertEquals(
        Set.of(CPP, JAVA),
        Set.copyOf(
            submissionMapper.getLanguageIdsOfReadyJudgers(JudgerService.JUDGER_TIMEOUT_SECONDS)));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: a pending submission in C++ claimed
   * by the only judger that supports C++, which stopped (its last heartbeat is 2 minutes old) and
   * whose lease expired. Expected: it is marked SE once the timeout is over, without the claim
   * token of the vanished judging.
   */
  @Test
  public void testSubmissionOfAStoppedJudgerIsSystemErrorAfterTheTimeout() throws Exception {
    registerJudger("2", true, 120);
    long abandoned = insertSubmission(CPP, 7L, -30L);

    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(1, submissionService.failSubmissionsWithoutJudger());

    Assertions.assertEquals("SE", getJudgeResult(abandoned));
    Assertions.assertFalse(submissionService.isCurrentJudging(abandoned, 7L));
    Assertions.assertNull(
        jdbcTemplate.queryForObject(
            "SELECT submission_claim_token FROM voj_submissions WHERE submission_id = ?",
            Long.class,
            abandoned));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: a backlog of pending submissions in
   * C++ queued a day ago (one of which a judging held until its lease expired), and a running
   * judger that takes submissions in C++ and Java. Expected: however long they waited, none is
   * marked, run after run.
   */
  @Test
  public void testBacklogWithARunningJudgerIsNeverSystemError() throws Exception {
    registerJudger("3,2", true, 0);
    List<Long> backlog = new ArrayList<>();
    for (int i = 0; i < 3; ++i) {
      backlog.add(insertSubmission(CPP, null, null));
    }
    backlog.add(insertSubmission(CPP, 8L, -60L));
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_queued_time = NOW() - INTERVAL 1 DAY"
            + " WHERE submission_id IN (?, ?, ?, ?)",
        backlog.toArray());

    for (int run = 0; run < 3; ++run) {
      Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
      Thread.sleep(TIMEOUT / 2 + 100);
    }
    for (long submissionId : backlog) {
      Assertions.assertEquals("PD", getJudgeResult(submissionId));
    }
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: the only judger that supports C++
   * runs but takes no submissions (its judgings failed too often in a row), a pending
   * submission in C++ that a judging holds (its lease has not expired) and one that waits. Expected:
   * only the waiting one is marked once the timeout is over; the held one is left to its judging.
   */
  @Test
  public void testAJudgingThatHoldsASubmissionIsLeftAlone() throws Exception {
    registerJudger("2", false, 0);
    long held = insertSubmission(CPP, 7L, 60L);
    long waiting = insertSubmission(CPP, null, null);

    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(1, submissionService.failSubmissionsWithoutJudger());

    Assertions.assertEquals("SE", getJudgeResult(waiting));
    Assertions.assertEquals("PD", getJudgeResult(held));
    Assertions.assertTrue(submissionService.isCurrentJudging(held, 7L));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: a pending submission in Java, which
   * a judger that takes submissions supports for a while, then not any more (it stopped). Expected:
   * the timeout starts over once the judger is gone, so the submission is marked only a timeout
   * after that.
   */
  @Test
  public void testTheTimeoutStartsOverOnceAJudgerWasBack() throws Exception {
    long pending = insertSubmission(JAVA, null, null);
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Thread.sleep(TIMEOUT + 200);

    registerJudger("3", true, 0);
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    jdbcTemplate.update("DELETE FROM voj_judgers");
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("PD", getJudgeResult(pending));

    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(1, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("SE", getJudgeResult(pending));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: the instance has seen no judger
   * support C++ for longer than the timeout when a submission in C++ arrives. Expected: the first
   * run after it arrives marks it; it does not wait for the timeout again.
   */
  @Test
  public void testASubmissionInALanguageLongWithoutAJudgerIsMarkedAtOnce() throws Exception {
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Thread.sleep(TIMEOUT + 200);

    long pending = insertSubmission(CPP, null, null);
    Assertions.assertEquals(1, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("SE", getJudgeResult(pending));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: a judger supports C++ (the instance
   * has observed for longer than the timeout), then stops; a submission in C++ arrives. Expected:
   * it waits for the timeout from when the judger was last seen, then it is marked.
   */
  @Test
  public void testASubmissionWaitsForTheTimeoutFromTheLastJudgerOfItsLanguage() throws Exception {
    registerJudger("2", true, 0);
    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    jdbcTemplate.update("DELETE FROM voj_judgers");

    long pending = insertSubmission(CPP, null, null);
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("PD", getJudgeResult(pending));

    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(1, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("SE", getJudgeResult(pending));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger(). Test data: no judger supports C++ for longer
   * than the timeout, then a run fails (the database is unreachable); a submission in C++ arrives.
   * Expected: the failed run starts the timeout over, as the judgers' heartbeats may not have been
   * recorded meanwhile; the submission is marked a timeout later.
   */
  @Test
  public void testAFailedRunStartsTheTimeoutOver() throws Exception {
    Thread.sleep(TIMEOUT + 200);
    SubmissionMapper unreachable = mock(SubmissionMapper.class);
    when(unreachable.getLanguageIdsOfReadyJudgers(anyInt()))
        .thenThrow(new DataAccessResourceFailureException("down"));
    ReflectionTestUtils.setField(getTarget(), "submissionMapper", unreachable);
    try {
      Assertions.assertThrows(
          DataAccessResourceFailureException.class,
          () -> submissionService.failSubmissionsWithoutJudger());
    } finally {
      ReflectionTestUtils.setField(getTarget(), "submissionMapper", submissionMapper);
    }

    long pending = insertSubmission(CPP, null, null);
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(1, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("SE", getJudgeResult(pending));
  }

  /**
   * Test case: tests failSubmissionsWithoutJudger() with a timeout of 0. Test data: a pending
   * submission in C++ that no judger supports. Expected: it waits for a judger, as long as it
   * takes.
   */
  @Test
  public void testWithoutTimeoutSubmissionsWaitForever() throws Exception {
    long pending = insertSubmission(CPP, null, null);
    setTimeout(0);

    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Thread.sleep(TIMEOUT + 200);
    Assertions.assertEquals(0, submissionService.failSubmissionsWithoutJudger());
    Assertions.assertEquals("PD", getJudgeResult(pending));
  }

  /**
   * Registers a process of the judger account #1001 (voj@judger) as a running judger.
   *
   * @param languageIds - the languages it supports, comma-separated
   * @param isReady - whether it takes submissions
   * @param heartbeatSecondsAgo - how long ago its last heartbeat was, by the database's clock
   */
  private void registerJudger(String languageIds, boolean isReady, long heartbeatSecondsAgo) {
    jdbcTemplate.update(
        "INSERT INTO voj_judgers (judger_instance, judger_uid, judger_language_ids,"
            + " judger_is_ready, judger_heartbeat_time)"
            + " VALUES (UUID(), 1001, ?, ?, NOW() - INTERVAL ? SECOND)",
        languageIds,
        isReady,
        heartbeatSecondsAgo);
  }

  /**
   * Inserts a pending submission of Problem #1000, queued now, committed, by the database's clock.
   *
   * @param languageId - the unique identifier of its language
   * @param claimToken - the claim token of the judging that holds it, or null
   * @param leaseExpiresInSeconds - in how many seconds the lease of that judging expires (negative:
   *     it expired), or null
   * @return the unique identifier of the submission
   */
  private long insertSubmission(int languageId, Long claimToken, Long leaseExpiresInSeconds) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_code,"
                      + " submission_claim_token, submission_lease_expiry_time)"
                      + " VALUES (1000, 1000, ?, 'Code', ?, NOW() + INTERVAL ? SECOND)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, languageId);
          statement.setObject(2, claimToken);
          statement.setObject(3, leaseExpiresInSeconds);
          return statement;
        },
        keyHolder);
    long submissionId = keyHolder.getKey().longValue();
    committedSubmissionIds.add(submissionId);
    return submissionId;
  }

  /** Gets the submission service behind its transactional proxy. */
  private SubmissionService getTarget() {
    return AopTestUtils.getTargetObject(submissionService);
  }

  /** Sets the timeout (ms) of the submission service. */
  private void setTimeout(long timeout) {
    ReflectionTestUtils.setField(getTarget(), "noJudgerTimeout", timeout);
  }

  /** Gets the slug of the judge result of a submission. */
  private String getJudgeResult(long submissionId) {
    return jdbcTemplate.queryForObject(
        "SELECT submission_judge_result FROM voj_submissions WHERE submission_id = ?",
        String.class,
        submissionId);
  }

  /** The timeout (ms) of the tests. */
  private static final long TIMEOUT = 1500;

  /** The language C++. */
  private static final int CPP = 2;

  /** The language Java. */
  private static final int JAVA = 3;

  /** The autowired SubmissionService object. */
  @Autowired private SubmissionService submissionService;

  /** The autowired SubmissionMapper object. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;

  /** Runs the fixture and assertion queries (auto-commit). */
  private JdbcTemplate jdbcTemplate;

  /** The submissions a test committed, removed after it. */
  private final List<Long> committedSubmissionIds = new ArrayList<>();
}
