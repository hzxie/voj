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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;

/**
 * SubmissionService测试类.
 *
 * <p>种子数据: 用户1000 (zjhzxhz) 共有3条提交记录 (2条AC), 用户1001共有2条提交记录 (均为CE), 合计5条提交记录 (最大ID为1004).
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionServiceTest {
  /** 测试用例: 测试getNumberOfSubmissionsUsingDate(Date, Date)方法 测试数据: 起止时间均为null 预期结果: 返回全部提交记录的数量 */
  @Test
  public void testGetNumberOfSubmissionsUsingDate() {
    Assertions.assertEquals(5, submissionService.getNumberOfSubmissionsUsingDate(null, null));
  }

  /** 测试用例: 测试getNumberOfSubmissionsUsingProblemIdAndUsername(long, String)方法 测试数据: 用户zjhzxhz对试题1000的提交 预期结果: 返回2 */
  @Test
  public void testGetNumberOfSubmissionsUsingProblemIdAndUsername() {
    Assertions.assertEquals(
        2,
        submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(1000, "zjhzxhz"));
  }

  /** 测试用例: 测试getLatestSubmissionId()方法 测试数据: N/a 预期结果: 返回最新提交记录的唯一标识符 */
  @Test
  public void testGetLatestSubmissionId() {
    Assertions.assertEquals(1004, submissionService.getLatestSubmissionId());
  }

  /** 测试用例: 测试getSubmission(long)方法 测试数据: 存在的提交记录标识符 预期结果: 返回对应的提交记录 */
  @Test
  public void testGetSubmissionExists() {
    Submission submission = submissionService.getSubmission(1000);
    Assertions.assertNotNull(submission);
    Assertions.assertEquals(1000, submission.getSubmissionId());
  }

  /** 测试用例: 测试getSubmission(long)方法 测试数据: 不存在的提交记录标识符 预期结果: 返回空引用 */
  @Test
  public void testGetSubmissionNotExists() {
    Assertions.assertNull(submissionService.getSubmission(0));
  }

  /** 测试用例: 测试getSubmissions(long, String, int)方法 测试数据: 用户zjhzxhz对试题1000的提交 预期结果: 返回该用户对该试题的全部提交 */
  @Test
  public void testGetSubmissions() {
    List<Submission> submissions = submissionService.getSubmissions(1000, "zjhzxhz", 10);
    Assertions.assertEquals(2, submissions.size());
  }

  /** 测试用例: 测试getSubmissionUsingProblemIdAndUserId(long, long, int)方法 测试数据: 用户1000对试题1000的提交 预期结果: 返回非空列表 */
  @Test
  public void testGetSubmissionUsingProblemIdAndUserId() {
    List<Submission> submissions =
        submissionService.getSubmissionUsingProblemIdAndUserId(1000, 1000, 10);
    Assertions.assertEquals(2, submissions.size());
  }

  /** 测试用例: 测试getSubmissionStatsOfUser(long)方法 测试数据: 用户1000 预期结果: 返回正确的通过数, 总数及通过率 */
  @Test
  public void testGetSubmissionStatsOfUser() {
    Map<String, Long> stats = submissionService.getSubmissionStatsOfUser(1000);
    Assertions.assertEquals(2L, stats.get("acceptedSubmission"));
    Assertions.assertEquals(3L, stats.get("totalSubmission"));
    Assertions.assertEquals(66L, stats.get("acRate"));
  }

  /** 测试用例: 测试getSubmissionStatsOfUser(long)方法 测试数据: 无提交记录的用户 预期结果: 通过率为0且不抛出除零异常 */
  @Test
  public void testGetSubmissionStatsOfUserWithoutSubmissions() {
    Map<String, Long> stats = submissionService.getSubmissionStatsOfUser(9999);
    Assertions.assertEquals(0L, stats.get("acceptedSubmission"));
    Assertions.assertEquals(0L, stats.get("totalSubmission"));
    Assertions.assertEquals(0L, stats.get("acRate"));
  }

  /** 测试用例: 测试getSubmissionOfUser(long)方法 测试数据: 用户1000 预期结果: 返回按试题索引的最新提交记录 */
  @Test
  public void testGetSubmissionOfUser() {
    Map<Long, Submission> submissions = submissionService.getSubmissionOfUser(1000);
    Assertions.assertNotNull(submissions);
    // 用户1000对试题1000和1001均有提交记录.
    Assertions.assertTrue(submissions.containsKey(1000L));
    Assertions.assertTrue(submissions.containsKey(1001L));
  }

  /** 测试用例: 测试getSubmissionOfProblems(long, long, long)方法 测试数据: 用户1000在试题区间[1000, 1001] 预期结果: AC提交记录覆盖最新提交记录 */
  @Test
  public void testGetSubmissionOfProblems() {
    Map<Long, Submission> submissions = submissionService.getSubmissionOfProblems(1000, 1000, 1001);
    // 试题1000虽有WA(1001)的最新提交, 但应被AC(1000)的提交覆盖.
    Assertions.assertEquals("AC", submissions.get(1000L).getJudgeResult().getJudgeResultSlug());
  }

  /** 测试用例: 测试createSubmission(...)方法 测试数据: 合法的提交数据 预期结果: 创建成功并返回提交记录标识符 */
  @Test
  public void testCreateSubmissionSuccessfully() {
    Map<String, Object> result =
        submissionService.createSubmission(
            userWithUid(1000), 1000, "text/x-java", "public class Main {}", true);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("submissionId"));
    Assertions.assertEquals(6, submissionService.getNumberOfSubmissionsUsingDate(null, null));
  }

  /** 测试用例: 测试createSubmission(...)方法 测试数据: 用户未登录 预期结果: 创建失败 */
  @Test
  public void testCreateSubmissionWithoutLogin() {
    Map<String, Object> result =
        submissionService.createSubmission(null, 1000, "text/x-java", "Code", true);
    Assertions.assertFalse((Boolean) result.get("isUserLogined"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createSubmission(...)方法 测试数据: 试题不存在 预期结果: 创建失败 */
  @Test
  public void testCreateSubmissionWithNonExistentProblem() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 99999, "text/x-java", "Code", true);
    Assertions.assertFalse((Boolean) result.get("isProblemExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createSubmission(...)方法 测试数据: 编程语言不存在 预期结果: 创建失败 */
  @Test
  public void testCreateSubmissionWithNonExistentLanguage() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 1000, "not-a-language", "Code", true);
    Assertions.assertFalse((Boolean) result.get("isLanguageExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createSubmission(...)方法 测试数据: 代码为空 预期结果: 创建失败 */
  @Test
  public void testCreateSubmissionWithEmptyCode() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 1000, "text/x-java", "", true);
    Assertions.assertTrue((Boolean) result.get("isCodeEmpty"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createSubmission(...)方法 测试数据: CSRF令牌无效 预期结果: 创建失败 */
  @Test
  public void testCreateSubmissionWithInvalidCsrfToken() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 1000, "text/x-java", "Code", false);
    Assertions.assertFalse((Boolean) result.get("isCsrfTokenValid"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试deleteSubmission(long)方法 测试数据: 未被竞赛引用的提交记录 预期结果: 删除成功, 提交记录不再存在 */
  @Test
  public void testDeleteSubmission() {
    // 提交记录1004未被voj_contest_submissions引用, 可安全删除.
    Assertions.assertTrue(submissionService.deleteSubmission(1004));
    Assertions.assertNull(submissionService.getSubmission(1004));
  }

  /** 测试用例: 测试createSubmissionTask(long)方法 测试数据: 任意提交标识符 预期结果: 消息发送不抛出异常 */
  @Test
  public void testCreateSubmissionTask() {
    Assertions.assertDoesNotThrow(() -> submissionService.createSubmissionTask(1000));
  }

  /** 构造一个仅设置UID的User对象, 用作提交用户参数. */
  private User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** 待测试的SubmissionService对象. */
  @Autowired private SubmissionService submissionService;
}
