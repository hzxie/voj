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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.ContestSubmission;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;

/**
 * ContestService测试类.
 *
 * <p>种子数据中竞赛的状态 (相对于当前时间): 竞赛#1 (OI, 已结束), 竞赛#2 (ACM, 进行中), 竞赛#3 (ACM, 未开始).
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestServiceTest {
  /** 测试用例: 测试getContests(String, long, int)方法 测试数据: 空关键词 预期结果: 返回全部竞赛 */
  @Test
  public void testGetContests() {
    List<Contest> contests = contestService.getContests("", 0, 10);
    Assertions.assertEquals(3, contests.size());
  }

  /** 测试用例: 测试getContest(long)方法 测试数据: 存在的竞赛标识符 预期结果: 返回对应的竞赛对象 */
  @Test
  public void testGetContestExists() {
    Contest contest = contestService.getContest(1);
    Assertions.assertNotNull(contest);
    Assertions.assertEquals("OI", contest.getContestMode());
  }

  /** 测试用例: 测试getContest(long)方法 测试数据: 不存在的竞赛标识符 预期结果: 返回空引用 */
  @Test
  public void testGetContestNotExists() {
    Assertions.assertNull(contestService.getContest(0));
  }

  /** 测试用例: 测试getProblemsOfContests(List)方法 测试数据: 包含存在与不存在的试题ID 预期结果: 仅返回存在的试题 */
  @Test
  public void testGetProblemsOfContests() {
    List<Problem> problems =
        contestService.getProblemsOfContests(Arrays.asList(1000L, 1001L, 99999L));
    Assertions.assertEquals(2, problems.size());
  }

  /** 测试用例: 测试getSubmissionsOfContestantOfContest(long, User)方法 测试数据: 参赛者为null 预期结果: 返回null */
  @Test
  public void testGetSubmissionsOfContestantOfContestWithNullContestant() {
    Assertions.assertNull(contestService.getSubmissionsOfContestantOfContest(1, null));
  }

  /** 测试用例: 测试getSubmissionsOfContestantOfContest(long, User)方法 测试数据: 参赛者1000参加了竞赛#1 预期结果: 按试题分组返回提交记录 */
  @Test
  public void testGetSubmissionsOfContestantOfContest() {
    Map<Long, ContestSubmission> submissions =
        contestService.getSubmissionsOfContestantOfContest(1, userWithUid(1000));
    // 参赛者1000在竞赛#1中分别对试题1000和1001有提交记录.
    Assertions.assertEquals(2, submissions.size());
    Assertions.assertTrue(submissions.containsKey(1000L));
    Assertions.assertTrue(submissions.containsKey(1001L));
  }

  /** 测试用例: 测试getSubmissionsOfContestantOfContestProblem(...)方法 测试数据: 竞赛或参赛者为null 预期结果: 返回null */
  @Test
  public void testGetSubmissionsOfContestantOfContestProblemWithNullArguments() {
    Assertions.assertNull(
        contestService.getSubmissionsOfContestantOfContestProblem(null, 1000, userWithUid(1000)));
    Assertions.assertNull(
        contestService.getSubmissionsOfContestantOfContestProblem(
            contestService.getContest(1), 1000, null));
  }

  /** 测试用例: 测试getSubmissionsOfContestantOfContestProblem(...)方法 测试数据: 已结束的竞赛#1 预期结果: 返回参赛者对该试题的提交记录 */
  @Test
  public void testGetSubmissionsOfContestantOfContestProblem() {
    List<Submission> submissions =
        contestService.getSubmissionsOfContestantOfContestProblem(
            contestService.getContest(1), 1000, userWithUid(1000));
    Assertions.assertEquals(1, submissions.size());
  }

  /** 测试用例: 测试getSubmissionsOfContestantOfContestProblem(...)方法 测试数据: 未开始的竞赛#3 预期结果: 返回空列表 */
  @Test
  public void testGetSubmissionsOfContestantOfContestProblemForReadyContest() {
    List<Submission> submissions =
        contestService.getSubmissionsOfContestantOfContestProblem(
            contestService.getContest(3), 1003, userWithUid(1000));
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** 测试用例: 测试getCodeSnippetOfContestProblem(...)方法 测试数据: 竞赛为null或非OI进行中竞赛 预期结果: 返回null */
  @Test
  public void testGetCodeSnippetOfContestProblemReturnsNull() {
    Assertions.assertNull(
        contestService.getCodeSnippetOfContestProblem(null, 1000, userWithUid(1000)));
    // 竞赛#2为ACM赛制, 不会返回代码快照.
    Assertions.assertNull(
        contestService.getCodeSnippetOfContestProblem(
            contestService.getContest(2), 1001, userWithUid(1000)));
  }

  /** 测试用例: 测试getNumberOfContestantsOfContest(long)方法 测试数据: 各竞赛 预期结果: 返回正确的参赛人数 */
  @Test
  public void testGetNumberOfContestantsOfContest() {
    Assertions.assertEquals(2, contestService.getNumberOfContestantsOfContest(1));
    Assertions.assertEquals(1, contestService.getNumberOfContestantsOfContest(2));
    Assertions.assertEquals(0, contestService.getNumberOfContestantsOfContest(3));
  }

  /** 测试用例: 测试isAttendContest(long, User)方法 测试数据: 已参加/未参加/未登录 预期结果: 返回正确的参加状态 */
  @Test
  public void testIsAttendContest() {
    Assertions.assertTrue(contestService.isAttendContest(1, userWithUid(1000)));
    Assertions.assertFalse(contestService.isAttendContest(1, userWithUid(9999)));
    Assertions.assertFalse(contestService.isAttendContest(1, null));
  }

  /** 测试用例: 测试attendContest(...)方法 测试数据: 满足全部条件参加未开始的竞赛#3 预期结果: 参加成功, 参赛人数加一 */
  @Test
  public void testAttendContestSuccessfully() {
    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000), true);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(1, contestService.getNumberOfContestantsOfContest(3));
  }

  /** 测试用例: 测试attendContest(...)方法 测试数据: CSRF令牌无效 预期结果: 参加失败 */
  @Test
  public void testAttendContestWithInvalidCsrfToken() {
    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000), false);
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertFalse(result.get("isCsrfTokenValid"));
  }

  /** 测试用例: 测试attendContest(...)方法 测试数据: 竞赛已结束 (非未开始状态) 预期结果: 参加失败 */
  @Test
  public void testAttendContestThatIsNotReady() {
    Map<String, Boolean> result = contestService.attendContest(1, userWithUid(1000), true);
    Assertions.assertFalse(result.get("isContestReady"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试attendContest(...)方法 测试数据: 用户未登录 预期结果: 参加失败 */
  @Test
  public void testAttendContestWithoutLogin() {
    Map<String, Boolean> result = contestService.attendContest(3, null, true);
    Assertions.assertFalse(result.get("isUserLogin"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试attendContest(...)方法 测试数据: 重复参加同一竞赛 预期结果: 第二次参加失败 */
  @Test
  public void testAttendContestThatIsAlreadyAttended() {
    contestService.attendContest(3, userWithUid(1000), true);

    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000), true);
    Assertions.assertTrue(result.get("isAttendedContest"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试getLeaderBoardForOi(long)方法 测试数据: OI赛制竞赛#1 预期结果: 返回参赛者与提交记录, 并完成排名 */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForOi() {
    Map<String, Object> leaderBoard = contestService.getLeaderBoardForOi(1);
    Assertions.assertTrue(leaderBoard.containsKey("contestants"));
    Assertions.assertTrue(leaderBoard.containsKey("submissions"));

    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(2, contestants.size());
    Assertions.assertEquals(1, contestants.get(0).getRank());
  }

  /** 测试用例: 测试getLeaderBoardForAcm(long)方法 测试数据: ACM赛制竞赛#2 预期结果: 返回参赛者与提交记录, 并完成排名 */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForAcm() {
    Map<String, Object> leaderBoard = contestService.getLeaderBoardForAcm(2);
    Assertions.assertTrue(leaderBoard.containsKey("contestants"));
    Assertions.assertTrue(leaderBoard.containsKey("submissions"));

    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(1, contestants.size());
    Assertions.assertEquals(1, contestants.get(0).getRank());
  }

  /** 测试用例: 测试rankingContestants(List)方法 测试数据: 含并列名次的参赛者列表 预期结果: 相同成绩并列, 后续名次跳过 */
  @Test
  public void testRankingContestants() {
    List<ContestContestant> contestants = new ArrayList<>();
    // 分数默认均为0, 通过罚时区分名次: 前两名罚时相同 (并列第1), 第三名罚时不同 (第3名).
    contestants.add(contestantWithTime(10));
    contestants.add(contestantWithTime(10));
    contestants.add(contestantWithTime(20));

    contestService.rankingContestants(contestants);
    Assertions.assertEquals(1, contestants.get(0).getRank());
    Assertions.assertEquals(1, contestants.get(1).getRank());
    Assertions.assertEquals(3, contestants.get(2).getRank());
  }

  /** 测试用例: 测试rankingContestants(List)方法 测试数据: 空列表 预期结果: 方法正常返回, 不抛出异常 */
  @Test
  public void testRankingContestantsWithEmptyList() {
    List<ContestContestant> contestants = new ArrayList<>();
    Assertions.assertDoesNotThrow(() -> contestService.rankingContestants(contestants));
  }

  /** 构造一个仅设置UID的User对象, 用作参赛者参数. */
  private User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** 构造一个仅设置罚时的ContestContestant对象, 用于排名测试. */
  private ContestContestant contestantWithTime(long time) {
    ContestContestant contestant = new ContestContestant();
    contestant.setTime(time);
    return contestant;
  }

  /** 待测试的ContestService对象. */
  @Autowired private ContestService contestService;
}
