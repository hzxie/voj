package com.trunkshell.voj.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.model.Problem;
import com.trunkshell.voj.model.Submission;

/**
 * SubmissionMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionMapperTest {
	/**
	 * 测试用例: 测试getSubmission(long)方法
	 * 测试数据: Problem#1000的提交记录的唯一标识符
	 * 预期结果: 返回预期的Submission对象
	 */
	@Test
	public void testGetSubmissionExists() {
		Submission submission = submissionMapper.getSubmission(1000);
		Assert.assertNotNull(submission);
		
		Problem problem = submission.getProblem();
		long problemId = problem.getProblemId();
		Assert.assertEquals(1000, problemId);
	}
	
	/**
	 * 测试用例: 测试getSubmission(long)方法
	 * 测试数据: 不存在的的提交记录唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetSubmissionNotExists() {
		Submission submission = submissionMapper.getSubmission(0);
		Assert.assertNull(submission);
	}
	
	/**
	 * 测试用例: 测试getSubmissionsUsingOffset(long, int)方法
	 * 测试数据: 获取ID从1010起始的10次提交(ID From 1010 to 1000)
	 * 预期结果: 返回提交列表(共4次提交)
	 */
	@Test
	public void testGetSubmissionsFrom1010WithLimit10() {
		List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(1010, 10);
		Assert.assertEquals(4, submissions.size());
		
		Submission firstSubmission = submissions.get(0);
		long submissionId = firstSubmission.getSubmissionId();
		Assert.assertEquals(1003, submissionId);
	}
	
	/**
	 * 测试用例: 测试getSubmissionsUsingOffset(long, int)方法
	 * 测试数据: 获取ID从1003起始的2次提交
	 * 预期结果: 返回提交列表(共2次提交)
	 */
	@Test
	public void testGetSubmissionsFrom1003WithLimit2() {
		List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(1003, 2);
		Assert.assertEquals(2, submissions.size());
		
		Submission firstSubmission = submissions.get(0);
		long submissionId = firstSubmission.getSubmissionId();
		Assert.assertEquals(1003, submissionId);
	}
	
	/**
	 * 测试用例: 测试getSubmissionsUsingOffset(long, int)方法
	 * 测试数据: 获取ID从100起始的10次提交(ID From 100 to 90)
	 * 预期结果: 返回空提交列表
	 */
	@Test
	public void testGetSubmissionsFrom100WithLimit10() {
		List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(100, 10);
		Assert.assertEquals(0, submissions.size());
	}
	
	/**
	 * 测试用例: 测试getSubmissionUsingProblemIdAndUserId(long, long, int)方法
	 * 测试数据: 获取试题ID#1000和用户ID#1000的最新1次提交
	 * 预期结果: 返回预期的提交列表(共1次提交)
	 */
	@Test
	public void testGetSubmissionUsingExistingProblemIdAndExistingUserId() {
		List<Submission> submissions = submissionMapper.getSubmissionUsingProblemIdAndUserId(1000, 1000, 1);
		Assert.assertEquals(1, submissions.size());
		
		Submission firstSubmission = submissions.get(0);
		long submissionId = firstSubmission.getSubmissionId();
		Assert.assertEquals(1001, submissionId);
		
		String judgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
		Assert.assertEquals("WA", judgeResultSlug);
	}
	
	/**
	 * 测试用例: 测试getSubmissionUsingProblemIdAndUserId(long, long, int)方法
	 * 测试数据: 使用不存在的试题唯一标识符
	 * 预期结果: 返回空提交列表
	 */
	@Test
	public void testGetSubmissionUsingNotExistingProblemIdAndExistingUserId() {
		List<Submission> submissions = submissionMapper.getSubmissionUsingProblemIdAndUserId(0, 1000, 10);
		Assert.assertEquals(0, submissions.size());
	}
	
	/**
	 * 测试用例: 测试getSubmissionUsingProblemIdAndUserId(long, long, int)方法
	 * 测试数据: 使用不存在的用户唯一标识符
	 * 预期结果: 返回空提交列表
	 */
	@Test
	public void testGetSubmissionUsingExistingProblemIdAndNotExistingUserId() {
		List<Submission> submissions = submissionMapper.getSubmissionUsingProblemIdAndUserId(1000, 0, 10);
		Assert.assertEquals(0, submissions.size());
	}
	
	/**
	 * 测试用例: 测试getLatestSubmissionOfProblems(long, long, long)方法
	 * 测试数据: 获取用户ID#1000在试题ID区间[1000, 1010)的最新评测结果
	 * 预期结果: 返回预期的提交列表(试题ID#1000 - WA, 试题ID#1001 - AC)
	 */
	@Test
	public void testGetLatestSubmissionOfProblemsWithUser1000() {
		List<Submission> submissions = submissionMapper.getLatestSubmissionOfProblems(1000, 1000, 1010);
		Assert.assertEquals(2, submissions.size());
		
		Submission firstSubmission = submissions.get(0);
		String firstJudgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
		Assert.assertEquals("WA", firstJudgeResultSlug);
		
		Submission secondSubmission = submissions.get(1);
		String secondJudgeResultSlug = secondSubmission.getJudgeResult().getJudgeResultSlug();
		Assert.assertEquals("AC", secondJudgeResultSlug);
	}
	
	/**
	 * 测试用例: 测试getLatestSubmissionOfProblems(long, long, long)方法
	 * 测试数据: 获取用户ID#1001在试题ID区间[1000, 1010)的最新评测结果
	 * 预期结果: 返回预期的提交列表(试题ID#1000 - CE)
	 */
	@Test
	public void testGetLatestSubmissionOfProblemsWithUser1001() {
		List<Submission> submissions = submissionMapper.getLatestSubmissionOfProblems(1001, 1000, 1010);
		Assert.assertEquals(1, submissions.size());
		
		Submission firstSubmission = submissions.get(0);
		String firstJudgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
		Assert.assertEquals("CE", firstJudgeResultSlug);
	}
	
	/**
	 * 测试用例: 测试getAcceptedSubmissionOfProblems(long, long, long)方法
	 * 测试数据: 获取用户ID#1000在试题ID区间[1000, 1010)的通过评测结果
	 * 预期结果: 返回预期的提交列表(试题ID#1000 - AC, 试题ID#1001 - AC)
	 */
	@Test
	public void testGetAcceptedSubmissionOfProblemsWithUser1000() {
		List<Submission> submissions = submissionMapper.getAcceptedSubmissionOfProblems(1000, 1000, 1010);
		Assert.assertEquals(2, submissions.size());
		
		Submission firstSubmission = submissions.get(0);
		String firstJudgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
		Assert.assertEquals("AC", firstJudgeResultSlug);
		
		Submission secondSubmission = submissions.get(1);
		String secondJudgeResultSlug = secondSubmission.getJudgeResult().getJudgeResultSlug();
		Assert.assertEquals("AC", secondJudgeResultSlug);
	}
	
	/**
	 * 测试用例: 测试getAcceptedSubmissionOfProblems(long, long, long)方法
	 * 测试数据: 获取用户ID#1001在试题ID区间[1000, 1010)的通过评测结果
	 * 预期结果: 返回空提交列表
	 */
	@Test
	public void testGetAcceptedSubmissionOfProblemsWithUser1001() {
		List<Submission> submissions = submissionMapper.getAcceptedSubmissionOfProblems(1001, 1000, 1010);
		Assert.assertEquals(0, submissions.size());
	}
	
	/**
	 * 测试用例: 测试getAcceptedSubmissionUsingUserId(long)方法
	 * 测试数据: 获取用户ID#1001通过的提交次数
	 * 预期结果: 返回用户ID#1001通过的提交次数(共2次)
	 */
	@Test
	public void testGetAcceptedSubmissionUsingUserIdWithUser1000() {
		long acceptedSubmission = submissionMapper.getAcceptedSubmissionUsingUserId(1000);
		Assert.assertEquals(2, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getTotalSubmissionUsingUserId(long)方法
	 * 测试数据: 获取用户ID#1001总提交次数
	 * 预期结果: 返回用户ID#1001总提交次数(共4次)
	 */
	@Test
	public void testGetTotalSubmissionUsingUserIdWithUser1000() {
		long acceptedSubmission = submissionMapper.getTotalSubmissionUsingUserId(1000);
		Assert.assertEquals(3, acceptedSubmission);
	}
	
	/**
	 * 待测试的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
}
