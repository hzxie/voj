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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.verwandlung.voj.web.messenger.MessageSender;
import org.verwandlung.voj.web.model.User;

/**
 * Checks that judging tasks are queued only once the transaction that stored the submission has
 * committed: never for a transaction that rolls back, and that a submission that waits in vain (its
 * task cannot be queued after the commit, or its judging stopped) is marked as a system error.
 *
 * <p>The tests drive real transactions (and the marking runs outside any transaction), so this
 * class is not {@code @Transactional}; rows that a test commits are removed afterwards. The message
 * queue is replaced by a mock.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionTaskQueueingTest {
  /** The identifier of the contest created by the contest test (not used by test-data.sql). */
  private static final long CONTEST_ID = 100;

  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @AfterEach
  public void tearDown() {
    for (long submissionId : committedSubmissionIds) {
      jdbcTemplate.update("DELETE FROM voj_submissions WHERE submission_id = ?", submissionId);
    }
  }

  /**
   * Test case: creates a submission in a transaction that rolls back. Test data: a valid
   * submission. Expected: no judging task is sent, and the submission does not exist.
   */
  @Test
  public void testNoTaskIsQueuedWhenTheTransactionRollsBack() {
    long submissionId =
        transactionTemplate.execute(
            status -> {
              long id = createSubmission();
              status.setRollbackOnly();
              return id;
            });

    verify(messageSender, never()).sendMessage(anyMap(), any());
    Assertions.assertNull(submissionService.getSubmission(submissionId));
  }

  /**
   * Test case: creates a submission in a transaction that commits. Test data: a valid submission.
   * Expected: the task is not sent while the transaction is open, and is sent once it commits.
   */
  @Test
  public void testTaskIsQueuedAfterCommit() {
    long submissionId =
        transactionTemplate.execute(
            status -> {
              long id = createSubmission();
              verify(messageSender, never()).sendMessage(anyMap(), any());
              return id;
            });
    committedSubmissionIds.add(submissionId);

    verify(messageSender).sendMessage(isTaskOf(submissionId), eq("text/x-java"));
    Assertions.assertEquals(
        "PD", submissionService.getSubmission(submissionId).getJudgeResult().getJudgeResultSlug());
  }

  /**
   * Test case: creates a submission, then rejudges it twice, each in a transaction that commits.
   * Test data: a valid submission. Expected: the task of the new submission carries the judge
   * generation 0 and each rejudge's task the generation that rejudge wrote (1, then 2), which is
   * also the submission's generation afterwards.
   */
  @Test
  public void testRejudgeTasksCarryTheNewJudgeGeneration() {
    long submissionId = transactionTemplate.execute(status -> createSubmission());
    committedSubmissionIds.add(submissionId);
    transactionTemplate.executeWithoutResult(
        status -> submissionService.createSubmissionTask(submissionId));
    transactionTemplate.executeWithoutResult(
        status -> submissionService.createSubmissionTasks(List.of(submissionId)));

    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(messageSender);
    for (long generation = 0; generation <= 2; ++generation) {
      inOrder
          .verify(messageSender)
          .sendMessage(isTaskOf(submissionId, generation), eq("text/x-java"));
    }
    Assertions.assertEquals(
        2,
        jdbcTemplate.queryForObject(
            "SELECT submission_judge_generation FROM voj_submissions WHERE submission_id = ?",
            Integer.class,
            submissionId));
  }

  /**
   * Test case: creates a submission (outside any caller transaction, as the controller does) while
   * the message queue is unavailable, then lets the time-to-live of its task pass. Test data: a
   * valid submission; sending throws. Expected: the user still gets the successful result, and the
   * submission stays pending until it waited too long for a judger, then is a system error.
   */
  @Test
  public void testSubmissionWhoseTaskCannotBeQueuedIsFailedInTime() {
    doThrow(new UncategorizedJmsException("broker down"))
        .when(messageSender)
        .sendMessage(anyMap(), any());

    Map<String, Object> result =
        submissionService.createSubmission(
            userWithUid(1000), 1000, "text/x-java", "public class Main {}");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    long submissionId = (Long) result.get("submissionId");
    committedSubmissionIds.add(submissionId);
    verify(messageSender).sendMessage(isTaskOf(submissionId), eq("text/x-java"));
    Assertions.assertEquals(
        "PD", submissionService.getSubmission(submissionId).getJudgeResult().getJudgeResultSlug());

    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_queued_time = NOW() - INTERVAL ? SECOND"
            + " WHERE submission_id = ?",
        submissionTaskTtl / 1000,
        submissionId);
    Assertions.assertEquals(1, submissionService.failStalledSubmissions());
    Assertions.assertEquals(
        SubmissionService.UNCLAIMED_JUDGE_LOG,
        submissionService.getSubmission(submissionId).getJudgeLog());
  }

  /**
   * Test case: tests failStalledSubmissions(). Test data, by the database's clock, pending
   * submissions: unclaimed and queued a minute more than the time-to-live ago (overdue), or a
   * minute less (due); claimed, with a lease renewed a minute more than the lease timeout ago
   * (expired); claimed (and queued) long ago, with a lease renewed a minute ago (fresh); queued
   * long ago, then rejudged; and a judged submission with an old queued time and lease. Expected:
   * the overdue one and the expired one are marked SE, each with the judge log of what happened,
   * and the expired one loses its claim token; the others are left as they are, and a second run
   * marks nothing.
   */
  @Test
  public void testFailStalledSubmissions() {
    long ttl = submissionTaskTtl / 1000;
    long leaseTimeout = LEASE_TIMEOUT / 1000;
    long overdue = insertSubmission("PD", null, ttl + 60, null);
    long due = insertSubmission("PD", null, ttl - 60, null);
    long expired = insertSubmission("PD", 7L, leaseTimeout + 120, leaseTimeout + 60);
    long fresh = insertSubmission("PD", 8L, ttl + 3600, 60L);
    long rejudged = insertSubmission("PD", null, ttl + 60, null);
    transactionTemplate.executeWithoutResult(
        status -> submissionService.createSubmissionTasks(List.of(rejudged)));
    long judged = insertSubmission("AC", 9L, ttl + 3600, ttl + 3600);

    Assertions.assertEquals(2, submissionService.failStalledSubmissions());
    Assertions.assertEquals("SE", getJudgeResult(overdue));
    Assertions.assertEquals(
        SubmissionService.UNCLAIMED_JUDGE_LOG,
        submissionService.getSubmission(overdue).getJudgeLog());
    Assertions.assertEquals("SE", getJudgeResult(expired));
    Assertions.assertEquals(
        SubmissionService.ABANDONED_JUDGE_LOG,
        submissionService.getSubmission(expired).getJudgeLog());
    Assertions.assertFalse(submissionService.isCurrentJudging(expired, 7L));
    for (long submissionId : new long[] {due, fresh, rejudged}) {
      Assertions.assertEquals("PD", getJudgeResult(submissionId));
    }
    Assertions.assertTrue(submissionService.isCurrentJudging(fresh, 8L));
    Assertions.assertEquals("AC", getJudgeResult(judged));

    Assertions.assertEquals(0, submissionService.failStalledSubmissions());
  }

  /**
   * Test case: tests failStalledSubmissions() with a time-to-live of 0. Test data: a pending
   * submission queued ten days ago and not claimed, and a claimed one whose lease expired.
   * Expected: the unclaimed one waits for a judger, as its task never expires; the other is marked
   * SE.
   */
  @Test
  public void testFailStalledSubmissionsWithoutTimeToLive() throws Exception {
    long unclaimed = insertSubmission("PD", null, 10 * 24 * 3600, null);
    long leaseTimeout = LEASE_TIMEOUT / 1000;
    long expired = insertSubmission("PD", 7L, leaseTimeout + 120, leaseTimeout + 60);
    SubmissionService target = AopTestUtils.getTargetObject(submissionService);
    ReflectionTestUtils.setField(target, "submissionTaskTtl", 0L);
    try {
      Assertions.assertEquals(1, submissionService.failStalledSubmissions());
    } finally {
      ReflectionTestUtils.setField(target, "submissionTaskTtl", submissionTaskTtl);
    }
    Assertions.assertEquals("PD", getJudgeResult(unclaimed));
    Assertions.assertEquals("SE", getJudgeResult(expired));
  }

  /**
   * Test case: tests the lease timeout. Test data: 0. Expected: it is refused, as every judging
   * would be failed at once.
   */
  @Test
  public void testLeaseTimeoutMustBePositive() throws Exception {
    SubmissionService target = AopTestUtils.getTargetObject(submissionService);
    Assertions.assertThrows(IllegalArgumentException.class, () -> target.setLeaseTimeout(0));
  }

  /**
   * Test case: creates a contest submission in a transaction that rolls back. Test data: a running
   * contest attended by user 1000. Expected: no judging task is sent.
   */
  @Test
  public void testNoContestTaskIsQueuedWhenTheTransactionRollsBack() {
    transactionTemplate.executeWithoutResult(
        status -> {
          long now = System.currentTimeMillis();
          jdbcTemplate.update(
              "INSERT INTO voj_contests (contest_id, contest_name, contest_notes,"
                  + " contest_start_time, contest_end_time, contest_problems,"
                  + " contest_status, contest_scoring, contest_penalty, contest_freeze)"
                  + " VALUES (?, 'Fixture', '', ?, ?, '[1000]', 'PUBLISHED', 'ICPC', 20, 0)",
              CONTEST_ID,
              new Timestamp(now - 3600_000L),
              new Timestamp(now + 3600_000L));
          jdbcTemplate.update(
              "INSERT INTO voj_contest_contestants (contest_id, contestant_uid) VALUES (?, 1000)",
              CONTEST_ID);
          Map<String, Object> result =
              contestService.createSubmission(
                  CONTEST_ID, 1000, userWithUid(1000), "text/x-java", "public class Main {}");
          Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"), result.toString());
          status.setRollbackOnly();
        });

    verify(messageSender, never()).sendMessage(anyMap(), any());
  }

  /**
   * Test case: rejudges a submission in a transaction that rolls back. Test data: submission 1000.
   * Expected: no judging task is sent, and the submission keeps its verdict.
   */
  @Test
  public void testNoRejudgeTaskIsQueuedWhenTheTransactionRollsBack() {
    transactionTemplate.executeWithoutResult(
        status -> {
          submissionService.createSubmissionTask(1000);
          status.setRollbackOnly();
        });

    verify(messageSender, never()).sendMessage(anyMap(), any());
    Assertions.assertEquals(
        "AC", submissionService.getSubmission(1000).getJudgeResult().getJudgeResultSlug());
  }

  /**
   * Inserts a submission, committed, by the database's clock.
   *
   * @param judgeResult - the slug of its judge result
   * @param claimToken - the claim token of the judging that claimed it, or null
   * @param queuedSecondsAgo - how long ago its task was queued, and claimed if it was
   * @param leaseSecondsAgo - how long ago its lease was renewed, or null
   * @return the unique identifier of the submission
   */
  private long insertSubmission(
      String judgeResult, Long claimToken, long queuedSecondsAgo, Long leaseSecondsAgo) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_code,"
                      + " submission_judge_result, submission_claim_token, submission_queued_time,"
                      + " submission_execute_time, submission_lease_time)"
                      + " VALUES (1000, 1000, 2, 'Code', ?, ?, NOW() - INTERVAL ? SECOND,"
                      + " IF(? IS NULL, NULL, NOW() - INTERVAL ? SECOND),"
                      + " NOW() - INTERVAL ? SECOND)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, judgeResult);
          statement.setObject(2, claimToken);
          statement.setLong(3, queuedSecondsAgo);
          statement.setObject(4, claimToken);
          statement.setLong(5, queuedSecondsAgo);
          statement.setObject(6, leaseSecondsAgo);
          return statement;
        },
        keyHolder);
    long submissionId = keyHolder.getKey().longValue();
    committedSubmissionIds.add(submissionId);
    return submissionId;
  }

  /** Gets the slug of the judge result of a submission. */
  private String getJudgeResult(long submissionId) {
    return jdbcTemplate.queryForObject(
        "SELECT submission_judge_result FROM voj_submissions WHERE submission_id = ?",
        String.class,
        submissionId);
  }

  /** Creates a valid submission and returns its identifier. */
  private long createSubmission() {
    Map<String, Object> result =
        submissionService.createSubmission(
            userWithUid(1000), 1000, "text/x-java", "public class Main {}");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"), result.toString());
    return (Long) result.get("submissionId");
  }

  /** Matches the judging task message of a new submission (judge generation 0). */
  private static Map<String, Object> isTaskOf(long submissionId) {
    return isTaskOf(submissionId, 0);
  }

  /** Matches the judging task message of a judge generation of a submission. */
  private static Map<String, Object> isTaskOf(long submissionId, long generation) {
    return argThat(
        message ->
            "SubmissionCreated".equals(message.get("event"))
                && Long.valueOf(submissionId).equals(message.get("submissionId"))
                && Long.valueOf(generation).equals(message.get("generation")));
  }

  private static User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** The message queue, replaced by a mock. */
  @MockitoBean private MessageSender messageSender;

  /** The autowired SubmissionService object. */
  @Autowired private SubmissionService submissionService;

  /** The autowired ContestService object. */
  @Autowired private ContestService contestService;

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;

  /** The time-to-live (ms) of a judging task. */
  @Value("${jms.submissionTask.ttl}")
  private long submissionTaskTtl;

  /** The lease timeout (ms) of a judging: the default, as the test properties do not set it. */
  private static final long LEASE_TIMEOUT = 600000;

  /** The transaction manager, used to drive the transactions under test. */
  @Autowired private PlatformTransactionManager transactionManager;

  /** Runs fixture queries, and the clean-up of committed rows. */
  private JdbcTemplate jdbcTemplate;

  /** Drives the transactions under test. */
  private TransactionTemplate transactionTemplate;

  /** The submissions committed by the current test, removed after it. */
  private final List<Long> committedSubmissionIds = new ArrayList<>();
}
