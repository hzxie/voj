package com.trunkshell.voj.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

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
	@Test
	public void testGetSubmissionExists() {
		Submission submission = submissionMapper.getSubmission(1);
		System.out.println(submission);
	}
	
	/**
	 * 待测试的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
}
