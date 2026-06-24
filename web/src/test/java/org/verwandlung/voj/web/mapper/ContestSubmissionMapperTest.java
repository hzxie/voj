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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestSubmission;
import org.verwandlung.voj.web.model.Submission;

/**
 * ContestSubmissionMapper测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestSubmissionMapperTest {
  /** 测试用例: 测试getSubmissionsOfContest(long)方法 测试数据: 使用存在提交记录的竞赛唯一标识符 预期结果: 返回该竞赛全部提交记录的列表 */
  @Test
  public void testGetSubmissionsOfContestExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionsOfContest(1);
    Assertions.assertEquals(3, submissions.size());

    ContestSubmission firstSubmission = submissions.get(0);
    Assertions.assertEquals(1, firstSubmission.getContest().getContestId());
    Assertions.assertNotNull(firstSubmission.getSubmission());
  }

  /** 测试用例: 测试getSubmissionsOfContest(long)方法 测试数据: 使用不存在提交记录的竞赛唯一标识符 预期结果: 返回空列表 */
  @Test
  public void testGetSubmissionsOfContestNotExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionsOfContest(0);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** 测试用例: 测试getAcceptedSubmissionsOfContest(long)方法 测试数据: 使用存在AC提交记录的竞赛唯一标识符 预期结果: 仅返回判定为AC的提交记录 */
  @Test
  public void testGetAcceptedSubmissionsOfContestExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getAcceptedSubmissionsOfContest(1);
    Assertions.assertEquals(2, submissions.size());

    for (ContestSubmission cs : submissions) {
      Assertions.assertEquals("AC", cs.getSubmission().getJudgeResult().getJudgeResultSlug());
    }
  }

  /** 测试用例: 测试getAcceptedSubmissionsOfContest(long)方法 测试数据: 使用不存在的竞赛唯一标识符 预期结果: 返回空列表 */
  @Test
  public void testGetAcceptedSubmissionsOfContestNotExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getAcceptedSubmissionsOfContest(0);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** 测试用例: 测试getSubmissionOfContestOfContest(long, long)方法 测试数据: 使用提交过的参赛者 预期结果: 仅返回该参赛者在该竞赛中的提交记录 */
  @Test
  public void testGetSubmissionOfContestOfContestForContestant() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(1, 1000);
    Assertions.assertEquals(2, submissions.size());

    for (ContestSubmission cs : submissions) {
      Assertions.assertEquals(1000, cs.getSubmission().getUser().getUid());
    }
  }

  /** 测试用例: 测试getSubmissionOfContestOfContest(long, long)方法 测试数据: 使用另一名参赛者 预期结果: 仅返回该参赛者的提交记录 */
  @Test
  public void testGetSubmissionOfContestOfContestForAnotherContestant() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(1, 1001);
    Assertions.assertEquals(1, submissions.size());
    Assertions.assertEquals(1001, submissions.get(0).getSubmission().getUser().getUid());
  }

  /** 测试用例: 测试getSubmissionOfContestOfContest(long, long)方法 测试数据: 使用未提交的参赛者 预期结果: 返回空列表 */
  @Test
  public void testGetSubmissionOfContestOfContestForNonContestant() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(1, 9999);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** 测试用例: 测试getSubmissionOfContestOfContestProblem(long, long, long)方法 测试数据: 参赛者提交过该试题 预期结果: 返回该参赛者对该试题的提交记录 */
  @Test
  public void testGetSubmissionOfContestOfContestProblemExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContestProblem(1, 1000, 1000);
    Assertions.assertEquals(1, submissions.size());

    Submission submission = submissions.get(0).getSubmission();
    Assertions.assertEquals(1000, submission.getProblem().getProblemId());
    Assertions.assertEquals(1000, submission.getUser().getUid());
  }

  /** 测试用例: 测试getSubmissionOfContestOfContestProblem(long, long, long)方法 测试数据: 参赛者未提交过该试题 预期结果: 返回空列表 */
  @Test
  public void testGetSubmissionOfContestOfContestProblemNotExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContestProblem(1, 1001, 1001);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** 测试用例: 测试createContestSubmission(ContestSubmission)方法 测试数据: 合法的竞赛提交记录 预期结果: 数据创建操作成功完成 */
  @Test
  public void testCreateContestSubmission() {
    Contest contest = new Contest();
    contest.setContestId(1);
    Submission submission = new Submission();
    submission.setSubmissionId(1001);

    int numberOfRowsAffected =
        contestSubmissionMapper.createContestSubmission(new ContestSubmission(contest, submission));
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertEquals(4, contestSubmissionMapper.getSubmissionsOfContest(1).size());
  }

  /** 测试用例: 测试deleteContestSubmission(long, long)方法 测试数据: 存在的竞赛提交记录 预期结果: 数据删除操作成功完成 */
  @Test
  public void testDeleteContestSubmissionExists() {
    int numberOfRowsAffected = contestSubmissionMapper.deleteContestSubmission(1, 1000);
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertEquals(2, contestSubmissionMapper.getSubmissionsOfContest(1).size());
  }

  /** 测试用例: 测试deleteContestSubmission(long, long)方法 测试数据: 不存在的竞赛提交记录 预期结果: 方法正常执行, 未影响数据表中的数据 */
  @Test
  public void testDeleteContestSubmissionNotExists() {
    int numberOfRowsAffected = contestSubmissionMapper.deleteContestSubmission(1, 9999);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** 待测试的ContestSubmissionMapper对象. */
  @Autowired private ContestSubmissionMapper contestSubmissionMapper;
}
