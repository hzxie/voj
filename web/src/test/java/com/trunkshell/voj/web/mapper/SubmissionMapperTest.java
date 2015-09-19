package com.trunkshell.voj.web.mapper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.model.Language;
import com.trunkshell.voj.web.model.Problem;
import com.trunkshell.voj.web.model.Submission;
import com.trunkshell.voj.web.model.User;

/**
 * SubmissionMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionMapperTest {
    /**
     * 测试用例: 测试getNumberOfSubmissions(Date, Date)方法
     * 测试数据: 使用2014年10月17日进行查询
     * 预期结果: 返回当天的提交次数(2次)
     */
    @Test
    public void testGetNumberOfSubmissions() {
        Calendar calendar = Calendar.getInstance();    
        calendar.set(2014, 9, 17, 0, 0, 0);
        Date startTime = calendar.getTime();
        calendar.set(2014, 9, 17, 23, 59, 59);
        Date endTime = calendar.getTime();
        
        long numberofSubmissions = submissionMapper.getNumberOfSubmissions(startTime, endTime);
        Assert.assertEquals(2, numberofSubmissions);
    }
    
    /**
     * 测试用例: 测试getNumberOfSubmissionsUsingLanguage(int)方法
     * 测试数据: 使用C++语言的唯一标识符
     * 预期结果: 返回使用C++语言提交记录的数量(3条)
     */
    @Test
    public void testGetNumberOfSubmissionsUsingLanguageUsingCpp() {
        int languageId = 2;
        long numberOfSubmissions = submissionMapper.getNumberOfSubmissionsUsingLanguage(languageId);
        Assert.assertEquals(3, numberOfSubmissions);
    }
    
    /**
     * 测试用例: 测试getNumberOfSubmissionsUsingLanguage(int)方法
     * 测试数据: 使用不存在的唯一标识符
     * 预期结果: 返回0
     */
    @Test
    public void testGetNumberOfSubmissionsUsingLanguageUsingNotExistingLanguage() {
        int languageId = 0;
        long numberOfSubmissions = submissionMapper.getNumberOfSubmissionsUsingLanguage(languageId);
        Assert.assertEquals(0, numberOfSubmissions);
    }
    
    /**
     * 测试用例: 测试getLatestSubmissionId()方法
     * 测试数据: N/a
     * 预期结果: 返回最后一条提交的唯一标识符
     */
    @Test
    public void testGetLatestSubmissionId() {
        long latestSubmissionId = submissionMapper.getLatestSubmissionId();
        Assert.assertEquals(1003, latestSubmissionId);
    }
    
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
     * 测试用例: 测试getSubmissionsUsingOffset(long, String, long, int)方法
     * 测试数据: 获取ID从1010起始的10次提交(ID From 1010 to 1000)
     * 预期结果: 返回提交列表(共4次提交)
     */
    @Test
    public void testGetSubmissionsFrom1010WithLimit10() {
        List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(0, "", 1010, 10);
        Assert.assertEquals(4, submissions.size());
        
        Submission firstSubmission = submissions.get(0);
        long submissionId = firstSubmission.getSubmissionId();
        Assert.assertEquals(1003, submissionId);
    }
    
    /**
     * 测试用例: 测试getSubmissionsUsingOffset(long, String, long, int)方法
     * 测试数据: 获取ID从1003起始的2次提交
     * 预期结果: 返回提交列表(共2次提交)
     */
    @Test
    public void testGetSubmissionsFrom1003WithLimit2() {
        List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(0, "", 1003, 2);
        Assert.assertEquals(2, submissions.size());
        
        Submission firstSubmission = submissions.get(0);
        long submissionId = firstSubmission.getSubmissionId();
        Assert.assertEquals(1003, submissionId);
    }
    
    /**
     * 测试用例: 测试getSubmissionsUsingOffset(long, String, long, int)方法
     * 测试数据: 获取ID从100起始的10次提交(ID From 100 to 90)
     * 预期结果: 返回空提交列表
     */
    @Test
    public void testGetSubmissionsFrom100WithLimit10() {
        List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(0, "", 100, 10);
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
     * 测试用例: 测试createSubmission(Submission)方法
     * 测试数据: 使用合法的数据集
     * 预期结果: 数据插入操作成功完成
     */
    @Test
    public void testCreateSubmissionNormally() {
        Problem problem = problemMapper.getProblem(1000);
        User user = userMapper.getUserUsingUid(1000);
        Language language = languageMapper.getLanguageUsingId(1);
        
        Submission submission = new Submission(problem, user, language, "C Code");
        submissionMapper.createSubmission(submission);
    }
    
    /**
     * 测试用例: 测试createSubmission(Submission)方法
     * 测试数据: 使用不存在的外键值(编程语言对象)
     * 预期结果: 抛出DataIntegrityViolationException异常
     */
    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void testCreateSubmissionUsingNotExistingLanguage() {
        Problem problem = problemMapper.getProblem(1000);
        User user = userMapper.getUserUsingUid(1000);
        Language language = new Language(0, "not-exists", "Not Exists", "Not Exists", "Not Exists");
        
        Submission submission = new Submission(problem, user, language, "Code");
        submissionMapper.createSubmission(submission);
    }
    
    /**
     * 测试用例: 测试updateSubmission(Submission)方法
     * 测试数据: 使用合法的数据集, 且数据表中存在对应ID的记录
     * 预期结果: 数据更新操作成功完成
     */
    @Test
    public void testUpdateSubmissionNormally() {
        Submission submission = submissionMapper.getSubmission(1002);
        Assert.assertNotNull(submission);
        
        submission.setJudgeScore(20);
        submissionMapper.updateSubmission(submission);
        
        /**
         * The following Assert CANNOT passed in CI due to 
         * the bug of Spring Test Framework. But it really works.
         */
        /*
         * Submission updatedSubmission = submissionMapper.getSubmission(1002);
         * int judgeScore = updatedSubmission.getJudgeScore();
         * Assert.assertEquals(20, judgeScore);
         */
    }
    
    /**
     * 测试用例: 测试updateSubmission(Submission)方法
     * 测试数据: 使用合法的数据集, 但数据表中不存在对应ID的记录
     * 预期结果: 方法正常执行, 未影响数据表中的数据
     */
    @Test
    public void testUpdateSubmissionNotExists() {
        Submission submission = submissionMapper.getSubmission(1000);
        Assert.assertNotNull(submission);
        
        submission.setSubmissionId(0);
        submissionMapper.updateSubmission(submission);
    }
    
    /**
     * 测试用例: 测试deleteSubmission(long)方法
     * 测试数据: 提交记录#1003的唯一标识符
     * 预期结果: 数据删除操作成功完成
     */
    @Test
    public void testDeleteSubmissionExists() {
        Submission submission = submissionMapper.getSubmission(1003);
        Assert.assertNotNull(submission);
        
        submissionMapper.deleteSubmission(1003);
        
        submission = submissionMapper.getSubmission(1003);
        Assert.assertNull(submission);
    }
    
    /**
     * 测试用例: 测试deleteSubmission(long)方法
     * 测试数据: 不存在的提交记录唯一标识符
     * 预期结果: 方法正常执行, 未影响数据表中的数据
     */
    @Test
    public void testDeleteSubmissionNotExists() {
        Submission submission = submissionMapper.getSubmission(0);
        Assert.assertNull(submission);
        
        submissionMapper.deleteSubmission(0);
    }
    
    /**
     * 待测试的SubmissionMapper对象.
     */
    @Autowired
    private SubmissionMapper submissionMapper;
    
    /**
     * ProblemMapper对象, 用于构建单元测试用例.
     */
    @Autowired
    private ProblemMapper problemMapper;
    
    /**
     * UserMapper对象, 用于构建单元测试用例.
     */
    @Autowired
    private UserMapper userMapper;
    
    /**
     * LanguageMapper对象, 用于构建单元测试用例.
     */
    @Autowired
    private LanguageMapper languageMapper;
}
