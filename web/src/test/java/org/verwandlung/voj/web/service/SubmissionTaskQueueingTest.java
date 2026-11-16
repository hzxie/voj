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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.verwandlung.voj.web.messenger.MessageSender;
import org.verwandlung.voj.web.model.User;

/**
 * Checks that judging tasks are queued only once the transaction that stored the submission has
 * committed: never for a transaction that rolls back, and that a task that cannot be queued after
 * the commit marks the submission as a system error instead of leaving it pending.
 *
 * <p>The tests drive real transactions, so this class is not {@code @Transactional}; rows that a
 * test commits are removed afterwards. The message queue is replaced by a mock.
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
    Assertions.assertEquals(2, submissionService.getSubmission(submissionId).getJudgeGeneration());
  }

  /**
   * Test case: creates a submission (outside any caller transaction, as the controller does) while
   * the message queue is unavailable. Test data: a valid submission; sending throws. Expected: the
   * user still gets the successful result, and the submission is marked as a system error rather
   * than left pending.
   */
  @Test
  public void testSubmissionIsMarkedAsSystemErrorWhenTheTaskCannotBeQueued() {
    doThrow(new UncategorizedJmsException("broker down"))
        .when(messageSender)
        .sendMessage(anyMap(), any());
    // The failure is recorded in a new transaction while the committed one still holds its
    // connection. The test pool has a single connection (production pools have several), so allow
    // a second one for this test.
    DruidDataSource druidDataSource = (DruidDataSource) dataSource;
    int maxActive = druidDataSource.getMaxActive();
    druidDataSource.setMaxActive(maxActive + 1);

    Map<String, Object> result;
    try {
      result =
          submissionService.createSubmission(
              userWithUid(1000), 1000, "text/x-java", "public class Main {}");
    } finally {
      druidDataSource.setMaxActive(maxActive);
    }
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    long submissionId = (Long) result.get("submissionId");
    committedSubmissionIds.add(submissionId);

    verify(messageSender).sendMessage(isTaskOf(submissionId), eq("text/x-java"));
    Assertions.assertEquals(
        "SE", submissionService.getSubmission(submissionId).getJudgeResult().getJudgeResultSlug());
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

  /** The transaction manager, used to drive the transactions under test. */
  @Autowired private PlatformTransactionManager transactionManager;

  /** Runs fixture queries, and the clean-up of committed rows. */
  private JdbcTemplate jdbcTemplate;

  /** Drives the transactions under test. */
  private TransactionTemplate transactionTemplate;

  /** The submissions committed by the current test, removed after it. */
  private final List<Long> committedSubmissionIds = new ArrayList<>();
}
