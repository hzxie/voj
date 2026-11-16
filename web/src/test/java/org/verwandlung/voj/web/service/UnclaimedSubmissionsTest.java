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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.messenger.ApplicationEventListener;

/**
 * Checks that the pending submissions no judger took within the maximum wait are marked as a
 * System Error, and only those: a submission a judger took is left to the judgers.
 *
 * <p>The marking runs outside any transaction, so this class is not {@code @Transactional}; the
 * rows a test commits are removed afterwards.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UnclaimedSubmissionsTest {
  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @AfterEach
  public void tearDown() {
    setMaxWait(MAX_WAIT);
    for (long submissionId : committedSubmissionIds) {
      jdbcTemplate.update("DELETE FROM voj_submissions WHERE submission_id = ?", submissionId);
    }
  }

  /**
   * Test case: tests failUnclaimedSubmissions(). Test data, by the database's clock, pending
   * submissions: never taken and queued a minute more than the maximum wait ago (overdue), or a
   * minute less (due); taken once and held by a judging with a live lease (held), or with an
   * expired lease (abandoned: the judging stopped, or failed and is to be taken again), both queued
   * long ago; queued long ago, then rejudged; and a judged submission queued long ago. Expected:
   * only the overdue one is marked SE, with the judge log that says no judger took it within the
   * maximum wait (in minutes); a second run marks nothing.
   */
  @Test
  public void testFailUnclaimedSubmissions() {
    long maxWait = MAX_WAIT / 1000;
    long overdue = insertSubmission("PD", 0, null, maxWait + 60, null);
    long due = insertSubmission("PD", 0, null, maxWait - 60, null);
    long held = insertSubmission("PD", 1, 7L, maxWait + 3600, 60L);
    long abandoned = insertSubmission("PD", 1, 8L, maxWait + 3600, -60L);
    long rejudged = insertSubmission("PD", 0, null, maxWait + 60, null);
    submissionService.rejudgeSubmissions(List.of(rejudged));
    long judged = insertSubmission("AC", 1, 9L, maxWait + 3600, -3600L);

    Assertions.assertEquals(1, submissionService.failUnclaimedSubmissions());
    Assertions.assertEquals("SE", getJudgeResult(overdue));
    Assertions.assertEquals(
        String.format(SubmissionService.UNCLAIMED_JUDGE_LOG, MAX_WAIT / 60000),
        submissionService.getSubmission(overdue).getJudgeLog());
    for (long submissionId : new long[] {due, held, abandoned, rejudged}) {
      Assertions.assertEquals("PD", getJudgeResult(submissionId));
    }
    Assertions.assertTrue(submissionService.isCurrentJudging(held, 7L));
    Assertions.assertEquals("AC", getJudgeResult(judged));

    Assertions.assertEquals(0, submissionService.failUnclaimedSubmissions());
  }

  /**
   * Test case: tests failUnclaimedSubmissions() with a maximum wait of 0. Test data: a pending
   * submission queued ten days ago that no judger took. Expected: it waits for a judger, as long as
   * it takes.
   */
  @Test
  public void testWithoutMaximumWaitSubmissionsWaitForever() {
    long unclaimed = insertSubmission("PD", 0, null, 10 * 24 * 3600, null);
    setMaxWait(0);

    Assertions.assertEquals(0, submissionService.failUnclaimedSubmissions());
    Assertions.assertEquals("PD", getJudgeResult(unclaimed));
  }

  /**
   * Test case: tests failUnclaimedSubmissions(). Test data: a viewer following the live judging of
   * a pending submission that no judger took in time. Expected: its stream receives the System
   * Error with the judge log, and is completed.
   */
  @Test
  public void testTheLiveStreamOfAFailedSubmissionIsCompleted() throws Exception {
    long overdue = insertSubmission("PD", 0, null, MAX_WAIT / 1000 + 60, null);
    SseEmitter viewer = mock(SseEmitter.class);
    applicationEventListener.addSseEmitters(overdue, viewer);

    Assertions.assertEquals(1, submissionService.failUnclaimedSubmissions());

    String judgeLog = String.format(SubmissionService.UNCLAIMED_JUDGE_LOG, MAX_WAIT / 60000);
    verify(viewer)
        .send(
            argThat(
                (Object message) ->
                    Map.of("judgeResult", "System Error", "message", judgeLog).equals(message)));
    verify(viewer).complete();
  }

  /**
   * Inserts a submission, committed, by the database's clock.
   *
   * @param judgeResult - the slug of its judge result
   * @param attempts - how many judgings took it
   * @param claimToken - the claim token of the judging that holds it, or null
   * @param queuedSecondsAgo - how long ago it was queued
   * @param leaseExpiresInSeconds - in how many seconds the lease of that judging expires (negative:
   *     it expired), or null
   * @return the unique identifier of the submission
   */
  private long insertSubmission(
      String judgeResult,
      int attempts,
      Long claimToken,
      long queuedSecondsAgo,
      Long leaseExpiresInSeconds) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_code,"
                      + " submission_judge_result, submission_judge_attempts,"
                      + " submission_claim_token, submission_queued_time,"
                      + " submission_lease_expiry_time)"
                      + " VALUES (1000, 1000, 2, 'Code', ?, ?, ?, NOW() - INTERVAL ? SECOND,"
                      + " NOW() + INTERVAL ? SECOND)",
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, judgeResult);
          statement.setInt(2, attempts);
          statement.setObject(3, claimToken);
          statement.setLong(4, queuedSecondsAgo);
          statement.setObject(5, leaseExpiresInSeconds);
          return statement;
        },
        keyHolder);
    long submissionId = keyHolder.getKey().longValue();
    committedSubmissionIds.add(submissionId);
    return submissionId;
  }

  /** Sets the maximum wait (ms) of the submission service. */
  private void setMaxWait(long maxWait) {
    SubmissionService target = AopTestUtils.getTargetObject(submissionService);
    ReflectionTestUtils.setField(target, "maxWait", maxWait);
  }

  /** Gets the slug of the judge result of a submission. */
  private String getJudgeResult(long submissionId) {
    return jdbcTemplate.queryForObject(
        "SELECT submission_judge_result FROM voj_submissions WHERE submission_id = ?",
        String.class,
        submissionId);
  }

  /** The maximum wait (ms): the default, as the test properties do not set it. */
  private static final long MAX_WAIT = 1800000;

  /** The autowired SubmissionService object. */
  @Autowired private SubmissionService submissionService;

  /** The autowired ApplicationEventListener, which holds the live judging streams. */
  @Autowired private ApplicationEventListener applicationEventListener;

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;

  /** Runs the fixture and assertion queries (auto-commit). */
  private JdbcTemplate jdbcTemplate;

  /** The submissions a test committed, removed after it. */
  private final List<Long> committedSubmissionIds = new ArrayList<>();
}
