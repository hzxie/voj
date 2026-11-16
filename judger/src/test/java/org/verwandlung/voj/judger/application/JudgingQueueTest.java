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
package org.verwandlung.voj.judger.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.core.CheckpointStore;
import org.verwandlung.voj.judger.core.Comparator;
import org.verwandlung.voj.judger.core.Compiler;
import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.core.Preprocessor;
import org.verwandlung.voj.judger.core.ProgramRunner;
import org.verwandlung.voj.judger.mapper.JudgeResultMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Checks the judging queue against the database: judgers that claim at the same time never claim
 * the same submission, the lease of a judging keeps other judgings off its submission however long
 * a step takes, until the judging is superseded, and the failed judgings of a submission are
 * counted until it is given up on, without holding up the others.
 *
 * <p>The judgers claim in transactions of their own, so this class is not {@code @Transactional};
 * the submissions a test commits are removed afterwards.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class JudgingQueueTest {
  @BeforeEach
  public void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @AfterEach
  public void tearDown() {
    jdbcTemplate.update(
        "DELETE FROM voj_submissions WHERE submission_id BETWEEN ? AND ?",
        FIRST_SUBMISSION_ID,
        FIRST_SUBMISSION_ID + 999);
  }

  /**
   * Test case: two judgers claim the pending submissions in C++ at the same time, each until there
   * is none left. Test data: 50 pending submissions in C++, queued at the same time. Expected
   * result: every submission is claimed by exactly one of them, once, and none counts a failed
   * judging.
   */
  @Test
  public void testJudgersNeverClaimTheSameSubmission() throws Exception {
    Set<Long> pending = new HashSet<>();
    for (int i = 0; i < 50; ++i) {
      pending.add(insertPendingSubmission(FIRST_SUBMISSION_ID + i));
    }
    CyclicBarrier start = new CyclicBarrier(2);
    Callable<List<Long>> judger =
        () -> {
          List<Long> claimed = new ArrayList<>();
          start.await();
          for (Submission submission = applicationDispatcher.claimSubmission(CPP, 60);
              submission != null;
              submission = applicationDispatcher.claimSubmission(CPP, 60)) {
            claimed.add(submission.getSubmissionId());
          }
          return claimed;
        };
    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<Long> first;
    List<Long> second;
    try {
      Future<List<Long>> firstJudger = executor.submit(judger);
      Future<List<Long>> secondJudger = executor.submit(judger);
      first = firstJudger.get();
      second = secondJudger.get();
    } finally {
      executor.shutdownNow();
    }

    Set<Long> claimed = new HashSet<>(first);
    claimed.addAll(second);
    Assertions.assertEquals(first.size() + second.size(), claimed.size(), first + " / " + second);
    Assertions.assertEquals(pending, claimed);
    Assertions.assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM voj_submissions WHERE submission_id BETWEEN ? AND ?"
                + " AND (submission_judge_attempts <> 0 OR submission_claim_token IS NULL)",
            Integer.class,
            FIRST_SUBMISSION_ID,
            FIRST_SUBMISSION_ID + 999));
  }

  /**
   * Test case: a judging whose compilation takes two and a half times as long as its lease (2 s),
   * while another judger looks for a submission in the middle of it. Test data: a pending
   * submission in C++. Expected result: the lease, renewed in the background, keeps the other
   * judger off the submission, and the judging records its verdict (a Compile Error) and reports
   * it.
   */
  @Test
  public void testLeaseKeepsALongJudgingAlive() throws Exception {
    long submissionId = insertPendingSubmission(FIRST_SUBMISSION_ID);
    AtomicReference<Submission> claimedByAnotherJudger = new AtomicReference<>();
    when(compiler.getCompileResult(any(), anyString()))
        .thenAnswer(
            invocation -> {
              Thread.sleep(LEASE_TIMEOUT * 3 / 2);
              claimedByAnotherJudger.set(applicationDispatcher.claimSubmission(CPP, 2));
              Thread.sleep(LEASE_TIMEOUT);
              return compileResult(false);
            });

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    Assertions.assertNull(claimedByAnotherJudger.get());
    Assertions.assertEquals(
        Map.of("submission_judge_result", "CE", "submission_judge_attempts", 0),
        jdbcTemplate.queryForMap(
            "SELECT submission_judge_result, submission_judge_attempts FROM voj_submissions"
                + " WHERE submission_id = ?",
            submissionId));
    verify(messageSender)
        .sendMessage(
            argThat(
                message ->
                    "CompileFinished".equals(message.get("event"))
                        && Long.valueOf(submissionId).equals(message.get("submissionId"))));
  }

  /**
   * Test case: a submission is rejudged while its judging compiles it. Test data: a pending
   * submission in C++. Expected result: the judging finds, when it renews its lease, that it was
   * superseded, and stops without running the program, recording anything or sending any message;
   * the submission waits for a judger, as the rejudge left it.
   */
  @Test
  public void testSupersededJudgingStopsSilently() throws Exception {
    long submissionId = insertPendingSubmission(FIRST_SUBMISSION_ID);
    when(compiler.getCompileResult(any(), anyString()))
        .thenAnswer(
            invocation -> {
              // A rejudge, as the web application does it (resetSubmissionToPending).
              jdbcTemplate.update(
                  "UPDATE voj_submissions SET submission_judge_result = 'PD',"
                      + " submission_execute_time = NULL, judger_uid = NULL,"
                      + " submission_queued_time = NOW(), submission_judge_attempts = 0,"
                      + " submission_claim_token = NULL, submission_lease_expiry_time = NULL"
                      + " WHERE submission_id = ?",
                  submissionId);
              Thread.sleep(LEASE_TIMEOUT);
              return compileResult(true);
            });

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    verify(programRunner, never())
        .getRuntimeResult(any(), anyString(), anyString(), anyString());
    verify(messageSender, never()).sendMessage(any());
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT submission_judge_result, submission_judge_attempts, submission_claim_token"
                + " FROM voj_submissions WHERE submission_id = ?",
            submissionId);
    Assertions.assertEquals("PD", row.get("submission_judge_result"));
    Assertions.assertEquals(0, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertNull(row.get("submission_claim_token"));
  }

  /**
   * Test case: a judger takes a submission and vanishes (its lease is never renewed, until it
   * expired), as often as allowed. Test data: a pending submission in C++. Expected result: each
   * judger that takes it after a vanished one counts a failed judging, with the reason of an
   * interrupted one; the one that takes it after the last allowed one judges it a System Error that
   * says how many judgings failed and why the last one did, without judging it.
   */
  @Test
  public void testVanishedJudgingsAreCountedUntilTheSubmissionIsGivenUpOn() throws Exception {
    long submissionId = insertPendingSubmission(FIRST_SUBMISSION_ID);
    for (int failed = 0; failed < Dispatcher.MAX_FAILED_JUDGINGS; ++failed) {
      Submission submission = applicationDispatcher.claimSubmission(CPP, 60);
      Assertions.assertEquals(failed, submission.getFailedJudgings());
      expireLease(submissionId);
    }

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    verify(compiler, never()).getCompileResult(any(), anyString());
    Map<String, Object> row = getRow(submissionId);
    Assertions.assertEquals("SE", row.get("submission_judge_result"));
    Assertions.assertEquals(
        Dispatcher.MAX_FAILED_JUDGINGS, ((Number) row.get("submission_judge_attempts")).intValue());
    String judgeLog = (String) row.get("submission_judge_log");
    Assertions.assertTrue(
        judgeLog.contains(Dispatcher.MAX_FAILED_JUDGINGS + " judgings of this submission failed"),
        judgeLog);
    Assertions.assertTrue(judgeLog.contains("ran past its deadline"), judgeLog);
  }

  /**
   * Test case: the judgings of a submission always fail (the sandbox cannot run its compiler, e.g.
   * as it broke its own runs), while another submission waits behind it. Test data: two pending
   * submissions in C++, the failing one queued first. Expected result: each failed judging sends
   * the failing submission to the end of the queue, so that the other one is judged after its first
   * failed judging; after as many failed judgings as allowed, the judger that takes it judges it a
   * System Error with the reason of the last one.
   */
  @Test
  public void testASubmissionWhoseJudgingsFailIsGivenUpOnWithoutHoldingUpOthers()
      throws Exception {
    long failing = insertPendingSubmission(FIRST_SUBMISSION_ID, 120);
    long other = insertPendingSubmission(FIRST_SUBMISSION_ID + 1, 60);
    Map<String, Object> sandboxFailure = compileResult(false);
    sandboxFailure.put("verdict", org.verwandlung.voj.judger.core.Verdict.INTERNAL_ERROR);
    when(compiler.getCompileResult(any(), anyString()))
        .thenAnswer(
            invocation ->
                invocation.<Submission>getArgument(0).getSubmissionId() == failing
                    ? sandboxFailure
                    : compileResult(false));

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));
    Assertions.assertEquals("PD", getRow(failing).get("submission_judge_result"));
    Assertions.assertEquals(1, ((Number) getRow(failing).get("submission_judge_attempts")).intValue());
    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));
    Assertions.assertEquals("CE", getRow(other).get("submission_judge_result"));
    for (int failed = 1; failed < Dispatcher.MAX_FAILED_JUDGINGS; ++failed) {
      Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));
      Assertions.assertEquals("PD", getRow(failing).get("submission_judge_result"));
    }
    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    Map<String, Object> row = getRow(failing);
    Assertions.assertEquals("SE", row.get("submission_judge_result"));
    Assertions.assertEquals(
        Dispatcher.MAX_FAILED_JUDGINGS, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertTrue(
        ((String) row.get("submission_judge_log")).contains("failed to run the compiler"),
        row.toString());
    Assertions.assertNull(applicationDispatcher.claimSubmission(CPP, 60));
  }

  /**
   * Test case: a judger whose downloads of the test data all fail with an HTTP 404 that the web
   * application did not mark (its judger.web.baseUrl is wrong). Test data: three pending
   * submissions in C++. Expected result: the judger takes each of them in turn (each failed judging
   * sends its submission to the end of the queue), is not ready after as many failed judgings in a
   * row as allowed, and judges none of them a System Error.
   */
  @Test
  public void testAJudgerWhoseDownloadsFailIsNotReadyWithoutFailingSubmissions()
      throws Exception {
    List<Long> submissionIds =
        List.of(
            insertPendingSubmission(FIRST_SUBMISSION_ID, 180),
            insertPendingSubmission(FIRST_SUBMISSION_ID + 1, 120),
            insertPendingSubmission(FIRST_SUBMISSION_ID + 2, 60));
    when(checkpointStore.sync(any()))
        .thenThrow(new java.io.IOException("Failed to download checkpoints: HTTP 404"));

    for (int i = 0; i < Dispatcher.MAX_FAILURES_IN_A_ROW; ++i) {
      Assertions.assertTrue(dispatcher.isReady());
      Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));
    }

    Assertions.assertFalse(dispatcher.isReady());
    for (long submissionId : submissionIds) {
      Map<String, Object> row = getRow(submissionId);
      Assertions.assertEquals("PD", row.get("submission_judge_result"), row.toString());
      Assertions.assertEquals(1, ((Number) row.get("submission_judge_attempts")).intValue());
      Assertions.assertNull(row.get("submission_claim_token"));
    }
  }

  /**
   * Test case: the web application says that it has no test data for the problem of a submission
   * (a 404 it marked). Test data: a pending submission in C++. Expected result: a System Error that
   * says so, without a failed judging; the judger is ready.
   */
  @Test
  public void testAProblemWithoutTestDataIsSystemError() throws Exception {
    long submissionId = insertPendingSubmission(FIRST_SUBMISSION_ID);
    when(checkpointStore.sync(any()))
        .thenThrow(new org.verwandlung.voj.judger.exception.NoCheckpointsException("HTTP 404"));

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    Map<String, Object> row = getRow(submissionId);
    Assertions.assertEquals("SE", row.get("submission_judge_result"));
    Assertions.assertEquals(0, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertTrue(
        ((String) row.get("submission_judge_log")).contains("no test data"), row.toString());
    Assertions.assertTrue(dispatcher.isReady());
  }

  /**
   * Test case: a judging fails (e.g. the web application cannot be reached from its judger's host).
   * Test data: a pending submission in C++. Expected result: the submission is released at once:
   * pending, held by no judging, with the failed judging and its reason counted, and queued anew,
   * so that a judger takes it again.
   */
  @Test
  public void testAFailedJudgingReleasesTheSubmission() throws Exception {
    long submissionId = insertPendingSubmission(FIRST_SUBMISSION_ID, 3600);
    when(checkpointStore.sync(any())).thenThrow(new java.io.IOException("Connection refused"));

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    Map<String, Object> row = getRow(submissionId);
    Assertions.assertEquals("PD", row.get("submission_judge_result"));
    Assertions.assertEquals(1, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertNull(row.get("submission_claim_token"));
    Assertions.assertNull(row.get("submission_lease_expiry_time"));
    Assertions.assertTrue(((Number) row.get("queued_seconds_ago")).longValue() < 60, row.toString());
    verify(messageSender, never()).sendMessage(any());

    Submission submission = applicationDispatcher.claimSubmission(CPP, 60);
    Assertions.assertEquals(submissionId, submission.getSubmissionId());
    Assertions.assertEquals(1, submission.getFailedJudgings());
    Assertions.assertEquals(
        "The judger could not download the test data of the problem.",
        submission.getLastFailure());
  }

  /**
   * Test case: the judger shuts down (e.g. in a rolling update) while it compiles a submission.
   * Test data: a pending submission in C++, queued an hour ago. Expected result: the submission is
   * released at its place in the queue, without a failed judging, and the verdict of the judging,
   * which goes on until the process exits, is dropped.
   */
  @Test
  public void testAShutdownReleasesTheSubmissionAtItsPlace() throws Exception {
    long submissionId = insertPendingSubmission(FIRST_SUBMISSION_ID, 3600);
    when(compiler.getCompileResult(any(), anyString()))
        .thenAnswer(
            invocation -> {
              dispatcher.destroy();
              return compileResult(false);
            });

    Assertions.assertTrue(dispatcher.judgeNextSubmission(CPP));

    Map<String, Object> row = getRow(submissionId);
    Assertions.assertEquals("PD", row.get("submission_judge_result"));
    Assertions.assertEquals(0, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertNull(row.get("submission_claim_token"));
    Assertions.assertNull(row.get("submission_lease_expiry_time"));
    Assertions.assertTrue(
        ((Number) row.get("queued_seconds_ago")).longValue() >= 3500, row.toString());
    Assertions.assertFalse(dispatcher.judgeNextSubmission(CPP));
  }

  /** Creates a judger whose judging steps are mocks, on the database of the tests. */
  @BeforeEach
  public void setUpJudger() throws Exception {
    messageSender = mock(MessageSender.class);
    ApplicationDispatcher judgerApplication = new ApplicationDispatcher();
    ReflectionTestUtils.setField(judgerApplication, "submissionMapper", submissionMapper);
    ReflectionTestUtils.setField(judgerApplication, "judgeResultMapper", judgeResultMapper);
    ReflectionTestUtils.setField(judgerApplication, "messageSender", messageSender);

    checkpointStore = mock(CheckpointStore.class);
    when(checkpointStore.open()).thenReturn(cacheDirectory);
    when(checkpointStore.getWorkDirectory()).thenReturn(Path.of("/tmp/voj-judging-queue-tests"));
    when(checkpointStore.sync(any())).thenReturn(Path.of("/tmp/voj-judging-queue-tests/1000"));
    when(checkpointStore.getCheckpoints(anyLong()))
        .thenReturn(List.of(new Checkpoint(1000L, 1, true, 100, "input", "output")));
    compiler = mock(Compiler.class);
    programRunner = mock(ProgramRunner.class);
    dispatcher = new Dispatcher();
    ReflectionTestUtils.setField(dispatcher, "applicationDispatcher", judgerApplication);
    ReflectionTestUtils.setField(dispatcher, "checkpointStore", checkpointStore);
    ReflectionTestUtils.setField(dispatcher, "preprocessor", mock(Preprocessor.class));
    ReflectionTestUtils.setField(dispatcher, "compiler", compiler);
    ReflectionTestUtils.setField(dispatcher, "programRunner", programRunner);
    ReflectionTestUtils.setField(dispatcher, "comparator", mock(Comparator.class));
    ReflectionTestUtils.setField(dispatcher, "pollInterval", 10L);
    ReflectionTestUtils.setField(dispatcher, "leaseTimeout", LEASE_TIMEOUT);
    ReflectionTestUtils.setField(dispatcher, "isJudging", true);
    ReflectionTestUtils.setField(dispatcher, "downloadTimeout", 300L);
    ReflectionTestUtils.setField(dispatcher, "compileTimeLimit", 20000L);
  }

  /**
   * Inserts a pending submission in C++ of Problem #1000, queued now, committed.
   *
   * @param submissionId - the unique identifier of the submission
   * @return the unique identifier of the submission
   */
  private long insertPendingSubmission(long submissionId) {
    return insertPendingSubmission(submissionId, 0);
  }

  /**
   * Inserts a pending submission in C++ of Problem #1000, committed.
   *
   * @param submissionId - the unique identifier of the submission
   * @param queuedSecondsAgo - how long ago it was queued, by the database's clock
   * @return the unique identifier of the submission
   */
  private long insertPendingSubmission(long submissionId, long queuedSecondsAgo) {
    jdbcTemplate.update(
        "INSERT INTO voj_submissions (submission_id, problem_id, uid, language_id,"
            + " submission_queued_time, submission_code)"
            + " VALUES (?, 1000, 1000, 2, NOW() - INTERVAL ? SECOND, 'Code')",
        submissionId,
        queuedSecondsAgo);
    return submissionId;
  }

  /**
   * Lets the lease of the judging of a submission expire.
   *
   * @param submissionId - the unique identifier of the submission
   */
  private void expireLease(long submissionId) {
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_lease_expiry_time = NOW() - INTERVAL 1 SECOND"
            + " WHERE submission_id = ?",
        submissionId);
  }

  /**
   * Gets the columns of the judging queue of a submission, its verdict, and how long ago it was
   * queued (queued_seconds_ago).
   *
   * @param submissionId - the unique identifier of the submission
   * @return the row
   */
  private Map<String, Object> getRow(long submissionId) {
    return jdbcTemplate.queryForMap(
        "SELECT submission_judge_result, submission_judge_log, submission_judge_attempts,"
            + " submission_judge_failure, submission_claim_token, submission_lease_expiry_time,"
            + " TIMESTAMPDIFF(SECOND, submission_queued_time, NOW()) AS queued_seconds_ago"
            + " FROM voj_submissions WHERE submission_id = ?",
        submissionId);
  }

  /**
   * Builds a compilation result.
   *
   * @param isSuccessful - whether the compilation succeeded
   * @return a compilation-result map
   */
  private static Map<String, Object> compileResult(boolean isSuccessful) {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", isSuccessful);
    result.put("log", isSuccessful ? "" : "error");
    return result;
  }

  /** The first identifier of the submissions the tests insert (test-data.sql uses none of them). */
  private static final long FIRST_SUBMISSION_ID = 5000;

  /** The languages of the judgers of the tests: C++ only. */
  private static final List<Integer> CPP = List.of(2);

  /** The lease (ms) of the judgings of the tests: renewed every 500 ms. */
  private static final long LEASE_TIMEOUT = 2000;

  /** The cache directory of that judger, which holds its count of failed judgings in a row. */
  @org.junit.jupiter.api.io.TempDir Path cacheDirectory;

  /** A judger of the tests, with the steps of a judging mocked. */
  private Dispatcher dispatcher;

  /** The mocked test data of that judger. */
  private CheckpointStore checkpointStore;

  /** The mocked compiler of that judger. */
  private Compiler compiler;

  /** The mocked program runner of that judger. */
  private ProgramRunner programRunner;

  /** The mocked messages of that judger. */
  private MessageSender messageSender;

  /** Another judger, which claims the submissions with the claim statements of the judger. */
  @Autowired private ApplicationDispatcher applicationDispatcher;

  /** The SubmissionMapper of the judgers. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The JudgeResultMapper of the judgers. */
  @Autowired private JudgeResultMapper judgeResultMapper;

  /** The data source of the test database. */
  @Autowired private DataSource dataSource;

  /** Runs the fixture and assertion queries (auto-commit). */
  private JdbcTemplate jdbcTemplate;
}
